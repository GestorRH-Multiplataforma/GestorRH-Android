package com.gestorrh.android.data.network.ausencia

import java.time.LocalDate

/**
 * Cuerpo JSON (parte `datos` del `multipart`) enviado a `POST /api/ausencias` y
 * `PUT /api/ausencias/{id}`.
 *
 * @property eliminarJustificante Solo relevante en modo edición cuando la parte
 *           `archivo` no se envía: si es `true` el servidor debe borrar el
 *           justificante actual; si es `false` debe mantenerlo intacto. En modo
 *           creación o cuando se adjunta un archivo nuevo se envía `null` para
 *           que el servidor ignore el flag.
 */
data class PeticionAusenciaDTO(
    val tipo: String,
    val descripcion: String?,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    val eliminarJustificante: Boolean? = null
)
