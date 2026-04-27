package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO

/**
 * Contrato de dominio para las operaciones de perfil del empleado autenticado.
 */
interface IPerfilRepository {

    /**
     * Recupera el perfil del empleado autenticado a partir del JWT inyectado por
     * el interceptor de red.
     *
     * @return [Result.success] con [RespuestaEmpleadoDTO], o [Result.failure] con
     *         el mensaje extraído del cuerpo de error de la API.
     */
    suspend fun obtenerMiPerfil(): Result<RespuestaEmpleadoDTO>

    /**
     * Cambia la contraseña del empleado autenticado.
     *
     * @param passwordActual Contraseña vigente, requerida por el servidor para validar la autoría.
     * @param nuevaPassword Nueva contraseña que sustituirá a la actual.
     * @return [Result.success] vacío si el servidor confirma el cambio, o
     *         [Result.failure] con el mensaje de error correspondiente.
     */
    suspend fun cambiarContrasena(passwordActual: String, nuevaPassword: String): Result<Unit>
}
