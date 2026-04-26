package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import com.gestorrh.android.data.network.fichaje.RespuestaEstadoFichajeDTO
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import java.time.LocalDate

/**
 * Contrato de la capa de dominio para las operaciones de fichaje.
 * Los ViewModels dependen de esta interfaz, nunca del servicio Retrofit directamente.
 * La implementación concreta vive en la capa de datos ([com.gestorrh.android.data.repository]).
 */
interface IFichajeRepository {

    /**
     * Consulta el estado de fichaje actual del empleado autenticado.
     *
     * @return [Result.success] con [RespuestaEstadoFichajeDTO], o [Result.failure] con
     *         el mensaje de error del servidor o de red.
     */
    suspend fun obtenerEstadoActual(): Result<RespuestaEstadoFichajeDTO>

    /**
     * Registra la entrada del empleado.
     *
     * @param peticion DTO con las coordenadas GPS (null si la modalidad es TELETRABAJO).
     * @return [Result.success] con [RespuestaFichajeDTO], o [Result.failure] con el
     *         mensaje de error del servidor (ej. empleado fuera del radio de la sede).
     */
    suspend fun ficharEntrada(peticion: PeticionFichajeEntradaDTO): Result<RespuestaFichajeDTO>

    /**
     * Registra la salida del empleado.
     *
     * @param peticion DTO con las coordenadas GPS (null si la modalidad es TELETRABAJO).
     * @return [Result.success] con [RespuestaFichajeDTO], o [Result.failure] con el
     *         mensaje de error del servidor.
     */
    suspend fun ficharSalida(peticion: PeticionFichajeSalidaDTO): Result<RespuestaFichajeDTO>

    /**
     * Recupera el historial de fichajes del empleado autenticado dentro de un rango
     * cerrado de fechas. El servidor filtra automáticamente por el JWT.
     *
     * @param fechaInicio Primer día incluido en la consulta.
     * @param fechaFin Último día incluido en la consulta.
     * @return [Result.success] con la lista de fichajes (puede venir vacía), o
     *         [Result.failure] con el mensaje de error del servidor o de red.
     */
    suspend fun obtenerHistorialFichajes(
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): Result<List<RespuestaFichajeDTO>>
}
