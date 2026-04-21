package com.gestorrh.android.ui.ausencia

import android.net.Uri
import androidx.annotation.StringRes
import com.gestorrh.android.core.ui.MensajeUi
import java.time.LocalDate

data class EstadoUiSolicitudAusencia(
    val tiposDisponibles: List<String> = emptyList(),
    val cargandoTipos: Boolean = false,
    val tipoSeleccionado: String? = null,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
    val descripcion: String = "",
    val archivoUri: Uri? = null,
    val nombreArchivo: String? = null,
    val esImagen: Boolean = false,
    val nombreJustificanteExistente: String? = null,
    val eliminarJustificanteExistente: Boolean = false,
    val descargandoJustificante: Boolean = false,
    val errorTipo: Int? = null,
    @StringRes val errorFechaInicio: Int? = null,
    @StringRes val errorFechaFin: Int? = null,
    @StringRes val avisoJustificante: Int? = null,
    val enviando: Boolean = false,
    val mensajeError: MensajeUi? = null,
    val envioExitoso: Boolean = false,
    val abrirJustificante: JustificanteParaAbrir? = null,
    val idAusenciaEditar: Long? = null
) {
    val modoEdicion: Boolean
        get() = idAusenciaEditar != null

    val hayJustificanteExistenteVisible: Boolean
        get() = nombreJustificanteExistente != null
            && !eliminarJustificanteExistente
            && archivoUri == null

    val formularioValido: Boolean
        get() = !tipoSeleccionado.isNullOrBlank()
            && fechaInicio != null
            && fechaFin != null
            && !fechaFin.isBefore(fechaInicio)
            && !fechaInicio.isBefore(LocalDate.now())
}

/**
 * Evento de un solo uso emitido por el ViewModel cuando un justificante ya ha sido
 * descargado y cacheado: la pantalla lo consume lanzando `Intent.ACTION_VIEW`.
 */
data class JustificanteParaAbrir(
    val uri: android.net.Uri,
    val nombreArchivo: String
)
