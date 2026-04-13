package com.gestorrh.android.data.network.autenticacion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Contrato de comunicación HTTP para el dominio de Autenticación.
 * Define los endpoints expuestos por el backend (Spring Boot) para gestionar
 * el acceso y las sesiones de los empleados.
 */
interface AuthApi {

    /**
     * Solicita la apertura de una nueva sesión verificando las credenciales del empleado.
     * Se ejecuta de forma asíncrona (suspend) para evitar el bloqueo del hilo principal (UI Thread).
     *
     * @param peticion Carga útil que contiene el correo y la contraseña.
     * @return La respuesta HTTP del servidor. Un código 200 contendrá el Token JWT en el cuerpo,
     * mientras que los códigos 401 o 403 indicarán credenciales inválidas o falta de permisos.
     */
    @POST("auth/login-empleado")
    suspend fun login(
        @Body peticion: PeticionLoginDTO
    ): Response<RespuestaLoginDTO>

}