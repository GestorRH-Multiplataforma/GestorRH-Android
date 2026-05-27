package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.fichaje.PeticionFichajeEntradaDTO
import com.gestorrh.android.data.network.fichaje.PeticionFichajeSalidaDTO
import com.gestorrh.android.data.network.fichaje.PeticionModificacionFichajeDTO
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

    /**
     * Obtiene el historial de fichajes de un empleado concreto del equipo del supervisor.
     * Si [empleadoId] es null, devuelve los fichajes de todos los empleados del departamento.
     *
     * @param fechaInicio Primer día del rango (inclusive).
     * @param fechaFin Último día del rango (inclusive).
     * @param empleadoId Identificador del empleado a filtrar, o null para todos.
     */
    suspend fun obtenerHistorialFichajesSupervisor(
        fechaInicio: LocalDate,
        fechaFin: LocalDate,
        empleadoId: Long?
    ): Result<List<RespuestaFichajeDTO>>

    /**
     * Modifica manualmente la hora de entrada o salida de un fichaje existente.
     * Solo accesible para SUPERVISOR o EMPRESA. La modificación queda registrada
     * en auditoría mediante el campo [motivoModificacion].
     *
     * @param idFichaje Identificador del fichaje a corregir.
     * @param peticion DTO con las nuevas horas y el motivo obligatorio de auditoría.
     * @return [Result.success] con el fichaje actualizado, o [Result.failure] con el
     *         mensaje de error del servidor.
     */
    suspend fun modificarFichaje(
        idFichaje: Long,
        peticion: PeticionModificacionFichajeDTO
    ): Result<RespuestaFichajeDTO>
}
