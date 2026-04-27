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

/**
 * Implementación de [IAusenciaRepository] que delega en [AusenciaApiService].
 *
 * Construye las peticiones `multipart/form-data` para crear y actualizar ausencias
 * (parte JSON `datos` con [PeticionAusenciaDTO] y parte binaria opcional `archivo`),
 * y centraliza la conversión de respuestas HTTP en [Result] extrayendo el campo
 * `message` del cuerpo de error de la API Spring Boot cuando esté disponible.
 *
 * @param apiService Servicio Retrofit para los endpoints de ausencias.
 * @param gson Serializador usado para construir la parte `datos` del multipart.
 */
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
            val datosPart = construirPartDatos(peticion)
            val archivoPart = construirPartArchivo(archivoBytes, nombreArchivo)

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

    override suspend fun actualizarAusencia(
        idAusencia: Long,
        peticion: PeticionAusenciaDTO,
        archivoBytes: ByteArray?,
        nombreArchivo: String?
    ): Result<RespuestaAusenciaDTO> = withContext(Dispatchers.IO) {
        try {
            val datosPart = construirPartDatos(peticion)
            val archivoPart = construirPartArchivo(archivoBytes, nombreArchivo)

            val respuesta = apiService.actualizarAusencia(idAusencia, datosPart, archivoPart)
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                Result.failure(
                    Exception(
                        extraerMensajeError(respuesta.errorBody())
                            ?: "Error ${respuesta.code()} al actualizar la ausencia"
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelarAusencia(idAusencia: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = apiService.cancelarAusencia(idAusencia)
                if (respuesta.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        Exception(
                            extraerMensajeError(respuesta.errorBody())
                                ?: "Error ${respuesta.code()} al cancelar la ausencia"
                        )
                    )
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun descargarJustificante(nombreArchivo: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = apiService.descargarJustificante(nombreArchivo)
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Result.success(respuesta.body()!!.bytes())
                } else {
                    Result.failure(
                        Exception(
                            extraerMensajeError(respuesta.errorBody())
                                ?: "Error ${respuesta.code()} al descargar el justificante"
                        )
                    )
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun construirPartDatos(peticion: PeticionAusenciaDTO): MultipartBody.Part {
        return MultipartBody.Part.createFormData(
            "datos",
            null,
            gson.toJson(peticion).toRequestBody("application/json".toMediaTypeOrNull())
        )
    }

    private fun construirPartArchivo(
        archivoBytes: ByteArray?,
        nombreArchivo: String?
    ): MultipartBody.Part? {
        if (archivoBytes == null) return null
        val tipoMime = tipoMimeDesdeNombre(nombreArchivo)
        return MultipartBody.Part.createFormData(
            "archivo",
            nombreArchivo ?: "justificante",
            archivoBytes.toRequestBody(tipoMime.toMediaTypeOrNull())
        )
    }

    private fun tipoMimeDesdeNombre(nombre: String?): String {
        val extension = nombre?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
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
