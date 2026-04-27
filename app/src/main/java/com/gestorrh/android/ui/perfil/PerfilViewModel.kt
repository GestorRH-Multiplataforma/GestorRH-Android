package com.gestorrh.android.ui.perfil

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gestorrh.android.R
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.local.GestorRhDatabase
import com.gestorrh.android.data.local.dao.AsignacionDao
import com.gestorrh.android.data.network.empleado.EmpleadoApi
import com.gestorrh.android.data.repository.PerfilRepository
import com.gestorrh.android.domain.repository.IPerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manejador de estado y lógica de presentación para la pantalla de Perfil.
 * Orquesta la carga del perfil del empleado autenticado, el flujo de logout
 * y el cambio de contraseña, delegando en [IPerfilRepository] para todas las
 * operaciones de red.
 *
 * @property perfilRepository Contrato de dominio para operaciones de perfil.
 * @property sessionManager Fuente de verdad de la sesión activa.
 */
class PerfilViewModel(
    private val perfilRepository: IPerfilRepository,
    private val sessionManager: SessionManager,
    private val asignacionDao: AsignacionDao
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoUiPerfil())
    val estadoUi: StateFlow<EstadoUiPerfil> = _estadoUi.asStateFlow()

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true, mensajeError = null) }
            perfilRepository.obtenerMiPerfil()
                .onSuccess { perfil ->
                    _estadoUi.update { it.copy(estaCargando = false, perfil = perfil) }
                }
                .onFailure { e ->
                    val mensaje = if (e.message != null) MensajeUi.Dinamico(e.message!!)
                    else MensajeUi.Recurso(R.string.error_conexion)
                    _estadoUi.update { it.copy(estaCargando = false, mensajeError = mensaje) }
                }
        }
    }

    fun mostrarDialogLogout() {
        _estadoUi.update { it.copy(mostrarDialogLogout = true) }
    }

    fun ocultarDialogLogout() {
        _estadoUi.update { it.copy(mostrarDialogLogout = false) }
    }

    fun cerrarSesion(alCerrarSesion: () -> Unit) {
        sessionManager.clearSession()
        viewModelScope.launch {
            asignacionDao.deleteAll()
            alCerrarSesion()
        }
    }

    fun mostrarDialogCambioPassword() {
        _estadoUi.update {
            it.copy(
                mostrarDialogCambioPassword = true,
                passwordActual = "",
                nuevaPassword = "",
                errorDialogPassword = null
            )
        }
    }

    fun ocultarDialogCambioPassword() {
        _estadoUi.update { it.copy(mostrarDialogCambioPassword = false) }
    }

    fun actualizarPasswordActual(valor: String) {
        _estadoUi.update { it.copy(passwordActual = valor, errorDialogPassword = null) }
    }

    fun actualizarNuevaPassword(valor: String) {
        _estadoUi.update { it.copy(nuevaPassword = valor, errorDialogPassword = null) }
    }

    fun cambiarPassword(mensajeErrorMinimo: String) {
        val estado = _estadoUi.value
        if (estado.nuevaPassword.length < 8) {
            _estadoUi.update { it.copy(errorDialogPassword = mensajeErrorMinimo) }
            return
        }

        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCambiandoPassword = true, errorDialogPassword = null) }
            perfilRepository.cambiarContrasena(estado.passwordActual, estado.nuevaPassword)
                .onSuccess {
                    _estadoUi.update {
                        it.copy(
                            estaCambiandoPassword = false,
                            mostrarDialogCambioPassword = false,
                            mensajeExito = MensajeUi.Recurso(R.string.perfil_password_cambiada_exito)
                        )
                    }
                }
                .onFailure { e ->
                    _estadoUi.update {
                        it.copy(
                            estaCambiandoPassword = false,
                            errorDialogPassword = e.message
                        )
                    }
                }
        }
    }

    fun exitoMostrado() {
        _estadoUi.update { it.copy(mensajeExito = null) }
    }

    companion object {
        fun crearFactory(contexto: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val sessionManager = SessionManager(contexto)
                    val retrofit = ApiClient.crearRetrofit(sessionManager)
                    val empleadoApi = retrofit.create(EmpleadoApi::class.java)
                    val perfilRepository = PerfilRepository(empleadoApi)
                    val asignacionDao = GestorRhDatabase.getInstance(contexto).asignacionDao()
                    return PerfilViewModel(perfilRepository, sessionManager, asignacionDao) as T
                }
            }
        }
    }
}
