package com.gestorrh.android.ui.ausencia

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.gestorrh.android.R

object AusenciaUtils {

    @StringRes
    fun obtenerStringEstado(estado: String): Int {
        return when (estado.uppercase()) {
            "SOLICITADA" -> R.string.ausencia_estado_solicitada
            "APROBADA" -> R.string.ausencia_estado_aprobada
            "RECHAZADA" -> R.string.ausencia_estado_rechazada
            else -> R.string.ausencia_estado_desconocido
        }
    }

    fun obtenerColorEstado(estado: String): Color {
        return when (estado.uppercase()) {
            "SOLICITADA" -> Color(0xFFF57C00)
            "APROBADA" -> Color(0xFF2E7D32)
            "RECHAZADA" -> Color(0xFFD32F2F)
            else -> Color(0xFF8B8B8B)
        }
    }
}
