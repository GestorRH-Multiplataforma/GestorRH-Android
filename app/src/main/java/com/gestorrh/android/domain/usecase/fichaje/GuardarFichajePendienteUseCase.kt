package com.gestorrh.android.domain.usecase.fichaje

import com.gestorrh.android.data.local.dao.FichajePendienteDao
import com.gestorrh.android.data.local.entity.FichajePendienteEntity

/**
 * Persiste un fichaje en la cola offline cuando no hay conectividad.
 * `SyncFichajeWorker` consumirá esta cola en cuanto WorkManager detecte red.
 */
class GuardarFichajePendienteUseCase(
    private val fichajePendienteDao: FichajePendienteDao
) {
    suspend operator fun invoke(
        tipo: String,
        latitud: Double?,
        longitud: Double?
    ): Result<Long> = try {
        val entidad = FichajePendienteEntity(
            tipo = tipo,
            latitud = latitud,
            longitud = longitud,
            timestamp = System.currentTimeMillis()
        )
        Result.success(fichajePendienteDao.insertar(entidad))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
