package com.gestorrh.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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
import com.gestorrh.android.ui.login.LoginScreen
import com.gestorrh.android.ui.login.LoginViewModel
import com.gestorrh.android.ui.theme.GestorRHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Instanciamos la caja fuerte y la red
        val tokenManager = TokenManager(this)
        val retrofit = ApiClient.createRetrofit(tokenManager)
        val authApi = retrofit.create(AuthApi::class.java)

        setContent {
            GestorRHTheme {
                // 2. Creamos el controlador de navegación
                val navController = rememberNavController()

                // 3. LÓGICA DE AUTO-LOGIN: ¿Dónde empezamos?
                val startDestination = if (tokenManager.getToken() != null) {
                    "dashboard" // Si hay token, directo dentro
                } else {
                    "login" // Si no hay token, a pedir credenciales
                }

                // 4. EL MAPA DE CARRETERAS (NavHost)
                NavHost(navController = navController, startDestination = startDestination) {

                    // Ruta A: Pantalla de Login
                    composable("login") {
                        val factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return LoginViewModel(authApi, tokenManager) as T
                            }
                        }
                        val loginViewModel: LoginViewModel = viewModel(factory = factory)

                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                // Navegamos al dashboard y DESTRUIMOS el login del historial
                                // para que si el usuario le da a "Atrás", salga de la app en vez de volver al login
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Ruta B: Pantalla del Dashboard
                    composable("dashboard") {
                        DashboardScreen()
                    }
                }
            }
        }
    }
}