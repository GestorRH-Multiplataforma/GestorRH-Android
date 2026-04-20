package com.gestorrh.android.data.repository.asignacion

import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.domain.repository.IAsignacionRepository
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Implementación de [IAsignacionRepository] que delega en el servicio Retrofit [AsignacionApiService].
 * Centraliza la extracción de mensajes de error del cuerpo JSON de la respuesta,
 * evitando que esta lógica de protocolo de red se filtre a la capa de presentación.
 *
 * @param apiService Servicio Retrofit para los endpoints de asignaciones.
 */
class AsignacionRepositoryImpl(
    private val apiService: AsignacionApiService
) : IAsignacionRepository {

    override suspend fun getMisAsignaciones(): Result<List<RespuestaAsignacionTurnoDTO>> {
        return try {
            val respuesta = apiService.getMisAsignaciones()
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                val mensaje = extraerMensajeError(respuesta.errorBody())
                    ?: "Error ${respuesta.code()} al obtener las asignaciones"
                Result.failure(Exception(mensaje))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extraerMensajeError(errorBody: ResponseBody?): String? {
        return try {
            val json = errorBody?.string() ?: return null
            JSONObject(json).getString("message")
        } catch (e: JSONException) {
            null
        }
    }
}
