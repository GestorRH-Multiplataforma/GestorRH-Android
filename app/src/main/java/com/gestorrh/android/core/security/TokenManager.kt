package com.gestorrh.android.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacenamiento seguro de credenciales y datos sensibles de la aplicación.
 * Emplea el sistema nativo Keystore de Android para cifrar y descifrar el Token JWT
 * de forma transparente, garantizando que no pueda ser extraído en texto plano
 * incluso si el dispositivo es comprometido (Root).
 *
 * @param contexto Contexto de la aplicación necesario para inicializar el almacenamiento.
 */
class TokenManager(contexto: Context) {

    private val claveMaestra = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferenciasCifradas = EncryptedSharedPreferences.create(
        contexto,
        "gestorrh_preferencias_seguras",
        claveMaestra,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val CLAVE_TOKEN_JWT = "token_jwt"
    }

    /**
     * Almacena el token de sesión en la caja fuerte del dispositivo.
     * El valor es cifrado físicamente utilizando el estándar AES256-GCM.
     *
     * @param token Cadena JWT devuelta por el servidor tras un inicio de sesión exitoso.
     */
    fun guardarToken(token: String) {
        preferenciasCifradas.edit().putString(CLAVE_TOKEN_JWT, token).apply()
    }

    /**
     * Recupera y descifra el token de sesión actual.
     *
     * @return El token en texto plano si la sesión está activa, o null si la caja fuerte está vacía.
     */
    fun obtenerToken(): String? {
        return preferenciasCifradas.getString(CLAVE_TOKEN_JWT, null)
    }

    /**
     * Purga el token del almacenamiento seguro de manera irreversible.
     * Invocado durante el flujo de cierre de sesión (Logout) o como medida
     * defensiva si el servidor detecta que la sesión ha expirado.
     */
    fun borrarToken() {
        preferenciasCifradas.edit().remove(CLAVE_TOKEN_JWT).apply()
    }
}