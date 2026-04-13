package com.gestorrh.android.ui.login

import androidx.annotation.StringRes

/**
 * Representa el estado inmutable de la Interfaz de Usuario (UI) para la pantalla de autenticación.
 * En Jetpack Compose, cualquier cambio visual se produce al emitir una nueva copia de esta clase,
 * garantizando un flujo de datos unidireccional (UDF) libre de condiciones de carrera (Race Conditions).
 *
 * @property email Valor actual introducido en el campo de correo electrónico.
 * @property password Valor actual introducido en el campo de contraseña.
 * @property botonLoginHabilitado Indica si el botón de acceso debe ser interactuable (se activa al cumplir la validación local).
 * @property estaCargando Activa los indicadores visuales de espera mientras el servidor procesa la petición de red.
 * @property mensajeError Identificador del recurso de string (StringRes) que contiene el error a mostrar (ej. "Credenciales inválidas"). Null si no hay errores.
 * @property loginExitoso Bandera de un solo uso (One-Time Event) que notifica a la vista que debe ejecutar la navegación hacia el Dashboard.
 */
data class EstadoUiLogin(
    val email: String = "",
    val password: String = "",
    val botonLoginHabilitado: Boolean = false,
    val estaCargando: Boolean = false,
    @StringRes val mensajeError: Int? = null,
    val loginExitoso: Boolean = false
)
