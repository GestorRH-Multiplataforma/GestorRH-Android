package com.gestorrh.android.ui.equipo.ausencias

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.archivos.GestorArchivosJustificante
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.ausencia.AusenciaApiService
import com.gestorrh.android.data.network.ausencia.EstadoAusencia
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.data.repository.ausencia.AusenciaRepositoryImpl
import com.gestorrh.android.domain.repository.IAusenciaRepository
import com.gestorrh.android.domain.usecase.supervisor.ObtenerAusenciasEquipoUseCase
import com.gestorrh.android.domain.usecase.supervisor.RevisarAusenciaUseCase
import com.gestorrh.android.ui.ausencia.JustificanteParaAbrir
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel de la pantalla de validación de ausencias del equipo para el rol SUPERVISOR.
 *
 * Responsabilidades:
 * - Carga las ausencias del departamento filtrando por [EstadoAusencia], mostrando
 *   por defecto las solicitudes pendientes ([EstadoAusencia.SOLICITADA]) al entrar.
 * - Gestiona el cambio de filtro recargando la lista desde el servidor.
 * - Aplica actualización optimista tanto en aprobación como en rechazo: el item
 *   se actualiza en la lista local de forma inmediata y se revierte si la llamada
 *   de red falla, mostrando el error en un Snackbar.
 * - Gestiona el estado del diálogo de rechazo mediante [EstadoUiAusenciasEquipo.ausenciaARechazar].
 * - Descarga justificantes con el mismo patrón que [com.gestorrh.android.ui.ausencia.MisAusenciasViewModel],
 *   delegando en [GestorArchivosJustificante] para el caché y apertura con el visor del sistema.
 *
 * @property obtenerAusenciasEquipoUseCase Caso de uso para listar ausencias del departamento.
 * @property revisarAusenciaUseCase Caso de uso que valida y ejecuta la aprobación o rechazo.
 * @property ausenciaRepository Repositorio usado directamente para la descarga de justificantes.
 * @property contextoAplicacion Contexto de aplicación necesario para [GestorArchivosJustificante].
 */
