package com.gestorrh.android.domain.usecase.supervisor

import com.gestorrh.android.data.network.ausencia.EstadoAusencia
import com.gestorrh.android.data.network.ausencia.PeticionRevisionAusenciaDTO
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.domain.repository.IAusenciaRepository

/**
 * Caso de uso que valida y ejecuta la revisión (aprobación o rechazo) de una
 * solicitud de ausencia por parte del supervisor.
 *
 * Validaciones aplicadas antes de la llamada de red:
 * - El estado destino debe ser [EstadoAusencia.APROBADA] o [EstadoAusencia.RECHAZADA].
 *   Intentar revisar dejando el estado en [EstadoAusencia.SOLICITADA] es un error
 *   de lógica que se rechaza en cliente sin necesidad de un viaje de red.
 * - Las observaciones son opcionales en ambos casos: el servidor las acepta como null.
 *
 * Si la validación es correcta delega en [IAusenciaRepository.revisarAusencia].
 * Los errores de red o de negocio devueltos por el servidor (400/404) se propagan
 * tal cual como [Result.failure] con el mensaje extraído del campo `message` del JSON.
 *
 * @property repository Contrato de dominio para las operaciones de ausencia.
 */
class RevisarAusenciaUseCase(
    private val repository: IAusenciaRepository
) {

    sealed class ErrorValidacion(mensaje: String) : Exception(mensaje) {
        data object EstadoInvalido : ErrorValidacion("estado_invalido")
    }

    suspend operator fun invoke(
        idAusencia: Long,
        estado: EstadoAusencia,
        observaciones: String? = null
    ): Result<RespuestaAusenciaDTO> {
        if (estado == EstadoAusencia.SOLICITADA) {
            return Result.failure(ErrorValidacion.EstadoInvalido)
        }

        val peticion = PeticionRevisionAusenciaDTO(
            estado = estado.name,
            observacionesRevision = observaciones?.takeIf { it.isNotBlank() }
        )

        return repository.revisarAusencia(idAusencia, peticion)
    }
}