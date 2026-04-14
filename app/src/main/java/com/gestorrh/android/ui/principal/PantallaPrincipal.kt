package com.gestorrh.android.ui.principal

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gestorrh.android.core.navigation.BarraNavegacionInferior
import com.gestorrh.android.core.navigation.RutasDestino
import com.gestorrh.android.ui.dashboard.PantallaDashboard

/**
 * Contenedor maestro de la experiencia "Post-Login".
 * Implementa el patrón visual Scaffold (Andamio), inyectando la barra de navegación
 * inferior y definiendo el enrutador interno (NavHost) para cambiar entre los
 * dominios de negocio principales (Fichaje, Turnos, Ausencias, Perfil).
 */
@Composable
fun PantallaPrincipal(
    alCerrarSesion: () -> Unit
) {
    val controladorNavegacionInterno = rememberNavController()

    // PUNTO ESTRATÉGICO PARA LA ÉPICA 6:
    // Aquí es donde en el futuro leeremos el rol del usuario.
    val pestañasBase = listOf(
        RutasDestino.Inicio,
        RutasDestino.Turnos,
        RutasDestino.Ausencias,
        RutasDestino.Perfil
    )

    Scaffold(
        bottomBar = {
            BarraNavegacionInferior(
                controladorNavegacion = controladorNavegacionInterno,
                destinos = pestañasBase
            )
        }
    ) { paddingInterior ->

        NavHost(
            navController = controladorNavegacionInterno,
            startDestination = RutasDestino.Inicio.ruta,
            modifier = Modifier.padding(paddingInterior)
        ) {

            composable(RutasDestino.Inicio.ruta) {
                PantallaDashboard()
            }

            composable(RutasDestino.Turnos.ruta) {
                // TODO: P1-01 -> UI de Mis Turnos
            }

            composable(RutasDestino.Ausencias.ruta) {
                // TODO: P1-03 -> Solicitud de Ausencias
            }

            composable(RutasDestino.Perfil.ruta) {
                // TODO: P1-05 -> Perfil de Usuario
            }
        }
    }
}

