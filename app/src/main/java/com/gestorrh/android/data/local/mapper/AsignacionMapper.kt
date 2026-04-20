package com.gestorrh.android.data.local.mapper

import com.gestorrh.android.data.local.entity.AsignacionEntity
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO

fun RespuestaAsignacionTurnoDTO.toEntity(fechaSincronizacion: Long): AsignacionEntity =
    AsignacionEntity(
        idAsignacion = idAsignacion,
        idTurno = idTurno,
        descripcionTurno = descripcionTurno,
        fecha = fecha.toString(),
        modalidad = modalidad.name,
        horaInicio = horaInicio,
        horaFin = horaFin,
        motivoCambio = motivoCambio,
        responsableCambio = responsableCambio,
        fechaSincronizacion = fechaSincronizacion
    )
