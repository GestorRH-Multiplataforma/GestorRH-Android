package com.gestorrh.android.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
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

    /**
     * Destino interno para el formulario de solicitud de ausencia. No aparece en la
     * barra de navegación inferior: se alcanza desde el FAB de [Ausencias] (modo
     * creación) o desde el botón "Editar" de una tarjeta (modo edición, con los
     * argumentos opcionales `id`, `tipo`, `inicio`, `fin` y `descripcion` ya
     * rellenos en la URL).
     */
    /**
     * Destino interno para el historial de fichajes personales del empleado autenticado.
     * No aparece en la barra de navegación inferior: se alcanza desde la pestaña de
     * [Perfil] o desde el botón secundario de la pantalla de [Inicio] (fichaje).
     */
    data object HistorialFichajes : RutasDestino(
        ruta = "fichajes/historial",
        tituloResId = R.string.historial_fichajes_titulo,
        icono = Icons.Filled.History
    )

    data object SolicitarAusencia : RutasDestino(
        ruta = "ausencias/solicitar?id={id}&tipo={tipo}&inicio={inicio}&fin={fin}&descripcion={descripcion}&justificante={justificante}",
        tituloResId = R.string.nav_ausencias,
        icono = Icons.Filled.EventBusy
    ) {
        const val ARG_ID: String = "id"
        const val ARG_TIPO: String = "tipo"
        const val ARG_INICIO: String = "inicio"
        const val ARG_FIN: String = "fin"
        const val ARG_DESCRIPCION: String = "descripcion"
        const val ARG_JUSTIFICANTE: String = "justificante"

        /** URL de entrada sin argumentos: abre el formulario en modo creación. */
        const val RUTA_CREAR: String = "ausencias/solicitar"

        /**
         * Construye la URL de edición con los datos de la ausencia seleccionada.
         * La descripción y el nombre del justificante se URL-encodean porque pueden
         * contener espacios, acentos o signos reservados.
         */
        fun rutaEditar(
            id: Long,
            tipo: String,
            fechaInicioIso: String,
            fechaFinIso: String,
            descripcion: String?,
            justificante: String?
        ): String {
            val descCodificada = descripcion
                ?.takeIf { it.isNotBlank() }
                ?.let { java.net.URLEncoder.encode(it, "UTF-8") }
                .orEmpty()
            val justCodificado = justificante
                ?.takeIf { it.isNotBlank() }
                ?.let { java.net.URLEncoder.encode(it, "UTF-8") }
                .orEmpty()
            return "ausencias/solicitar" +
                "?id=$id" +
                "&tipo=$tipo" +
                "&inicio=$fechaInicioIso" +
                "&fin=$fechaFinIso" +
                "&descripcion=$descCodificada" +
                "&justificante=$justCodificado"
        }
    }
}