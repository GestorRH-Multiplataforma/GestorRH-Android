package com.gestorrh.android.core.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.gestorrh.android.R

private val NavyPrimary = Color(0xFF1A365D)

/**
 * Header global de la aplicación, visible en todas las pantallas principales.
 *
 * Muestra el nombre de la empresa a la izquierda en tamaño prominente y el logo
 * de la app a la derecha, sobre fondo Navy corporativo continuo con la status bar.
 *
 * @param nombreEmpresa Nombre de la empresa leído desde [SessionManager].
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
            Text(
                text = if (nombreEmpresa.isNotBlank()) nombreEmpresa else stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = stringResource(id = R.string.login_logo_cd),
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}
