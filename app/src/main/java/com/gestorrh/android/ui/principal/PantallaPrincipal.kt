package com.gestorrh.android.ui.principal

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gestorrh.android.core.navigation.BarraNavegacionInferior
import com.gestorrh.android.core.navigation.HeaderGlobal
import com.gestorrh.android.core.navigation.RutasDestino
import com.gestorrh.android.ui.ausencia.PantallaMisAusencias
import com.gestorrh.android.ui.ausencia.PantallaSolicitudAusencia
import com.gestorrh.android.ui.ausencia.SolicitudAusenciaViewModel
import com.gestorrh.android.ui.dashboard.PantallaDashboard
import com.gestorrh.android.ui.equipo.PantallaGestionEquipo
import com.gestorrh.android.ui.historial.PantallaHistorialFichajes
import com.gestorrh.android.ui.perfil.PantallaPerfil
import com.gestorrh.android.ui.turnos.PantallaMisTurnos
import java.net.URLDecoder
import java.time.LocalDate

private const val DURACION_TRANSICION_MS = 300

/**
 * Contenedor maestro de la experiencia post-login.
 *
 * Implementa el patrón Scaffold con header global superior, barra de navegación
 * inferior y un NavHost con transiciones animadas entre destinos. Tanto el header
 * como la barra inferior son comunes a todas las pantallas y sub-rutas.
 *
 * @param isSupervisor Indica si el empleado autenticado tiene rol SUPERVISOR.
 * @param nombreEmpresa Nombre de la empresa leído desde [SessionManager], mostrado
 *   en el header global sin petición de red adicional.
 * @param alCerrarSesion Callback que limpia la sesión y navega al login.
 */
@Composable
fun PantallaPrincipal(
    isSupervisor: Boolean,
    nombreEmpresa: String,
    alCerrarSesion: () -> Unit
) {
    val controladorNavegacionInterno = rememberNavController()

    val pestanas = if (isSupervisor) {
        listOf(
            RutasDestino.Inicio,
            RutasDestino.Turnos,
            RutasDestino.Ausencias,
            RutasDestino.GestionEquipo,
            RutasDestino.Perfil
        )
    } else {
        listOf(
            RutasDestino.Inicio,
            RutasDestino.Turnos,
            RutasDestino.Ausencias,
            RutasDestino.Perfil
        )
    }

    Scaffold(
        topBar = {
            HeaderGlobal(nombreEmpresa = nombreEmpresa)
        },
        bottomBar = {
            BarraNavegacionInferior(
                controladorNavegacion = controladorNavegacionInterno,
                destinos = pestanas
            )
        }
    ) { paddingInterior ->

        NavHost(
            navController = controladorNavegacionInterno,
            startDestination = RutasDestino.Inicio.ruta,
            modifier = Modifier.padding(
                start = paddingInterior.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = paddingInterior.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = 40.dp,
                bottom = paddingInterior.calculateBottomPadding()
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(DURACION_TRANSICION_MS))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(DURACION_TRANSICION_MS))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(DURACION_TRANSICION_MS))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(DURACION_TRANSICION_MS))
            }
        ) {

            composable(RutasDestino.Inicio.ruta) {
                PantallaDashboard(
                    alVerHistorialCompleto = {
                        controladorNavegacionInterno.navigate(
                            RutasDestino.HistorialFichajes.ruta
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
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
                                tipo = ausencia.tipo.name,
                                fechaInicioIso = ausencia.fechaInicio.toString(),
                                fechaFinIso = ausencia.fechaFin.toString(),
                                descripcion = ausencia.descripcion,
                                justificante = ausencia.justificante
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
                    },
                    navArgument(RutasDestino.SolicitarAusencia.ARG_JUSTIFICANTE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(DURACION_TRANSICION_MS)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(DURACION_TRANSICION_MS)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(DURACION_TRANSICION_MS))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(DURACION_TRANSICION_MS)
                    )
                }
            ) { entrada ->
                val args = entrada.arguments
                val id = args?.getLong(RutasDestino.SolicitarAusencia.ARG_ID) ?: -1L
                val tipoArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_TIPO)
                val inicioArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_INICIO)
                val finArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_FIN)
                val descripcionArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_DESCRIPCION)
                val justificanteArg = args?.getString(RutasDestino.SolicitarAusencia.ARG_JUSTIFICANTE)

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
                            ?.let { URLDecoder.decode(it, "UTF-8") },
                        justificante = justificanteArg
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
                PantallaPerfil(
                    alCerrarSesion = alCerrarSesion,
                    alVerHistorialFichajes = {
                        controladorNavegacionInterno.navigate(
                            RutasDestino.HistorialFichajes.ruta
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(RutasDestino.HistorialFichajes.ruta) {
                PantallaHistorialFichajes(
                    alVolver = { controladorNavegacionInterno.popBackStack() }
                )
            }

            if (isSupervisor) {
                composable(RutasDestino.GestionEquipo.ruta) {
                    PantallaGestionEquipo()
                }
            }
        }
    }
}