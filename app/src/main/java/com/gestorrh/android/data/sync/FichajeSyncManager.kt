package com.gestorrh.android.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Punto único para encolar el `SyncFichajeWorker`. Usa `ExistingWorkPolicy.KEEP`
 * para no duplicar trabajos cuando el usuario acumula varios fichajes offline
 * consecutivos y un backoff exponencial para no martillear al backend cuando la
 * causa del fallo sea transitoria.
 */
object FichajeSyncManager {

    private const val NOMBRE_TRABAJO = "sync_fichajes"

    fun encolarSincronizacion(contexto: Context) {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val peticion = OneTimeWorkRequestBuilder<SyncFichajeWorker>()
            .setConstraints(restricciones)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.KEEP, peticion)
    }
}
