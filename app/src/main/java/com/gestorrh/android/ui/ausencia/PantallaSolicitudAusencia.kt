package com.gestorrh.android.ui.ausencia

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gestorrh.android.R
import com.gestorrh.android.core.archivos.GestorArchivosJustificante
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.ausencia.TipoAusencia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val ColorAvisoAmbar = Color(0xFFF57C00)

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
    val recursos = LocalResources.current
    val mensajeExito = stringResource(R.string.ausencia_envio_exitoso)

    var mostrarSelectorFuente by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }
    var uriCapturaPendiente by remember { mutableStateOf<GestorArchivosJustificante.UriCaptura?>(null) }

    val hayDatosEnFormulario = estadoUi.tipoSeleccionado != null ||
        estadoUi.fechaInicio != null ||
        estadoUi.fechaFin != null ||
        estadoUi.descripcion.isNotBlank() ||
        estadoUi.archivoUri != null

    val intentarVolver: () -> Unit = {
        if (hayDatosEnFormulario) {
            mostrarDialogoSalir = true
        } else {
            alVolver()
        }
    }

    BackHandler(enabled = hayDatosEnFormulario) {
        mostrarDialogoSalir = true
    }

    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.seleccionarArchivoSeleccionado(uri)
    }

    val selectorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val pendiente = uriCapturaPendiente
        if (exito && pendiente != null) {
            viewModel.registrarFotoCamara(pendiente.uri, pendiente.rutaAbsoluta)
        }
        uriCapturaPendiente = null
    }

    val solicitudPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val captura = GestorArchivosJustificante.crearUriCapturaCamara(contextoLocal)
            uriCapturaPendiente = captura
            selectorCamara.launch(captura.uri)
        } else {
            viewModel.errorMostrado()
        }
    }

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

    LaunchedEffect(estadoUi.envioExitoso) {
        if (estadoUi.envioExitoso) {
            snackbarHostState.showSnackbar(mensajeExito, duration = SnackbarDuration.Short)
            viewModel.envioConsumido()
            alEnvioExitoso()
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

            SeccionAdjunto(
                estadoUi = estadoUi,
                onAdjuntar = { mostrarSelectorFuente = true },
                onQuitar = viewModel::quitarArchivo,
                onDescargarExistente = viewModel::descargarJustificanteExistente,
                onEliminarExistente = viewModel::eliminarJustificanteExistente
            )

            estadoUi.avisoJustificante?.let { idAviso ->
                Text(
                    text = stringResource(idAviso),
                    color = ColorAvisoAmbar,
                    style = MaterialTheme.typography.bodySmall
                )
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
                onClick = intentarVolver,
                modifier = Modifier.fillMaxWidth(),
                enabled = !estadoUi.enviando
            ) {
                Text(stringResource(R.string.ausencia_btn_volver))
            }
        }
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            title = { Text(stringResource(R.string.ausencia_dialogo_salir_titulo)) },
            text = { Text(stringResource(R.string.ausencia_dialogo_salir_mensaje)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoSalir = false
                        alVolver()
                    }
                ) {
                    Text(stringResource(R.string.ausencia_dialogo_salir_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalir = false }) {
                    Text(stringResource(R.string.ausencia_dialogo_salir_cancelar))
                }
            }
        )
    }

    if (mostrarSelectorFuente) {
        SelectorFuenteAdjunto(
            onCerrar = { mostrarSelectorFuente = false },
            onCamaraSeleccionada = {
                mostrarSelectorFuente = false
                val permiso = ContextCompat.checkSelfPermission(
                    contextoLocal, Manifest.permission.CAMERA
                )
                if (permiso == PackageManager.PERMISSION_GRANTED) {
                    val captura = GestorArchivosJustificante.crearUriCapturaCamara(contextoLocal)
                    uriCapturaPendiente = captura
                    selectorCamara.launch(captura.uri)
                } else {
                    solicitudPermisoCamara.launch(Manifest.permission.CAMERA)
                }
            },
            onArchivoSeleccionado = {
                mostrarSelectorFuente = false
                selectorArchivo.launch("*/*")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFuenteAdjunto(
    onCerrar: () -> Unit,
    onCamaraSeleccionada: () -> Unit,
    onArchivoSeleccionado: () -> Unit
) {
    val estadoSheet = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estadoSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ausencia_selector_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OpcionSelector(
                icono = Icons.Filled.CameraAlt,
                texto = stringResource(R.string.ausencia_selector_camara),
                onClick = onCamaraSeleccionada
            )
            OpcionSelector(
                icono = Icons.Filled.FolderOpen,
                texto = stringResource(R.string.ausencia_selector_archivo),
                onClick = onArchivoSeleccionado
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OpcionSelector(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icono, contentDescription = null)
        Spacer(Modifier.size(12.dp))
        Text(text = texto, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SeccionAdjunto(
    estadoUi: EstadoUiSolicitudAusencia,
    onAdjuntar: () -> Unit,
    onQuitar: () -> Unit,
    onDescargarExistente: () -> Unit,
    onEliminarExistente: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (estadoUi.hayJustificanteExistenteVisible) {
            JustificanteExistente(
                nombre = estadoUi.nombreJustificanteExistente!!,
                descargando = estadoUi.descargandoJustificante,
                onDescargar = onDescargarExistente,
                onEliminar = onEliminarExistente
            )
        }

        OutlinedButton(
            onClick = onAdjuntar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AttachFile, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            val textoBoton = if (estadoUi.hayJustificanteExistenteVisible) {
                R.string.ausencia_btn_reemplazar
            } else {
                R.string.ausencia_btn_adjuntar
            }
            Text(stringResource(textoBoton))
        }

        if (estadoUi.archivoUri != null && estadoUi.nombreArchivo != null) {
            PreviaArchivoSeleccionado(
                uri = estadoUi.archivoUri,
                nombre = estadoUi.nombreArchivo,
                esImagen = estadoUi.esImagen,
                onQuitar = onQuitar
            )
        }
    }
}

@Composable
private fun JustificanteExistente(
    nombre: String,
    descargando: Boolean,
    onDescargar: () -> Unit,
    onEliminar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.FileOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ausencia_justificante_actual),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onDescargar, enabled = !descargando) {
            if (descargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.ausencia_cd_descargar_justificante)
                )
            }
        }
        IconButton(onClick = onEliminar) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.ausencia_cd_eliminar_justificante_existente),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PreviaArchivoSeleccionado(
    uri: Uri,
    nombre: String,
    esImagen: Boolean,
    onQuitar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (esImagen) {
            MiniaturaImagen(
                uri = uri,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onQuitar) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.ausencia_cd_quitar_archivo)
            )
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
            value = estadoUi.tipoSeleccionado?.let { tipo ->
                runCatching { TipoAusencia.valueOf(tipo) }
                    .getOrNull()
                    ?.let { etiquetaTipo(it) }
                    ?: tipo
            }  ?: if (estadoUi.cargandoTipos) stringResource(R.string.ausencia_cargando_tipos) else "",
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
                    text = { Text(runCatching { TipoAusencia.valueOf(tipo) }.getOrNull()?.let { etiquetaTipo(it) } ?: tipo) },
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
    fechaMinima: LocalDate?
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
            initialSelectedDateMillis = (fecha ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrandoSelector = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoPicker.selectedDateMillis?.let { millis ->
                        val fechaSeleccionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
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
private fun MiniaturaImagen(uri: Uri, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
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
