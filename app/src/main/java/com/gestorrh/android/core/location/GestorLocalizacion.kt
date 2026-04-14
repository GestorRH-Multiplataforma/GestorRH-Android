package com.gestorrh.android.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await





/**
 * Envoltorio (Wrapper) para el FusedLocationProviderClient de Google.
 * Aísla la lógica de hardware de la capa de presentación.
 */
class GestorLocalizacion(private val contexto: Context) {

    private val clienteLocalizacion = LocationServices.getFusedLocationProviderClient(contexto)

    /**
     * Obtiene la ubicación exacta actual del dispositivo.
     * @return Objeto [Location] con Latitud y Longitud, o null si el GPS está apagado o falla.
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): Location? {
        return try {
            val tareaUbicacion = clienteLocalizacion.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            tareaUbicacion.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}