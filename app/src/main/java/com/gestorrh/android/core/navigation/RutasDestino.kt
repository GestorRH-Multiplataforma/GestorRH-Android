package com.gestorrh.android.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.gestorrh.android.R

/**
 * Define de forma estricta todos los destinos principales de la barra de navegación inferior.
 * @property ruta El identificador en formato String (URL) que usa NavHost internamente.
 * @property tituloResId El recurso de string que se mostrará en la barra.
 * @property icono El icono vectorial nativo de Material Design asociado a la pestaña.
 */
sealed class RutasDestino(
    val ruta: String,
    @StringRes val tituloResId: Int,
    val icono: ImageVector
) {
    data object Inicio : RutasDestino("inicio", R.string.nav_inicio, Icons.Filled.Dashboard)
    data object Turnos : RutasDestino("turnos", R.string.nav_turnos, Icons.Filled.CalendarMonth)
    data object Ausencias : RutasDestino("ausencias", R.string.nav_ausencias, Icons.Filled.EventBusy)
    data object Perfil : RutasDestino("perfil", R.string.nav_perfil, Icons.Filled.Person)
    data object GestionEquipo : RutasDestino("equipo", R.string.nav_equipo, Icons.Filled.Group)
}