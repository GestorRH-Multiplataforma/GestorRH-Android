package com.gestorrh.android.data.network.asignacion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AsignacionApiService {

    @GET("api/asignaciones/me")
    suspend fun getMisAsignaciones(): Response<List<RespuestaAsignacionTurnoDTO>>

    @GET("api/asignaciones")
    suspend fun getAsignacionesEquipo(): Response<List<RespuestaAsignacionTurnoDTO>>

    @POST("api/asignaciones")
    suspend fun crearAsignacion(
        @Body body: PeticionAsignacionTurnoDTO
    ): Response<RespuestaAsignacionTurnoDTO>

    @PUT("api/asignaciones/{id}")
    suspend fun actualizarAsignacion(
        @Path("id") id: Long,
        @Body body: PeticionAsignacionTurnoDTO
    ): Response<RespuestaAsignacionTurnoDTO>

    @DELETE("api/asignaciones/{id}")
    suspend fun eliminarAsignacion(
        @Path("id") id: Long
    ): Response<Unit>
}
