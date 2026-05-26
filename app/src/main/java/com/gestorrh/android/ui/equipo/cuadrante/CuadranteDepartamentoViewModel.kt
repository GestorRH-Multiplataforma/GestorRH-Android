package com.gestorrh.android.ui.equipo.cuadrante

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.data.network.asignacion.ModalidadAsignacion
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.supervisor.SupervisorEmpleadoApi
import com.gestorrh.android.data.network.turno.RespuestaTurnoDTO
import com.gestorrh.android.data.network.turno.TurnoApiService
import com.gestorrh.android.data.repository.cuadrante.CuadranteRepositoryImpl
import com.gestorrh.android.domain.repository.ICuadranteRepository
import com.gestorrh.android.domain.usecase.supervisor.AsignarTurnoUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

/**
 * ViewModel de la pantalla de cuadrante del departamento para el rol SUPERVISOR.
 *
 * Responsabilidades:
 * - Carga las asignaciones del equipo al inicializar y expone el filtro reactivo
 *   por fecha sin nueva llamada de red al cambiar [fechaSeleccionada].
 * - Gestiona el estado del BottomSheet de asignación, cargando los catálogos
 *   (empleados y turnos) en la primera apertura y reutilizándolos en las siguientes.
 * - Bloquea reenvíos con el flag [EstadoUiCuadranteDepartamento.estaAsignando].
 * - Refresca el cuadrante tras una asignación exitosa para reflejar el nuevo turno.
 *
 * @param cuadranteRepository Contrato de dominio para las operaciones del cuadrante.
 * @param asignarTurnoUseCase Caso de uso que valida y delega la creación de asignaciones.
 */
