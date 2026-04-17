package com.gestorrh.android.domain.repository

import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO

/**
 * Contrato de dominio para las operaciones de perfil del empleado autenticado.
 */
interface IPerfilRepository {
    suspend fun obtenerMiPerfil(): Result<RespuestaEmpleadoDTO>
    suspend fun cambiarContrasena(passwordActual: String, nuevaPassword: String): Result<Unit>
}
