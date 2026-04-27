package com.gestorrh.android.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.location.GestorLocalizacion
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.fichaje.ModalidadTurno
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import com.gestorrh.android.ui.theme.SemanticSuccess
import com.gestorrh.android.ui.theme.SemanticWarning
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.WifiOff
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityCompat

@Composable
fun PantallaDashboard(
    alVerHistorialCompleto: () -> Unit,
    contexto: android.content.Context = LocalContext.current,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(contexto.applicationContext)
    )
) {
    val estadoUi by viewModel.estadoUi.collectAsState()

    val scopeDeCorrutina = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val gestorLocalizacion = remember { GestorLocalizacion(contexto) }

    val errorPermisoTexto = stringResource(id = R.string.error_permiso_ubicacion)
    val contextoLocal = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.cargarFichajesPendientes()
    }

    val propietarioCicloVida = LocalLifecycleOwner.current
    DisposableEffect(propietarioCicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                viewModel.sincronizarEstado()
                viewModel.cargarFichajesPendientes()
                viewModel.cargarProximoTurno()
                viewModel.cargarProximaAusencia()
                viewModel.cargarUltimosFichajes()
            }
        }
        propietarioCicloVida.lifecycle.addObserver(observador)
        onDispose {
            propietarioCicloVida.lifecycle.removeObserver(observador)
        }
    }

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensajeUi ->
            val textoMensaje = when (mensajeUi) {
                is MensajeUi.Recurso -> contextoLocal.getString(mensajeUi.idRecurso)
                is MensajeUi.Dinamico -> mensajeUi.texto
            }
            snackbarHostState.showSnackbar(textoMensaje)
            viewModel.errorMostrado()
        }
    }

    LaunchedEffect(estadoUi.mensajeInfo) {
        estadoUi.mensajeInfo?.let { mensajeUi ->
            val textoMensaje = when (mensajeUi) {
                is MensajeUi.Recurso -> contextoLocal.getString(mensajeUi.idRecurso)
                is MensajeUi.Dinamico -> mensajeUi.texto
            }
            snackbarHostState.showSnackbar(textoMensaje)
            viewModel.infoMostrado()
        }
    }

    val activity = contexto as? android.app.Activity
    val textoCta = stringResource(id = R.string.error_permiso_ubicacion_btn_ajustes)
    val textoPermanente = stringResource(id = R.string.error_permiso_ubicacion_permanente)

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { concedido ->
            if (concedido) {
                scopeDeCorrutina.launch {
                    val ubicacion = gestorLocalizacion.obtenerUbicacionActual()
                    viewModel.alternarFichaje(ubicacion)
                }
            } else {
                val denegadoPermanentemente = activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        )

                if (denegadoPermanentemente) {
                    scopeDeCorrutina.launch {
                        val resultado = snackbarHostState.showSnackbar(
                            message = textoPermanente,
                            actionLabel = textoCta,
                            duration = SnackbarDuration.Long
                        )
                        if (resultado == SnackbarResult.ActionPerformed) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", contexto.packageName, null)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            contexto.startActivity(intent)
                        }
                    }
                } else {
                    scopeDeCorrutina.launch {
                        snackbarHostState.showSnackbar(errorPermisoTexto)
                    }
                }
            }
        }
    )

    val intentarFichar: () -> Unit = {
        if (estadoUi.modalidadHoy == ModalidadTurno.TELETRABAJO) {
            viewModel.alternarFichaje(null)
        } else {
            val permisoConcedido = ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (permisoConcedido) {
                scopeDeCorrutina.launch {
                    val ubicacion = gestorLocalizacion.obtenerUbicacionActual()
                    viewModel.alternarFichaje(ubicacion)
                }
            } else {
                lanzadorPermisos.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValores ->
        when {
            estadoUi.errorCritico -> {
                PantallaErrorCriticoDashboard(
                    alReintentar = viewModel::reintentarTrasErrorCritico
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CabeceraDashboard(nombreEmpleado = estadoUi.nombreEmpleado)

                    Spacer(modifier = Modifier.height(40.dp))

                    WidgetFichaje(
                        estadoActual = estadoUi.estadoActual,
                        tiempoTranscurrido = estadoUi.tiempoTranscurrido,
                        estaCargando = estadoUi.estaCargando,
                        modalidad = estadoUi.modalidadHoy,
                        fichajesPendientes = estadoUi.fichajesPendientesSincronizar,
                        alClickFichar = intentarFichar
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TarjetaProximoTurno(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            proximoTurno = estadoUi.proximoTurno
                        )
                        TarjetaProximaAusencia(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            proximaAusencia = estadoUi.proximaAusencia
                        )
                    }

                    if (estadoUi.ultimosFichajes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SeccionUltimosFichajes(
                            ultimosFichajes = estadoUi.ultimosFichajes,
                            alVerTodos = alVerHistorialCompleto
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraDashboard(nombreEmpleado: String) {
    // Fecha actual formateada y localizada con java.time, sin llamadas de red adicionales.
    // El patrón de formato se carga desde strings.xml para que cada locale tenga su propia
    // representación (ES: "EEEE, d 'de' MMMM" / EN: "EEEE, MMMM d").
    val patronFecha = stringResource(id = R.string.dashboard_formato_fecha)
    val fechaHoy = remember(patronFecha) {
        val locale = Locale.getDefault()
        val formatter = DateTimeFormatter.ofPattern(patronFecha, locale)
        val fechaFormateada = LocalDate.now().format(formatter)
        // Capitaliza la primera letra (los locales suelen devolver el día en minúsculas)
        fechaFormateada.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    val saludo = if (nombreEmpleado.isNotEmpty()) {
        stringResource(id = R.string.dashboard_saludo_nombre, nombreEmpleado)
    } else {
        stringResource(id = R.string.dashboard_saludo_generico)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = fechaHoy,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = saludo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(id = R.string.cd_perfil),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TarjetaInformativa(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String,
    icono: ImageVector,
    subtitulo: String? = null,
    detalle: String? = null,
    colorDetalle: Color? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitulo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!detalle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorDetalle ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TarjetaProximoTurno(
    modifier: Modifier = Modifier,
    proximoTurno: ResumenProximoTurno?
) {
    val titulo = stringResource(id = R.string.dashboard_turno_titulo)
    if (proximoTurno == null) {
        TarjetaInformativa(
            modifier = modifier,
            titulo = titulo,
            valor = stringResource(id = R.string.dashboard_sin_turno),
            icono = Icons.Filled.Schedule
        )
        return
    }
    TarjetaInformativa(
        modifier = modifier,
        titulo = titulo,
        valor = proximoTurno.nombreTurno,
        subtitulo = formatearFechaYHorarioTurno(
            fecha = proximoTurno.fecha,
            horaInicio = proximoTurno.horaInicio,
            horaFin = proximoTurno.horaFin
        ),
        icono = Icons.Filled.Schedule
    )
}

@Composable
private fun TarjetaProximaAusencia(
    modifier: Modifier = Modifier,
    proximaAusencia: ResumenProximaAusencia?
) {
    val titulo = stringResource(id = R.string.dashboard_ausencia_titulo)
    if (proximaAusencia == null) {
        TarjetaInformativa(
            modifier = modifier,
            titulo = titulo,
            valor = stringResource(id = R.string.dashboard_sin_ausencias),
            icono = Icons.Filled.EventBusy
        )
        return
    }
    val (etiquetaEstado, colorEstado) = when (proximaAusencia.estado) {
        "SOLICITADA" -> stringResource(id = R.string.dashboard_estado_ausencia_pendiente) to SemanticWarning
        "APROBADA" -> stringResource(id = R.string.dashboard_estado_ausencia_aprobada) to SemanticSuccess
        else -> proximaAusencia.estado to MaterialTheme.colorScheme.onSurfaceVariant
    }
    TarjetaInformativa(
        modifier = modifier,
        titulo = titulo,
        valor = etiquetaTipoAusenciaDashboard(proximaAusencia.tipo),
        subtitulo = formatearRangoFechasAusencia(proximaAusencia.fechaInicio, proximaAusencia.fechaFin),
        detalle = etiquetaEstado,
        colorDetalle = colorEstado,
        icono = Icons.Filled.EventBusy
    )
}

@Composable
private fun etiquetaTipoAusenciaDashboard(tipo: String): String {
    val resId = when (tipo) {
        "MEDICA" -> R.string.ausencia_tipo_medica
        "VACACIONES" -> R.string.ausencia_tipo_vacaciones
        "MOTIVO_PERSONAL" -> R.string.ausencia_tipo_motivo_personal
        "OTROS" -> R.string.ausencia_tipo_otros
        else -> null
    }
    return resId?.let { stringResource(it) } ?: tipo
}

@Composable
private fun formatearFechaYHorarioTurno(
    fecha: LocalDate,
    horaInicio: LocalTime?,
    horaFin: LocalTime?
): String {
    val formatoHora = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val formatoFecha = remember { DateTimeFormatter.ofPattern("dd/MM") }
    val locale = Locale.getDefault()
    val horaInicioStr = horaInicio?.format(formatoHora).orEmpty()
    val horaFinStr = horaFin?.format(formatoHora).orEmpty()
    val hoy = LocalDate.now()
    return when {
        fecha == hoy -> stringResource(
            id = R.string.dashboard_turno_horario_hoy,
            horaInicioStr,
            horaFinStr
        )
        fecha == hoy.plusDays(1) -> stringResource(
            id = R.string.dashboard_turno_horario_manana,
            horaInicioStr,
            horaFinStr
        )
        else -> {
            val diaSemana = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            stringResource(
                id = R.string.dashboard_turno_horario_proximo,
                diaSemana,
                fecha.format(formatoFecha),
                horaInicioStr
            )
        }
    }
}

private fun formatearRangoFechasAusencia(inicio: LocalDate, fin: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM")
    return if (inicio == fin) {
        inicio.format(formatter)
    } else {
        "${inicio.format(formatter)} - ${fin.format(formatter)}"
    }
}

@Composable
private fun WidgetFichaje(
    estadoActual: EstadoFichaje,
    tiempoTranscurrido: String,
    estaCargando: Boolean,
    modalidad: ModalidadTurno?,
    fichajesPendientes: Int,
    alClickFichar: () -> Unit
) {
    val esActivo = estadoActual == EstadoFichaje.TRABAJANDO
    val feedbackHaptico = LocalHapticFeedback.current

    val colorFondoCard = if (esActivo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val colorTextoEstado = if (esActivo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val colorBoton = if (esActivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val textoBoton = if (esActivo) R.string.fichaje_btn_finalizar else R.string.fichaje_btn_iniciar
    val textoEstado = if (esActivo) R.string.fichaje_estado_activo else R.string.fichaje_estado_fuera

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondoCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (modalidad == ModalidadTurno.TELETRABAJO) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Home, contentDescription = stringResource(id = R.string.cd_teletrabajo), tint = colorTextoEstado, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.dashboard_modalidad_remota), style = MaterialTheme.typography.labelSmall, color = colorTextoEstado)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = stringResource(id = textoEstado).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = colorTextoEstado,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = tiempoTranscurrido,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        feedbackHaptico.performHapticFeedback(HapticFeedbackType.LongPress)
                        alClickFichar()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colorBoton),
                    enabled = !estaCargando
                ) {
                    if (estaCargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = textoBoton),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (fichajesPendientes > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                    ) {
                        Text(
                            text = fichajesPendientes.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (fichajesPendientes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.fichajes_pendientes, fichajesPendientes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SeccionUltimosFichajes(
    ultimosFichajes: List<RespuestaFichajeDTO>,
    alVerTodos: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_ultimos_fichajes_titulo),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = alVerTodos) {
                Text(stringResource(id = R.string.dashboard_ver_todos))
            }
        }
        ultimosFichajes.forEach { fichaje ->
            CardFichajeResumido(fichaje = fichaje)
        }
    }
}

@Composable
private fun CardFichajeResumido(fichaje: RespuestaFichajeDTO) {
    val formatoFecha = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formatoHora = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fichaje.fecha.format(formatoFecha),
                    style = MaterialTheme.typography.labelMedium
                )
                fichaje.descripcionTurno?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.historial_fichajes_hora_entrada),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = fichaje.horaEntrada.format(formatoHora),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.historial_fichajes_hora_salida),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fichaje.horaSalida != null) {
                        Text(
                            text = fichaje.horaSalida.format(formatoHora),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.historial_fichajes_en_curso),
                            style = MaterialTheme.typography.bodySmall,
                            color = SemanticSuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PantallaErrorCriticoDashboard(
    alReintentar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.dashboard_error_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.dashboard_error_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = alReintentar) {
            Text(text = stringResource(id = R.string.dashboard_error_btn_reintentar))
        }
    }
}