class CuadranteDepartamentoViewModel(
    private val cuadranteRepository: ICuadranteRepository,
    private val asignarTurnoUseCase: AsignarTurnoUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiCuadranteDepartamento())
    val estadoUi: StateFlow<EstadoUiCuadranteDepartamento> = _estadoUi.asStateFlow()

    init {
        cargarAsignaciones()
    }

    fun cargarAsignaciones() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, mensajeError = null) }
            cuadranteRepository.getAsignacionesEquipo()
                .onSuccess { lista ->
                    val fechaActual = _estadoUi.value.fechaSeleccionada
                    _estadoUi.update {
                        it.copy(
                            cargando = false,
                            asignaciones = lista,
                            asignacionesFiltradas = filtrarPorFecha(lista, fechaActual)
                        )
                    }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            cargando = false,
                            mensajeError = mensajeDesdeError(error)
                        )
                    }
                }
        }
    }

    fun seleccionarFecha(fecha: LocalDate) {
        _estadoUi.update {
            it.copy(
                fechaSeleccionada = fecha,
                asignacionesFiltradas = filtrarPorFecha(it.asignaciones, fecha)
            )
        }
    }

    fun abrirBottomSheet() {
        _estadoUi.update {
            it.copy(
                mostrarBottomSheet = true,
                empleadoSeleccionado = null,
                turnoSeleccionado = null,
                fechaAsignacion = _estadoUi.value.fechaSeleccionada,
                modalidadSeleccionada = null,
                motivoCambio = ""
            )
        }
        val catalogosCargados = _estadoUi.value.empleados.isNotEmpty() &&
                _estadoUi.value.turnos.isNotEmpty()
        if (!catalogosCargados) {
            cargarCatalogos()
        }
    }

    fun cerrarBottomSheet() {
        _estadoUi.update { it.copy(mostrarBottomSheet = false) }
    }

    fun seleccionarEmpleado(empleado: RespuestaEmpleadoDTO) {
        _estadoUi.update { it.copy(empleadoSeleccionado = empleado) }
    }

    fun seleccionarTurno(turno: RespuestaTurnoDTO) {
        _estadoUi.update { it.copy(turnoSeleccionado = turno) }
    }

    fun seleccionarFechaAsignacion(fecha: LocalDate) {
        _estadoUi.update { it.copy(fechaAsignacion = fecha) }
    }

    fun seleccionarModalidad(modalidad: ModalidadAsignacion) {
        _estadoUi.update { it.copy(modalidadSeleccionada = modalidad) }
    }

    fun actualizarMotivoCambio(motivo: String) {
        _estadoUi.update { it.copy(motivoCambio = motivo) }
    }

    fun asignarTurno() {
        if (_estadoUi.value.estaAsignando) return
        val estado = _estadoUi.value
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaAsignando = true, mensajeError = null) }
            asignarTurnoUseCase(
                idEmpleado = estado.empleadoSeleccionado?.idEmpleado,
                idTurno = estado.turnoSeleccionado?.idTurno,
                fecha = estado.fechaAsignacion,
                modalidad = estado.modalidadSeleccionada,
                motivoCambio = estado.motivoCambio
            )
                .onSuccess {
                    _estadoUi.update {
                        it.copy(
                            estaAsignando = false,
                            mostrarBottomSheet = false,
                            mensajeExito = MensajeUi.Recurso(R.string.cuadrante_asignacion_exitosa)
                        )
                    }
                    cargarAsignaciones()
                }
                .onFailure { error ->
                    val mensaje = when (error) {
                        AsignarTurnoUseCase.ErrorValidacion.EmpleadoNoSeleccionado ->
                            MensajeUi.Recurso(R.string.cuadrante_error_empleado_requerido)
                        AsignarTurnoUseCase.ErrorValidacion.TurnoNoSeleccionado ->
                            MensajeUi.Recurso(R.string.cuadrante_error_turno_requerido)
                        AsignarTurnoUseCase.ErrorValidacion.FechaPasada ->
                            MensajeUi.Recurso(R.string.cuadrante_error_fecha_pasada)
                        AsignarTurnoUseCase.ErrorValidacion.ModalidadNoSeleccionada ->
                            MensajeUi.Recurso(R.string.cuadrante_error_modalidad_requerida)
                        else -> mensajeDesdeError(error)
                    }
                    _estadoUi.update { it.copy(estaAsignando = false, mensajeError = mensaje) }
                }
        }
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun exitoMostrado() {
        _estadoUi.update { it.copy(mensajeExito = null) }
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargandoCatalogos = true) }
            val empleadosDeferred = async { cuadranteRepository.getEmpleadosEquipo() }
            val turnosDeferred = async { cuadranteRepository.getTurnos() }
            val resultadoEmpleados = empleadosDeferred.await()
            val resultadoTurnos = turnosDeferred.await()
            val idSupervisor = sessionManager.getId()
            val empleados = resultadoEmpleados.getOrElse { emptyList() }
                .filter { it.idEmpleado != idSupervisor }
            val turnos = resultadoTurnos.getOrElse { emptyList() }
            val errorEmpleados = resultadoEmpleados.exceptionOrNull()
            val errorTurnos = resultadoTurnos.exceptionOrNull()
            val error = errorEmpleados ?: errorTurnos
            _estadoUi.update {
                it.copy(
                    cargandoCatalogos = false,
                    empleados = empleados,
                    turnos = turnos,
                    mensajeError = error?.let { e -> mensajeDesdeError(e) }
                )
            }
        }
    }

    private fun filtrarPorFecha(
        asignaciones: List<com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO>,
        fecha: LocalDate
    ) = asignaciones.filter { it.fecha == fecha }

    private fun mensajeDesdeError(error: Throwable): MensajeUi =
        if (error is IOException) MensajeUi.Recurso(R.string.error_conexion)
        else MensajeUi.Dinamico(error.message ?: "")

    companion object {
        fun factory(contexto: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val asignacionApiService = retrofit.create(AsignacionApiService::class.java)
                    val turnoApiService = retrofit.create(TurnoApiService::class.java)
                    val supervisorEmpleadoApi = retrofit.create(SupervisorEmpleadoApi::class.java)
                    val repository = CuadranteRepositoryImpl(
                        asignacionApiService,
                        turnoApiService,
                        supervisorEmpleadoApi
                    )
                    val useCase = AsignarTurnoUseCase(repository)
                    return CuadranteDepartamentoViewModel(
                        repository,
                        useCase,
                        sessionManager
                    ) as T
                }
            }
    }
}