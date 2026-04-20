package com.gestorrh.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representación persistida de una asignación de turno en la base de datos local.
 *
 * Esta entidad espeja los campos relevantes de `RespuestaAsignacionTurnoDTO` pero
 * transforma los tipos complejos (`LocalDate`, enums) a tipos primitivos soportados
 * por Room sin necesidad de TypeConverters:
 *
 * - `fecha` se almacena como String con formato ISO `yyyy-MM-dd`.
 * - `modalidad` se almacena como String con los valores `PRESENCIAL` o `TELETRABAJO`.
 *
 * Los campos `horaInicio` y `horaFin` se persisten para que la pantalla de Mis Turnos
 * pueda renderizar la tarjeta y el detalle de la asignación sin red.
 *
 * `fechaSincronizacion` es el timestamp en milisegundos del último refresco exitoso
 * desde el servidor; sirve para conocer la antigüedad de los datos cacheados.
 */
@Entity(tableName = "asignaciones")
data class AsignacionEntity(
    @PrimaryKey val idAsignacion: Long,
    val idTurno: Long,
    val descripcionTurno: String,
    val fecha: String,
    val modalidad: String,
    val horaInicio: String?,
    val horaFin: String?,
    val motivoCambio: String?,
    val responsableCambio: String?,
    val fechaSincronizacion: Long
)
