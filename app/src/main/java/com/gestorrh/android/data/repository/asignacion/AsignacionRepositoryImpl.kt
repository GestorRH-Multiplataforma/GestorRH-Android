package com.gestorrh.android.data.repository.asignacion

import com.gestorrh.android.data.local.dao.AsignacionDao
import com.gestorrh.android.data.local.entity.AsignacionEntity
import com.gestorrh.android.data.local.mapper.toEntity
import com.gestorrh.android.data.network.asignacion.AsignacionApiService
import com.gestorrh.android.domain.repository.IAsignacionRepository
import com.gestorrh.android.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Implementación offline-first de [IAsignacionRepository].
 *
 * La caché de Room actúa como fuente única de verdad para la UI: el `Flow` expuesto
 * en [observarAsignaciones] emite siempre los datos locales. El método [sincronizar]
 * refresca la caché en segundo plano llamando al backend y escribiendo la respuesta
 * con `upsertAll`; la UI se actualiza automáticamente gracias a la reactividad de
 * Room. Si la red falla pero hay datos en caché, la UI seguirá mostrando la versión
 * guardada y el ViewModel decidirá el aviso al usuario.
 *
 * @param apiService Servicio Retrofit para los endpoints de asignaciones.
 * @param dao DAO Room para la tabla `asignaciones`.
 */
class AsignacionRepositoryImpl(
    private val apiService: AsignacionApiService,
    private val dao: AsignacionDao
) : IAsignacionRepository {

    override fun observarAsignaciones(): Flow<List<AsignacionEntity>> = dao.getAll()

    /**
     * Ejecuta el refresco contra `GET /api/asignaciones/me`.
     *
     * - Si el servidor responde 200, persiste la lista completa en Room con la
     *   marca de `fechaSincronizacion` actual. El `Flow` emitirá automáticamente.
     * - Si se produce un [IOException] (sin red, timeout) devuelve
     *   [ResultadoSincronizacion.SinConexion] para que la UI pueda avisar pero
     *   sin invalidar la caché existente.
     * - Cualquier otro fallo (4xx/5xx o parseo) se traduce a [ResultadoSincronizacion.Error]
     *   con el mensaje `"message"` extraído del cuerpo JSON cuando esté disponible.
     */
    override suspend fun sincronizar(): ResultadoSincronizacion = withContext(Dispatchers.IO) {
        try {
            val respuesta = apiService.getMisAsignaciones()
            if (respuesta.isSuccessful && respuesta.body() != null) {
                val ahora = System.currentTimeMillis()
                val entidades = respuesta.body()!!.map { it.toEntity(ahora) }
                dao.upsertAll(entidades)
                ResultadoSincronizacion.Exito
            } else {
                val mensaje = extraerMensajeError(respuesta.errorBody())
                    ?: "Error ${respuesta.code()} al obtener las asignaciones"
                ResultadoSincronizacion.Error(mensaje)
            }
        } catch (e: IOException) {
            ResultadoSincronizacion.SinConexion
        } catch (e: Exception) {
            ResultadoSincronizacion.Error(e.message ?: "Error desconocido")
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
