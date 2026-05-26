package com.gestorrh.android.data.network.turno

import java.time.LocalTime

data class RespuestaTurnoDTO(
    val idTurno: Long,
    val descripcion: String,
    val horaInicio: LocalTime?,
    val horaFin: LocalTime?
)