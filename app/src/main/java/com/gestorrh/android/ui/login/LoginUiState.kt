package com.gestorrh.android.ui.login

import androidx.annotation.StringRes

/**
 * Representa el estado inmutable de la pantalla de Login en cualquier instante de tiempo.
 */
data class LoginUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoginButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val isLoginSuccessful: Boolean = false
)
