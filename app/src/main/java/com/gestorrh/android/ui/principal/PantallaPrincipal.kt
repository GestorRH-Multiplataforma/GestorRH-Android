package com.gestorrh.android.ui.principal

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gestorrh.android.core.navigation.BarraNavegacionInferior
import com.gestorrh.android.core.navigation.RutasDestino
import com.gestorrh.android.ui.ausencia.PantallaMisAusencias
import com.gestorrh.android.ui.ausencia.PantallaSolicitudAusencia
import com.gestorrh.android.ui.ausencia.SolicitudAusenciaViewModel
import com.gestorrh.android.ui.dashboard.PantallaDashboard
import com.gestorrh.android.ui.perfil.PantallaPerfil
import com.gestorrh.android.ui.turnos.PantallaMisTurnos
import java.net.URLDecoder
import java.time.LocalDate

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
                PantallaMisTurnos()
            }

            composable(RutasDestino.Ausencias.ruta) {
                PantallaMisAusencias(
                    alSolicitarNueva = {
                        controladorNavegacionInterno.navigate(
                            RutasDestino.SolicitarAusencia.RUTA_CREAR
                        ) {
                            launchSingleTop = true
                        }
                    },
                    alEditarAusencia = { ausencia ->
                        controladorNavegacionInterno.navigate(
                            RutasDestino.SolicitarAusencia.rutaEditar(
                                id = ausencia.idAusencia,
                                tipo = ausencia.tipo,
                                fechaInicioIso = ausencia.fechaInicio.toString(),
                                fechaFinIso = ausencia.fechaFin.toString(),
                                descripcion = ausencia.descripcion
                            )
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = RutasDestino.SolicitarAusencia.ruta,
                arguments = listOf(
                    navArgument(RutasDestino.SolicitarAusencia.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(RutasDestino.SolicitarAusencia.ARG_TIPO) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(RutasDestino.SolicitarAusencia.ARG_INICIO) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(RutasDestino.SolicitarAusencia.ARG_FIN) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(RutasDestino.SolicitarAusencia.ARG_DESCRIPCION) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entrada ->
                val args = entrada.arguments
                val id = args?.getLong(RutasDestino.SolicitarAusencia.ARG_ID) ?: -1L
                val tipoArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_TIPO)
                val inicioArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_INICIO)
                val finArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_FIN)
                val descripcionArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_DESCRIPCION)

                val datosEdicion = if (
                    id > 0 && !tipoArg.isNullOrBlank() &&
                    !inicioArg.isNullOrBlank() && !finArg.isNullOrBlank()
                ) {
                    SolicitudAusenciaViewModel.PrerrellenoEdicion(
                        idAusencia = id,
                        tipo = tipoArg,
                        fechaInicio = LocalDate.parse(inicioArg),
                        fechaFin = LocalDate.parse(finArg),
                        descripcion = descripcionArg
                            ?.takeIf { it.isNotBlank() }
                            ?.let { URLDecoder.decode(it, "UTF-8") }
                    )
                } else {
                    null
                }

                PantallaSolicitudAusencia(
                    datosPrerelleno = datosEdicion,
                    alVolver = {
                        controladorNavegacionInterno.popBackStack(
                            route = RutasDestino.Ausencias.ruta,
                            inclusive = false
                        )
                    },
                    alEnvioExitoso = {
                        controladorNavegacionInterno.popBackStack(
                            route = RutasDestino.Ausencias.ruta,
                            inclusive = false
                        )
                    }
                )
            }

            composable(RutasDestino.Perfil.ruta) {
                PantallaPerfil(alCerrarSesion = alCerrarSesion)
            }
        }
    }
}
