package com.gestorrh.android.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    alCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.crearFactory(LocalContext.current))
) {
    val estado by viewModel.estadoUi.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val mensajeExito = estado.mensajeExito
    LaunchedEffect(mensajeExito) {
        if (mensajeExito != null) {
            val texto = when (mensajeExito) {
                is MensajeUi.Recurso -> context.getString(mensajeExito.idRecurso)
                is MensajeUi.Dinamico -> mensajeExito.texto
            }
            scope.launch { snackbarHostState.showSnackbar(texto) }
            viewModel.exitoMostrado()
        }
    }

    if (estado.mostrarDialogLogout) {
        AlertDialog(
            onDismissRequest = { viewModel.ocultarDialogLogout() },
            title = { Text(stringResource(R.string.perfil_dialog_logout_titulo)) },
            text = { Text(stringResource(R.string.perfil_dialog_logout_texto)) },
            confirmButton = {
                TextButton(onClick = { viewModel.cerrarSesion(alCerrarSesion) }) {
                    Text(stringResource(R.string.perfil_dialog_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.ocultarDialogLogout() }) {
                    Text(stringResource(R.string.perfil_dialog_cancelar))
                }
            }
        )
    }

    if (estado.mostrarDialogCambioPassword) {
        val mensajeErrorMinimo = stringResource(R.string.perfil_error_password_minimo)
        var mostrarPasswordActual by remember { mutableStateOf(false) }
        var mostrarNuevaPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.ocultarDialogCambioPassword() },
            title = { Text(stringResource(R.string.perfil_dialog_cambiar_password_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estado.passwordActual,
                        onValueChange = { viewModel.actualizarPasswordActual(it) },
                        label = { Text(stringResource(R.string.perfil_password_actual_hint)) },
                        visualTransformation = if (mostrarPasswordActual) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { mostrarPasswordActual = !mostrarPasswordActual }) {
                                Icon(
                                    imageVector = if (mostrarPasswordActual) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = estado.nuevaPassword,
                        onValueChange = { viewModel.actualizarNuevaPassword(it) },
                        label = { Text(stringResource(R.string.perfil_nueva_password_hint)) },
                        visualTransformation = if (mostrarNuevaPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { mostrarNuevaPassword = !mostrarNuevaPassword }) {
                                Icon(
                                    imageVector = if (mostrarNuevaPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = estado.errorDialogPassword != null,
                        singleLine = true
                    )
                    val errorPassword = estado.errorDialogPassword
                    if (errorPassword != null) {
                        Text(
                            text = errorPassword,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.cambiarPassword(mensajeErrorMinimo) },
                    enabled = !estado.estaCambiandoPassword
                ) {
                    if (estado.estaCambiandoPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.perfil_dialog_confirmar))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.ocultarDialogCambioPassword() }) {
                    Text(stringResource(R.string.perfil_dialog_cancelar))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.perfil_titulo)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingInterior ->

        when {
            estado.estaCargando -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingInterior),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            estado.mensajeError != null -> {
                val textoError = when (val error = estado.mensajeError) {
                    is MensajeUi.Recurso -> stringResource(error.idRecurso)
                    is MensajeUi.Dinamico -> error.texto
                    else -> ""
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingInterior),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = textoError, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { viewModel.cargarPerfil() }) {
                            Text(stringResource(R.string.perfil_reintentar))
                        }
                        OutlinedButton(onClick = { viewModel.cerrarSesion(alCerrarSesion) }) {
                            Text(stringResource(R.string.perfil_btn_cerrar_sesion))
                        }
                    }
                }
            }

            estado.perfil != null -> {
                val perfilActual = estado.perfil!!
                ContenidoPerfil(
                    perfil = perfilActual,
                    modifier = Modifier.padding(paddingInterior),
                    alCambiarPassword = { viewModel.mostrarDialogCambioPassword() },
                    alCerrarSesion = { viewModel.mostrarDialogLogout() }
                )
            }
        }
    }
}

@Composable
private fun ContenidoPerfil(
    perfil: RespuestaEmpleadoDTO,
    modifier: Modifier = Modifier,
    alCambiarPassword: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val inicial = perfil.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inicial,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${perfil.nombre} ${perfil.apellidos}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = perfil.rol,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        FilaInfoPerfil(
            etiqueta = stringResource(R.string.perfil_email),
            valor = perfil.email
        )
        FilaInfoPerfil(
            etiqueta = stringResource(R.string.perfil_telefono),
            valor = perfil.telefono ?: stringResource(R.string.perfil_sin_dato)
        )
        FilaInfoPerfil(
            etiqueta = stringResource(R.string.perfil_puesto),
            valor = perfil.puesto ?: stringResource(R.string.perfil_sin_dato)
        )
        FilaInfoPerfil(
            etiqueta = stringResource(R.string.perfil_departamento),
            valor = perfil.departamento ?: stringResource(R.string.perfil_sin_dato)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = alCambiarPassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.perfil_btn_cambiar_password))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = alCerrarSesion,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(stringResource(R.string.perfil_btn_cerrar_sesion))
        }
    }
}

@Composable
private fun FilaInfoPerfil(etiqueta: String, valor: String) {
    ListItem(
        headlineContent = { Text(valor) },
        overlineContent = { Text(etiqueta) }
    )
    HorizontalDivider()
}
