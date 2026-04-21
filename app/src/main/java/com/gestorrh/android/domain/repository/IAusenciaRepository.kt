package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.ausencia.PeticionAusenciaDTO
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO

/**
 * Contrato de dominio para las operaciones sobre ausencias del empleado autenticado.
 *
 * Concentra las operaciones necesarias para el flujo de creación, edición, listado
 * y gestión de justificantes: lectura del diccionario de tipos, envío de solicitudes
 * con `multipart/form-data` (parte JSON `datos` y parte binaria opcional `archivo`),
 * consulta del listado propio, actualización y cancelación, y descarga del justificante
 * ya persistido en el servidor.
 *
 * Las implementaciones devuelven [Result] envolviendo el cuerpo de respuesta en caso
 * de éxito o una [Throwable] cuyo mensaje sea apto para mostrar al usuario directamente
 * (extraído del campo `message` del JSON de error de la API cuando esté disponible).
 */
interface IAusenciaRepository {

    /**
     * Recupera la lista de tipos de ausencia válidos desde `GET /api/ausencias/tipos`.
     */
    suspend fun obtenerTipos(): Result<List<String>>

    /**
     * Envía una nueva solicitud de ausencia a `POST /api/ausencias` empaquetando
     * [peticion] como parte JSON `datos` y [archivoBytes] (opcional) como parte
     * binaria `archivo`.
     *
     * @param peticion Datos validados de la solicitud.
     * @param archivoBytes Contenido binario del justificante o `null` si no se adjunta.
     * @param nombreArchivo Nombre original del archivo, usado en el `filename` del multipart.
     */
    suspend fun crearAusencia(
        peticion: PeticionAusenciaDTO,
        archivoBytes: ByteArray?,
        nombreArchivo: String?
    ): Result<RespuestaAusenciaDTO>

    /**
     * Recupera las solicitudes de ausencia del empleado autenticado desde
     * `GET /api/ausencias/me`, admitiendo un filtro opcional por [estado].
     *
     * @param estado Valor del enum `EstadoAusencia` (`SOLICITADA`, `APROBADA`, `RECHAZADA`) o
     *        `null` para recuperar todas.
     */
    suspend fun obtenerMisAusencias(estado: String? = null): Result<List<RespuestaAusenciaDTO>>

    /**
     * Actualiza una ausencia existente mediante `PUT /api/ausencias/{id}` usando
     * `multipart/form-data`. El servidor solo permite la operación si la ausencia está
     * en estado `SOLICITADA`; en caso contrario devuelve 409/400 y el mensaje se
     * propaga tal cual desde el campo `message` del cuerpo de error.
     *
     * @param archivoBytes Bytes del nuevo justificante o `null` si no se desea sustituirlo.
     * @param nombreArchivo Nombre original del nuevo archivo (`filename` del multipart).
     */
    suspend fun actualizarAusencia(
        idAusencia: Long,
        peticion: PeticionAusenciaDTO,
        archivoBytes: ByteArray?,
        nombreArchivo: String?
    ): Result<RespuestaAusenciaDTO>

    /**
     * Cancela (elimina) una ausencia propia mediante `DELETE /api/ausencias/{id}`.
     * Solo permitido si la ausencia está en estado `SOLICITADA`.
     */
    suspend fun cancelarAusencia(idAusencia: Long): Result<Unit>

    /**
     * Descarga los bytes del justificante asociado a una ausencia desde
     * `GET /api/ausencias/justificantes/{nombreArchivo}`.
     */
    suspend fun descargarJustificante(nombreArchivo: String): Result<ByteArray>
}
