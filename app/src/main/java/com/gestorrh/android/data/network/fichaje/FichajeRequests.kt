package com.gestorrh.android.data.network.fichaje

import java.time.LocalDateTime

data class PeticionFichajeEntradaDTO(
    val latitud: Double?,
    val longitud: Double?
)

data class PeticionFichajeSalidaDTO(
    val latitud: Double?,
    val longitud: Double?
)

data class PeticionModificacionFichajeDTO(
    val nuevaHoraEntrada: LocalDateTime?,
    val nuevaHoraSalida: LocalDateTime?,
    val motivoModificacion: String
)
