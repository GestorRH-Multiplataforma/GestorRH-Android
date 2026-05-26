package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.asignacion.PeticionAsignacionTurnoDTO
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.turno.RespuestaTurnoDTO

/**
 * Contrato de dominio para las operaciones del cuadrante del departamento.
 * Usado exclusivamente por el rol SUPERVISOR. No implementa caché local:
 * los datos deben ser siempre frescos para evitar asignaciones sobre
 * información desactualizada.
 */
interface ICuadranteRepository {

    /**
     * Recupera todas las asignaciones del departamento del supervisor
     * autenticado desde `GET /api/asignaciones`.
     */
    suspend fun getAsignacionesEquipo(): Result<List<RespuestaAsignacionTurnoDTO>>

    /**
     * Crea una nueva asignación de turno para un empleado del equipo
     * mediante `POST /api/asignaciones`.
     */
    suspend fun crearAsignacion(
        peticion: PeticionAsignacionTurnoDTO
    ): Result<RespuestaAsignacionTurnoDTO>

    /**
     * Modifica una asignación existente mediante `PUT /api/asignaciones/{id}`.
     * El motivo de cambio es obligatorio para la auditoría.
     *
     * @param id Identificador de la asignación a modificar.
     * @param peticion DTO con los nuevos datos y el motivo del cambio.
     */
    suspend fun actualizarAsignacion(
        id: Long,
        peticion: PeticionAsignacionTurnoDTO
    ): Result<RespuestaAsignacionTurnoDTO>

    /**
     * Elimina de forma permanente una asignación mediante
     * `DELETE /api/asignaciones/{id}`.
     *
     * @param id Identificador de la asignación a eliminar.
     */
    suspend fun eliminarAsignacion(id: Long): Result<Unit>

    /**
     * Recupera el catálogo de plantillas de turno disponibles
     * desde `GET /api/turnos`.
     */
    suspend fun getTurnos(): Result<List<RespuestaTurnoDTO>>

    /**
     * Recupera la lista de empleados del departamento del supervisor
     * autenticado desde `GET /api/empleados`.
     */
    suspend fun getEmpleadosEquipo(): Result<List<RespuestaEmpleadoDTO>>
}