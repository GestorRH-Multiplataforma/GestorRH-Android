package com.gestorrh.android.data.repository.ausencia

import com.gestorrh.android.data.network.ausencia.AusenciaApiService
import com.gestorrh.android.data.network.ausencia.PeticionAusenciaDTO
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.domain.repository.IAusenciaRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class AusenciaRepositoryImpl(
    private val apiService: AusenciaApiService,
    private val gson: Gson
) : IAusenciaRepository {

    override suspend fun obtenerTipos(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val respuesta = apiService.getTiposAusencia()
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                Result.failure(
                    Exception(
                        extraerMensajeError(respuesta.errorBody())
                            ?: "Error ${respuesta.code()} al obtener los tipos de ausencia"
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun crearAusencia(
        peticion: PeticionAusenciaDTO,
        archivoBytes: ByteArray?,
        nombreArchivo: String?
    ): Result<RespuestaAusenciaDTO> = withContext(Dispatchers.IO) {
        try {
            val datosPart = MultipartBody.Part.createFormData(
                "datos",
                null,
                gson.toJson(peticion).toRequestBody("application/json".toMediaTypeOrNull())
            )

            val archivoPart = archivoBytes?.let { bytes ->
                MultipartBody.Part.createFormData(
                    "archivo",
                    nombreArchivo ?: "justificante",
                    bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
            }

            val respuesta = apiService.crearAusencia(datosPart, archivoPart)
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                Result.failure(
                    Exception(
                        extraerMensajeError(respuesta.errorBody())
                            ?: "Error ${respuesta.code()} al crear la ausencia"
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerMisAusencias(estado: String?): Result<List<RespuestaAusenciaDTO>> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = apiService.getMisAusencias(estado)
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Result.success(respuesta.body()!!)
                } else {
                    Result.failure(
                        Exception(
                            extraerMensajeError(respuesta.errorBody())
                                ?: "Error ${respuesta.code()} al obtener las ausencias"
                        )
                    )
                }
            } catch (e: IOException) {
                Result.failure(e)
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
