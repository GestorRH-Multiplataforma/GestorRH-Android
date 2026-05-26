package com.gestorrh.android.data.repository.cuadrante

import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.data.network.asignacion.PeticionAsignacionTurnoDTO
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.supervisor.SupervisorEmpleadoApi
import com.gestorrh.android.data.network.turno.RespuestaTurnoDTO
import com.gestorrh.android.data.network.turno.TurnoApiService
import com.gestorrh.android.domain.repository.ICuadranteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Implementación de [ICuadranteRepository] que delega en los servicios Retrofit
 * correspondientes. No usa caché local: todos los datos se obtienen siempre
 * frescos desde el servidor para garantizar la consistencia del cuadrante.
 *
 * @param asignacionApiService Servicio Retrofit para los endpoints de asignaciones.
 * @param turnoApiService Servicio Retrofit para el catálogo de turnos.
 * @param supervisorEmpleadoApi Servicio Retrofit para los empleados del departamento.
 */
class CuadranteRepositoryImpl(
    private val asignacionApiService: AsignacionApiService,
    private val turnoApiService: TurnoApiService,
    private val supervisorEmpleadoApi: SupervisorEmpleadoApi
) : ICuadranteRepository {

    override suspend fun getAsignacionesEquipo(): Result<List<RespuestaAsignacionTurnoDTO>> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = asignacionApiService.getAsignacionesEquipo()
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Result.success(respuesta.body()!!)
                } else {
                    Result.failure(
                        Exception(
                            extraerMensajeError(respuesta.errorBody())
                                ?: "Error ${respuesta.code()} al obtener el cuadrante"
                        )
                    )
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun crearAsignacion(
        peticion: PeticionAsignacionTurnoDTO
    ): Result<RespuestaAsignacionTurnoDTO> = withContext(Dispatchers.IO) {
        try {
            val respuesta = asignacionApiService.crearAsignacion(peticion)
            if (respuesta.isSuccessful && respuesta.body() != null) {
                Result.success(respuesta.body()!!)
            } else {
                Result.failure(
                    Exception(
                        extraerMensajeError(respuesta.errorBody())
                            ?: "Error ${respuesta.code()} al crear la asignación"
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTurnos(): Result<List<RespuestaTurnoDTO>> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = turnoApiService.getTurnos()
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Result.success(respuesta.body()!!)
                } else {
                    Result.failure(
                        Exception(
                            extraerMensajeError(respuesta.errorBody())
                                ?: "Error ${respuesta.code()} al obtener los turnos"
                        )
                    )
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getEmpleadosEquipo(): Result<List<RespuestaEmpleadoDTO>> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = supervisorEmpleadoApi.getEmpleadosEquipo()
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Result.success(respuesta.body()!!)
                } else {
                    Result.failure(
                        Exception(
                            extraerMensajeError(respuesta.errorBody())
                                ?: "Error ${respuesta.code()} al obtener los empleados"
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