package com.gestorrh.android.data.network.ausencia

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
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

    @Multipart
    @PUT("api/ausencias/{id}")
    suspend fun actualizarAusencia(
        @Path("id") id: Long,
        @Part datos: MultipartBody.Part,
        @Part archivo: MultipartBody.Part?
    ): Response<RespuestaAusenciaDTO>

    @DELETE("api/ausencias/{id}")
    suspend fun cancelarAusencia(
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/ausencias/justificantes/{nombreArchivo}")
    suspend fun descargarJustificante(
        @Path("nombreArchivo") nombreArchivo: String
    ): Response<ResponseBody>

    @GET("api/ausencias")
    suspend fun getAusenciasEquipo(
        @Query("estado") estado: String? = null
    ): Response<List<RespuestaAusenciaDTO>>

    @PUT("api/ausencias/{id}/revision")
    suspend fun revisarAusencia(
        @Path("id") id: Long,
        @Body body: PeticionRevisionAusenciaDTO
    ): Response<RespuestaAusenciaDTO>
}
