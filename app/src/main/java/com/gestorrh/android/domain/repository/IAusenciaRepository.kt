package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.ausencia.PeticionAusenciaDTO
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO

/**
 * Contrato de dominio para las operaciones sobre ausencias del empleado autenticado.
 *
 * Concentra las dos operaciones que la pantalla de solicitud necesita: leer el
 * diccionario de tipos disponibles desde el servidor (para alimentar el desplegable
 * sin acoplarlo al enum local) y enviar una nueva solicitud usando `multipart/form-data`
 * con la parte JSON `datos` y la parte binaria opcional `archivo`.
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
     * Actualiza una ausencia existente mediante `PUT /api/ausencias/{id}` enviando
     * [peticion] como JSON. El servidor solo permite la operación si la ausencia está
     * en estado `SOLICITADA`; en caso contrario devuelve 409/400 y el mensaje se
     * propaga tal cual desde el campo `message` del cuerpo de error.
     */
    suspend fun actualizarAusencia(
        idAusencia: Long,
        peticion: PeticionAusenciaDTO
    ): Result<RespuestaAusenciaDTO>

    /**
     * Cancela (elimina) una ausencia propia mediante `DELETE /api/ausencias/{id}`.
     * Solo permitido si la ausencia está en estado `SOLICITADA`.
     */
    suspend fun cancelarAusencia(idAusencia: Long): Result<Unit>
}
