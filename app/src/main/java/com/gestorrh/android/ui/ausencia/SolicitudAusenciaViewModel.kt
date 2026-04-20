package com.gestorrh.android.ui.ausencia

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import com.gestorrh.android.domain.usecase.ausencia.SolicitarAusenciaUseCase
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * `Uri` adjunto a bytes y la construcción del `multipart` viven aquí o en el UseCase.
 *
 * Bloquea reentradas en [enviar] mediante el flag `enviando` para evitar el doble envío
 * por doble clic, y traduce los errores de validación del UseCase a recursos de string
 * para que la pantalla los muestre directamente con `stringResource`.
 *
 * @param ausenciaRepository Acceso a los endpoints de ausencias.
 * @param solicitarAusenciaUseCase Encapsula validaciones locales y la llamada al repositorio.
 * @param contextoAplicacion Contexto de aplicación para resolver el `ContentResolver` al
 *        leer el `Uri` del justificante. Solo se usa para esta operación de I/O.
 */
class SolicitudAusenciaViewModel(
    private val ausenciaRepository: IAusenciaRepository,
    private val solicitarAusenciaUseCase: SolicitarAusenciaUseCase,
    private val contextoAplicacion: Context
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiSolicitudAusencia())
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
                avisoJustificante = calcularAviso(tipo, it.archivoUri != null)
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

    fun seleccionarArchivo(uri: Uri?) {
        viewModelScope.launch {
            val nombre = uri?.let { obtenerNombreArchivo(it) }
            _estadoUi.update {
                it.copy(
                    archivoUri = uri,
                    nombreArchivo = nombre,
                    avisoJustificante = calcularAviso(it.tipoSeleccionado, uri != null)
                )
            }
        }
    }

    fun quitarArchivo() {
        _estadoUi.update {
            it.copy(
                archivoUri = null,
                nombreArchivo = null,
                avisoJustificante = calcularAviso(it.tipoSeleccionado, false)
            )
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

            val archivoBytes = estadoActual.archivoUri?.let { leerBytes(it) }

            val resultado = solicitarAusenciaUseCase(
                tipo = estadoActual.tipoSeleccionado,
                descripcion = estadoActual.descripcion,
                fechaInicio = estadoActual.fechaInicio,
                fechaFin = estadoActual.fechaFin,
                archivoBytes = archivoBytes,
                nombreArchivo = estadoActual.nombreArchivo
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

    private fun calcularAviso(tipo: String?, hayArchivo: Boolean): Int? {
        return if (tipo == "MEDICA" && !hayArchivo) {
            R.string.ausencia_aviso_justificante_medica
        } else {
            null
        }
    }

    private suspend fun leerBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            contextoAplicacion.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun obtenerNombreArchivo(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            contextoAplicacion.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (indice >= 0 && cursor.moveToFirst()) cursor.getString(indice) else null
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    companion object {
        val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun factory(contexto: Context): ViewModelProvider.Factory =
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
                    return SolicitudAusenciaViewModel(repository, useCase, contexto) as T
                }
            }
    }
}
