package com.gestorrh.android.data.repository

import com.gestorrh.android.data.network.autenticacion.AuthApi
import com.gestorrh.android.data.network.autenticacion.PeticionLoginDTO
import com.gestorrh.android.data.network.autenticacion.RespuestaLoginDTO
import com.gestorrh.android.domain.repository.IAuthRepository
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Implementación de [IAuthRepository] que delega en el servicio Retrofit [AuthApi].
 * Encapsula toda la lógica de conversión HTTP → Result, liberando a los ViewModels
 * de conocer los detalles del protocolo de red.
 *
 * @param authApi Servicio Retrofit para los endpoints de autenticación.
 */
class AuthRepository(private val authApi: AuthApi) : IAuthRepository {

    override suspend fun login(email: String, password: String): Result<RespuestaLoginDTO> {
        return try {
            val respuesta = authApi.login(PeticionLoginDTO(email, password))
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                val mensaje = extraerMensajeError(respuesta.errorBody())
                Result.failure(Exception(mensaje))
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
