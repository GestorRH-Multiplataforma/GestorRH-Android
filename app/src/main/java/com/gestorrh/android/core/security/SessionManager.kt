package com.gestorrh.android.core.security

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Fuente de verdad única para la sesión del empleado autenticado.
 * Persiste el token JWT y el nombre completo en el Keystore nativo de Android
 * mediante cifrado AES256-GCM, garantizando que no puedan extraerse en texto plano.
 *
 * El campo rol se gestionará en la issue P2-01.
 *
 * @param contexto Contexto de la aplicación necesario para inicializar el almacenamiento cifrado.
 */
class SessionManager(contexto: Context) {

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
        private const val CLAVE_NOMBRE_EMPLEADO = "nombre_empleado"
        // TODO(#17): añadir persistencia de rol
    }

    /**
     * Persiste el token JWT y el nombre del empleado al iniciar sesión.
     *
     * @param token Cadena JWT devuelta por el servidor tras autenticación exitosa.
     * @param nombre Nombre completo del empleado tal como lo devuelve la API.
     */
    fun saveSession(token: String, nombre: String) {
        preferenciasCifradas.edit {
            putString(CLAVE_TOKEN_JWT, token)
            putString(CLAVE_NOMBRE_EMPLEADO, nombre)
        }
    }

    /**
     * Recupera y descifra el token de sesión activo.
     *
     * @return El token en texto plano si hay sesión activa, null en caso contrario.
     */
    fun getToken(): String? = preferenciasCifradas.getString(CLAVE_TOKEN_JWT, null)

    /**
     * Recupera el nombre del empleado de la sesión activa.
     *
     * @return El nombre completo si existe sesión, null en caso contrario.
     */
    fun getNombre(): String? = preferenciasCifradas.getString(CLAVE_NOMBRE_EMPLEADO, null)

    /**
     * Purga todos los datos de sesión del almacenamiento seguro.
     * Invocado durante el logout manual o cuando el servidor devuelve 401.
     */
    fun clearSession() {
        preferenciasCifradas.edit {
            remove(CLAVE_TOKEN_JWT)
            remove(CLAVE_NOMBRE_EMPLEADO)
        }
    }
}
