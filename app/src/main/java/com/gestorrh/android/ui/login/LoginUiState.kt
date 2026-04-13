package com.gestorrh.android.ui.login

/**
 * Representa el estado inmutable de la pantalla de Login en cualquier instante de tiempo.
 */
data class LoginUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoginButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
