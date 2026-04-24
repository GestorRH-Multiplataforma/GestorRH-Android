package com.gestorrh.android.ui.ausencia

import android.content.Context
import android.net.Uri
import android.util.Log
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
import com.gestorrh.android.domain.usecase.ausencia.SolicitarAusenciaUseCase
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel del formulario de solicitud de ausencia.
 *
 * Mantiene en un único [StateFlow] de [EstadoUiSolicitudAusencia] todo el estado
 * del formulario, incluida la lista de tipos cargada desde `GET /api/ausencias/tipos`,
 * los valores actuales de los campos, los errores inline por campo y el resultado del
 * envío. La pantalla solo emite eventos: la lógica de validación, la conversión del
 * `Uri` adjunto a bytes (con compresión de imagen cuando procede) y la construcción
 * del `multipart` viven aquí o en el UseCase.
 *
 * Bloquea reentradas en [enviar] mediante el flag `enviando` para evitar el doble envío
 * por doble clic, y traduce los errores de validación del UseCase a recursos de string
 * para que la pantalla los muestre directamente con `stringResource`.
 *
 * En modo edición se precarga el nombre del justificante ya persistido en el servidor
 * y se ofrece descargarlo, reemplazarlo por uno nuevo (seleccionando un archivo local,
 * que sustituirá al anterior al hacer `PUT`) o eliminarlo (se envía `PUT` sin parte
 * archivo y con la marca correspondiente).
 *
 * @param ausenciaRepository Acceso a los endpoints de ausencias.
 * @param solicitarAusenciaUseCase Encapsula validaciones locales y la llamada al repositorio.
 * @param contextoAplicacion Contexto de aplicación para resolver el `ContentResolver` al
 *        leer el `Uri` del justificante. Solo se usa para esta operación de I/O.
 */
