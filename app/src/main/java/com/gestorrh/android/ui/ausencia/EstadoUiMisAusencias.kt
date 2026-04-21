package com.gestorrh.android.ui.ausencia

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO

/**
 * Estado inmutable que consume la pantalla [PantallaMisAusencias].
 *
 * @property cargando `true` mientras hay una petición en vuelo — la UI muestra el
 *           indicador de progreso centrado cuando la lista aún no se ha cargado.
 * @property ausencias Listado de solicitudes del empleado autenticado tal como llega de
 *           `GET /api/ausencias/me` (el servidor ya aplica el orden deseado).
 * @property mensajeError Error a mostrar en el Snackbar o `null` si no hay error pendiente.
 */
data class EstadoUiMisAusencias(
    val cargando: Boolean = false,
    val ausencias: List<RespuestaAusenciaDTO> = emptyList(),
    val mensajeError: MensajeUi? = null
)
