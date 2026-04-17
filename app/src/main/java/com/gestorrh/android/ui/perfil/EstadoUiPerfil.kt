package com.gestorrh.android.ui.perfil

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO

data class EstadoUiPerfil(
    val estaCargando: Boolean = true,
    val perfil: RespuestaEmpleadoDTO? = null,
    val mensajeError: MensajeUi? = null,
    val mostrarDialogLogout: Boolean = false,
    val mostrarDialogCambioPassword: Boolean = false,
    val passwordActual: String = "",
    val nuevaPassword: String = "",
    val errorDialogPassword: String? = null,
    val estaCambiandoPassword: Boolean = false,
    val mensajeExito: MensajeUi? = null
)
