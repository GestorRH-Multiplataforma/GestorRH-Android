package com.gestorrh.android.ui.turnos

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO

/**
 * Representa el estado inmutable de la UI para la pantalla de Mis Turnos.
 * En Jetpack Compose, cualquier cambio visual se produce al emitir una nueva copia de esta clase,
 * garantizando un flujo de datos unidireccional (UDF) libre de condiciones de carrera.
 *
 * @property cargando Indica si hay una petición de red en curso.
 * @property asignaciones Lista de asignaciones de turno del empleado autenticado.
 * @property mensajeError Mensaje de error a mostrar en el Snackbar. Null si no hay error.
 * @property vistaActual Controla qué modo de visualización está activo (lista o calendario).
 */
data class EstadoUiMisTurnos(
    val cargando: Boolean = true,
    val asignaciones: List<RespuestaAsignacionTurnoDTO> = emptyList(),
    val mensajeError: MensajeUi? = null,
    val vistaActual: VistaActual = VistaActual.LISTA
)

enum class VistaActual {
    LISTA,
    CALENDARIO
}
