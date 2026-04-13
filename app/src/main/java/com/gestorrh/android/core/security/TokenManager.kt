package com.gestorrh.android.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor de almacenamiento seguro para credenciales sensibles.
 * Utiliza Android Keystore para encriptar los datos en el dispositivo.
 */
class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gestorrh_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
    }

    /**
     * Guarda el token encriptado.
     */
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    /**
     * Recupera el token desencriptado. Retorna null si no existe.
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    /**
     * Elimina el token (Logout).
     */
    fun clearToken() {
        sharedPreferences.edit().remove(KEY_JWT_TOKEN).apply()
    }
}