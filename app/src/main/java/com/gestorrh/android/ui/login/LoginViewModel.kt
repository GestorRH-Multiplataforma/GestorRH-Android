package com.gestorrh.android.ui.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel : ViewModel() {

    // Estado interno que solo el ViewModel puede modificar
    private val _uiState = MutableStateFlow(LoginUiState())
    // Estado de solo lectura al que se suscribirá la pantalla
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    /**
     * Invocado por la UI cada vez que el usuario teclea en el campo de Email.
     */
    fun onEmailChange(newEmail: String) {
        _uiState.update { currentState ->
            currentState.copy(
                emailInput = newEmail,
                errorMessage = null
            )
        }
        validateForm()
    }

    /**
     * Invocado por la UI cada vez que el usuario teclea en el campo de Contraseña.
     */
    fun onPasswordChange(newPassword: String) {
        _uiState.update { currentState ->
            currentState.copy(
                passwordInput = newPassword,
                errorMessage = null
            )
        }
        validateForm()
    }

    /**
     * Motor de validación reactiva. Comprueba matemáticamente si los datos cumplen
     * los requisitos para habilitar el botón de envío a tu API.
     */
    private fun validateForm() {
        val email = _uiState.value.emailInput
        val password = _uiState.value.passwordInput

        val isEmailValid = email.matches(emailPattern)
        val isPasswordValid = password.isNotEmpty()
        _uiState.update { currentState ->
            currentState.copy(isLoginButtonEnabled = isEmailValid && isPasswordValid)
        }
    }
}