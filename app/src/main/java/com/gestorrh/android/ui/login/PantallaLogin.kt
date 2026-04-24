package com.gestorrh.android.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.gestorrh.android.R
import com.gestorrh.android.core.ui.MensajeUi

/**
 * Interfaz gráfica para la autenticación de usuarios.
 * Sigue el patrón de diseño Unidirectional Data Flow (UDF), reaccionando a los
 * cambios emitidos por el [LoginViewModel] y elevando los eventos de usuario.
 *
 * El logotipo se carga desde el recurso PNG `ic_logo` (res/drawable),
 * usando [painterResource] para respetar el asset original sin reinterpretaciones
 * de color por parte del sistema.
 *
 * @param viewModel Manejador de la lógica de negocio y estado de la pantalla.
 * @param onLoginExitoso Callback ejecutado cuando el servidor valida las credenciales,
 * delegando la navegación al enrutador principal (NavHost).
 */
@Composable
fun PantallaLogin(
    viewModel: LoginViewModel,
    onLoginExitoso: () -> Unit
) {
    val estadoUi by viewModel.estadoUi.collectAsState()

    var mostrarDialogoRecuperacion by remember { mutableStateOf(false) }

    LaunchedEffect(estadoUi.loginExitoso) {
        if (estadoUi.loginExitoso) {
            onLoginExitoso()
        }
    }

    if (mostrarDialogoRecuperacion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoRecuperacion = false },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoRecuperacion = false }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.login_forgot_password))
            },
            text = {
                Text(text = stringResource(id = R.string.login_forgot_password_dialog_text))
            },
            shape = MaterialTheme.shapes.large
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Logotipo corporativo ──────────────────────────────────────────
            // Se usa Image + painterResource en lugar de Icon para que el sistema
            // NO aplique tinte (tint) sobre el PNG y los colores originales del
            // logo se muestren fielmente.
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = stringResource(id = R.string.login_logo_cd),
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.login_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(id = R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
            )

            estadoUi.mensajeError?.let { mensajeUi ->
                val contextoLocal = LocalContext.current
                val textoError = when (mensajeUi) {
                    is MensajeUi.Recurso -> stringResource(id = mensajeUi.idRecurso)
                    is MensajeUi.Dinamico -> mensajeUi.texto
                }
                Text(
                    text = textoError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = estadoUi.email,
                onValueChange = { viewModel.actualizarEmail(it) },
                label = { Text(stringResource(id = R.string.login_email_hint)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = estadoUi.password,
                onValueChange = { viewModel.actualizarPassword(it) },
                label = { Text(stringResource(id = R.string.login_password_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            Button(
                onClick = { viewModel.realizarLogin() },
                enabled = estadoUi.botonLoginHabilitado,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (estadoUi.estaCargando) {
                        stringResource(id = R.string.login_button_loading)
                    } else {
                        stringResource(id = R.string.login_button)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { mostrarDialogoRecuperacion = true }
            ) {
                Text(
                    text = stringResource(id = R.string.login_forgot_password),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
