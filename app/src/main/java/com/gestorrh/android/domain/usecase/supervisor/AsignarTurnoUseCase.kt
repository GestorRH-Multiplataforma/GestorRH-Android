package com.gestorrh.android.domain.usecase.supervisor

import com.gestorrh.android.data.network.asignacion.ModalidadAsignacion
import com.gestorrh.android.data.network.asignacion.PeticionAsignacionTurnoDTO
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.domain.repository.ICuadranteRepository
import java.time.LocalDate

/**
 * Caso de uso que valida una asignación de turno antes de delegarla al repositorio.
 *
 * Validaciones aplicadas en cliente para evitar viajes de red innecesarios:
 * - El empleado debe estar seleccionado.
 * - El turno debe estar seleccionado.
 * - La fecha debe ser hoy o futura.
 * - La modalidad debe estar seleccionada.
 *
 * El servidor aplica validaciones adicionales de solapamiento (400) y
 * concurrencia (409) que se propagan tal cual al ViewModel.
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
    }

    suspend operator fun invoke(
        idEmpleado: Long?,
        idTurno: Long?,
        fecha: LocalDate?,
        modalidad: ModalidadAsignacion?,
        motivoCambio: String? = null,
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

        val peticion = PeticionAsignacionTurnoDTO(
            idEmpleado = idEmpleado,
            idTurno = idTurno,
            fecha = fecha,
            modalidad = modalidad,
            motivoCambio = motivoCambio?.takeIf { it.isNotBlank() }
        )

        return repository.crearAsignacion(peticion)
    }
}