package com.gestorrh.android.ui.historial

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.repository.FichajeRepository
import com.gestorrh.android.domain.usecase.fichaje.ObtenerHistorialFichajesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Manejador de estado y lógica de presentación para la pantalla de historial de
 * fichajes personales del empleado autenticado.
 *
 * Mantiene el rango de fechas seleccionado por el usuario y dispara la consulta al
 * caso de uso cada vez que se aplica el filtro. El estado se expone como un único
 * [StateFlow] con [EstadoUiHistorialFichajes] para que la UI sea puramente reactiva.
 *
 * Por defecto, en la primera carga, se solicitan los fichajes del último mes
 * (hoy menos 30 días → hoy).
 *
 * @property obtenerHistorialFichajesUseCase Caso de uso que devuelve la lista ya ordenada.
 */
class HistorialFichajesViewModel(
    private val obtenerHistorialFichajesUseCase: ObtenerHistorialFichajesUseCase
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiHistorialFichajes())
    val estadoUi: StateFlow<EstadoUiHistorialFichajes> = _estadoUi.asStateFlow()

    init {
        cargarFichajes()
    }

    /**
     * Lanza la consulta al servidor con el rango actualmente seleccionado y refresca
     * la lista. Bloquea la UI durante la petición a través de [EstadoUiHistorialFichajes.cargando].
     */
    fun cargarFichajes() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, mensajeError = null) }

            val rango = _estadoUi.value
            obtenerHistorialFichajesUseCase(
                fechaInicio = rango.fechaInicio,
                fechaFin = rango.fechaFin
            )
                .onSuccess { fichajes ->
                    _estadoUi.update { it.copy(cargando = false, fichajes = fichajes) }
                }
                .onFailure { e ->
                    val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                    else MensajeUi.Recurso(R.string.error_conexion)
                    _estadoUi.update {
                        it.copy(cargando = false, mensajeError = mensaje)
                    }
                }
        }
    }

    fun actualizarFechaInicio(nuevaFecha: LocalDate) {
        _estadoUi.update { it.copy(fechaInicio = nuevaFecha) }
    }

    fun actualizarFechaFin(nuevaFecha: LocalDate) {
        _estadoUi.update { it.copy(fechaFin = nuevaFecha) }
    }

    fun aplicarFiltro() {
        cargarFichajes()
    }

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    companion object {
        fun crearFactory(contexto: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val contextoApp = contexto.applicationContext
                    val sessionManager = SessionManager(contextoApp)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val apiService = retrofit.create(FichajeApiService::class.java)
                    val repository = FichajeRepository(apiService)
                    val useCase = ObtenerHistorialFichajesUseCase(repository)
                    return HistorialFichajesViewModel(useCase) as T
                }
            }
        }
    }
}
