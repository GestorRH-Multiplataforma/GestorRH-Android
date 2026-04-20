package com.gestorrh.android.data.network.ausencia

import java.time.LocalDate

data class PeticionAusenciaDTO(
    val tipo: String,
    val descripcion: String?,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate
)
