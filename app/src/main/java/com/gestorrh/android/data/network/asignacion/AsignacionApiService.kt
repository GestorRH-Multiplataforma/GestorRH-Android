package com.gestorrh.android.data.network.asignacion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AsignacionApiService {

    @GET("api/asignaciones/me")
    suspend fun getMisAsignaciones(): Response<List<RespuestaAsignacionTurnoDTO>>

    @GET("api/asignaciones")
    suspend fun getAsignacionesEquipo(): Response<List<RespuestaAsignacionTurnoDTO>>

    @POST("api/asignaciones")
    suspend fun crearAsignacion(
        @Body body: PeticionAsignacionTurnoDTO
    ): Response<RespuestaAsignacionTurnoDTO>
}
