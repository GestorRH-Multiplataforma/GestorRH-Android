package com.gestorrh.android.ui.dashboard

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.ModalidadTurno
import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import com.gestorrh.android.data.repository.FichajeRepository
import com.gestorrh.android.domain.repository.IFichajeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class EstadoUiDashboard(
    val estadoActual: EstadoFichaje = EstadoFichaje.FUERA_TURNO,
    val tiempoTranscurrido: String = "00:00:00",
    val estaCargando: Boolean = true,
    val mensajeError: MensajeUi? = null,
    val idFichajeAbierto: Long? = null,
    val modalidadHoy: ModalidadTurno? = null,
    val tieneTurnoHoy: Boolean = false,
    /** Nombre completo del empleado autenticado, leído desde [SessionManager] sin llamadas de red. */
    val nombreEmpleado: String = ""
)

enum class EstadoFichaje {
    TRABAJANDO, FUERA_TURNO
}

class DashboardViewModel(
    private val fichajeRepository: IFichajeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiDashboard())
    val estadoUi: StateFlow<EstadoUiDashboard> = _estadoUi.asStateFlow()

    private var temporizadorJob: Job? = null
    private var segundosAcumulados: Long = 0

    init {
        // Carga el nombre desde el almacenamiento seguro sin llamada de red
        val nombre = sessionManager.getNombre() ?: ""
        _estadoUi.update { it.copy(nombreEmpleado = nombre) }

        sincronizarEstado()
    }

    /**
     * P0-11: Sincronización Inicial (BFF).
     * Consulta el estado de fichaje actual a través del repositorio.
     */
    fun sincronizarEstado() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }

            fichajeRepository.obtenerEstadoActual()
                .onSuccess { datos ->
                    _estadoUi.update {
                        it.copy(
                            idFichajeAbierto = datos.idFichajeAbierto,
                            modalidadHoy = datos.modalidadHoy,
                            tieneTurnoHoy = datos.tieneTurnoHoy,
                            estadoActual = if (datos.trabajandoActualmente) EstadoFichaje.TRABAJANDO else EstadoFichaje.FUERA_TURNO
                        )
                    }

                    if (datos.trabajandoActualmente && datos.horaEntrada != null) {
                        segundosAcumulados = ChronoUnit.SECONDS.between(datos.horaEntrada, LocalDateTime.now())
                        iniciarTemporizador()
                    }
                }
                .onFailure { e ->
                    val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                    else MensajeUi.Recurso(R.string.error_conexion)
                    mostrarError(mensaje)
                }

            _estadoUi.update { it.copy(estaCargando = false) }
        }
    }

    /**
     * P0-10 y P0-12: Orquestador de Fichaje.
     * Decide si fichar entrada o salida según el estado actual.
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

            val peticion = if (modalidadHoy == ModalidadTurno.TELETRABAJO) {
                PeticionFichajeEntradaDTO(latitud = null, longitud = null)
            } else {
                PeticionFichajeEntradaDTO(latitud = ubicacion?.latitude, longitud = ubicacion?.longitude)
            }

            fichajeRepository.ficharEntrada(peticion)
                .onSuccess { fichaje ->
                    _estadoUi.update {
                        it.copy(
                            estadoActual = EstadoFichaje.TRABAJANDO,
                            idFichajeAbierto = fichaje.idFichaje
                        )
                    }
                    segundosAcumulados = 0
                    iniciarTemporizador()
                }
                .onFailure { e ->
                    val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                    else MensajeUi.Recurso(R.string.error_fallo_red_entrada)
                    mostrarError(mensaje)
                }

            _estadoUi.update { it.copy(estaCargando = false) }
        }
    }

    private fun ficharSalida(ubicacion: Location?, idFichaje: Long?) {
        if (idFichaje == null) return

        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }

            val peticion = PeticionFichajeSalidaDTO(
                latitud = ubicacion?.latitude,
                longitud = ubicacion?.longitude
            )

            fichajeRepository.ficharSalida(peticion)
                .onSuccess {
                    detenerTemporizador()
                    _estadoUi.update {
                        it.copy(
                            estadoActual = EstadoFichaje.FUERA_TURNO,
                            idFichajeAbierto = null,
                            tiempoTranscurrido = "00:00:00"
                        )
                    }
                }
                .onFailure { e ->
                    val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                    else MensajeUi.Recurso(R.string.error_fallo_red_salida)
                    mostrarError(mensaje)
                }

            _estadoUi.update { it.copy(estaCargando = false) }
        }
    }

    // ── FUNCIONES AUXILIARES ──────────────────────────────────────────────────

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    private fun mostrarError(mensaje: MensajeUi) {
        _estadoUi.update { it.copy(mensajeError = mensaje) }
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

// ── FACTORY MANUAL ────────────────────────────────────────────────────────────

class DashboardViewModelFactory(private val contexto: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val sessionManager = SessionManager(contexto)
            val retrofit = ApiClient.crearRetrofit(sessionManager)
            val apiService = retrofit.create(FichajeApiService::class.java)
            val fichajeRepository = FichajeRepository(apiService)

            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(fichajeRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}
