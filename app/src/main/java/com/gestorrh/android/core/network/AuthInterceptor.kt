package com.gestorrh.android.core.network

import com.gestorrh.android.core.security.AuthEventBus
import com.gestorrh.android.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp con doble responsabilidad:
 * 1. Inyecta el token JWT de sesión como cabecera Authorization en todas las
 *    peticiones autenticadas, liberando a cada servicio Retrofit de hacerlo manualmente.
 * 2. Inspecciona cada respuesta del servidor: si el código es 401 (sesión expirada),
 *    limpia la sesión y emite un evento global mediante [AuthEventBus] para que
 *    la UI redirija al Login de forma automática.
 *
 * Las rutas `/auth/` quedan excluidas de ambos mecanismos para evitar bucles
 * infinitos durante el proceso de autenticación inicial.
 *
 * @param sessionManager Fuente de verdad del token de sesión activo.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticionOriginal = chain.request()
        val ruta = peticionOriginal.url.encodedPath

        if (ruta.contains("/auth/")) {
            return chain.proceed(peticionOriginal)
        }

        val token = sessionManager.getToken()
        val constructorPeticion = peticionOriginal.newBuilder()

        if (!token.isNullOrEmpty()) {
            constructorPeticion.addHeader("Authorization", "Bearer $token")
        }

        val respuesta = chain.proceed(constructorPeticion.build())

        if (respuesta.code == 401) {
            sessionManager.clearSession()
            AuthEventBus.emitirSesionExpirada()
        }

        return respuesta
    }
}
