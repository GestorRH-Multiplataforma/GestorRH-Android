package com.gestorrh.android.data.network.empleado

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface EmpleadoApi {

    @GET("api/empleados/me")
    suspend fun getMiPerfil(): Response<RespuestaEmpleadoDTO>

    @PUT("api/empleados/me/contrasena")
    suspend fun cambiarContrasena(@Body body: PeticionCambiarPasswordDTO): Response<Unit>
}
