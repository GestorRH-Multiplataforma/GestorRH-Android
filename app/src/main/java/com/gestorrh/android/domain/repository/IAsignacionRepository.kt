package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO

/**
 * Contrato de la capa de dominio para las operaciones de asignación de turnos.
 * Los ViewModels dependen de esta interfaz, nunca del servicio Retrofit directamente.
 * La implementación concreta vive en la capa de datos ([com.gestorrh.android.data.repository.asignacion]).
 */
interface IAsignacionRepository {

    /**
     * Obtiene la lista de asignaciones de turno del empleado autenticado.
     *
     * @return [Result.success] con la lista de [RespuestaAsignacionTurnoDTO], o [Result.failure]
     *         con el mensaje de error del servidor o de red.
     */
    suspend fun getMisAsignaciones(): Result<List<RespuestaAsignacionTurnoDTO>>
}
