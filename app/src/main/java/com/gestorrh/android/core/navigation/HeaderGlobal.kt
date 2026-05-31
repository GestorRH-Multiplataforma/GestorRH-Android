package com.gestorrh.android.core.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.gestorrh.android.R

private val NavyPrimary = Color(0xFF1A365D)

/**
 * Header global de la aplicación, visible en todas las pantallas principales.
 *
 * Se sitúa en el [topBar] del Scaffold de [PantallaPrincipal], comportándose
 * de forma simétrica a la barra de navegación inferior: fijo, común a todas
 * las pestañas y sub-rutas, sin necesidad de que cada pantalla declare el suyo.
 *
 * Respeta [WindowInsets.statusBars] para que el fondo Navy cubra correctamente
 * la zona de la cámara y los iconos del sistema en cualquier dispositivo,
 * eliminando el corte visual entre la status bar y el contenido de la app.
 *
 * @param nombreEmpresa Nombre de la empresa leído desde [SessionManager], persistido
 *   en el login sin necesidad de petición de red adicional.
 */
@Composable
fun HeaderGlobal(nombreEmpresa: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = stringResource(id = R.string.login_logo_cd),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
                if (nombreEmpresa.isNotBlank()) {
                    Text(
                        text = nombreEmpresa,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
