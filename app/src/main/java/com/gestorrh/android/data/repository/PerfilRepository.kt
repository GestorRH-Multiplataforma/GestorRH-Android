package com.gestorrh.android.data.repository

import com.gestorrh.android.data.network.empleado.EmpleadoApi
import com.gestorrh.android.data.network.empleado.PeticionCambiarPasswordDTO
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.domain.repository.IPerfilRepository
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Implementación de [IPerfilRepository] que delega en [EmpleadoApi].
 * Convierte las respuestas HTTP en [Result] y extrae el campo "message"
 * de los cuerpos de error 4xx/5xx, siguiendo el contrato de la API Spring Boot.
 *
 * @param empleadoApi Servicio Retrofit para los endpoints de empleado autenticado.
 */
class PerfilRepository(private val empleadoApi: EmpleadoApi) : IPerfilRepository {

    override suspend fun obtenerMiPerfil(): Result<RespuestaEmpleadoDTO> {
        return try {
            val respuesta = empleadoApi.getMiPerfil()
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                Result.failure(Exception(extraerMensajeError(respuesta.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cambiarContrasena(passwordActual: String, nuevaPassword: String): Result<Unit> {
        return try {
            val respuesta = empleadoApi.cambiarContrasena(
                PeticionCambiarPasswordDTO(passwordActual, nuevaPassword)
            )
            if (respuesta.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(extraerMensajeError(respuesta.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extraerMensajeError(errorBody: ResponseBody?): String {
        return try {
            val json = errorBody?.string() ?: return MENSAJE_ERROR_DESCONOCIDO
            JSONObject(json).getString("message")
        } catch (e: JSONException) {
            MENSAJE_ERROR_DESCONOCIDO
        }
    }

    companion object {
        private const val MENSAJE_ERROR_DESCONOCIDO = "Error desconocido del servidor"
    }
}
