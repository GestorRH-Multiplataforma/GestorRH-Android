package com.gestorrh.android.ui.turnos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.data.repository.asignacion.AsignacionRepositoryImpl
import com.gestorrh.android.domain.repository.IAsignacionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para la pantalla de Mis Turnos.
 * Expone un único [StateFlow] de [EstadoUiMisTurnos] y orquesta la carga de asignaciones
 * desde el repositorio ejecutándola en [Dispatchers.IO].
 *
 * @param asignacionRepository Repositorio que provee las asignaciones del empleado autenticado.
 */
class MisTurnosViewModel(
    private val asignacionRepository: IAsignacionRepository
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiMisTurnos())
    val estadoUi: StateFlow<EstadoUiMisTurnos> = _estadoUi.asStateFlow()

    init {
        cargarAsignaciones()
    }

    fun cargarAsignaciones() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, mensajeError = null) }

            val resultado = withContext(Dispatchers.IO) {
                asignacionRepository.getMisAsignaciones()
            }

            resultado
                .onSuccess { asignaciones ->
                    val ordenadas = asignaciones.sortedBy { it.fecha }
                    _estadoUi.update { it.copy(cargando = false, asignaciones = ordenadas) }
                }
                .onFailure { error ->
                    val mensaje = if (error.message != null) MensajeUi.Dinamico(error.message!!)
                    else MensajeUi.Recurso(com.gestorrh.android.R.string.error_conexion)
                    _estadoUi.update { it.copy(cargando = false, mensajeError = mensaje) }
                }
        }
    }

    fun cambiarVista(vista: VistaActual) {
        _estadoUi.update { it.copy(vistaActual = vista) }
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    companion object {
        fun factory(contexto: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val apiService = retrofit.create(AsignacionApiService::class.java)
                    val repository = AsignacionRepositoryImpl(apiService)
                    return MisTurnosViewModel(repository) as T
                }
            }
    }
}
