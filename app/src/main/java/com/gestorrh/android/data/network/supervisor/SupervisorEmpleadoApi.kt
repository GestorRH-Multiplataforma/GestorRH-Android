package com.gestorrh.android.data.network.supervisor

import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import retrofit2.Response
import retrofit2.http.GET

interface SupervisorEmpleadoApi {

    @GET("api/empleados")
    suspend fun getEmpleadosEquipo(): Response<List<RespuestaEmpleadoDTO>>
}