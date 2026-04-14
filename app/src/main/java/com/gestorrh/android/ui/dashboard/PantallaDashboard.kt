package com.gestorrh.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.gestorrh.android.core.location.GestorLocalizacion

/**
 * Pantalla principal (Portal del Empleado).
 * Muestra la información contextual del empleado, el estado de sus turnos y ausencias,
 * y reserva el espacio central para el motor de fichaje.
 */
@Composable
fun PantallaDashboard(
    viewModel: DashboardViewModel = viewModel()
) {
    val estadoUi by viewModel.estadoUi.collectAsState()
    val contexto = LocalContext.current
    val scopeDeCorrutina = rememberCoroutineScope()
    val gestorLocalizacion = remember { GestorLocalizacion(contexto) }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { concedido ->
            if (concedido) {
                scopeDeCorrutina.launch {
                    val ubicacion = gestorLocalizacion.obtenerUbicacionActual()
                    viewModel.alternarFichaje(ubicacion)
                }
            } else {
                println("Fichaje cancelado: Permiso de ubicación denegado.")
            }
        }
    )

    val intentarFichar: () -> Unit = {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        CabeceraDashboard()

        Spacer(modifier = Modifier.height(40.dp))

        WidgetFichaje(
            estadoActual = estadoUi.estadoActual,
            tiempoTranscurrido = estadoUi.tiempoTranscurrido,
            alClickFichar = intentarFichar
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaInformativa(
                modifier = Modifier.weight(1f),
                titulo = stringResource(id = R.string.dashboard_turno_titulo),
                valor = stringResource(id = R.string.dashboard_turno_valor),
                icono = Icons.Filled.CalendarToday
            )
            TarjetaInformativa(
                modifier = Modifier.weight(1f),
                titulo = stringResource(id = R.string.dashboard_ausencia_titulo),
                valor = stringResource(id = R.string.dashboard_ausencia_valor),
                icono = Icons.Filled.EventBusy
            )
        }
    }
}

@Composable
private fun CabeceraDashboard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.dashboard_fecha_ejemplo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.dashboard_saludo),
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
                contentDescription = "Perfil",
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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

/**
 * Componente visual dinámico (UI Reactiva).
 * Cambia sus colores, textos y acciones en base al [EstadoFichaje].
 */
@Composable
private fun WidgetFichaje(
    estadoActual: EstadoFichaje,
    tiempoTranscurrido: String,
    alClickFichar: () -> Unit
) {
    val esActivo = estadoActual == EstadoFichaje.TRABAJANDO

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

            Button(
                onClick = alClickFichar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colorBoton)
            ) {
                Text(
                    text = stringResource(id = textoBoton),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

