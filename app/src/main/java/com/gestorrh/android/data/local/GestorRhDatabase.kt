package com.gestorrh.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gestorrh.android.data.local.dao.AsignacionDao
import com.gestorrh.android.data.local.entity.AsignacionEntity

@Database(
    entities = [AsignacionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GestorRhDatabase : RoomDatabase() {

    abstract fun asignacionDao(): AsignacionDao

    companion object {
        private const val NOMBRE_BASE_DATOS = "gestorrh.db"

        @Volatile
        private var instancia: GestorRhDatabase? = null

        fun getInstance(contexto: Context): GestorRhDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    GestorRhDatabase::class.java,
                    NOMBRE_BASE_DATOS
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
