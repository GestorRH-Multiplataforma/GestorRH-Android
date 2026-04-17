package com.gestorrh.android.core.ui

import androidx.annotation.StringRes

/**
 * Abstracción que unifica los dos tipos de mensajes de error posibles en los ViewModels:
 *
 * - [Recurso]: errores estáticos conocidos en tiempo de compilación (ej. "Error de red",
 *   "Permiso denegado"). Se referencian por su ID de recurso de string para garantizar
 *   la correcta localización (i18n) y evitar strings hardcodeados en Kotlin.
 *
 * - [Dinamico]: mensajes procedentes del servidor (campo "message" del JSON de error).
 *   Son cadenas que el backend genera en tiempo de ejecución y que la aplicación no puede
 *   conocer de antemano (ej. "El empleado se encuentra fuera del radio de la sede").
 *
 * Las pantallas (Composables) resuelven esta abstracción con [android.content.Context.getString]
 * o [androidx.compose.ui.res.stringResource] según el tipo, evitando que los ViewModels
 * necesiten acceder a [android.content.Context].
 */
sealed class MensajeUi {

    /**
     * Error estático localizable mediante un ID de recurso string.
     *
     * @property idRecurso Identificador del string en `res/values/strings.xml`.
     */
    data class Recurso(@StringRes val idRecurso: Int) : MensajeUi()

    /**
     * Error dinámico cuyo texto proviene del servidor en tiempo de ejecución.
     *
     * @property texto Cadena de texto tal como la devuelve la API.
     */
    data class Dinamico(val texto: String) : MensajeUi()
}