class AusenciasEquipoViewModel(
    private val obtenerAusenciasEquipoUseCase: ObtenerAusenciasEquipoUseCase,
    private val revisarAusenciaUseCase: RevisarAusenciaUseCase,
    private val ausenciaRepository: IAusenciaRepository,
    private val contextoAplicacion: Context
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiAusenciasEquipo())
    val estadoUi: StateFlow<EstadoUiAusenciasEquipo> = _estadoUi.asStateFlow()

    init {
        cargarAusencias()
    }

    fun cargarAusencias() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, mensajeError = null) }
            obtenerAusenciasEquipoUseCase(_estadoUi.value.filtroActivo)
                .onSuccess { lista ->
                    val listaOrdenada = lista
                        .sortedWith(
                            compareBy<RespuestaAusenciaDTO> { it.nombreCompletoEmpleado ?: "" }
                                .thenByDescending { it.fechaInicio }
                        )
                    _estadoUi.update { it.copy(cargando = false, ausencias = listaOrdenada) }
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

    fun cambiarFiltro(estado: EstadoAusencia?) {
        _estadoUi.update { it.copy(filtroActivo = estado) }
        cargarAusencias()
    }

    fun aprobarAusencia(id: Long, observaciones: String? = null) {
        if (_estadoUi.value.revisando != null) return
        val listaOriginal = _estadoUi.value.ausencias
        _estadoUi.update { estado ->
            estado.copy(
                revisando = id,
                ausencias = estado.ausencias.map { ausencia ->
                    if (ausencia.idAusencia == id) {
                        ausencia.copy(estado = EstadoAusencia.APROBADA)
                    } else {
                        ausencia
                    }
                }
            )
        }
        viewModelScope.launch {
            revisarAusenciaUseCase(id, EstadoAusencia.APROBADA, observaciones)
                .onSuccess { actualizada ->
                    _estadoUi.update { estado ->
                        estado.copy(
                            revisando = null,
                            ausencias = estado.ausencias.map { ausencia ->
                                if (ausencia.idAusencia == id) actualizada else ausencia
                            },
                            mensajeExito = MensajeUi.Recurso(R.string.ausencias_equipo_aprobada_ok)
                        )
                    }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            revisando = null,
                            ausencias = listaOriginal,
                            mensajeError = mensajeDesdeError(error)
                        )
                    }
                }
        }
    }

    fun iniciarAprobacion(ausencia: RespuestaAusenciaDTO) {
        _estadoUi.update { it.copy(ausenciaAAprobar = ausencia) }
    }

    fun cerrarDialogAprobacion() {
        _estadoUi.update { it.copy(ausenciaAAprobar = null) }
    }

    fun iniciarRechazo(ausencia: RespuestaAusenciaDTO) {
        _estadoUi.update { it.copy(ausenciaARechazar = ausencia) }
    }

    fun confirmarRechazo(observaciones: String?) {
        val ausencia = _estadoUi.value.ausenciaARechazar ?: return
        if (_estadoUi.value.revisando != null) return
        val id = ausencia.idAusencia
        val listaOriginal = _estadoUi.value.ausencias
        _estadoUi.update { estado ->
            estado.copy(
                revisando = id,
                ausenciaARechazar = null,
                ausencias = estado.ausencias.map {
                    if (it.idAusencia == id) it.copy(estado = EstadoAusencia.RECHAZADA) else it
                }
            )
        }
        viewModelScope.launch {
            revisarAusenciaUseCase(id, EstadoAusencia.RECHAZADA, observaciones)
                .onSuccess { actualizada ->
                    _estadoUi.update { estado ->
                        estado.copy(
                            revisando = null,
                            ausencias = estado.ausencias.map {
                                if (it.idAusencia == id) actualizada else it
                            },
                            mensajeExito = MensajeUi.Recurso(R.string.ausencias_equipo_rechazada_ok)
                        )
                    }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            revisando = null,
                            ausencias = listaOriginal,
                            mensajeError = mensajeDesdeError(error)
                        )
                    }
                }
        }
    }

    fun cerrarDialogRechazo() {
        _estadoUi.update { it.copy(ausenciaARechazar = null) }
    }

    fun descargarJustificante(idAusencia: Long, nombreArchivo: String) {
        if (_estadoUi.value.descargandoJustificanteDe != null) return
        viewModelScope.launch {
            _estadoUi.update { it.copy(descargandoJustificanteDe = idAusencia) }
            ausenciaRepository.descargarJustificante(nombreArchivo)
                .onSuccess { bytes ->
                    val uri = GestorArchivosJustificante.guardarEnCacheYObtenerUri(
                        contextoAplicacion, bytes, nombreArchivo
                    )
                    _estadoUi.update {
                        it.copy(
                            descargandoJustificanteDe = null,
                            abrirJustificante = JustificanteParaAbrir(uri, nombreArchivo)
                        )
                    }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            descargandoJustificanteDe = null,
                            mensajeError = mensajeDesdeError(error)
                        )
                    }
                }
        }
    }

    fun aperturaJustificanteConsumida() {
        _estadoUi.update { it.copy(abrirJustificante = null) }
    }

    fun notificarSinVisorDisponible() {
        _estadoUi.update {
            it.copy(mensajeError = MensajeUi.Recurso(R.string.ausencia_error_sin_visor))
        }
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun exitoMostrado() {
        _estadoUi.update { it.copy(mensajeExito = null) }
    }

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
                    val apiService = retrofit.create(AusenciaApiService::class.java)
                    val repository = AusenciaRepositoryImpl(apiService, Gson())
                    val obtenerUseCase = ObtenerAusenciasEquipoUseCase(repository)
                    val revisarUseCase = RevisarAusenciaUseCase(repository)
                    return AusenciasEquipoViewModel(
                        obtenerAusenciasEquipoUseCase = obtenerUseCase,
                        revisarAusenciaUseCase = revisarUseCase,
                        ausenciaRepository = repository,
                        contextoAplicacion = contexto.applicationContext
                    ) as T
                }
            }
    }
}