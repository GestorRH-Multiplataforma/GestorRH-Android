package com.gestorrh.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.TokenManager
import com.gestorrh.android.data.network.autenticacion.AuthApi
import com.gestorrh.android.data.repository.AuthRepository
import com.gestorrh.android.ui.login.LoginViewModel
import com.gestorrh.android.ui.login.PantallaLogin
import com.gestorrh.android.ui.principal.PantallaPrincipal
import com.gestorrh.android.ui.theme.GestorRHTheme

/**
 * Actividad principal y punto de entrada (Entry Point) de la aplicación Android.
 * Actúa como el orquestador maestro (Router), encargado de inicializar las dependencias
 * críticas de nivel global (Motor de Red, Caja Fuerte y Repositorios) y de gestionar
 * la navegación base.
 *
 * Implementa la directriz de "Auto-Login", evaluando la persistencia del Token JWT
 * de manera síncrona en el arranque para decidir la ruta inicial óptima.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gestorToken = TokenManager(this)
        val retrofit = ApiClient.crearRetrofit(gestorToken)
        val authRepository = AuthRepository(retrofit.create(AuthApi::class.java))

        setContent {
            GestorRHTheme {

                val controladorNavegacion = rememberNavController()

                val destinoInicial = if (gestorToken.obtenerToken() != null) {
                    "principal"
                } else {
                    "login"
                }

                NavHost(
                    navController = controladorNavegacion,
                    startDestination = destinoInicial
                ) {

                    composable("login") {
                        val fabricaViewModel = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return LoginViewModel(authRepository, gestorToken) as T
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
                            alCerrarSesion = {
                                // Lógica para cerrar sesión y volver al login (la haremos en la P1-05)
                                // gestorToken.borrarToken()
                                // controladorNavegacion.navigate("login") { popUpTo("principal") { inclusive = true } }
                            }
                        )
                    }
                }
            }
        }
    }
}
