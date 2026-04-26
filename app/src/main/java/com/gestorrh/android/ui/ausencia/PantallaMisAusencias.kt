package com.gestorrh.android.ui.ausencia

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.archivos.GestorArchivosJustificante
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.ausencia.RespuestaAusenciaDTO
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.LocalLifecycleOwner

private val ColorEstadoSolicitada = Color(0xFFF57C00)
private val ColorEstadoAprobada = Color(0xFF2E7D32)
private val ColorEstadoRechazada = Color(0xFFD32F2F)

private val FormatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisAusencias(
    contexto: Context = LocalContext.current,
    viewModel: MisAusenciasViewModel = viewModel(
        factory = MisAusenciasViewModel.factory(contexto.applicationContext)
    ),
    alSolicitarNueva: () -> Unit = {},
    alEditarAusencia: (RespuestaAusenciaDTO) -> Unit = {}
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contextoLocal = LocalContext.current
    val propietarioCicloVida = LocalLifecycleOwner.current

    var ausenciaAConfirmarCancelar by remember { mutableStateOf<RespuestaAusenciaDTO?>(null) }

    DisposableEffect(propietarioCicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                viewModel.cargarAusencias()
            }
        }
        propietarioCicloVida.lifecycle.addObserver(observador)
        onDispose { propietarioCicloVida.lifecycle.removeObserver(observador) }
    }

    LaunchedEffect(Unit) {
        viewModel.iniciarPolling(propietarioCicloVida.lifecycle)
    }

    val textoReintentar = stringResource(R.string.mis_ausencias_reintentar)
    val mensajeCancelada = stringResource(R.string.mis_ausencias_cancelada_ok)

    LaunchedEffect(estadoUi.cancelacionExitosa) {
        if (estadoUi.cancelacionExitosa) {
            snackbarHostState.showSnackbar(
                message = mensajeCancelada,
                duration = SnackbarDuration.Short
            )
            viewModel.cancelacionConsumida()
        }
    }

    LaunchedEffect(estadoUi.abrirJustificante) {
        estadoUi.abrirJustificante?.let { evento ->
            val abierto = GestorArchivosJustificante.abrirConVisorSistema(
                contextoLocal, evento.uri, evento.nombreArchivo
            )
            viewModel.aperturaJustificanteConsumida()
            if (!abierto) {
                viewModel.notificarSinVisorDisponible()
            }
        }
    }

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensaje ->
            val texto = when (mensaje) {
                is MensajeUi.Recurso -> contextoLocal.getString(mensaje.idRecurso)
                is MensajeUi.Dinamico -> mensaje.texto
            }
            val resultado = snackbarHostState.showSnackbar(
                message = texto,
                actionLabel = textoReintentar,
                duration = SnackbarDuration.Long
            )
            viewModel.errorMostrado()
            if (resultado == SnackbarResult.ActionPerformed) {
                viewModel.cargarAusencias()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.mis_ausencias_titulo)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = alSolicitarNueva) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.mis_ausencias_cd_nueva)
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = estadoUi.cargando && estadoUi.ausencias.isNotEmpty(),
            onRefresh = { viewModel.cargarAusencias() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                estadoUi.cargando && estadoUi.ausencias.isEmpty() -> IndicadorCargando()
                estadoUi.ausencias.isEmpty() -> EstadoVacio()
                else -> ListaAusencias(
                    ausencias = estadoUi.ausencias,
                    descargandoJustificanteDe = estadoUi.descargandoJustificanteDe,
                    padding = PaddingValues(16.dp),
                    alEditar = alEditarAusencia,
                    alCancelar = { ausenciaAConfirmarCancelar = it },
                    alDescargarJustificante = { ausencia ->
                        ausencia.justificante?.let { nombre ->
                            viewModel.descargarJustificante(ausencia.idAusencia, nombre)
                        }
                    }
                )
            }
        }
    }

    ausenciaAConfirmarCancelar?.let { ausencia ->
        AlertDialog(
            onDismissRequest = { ausenciaAConfirmarCancelar = null },
            title = { Text(stringResource(R.string.mis_ausencias_dialog_cancelar_titulo)) },
            text = { Text(stringResource(R.string.mis_ausencias_dialog_cancelar_mensaje)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        ausenciaAConfirmarCancelar = null
                        viewModel.cancelarAusencia(ausencia.idAusencia)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ColorEstadoRechazada
                    )
                ) {
                    Text(stringResource(R.string.mis_ausencias_dialog_cancelar_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = { ausenciaAConfirmarCancelar = null }) {
                    Text(stringResource(R.string.mis_ausencias_dialog_cancelar_volver))
                }
            }
        )
    }
}

