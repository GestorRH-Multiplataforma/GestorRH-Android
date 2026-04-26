package com.gestorrh.android.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gestorrh.android.R
import kotlinx.coroutines.launch

private val CyanPrimario = Color(0xFF00A8E8)
private val NavyPrimario = Color(0xFF1A365D)

/**
 * Modelo de datos de cada página del onboarding.
 *
 * @property icono Icono outlined de Material Icons que ilustra la funcionalidad.
 * @property tituloRes Recurso de string del título de la página.
 * @property descripcionRes Recurso de string de la descripción de la página.
 * @property colorIcono Color de tinte del icono. Las tres primeras páginas usan
 *   Navy y la última usa Cyan para destacar el carácter diferencial del mensaje.
 */
private data class PaginaOnboarding(
    val icono: ImageVector,
    val tituloRes: Int,
    val descripcionRes: Int,
    val colorIcono: Color
)

private val PAGINAS = listOf(
    PaginaOnboarding(
        icono = Icons.Outlined.AccessTime,
        tituloRes = R.string.onboarding_pagina1_titulo,
        descripcionRes = R.string.onboarding_pagina1_descripcion,
        colorIcono = NavyPrimario
    ),
    PaginaOnboarding(
        icono = Icons.Outlined.CalendarMonth,
        tituloRes = R.string.onboarding_pagina2_titulo,
        descripcionRes = R.string.onboarding_pagina2_descripcion,
        colorIcono = NavyPrimario
    ),
    PaginaOnboarding(
        icono = Icons.Outlined.EventBusy,
        tituloRes = R.string.onboarding_pagina3_titulo,
        descripcionRes = R.string.onboarding_pagina3_descripcion,
        colorIcono = NavyPrimario
    ),
    PaginaOnboarding(
        icono = Icons.Outlined.MarkEmailRead,
        tituloRes = R.string.onboarding_pagina4_titulo,
        descripcionRes = R.string.onboarding_pagina4_descripcion,
        colorIcono = CyanPrimario
    )
)

/**
 * Pantalla de onboarding que se muestra una única vez en el primer arranque.
 *
 * Estructura:
 * - [HorizontalPager] con 4 páginas deslizables
 * - Indicadores de página (dots) animados en la parte inferior
 * - Botón "Saltar" en la esquina superior derecha (visible en todas menos la última)
 * - Botón principal que avanza páginas y en la última completa el onboarding
 *
 * @param alCompletarOnboarding Callback invocado al pulsar "Empezar" o "Saltar",
 *   que navega al login y persiste el flag de completado.
 */
@Composable
fun PantallaOnboarding(
    alCompletarOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGINAS.size })
    val scope = rememberCoroutineScope()
    val esUltimaPagina = pagerState.currentPage == PAGINAS.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (!esUltimaPagina) {
                TextButton(
                    onClick = alCompletarOnboarding,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_btn_saltar),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.weight(1f))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { indice ->
                    ContenidoPagina(pagina = PAGINAS[indice])
                }

                Spacer(modifier = Modifier.height(48.dp))

                DotsIndicador(
                    totalPaginas = PAGINAS.size,
                    paginaActual = pagerState.currentPage
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (esUltimaPagina) {
                            alCompletarOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esUltimaPagina) CyanPrimario
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(
                            id = if (esUltimaPagina) R.string.onboarding_btn_empezar
                            else R.string.onboarding_btn_siguiente
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

/**
 * Contenido de una página individual del onboarding.
 * Icono grande centrado, título en Navy y descripción en gris neutro.
 */
@Composable
private fun ContenidoPagina(pagina: PaginaOnboarding) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    color = pagina.colorIcono.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = pagina.icono,
                contentDescription = stringResource(id = R.string.onboarding_cd_ilustracion),
                modifier = Modifier.size(72.dp),
                tint = pagina.colorIcono
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(id = pagina.tituloRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = pagina.descripcionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

/**
 * Fila de dots indicadores de página.
 * El dot activo tiene el color Cyan corporativo y es más ancho (pill shape).
 * Los inactivos son pequeños círculos grises.
 */
@Composable
private fun DotsIndicador(
    totalPaginas: Int,
    paginaActual: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPaginas) { indice ->
            val esActivo = indice == paginaActual

            val ancho by animateColorAsState(
                targetValue = if (esActivo) CyanPrimario
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                animationSpec = tween(durationMillis = 300),
                label = "color_dot_$indice"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (esActivo) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(ancho)
            )
        }
    }
}