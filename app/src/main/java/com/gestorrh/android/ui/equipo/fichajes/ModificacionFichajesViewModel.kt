package com.gestorrh.android.ui.equipo.fichajes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import com.gestorrh.android.data.network.supervisor.SupervisorEmpleadoApi
import com.gestorrh.android.data.repository.FichajeRepository
import com.gestorrh.android.domain.repository.IFichajeRepository
import com.gestorrh.android.domain.usecase.fichaje.ModificarFichajeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ViewModel de la pantalla de modificación manual de fichajes para el rol SUPERVISOR.
 *
 * Gestiona tres responsabilidades diferenciadas:
 * - Carga del catálogo de empleados del departamento al inicializar (reutilizando
 *   [SupervisorEmpleadoApi], excluyendo al propio supervisor igual que en el cuadrante).
 * - Carga y filtrado de fichajes por empleado y rango de fechas, con carga automática
 *   al entrar al tab (todos los empleados, hoy) y recarga al cambiar filtros.
 * - Gestión completa del estado del dialog de edición: pre-relleno con datos reales
 *   del fichaje original, validación local de horas con las mismas reglas que el servidor,
 *   y envío de la modificación al repositorio con manejo diferenciado de códigos de error.
 *
 * @property fichajeRepository Contrato de dominio para las operaciones de fichaje.
 * @property modificarFichajeUseCase Caso de uso que valida y delega la modificación.
 * @property supervisorEmpleadoApi Servicio para obtener los empleados del departamento.
 * @property sessionManager Fuente de verdad de la sesión activa, usado para excluir
 *   al propio supervisor de la lista de empleados filtrables.
 */
