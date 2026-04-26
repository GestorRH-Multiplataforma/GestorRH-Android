package com.gestorrh.android.data.network.fichaje

import java.time.LocalDate
import java.time.LocalDateTime

data class RespuestaEstadoFichajeDTO(
    val trabajandoActualmente: Boolean,
    val idFichajeAbierto: Long?,
    val horaEntrada: LocalDateTime?,
    val tieneTurnoHoy: Boolean,
    val modalidadHoy: ModalidadTurno?
)

data class RespuestaFichajeDTO(
    val idFichaje: Long,
    val idEmpleado: Long,
    val nombreEmpleado: String,
    val idAsignacion: Long?,
    val descripcionTurno: String?,
    val fecha: LocalDate,
    val horaEntrada: LocalDateTime,
    val horaSalida: LocalDateTime?,
    val incidencias: String?
)
