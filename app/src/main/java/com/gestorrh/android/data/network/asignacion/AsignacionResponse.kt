package com.gestorrh.android.data.network.asignacion

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class RespuestaAsignacionTurnoDTO(
    val idAsignacion: Long,
    val idEmpleado: Long,
    val nombreCompletoEmpleado: String,
    val idTurno: Long,
    val descripcionTurno: String,
    val fecha: LocalDate,
    val modalidad: ModalidadAsignacion,
    val horaInicio: LocalTime?,
    val horaFin: LocalTime?,
    val motivoCambio: String?,
    val fechaCambio: LocalDateTime?,
    val responsableCambio: String?
)

enum class ModalidadAsignacion {
    PRESENCIAL,
    TELETRABAJO
}
