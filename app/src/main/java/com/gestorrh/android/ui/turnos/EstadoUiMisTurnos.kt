package com.gestorrh.android.ui.turnos

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.local.entity.AsignacionEntity

/**
 * Representa el estado inmutable de la UI para la pantalla de Mis Turnos.
 * En Jetpack Compose, cualquier cambio visual se produce al emitir una nueva copia de esta clase,
 * garantizando un flujo de datos unidireccional (UDF) libre de condiciones de carrera.
 *
 * @property cargando Indica si hay una sincronización en curso y todavía no hay caché.
 * @property asignaciones Lista de asignaciones cacheadas en Room, ordenadas por fecha ascendente.
 * @property mensajeError Mensaje de error a mostrar en el Snackbar. Null si no hay error.
 * @property sinConexion true cuando la última sincronización falló por falta de red pero se
 *                       están mostrando datos cacheados válidos.
 * @property vistaActual Controla qué modo de visualización está activo (lista o calendario).
 */
data class EstadoUiMisTurnos(
    val cargando: Boolean = true,
    val asignaciones: List<AsignacionEntity> = emptyList(),
    val mensajeError: MensajeUi? = null,
    val sinConexion: Boolean = false,
    val vistaActual: VistaActual = VistaActual.CALENDARIO
)

enum class VistaActual {
    LISTA,
    CALENDARIO
}
