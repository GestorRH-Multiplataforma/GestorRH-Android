package com.gestorrh.android.core.security

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Fuente de verdad única para la sesión del empleado autenticado.
 *
 * Persiste el token JWT, el nombre completo, el rol, el identificador numérico
 * y el nombre de la empresa del empleado en el Keystore nativo de Android mediante
 * cifrado AES256-GCM, garantizando que no puedan extraerse en texto plano.
 *
 * El rol se persiste para poder determinar la navegación condicional (EMPLEADO vs
 * SUPERVISOR) en arranques en frío sin necesidad de realizar ninguna petición de red.
 * El nombre de empresa se persiste para mostrarlo en el header global sin petición de red.
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
        private const val CLAVE_ROL_EMPLEADO = "rol_empleado"
        private const val CLAVE_ID_EMPLEADO = "id_empleado"
        private const val CLAVE_EMPRESA = "nombre_empresa"
        private const val ROL_SUPERVISOR = "SUPERVISOR"
        private const val ID_EMPLEADO_SIN_SESION = -1L
    }

    /**
     * Persiste el token JWT, el nombre, el rol, el identificador y el nombre de empresa
     * del empleado al iniciar sesión correctamente.
     *
     * @param token Cadena JWT devuelta por el servidor tras autenticación exitosa.
     * @param nombre Nombre completo del empleado tal como lo devuelve la API.
     * @param rol Rol del empleado: "EMPLEADO" o "SUPERVISOR".
     * @param id Identificador numérico único del empleado en el sistema.
     * @param empresa Nombre de la empresa a la que pertenece el empleado.
     */
    fun saveSession(token: String, nombre: String, rol: String, id: Long, empresa: String) {
        preferenciasCifradas.edit {
            putString(CLAVE_TOKEN_JWT, token)
            putString(CLAVE_NOMBRE_EMPLEADO, nombre)
            putString(CLAVE_ROL_EMPLEADO, rol)
            putLong(CLAVE_ID_EMPLEADO, id)
            putString(CLAVE_EMPRESA, empresa)
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
     * Recupera el rol del empleado de la sesión activa.
     *
     * @return El rol ("EMPLEADO" o "SUPERVISOR") si existe sesión, null en caso contrario.
     */
    fun getRol(): String? = preferenciasCifradas.getString(CLAVE_ROL_EMPLEADO, null)

    /**
     * Recupera el identificador numérico del empleado autenticado.
     *
     * @return El id del empleado si existe sesión, -1 en caso contrario.
     */
    fun getId(): Long = preferenciasCifradas.getLong(CLAVE_ID_EMPLEADO, ID_EMPLEADO_SIN_SESION)

    /**
     * Recupera el nombre de la empresa del empleado autenticado.
     * Se usa para mostrarlo en el header global sin necesidad de petición de red.
     *
     * @return El nombre de la empresa si existe sesión, null en caso contrario.
     */
    fun getEmpresa(): String? = preferenciasCifradas.getString(CLAVE_EMPRESA, null)

    /**
     * Indica si el empleado autenticado tiene rol de supervisor.
     *
     * @return true si el rol persistido es "SUPERVISOR", false en cualquier otro caso.
     */
    fun isSupervisor(): Boolean = getRol() == ROL_SUPERVISOR

    /**
     * Purga todos los datos de sesión del almacenamiento seguro.
     * Invocado durante el logout manual o cuando el servidor devuelve 401.
     */
    fun clearSession() {
        preferenciasCifradas.edit {
            remove(CLAVE_TOKEN_JWT)
            remove(CLAVE_NOMBRE_EMPLEADO)
            remove(CLAVE_ROL_EMPLEADO)
            remove(CLAVE_ID_EMPLEADO)
            remove(CLAVE_EMPRESA)
        }
    }
}
