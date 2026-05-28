package com.gestorrh.android.ui.ausencia

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.gestorrh.android.R
import com.gestorrh.android.data.network.ausencia.EstadoAusencia

object AusenciaUtils {

    @StringRes
    fun obtenerStringEstado(estado: EstadoAusencia): Int {
        return when (estado) {
            EstadoAusencia.SOLICITADA -> R.string.ausencia_estado_solicitada
            EstadoAusencia.APROBADA -> R.string.ausencia_estado_aprobada
            EstadoAusencia.RECHAZADA -> R.string.ausencia_estado_rechazada
        }
    }

    fun obtenerColorEstado(estado: EstadoAusencia): Color {
        return when (estado) {
            EstadoAusencia.SOLICITADA -> Color(0xFFF57C00)
            EstadoAusencia.APROBADA -> Color(0xFF2E7D32)
            EstadoAusencia.RECHAZADA -> Color(0xFFD32F2F)
        }
    }
}
