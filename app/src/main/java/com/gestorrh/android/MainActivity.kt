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
import com.gestorrh.android.ui.dashboard.DashboardScreen
import com.gestorrh.android.ui.login.LoginViewModel
import com.gestorrh.android.ui.login.PantallaLogin
import com.gestorrh.android.ui.theme.GestorRHTheme

/**
 * Actividad principal y punto de entrada (Entry Point) de la aplicación Android.
 * Actúa como el orquestador maestro (Router), encargado de inicializar las dependencias
 * críticas de nivel global (Motor de Red y Caja Fuerte) y de gestionar la navegación base.
 * * Implementa la directriz de "Auto-Login", evaluando la persistencia del Token JWT
 * de manera síncrona en el arranque para decidir la ruta inicial óptima.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gestorToken = TokenManager(this)
        val retrofit = ApiClient.crearRetrofit(gestorToken)
        val apiAutenticacion = retrofit.create(AuthApi::class.java)

        setContent {
            GestorRHTheme {

                val controladorNavegacion = rememberNavController()

                val destinoInicial = if (gestorToken.obtenerToken() != null) {
                    "dashboard"
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
                                return LoginViewModel(apiAutenticacion, gestorToken) as T
                            }
                        }

                        val loginViewModel: LoginViewModel = viewModel(factory = fabricaViewModel)

                        PantallaLogin(
                            viewModel = loginViewModel,
                            onLoginExitoso = {
                                // Redirección post-autenticación purgando el historial
                                // para evitar que el usuario retroceda al Login
                                controladorNavegacion.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen()
                    }
                }
            }
        }
    }
}