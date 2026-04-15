package com.gestorrh.android.ui.dashboard

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.ModalidadTurno
import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class EstadoUiDashboard(
    val estadoActual: EstadoFichaje = EstadoFichaje.FUERA_TURNO,
    val tiempoTranscurrido: String = "00:00:00",

    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val idFichajeAbierto: Long? = null,
    val modalidadHoy: ModalidadTurno? = null,
    val tieneTurnoHoy: Boolean = false
)

enum class EstadoFichaje {
    TRABAJANDO, FUERA_TURNO
}

class DashboardViewModel(
    private val apiService: FichajeApiService
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiDashboard())
    val estadoUi: StateFlow<EstadoUiDashboard> = _estadoUi.asStateFlow()

    private var temporizadorJob: Job? = null
    private var segundosAcumulados: Long = 0

    init {
        sincronizarEstado()
    }

    /**
     * P0-11: Sincronización Inicial (BFF)
     */
    fun sincronizarEstado() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }

            try {
                val respuesta = apiService.obtenerEstadoActual()

                if (respuesta.isSuccessful) {
                    val datos = respuesta.body()
                    if (datos != null) {

                        _estadoUi.update {
                            it.copy(
                                idFichajeAbierto = datos.idFichajeAbierto,
                                modalidadHoy = datos.modalidadHoy,
                                tieneTurnoHoy = datos.tieneTurnoHoy,
                                estadoActual = if (datos.trabajandoActualmente) EstadoFichaje.TRABAJANDO else EstadoFichaje.FUERA_TURNO
                            )
                        }

                        if (datos.trabajandoActualmente && datos.horaEntrada != null) {
                            val ahora = LocalDateTime.now()
                            segundosAcumulados = ChronoUnit.SECONDS.between(datos.horaEntrada, ahora)
                            iniciarTemporizador()
                        }
                    }
                } else {
                    mostrarError("No se pudo obtener el estado actual. Código: ${respuesta.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mostrarError("Error de conexión con el servidor.")
            } finally {
                _estadoUi.update { it.copy(estaCargando = false) }
            }
        }
    }

    /**
     * P0-10 y P0-12: Orquestador de Fichaje
     */
    fun alternarFichaje(ubicacion: Location?) {
        val estadoActual = _estadoUi.value

        if (estadoActual.estadoActual == EstadoFichaje.FUERA_TURNO) {
            ficharEntrada(ubicacion, estadoActual.modalidadHoy)
        } else {
            ficharSalida(ubicacion, estadoActual.idFichajeAbierto)
        }
    }

    private fun ficharEntrada(ubicacion: Location?, modalidadHoy: ModalidadTurno?) {
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }

            try {
                val peticion = if (modalidadHoy == ModalidadTurno.TELETRABAJO) {
                    PeticionFichajeEntradaDTO(latitud = null, longitud = null)
                } else {
                    PeticionFichajeEntradaDTO(latitud = ubicacion?.latitude, longitud = ubicacion?.longitude)
                }

                val respuesta = apiService.ficharEntrada(peticion)

                if (respuesta.isSuccessful) {
                    val fichaje = respuesta.body()
                    _estadoUi.update {
                        it.copy(
                            estadoActual = EstadoFichaje.TRABAJANDO,
                            idFichajeAbierto = fichaje?.idFichaje
                        )
                    }
                    segundosAcumulados = 0
                    iniciarTemporizador()
                } else {
                    mostrarError(extraerMensajeError(respuesta.errorBody()))
                }
            } catch (e: Exception) {
                mostrarError("Fallo de red al intentar fichar.")
            } finally {
                _estadoUi.update { it.copy(estaCargando = false) }
            }
        }
    }

    private fun ficharSalida(ubicacion: Location?, idFichaje: Long?) {
        if (idFichaje == null) return

        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }

            try {
                val peticion = PeticionFichajeSalidaDTO(latitud = ubicacion?.latitude, longitud = ubicacion?.longitude)
                val respuesta = apiService.ficharSalida(peticion)

                if (respuesta.isSuccessful) {
                    detenerTemporizador()
                    _estadoUi.update {
                        it.copy(
                            estadoActual = EstadoFichaje.FUERA_TURNO,
                            idFichajeAbierto = null,
                            tiempoTranscurrido = "00:00:00"
                        )
                    }
                } else {
                    mostrarError(extraerMensajeError(respuesta.errorBody()))
                }
            } catch (e: Exception) {
                mostrarError("Fallo de red al intentar finalizar la jornada.")
            } finally {
                _estadoUi.update { it.copy(estaCargando = false) }
            }
        }
    }

    // FUNCIONES AUXILIARES

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    private fun mostrarError(mensaje: String) {
        _estadoUi.update { it.copy(mensajeError = mensaje) }
    }

    private fun extraerMensajeError(errorBody: ResponseBody?): String {
        return try {
            val jsonStr = errorBody?.string() ?: return "Error desconocido"
            val jsonObject = JSONObject(jsonStr)
            jsonObject.getString("message")
        } catch (e: Exception) {
            "Ocurrió un error en el servidor."
        }
    }

    private fun iniciarTemporizador() {
        temporizadorJob?.cancel()
        temporizadorJob = viewModelScope.launch {
            while (true) {
                _estadoUi.update { it.copy(tiempoTranscurrido = formatearTiempo(segundosAcumulados)) }
                delay(1000L)
                segundosAcumulados++
            }
        }
    }

    private fun detenerTemporizador() {
        temporizadorJob?.cancel()
        temporizadorJob = null
        segundosAcumulados = 0
    }

    private fun formatearTiempo(segundosTotales: Long): String {
        val horas = segundosTotales / 3600
        val minutos = (segundosTotales % 3600) / 60
        val segundos = segundosTotales % 60
        return String.format("%02d:%02d:%02d", horas, minutos, segundos)
    }
}

// FACTORY MANUAL
class DashboardViewModelFactory(private val contexto: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val tokenManager = com.gestorrh.android.core.security.TokenManager(contexto)
            val retrofit = ApiClient.crearRetrofit(tokenManager)
            val apiService = retrofit.create(FichajeApiService::class.java)

            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(apiService) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}