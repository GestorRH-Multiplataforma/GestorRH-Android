package com.gestorrh.android.data.network.asignacion

import retrofit2.Response
import retrofit2.http.GET

interface AsignacionApiService {

    @GET("api/asignaciones/me")
    suspend fun getMisAsignaciones(): Response<List<RespuestaAsignacionTurnoDTO>>
}
