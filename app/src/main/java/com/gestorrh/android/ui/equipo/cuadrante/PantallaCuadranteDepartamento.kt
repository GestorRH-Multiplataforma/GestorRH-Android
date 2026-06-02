package com.gestorrh.android.ui.equipo.cuadrante

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.asignacion.ModalidadAsignacion
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.turno.RespuestaTurnoDTO
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ColorPresencial = Color(0xFF1A365D)
private val ColorTeletrabajo = Color(0xFF00A8E8)
private val ColorEliminar = Color(0xFFD32F2F)
private val FormatoHora = DateTimeFormatter.ofPattern("HH:mm")
private val FormatoFechaModificacion = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCuadranteDepartamento(
    contexto: Context = LocalContext.current,
    viewModel: CuadranteDepartamentoViewModel = viewModel(
        factory = CuadranteDepartamentoViewModel.factory(contexto.applicationContext)
    )
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val recursos = LocalResources.current
    val textoReintentar = stringResource(R.string.cuadrante_reintentar)
    val propietarioCicloVida = LocalLifecycleOwner.current

    DisposableEffect(propietarioCicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                viewModel.cargarAsignaciones()
            }
        }
        propietarioCicloVida.lifecycle.addObserver(observador)
        onDispose { propietarioCicloVida.lifecycle.removeObserver(observador) }
    }

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensaje ->
            val texto = when (mensaje) {
                is MensajeUi.Recurso -> recursos.getString(mensaje.idRecurso)
                is MensajeUi.Dinamico -> mensaje.texto
            }
            val resultado = snackbarHostState.showSnackbar(
                message = texto,
                actionLabel = textoReintentar,
                duration = SnackbarDuration.Long
            )
            viewModel.errorMostrado()
            if (resultado == SnackbarResult.ActionPerformed) {
                viewModel.cargarAsignaciones()
            }
        }
    }

    LaunchedEffect(estadoUi.mensajeExito) {
        estadoUi.mensajeExito?.let { mensaje ->
            val texto = when (mensaje) {
                is MensajeUi.Recurso -> recursos.getString(mensaje.idRecurso)
                is MensajeUi.Dinamico -> mensaje.texto
            }
            snackbarHostState.showSnackbar(
                message = texto,
                duration = SnackbarDuration.Short
            )
            viewModel.exitoMostrado()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirBottomSheet) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cuadrante_fab_cd)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SelectorFecha(
                fechaSeleccionada = estadoUi.fechaSeleccionada,
                alRetroceder = {
                    viewModel.seleccionarFecha(estadoUi.fechaSeleccionada.minusDays(1))
                },
                alAvanzar = {
                    viewModel.seleccionarFecha(estadoUi.fechaSeleccionada.plusDays(1))
                }
            )

            CabeceraCuadrante(
                totalEmpleados = estadoUi.asignacionesFiltradas.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                estadoUi.cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                estadoUi.asignacionesFiltradas.isEmpty() -> {
                    EstadoVacio(modifier = Modifier.fillMaxSize())
                }
                else -> {
                    ListaEmpleados(
                        asignaciones = estadoUi.asignacionesFiltradas,
                        idSupervisor = estadoUi.idSupervisor,
                        eliminando = estadoUi.eliminando,
                        onEditar = viewModel::abrirBottomSheetEdicion,
                        onEliminar = viewModel::confirmarEliminar,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (estadoUi.mostrarBottomSheet) {
        BottomSheetAsignacionTurno(
            estadoUi = estadoUi,
            onDismiss = viewModel::cerrarBottomSheet,
            onEmpleadoSeleccionado = viewModel::seleccionarEmpleado,
            onTurnoSeleccionado = viewModel::seleccionarTurno,
            onFechaSeleccionada = viewModel::seleccionarFechaAsignacion,
            onModalidadSeleccionada = viewModel::seleccionarModalidad,
            onMotivoCambio = viewModel::actualizarMotivoCambio,
            onAsignar = viewModel::asignarTurno
        )
    }

    estadoUi.asignacionAEliminar?.let { asignacion ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = { Text(stringResource(R.string.cuadrante_dialog_eliminar_titulo)) },
            text = { Text(stringResource(R.string.cuadrante_dialog_eliminar_mensaje)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::eliminarAsignacion,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ColorEliminar
                    )
                ) {
                    Text(stringResource(R.string.cuadrante_dialog_eliminar_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEliminar) {
                    Text(stringResource(R.string.cuadrante_dialog_eliminar_cancelar))
                }
            }
        )
    }
}

@Composable
private fun SelectorFecha(
    fechaSeleccionada: LocalDate,
    alRetroceder: () -> Unit,
    alAvanzar: () -> Unit
) {
    val hoy = remember { LocalDate.now() }
    val esHoy = fechaSeleccionada == hoy
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = alRetroceder) {
            Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fechaSeleccionada.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (esHoy) {
                Text(
                    text = stringResource(R.string.cuadrante_subtitulo_hoy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = alAvanzar) {
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun CabeceraCuadrante(
    totalEmpleados: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.cuadrante_contador_empleados, totalEmpleados),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun ListaEmpleados(
    asignaciones: List<RespuestaAsignacionTurnoDTO>,
    idSupervisor: Long,
    eliminando: Boolean,
    onEditar: (RespuestaAsignacionTurnoDTO) -> Unit,
    onEliminar: (RespuestaAsignacionTurnoDTO) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(asignaciones, key = { it.idAsignacion }) { asignacion ->
            TarjetaEmpleado(
                asignacion = asignacion,
                idSupervisor = idSupervisor,
                eliminando = eliminando,
                onEditar = { onEditar(asignacion) },
                onEliminar = { onEliminar(asignacion) }
            )
        }
    }
}

@Composable
private fun TarjetaEmpleado(
    asignacion: RespuestaAsignacionTurnoDTO,
    idSupervisor: Long,
    eliminando: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val iniciales = remember(asignacion.nombreCompletoEmpleado) {
        asignacion.nombreCompletoEmpleado
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarIniciales(iniciales = iniciales)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asignacion.nombreCompletoEmpleado,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BadgeModalidad(modalidad = asignacion.modalidad)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val horario = when {
                                asignacion.horaInicio != null && asignacion.horaFin != null ->
                                    stringResource(
                                        R.string.cuadrante_horario,
                                        asignacion.horaInicio.format(FormatoHora),
                                        asignacion.horaFin.format(FormatoHora)
                                    )
                                else -> asignacion.descripcionTurno
                            }
                            Text(
                                text = horario,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (asignacion.idEmpleado != idSupervisor) {
                    IconButton(onClick = onEditar, enabled = !eliminando) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEliminar, enabled = !eliminando) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = ColorEliminar
                        )
                    }
                }
            }

            if (asignacion.fechaCambio != null && asignacion.responsableCambio != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.cuadrante_modificado_por,
                        asignacion.responsableCambio,
                        asignacion.fechaCambio.format(FormatoFechaModificacion),
                        asignacion.motivoCambio ?: ""
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AvatarIniciales(iniciales: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BadgeModalidad(modalidad: ModalidadAsignacion) {
    val (color, textoRes) = when (modalidad) {
        ModalidadAsignacion.PRESENCIAL ->
            ColorPresencial to R.string.cuadrante_modalidad_presencial
        ModalidadAsignacion.TELETRABAJO ->
            ColorTeletrabajo to R.string.cuadrante_modalidad_teletrabajo
    }
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(textoRes),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
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
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.cuadrante_vacio),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetAsignacionTurno(
    estadoUi: EstadoUiCuadranteDepartamento,
    onDismiss: () -> Unit,
    onEmpleadoSeleccionado: (RespuestaEmpleadoDTO) -> Unit,
    onTurnoSeleccionado: (RespuestaTurnoDTO) -> Unit,
    onFechaSeleccionada: (LocalDate) -> Unit,
    onModalidadSeleccionada: (ModalidadAsignacion) -> Unit,
    onMotivoCambio: (String) -> Unit,
    onAsignar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    if (estadoUi.modoEdicion) R.string.cuadrante_sheet_titulo_edicion
                    else R.string.cuadrante_sheet_titulo
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (estadoUi.cargandoCatalogos) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                DropdownEmpleado(
                    empleados = estadoUi.empleados,
                    seleccionado = estadoUi.empleadoSeleccionado,
                    onSeleccionar = onEmpleadoSeleccionado
                )

                DropdownTurno(
                    turnos = estadoUi.turnos,
                    seleccionado = estadoUi.turnoSeleccionado,
                    onSeleccionar = onTurnoSeleccionado
                )

                OutlinedTextField(
                    value = estadoUi.fechaAsignacion.format(formatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.cuadrante_label_fecha)) },
                    trailingIcon = {
                        IconButton(onClick = { mostrarSelectorFecha = true }) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = stringResource(R.string.cuadrante_label_fecha)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownModalidad(
                    modalidades = estadoUi.modalidades,
                    seleccionada = estadoUi.modalidadSeleccionada,
                    onSeleccionar = onModalidadSeleccionada
                )

                if (estadoUi.modoEdicion) {
                    OutlinedTextField(
                        value = estadoUi.motivoCambio,
                        onValueChange = onMotivoCambio,
                        label = { Text(stringResource(R.string.cuadrante_label_motivo_edicion)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                Button(
                    onClick = onAsignar,
                    enabled = estadoUi.formularioValido && !estadoUi.estaAsignando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (estadoUi.estaAsignando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            stringResource(
                                if (estadoUi.modoEdicion) R.string.cuadrante_btn_actualizar
                                else R.string.cuadrante_btn_asignar
                            )
                        )
                    }
                }
            }
        }
    }

    if (mostrarSelectorFecha) {
        val zonaHoraria = ZoneId.systemDefault()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = estadoUi.fechaAsignacion
                .atStartOfDay(zonaHoraria)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fecha = Instant.ofEpochMilli(millis)
                            .atZone(zonaHoraria)
                            .toLocalDate()
                        onFechaSeleccionada(fecha)
                    }
                    mostrarSelectorFecha = false
                }) {
                    Text(stringResource(R.string.ausencia_dialog_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) {
                    Text(stringResource(R.string.ausencia_dialog_cancelar))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownEmpleado(
    empleados: List<RespuestaEmpleadoDTO>,
    seleccionado: RespuestaEmpleadoDTO?,
    onSeleccionar: (RespuestaEmpleadoDTO) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = seleccionado?.let { "${it.nombre} ${it.apellidos}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cuadrante_label_empleado)) },
            trailingIcon = {
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownTurno(
    turnos: List<RespuestaTurnoDTO>,
    seleccionado: RespuestaTurnoDTO?,
    onSeleccionar: (RespuestaTurnoDTO) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val textoSeleccionado = seleccionado?.let { turno ->
        if (turno.horaInicio != null && turno.horaFin != null) {
            "${turno.descripcion} · ${turno.horaInicio.format(FormatoHora)} - ${turno.horaFin.format(FormatoHora)}"
        } else {
            turno.descripcion
        }
    } ?: ""

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = textoSeleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cuadrante_label_turno)) },
            trailingIcon = {
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            turnos.forEach { turno ->
                val etiqueta = if (turno.horaInicio != null && turno.horaFin != null) {
                    "${turno.descripcion} · ${turno.horaInicio.format(FormatoHora)} - ${turno.horaFin.format(FormatoHora)}"
                } else {
                    turno.descripcion
                }
                DropdownMenuItem(
                    text = { Text(etiqueta) },
                    onClick = {
                        onSeleccionar(turno)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownModalidad(
    modalidades: List<ModalidadAsignacion>,
    seleccionada: ModalidadAsignacion?,
    onSeleccionar: (ModalidadAsignacion) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val textoPresencial = stringResource(R.string.cuadrante_modalidad_presencial)
    val textoTeletrabajo = stringResource(R.string.cuadrante_modalidad_teletrabajo)

    fun etiqueta(modalidad: ModalidadAsignacion) = when (modalidad) {
        ModalidadAsignacion.PRESENCIAL -> textoPresencial
        ModalidadAsignacion.TELETRABAJO -> textoTeletrabajo
    }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = seleccionada?.let { etiqueta(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cuadrante_label_modalidad)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            modalidades.forEach { modalidad ->
                DropdownMenuItem(
                    text = { Text(etiqueta(modalidad)) },
                    onClick = {
                        onSeleccionar(modalidad)
                        expandido = false
                    }
                )
            }
        }
    }
}