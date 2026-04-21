package com.gestorrh.android.data.network.ausencia

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface AusenciaApiService {

    @Multipart
    @POST("api/ausencias")
    suspend fun crearAusencia(
        @Part datos: MultipartBody.Part,
        @Part archivo: MultipartBody.Part?
    ): Response<RespuestaAusenciaDTO>

    @GET("api/ausencias/tipos")
    suspend fun getTiposAusencia(): Response<List<String>>

    @GET("api/ausencias/me")
    suspend fun getMisAusencias(
        @Query("estado") estado: String? = null
    ): Response<List<RespuestaAusenciaDTO>>
}
