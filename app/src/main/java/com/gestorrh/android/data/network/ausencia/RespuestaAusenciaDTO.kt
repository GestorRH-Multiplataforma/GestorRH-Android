package com.gestorrh.android.data.network.ausencia

import java.time.LocalDate
import java.time.LocalDateTime

data class RespuestaAusenciaDTO(
    val idAusencia: Long,
    val idEmpleado: Long?,
    val nombreCompletoEmpleado: String?,
    val tipo: String,
    val descripcion: String?,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    val estado: String,
    val justificante: String?,
    val fechaSolicitud: LocalDateTime?,
    val fechaResolucion: LocalDateTime?,
    val responsableResolucion: String?,
    val motivoRechazo: String?
)
