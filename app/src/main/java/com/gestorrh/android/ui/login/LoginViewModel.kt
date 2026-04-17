package com.gestorrh.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manejador de la lógica de presentación y el estado para la pantalla de Autenticación.
 * Actúa como puente (Middleware) entre la Interfaz de Usuario (Jetpack Compose) y la capa
 * de dominio a través del [IAuthRepository], sin acoplarse a ningún detalle de red.
 * Utiliza flujos reactivos (StateFlow) para garantizar una actualización de la UI
 * segura y unidireccional.
 *
 * @property authRepository Dependencia del contrato de dominio para autenticación.
 * @property sessionManager Fuente de verdad de la sesión activa.
 */
class LoginViewModel(
    private val authRepository: IAuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiLogin())
    val estadoUi: StateFlow<EstadoUiLogin> = _estadoUi.asStateFlow()

    private val patronEmail = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    /**
     * Actualiza el estado reactivo con el nuevo correo electrónico introducido por el usuario
     * y lanza la validación del formulario.
     *
     * @param nuevoEmail Cadena de texto actual en el campo de correo.
     */
    fun actualizarEmail(nuevoEmail: String) {
        _estadoUi.update { estadoActual ->
            estadoActual.copy(email = nuevoEmail, mensajeError = null)
        }
        validarFormulario()
    }

    /**
     * Actualiza el estado reactivo con la nueva contraseña introducida por el usuario
     * y lanza la validación del formulario.
     *
     * @param nuevaPassword Cadena de texto actual en el campo de contraseña.
     */
    fun actualizarPassword(nuevaPassword: String) {
        _estadoUi.update { estadoActual ->
            estadoActual.copy(password = nuevaPassword, mensajeError = null)
        }
        validarFormulario()
    }

    /**
     * Ejecuta la petición de autenticación a través del repositorio.
     * La llamada se suspende en un hilo secundario (viewModelScope) para operar
     * asíncronamente sin bloquear la interfaz gráfica del móvil.
     */
    fun realizarLogin() {
        val estadoActual = _estadoUi.value

        _estadoUi.update {
            it.copy(estaCargando = true, mensajeError = null, botonLoginHabilitado = false)
        }

        viewModelScope.launch {
            authRepository.login(estadoActual.email, estadoActual.password)
                .onSuccess { respuesta ->
                    sessionManager.saveSession(respuesta.token, respuesta.nombre)
                    _estadoUi.update { it.copy(estaCargando = false, loginExitoso = true) }
                }
                .onFailure {
                    _estadoUi.update {
                        it.copy(
                            estaCargando = false,
                            mensajeError = MensajeUi.Recurso(R.string.login_error_credentials),
                            botonLoginHabilitado = true
                        )
                    }
                }
        }
    }

    /**
     * Motor de validación reactiva en local.
     * Comprueba si los datos cumplen los requisitos mínimos para habilitar el botón,
     * evitando enviar peticiones basura a la API.
     */
    private fun validarFormulario() {
        val email = _estadoUi.value.email
        val password = _estadoUi.value.password

        _estadoUi.update { estadoActual ->
            estadoActual.copy(
                botonLoginHabilitado = email.matches(patronEmail) && password.isNotEmpty()
            )
        }
    }
}