class ModificacionFichajesViewModel(
    private val fichajeRepository: IFichajeRepository,
    private val modificarFichajeUseCase: ModificarFichajeUseCase,
    private val supervisorEmpleadoApi: SupervisorEmpleadoApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiModificacionFichajes())
    val estadoUi: StateFlow<EstadoUiModificacionFichajes> = _estadoUi.asStateFlow()

    init {
        cargarEmpleados()
    }

    private fun cargarEmpleados() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargandoEmpleados = true) }
            try {
                val respuesta = supervisorEmpleadoApi.getEmpleadosEquipo()
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val idSupervisor = sessionManager.getId()
                    val empleados = respuesta.body()!!
                        .filter { it.idEmpleado != idSupervisor }
                    _estadoUi.update {
                        it.copy(cargandoEmpleados = false, empleados = empleados)
                    }
                    cargarFichajes()
                } else {
                    _estadoUi.update {
                        it.copy(
                            cargandoEmpleados = false,
                            mensajeError = MensajeUi.Recurso(R.string.error_conexion)
                        )
                    }
                }
            } catch (e: IOException) {
                _estadoUi.update {
                    it.copy(
                        cargandoEmpleados = false,
                        mensajeError = MensajeUi.Recurso(R.string.error_conexion)
                    )
                }
            }
        }
    }

    fun cargarFichajes() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargandoFichajes = true, mensajeError = null) }
            val estado = _estadoUi.value
            fichajeRepository.obtenerHistorialFichajesSupervisor(
                fechaInicio = estado.fechaInicio,
                fechaFin = estado.fechaFin,
                empleadoId = estado.empleadoSeleccionado?.idEmpleado
            )
                .onSuccess { lista ->
                    val ordenada = lista.sortedByDescending { it.horaEntrada }
                    _estadoUi.update { it.copy(cargandoFichajes = false, fichajes = ordenada) }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            cargandoFichajes = false,
                            mensajeError = if (error is IOException) {
                                MensajeUi.Recurso(R.string.error_conexion)
                            } else {
                                MensajeUi.Dinamico(error.message ?: "")
                            }
                        )
                    }
                }
        }
    }

    fun seleccionarEmpleado(empleado: RespuestaEmpleadoDTO?) {
        _estadoUi.update { it.copy(empleadoSeleccionado = empleado) }
        cargarFichajes()
    }

    fun actualizarFechaInicio(fecha: LocalDate) {
        _estadoUi.update { it.copy(fechaInicio = fecha) }
    }

    fun actualizarFechaFin(fecha: LocalDate) {
        _estadoUi.update { it.copy(fechaFin = fecha) }
    }

    fun abrirDialogEdicion(fichaje: RespuestaFichajeDTO) {
        _estadoUi.update {
            it.copy(
                fichajeEditando = fichaje,
                dialogEntrada = fichaje.horaEntrada,
                dialogSalida = fichaje.horaSalida,
                dialogMotivo = "",
                dialogErrorHoras = null
            )
        }
    }

    fun cerrarDialog() {
        _estadoUi.update {
            it.copy(
                fichajeEditando = null,
                dialogEntrada = null,
                dialogSalida = null,
                dialogMotivo = "",
                dialogErrorHoras = null
            )
        }
    }

    fun actualizarDialogEntrada(hora: LocalDateTime) {
        _estadoUi.update { it.copy(dialogEntrada = hora, dialogErrorHoras = null) }
    }

    fun actualizarDialogSalida(hora: LocalDateTime?) {
        _estadoUi.update { it.copy(dialogSalida = hora, dialogErrorHoras = null) }
    }

    fun actualizarDialogMotivo(motivo: String) {
        _estadoUi.update { it.copy(dialogMotivo = motivo) }
    }

    fun guardarModificacion() {
        val estado = _estadoUi.value
        val fichaje = estado.fichajeEditando ?: return
        if (estado.guardando) return

        viewModelScope.launch {
            _estadoUi.update { it.copy(guardando = true, mensajeError = null) }

            modificarFichajeUseCase(
                idFichaje = fichaje.idFichaje,
                nuevaHoraEntrada = estado.dialogEntrada,
                nuevaHoraSalida = estado.dialogSalida,
                motivoModificacion = estado.dialogMotivo
            )
                .onSuccess { fichajeActualizado ->
                    val fichajesActualizados = _estadoUi.value.fichajes.map {
                        if (it.idFichaje == fichajeActualizado.idFichaje) fichajeActualizado else it
                    }
                    _estadoUi.update {
                        it.copy(
                            guardando = false,
                            fichajeEditando = null,
                            dialogEntrada = null,
                            dialogSalida = null,
                            dialogMotivo = "",
                            dialogErrorHoras = null,
                            fichajes = fichajesActualizados,
                            mensajeExito = MensajeUi.Recurso(R.string.modificacion_fichaje_exito)
                        )
                    }
                }
                .onFailure { error ->
                    when {
                        error is ModificarFichajeUseCase.ErrorValidacion.MotivoVacio -> {
                            _estadoUi.update { it.copy(guardando = false) }
                        }
                        error is ModificarFichajeUseCase.ErrorValidacion.HorasInvalidas -> {
                            _estadoUi.update {
                                it.copy(
                                    guardando = false,
                                    dialogErrorHoras = R.string.modificacion_fichaje_error_horas
                                )
                            }
                        }
                        error.message?.contains("409") == true -> {
                            _estadoUi.update {
                                it.copy(
                                    guardando = false,
                                    fichajeEditando = null,
                                    mensajeError = MensajeUi.Recurso(R.string.modificacion_fichaje_error_conflicto)
                                )
                            }
                        }
                        else -> {
                            _estadoUi.update {
                                it.copy(
                                    guardando = false,
                                    mensajeError = if (error is IOException) {
                                        MensajeUi.Recurso(R.string.error_conexion)
                                    } else {
                                        MensajeUi.Dinamico(error.message ?: "")
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun exitoMostrado() {
        _estadoUi.update { it.copy(mensajeExito = null) }
    }

    companion object {
        fun factory(contexto: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val fichajeRepository = FichajeRepository(
                        retrofit.create(FichajeApiService::class.java)
                    )
                    val supervisorEmpleadoApi = retrofit.create(SupervisorEmpleadoApi::class.java)
                    val useCase = ModificarFichajeUseCase(fichajeRepository)
                    return ModificacionFichajesViewModel(
                        fichajeRepository = fichajeRepository,
                        modificarFichajeUseCase = useCase,
                        supervisorEmpleadoApi = supervisorEmpleadoApi,
                        sessionManager = sessionManager
                    ) as T
                }
            }
    }
}