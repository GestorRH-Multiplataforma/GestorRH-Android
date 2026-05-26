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
     * autenticado desde `GET /api/asignaciones`. El servidor filtra
     * automáticamente por el JWT.
     */
    suspend fun getAsignacionesEquipo(): Result<List<RespuestaAsignacionTurnoDTO>>

    /**
     * Crea una nueva asignación de turno para un empleado del equipo
     * mediante `POST /api/asignaciones`.
     *
     * @param peticion DTO con los datos validados de la asignación.
     * @return [Result.success] con la asignación creada, o [Result.failure]
     *         con el mensaje de error del servidor.
     */
    suspend fun crearAsignacion(
        peticion: PeticionAsignacionTurnoDTO
    ): Result<RespuestaAsignacionTurnoDTO>

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