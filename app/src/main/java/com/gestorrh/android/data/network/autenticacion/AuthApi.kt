package com.gestorrh.android.data.network.autenticacion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    /**
     * Endpoint para autenticar a un empleado.
     * @param peticion Contiene las credenciales (email y password).
     * @return Response envuelve la respuesta HTTP para poder leer los códigos (200, 401, etc).
     */
    @POST("auth/login-empleado")
    suspend fun login(
        @Body peticion: PeticionLoginDTO
    ): Response<RespuestaLoginDTO>

}