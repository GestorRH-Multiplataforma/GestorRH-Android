package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.local.entity.AsignacionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Resultado de una sincronización con el servidor.
 *
 * Permite al ViewModel distinguir entre un refresco correcto, una caída de red
 * (donde puede seguir mostrando la caché) y un error explícito de servidor.
 */
sealed class ResultadoSincronizacion {
    data object Exito : ResultadoSincronizacion()
    data object SinConexion : ResultadoSincronizacion()
    data class Error(val mensaje: String) : ResultadoSincronizacion()
}

/**
 * Contrato de la capa de dominio para las operaciones de asignación de turnos.
 * Sigue una estrategia offline-first: la UI observa siempre la caché local como
 * fuente única de verdad, y el refresco con el servidor se lanza por separado.
 */
interface IAsignacionRepository {

    /**
     * Flujo reactivo con las asignaciones persistidas en Room, ordenadas por fecha.
     * Emite una nueva lista cada vez que la caché local cambia (por ejemplo, tras
     * una sincronización exitosa o un `deleteAll()` al cerrar sesión).
     */
    fun observarAsignaciones(): Flow<List<AsignacionEntity>>

    /**
     * Lanza la petición `GET /api/asignaciones/me` y, si es correcta, escribe los
     * datos recibidos en Room con la marca de `fechaSincronizacion` actual.
     *
     * No emite datos directamente: los consumidores reaccionan al `Flow` de
     * [observarAsignaciones]. Devuelve el resultado de la operación de red para
     * que la capa superior pueda informar al usuario (Snackbar, estado de error).
     */
    suspend fun sincronizar(): ResultadoSincronizacion
}
