package com.gestorrh.android.data.network.fichaje

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface FichajeApiService {

    @GET("api/fichajes/estado-actual")
    suspend fun obtenerEstadoActual(): Response<RespuestaEstadoFichajeDTO>

    @POST("api/fichajes/entrada")
    suspend fun ficharEntrada(@Body peticion: PeticionFichajeEntradaDTO): Response<RespuestaFichajeDTO>

    @PUT("api/fichajes/salida")
    suspend fun ficharSalida(@Body peticion: PeticionFichajeSalidaDTO): Response<RespuestaFichajeDTO>
}