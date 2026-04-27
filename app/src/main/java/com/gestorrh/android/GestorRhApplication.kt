package com.gestorrh.android

import android.app.Application
import androidx.work.Configuration

/**
 * Clase Application personalizada de GestorRH.
 *
 * Responsabilidades:
 *  - Proporcionar el punto de inicialización global de la aplicación antes
 *    de que se cree cualquier Activity o Service.
 *  - Implementar [Configuration.Provider] para que WorkManager use la configuración
 *    del proyecto en lugar de la inicialización automática por ContentProvider,
 *    evitando problemas de arranque en Android 12+ con `androidx.startup`.
 *
 * IMPORTANTE: Registrar en AndroidManifest.xml:
 *   <application android:name=".GestorRhApplication" ...>
 */
class GestorRhApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Configuración de WorkManager.
     * Se usa la configuración por defecto del sistema. En el futuro se puede
     * personalizar el [androidx.work.WorkerFactory] para inyectar dependencias
     * en los workers sin Hilt (siguiendo el patrón de factories manuales del proyecto).
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG
                else android.util.Log.ERROR
            )
            .build()
}
