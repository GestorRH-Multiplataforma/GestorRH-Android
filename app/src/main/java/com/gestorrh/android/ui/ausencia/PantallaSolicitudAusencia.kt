package com.gestorrh.android.ui.ausencia

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.ui.MensajeUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val ColorAvisoAmbar = Color(0xFFB26A00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSolicitudAusencia(
    contexto: Context = LocalContext.current,
    datosPrerelleno: SolicitudAusenciaViewModel.PrerrellenoEdicion? = null,
    viewModel: SolicitudAusenciaViewModel = viewModel(
        key = datosPrerelleno?.idAusencia?.let { "editar-$it" } ?: "crear",
        factory = SolicitudAusenciaViewModel.factory(
            contexto.applicationContext,
            datosPrerelleno
        )
    ),
    alVolver: () -> Unit = {},
    alEnvioExitoso: () -> Unit = {}
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contextoLocal = LocalContext.current
    val mensajeExito = stringResource(R.string.ausencia_envio_exitoso)

    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.seleccionarArchivo(uri)
    }

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensaje ->
            val texto = when (mensaje) {
                is MensajeUi.Recurso -> contextoLocal.getString(mensaje.idRecurso)
                is MensajeUi.Dinamico -> mensaje.texto
            }
            snackbarHostState.showSnackbar(texto, duration = SnackbarDuration.Long)
            viewModel.errorMostrado()
        }
    }

    LaunchedEffect(estadoUi.envioExitoso) {
        if (estadoUi.envioExitoso) {
            snackbarHostState.showSnackbar(mensajeExito, duration = SnackbarDuration.Short)
            viewModel.envioConsumido()
            alEnvioExitoso()
        }
    }

    val tituloRes = if (estadoUi.modoEdicion) {
        R.string.ausencia_titulo_pantalla_editar
    } else {
        R.string.ausencia_titulo_pantalla
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(tituloRes)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DesplegableTipo(estadoUi, viewModel)

            CampoFecha(
                etiqueta = stringResource(R.string.ausencia_label_fecha_inicio),
                fecha = estadoUi.fechaInicio,
                errorRes = estadoUi.errorFechaInicio,
                onFechaSeleccionada = viewModel::cambiarFechaInicio,
                fechaMinima = LocalDate.now()
            )

            CampoFecha(
                etiqueta = stringResource(R.string.ausencia_label_fecha_fin),
                fecha = estadoUi.fechaFin,
                errorRes = estadoUi.errorFechaFin,
                onFechaSeleccionada = viewModel::cambiarFechaFin,
                fechaMinima = estadoUi.fechaInicio ?: LocalDate.now()
            )

            OutlinedTextField(
                value = estadoUi.descripcion,
                onValueChange = viewModel::cambiarDescripcion,
                label = { Text(stringResource(R.string.ausencia_label_descripcion)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            if (!estadoUi.modoEdicion) {
                BotonAdjuntar(
                    nombreArchivo = estadoUi.nombreArchivo,
                    onAdjuntar = { selectorArchivo.launch("*/*") },
                    onQuitar = viewModel::quitarArchivo
                )

                estadoUi.avisoJustificante?.let { idAviso ->
                    Text(
                        text = stringResource(idAviso),
                        color = ColorAvisoAmbar,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::enviar,
                enabled = estadoUi.formularioValido && !estadoUi.enviando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estadoUi.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    val textoBoton = if (estadoUi.modoEdicion) {
                        R.string.ausencia_btn_actualizar
                    } else {
                        R.string.ausencia_btn_enviar
                    }
                    Text(stringResource(textoBoton))
                }
            }

            OutlinedButton(
                onClick = alVolver,
                modifier = Modifier.fillMaxWidth(),
                enabled = !estadoUi.enviando
            ) {
                Text(stringResource(R.string.ausencia_btn_cancelar))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesplegableTipo(
    estadoUi: EstadoUiSolicitudAusencia,
    viewModel: SolicitudAusenciaViewModel
) {
    var expandido by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = estadoUi.tipoSeleccionado?.let { etiquetaTipo(it) }
                ?: if (estadoUi.cargandoTipos) stringResource(R.string.ausencia_cargando_tipos) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ausencia_label_tipo)) },
            trailingIcon = {
                IconButton(onClick = {
                    if (estadoUi.tiposDisponibles.isNotEmpty()) expandido = true
                }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            isError = estadoUi.errorTipo != null,
            supportingText = {
                estadoUi.errorTipo?.let { Text(stringResource(it)) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            estadoUi.tiposDisponibles.forEach { tipo ->
                DropdownMenuItem(
                    text = { Text(etiquetaTipo(tipo)) },
                    onClick = {
                        viewModel.seleccionarTipo(tipo)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate?,
    errorRes: Int?,
    onFechaSeleccionada: (LocalDate) -> Unit,
    fechaMinima: LocalDate
) {
    var mostrandoSelector by remember { mutableStateOf(false) }
    val texto = fecha?.format(SolicitudAusenciaViewModel.FORMATO_FECHA) ?: ""

    OutlinedTextField(
        value = texto,
        onValueChange = {},
        readOnly = true,
        label = { Text(etiqueta) },
        trailingIcon = {
            IconButton(onClick = { mostrandoSelector = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = etiqueta)
            }
        },
        isError = errorRes != null,
        supportingText = {
            errorRes?.let { Text(stringResource(it)) }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (mostrandoSelector) {
        val estadoPicker = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: fechaMinima)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrandoSelector = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoPicker.selectedDateMillis?.let { millis ->
                        val fechaSeleccionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onFechaSeleccionada(fechaSeleccionada)
                    }
                    mostrandoSelector = false
                }) {
                    Text(stringResource(R.string.ausencia_dialog_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoSelector = false }) {
                    Text(stringResource(R.string.ausencia_dialog_cancelar))
                }
            }
        ) {
            DatePicker(state = estadoPicker)
        }
    }
}

@Composable
private fun BotonAdjuntar(
    nombreArchivo: String?,
    onAdjuntar: () -> Unit,
    onQuitar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = onAdjuntar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AttachFile, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.ausencia_btn_adjuntar))
        }
        if (nombreArchivo != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = nombreArchivo,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = 40.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onQuitar,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.ausencia_cd_quitar_archivo)
                    )
                }
            }
        }
    }
}

@Composable
private fun etiquetaTipo(tipo: String): String {
    val resId = when (tipo) {
        "MEDICA" -> R.string.ausencia_tipo_medica
        "VACACIONES" -> R.string.ausencia_tipo_vacaciones
        "MOTIVO_PERSONAL" -> R.string.ausencia_tipo_motivo_personal
        "OTROS" -> R.string.ausencia_tipo_otros
        else -> null
    }
    return resId?.let { stringResource(it) } ?: tipo
}
