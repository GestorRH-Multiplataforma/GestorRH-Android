package com.gestorrh.android.ui.equipo.ausencias

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.archivos.GestorArchivosJustificante
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.ausencia.EstadoAusencia
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import com.gestorrh.android.data.network.ausencia.TipoAusencia
import com.gestorrh.android.ui.ausencia.AusenciaUtils
import com.gestorrh.android.ui.theme.SemanticError
import com.gestorrh.android.ui.theme.SemanticSuccess
import java.time.format.DateTimeFormatter

private val FormatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAusenciasEquipo(
    contexto: Context = LocalContext.current,
    viewModel: AusenciasEquipoViewModel = viewModel(
        factory = AusenciasEquipoViewModel.factory(contexto.applicationContext)
    )
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val recursos = LocalResources.current
    val contextoLocal = LocalContext.current

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensaje ->
            val texto = when (mensaje) {
                is MensajeUi.Recurso -> recursos.getString(mensaje.idRecurso)
                is MensajeUi.Dinamico -> mensaje.texto
            }
            snackbarHostState.showSnackbar(texto, duration = SnackbarDuration.Long)
            viewModel.errorMostrado()
        }
    }

    LaunchedEffect(estadoUi.mensajeExito) {
        estadoUi.mensajeExito?.let { mensaje ->
            val texto = when (mensaje) {
                is MensajeUi.Recurso -> recursos.getString(mensaje.idRecurso)
                is MensajeUi.Dinamico -> mensaje.texto
            }
            snackbarHostState.showSnackbar(texto, duration = SnackbarDuration.Short)
            viewModel.exitoMostrado()
        }
    }

    LaunchedEffect(estadoUi.abrirJustificante) {
        estadoUi.abrirJustificante?.let { evento ->
            val abierto = GestorArchivosJustificante.abrirConVisorSistema(
                contextoLocal, evento.uri, evento.nombreArchivo
            )
            viewModel.aperturaJustificanteConsumida()
            if (!abierto) viewModel.notificarSinVisorDisponible()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FilaFiltros(
                filtroActivo = estadoUi.filtroActivo,
                onFiltroSeleccionado = viewModel::cambiarFiltro
            )

            HorizontalDivider()

            PullToRefreshBox(
                isRefreshing = estadoUi.cargando && estadoUi.ausencias.isNotEmpty(),
                onRefresh = viewModel::cargarAusencias,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    estadoUi.cargando && estadoUi.ausencias.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    estadoUi.ausencias.isEmpty() -> EstadoVacio(
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> ListaAusencias(
                        ausencias = estadoUi.ausencias,
                        revisando = estadoUi.revisando,
                        descargandoJustificanteDe = estadoUi.descargandoJustificanteDe,
                        onAprobar = viewModel::iniciarAprobacion,
                        onRechazar = viewModel::iniciarRechazo,
                        onDescargarJustificante = { ausencia ->
                            ausencia.justificante?.let { nombre ->
                                viewModel.descargarJustificante(ausencia.idAusencia, nombre)
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    estadoUi.ausenciaARechazar?.let { ausencia ->
        DialogRechazo(
            ausencia = ausencia,
            onConfirmar = viewModel::confirmarRechazo,
            onDescartar = viewModel::cerrarDialogRechazo
        )
    }

    estadoUi.ausenciaAAprobar?.let { ausencia ->
        DialogAprobacion(
            ausencia = ausencia,
            onConfirmar = { observaciones ->
                viewModel.aprobarAusencia(ausencia.idAusencia, observaciones)
                viewModel.cerrarDialogAprobacion()
            },
            onDescartar = viewModel::cerrarDialogAprobacion
        )
    }
}

@Composable
private fun FilaFiltros(
    filtroActivo: EstadoAusencia?,
    onFiltroSeleccionado: (EstadoAusencia?) -> Unit
) {
    data class OpcionFiltro(val estado: EstadoAusencia?, val etiquetaRes: Int)

    val opciones = listOf(
        OpcionFiltro(null, R.string.ausencias_equipo_filtro_todos),
        OpcionFiltro(EstadoAusencia.SOLICITADA, R.string.ausencias_equipo_filtro_solicitadas),
        OpcionFiltro(EstadoAusencia.APROBADA, R.string.ausencias_equipo_filtro_aprobadas),
        OpcionFiltro(EstadoAusencia.RECHAZADA, R.string.ausencias_equipo_filtro_rechazadas)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(opciones) { opcion ->
            FilterChip(
                selected = filtroActivo == opcion.estado,
                onClick = { onFiltroSeleccionado(opcion.estado) },
                label = { Text(stringResource(opcion.etiquetaRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun ListaAusencias(
    ausencias: List<RespuestaAusenciaDTO>,
    revisando: Long?,
    descargandoJustificanteDe: Long?,
    onAprobar: (RespuestaAusenciaDTO) -> Unit,
    onRechazar: (RespuestaAusenciaDTO) -> Unit,
    onDescargarJustificante: (RespuestaAusenciaDTO) -> Unit
) {
    val grupos = remember(ausencias) {
        ausencias.groupBy { it.nombreCompletoEmpleado ?: "" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grupos.forEach { (nombre, ausenciasEmpleado) ->
            item(key = "empleado_$nombre") {
                SeparadorEmpleado(nombre = nombre)
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(ausenciasEmpleado, key = { it.idAusencia }) { ausencia ->
                TarjetaAusenciaEquipo(
                    ausencia = ausencia,
                    revisando = revisando == ausencia.idAusencia,
                    descargandoJustificante = descargandoJustificanteDe == ausencia.idAusencia,
                    onAprobar = { onAprobar(ausencia) },
                    onRechazar = { onRechazar(ausencia) },
                    onDescargarJustificante = { onDescargarJustificante(ausencia) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SeparadorEmpleado(nombre: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun TarjetaAusenciaEquipo(
    ausencia: RespuestaAusenciaDTO,
    revisando: Boolean,
    descargandoJustificante: Boolean,
    onAprobar: (RespuestaAusenciaDTO) -> Unit,
    onRechazar: (RespuestaAusenciaDTO) -> Unit,
    onDescargarJustificante: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ausencia.nombreCompletoEmpleado ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = etiquetaTipo(ausencia.tipo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!ausencia.justificante.isNullOrBlank()) {
                    IconButton(
                        onClick = onDescargarJustificante,
                        enabled = !descargandoJustificante
                    ) {
                        if (descargandoJustificante) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = stringResource(
                                    R.string.ausencias_equipo_cd_justificante
                                ),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                ChipEstado(estado = ausencia.estado)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.mis_ausencias_rango_fechas,
                    ausencia.fechaInicio.format(FormatoFecha),
                    ausencia.fechaFin.format(FormatoFecha)
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            if (!ausencia.descripcion.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ausencia.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!ausencia.observacionesRevision.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.ausencias_equipo_observaciones,
                        ausencia.observacionesRevision
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ausencia.estado == EstadoAusencia.SOLICITADA) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onAprobar(ausencia) },
                        enabled = !revisando,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SemanticSuccess
                        )
                    ) {
                        if (revisando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SemanticSuccess
                            )
                        } else {
                            Text(stringResource(R.string.ausencias_equipo_btn_aprobar))
                        }
                    }
                    TextButton(
                        onClick = { onRechazar(ausencia) },
                        enabled = !revisando,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SemanticError
                        )
                    ) {
                        Text(stringResource(R.string.ausencias_equipo_btn_rechazar))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipEstado(estado: EstadoAusencia) {
    Box(
        modifier = Modifier
            .background(
                color = AusenciaUtils.obtenerColorEstado(estado),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(id = AusenciaUtils.obtenerStringEstado(estado)),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DialogRechazo(
    ausencia: RespuestaAusenciaDTO,
    onConfirmar: (String?) -> Unit,
    onDescartar: () -> Unit
) {
    var observaciones by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDescartar,
        title = {
            Text(stringResource(R.string.ausencias_equipo_dialog_rechazo_titulo))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = ausencia.nombreCompletoEmpleado ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = {
                        Text(stringResource(R.string.ausencias_equipo_dialog_rechazo_hint))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmar(observaciones.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = SemanticError
                )
            ) {
                Text(stringResource(R.string.ausencias_equipo_dialog_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) {
                Text(stringResource(R.string.ausencia_dialog_cancelar))
            }
        }
    )
}

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.ausencias_equipo_vacio_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ausencias_equipo_vacio_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun etiquetaTipo(tipo: TipoAusencia): String {
    val resId = when (tipo) {
        TipoAusencia.MEDICA -> R.string.ausencia_tipo_medica
        TipoAusencia.VACACIONES -> R.string.ausencia_tipo_vacaciones
        TipoAusencia.MOTIVO_PERSONAL -> R.string.ausencia_tipo_motivo_personal
        TipoAusencia.OTROS -> R.string.ausencia_tipo_otros
    }
    return stringResource(resId)
}

@Composable
private fun DialogAprobacion(
    ausencia: RespuestaAusenciaDTO,
    onConfirmar: (String?) -> Unit,
    onDescartar: () -> Unit
) {
    var observaciones by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDescartar,
        title = {
            Text(stringResource(R.string.ausencias_equipo_dialog_aprobacion_titulo))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = ausencia.nombreCompletoEmpleado ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = {
                        Text(stringResource(R.string.ausencias_equipo_dialog_aprobacion_hint))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmar(observaciones.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = SemanticSuccess
                )
            ) {
                Text(stringResource(R.string.ausencias_equipo_dialog_confirmar_aprobacion))
            }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) {
                Text(stringResource(R.string.ausencia_dialog_cancelar))
            }
        }
    )
}

