package com.gestorrh.android.domain.usecase.ausencia

import com.gestorrh.android.data.network.ausencia.PeticionAusenciaDTO
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.domain.repository.IAusenciaRepository
import java.time.LocalDate

/**
 * Caso de uso que valida una solicitud de ausencia antes de delegarla al repositorio.
 *
 * Las validaciones replican las reglas que el servidor también aplica, pero se ejecutan
 * en el cliente para evitar viajes de red innecesarios y poder anclar el mensaje de error
 * al campo concreto del formulario. Si alguna regla falla devuelve [Result.failure] con
 * un identificador estable en el mensaje (`fechaInicio_pasada`, `fechaFin_anterior`) para
 * que la capa de presentación pueda traducirlo al recurso de string adecuado.
 *
 * Reglas aplicadas:
 * - `fechaInicio` debe ser hoy o futura.
 * - `fechaFin` debe ser igual o posterior a `fechaInicio`.
 *
 * Si la entrada es válida invoca a [IAusenciaRepository.crearAusencia] reenviando el
 * justificante opcional sin modificarlo.
 */
class SolicitarAusenciaUseCase(
    private val repository: IAusenciaRepository
) {

    sealed class ErrorValidacion(mensaje: String) : Exception(mensaje) {
        data object FechaInicioPasada : ErrorValidacion("fechaInicio_pasada")
        data object FechaFinAnterior : ErrorValidacion("fechaFin_anterior")
        data object TipoVacio : ErrorValidacion("tipo_vacio")
    }

    suspend operator fun invoke(
        tipo: String?,
        descripcion: String?,
        fechaInicio: LocalDate?,
        fechaFin: LocalDate?,
        archivoBytes: ByteArray?,
        nombreArchivo: String?,
        idAusenciaEditar: Long? = null,
        hoy: LocalDate = LocalDate.now()
    ): Result<RespuestaAusenciaDTO> {
        if (tipo.isNullOrBlank()) {
            return Result.failure(ErrorValidacion.TipoVacio)
        }
        if (fechaInicio == null || fechaInicio.isBefore(hoy)) {
            return Result.failure(ErrorValidacion.FechaInicioPasada)
        }
        if (fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            return Result.failure(ErrorValidacion.FechaFinAnterior)
        }

        val peticion = PeticionAusenciaDTO(
            tipo = tipo,
            descripcion = descripcion?.takeIf { it.isNotBlank() },
            fechaInicio = fechaInicio,
            fechaFin = fechaFin
        )
        return if (idAusenciaEditar != null) {
            repository.actualizarAusencia(idAusenciaEditar, peticion)
        } else {
            repository.crearAusencia(peticion, archivoBytes, nombreArchivo)
        }
    }
}
