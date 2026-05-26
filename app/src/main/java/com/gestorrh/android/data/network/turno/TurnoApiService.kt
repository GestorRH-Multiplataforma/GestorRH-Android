package com.gestorrh.android.data.network.turno

import retrofit2.Response
import retrofit2.http.GET

interface TurnoApiService {

    @GET("api/turnos")
    suspend fun getTurnos(): Response<List<RespuestaTurnoDTO>>
}