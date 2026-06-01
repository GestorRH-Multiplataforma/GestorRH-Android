package com.gestorrh.android.data.network.autenticacion

import com.google.gson.annotations.SerializedName

/**
 * DTO que encapsula las credenciales del usuario.
 * Representa el cuerpo de la petición HTTP enviada al servidor para abrir una sesión.
 *
 * @property email Correo del empleado.
 * @property password Contraseña introducida por el usuario.
 */
data class PeticionLoginDTO(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * DTO que modela la respuesta exitosa del servidor.
 * Contiene el token de seguridad y la información básica del perfil necesaria para
 * inicializar la aplicación sin hacer llamadas extra a la API.
 *
 * @property token Cadena JWT firmada por el servidor para autorizar futuras peticiones.
 * @property rol Nivel de permisos del usuario (ej. EMPLEADO, SUPERVISOR).
 * @property id Identificador único del empleado en la base de datos central.
 * @property nombre Nombre completo del empleado.
 * @property nombreEmpresa Nombre de la empresa a la que pertenece el empleado.
 */
data class RespuestaLoginDTO(
    @SerializedName("token") val token: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("nombreEmpresa") val nombreEmpresa: String
)
