package com.gestorrh.android.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.data.local.GestorRhDatabase
import com.gestorrh.android.data.local.entity.FichajePendienteEntity
import com.gestorrh.android.data.network.fichaje.FichajeApiService
import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO

/**
 * Worker responsable de vaciar la cola de fichajes pendientes.
 *
 * Estrategia:
 *  - 2xx: fichaje aceptado, se elimina de Room.
 *  - 4xx: error definitivo (geovallado, datos inválidos); se elimina para no reintentar
 *    indefinidamente una petición que el servidor siempre rechazará.
 *  - 5xx o error de red: se incrementa el contador `intentos` y se devuelve `Result.retry()`
 *    para que WorkManager aplique backoff exponencial sobre el `WorkRequest`.
 */
class SyncFichajeWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    private val base = applicationContext
    private val sessionManager = SessionManager(base)
    private val fichajePendienteDao = GestorRhDatabase.getInstance(base).fichajePendienteDao()
    private val fichajeApiService: FichajeApiService = ApiClient.crearRetrofit(sessionManager)
        .create(FichajeApiService::class.java)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Iniciando sincronización de fichajes pendientes")
        val pendientes = fichajePendienteDao.obtenerTodos()

        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay fichajes pendientes")
            return Result.success()
        }

        var necesitaReintento = false

        pendientes.forEach { fichaje ->
            val resultado = procesar(fichaje)
            if (resultado == ResultadoSync.REINTENTAR) {
                fichajePendienteDao.incrementarIntentos(fichaje.id)
                necesitaReintento = true
            } else {
                fichajePendienteDao.eliminar(fichaje.id)
            }
        }

        return if (necesitaReintento) Result.retry() else Result.success()
    }

    private suspend fun procesar(fichaje: FichajePendienteEntity): ResultadoSync {
        return try {
            val respuesta = when (fichaje.tipo) {
                FichajePendienteEntity.TIPO_ENTRADA -> fichajeApiService.ficharEntrada(
                    PeticionFichajeEntradaDTO(fichaje.latitud, fichaje.longitud)
                )
                FichajePendienteEntity.TIPO_SALIDA -> fichajeApiService.ficharSalida(
                    PeticionFichajeSalidaDTO(fichaje.latitud, fichaje.longitud)
                )
                else -> {
                    Log.w(TAG, "Tipo de fichaje desconocido: ${fichaje.tipo}")
                    return ResultadoSync.DESCARTAR
                }
            }

            when {
                respuesta.isSuccessful -> {
                    Log.d(TAG, "Fichaje ${fichaje.id} sincronizado (${fichaje.tipo})")
                    ResultadoSync.EXITO
                }
                respuesta.code() in 400..499 -> {
                    Log.w(TAG, "Fichaje ${fichaje.id} rechazado (${respuesta.code()})")
                    ResultadoSync.DESCARTAR
                }
                else -> {
                    Log.e(TAG, "Error servidor sincronizando fichaje ${fichaje.id}: ${respuesta.code()}")
                    ResultadoSync.REINTENTAR
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción sincronizando fichaje ${fichaje.id}", e)
            ResultadoSync.REINTENTAR
        }
    }

    private enum class ResultadoSync { EXITO, DESCARTAR, REINTENTAR }

    companion object {
        private const val TAG = "SyncFichajeWorker"
    }
}
