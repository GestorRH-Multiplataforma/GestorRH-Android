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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    alVerHistorial: () -> Unit,
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = alVerHistorial,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.dashboard_btn_ver_historial))
            }

            Spacer(modifier = Modifier.height(32.dp))
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TarjetaInformativa(
                            modifier = Modifier.weight(1f),
                            titulo = stringResource(id = R.string.dashboard_turno_titulo),
                            valor = if (estadoUi.tieneTurnoHoy) estadoUi.modalidadHoy?.name ?: stringResource(id = R.string.dashboard_sin_asignar) else stringResource(id = R.string.dashboard_libre),
                            icono = Icons.Filled.CalendarToday
                        )
                        TarjetaInformativa(
                            modifier = Modifier.weight(1f),
                            titulo = stringResource(id = R.string.dashboard_ausencia_titulo),
                            valor = stringResource(id = R.string.dashboard_cero_pendientes),
                            icono = Icons.Filled.EventBusy
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
    icono: ImageVector
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
        }
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
