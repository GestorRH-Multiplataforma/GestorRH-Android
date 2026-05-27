package com.gestorrh.android.data.network.fichaje

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FichajeApiService {

    @GET("api/fichajes/estado-actual")
    suspend fun obtenerEstadoActual(): Response<RespuestaEstadoFichajeDTO>

    @POST("api/fichajes/entrada")
    suspend fun ficharEntrada(@Body peticion: PeticionFichajeEntradaDTO): Response<RespuestaFichajeDTO>

    @PUT("api/fichajes/salida")
    suspend fun ficharSalida(@Body peticion: PeticionFichajeSalidaDTO): Response<RespuestaFichajeDTO>

    @GET("api/fichajes")
    suspend fun obtenerHistorial(
        @Query("fechaInicio") fechaInicio: String,
        @Query("fechaFin") fechaFin: String,
        @Query("empleadoId") empleadoId: Long? = null
    ): Response<List<RespuestaFichajeDTO>>

    @PUT("api/fichajes/{idFichaje}/modificar")
    suspend fun modificarFichaje(
        @Path("idFichaje") idFichaje: Long,
        @Body body: PeticionModificacionFichajeDTO
    ): Response<RespuestaFichajeDTO>
}