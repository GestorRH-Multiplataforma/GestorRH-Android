package com.gestorrh.android.core.security

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Canal de comunicación global para eventos críticos de autenticación.
 * Permite que el [com.gestorrh.android.core.network.AuthInterceptor] notifique
 * a la capa de UI sobre una sesión expirada (HTTP 401) sin acoplarse a ningún
 * componente de presentación.
 *
 * Al ser un singleton (object), cualquier colector activo en la app recibirá
 * el evento independientemente de la pantalla en la que se encuentre el usuario.
 */
object AuthEventBus {

    private val _sesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Flujo al que se suscribe MainActivity para redirigir al login cuando la sesión expira. */
    val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

    /**
     * Emite un evento de sesión expirada. Llamado desde el hilo de red de OkHttp,
     * por lo que se usa [MutableSharedFlow.tryEmit] (no suspendida) con buffer de 1
     * para garantizar que el evento no se pierda si aún no hay colector activo.
     */
    fun emitirSesionExpirada() {
        _sesionExpirada.tryEmit(Unit)
    }
}
