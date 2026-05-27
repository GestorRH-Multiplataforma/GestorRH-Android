package com.gestorrh.android.ui.equipo.fichajes

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ColorAmbar = Color(0xFFF57C00)
private val ColorAmbarFondo = Color(0xFFFFF3E0)
private val FormatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val FormatoHora = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaModificacionFichajes(
    contexto: Context = LocalContext.current,
    viewModel: ModificacionFichajesViewModel = viewModel(
        factory = ModificacionFichajesViewModel.factory(contexto.applicationContext)
    )
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val recursos = LocalResources.current

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            SeccionFiltros(
                estadoUi = estadoUi,
                onEmpleadoSeleccionado = viewModel::seleccionarEmpleado,
                onFechaInicioActualizada = viewModel::actualizarFechaInicio,
                onFechaFinActualizada = viewModel::actualizarFechaFin,
                onAplicarFiltro = viewModel::cargarFichajes
            )

            HorizontalDivider()

            when {
                estadoUi.cargandoFichajes -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                estadoUi.fichajes.isEmpty() && !estadoUi.cargandoEmpleados -> {
                    EstadoVacio(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    ListaFichajes(
                        fichajes = estadoUi.fichajes,
                        onEditar = viewModel::abrirDialogEdicion,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (estadoUi.dialogAbierto) {
        DialogEdicionFichaje(
            estadoUi = estadoUi,
            onDismiss = viewModel::cerrarDialog,
            onEntradaCambiada = viewModel::actualizarDialogEntrada,
            onSalidaCambiada = viewModel::actualizarDialogSalida,
            onMotivoCambiado = viewModel::actualizarDialogMotivo,
            onGuardar = viewModel::guardarModificacion
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeccionFiltros(
    estadoUi: EstadoUiModificacionFichajes,
    onEmpleadoSeleccionado: (RespuestaEmpleadoDTO?) -> Unit,
    onFechaInicioActualizada: (LocalDate) -> Unit,
    onFechaFinActualizada: (LocalDate) -> Unit,
    onAplicarFiltro: () -> Unit
) {
    var mostrarSelectorInicio by remember { mutableStateOf(false) }
    var mostrarSelectorFin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownEmpleado(
            empleados = estadoUi.empleados,
            seleccionado = estadoUi.empleadoSeleccionado,
            cargando = estadoUi.cargandoEmpleados,
            onSeleccionar = onEmpleadoSeleccionado
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonSelectorFecha(
                etiqueta = stringResource(R.string.historial_fichajes_fecha_inicio),
                fecha = estadoUi.fechaInicio,
                modifier = Modifier.weight(1f),
                alSeleccionar = { mostrarSelectorInicio = true }
            )
            BotonSelectorFecha(
                etiqueta = stringResource(R.string.historial_fichajes_fecha_fin),
                fecha = estadoUi.fechaFin,
                modifier = Modifier.weight(1f),
                alSeleccionar = { mostrarSelectorFin = true }
            )
        }

        Button(
            onClick = onAplicarFiltro,
            modifier = Modifier.fillMaxWidth(),
            enabled = !estadoUi.cargandoFichajes
        ) {
            Text(stringResource(R.string.historial_fichajes_aplicar_filtro))
        }
    }

    if (mostrarSelectorInicio) {
        SelectorFecha(
            fechaInicial = estadoUi.fechaInicio,
            alSeleccionar = onFechaInicioActualizada,
            alDescartar = { mostrarSelectorInicio = false }
        )
    }

    if (mostrarSelectorFin) {
        SelectorFecha(
            fechaInicial = estadoUi.fechaFin,
            alSeleccionar = onFechaFinActualizada,
            alDescartar = { mostrarSelectorFin = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownEmpleado(
    empleados: List<RespuestaEmpleadoDTO>,
    seleccionado: RespuestaEmpleadoDTO?,
    cargando: Boolean,
    onSeleccionar: (RespuestaEmpleadoDTO?) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    val textoSeleccionado = when {
        cargando -> stringResource(R.string.modificacion_fichaje_cargando_empleados)
        seleccionado != null -> "${seleccionado.nombre} ${seleccionado.apellidos}"
        else -> stringResource(R.string.modificacion_fichaje_todos_empleados)
    }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { if (!cargando) expandido = it }
    ) {
        OutlinedTextField(
            value = textoSeleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.modificacion_fichaje_label_empleado)) },
            trailingIcon = {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.modificacion_fichaje_todos_empleados),
                        fontWeight = FontWeight.Medium
                    )
                },
                onClick = {
                    onSeleccionar(null)
                    expandido = false
                }
            )
            HorizontalDivider()
            empleados.forEach { empleado ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${empleado.nombre} ${empleado.apellidos}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            empleado.puesto?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSeleccionar(empleado)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BotonSelectorFecha(
    etiqueta: String,
    fecha: LocalDate,
    modifier: Modifier = Modifier,
    alSeleccionar: () -> Unit
) {
    OutlinedButton(
        onClick = alSeleccionar,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = fecha.format(FormatoFecha),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(
    fechaInicial: LocalDate,
    alSeleccionar: (LocalDate) -> Unit,
    alDescartar: () -> Unit
) {
    val zonaHoraria = ZoneId.systemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = fechaInicial
            .atStartOfDay(zonaHoraria)
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = alDescartar,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    alSeleccionar(
                        Instant.ofEpochMilli(millis).atZone(zonaHoraria).toLocalDate()
                    )
                }
                alDescartar()
            }) { Text(stringResource(R.string.ausencia_dialog_aceptar)) }
        },
        dismissButton = {
            TextButton(onClick = alDescartar) {
                Text(stringResource(R.string.ausencia_dialog_cancelar))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun ListaFichajes(
    fichajes: List<RespuestaFichajeDTO>,
    onEditar: (RespuestaFichajeDTO) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(fichajes, key = { it.idFichaje }) { fichaje ->
            TarjetaFichajeSupervisor(
                fichaje = fichaje,
                onEditar = { onEditar(fichaje) }
            )
        }
    }
}

@Composable
private fun TarjetaFichajeSupervisor(
    fichaje: RespuestaFichajeDTO,
    onEditar: () -> Unit
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
                        text = fichaje.nombreEmpleado,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = fichaje.fecha.format(FormatoFecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    fichaje.descripcionTurno?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEditar) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.modificacion_fichaje_cd_editar),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.historial_fichajes_hora_entrada),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = fichaje.horaEntrada.format(FormatoHora),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.historial_fichajes_hora_salida),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fichaje.horaSalida != null) {
                        Text(
                            text = fichaje.horaSalida.format(FormatoHora),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.modificacion_fichaje_sin_salida),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorAmbar
                        )
                    }
                }
            }

            fichaje.incidencias?.let { incidencia ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ColorAmbarFondo,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = incidencia,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorAmbar
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogEdicionFichaje(
    estadoUi: EstadoUiModificacionFichajes,
    onDismiss: () -> Unit,
    onEntradaCambiada: (LocalDateTime) -> Unit,
    onSalidaCambiada: (LocalDateTime?) -> Unit,
    onMotivoCambiado: (String) -> Unit,
    onGuardar: () -> Unit
) {
    val fichaje = estadoUi.fichajeEditando ?: return

    val horaEntradaInicial = estadoUi.dialogEntrada ?: fichaje.horaEntrada
    val horaSalidaInicial = estadoUi.dialogSalida ?: fichaje.horaSalida

    var salidaActivada by remember {
        mutableStateOf(horaSalidaInicial != null)
    }

    val timePickerEntradaState = rememberTimePickerState(
        initialHour = horaEntradaInicial.hour,
        initialMinute = horaEntradaInicial.minute,
        is24Hour = true
    )

    val horaSalidaParaPicker = horaSalidaInicial ?: LocalDateTime.now()
    val timePickerSalidaState = rememberTimePickerState(
        initialHour = horaSalidaParaPicker.hour,
        initialMinute = horaSalidaParaPicker.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.modificacion_fichaje_dialog_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BannerAdvertencia()

                Text(
                    text = stringResource(
                        R.string.modificacion_fichaje_dialog_subtitulo,
                        fichaje.nombreEmpleado,
                        fichaje.fecha.format(FormatoFecha)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.modificacion_fichaje_label_entrada),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TimePicker(
                    state = timePickerEntradaState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectorColor = MaterialTheme.colorScheme.primary
                    )
                )
                LaunchedEffect(
                    timePickerEntradaState.hour,
                    timePickerEntradaState.minute
                ) {
                    val nuevaEntrada = fichaje.horaEntrada
                        .withHour(timePickerEntradaState.hour)
                        .withMinute(timePickerEntradaState.minute)
                        .withSecond(0)
                    onEntradaCambiada(nuevaEntrada)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.modificacion_fichaje_label_salida),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.modificacion_fichaje_incluir_salida),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = salidaActivada,
                            onCheckedChange = { activada ->
                                salidaActivada = activada
                                if (activada) {
                                    // Al activar, propagamos la hora actual del picker
                                    val baseFecha = fichaje.horaSalida ?: fichaje.horaEntrada
                                    val nuevaSalida = baseFecha
                                        .withHour(timePickerSalidaState.hour)
                                        .withMinute(timePickerSalidaState.minute)
                                        .withSecond(0)
                                    onSalidaCambiada(nuevaSalida)
                                } else {
                                    onSalidaCambiada(null)
                                }
                            }
                        )
                    }
                }

                if (salidaActivada) {
                    TimePicker(
                        state = timePickerSalidaState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    LaunchedEffect(
                        timePickerSalidaState.hour,
                        timePickerSalidaState.minute
                    ) {
                        val baseFecha = fichaje.horaSalida ?: fichaje.horaEntrada
                        val nuevaSalida = baseFecha
                            .withHour(timePickerSalidaState.hour)
                            .withMinute(timePickerSalidaState.minute)
                            .withSecond(0)
                        onSalidaCambiada(nuevaSalida)
                    }
                } else {
                    Text(
                        text = stringResource(R.string.modificacion_fichaje_sin_salida),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorAmbar,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                estadoUi.dialogErrorHoras?.let { idError ->
                    Text(
                        text = stringResource(idError),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = estadoUi.dialogMotivo,
                    onValueChange = onMotivoCambiado,
                    label = { Text(stringResource(R.string.modificacion_fichaje_label_motivo)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    isError = estadoUi.dialogMotivo.isEmpty() && estadoUi.guardando
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGuardar,
                enabled = estadoUi.dialogGuardarHabilitado
            ) {
                if (estadoUi.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.modificacion_fichaje_btn_guardar))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !estadoUi.guardando
            ) {
                Text(stringResource(R.string.ausencia_dialog_cancelar))
            }
        }
    )
}

@Composable
private fun BannerAdvertencia() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ColorAmbarFondo,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = ColorAmbar,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.modificacion_fichaje_aviso_auditoria),
            style = MaterialTheme.typography.bodySmall,
            color = ColorAmbar,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.modificacion_fichaje_vacio_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.modificacion_fichaje_vacio_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

