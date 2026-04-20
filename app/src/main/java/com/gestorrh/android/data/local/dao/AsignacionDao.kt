package com.gestorrh.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gestorrh.android.data.local.entity.AsignacionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder a la tabla `asignaciones`.
 *
 * Expone las operaciones mínimas necesarias para la estrategia offline-first
 * de la pestaña Mis Turnos: observación reactiva de la caché, inserción/actualización
 * masiva tras cada sincronización con el servidor y limpieza total al cerrar sesión.
 */
@Dao
interface AsignacionDao {

    /**
     * Inserta o actualiza todas las asignaciones recibidas del servidor.
     * Se usa al finalizar con éxito la llamada a `GET /api/asignaciones/me`.
     */
    @Upsert
    suspend fun upsertAll(asignaciones: List<AsignacionEntity>)

    /**
     * Devuelve un `Flow` con todas las asignaciones cacheadas ordenadas por fecha
     * ascendente. El `Flow` emite una nueva lista cada vez que cambian los datos
     * subyacentes, permitiendo a la UI actualizarse sin acción explícita.
     */
    @Query("SELECT * FROM asignaciones ORDER BY fecha ASC")
    fun getAll(): Flow<List<AsignacionEntity>>

    /**
     * Elimina todas las filas. Debe invocarse al cerrar sesión para no exponer
     * los turnos del usuario anterior al siguiente login.
     */
    @Query("DELETE FROM asignaciones")
    suspend fun deleteAll()
}