@Composable
private fun IndicadorCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EstadoVacio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .height(72.dp)
                .fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.mis_ausencias_vacio_titulo),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.mis_ausencias_vacio_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListaAusencias(
    ausencias: List<RespuestaAusenciaDTO>,
    descargandoJustificanteDe: Long?,
    padding: PaddingValues,
    alEditar: (RespuestaAusenciaDTO) -> Unit,
    alCancelar: (RespuestaAusenciaDTO) -> Unit,
    alDescargarJustificante: (RespuestaAusenciaDTO) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(ausencias, key = { it.idAusencia }) { ausencia ->
            TarjetaAusencia(
                ausencia = ausencia,
                descargandoJustificante = descargandoJustificanteDe == ausencia.idAusencia,
                alEditar = { alEditar(ausencia) },
                alCancelar = { alCancelar(ausencia) },
                alDescargarJustificante = { alDescargarJustificante(ausencia) }
            )
        }
    }
}

@Composable
private fun TarjetaAusencia(
    ausencia: RespuestaAusenciaDTO,
    descargandoJustificante: Boolean,
    alEditar: () -> Unit,
    alCancelar: () -> Unit,
    alDescargarJustificante: () -> Unit
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
                Text(
                    text = etiquetaTipoAusencia(ausencia.tipo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (!ausencia.justificante.isNullOrBlank()) {
                    IconButton(
                        onClick = alDescargarJustificante,
                        enabled = !descargandoJustificante
                    ) {
                        if (descargandoJustificante) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = stringResource(
                                    R.string.mis_ausencias_cd_descargar_justificante
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ausencia.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!ausencia.motivoRechazo.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.mis_ausencias_observaciones,
                        ausencia.motivoRechazo
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ausencia.estado == "SOLICITADA") {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = alEditar) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(Modifier.height(0.dp))
                        Text(
                            text = stringResource(R.string.mis_ausencias_btn_editar),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = alCancelar,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ColorEstadoRechazada
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.mis_ausencias_btn_cancelar),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipEstado(estado: String) {
    val color = when (estado) {
        "SOLICITADA" -> ColorEstadoSolicitada
        "APROBADA" -> ColorEstadoAprobada
        "RECHAZADA" -> ColorEstadoRechazada
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = etiquetaEstado(estado),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun etiquetaEstado(estado: String): String {
    val resId = when (estado) {
        "SOLICITADA" -> R.string.mis_ausencias_estado_solicitada
        "APROBADA" -> R.string.mis_ausencias_estado_aprobada
        "RECHAZADA" -> R.string.mis_ausencias_estado_rechazada
        else -> null
    }
    return resId?.let { stringResource(it) } ?: estado
}

@Composable
private fun etiquetaTipoAusencia(tipo: String): String {
    val resId = when (tipo) {
        "MEDICA" -> R.string.ausencia_tipo_medica
        "VACACIONES" -> R.string.ausencia_tipo_vacaciones
        "MOTIVO_PERSONAL" -> R.string.ausencia_tipo_motivo_personal
        "OTROS" -> R.string.ausencia_tipo_otros
        else -> null
    }
    return resId?.let { stringResource(it) } ?: tipo
}

