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
 * - El archivo adjunto (si existe) debe tener extensión soportada (pdf/jpg/jpeg/png)
 *   y no superar el tamaño máximo admitido.
 *
 * Si la entrada es válida invoca al método correspondiente del repositorio (crear o
 * actualizar) reenviando el justificante opcional sin modificarlo.
 */
class SolicitarAusenciaUseCase(
    private val repository: IAusenciaRepository
) {

    sealed class ErrorValidacion(mensaje: String) : Exception(mensaje) {
        data object FechaFinAnterior : ErrorValidacion("fechaFin_anterior")
        data object TipoVacio : ErrorValidacion("tipo_vacio")
        data object ArchivoTipoNoSoportado : ErrorValidacion("archivo_tipo_no_soportado")
        data object ArchivoDemasiadoGrande : ErrorValidacion("archivo_demasiado_grande")
    }

    suspend operator fun invoke(
        tipo: String?,
        descripcion: String?,
        fechaInicio: LocalDate?,
        fechaFin: LocalDate?,
        archivoBytes: ByteArray?,
        nombreArchivo: String?,
        idAusenciaEditar: Long? = null,
        eliminarJustificante: Boolean? = null
    ): Result<RespuestaAusenciaDTO> {
        if (tipo.isNullOrBlank()) {
            return Result.failure(ErrorValidacion.TipoVacio)
        }
        if (fechaInicio == null) {
            return Result.failure(ErrorValidacion.FechaFinAnterior)
        }
        if (fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            return Result.failure(ErrorValidacion.FechaFinAnterior)
        }
        if (archivoBytes != null) {
            if (!extensionSoportada(nombreArchivo)) {
                return Result.failure(ErrorValidacion.ArchivoTipoNoSoportado)
            }
            if (archivoBytes.size > TAMANO_MAXIMO_BYTES) {
                return Result.failure(ErrorValidacion.ArchivoDemasiadoGrande)
            }
        }

        val peticion = PeticionAusenciaDTO(
            tipo = tipo,
            descripcion = descripcion?.takeIf { it.isNotBlank() },
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            eliminarJustificante = eliminarJustificante
        )
        return if (idAusenciaEditar != null) {
            repository.actualizarAusencia(idAusenciaEditar, peticion, archivoBytes, nombreArchivo)
        } else {
            repository.crearAusencia(peticion, archivoBytes, nombreArchivo)
        }
    }

    private fun extensionSoportada(nombre: String?): Boolean {
        val extension = nombre?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return extension in EXTENSIONES_SOPORTADAS
    }

    companion object {
        const val TAMANO_MAXIMO_BYTES: Long = 10L * 1024 * 1024
        val EXTENSIONES_SOPORTADAS: Set<String> = setOf("pdf", "jpg", "jpeg", "png")
    }
}
