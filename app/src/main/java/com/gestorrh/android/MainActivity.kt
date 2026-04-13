package com.gestorrh.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.core.network.ApiClient
import com.gestorrh.android.core.security.TokenManager
import com.gestorrh.android.data.network.autenticacion.AuthApi
import com.gestorrh.android.ui.login.LoginScreen
import com.gestorrh.android.ui.login.LoginViewModel
import com.gestorrh.android.ui.theme.GestorRHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Instanciamos el TokenManager con el Contexto de la Actividad
        val tokenManager = TokenManager(this)

        // 2. Creamos Retrofit y le pedimos que genere nuestra AuthApi
        val retrofit = ApiClient.createRetrofit(tokenManager)
        val authApi = retrofit.create(AuthApi::class.java)

        setContent {
            GestorRHTheme {
                // 3. Usamos un Factory para inyectar las dependencias al ViewModel
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
                        println("¡Token guardado! Navegando al Dashboard...")
                    }
                )
            }
        }
    }
}