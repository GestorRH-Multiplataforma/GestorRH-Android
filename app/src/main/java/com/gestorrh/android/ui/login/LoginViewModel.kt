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

/**
 * Manejador de la lógica de presentación y el estado para la pantalla de Autenticación.
 * Actúa como puente (Middleware) entre la Interfaz de Usuario (Jetpack Compose) y la capa de Red/Seguridad.
 * Utiliza flujos reactivos (StateFlow) para garantizar una actualización de la UI segura y unidireccional.
 *
 * @property authApi Dependencia para invocar los servicios de red de autenticación.
 * @property tokenManager Dependencia para interactuar con la caja fuerte del dispositivo (Keystore).
 */
class LoginViewModel(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiLogin())
    val estadoUi: StateFlow<EstadoUiLogin> = _estadoUi.asStateFlow()

    private val patronEmail = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    /**
     * Actualiza el estado reactivo con el nuevo correo electrónico introducido por el usuario
     * y lanza la validación matemática del formulario.
     *
     * @param nuevoEmail Cadena de texto actual en el campo de correo.
     */
    fun actualizarEmail(nuevoEmail: String) {
        _estadoUi.update { estadoActual ->
            estadoActual.copy(
                email = nuevoEmail,
                mensajeError = null
            )
        }
        validarFormulario()
    }

    /**
     * Actualiza el estado reactivo con la nueva contraseña introducida por el usuario
     * y lanza la validación matemática del formulario.
     *
     * @param nuevaPassword Cadena de texto actual en el campo de contraseña.
     */
    fun actualizarPassword(nuevaPassword: String) {
        _estadoUi.update { estadoActual ->
            estadoActual.copy(
                password = nuevaPassword,
                mensajeError = null
            )
        }
        validarFormulario()
    }

    /**
     * Ejecuta la petición de red para autenticar al empleado contra el servidor Spring Boot.
     * La llamada se suspende en un hilo secundario (viewModelScope) para operar
     * asíncronamente sin bloquear la interfaz gráfica del móvil.
     */
    fun realizarLogin() {
        val estadoActual = _estadoUi.value

        _estadoUi.update {
            it.copy(estaCargando = true, mensajeError = null, botonLoginHabilitado = false)
        }

        viewModelScope.launch {
            try {
                val peticion = PeticionLoginDTO(estadoActual.email, estadoActual.password)
                val respuesta = authApi.login(peticion)

                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val token = respuesta.body()!!.token
                    tokenManager.guardarToken(token)

                    _estadoUi.update { it.copy(estaCargando = false, loginExitoso = true) }
                } else {
                    _estadoUi.update {
                        it.copy(
                            estaCargando = false,
                            mensajeError = R.string.login_error_credentials,
                            botonLoginHabilitado = true
                        )
                    }
                }
            } catch (e: Exception) {
                _estadoUi.update {
                    it.copy(
                        estaCargando = false,
                        mensajeError = R.string.login_error_network,
                        botonLoginHabilitado = true
                    )
                }
            }
        }
    }

    /**
     * Motor de validación reactiva en local.
     * Comprueba si los datos cumplen los requisitos mínimos para
     * habilitar el botón, evitando enviar peticiones basura a la API.
     */
    private fun validarFormulario() {
        val email = _estadoUi.value.email
        val password = _estadoUi.value.password

        val emailValido = email.matches(patronEmail)
        val passwordValida = password.isNotEmpty()

        _estadoUi.update { estadoActual ->
            estadoActual.copy(botonLoginHabilitado = emailValido && passwordValida)
        }
    }
}