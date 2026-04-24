package com.gestorrh.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fichaje generado en un entorno sin conectividad. Se persiste localmente hasta que
 * `SyncFichajeWorker` logre entregarlo al backend. El campo `timestamp` almacena el
 * instante real del clic en milisegundos (epoch) para preservar el orden cronológico
 * cuando se reenvíen varios fichajes acumulados. `intentos` se utiliza como métrica
 * interna de diagnóstico para detectar fichajes que no pueden sincronizarse.
 */
@Entity(tableName = "fichajes_pendientes")
data class FichajePendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val latitud: Double?,
    val longitud: Double?,
    val timestamp: Long,
    val intentos: Int = 0
) {
    companion object {
        const val TIPO_ENTRADA = "ENTRADA"
        const val TIPO_SALIDA = "SALIDA"
    }
}
