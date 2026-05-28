package com.gestorrh.android.ui.equipo.ausencias

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.ausencia.EstadoAusencia
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.ui.ausencia.JustificanteParaAbrir

data class EstadoUiAusenciasEquipo(
    val ausencias: List<RespuestaAusenciaDTO> = emptyList(),
    val cargando: Boolean = false,
    val filtroActivo: EstadoAusencia? = EstadoAusencia.SOLICITADA,
    val mensajeError: MensajeUi? = null,
    val mensajeExito: MensajeUi? = null,
    val revisando: Long? = null,
    val ausenciaAAprobar: RespuestaAusenciaDTO? = null,
    val ausenciaARechazar: RespuestaAusenciaDTO? = null,
    val descargandoJustificanteDe: Long? = null,
    val abrirJustificante: JustificanteParaAbrir? = null
)
