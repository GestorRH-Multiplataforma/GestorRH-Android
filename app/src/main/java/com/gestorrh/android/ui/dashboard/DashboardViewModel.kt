package com.gestorrh.android.ui.dashboard

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.network.hayConexion
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.local.GestorRhDatabase
import com.gestorrh.android.data.local.dao.FichajePendienteDao
import com.gestorrh.android.data.local.entity.FichajePendienteEntity
import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.data.network.ausencia.AusenciaApiService
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.ModalidadTurno
import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import com.gestorrh.android.data.repository.FichajeRepository
import com.gestorrh.android.data.repository.asignacion.AsignacionRepositoryImpl
import com.gestorrh.android.data.repository.ausencia.AusenciaRepositoryImpl
import com.gestorrh.android.data.sync.FichajeSyncManager
import com.gestorrh.android.domain.repository.IAsignacionRepository
import com.gestorrh.android.domain.repository.IAusenciaRepository
import com.gestorrh.android.domain.repository.IFichajeRepository
import com.gestorrh.android.domain.usecase.fichaje.GuardarFichajePendienteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

data class EstadoUiDashboard(
    val estadoActual: EstadoFichaje = EstadoFichaje.FUERA_TURNO,
    val tiempoTranscurrido: String = "00:00:00",
    val estaCargando: Boolean = true,
    val mensajeError: MensajeUi? = null,
    val mensajeInfo: MensajeUi? = null,
    val errorCritico: Boolean = false,
    val idFichajeAbierto: Long? = null,
    val modalidadHoy: ModalidadTurno? = null,
    val tieneTurnoHoy: Boolean = false,
    /** Nombre completo del empleado autenticado, leído desde [SessionManager] sin llamadas de red. */
    val nombreEmpleado: String = "",
    /** Número de fichajes en la cola offline a la espera de ser sincronizados por `SyncFichajeWorker`. */
    val fichajesPendientesSincronizar: Int = 0,
    /** Resumen del próximo turno asignado (hoy o futuro), o `null` si no hay ninguno. */
    val proximoTurno: ResumenProximoTurno? = null,
    /** Resumen de la próxima ausencia solicitada o aprobada, o `null` si no hay ninguna. */
    val proximaAusencia: ResumenProximaAusencia? = null,
    /** Últimos fichajes (máximo 3) de los últimos 7 días, mostrados inline en el dashboard. */
    val ultimosFichajes: List<RespuestaFichajeDTO> = emptyList()
)

/**
 * Datos crudos del próximo turno expuestos al UI para que pueda formatear
 * la fecha y el horario con los recursos localizados (`stringResource`).
 */
data class ResumenProximoTurno(
    val nombreTurno: String,
    val fecha: LocalDate,
    val horaInicio: LocalTime?,
    val horaFin: LocalTime?
)

/**
 * Datos crudos de la próxima ausencia (estado `SOLICITADA` o `APROBADA`)
 * para que el UI traduzca tipo y estado a recursos.
 */
data class ResumenProximaAusencia(
    val tipo: String,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    val estado: String
)

enum class EstadoFichaje {
    TRABAJANDO,
    TURNO_EN_CURSO_SIN_FICHAR,
    TURNO_PENDIENTE,
    FUERA_TURNO
}

/**
 * ViewModel de la pantalla principal del empleado.
 *
 * Centraliza el estado del fichaje en curso (cronómetro, modalidad del turno,
 * último resultado de la API), del próximo turno y de la próxima ausencia.
 * Coordina las peticiones online con la persistencia local de fichajes
 * pendientes cuando el dispositivo carece de conexión, garantizando que
 * el empleado siempre pueda registrar su jornada.
 *
 * @property fichajeRepository Acceso a las operaciones de fichaje contra la API.
 * @property sessionManager Fuente de la identidad del empleado autenticado.
 * @property fichajePendienteDao DAO Room para fichajes acumulados sin conexión.
 * @property guardarFichajePendienteUseCase Caso de uso que valida y persiste un fichaje pendiente.
 * @property asignacionRepository Acceso al cuadrante de turnos del empleado.
 * @property ausenciaRepository Acceso a la lista de ausencias del empleado.
 * @property contextoAplicacion Contexto de aplicación necesario para chequear conectividad.
 */