class SolicitudAusenciaViewModel(
    private val ausenciaRepository: IAusenciaRepository,
    private val solicitarAusenciaUseCase: SolicitarAusenciaUseCase,
    private val contextoAplicacion: Context,
    datosPrerelleno: PrerrellenoEdicion? = null
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(
        EstadoUiSolicitudAusencia(
            idAusenciaEditar = datosPrerelleno?.idAusencia,
            tipoSeleccionado = datosPrerelleno?.tipo,
            fechaInicio = datosPrerelleno?.fechaInicio,
            fechaFin = datosPrerelleno?.fechaFin,
            descripcion = datosPrerelleno?.descripcion.orEmpty(),
            nombreJustificanteExistente = datosPrerelleno?.justificante
        )
    )
    val estadoUi: StateFlow<EstadoUiSolicitudAusencia> = _estadoUi.asStateFlow()

    init {
        cargarTipos()
    }

    fun cargarTipos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(cargandoTipos = true) }
            ausenciaRepository.obtenerTipos()
                .onSuccess { lista ->
                    _estadoUi.update { it.copy(cargandoTipos = false, tiposDisponibles = lista) }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            cargandoTipos = false,
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

    fun seleccionarTipo(tipo: String) {
        _estadoUi.update {
            it.copy(
                tipoSeleccionado = tipo,
                errorTipo = null,
                avisoJustificante = calcularAviso(tipo, hayAdjuntoEfectivo(it))
            )
        }
    }

    fun cambiarFechaInicio(fecha: LocalDate) {
        _estadoUi.update {
            val errorInicio = if (fecha.isBefore(LocalDate.now())) {
                R.string.ausencia_error_fecha_inicio_pasada
            } else {
                null
            }
            val errorFin = if (it.fechaFin != null && it.fechaFin.isBefore(fecha)) {
                R.string.ausencia_error_fecha_fin_anterior
            } else {
                null
            }
            it.copy(fechaInicio = fecha, errorFechaInicio = errorInicio, errorFechaFin = errorFin)
        }
    }

    fun cambiarFechaFin(fecha: LocalDate) {
        _estadoUi.update {
            val errorFin = if (it.fechaInicio != null && fecha.isBefore(it.fechaInicio)) {
                R.string.ausencia_error_fecha_fin_anterior
            } else {
                null
            }
            it.copy(fechaFin = fecha, errorFechaFin = errorFin)
        }
    }

    fun cambiarDescripcion(texto: String) {
        _estadoUi.update { it.copy(descripcion = texto) }
    }

    /**
     * Registra un archivo seleccionado por el usuario desde el selector de archivos
     * o desde el `content://` devuelto por `GetContent`. Valida su nombre y tipo,
     * y si es aceptable lo deja como adjunto pendiente de subida.
     */
    fun seleccionarArchivoSeleccionado(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val nombre = GestorArchivosJustificante.obtenerNombre(contextoAplicacion, uri)
            aplicarArchivo(uri, nombre)
        }
    }

    /**
     * Registra una foto tomada con cámara dado el `Uri` y la ruta física creadas
     * previamente por el `GestorArchivosJustificante`. Genera un nombre con
     * extensión `.jpg` para que los validadores y el `multipart` lo traten como imagen.
     */
    fun registrarFotoCamara(uri: Uri, rutaAbsoluta: String) {
        val nombre = rutaAbsoluta.substringAfterLast('/', "justificante.jpg")
        aplicarArchivo(uri, nombre)
    }

    private fun aplicarArchivo(uri: Uri, nombreOriginal: String?) {
        val nombre = nombreOriginal?.takeIf { it.isNotBlank() } ?: "justificante"
        val esImagen = GestorArchivosJustificante.esImagenPorNombre(nombre)
        val esPdf = nombre.substringAfterLast('.', "").lowercase() == "pdf"
        Log.d(
            "DiagAdjunto",
            "aplicarArchivo: scheme=${uri.scheme} tieneNombre=${nombreOriginal != null} esImagen=$esImagen esPdf=$esPdf"
        )
        if (!esImagen && !esPdf) {
            _estadoUi.update {
                it.copy(mensajeError = MensajeUi.Recurso(R.string.ausencia_error_tipo_archivo))
            }
            return
        }
        _estadoUi.update {
            it.copy(
                archivoUri = uri,
                nombreArchivo = nombre,
                esImagen = esImagen,
                eliminarJustificanteExistente = false,
                avisoJustificante = calcularAviso(it.tipoSeleccionado, true)
            )
        }
    }

    fun quitarArchivo() {
        _estadoUi.update {
            it.copy(
                archivoUri = null,
                nombreArchivo = null,
                esImagen = false,
                avisoJustificante = calcularAviso(it.tipoSeleccionado, hayAdjuntoEfectivo(it.copy(archivoUri = null)))
            )
        }
    }

    /**
     * Marca el justificante ya persistido en el servidor para ser eliminado en el
     * próximo `PUT`. La parte `archivo` no se enviará y el servidor debe limpiar la
     * referencia si recibe la marca correspondiente.
     */
    fun eliminarJustificanteExistente() {
        _estadoUi.update {
            it.copy(
                eliminarJustificanteExistente = true,
                avisoJustificante = calcularAviso(it.tipoSeleccionado, hayAdjuntoEfectivo(it.copy(eliminarJustificanteExistente = true)))
            )
        }
    }

    /**
     * Descarga el justificante ya persistido en el servidor, lo guarda en el caché
     * y publica el `Uri` resultante en [EstadoUiSolicitudAusencia.abrirJustificante]
     * para que la pantalla lo abra con el visor del sistema.
     */
    fun descargarJustificanteExistente() {
        val nombre = _estadoUi.value.nombreJustificanteExistente ?: return
        if (_estadoUi.value.descargandoJustificante) return
        viewModelScope.launch {
            _estadoUi.update { it.copy(descargandoJustificante = true) }
            ausenciaRepository.descargarJustificante(nombre)
                .onSuccess { bytes ->
                    val uri = GestorArchivosJustificante.guardarEnCacheYObtenerUri(
                        contextoAplicacion, bytes, nombre
                    )
                    _estadoUi.update {
                        it.copy(
                            descargandoJustificante = false,
                            abrirJustificante = JustificanteParaAbrir(uri, nombre)
                        )
                    }
                }
                .onFailure { error ->
                    _estadoUi.update {
                        it.copy(
                            descargandoJustificante = false,
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

    fun errorMostrado() {
        _estadoUi.update { it.copy(mensajeError = null) }
    }

    fun envioConsumido() {
        _estadoUi.update { it.copy(envioExitoso = false) }
    }

    fun enviar() {
        val estadoActual = _estadoUi.value
        if (estadoActual.enviando) return

        viewModelScope.launch {
            _estadoUi.update { it.copy(enviando = true, mensajeError = null) }

            Log.d(
                "DiagAdjunto",
                "enviar:estado archivoUri=${estadoActual.archivoUri != null} nombre=${estadoActual.nombreArchivo != null} esImagen=${estadoActual.esImagen} modoEdicion=${estadoActual.modoEdicion}"
            )

            val archivoBytes = estadoActual.archivoUri?.let { uri ->
                GestorArchivosJustificante.leerBytesParaSubida(
                    contextoAplicacion, uri, estadoActual.esImagen
                )
            }
            val nombreParaSubir = estadoActual.archivoUri?.let {
                nombreNormalizadoParaSubida(estadoActual.nombreArchivo, estadoActual.esImagen)
            }

            Log.d(
                "DiagAdjunto",
                "enviar:preUseCase bytesNull=${archivoBytes == null} size=${archivoBytes?.size ?: -1} nombreNull=${nombreParaSubir == null}"
            )

            // En edición sin archivo nuevo propagamos el flag para que el servidor sepa
            // si debe borrar el justificante existente (true) o mantenerlo (false).
            val flagEliminar = if (
                estadoActual.idAusenciaEditar != null &&
                archivoBytes == null &&
                estadoActual.nombreJustificanteExistente != null
            ) {
                estadoActual.eliminarJustificanteExistente
            } else {
                null
            }

            val resultado = solicitarAusenciaUseCase(
                tipo = estadoActual.tipoSeleccionado,
                descripcion = estadoActual.descripcion,
                fechaInicio = estadoActual.fechaInicio,
                fechaFin = estadoActual.fechaFin,
                archivoBytes = archivoBytes,
                nombreArchivo = nombreParaSubir,
                idAusenciaEditar = estadoActual.idAusenciaEditar,
                eliminarJustificante = flagEliminar
            )

            resultado
                .onSuccess {
                    _estadoUi.update {
                        it.copy(enviando = false, envioExitoso = true)
                    }
                }
                .onFailure { error ->
                    when (error) {
                        SolicitarAusenciaUseCase.ErrorValidacion.FechaInicioPasada ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    errorFechaInicio = R.string.ausencia_error_fecha_inicio_pasada
                                )
                            }
                        SolicitarAusenciaUseCase.ErrorValidacion.FechaFinAnterior ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    errorFechaFin = R.string.ausencia_error_fecha_fin_anterior
                                )
                            }
                        SolicitarAusenciaUseCase.ErrorValidacion.TipoVacio ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    errorTipo = R.string.ausencia_error_tipo_vacio
                                )
                            }
                        SolicitarAusenciaUseCase.ErrorValidacion.ArchivoTipoNoSoportado ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    mensajeError = MensajeUi.Recurso(R.string.ausencia_error_tipo_archivo)
                                )
                            }
                        SolicitarAusenciaUseCase.ErrorValidacion.ArchivoDemasiadoGrande ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    mensajeError = MensajeUi.Recurso(R.string.ausencia_error_archivo_grande)
                                )
                            }
                        is IOException ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    mensajeError = MensajeUi.Recurso(R.string.error_conexion)
                                )
                            }
                        else ->
                            _estadoUi.update {
                                it.copy(
                                    enviando = false,
                                    mensajeError = MensajeUi.Dinamico(
                                        error.message ?: ""
                                    )
                                )
                            }
                    }
                }
        }
    }

    private fun hayAdjuntoEfectivo(estado: EstadoUiSolicitudAusencia): Boolean {
        if (estado.archivoUri != null) return true
        return estado.nombreJustificanteExistente != null
            && !estado.eliminarJustificanteExistente
    }

    private fun nombreNormalizadoParaSubida(nombre: String?, esImagen: Boolean): String {
        val base = nombre?.takeIf { it.isNotBlank() } ?: "justificante"
        val extension = base.substringAfterLast('.', "").lowercase()
        return if (esImagen && extension != "jpg" && extension != "jpeg" && extension != "png") {
            "$base.jpg"
        } else {
            base
        }
    }

    private fun calcularAviso(tipo: String?, hayArchivo: Boolean): Int? {
        return if (tipo == "MEDICA" && !hayArchivo) {
            R.string.ausencia_aviso_justificante_medica
        } else {
            null
        }
    }

    /**
     * Conjunto mínimo de datos necesarios para abrir la pantalla en modo edición.
     * Solo se incluyen los campos editables por el usuario y la referencia al
     * justificante existente (si lo hubiera), para que la pantalla ofrezca las
     * opciones de descarga, sustitución o eliminación.
     */
    data class PrerrellenoEdicion(
        val idAusencia: Long,
        val tipo: String,
        val fechaInicio: LocalDate,
        val fechaFin: LocalDate,
        val descripcion: String?,
        val justificante: String? = null
    )

    companion object {
        val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun factory(
            contexto: Context,
            datosPrerelleno: PrerrellenoEdicion? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val apiService = retrofit.create(AusenciaApiService::class.java)
                    val gson = GsonBuilder()
                        .registerTypeAdapter(
                            LocalDate::class.java,
                            com.google.gson.JsonSerializer<LocalDate> { src, _, _ ->
                                com.google.gson.JsonPrimitive(
                                    src.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                )
                            }
                        )
                        .create()
                    val repository = AusenciaRepositoryImpl(apiService, gson)
                    val useCase = SolicitarAusenciaUseCase(repository)
                    return SolicitudAusenciaViewModel(
                        ausenciaRepository = repository,
                        solicitarAusenciaUseCase = useCase,
                        contextoAplicacion = contexto,
                        datosPrerelleno = datosPrerelleno
                    ) as T
                }
            }
    }
}
