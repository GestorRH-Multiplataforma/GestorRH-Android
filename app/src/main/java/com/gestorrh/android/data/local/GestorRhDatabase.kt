package com.gestorrh.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gestorrh.android.data.local.dao.AsignacionDao
import com.gestorrh.android.data.local.dao.FichajePendienteDao
import com.gestorrh.android.data.local.entity.AsignacionEntity
import com.gestorrh.android.data.local.entity.FichajePendienteEntity

@Database(
    entities = [AsignacionEntity::class, FichajePendienteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GestorRhDatabase : RoomDatabase() {

    abstract fun asignacionDao(): AsignacionDao

    abstract fun fichajePendienteDao(): FichajePendienteDao

    companion object {
        private const val NOMBRE_BASE_DATOS = "gestorrh.db"

        /**
         * Migración de la versión 1 a la 2: añade la tabla `fichajes_pendientes` para
         * soportar la cola offline de fichajes sincronizada por `SyncFichajeWorker`.
         * Se define explícitamente para preservar la caché local de asignaciones de los
         * usuarios que ya tengan la aplicación instalada al desplegar esta versión.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fichajes_pendientes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tipo TEXT NOT NULL,
                        latitud REAL,
                        longitud REAL,
                        timestamp INTEGER NOT NULL,
                        intentos INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instancia: GestorRhDatabase? = null

        fun getInstance(contexto: Context): GestorRhDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    GestorRhDatabase::class.java,
                    NOMBRE_BASE_DATOS
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
