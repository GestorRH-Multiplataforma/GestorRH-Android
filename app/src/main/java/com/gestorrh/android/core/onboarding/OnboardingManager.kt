package com.gestorrh.android.core.onboarding

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona la persistencia del estado del onboarding.
 *
 * Usa [SharedPreferences] estándar (no cifradas) porque el flag
 * de onboarding no es un dato sensible — solo indica si el usuario
 * ya ha visto la pantalla de bienvenida en este dispositivo.
 *
 * @param contexto Contexto de la aplicación.
 */
class OnboardingManager(contexto: Context) {

    private val preferencias: SharedPreferences = contexto.getSharedPreferences(
        NOMBRE_PREFERENCIAS,
        Context.MODE_PRIVATE
    )

    /**
     * Devuelve `true` si el usuario ya ha completado el onboarding
     * en este dispositivo. Se evalúa de forma síncrona en el arranque,
     * igual que la lectura del token JWT en [SessionManager].
     */
    fun onboardingCompletado(): Boolean =
        preferencias.getBoolean(CLAVE_ONBOARDING_COMPLETADO, false)

    /**
     * Marca el onboarding como completado. Se llama al pulsar
     * "Empezar" o "Saltar" en la pantalla de onboarding.
     */
    fun marcarOnboardingCompletado() {
        preferencias.edit()
            .putBoolean(CLAVE_ONBOARDING_COMPLETADO, true)
            .apply()
    }

    companion object {
        private const val NOMBRE_PREFERENCIAS = "gestorrh_onboarding"
        private const val CLAVE_ONBOARDING_COMPLETADO = "onboarding_completado"
    }
}