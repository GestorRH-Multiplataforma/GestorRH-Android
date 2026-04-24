package com.gestorrh.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestorrh.android.data.local.entity.FichajePendienteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a la tabla `fichajes_pendientes` utilizada para la cola offline de fichajes.
 * Los fichajes se listan por `timestamp` ascendente para garantizar que las entradas
 * se reenvían antes que las salidas, respetando la secuencia que el usuario registró.
 */
@Dao
interface FichajePendienteDao {

    @Query("SELECT * FROM fichajes_pendientes ORDER BY timestamp ASC")
    suspend fun obtenerTodos(): List<FichajePendienteEntity>

    @Insert
    suspend fun insertar(fichaje: FichajePendienteEntity): Long

    @Query("DELETE FROM fichajes_pendientes WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("SELECT COUNT(*) FROM fichajes_pendientes")
    suspend fun contarPendientes(): Int

    @Query("SELECT COUNT(*) FROM fichajes_pendientes")
    fun observarContadorPendientes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM fichajes_pendientes WHERE tipo = :tipo")
    suspend fun contarPorTipo(tipo: String): Int

    @Query("UPDATE fichajes_pendientes SET intentos = intentos + 1 WHERE id = :id")
    suspend fun incrementarIntentos(id: Long)
}
