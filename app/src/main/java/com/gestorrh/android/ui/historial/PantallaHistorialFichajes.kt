package com.gestorrh.android.ui.historial

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ColorEnCurso = Color(0xFF2E7D32)
private val ColorIncidencia = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorialFichajes(
    alVolver: () -> Unit,
    contexto: Context = LocalContext.current,
    viewModel: HistorialFichajesViewModel = viewModel(
        factory = HistorialFichajesViewModel.crearFactory(contexto.applicationContext)
    )
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contextoLocal = LocalContext.current

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensajeUi ->
            val texto = when (mensajeUi) {
                is MensajeUi.Recurso -> contextoLocal.getString(mensajeUi.idRecurso)
                is MensajeUi.Dinamico -> mensajeUi.texto
            }
            snackbarHostState.showSnackbar(texto)
            viewModel.errorMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.historial_fichajes_titulo)) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.historial_fichajes_cd_volver)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingInterior ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterior)
        ) {
            FilaFiltrosFechas(
                fechaInicio = estadoUi.fechaInicio,
                fechaFin = estadoUi.fechaFin,
                cargando = estadoUi.cargando,
                alCambiarFechaInicio = viewModel::actualizarFechaInicio,
                alCambiarFechaFin = viewModel::actualizarFechaFin,
                alAplicarFiltro = viewModel::aplicarFiltro
            )

            HorizontalDivider()

            when {
                estadoUi.cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                estadoUi.fichajes.isEmpty() -> {
                    EstadoVacio()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = estadoUi.fichajes,
                            key = { it.idFichaje }
                        ) { fichaje ->
                            TarjetaFichaje(fichaje = fichaje)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaFiltrosFechas(
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    cargando: Boolean,
    alCambiarFechaInicio: (LocalDate) -> Unit,
    alCambiarFechaFin: (LocalDate) -> Unit,
    alAplicarFiltro: () -> Unit
) {
    val formateador = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var mostrarSelectorInicio by remember { mutableStateOf(false) }
    var mostrarSelectorFin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonSelectorFecha(
                etiqueta = stringResource(R.string.historial_fichajes_fecha_inicio),
                fecha = fechaInicio,
                formateador = formateador,
                modifier = Modifier.weight(1f),
                alSeleccionar = { mostrarSelectorInicio = true }
            )
            BotonSelectorFecha(
                etiqueta = stringResource(R.string.historial_fichajes_fecha_fin),
                fecha = fechaFin,
                formateador = formateador,
                modifier = Modifier.weight(1f),
                alSeleccionar = { mostrarSelectorFin = true }
            )
        }

        Button(
            onClick = alAplicarFiltro,
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando
        ) {
            Text(stringResource(R.string.historial_fichajes_aplicar_filtro))
        }
    }

    if (mostrarSelectorInicio) {
        SelectorFecha(
            fechaInicial = fechaInicio,
            alSeleccionar = alCambiarFechaInicio,
            alDescartar = { mostrarSelectorInicio = false }
        )
    }

    if (mostrarSelectorFin) {
        SelectorFecha(
            fechaInicial = fechaFin,
            alSeleccionar = alCambiarFechaFin,
            alDescartar = { mostrarSelectorFin = false }
        )
    }
}

@Composable
private fun BotonSelectorFecha(
    etiqueta: String,
    fecha: LocalDate,
    formateador: DateTimeFormatter,
    modifier: Modifier = Modifier,
    alSeleccionar: () -> Unit
) {
    OutlinedButton(
        onClick = alSeleccionar,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = fecha.format(formateador),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Selector de fecha modal basado en el `DatePicker` de Material 3 Compose. Reemplaza
 * al `DatePickerDialog` nativo de Android para que los colores del calendario y de
 * los botones respeten la paleta declarada en `MaterialTheme`.
 *
 * Las fechas se intercambian con el `DatePickerState` como milisegundos UTC, por lo
 * que se convierten desde y hacia [LocalDate] usando la zona horaria del dispositivo.
 *
 * @param fechaInicial Fecha mostrada como seleccionada al abrir el diálogo.
 * @param alSeleccionar Callback con la fecha confirmada por el usuario.
 * @param alDescartar Callback invocado tanto al cancelar como tras confirmar para
 *                    cerrar el diálogo.
 */
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
                    val fecha = Instant.ofEpochMilli(millis)
                        .atZone(zonaHoraria)
                        .toLocalDate()
                    alSeleccionar(fecha)
                }
                alDescartar()
            }) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = alDescartar) {
                Text(stringResource(id = android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TarjetaFichaje(fichaje: RespuestaFichajeDTO) {
    val formateadorFecha = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formateadorHora = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = fichaje.fecha.format(formateadorFecha),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            fichaje.descripcionTurno?.let { turno ->
                Text(
                    text = turno,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                        text = fichaje.horaEntrada.format(formateadorHora),
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
                            text = fichaje.horaSalida.format(formateadorHora),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.historial_fichajes_en_curso),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorEnCurso
                        )
                    }
                }
            }

            fichaje.incidencias?.let { incidencia ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = ColorIncidencia
                ) {
                    Text(
                        text = incidencia,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
            imageVector = Icons.Filled.History,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.historial_fichajes_vacio),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

