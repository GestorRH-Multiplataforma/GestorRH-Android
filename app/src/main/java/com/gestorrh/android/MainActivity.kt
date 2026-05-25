package com.gestorrh.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.AuthEventBus
import com.gestorrh.android.core.security.SessionManager
import com.gestorrh.android.data.local.GestorRhDatabase
import com.gestorrh.android.data.network.autenticacion.AuthApi
import com.gestorrh.android.data.repository.AuthRepository
import com.gestorrh.android.ui.login.LoginViewModel
import com.gestorrh.android.ui.login.PantallaLogin
import com.gestorrh.android.ui.principal.PantallaPrincipal
import com.gestorrh.android.ui.theme.GestorRHTheme
import kotlinx.coroutines.launch
import com.gestorrh.android.core.onboarding.OnboardingManager
import com.gestorrh.android.ui.onboarding.PantallaOnboarding

/**
 * Actividad principal y punto de entrada (Entry Point) de la aplicación Android.
 * Actúa como el orquestador maestro (Router), encargado de inicializar las dependencias
 * críticas de nivel global (Motor de Red, Sesión y Repositorios) y de gestionar
 * la navegación base.
 *
 * Implementa la directriz de "Auto-Login", evaluando la persistencia del Token JWT
 * de manera síncrona en el arranque para decidir la ruta inicial óptima.
 * Lee también el rol persistido para determinar si el destino principal debe
 * mostrar la navegación de EMPLEADO o de SUPERVISOR, sin necesidad de red.
 * Observa [AuthEventBus] para redirigir al Login ante cualquier 401 global.
 *
 * SplashScreen: llama a [installSplashScreen] ANTES de [setContent] para que el
 * sistema gestione la transición animada desde el tema Theme.GestorRH.Splash
 * al tema principal Theme.GestorRH.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        val onboardingManager = OnboardingManager(this)
        val retrofit = ApiClient.crearRetrofit(sessionManager)
        val authRepository = AuthRepository(retrofit.create(AuthApi::class.java))
        val baseDatos = GestorRhDatabase.getInstance(this)

        setContent {
            GestorRHTheme {

                val controladorNavegacion = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val mensajeSesionExpirada = stringResource(R.string.sesion_expirada)

                LaunchedEffect(Unit) {
                    AuthEventBus.sesionExpirada.collect {
                        scope.launch {
                            baseDatos.asignacionDao().deleteAll()
                        }
                        controladorNavegacion.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(mensajeSesionExpirada)
                        }
                    }
                }

                val destinoInicial = when {
                    !onboardingManager.onboardingCompletado() -> "onboarding"
                    sessionManager.getToken() != null -> "principal"
                    else -> "login"
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = controladorNavegacion,
                        startDestination = destinoInicial
                    ) {

                        composable("onboarding") {
                            PantallaOnboarding(
                                alCompletarOnboarding = {
                                    onboardingManager.marcarOnboardingCompletado()
                                    controladorNavegacion.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            val fabricaViewModel = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return LoginViewModel(authRepository, sessionManager) as T
                                }
                            }

                            val loginViewModel: LoginViewModel = viewModel(factory = fabricaViewModel)

                            PantallaLogin(
                                viewModel = loginViewModel,
                                onLoginExitoso = {
                                    controladorNavegacion.navigate("principal") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("principal") {
                            PantallaPrincipal(
                                isSupervisor = sessionManager.isSupervisor(),
                                alCerrarSesion = {
                                    sessionManager.clearSession()
                                    scope.launch {
                                        baseDatos.asignacionDao().deleteAll()
                                    }
                                    controladorNavegacion.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
