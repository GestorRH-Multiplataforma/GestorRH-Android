package com.gestorrh.android.ui.dashboard

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.network.hayConexion
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.local.GestorRhDatabase
import com.gestorrh.android.data.local.dao.FichajePendienteDao
import com.gestorrh.android.data.local.entity.FichajePendienteEntity
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.ModalidadTurno
import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import com.gestorrh.android.data.repository.FichajeRepository
import com.gestorrh.android.data.sync.FichajeSyncManager
import com.gestorrh.android.domain.repository.IFichajeRepository
import com.gestorrh.android.domain.usecase.fichaje.GuardarFichajePendienteUseCase
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
    val mensajeInfo: MensajeUi? = null,
    val idFichajeAbierto: Long? = null,
    val modalidadHoy: ModalidadTurno? = null,
    val tieneTurnoHoy: Boolean = false,
    /** Nombre completo del empleado autenticado, leído desde [SessionManager] sin llamadas de red. */
    val nombreEmpleado: String = "",
    /** Número de fichajes en la cola offline a la espera de ser sincronizados por `SyncFichajeWorker`. */
    val fichajesPendientesSincronizar: Int = 0
)

enum class EstadoFichaje {
    TRABAJANDO, FUERA_TURNO
}

class DashboardViewModel(
    private val fichajeRepository: IFichajeRepository,
    private val sessionManager: SessionManager,
    private val fichajePendienteDao: FichajePendienteDao,
    private val guardarFichajePendienteUseCase: GuardarFichajePendienteUseCase,
    private val contextoAplicacion: Context
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiDashboard())
    val estadoUi: StateFlow<EstadoUiDashboard> = _estadoUi.asStateFlow()

    private var temporizadorJob: Job? = null
    private var segundosAcumulados: Long = 0

    init {
        val nombre = sessionManager.getNombre() ?: ""
        _estadoUi.update { it.copy(nombreEmpleado = nombre) }

        sincronizarEstado()
        cargarFichajesPendientes()
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
     * Cuenta los fichajes que esperan ser reenviados por `SyncFichajeWorker` y actualiza
     * el estado para que la UI pueda mostrar el badge sobre el botón de fichaje.
     */
    fun cargarFichajesPendientes() {
        viewModelScope.launch {
            val pendientes = fichajePendienteDao.contarPendientes()
            _estadoUi.update { it.copy(fichajesPendientesSincronizar = pendientes) }
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

            val latitud: Double?
            val longitud: Double?
            if (modalidadHoy == ModalidadTurno.TELETRABAJO) {
                latitud = null
                longitud = null
            } else {
                latitud = ubicacion?.latitude
                longitud = ubicacion?.longitude
            }

            if (!contextoAplicacion.hayConexion()) {
                // Si ya existe una entrada pendiente, impedimos duplicar: el empleado
                // no puede haber iniciado dos jornadas simultáneas offline.
                val entradasPendientes = fichajePendienteDao.contarPorTipo(
                    FichajePendienteEntity.TIPO_ENTRADA
                )
                if (entradasPendientes > 0) {
                    mostrarError(MensajeUi.Recurso(R.string.fichaje_entrada_duplicada_offline))
                    _estadoUi.update { it.copy(estaCargando = false) }
                    return@launch
                }
                guardarOfflineYEncolar(FichajePendienteEntity.TIPO_ENTRADA, latitud, longitud)
                _estadoUi.update { it.copy(estaCargando = false) }
                return@launch
            }

            val peticion = PeticionFichajeEntradaDTO(latitud = latitud, longitud = longitud)

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
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }

            val latitud = ubicacion?.latitude
            val longitud = ubicacion?.longitude

            if (!contextoAplicacion.hayConexion()) {
                guardarOfflineYEncolar(FichajePendienteEntity.TIPO_SALIDA, latitud, longitud)
                _estadoUi.update { it.copy(estaCargando = false) }
                return@launch
            }

            if (idFichaje == null) {
                _estadoUi.update { it.copy(estaCargando = false) }
                return@launch
            }

            val peticion = PeticionFichajeSalidaDTO(latitud = latitud, longitud = longitud)

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

    private suspend fun guardarOfflineYEncolar(
        tipo: String,
        latitud: Double?,
        longitud: Double?
    ) {
        guardarFichajePendienteUseCase(tipo, latitud, longitud)
            .onSuccess {
                FichajeSyncManager.encolarSincronizacion(contextoAplicacion)
                _estadoUi.update {
                    it.copy(
                        mensajeInfo = MensajeUi.Recurso(R.string.fichaje_sin_conexion),
                        fichajesPendientesSincronizar = it.fichajesPendientesSincronizar + 1
                    )
                }
            }
            .onFailure { e ->
                val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                else MensajeUi.Recurso(R.string.error_desconocido)
                mostrarError(mensaje)
            }
    }

    // ── FUNCIONES AUXILIARES ──────────────────────────────────────────────────

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun infoMostrado() {
        _estadoUi.update { it.copy(mensajeInfo = null) }
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
            val contextoAplicacion = contexto.applicationContext
            val sessionManager = SessionManager(contextoAplicacion)
            val retrofit = ApiClient.crearRetrofit(sessionManager)
            val apiService = retrofit.create(FichajeApiService::class.java)
            val fichajeRepository = FichajeRepository(apiService)
            val fichajePendienteDao = GestorRhDatabase.getInstance(contextoAplicacion).fichajePendienteDao()
            val guardarFichajePendienteUseCase = GuardarFichajePendienteUseCase(fichajePendienteDao)

            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                fichajeRepository = fichajeRepository,
                sessionManager = sessionManager,
                fichajePendienteDao = fichajePendienteDao,
                guardarFichajePendienteUseCase = guardarFichajePendienteUseCase,
                contextoAplicacion = contextoAplicacion
            ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}
