package com.gestorrh.android.ui.historial

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import java.time.LocalDate

/**
 * Estado inmutable de la pantalla de historial de fichajes personales.
 *
 * @property fichajes Lista visible ya ordenada (más reciente primero).
 * @property cargando Indica si hay una petición en curso al servidor.
 * @property mensajeError Mensaje a mostrar en Snackbar cuando una operación falla.
 * @property fechaInicio Inicio del rango seleccionado, por defecto hoy menos 30 días.
 * @property fechaFin Fin del rango seleccionado, por defecto hoy.
 */
data class EstadoUiHistorialFichajes(
    val fichajes: List<RespuestaFichajeDTO> = emptyList(),
    val cargando: Boolean = false,
    val mensajeError: MensajeUi? = null,
    val fechaInicio: LocalDate = LocalDate.now().minusDays(30),
    val fechaFin: LocalDate = LocalDate.now()
)
