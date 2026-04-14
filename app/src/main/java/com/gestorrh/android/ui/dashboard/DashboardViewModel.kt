package com.gestorrh.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estados posibles del motor de fichaje.
 */
enum class EstadoFichaje {
    FUERA_TURNO, TRABAJANDO
}

/**
 * Estado inmutable de la pantalla Dashboard.
 */
data class EstadoUiDashboard(
    val estadoActual: EstadoFichaje = EstadoFichaje.FUERA_TURNO,
    val tiempoTranscurrido: String = "00:00:00"
)

class DashboardViewModel : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiDashboard())
    val estadoUi: StateFlow<EstadoUiDashboard> = _estadoUi.asStateFlow()

    private var temporizadorJob: Job? = null
    private var segundosTranscurridos = 0

    /**
     * Alterna el estado de fichaje simulando un Clock-in o Clock-out.
     * En la P0-10 y P0-12, aquí es donde llamaremos a la API y al GPS.
     */
    fun alternarFichaje() {
        val esFueraDeTurno = _estadoUi.value.estadoActual == EstadoFichaje.FUERA_TURNO

        if (esFueraDeTurno) {
            _estadoUi.update { it.copy(estadoActual = EstadoFichaje.TRABAJANDO) }
            iniciarTemporizador()
        } else {
            _estadoUi.update {
                it.copy(estadoActual = EstadoFichaje.FUERA_TURNO, tiempoTranscurrido = "00:00:00")
            }
            detenerTemporizador()
        }
    }

    private fun iniciarTemporizador() {
        segundosTranscurridos = 0
        temporizadorJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                segundosTranscurridos++
                formatearTiempo()
            }
        }
    }

    private fun detenerTemporizador() {
        temporizadorJob?.cancel()
        temporizadorJob = null
    }

    private fun formatearTiempo() {
        val horas = segundosTranscurridos / 3600
        val minutos = (segundosTranscurridos % 3600) / 60
        val segundos = segundosTranscurridos % 60
        val texto = String.format(Locale.getDefault(), "%02d:%02d:%02d", horas, minutos, segundos)
        _estadoUi.update { it.copy(tiempoTranscurrido = texto) }
    }
}