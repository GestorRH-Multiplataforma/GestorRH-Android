package com.gestorrh.android.data.network.asignacion

import java.time.LocalDate

data class PeticionAsignacionTurnoDTO(
    val idEmpleado: Long,
    val idTurno: Long,
    val fecha: LocalDate,
    val modalidad: ModalidadAsignacion,
    val motivoCambio: String? = null
)
