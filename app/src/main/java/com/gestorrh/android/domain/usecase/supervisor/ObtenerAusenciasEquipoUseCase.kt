package com.gestorrh.android.domain.usecase.supervisor

import com.gestorrh.android.data.network.ausencia.EstadoAusencia
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.domain.repository.IAusenciaRepository

class ObtenerAusenciasEquipoUseCase(
    private val repository: IAusenciaRepository
) {
    suspend operator fun invoke(
        estado: EstadoAusencia? = null
    ): Result<List<RespuestaAusenciaDTO>> =
        repository.obtenerAusenciasEquipo(estado?.name)
}