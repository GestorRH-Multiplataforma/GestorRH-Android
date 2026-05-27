package com.gestorrh.android.domain.usecase.fichaje

import com.gestorrh.android.data.network.fichaje.PeticionModificacionFichajeDTO
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import com.gestorrh.android.domain.repository.IFichajeRepository
import java.time.LocalTime

/**
 * Caso de uso que valida y ejecuta la modificación manual de un fichaje existente.
 *
 * Aplica las mismas reglas de negocio horarias que el servidor para dar feedback
 * inmediato al supervisor sin necesidad de un viaje de red innecesario:
 *
 * - **Turno diurno válido:** [nuevaHoraSalida] es estrictamente posterior a [nuevaHoraEntrada].
 * - **Turno nocturno válido:** [nuevaHoraEntrada] >= 16:00 y [nuevaHoraSalida] <= 08:00.
 *
 * La validación solo se aplica cuando se proporcionan **ambas** horas. Si solo se
 * proporciona una de las dos, la validación se delega íntegramente al servidor.
 *
 * El campo [motivoModificacion] es obligatorio por motivos legales y de auditoría;
 * si viene vacío o en blanco se devuelve [ErrorValidacion.MotivoVacio] sin llamada de red.
 *
 * @property fichajeRepository Contrato de dominio para las operaciones de fichaje.
 */
class ModificarFichajeUseCase(
    private val fichajeRepository: IFichajeRepository
) {

    sealed class ErrorValidacion(mensaje: String) : Exception(mensaje) {
        data object MotivoVacio : ErrorValidacion("motivo_vacio")
        data object HorasInvalidas : ErrorValidacion("horas_invalidas")
    }

    /**
     * Valida la petición y, si es correcta, delega la modificación en el repositorio.
     *
     * @param idFichaje Identificador del fichaje a corregir.
     * @param nuevaHoraEntrada Nueva hora de entrada, o null para no modificarla.
     * @param nuevaHoraSalida Nueva hora de salida, o null para no modificarla.
     * @param motivoModificacion Motivo obligatorio de la modificación para auditoría.
     * @return [Result.success] con el [RespuestaFichajeDTO] actualizado, o
     *         [Result.failure] con [ErrorValidacion] o el error del servidor.
     */
    suspend operator fun invoke(
        idFichaje: Long,
        nuevaHoraEntrada: java.time.LocalDateTime?,
        nuevaHoraSalida: java.time.LocalDateTime?,
        motivoModificacion: String
    ): Result<RespuestaFichajeDTO> {

        if (motivoModificacion.isBlank()) {
            return Result.failure(ErrorValidacion.MotivoVacio)
        }

        if (nuevaHoraEntrada != null && nuevaHoraSalida != null) {
            val horaInicio = nuevaHoraEntrada.toLocalTime()
            val horaFin = nuevaHoraSalida.toLocalTime()

            val esDiurnoValido = horaFin.isAfter(horaInicio)

            val esNocturnoValido = !horaInicio.isBefore(LocalTime.of(16, 0))
                    && !horaFin.isAfter(LocalTime.of(8, 0))

            if (!esDiurnoValido && !esNocturnoValido) {
                return Result.failure(ErrorValidacion.HorasInvalidas)
            }
        }

        return fichajeRepository.modificarFichaje(
            idFichaje = idFichaje,
            peticion = PeticionModificacionFichajeDTO(
                nuevaHoraEntrada = nuevaHoraEntrada,
                nuevaHoraSalida = nuevaHoraSalida,
                motivoModificacion = motivoModificacion
            )
        )
    }
}