package com.gestorrh.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gestorrh.android.ui.login.LoginScreen
import com.gestorrh.android.ui.theme.GestorRHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestorRHTheme {
                val loginViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.gestorrh.android.ui.login.LoginViewModel>()

                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        println("¡Navegando al Dashboard!")
                    }
                )
            }
        }
    }
}