package com.gestorrh.android.ui.turnos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.local.GestorRhDatabase
import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.data.repository.asignacion.AsignacionRepositoryImpl
import com.gestorrh.android.domain.repository.IAsignacionRepository
import com.gestorrh.android.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel para la pantalla de Mis Turnos.
 *
 * Expone un único [StateFlow] de [EstadoUiMisTurnos]. La lista de asignaciones procede
 * siempre del `Flow` reactivo de Room; la red se usa únicamente para refrescar la caché.
 * Esto garantiza el comportamiento offline-first: al abrir la pantalla el usuario ve
 * de inmediato los datos guardados, y la UI se actualiza sola cuando termina la sincronización.
 *
 * @param asignacionRepository Repositorio que provee el flujo de asignaciones y lanza
 *        la sincronización con el servidor.
 */
class MisTurnosViewModel(
    private val asignacionRepository: IAsignacionRepository
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiMisTurnos())
    val estadoUi: StateFlow<EstadoUiMisTurnos> = _estadoUi.asStateFlow()

    init {
        observarCache()
    }

    private fun observarCache() {
        viewModelScope.launch {
            asignacionRepository.observarAsignaciones().collect { lista ->
                _estadoUi.update { it.copy(asignaciones = lista) }
            }
        }
    }

    fun cargarAsignaciones() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, mensajeError = null) }

            when (val resultado = asignacionRepository.sincronizar()) {
                is ResultadoSincronizacion.Exito -> {
                    _estadoUi.update { it.copy(cargando = false, sinConexion = false) }
                }
                is ResultadoSincronizacion.SinConexion -> {
                    val hayCache = _estadoUi.value.asignaciones.isNotEmpty()
                    _estadoUi.update {
                        it.copy(
                            cargando = false,
                            sinConexion = hayCache,
                            mensajeError = if (!hayCache) MensajeUi.Recurso(R.string.error_conexion) else null
                        )
                    }
                }
                is ResultadoSincronizacion.Error -> {
                    _estadoUi.update {
                        it.copy(
                            cargando = false,
                            sinConexion = false,
                            mensajeError = MensajeUi.Dinamico(resultado.mensaje)
                        )
                    }
                }
            }
        }
    }

    fun seleccionarDia(fecha: LocalDate) {
        _estadoUi.update { it.copy(diaSeleccionado = fecha) }
    }

    fun cambiarVista(vista: VistaActual) {
        _estadoUi.update { it.copy(vistaActual = vista) }
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun avisoSinConexionMostrado() {
        _estadoUi.update { it.copy(sinConexion = false) }
    }

    companion object {
        fun factory(contexto: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val apiService = retrofit.create(AsignacionApiService::class.java)
                    val dao = GestorRhDatabase.getInstance(contexto).asignacionDao()
                    val repository = AsignacionRepositoryImpl(apiService, dao)
                    return MisTurnosViewModel(repository) as T
                }
            }
    }
}
