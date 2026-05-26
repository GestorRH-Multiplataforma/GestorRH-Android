package com.gestorrh.android.domain.usecase.supervisor

import com.gestorrh.android.data.network.asignacion.ModalidadAsignacion
import com.gestorrh.android.data.network.asignacion.PeticionAsignacionTurnoDTO
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.domain.repository.ICuadranteRepository
import java.time.LocalDate

/**
 * Caso de uso que valida y ejecuta la creación o edición de una asignación de turno.
 *
 * En modo creación (`idAsignacion == null`):
 * - Valida que empleado, turno, fecha (hoy o futura) y modalidad no sean nulos.
 *
 * En modo edición (`idAsignacion != null`):
 * - Aplica las mismas validaciones anteriores.
 * - Además valida que el motivo de cambio no esté vacío, requerido por el
 *   servidor para auditoría.
 *
 * @param repository Contrato de dominio para las operaciones del cuadrante.
 */
class AsignarTurnoUseCase(
    private val repository: ICuadranteRepository
) {

    sealed class ErrorValidacion(mensaje: String) : Exception(mensaje) {
        data object EmpleadoNoSeleccionado : ErrorValidacion("empleado_no_seleccionado")
        data object TurnoNoSeleccionado : ErrorValidacion("turno_no_seleccionado")
        data object FechaPasada : ErrorValidacion("fecha_pasada")
        data object ModalidadNoSeleccionada : ErrorValidacion("modalidad_no_seleccionada")
        data object MotivoVacio : ErrorValidacion("motivo_vacio")
    }

    suspend operator fun invoke(
        idEmpleado: Long?,
        idTurno: Long?,
        fecha: LocalDate?,
        modalidad: ModalidadAsignacion?,
        motivoCambio: String? = null,
        idAsignacion: Long? = null,
        hoy: LocalDate = LocalDate.now()
    ): Result<RespuestaAsignacionTurnoDTO> {

        if (idEmpleado == null) {
            return Result.failure(ErrorValidacion.EmpleadoNoSeleccionado)
        }
        if (idTurno == null) {
            return Result.failure(ErrorValidacion.TurnoNoSeleccionado)
        }
        if (fecha == null || fecha.isBefore(hoy)) {
            return Result.failure(ErrorValidacion.FechaPasada)
        }
        if (modalidad == null) {
            return Result.failure(ErrorValidacion.ModalidadNoSeleccionada)
        }
        if (idAsignacion != null && motivoCambio.isNullOrBlank()) {
            return Result.failure(ErrorValidacion.MotivoVacio)
        }

        val peticion = PeticionAsignacionTurnoDTO(
            idEmpleado = idEmpleado,
            idTurno = idTurno,
            fecha = fecha,
            modalidad = modalidad,
            motivoCambio = motivoCambio?.takeIf { it.isNotBlank() }
        )

        return if (idAsignacion != null) {
            repository.actualizarAsignacion(idAsignacion, peticion)
        } else {
            repository.crearAsignacion(peticion)
        }
    }
}