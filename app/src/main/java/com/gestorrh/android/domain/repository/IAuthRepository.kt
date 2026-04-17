package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.autenticacion.RespuestaLoginDTO

/**
 * Contrato de la capa de dominio para las operaciones de autenticación.
 * Los ViewModels dependen de esta interfaz, nunca de la implementación concreta
 * ni del servicio Retrofit directamente. Esto facilita el testeo unitario
 * mediante sustitución por dobles de prueba (fakes/mocks).
 */
interface IAuthRepository {

    /**
     * Autentica al empleado con sus credenciales.
     *
     * @param email Correo electrónico del empleado.
     * @param password Contraseña del empleado.
     * @return [Result.success] con [RespuestaLoginDTO] si el servidor acepta las credenciales,
     *         o [Result.failure] con el mensaje de error extraído del cuerpo de la respuesta.
     */
    suspend fun login(email: String, password: String): Result<RespuestaLoginDTO>
}
