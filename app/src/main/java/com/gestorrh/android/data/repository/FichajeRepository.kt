package com.gestorrh.android.data.repository

import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import com.gestorrh.android.data.network.fichaje.RespuestaEstadoFichajeDTO
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import com.gestorrh.android.domain.repository.IFichajeRepository
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Implementación de [IFichajeRepository] que delega en el servicio Retrofit [FichajeApiService].
 * Centraliza la extracción de mensajes de error del cuerpo JSON de la respuesta,
 * evitando que esta lógica de protocolo de red se filtre a la capa de presentación.
 *
 * @param apiService Servicio Retrofit para los endpoints de fichaje.
 */
class FichajeRepository(private val apiService: FichajeApiService) : IFichajeRepository {

    override suspend fun obtenerEstadoActual(): Result<RespuestaEstadoFichajeDTO> {
        return try {
            val respuesta = apiService.obtenerEstadoActual()
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                val mensaje = extraerMensajeError(respuesta.errorBody())
                    ?: "Error ${respuesta.code()} al obtener el estado"
                Result.failure(Exception(mensaje))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ficharEntrada(peticion: PeticionFichajeEntradaDTO): Result<RespuestaFichajeDTO> {
        return try {
            val respuesta = apiService.ficharEntrada(peticion)
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                val mensaje = extraerMensajeError(respuesta.errorBody())
                    ?: "Error ${respuesta.code()} al fichar entrada"
                Result.failure(Exception(mensaje))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ficharSalida(peticion: PeticionFichajeSalidaDTO): Result<RespuestaFichajeDTO> {
        return try {
            val respuesta = apiService.ficharSalida(peticion)
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                val mensaje = extraerMensajeError(respuesta.errorBody())
                    ?: "Error ${respuesta.code()} al fichar salida"
                Result.failure(Exception(mensaje))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extrae el campo "message" del cuerpo JSON de una respuesta de error de la API Spring Boot.
     * La API siempre responde errores 4xx/5xx con un JSON que contiene dicho campo.
     *
     * @return El mensaje de error legible, o null si el cuerpo no es parseable.
     */
    private fun extraerMensajeError(errorBody: ResponseBody?): String? {
        return try {
            val json = errorBody?.string() ?: return null
            JSONObject(json).getString("message")
        } catch (e: JSONException) {
            null
        }
    }
}
