package com.gestorrh.android.domain.usecase.fichaje

import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import com.gestorrh.android.domain.repository.IFichajeRepository
import java.time.LocalDate

/**
 * Caso de uso que obtiene el historial de fichajes del empleado autenticado
 * dentro de un rango cerrado de fechas y lo entrega ya ordenado para presentación.
 *
 * El servidor filtra automáticamente la lista por el JWT del cliente, por lo que no
 * es necesario pasar el `idEmpleado`. Tras delegar la consulta al repositorio, esta
 * capa ordena los fichajes por hora de entrada de forma descendente, dejando el más
 * reciente en primera posición — orden esperado por la pantalla de historial.
 *
 * @property fichajeRepository Contrato de dominio para las operaciones de fichaje.
 */
class ObtenerHistorialFichajesUseCase(
    private val fichajeRepository: IFichajeRepository
) {

    /**
     * @param fechaInicio Primer día del rango (inclusive).
     * @param fechaFin Último día del rango (inclusive).
     * @return [Result.success] con la lista ordenada del más reciente al más antiguo,
     *         o [Result.failure] con el mensaje de error propagado por el servidor.
     */
    suspend operator fun invoke(
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): Result<List<RespuestaFichajeDTO>> {
        return fichajeRepository.obtenerHistorialFichajes(fechaInicio, fechaFin)
            .map { fichajes -> fichajes.sortedByDescending { it.horaEntrada } }
    }
}
