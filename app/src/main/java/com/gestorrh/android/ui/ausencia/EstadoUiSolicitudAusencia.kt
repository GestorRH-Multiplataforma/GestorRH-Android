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
    val errorTipo: Int? = null,
    @StringRes val errorFechaInicio: Int? = null,
    @StringRes val errorFechaFin: Int? = null,
    @StringRes val avisoJustificante: Int? = null,
    val enviando: Boolean = false,
    val mensajeError: MensajeUi? = null,
    val envioExitoso: Boolean = false,
    val idAusenciaEditar: Long? = null
) {
    val modoEdicion: Boolean
        get() = idAusenciaEditar != null

    val formularioValido: Boolean
        get() = !tipoSeleccionado.isNullOrBlank()
            && fechaInicio != null
            && fechaFin != null
            && !fechaFin.isBefore(fechaInicio)
            && !fechaInicio.isBefore(LocalDate.now())
}
