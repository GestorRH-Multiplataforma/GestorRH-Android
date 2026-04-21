package com.gestorrh.android.ui.ausencia

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
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

/**
 * ViewModel del listado "Mis ausencias".
 *
 * Expone un único [StateFlow] de [EstadoUiMisAusencias] con el listado de solicitudes
 * del empleado autenticado leído desde `GET /api/ausencias/me`. La pantalla invoca
 * [cargarAusencias] tanto al producirse el evento `Lifecycle.Event.ON_RESUME` (para
 * refrescar al regresar de la pantalla de solicitud de P1-03) como al gesto de
 * pull-to-refresh.
 *
 * La petición se ejecuta en `Dispatchers.IO` dentro del repositorio, por lo que el
 * ViewModel solo orquesta el lanzamiento en [viewModelScope] y mantiene el flag
 * `cargando` para que la UI pueda distinguir entre carga inicial y refresco manual.
 *
 * @param ausenciaRepository Acceso al endpoint de listado de ausencias propias.
 */
class MisAusenciasViewModel(
    private val ausenciaRepository: IAusenciaRepository
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

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    /**
     * Ejecuta `DELETE /api/ausencias/{id}` para cancelar la solicitud indicada.
     * En éxito marca el flag [EstadoUiMisAusencias.cancelacionExitosa] para que la
     * pantalla muestre el Snackbar y recarga el listado. En error se rellena
     * `mensajeError` con el texto que haya devuelto el servidor.
     */
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

    companion object {
        fun factory(contexto: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val apiService = retrofit.create(AusenciaApiService::class.java)
                    val repository = AusenciaRepositoryImpl(apiService, Gson())
                    return MisAusenciasViewModel(repository) as T
                }
            }
    }
}
