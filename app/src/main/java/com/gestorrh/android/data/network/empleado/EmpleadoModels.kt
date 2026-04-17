package com.gestorrh.android.data.network.empleado

import java.time.LocalDateTime

data class RespuestaEmpleadoDTO(
    val idEmpleado: Long,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val telefono: String?,
    val puesto: String?,
    val departamento: String?,
    val rol: String,
    val activo: Boolean,
    val fechaBajaContrato: LocalDateTime?
)

data class PeticionCambiarPasswordDTO(
    val passwordActual: String,
    val nuevaPassword: String
)
