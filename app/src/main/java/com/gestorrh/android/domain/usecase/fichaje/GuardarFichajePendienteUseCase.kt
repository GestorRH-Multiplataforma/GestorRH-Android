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
    /**
     * Encola un nuevo fichaje pendiente con la marca de tiempo actual.
     *
     * @param tipo Discriminante del fichaje (ENTRADA o SALIDA) tal como lo espera la API.
     * @param latitud Coordenada GPS si el turno es presencial; `null` para teletrabajo.
     * @param longitud Coordenada GPS si el turno es presencial; `null` para teletrabajo.
     * @return [Result.success] con el `id` autogenerado de la fila Room insertada, o
     *         [Result.failure] envolviendo la excepción de I/O si la inserción falla.
     */
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
