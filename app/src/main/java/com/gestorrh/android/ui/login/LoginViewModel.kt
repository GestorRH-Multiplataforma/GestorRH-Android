package com.gestorrh.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.core.security.TokenManager
import com.gestorrh.android.data.network.autenticacion.AuthApi
import com.gestorrh.android.data.network.autenticacion.PeticionLoginDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.gestorrh.android.R

class LoginViewModel (
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
): ViewModel() {

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

    fun performLogin() {
        val currentState = _uiState.value

        _uiState.update { it.copy(isLoading = true, errorMessage = null, isLoginButtonEnabled = false) }

        viewModelScope.launch {
            try {
                val peticion = PeticionLoginDTO(currentState.emailInput, currentState.passwordInput)
                val response = authApi.login(peticion)

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    tokenManager.saveToken(token)

                    _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = R.string.login_error_credentials,
                        isLoginButtonEnabled = true
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = R.string.login_error_network,
                    isLoginButtonEnabled = true
                )}
            }
        }
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