class DashboardViewModel(
    private val fichajeRepository: IFichajeRepository,
    private val sessionManager: SessionManager,
    private val fichajePendienteDao: FichajePendienteDao,
    private val guardarFichajePendienteUseCase: GuardarFichajePendienteUseCase,
    private val asignacionRepository: IAsignacionRepository,
    private val ausenciaRepository: IAusenciaRepository,
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
        cargarProximoTurno()
        cargarProximaAusencia()
        cargarUltimosFichajes()
    }

    /**
     * Recupera los últimos fichajes del empleado en los 7 días previos para mostrarlos
     * inline en el dashboard. Limitado a 3 entradas, ordenados por hora de entrada
     * descendente. Los errores se silencian: si la petición falla, la sección
     * simplemente queda vacía y el dashboard sigue siendo usable.
     */
    fun cargarUltimosFichajes() {
        viewModelScope.launch {
            val hoy = LocalDate.now()
            val hace7Dias = hoy.minusDays(7)
            fichajeRepository.obtenerHistorialFichajes(hace7Dias, hoy)
                .onSuccess { fichajes ->
                    val ultimos = fichajes
                        .sortedByDescending { it.horaEntrada }
                        .take(3)
                    _estadoUi.update { it.copy(ultimosFichajes = ultimos) }
                }
        }
    }

    /**
     * Recupera la asignación más cercana en el futuro (incluido hoy) usando el flujo
     * reactivo de la caché Room — la sincronización contra el backend se delega al
     * `Repository`, que actualizará el flujo cuando termine. Si no hay asignaciones
     * futuras o falla la lectura, el estado expuesto queda en `null`.
     */
    fun cargarProximoTurno() {
        viewModelScope.launch {
            asignacionRepository.sincronizar()
            val asignaciones = asignacionRepository.observarAsignaciones().firstOrNull()
                ?: return@launch
            val hoy = LocalDate.now()
            val ahora = LocalTime.now()
            val proxima = asignaciones
                .mapNotNull { entidad ->
                    val fecha = parsearFecha(entidad.fecha) ?: return@mapNotNull null
                    if (fecha < hoy) return@mapNotNull null
                    val horaFin = parsearHora(entidad.horaFin)
                    if (fecha == hoy && horaFin != null && ahora.isAfter(horaFin)) return@mapNotNull null
                    ResumenProximoTurno(
                        nombreTurno = entidad.descripcionTurno,
                        fecha = fecha,
                        horaInicio = parsearHora(entidad.horaInicio),
                        horaFin = horaFin
                    )
                }
                .minByOrNull { it.fecha }
            _estadoUi.update { it.copy(proximoTurno = proxima) }
        }
    }

    /**
     * Recupera la ausencia futura más cercana en estado `SOLICITADA` o `APROBADA`.
     * Las ausencias rechazadas o ya finalizadas se ignoran para que la tarjeta solo
     * informe de algo accionable. Los errores se silencian: la tarjeta cae a su
     * estado vacío en lugar de generar ruido en el dashboard.
     */
    fun cargarProximaAusencia() {
        viewModelScope.launch {
            val resultado = ausenciaRepository.obtenerMisAusencias(estado = null)
            val ausencias = resultado.getOrNull() ?: return@launch
            val hoy = LocalDate.now()
            val proxima = ausencias
                .filter { it.estado == ESTADO_AUSENCIA_SOLICITADA || it.estado == ESTADO_AUSENCIA_APROBADA }
                .filter { !it.fechaFin.isBefore(hoy) }
                .minByOrNull { it.fechaInicio }
                ?.let { dto ->
                    ResumenProximaAusencia(
                        tipo = dto.tipo,
                        fechaInicio = dto.fechaInicio,
                        fechaFin = dto.fechaFin,
                        estado = dto.estado
                    )
                }
            _estadoUi.update { it.copy(proximaAusencia = proxima) }
        }
    }

    private fun parsearFecha(valor: String): LocalDate? = try {
        LocalDate.parse(valor)
    } catch (e: DateTimeParseException) {
        null
    }

    private fun parsearHora(valor: String?): LocalTime? {
        if (valor.isNullOrBlank()) return null
        return try {
            LocalTime.parse(valor)
        } catch (e: DateTimeParseException) {
            null
        }
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
                            estadoActual = calcularEstadoFichaje(
                                trabajandoActualmente = datos.trabajandoActualmente,
                                tieneTurnoHoy = datos.tieneTurnoHoy,
                                proximoTurno = _estadoUi.value.proximoTurno
                            )
                        )
                    }

                    if (datos.trabajandoActualmente && datos.horaEntrada != null) {
                        val horaEntradaUtc = datos.horaEntrada.toInstant(java.time.ZoneOffset.UTC)
                        val segundos = ChronoUnit.SECONDS.between(horaEntradaUtc, java.time.Instant.now())
                        segundosAcumulados = segundos.coerceAtLeast(0L)
                        iniciarTemporizador()
                    } else if (!datos.trabajandoActualmente) {
                        detenerTemporizador()
                        segundosAcumulados = 0
                    }
                }
                .onFailure { e ->
                    val hayEstadoConocido = _estadoUi.value.idFichajeAbierto != null
                    if (hayEstadoConocido) {
                        val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                        else MensajeUi.Recurso(R.string.error_conexion)
                        mostrarError(mensaje)
                    } else {
                        _estadoUi.update { it.copy(errorCritico = true) }
                    }
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

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun infoMostrado() {
        _estadoUi.update { it.copy(mensajeInfo = null) }
    }

    fun reintentarTrasErrorCritico() {
        _estadoUi.update { it.copy(errorCritico = false) }
        sincronizarEstado()
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

    private fun calcularEstadoFichaje(
        trabajandoActualmente: Boolean,
        tieneTurnoHoy: Boolean,
        proximoTurno: ResumenProximoTurno?
    ): EstadoFichaje {
        if (trabajandoActualmente) return EstadoFichaje.TRABAJANDO
        if (!tieneTurnoHoy || proximoTurno == null) return EstadoFichaje.FUERA_TURNO

        val ahora = LocalTime.now()
        val horaInicio = proximoTurno.horaInicio
        val horaFin = proximoTurno.horaFin

        return when {
            horaInicio != null && ahora.isBefore(horaInicio) -> EstadoFichaje.TURNO_PENDIENTE
            horaFin != null && ahora.isAfter(horaFin) -> EstadoFichaje.FUERA_TURNO
            else -> EstadoFichaje.TURNO_EN_CURSO_SIN_FICHAR
        }
    }

    private fun formatearTiempo(segundosTotales: Long): String {
        val horas = segundosTotales / 3600
        val minutos = (segundosTotales % 3600) / 60
        val segundos = segundosTotales % 60
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", horas, minutos, segundos)
    }

    companion object {
        private const val ESTADO_AUSENCIA_SOLICITADA = "SOLICITADA"
        private const val ESTADO_AUSENCIA_APROBADA = "APROBADA"
    }
}

/**
 * Factory manual para [DashboardViewModel]. Resuelve dependencias a partir
 * del contexto de aplicación pasado por la pantalla.
 */
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

            val asignacionApi = retrofit.create(AsignacionApiService::class.java)
            val asignacionDao = GestorRhDatabase.getInstance(contextoAplicacion).asignacionDao()
            val asignacionRepository = AsignacionRepositoryImpl(asignacionApi, asignacionDao)

            val ausenciaApi = retrofit.create(AusenciaApiService::class.java)
            val ausenciaRepository = AusenciaRepositoryImpl(ausenciaApi, Gson())

            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                fichajeRepository = fichajeRepository,
                sessionManager = sessionManager,
                fichajePendienteDao = fichajePendienteDao,
                guardarFichajePendienteUseCase = guardarFichajePendienteUseCase,
                asignacionRepository = asignacionRepository,
                ausenciaRepository = ausenciaRepository,
                contextoAplicacion = contextoAplicacion
            ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}
