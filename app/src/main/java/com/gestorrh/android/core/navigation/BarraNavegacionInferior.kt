package com.gestorrh.android.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Componente visual de navegación inferior (BottomBar) basado en Material Design 3.
 * Este componente no decide qué pestañas mostrar, solo dibuja
 * la lista de destinos que se le pasa por parámetro. Esto permite inyectar
 * pestañas dinámicamente según el rol del usuario (Empleado vs Supervisor).
 *
 * @param controladorNavegacion El router de Jetpack Compose para efectuar los saltos de pantalla.
 * @param destinos Lista de pestañas (RutasDestino) permitidas para la sesión actual.
 */
@Composable
fun BarraNavegacionInferior(
    controladorNavegacion: NavHostController,
    destinos: List<RutasDestino>
) {
    val entradaPilaNavegacion by controladorNavegacion.currentBackStackEntryAsState()
    val rutaActual = entradaPilaNavegacion?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        destinos.forEach { destino ->
            NavigationBarItem(
                icon = { Icon(imageVector = destino.icono, contentDescription = stringResource(id = destino.tituloResId)) },
                label = { Text(text = stringResource(id = destino.tituloResId)) },
                selected = rutaActual == destino.ruta,
                onClick = {
                    if (rutaActual != destino.ruta) {
                        controladorNavegacion.navigate(destino.ruta) {
                            popUpTo(controladorNavegacion.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

