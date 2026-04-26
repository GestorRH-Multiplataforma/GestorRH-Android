package com.gestorrh.android.ui.ausencia

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
import com.gestorrh.android.data.repository.ausencia.AusenciaRepositoryImpl
import com.gestorrh.android.domain.repository.IAusenciaRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * ViewModel del listado "Mis ausencias".
 *
 * Expone un único [StateFlow] de [EstadoUiMisAusencias] con el listado de solicitudes
 * del empleado autenticado leído desde `GET /api/ausencias/me`. La pantalla invoca
 * [cargarAusencias] tanto al producirse el evento `Lifecycle.Event.ON_RESUME` (para
 * refrescar al regresar de la pantalla de solicitud de P1-03) como al gesto de
 * pull-to-refresh.
 *
 * Además gestiona la descarga del justificante asociado a una ausencia para abrirlo
 * con el visor del sistema del dispositivo desde la propia tarjeta del listado.
 */
class MisAusenciasViewModel(
    private val ausenciaRepository: IAusenciaRepository,
    private val contextoAplicacion: Context
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiMisAusencias())
    val estadoUi: StateFlow<EstadoUiMisAusencias> = _estadoUi.asStateFlow()

    fun cargarAusencias() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, mensajeError = null) }
            ausenciaRepository.obtenerMisAusencias()
                .onSuccess { lista ->
                    _estadoUi.update { it.copy(cargando = false, ausencias = lista) }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            cargando = false,
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

    /**
     * Inicia un bucle de refresco periódico cada [INTERVALO_POLLING_MS] ms.
     *
     * Se vincula al [Lifecycle] de la pantalla mediante [repeatOnLifecycle]:
     * el polling se pausa automáticamente cuando la app va a segundo plano
     * (estado STARTED → STOPPED) y se reanuda al volver al primer plano,
     * sin necesidad de gestión manual en la UI.
     *
     * La primera carga la realiza [cargarAusencias] desde la pantalla en
     * ON_RESUME, por lo que aquí esperamos el intervalo completo antes del
     * primer tick para no duplicar la petición inicial.
     */
    fun iniciarPolling(lifecycle: Lifecycle) {
        viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(INTERVALO_POLLING_MS)
                while (true) {
                    cargarAusencias()
                    delay(INTERVALO_POLLING_MS)
                }
            }
        }
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun cancelarAusencia(idAusencia: Long) {
        if (_estadoUi.value.cancelando) return
        viewModelScope.launch {
            _estadoUi.update { it.copy(cancelando = true, mensajeError = null) }
            ausenciaRepository.cancelarAusencia(idAusencia)
                .onSuccess {
                    _estadoUi.update { it.copy(cancelando = false, cancelacionExitosa = true) }
                    cargarAusencias()
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            cancelando = false,
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

    fun cancelacionConsumida() {
        _estadoUi.update { it.copy(cancelacionExitosa = false) }
    }

    /**
     * Descarga el justificante de la ausencia indicada y guarda los bytes en el
     * caché del dispositivo, exponiendo el `Uri` resultante vía
     * [EstadoUiMisAusencias.abrirJustificante] para que la pantalla lance el visor.
     */
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

    fun aperturaJustificanteConsumida() {
        _estadoUi.update { it.copy(abrirJustificante = null) }
    }

    /**
     * Notifica que el sistema no ha podido abrir el justificante porque no hay
     * ninguna aplicación instalada capaz de gestionar su tipo MIME. Se muestra
     * un mensaje en el Snackbar reutilizando el canal de errores.
     */
    fun notificarSinVisorDisponible() {
        _estadoUi.update {
            it.copy(mensajeError = MensajeUi.Recurso(R.string.ausencia_error_sin_visor))
        }
    }

    companion object {
        private const val INTERVALO_POLLING_MS = 60_000L
        fun factory(contexto: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val apiService = retrofit.create(AusenciaApiService::class.java)
                    val repository = AusenciaRepositoryImpl(apiService, Gson())
                    return MisAusenciasViewModel(repository, contexto) as T
                }
            }
    }
}
