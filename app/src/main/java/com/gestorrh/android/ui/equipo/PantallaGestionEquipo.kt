package com.gestorrh.android.ui.equipo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestorrh.android.R

/**
 * Pantalla contenedora de las funcionalidades del rol SUPERVISOR.
 *
 * Actúa como shell vacío listo para recibir el contenido.
 * Cada subsección se implementará como una pantalla
 * independiente que se mostrará en el área de contenido de esta pantalla
 * según la pestaña activa.
 *
 * La pestaña seleccionada se gestiona como estado local del composable
 * ya que no hay lógica de negocio asociada hasta que se implementen
 * las subsecciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGestionEquipo() {
    val tabs = listOf(
        stringResource(R.string.equipo_tab_ausencias),
        stringResource(R.string.equipo_tab_cuadrante),
        stringResource(R.string.equipo_tab_turnos),
        stringResource(R.string.equipo_tab_fichajes)
    )

    var tabSeleccionada by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.equipo_titulo),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = tabSeleccionada,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { indice, titulo ->
                    Tab(
                        selected = tabSeleccionada == indice,
                        onClick = { tabSeleccionada = indice },
                        text = {
                            Text(
                                text = titulo,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            ContenidoTab(tabSeleccionada = tabSeleccionada)
        }
    }
}

/**
 * Área de contenido de cada pestaña del supervisor.
 * Muestra un placeholder hasta que cada subsección sea implementada.
 */
@Composable
private fun ContenidoTab(tabSeleccionada: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.equipo_proximamente),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}