package com.gestorrh.android.ui.equipo.fichajes

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.fichaje.RespuestaFichajeDTO
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Estado inmutable que consume [PantallaModificacionFichajes].
 *
 * @property empleados Catálogo de empleados del departamento cargado al inicializar.
 * @property cargandoEmpleados true mientras se obtiene el catálogo de empleados.
 * @property empleadoSeleccionado Empleado actualmente filtrado, null significa "Todos".
 * @property fechaInicio Inicio del rango de fechas del filtro, por defecto hoy.
 * @property fechaFin Fin del rango de fechas del filtro, por defecto hoy.
 * @property fichajes Lista de fichajes del rango y empleado seleccionados.
 * @property cargandoFichajes true mientras hay una petición de fichajes en curso.
 * @property fichajeEditando Fichaje sobre el que se ha pulsado "Editar", null si el dialog está cerrado.
 * @property dialogEntrada Nueva hora de entrada introducida en el dialog (pre-rellena con la original).
 * @property dialogSalida Nueva hora de salida introducida en el dialog (pre-rellena con la original).
 * @property dialogMotivo Texto del motivo de modificación introducido en el dialog.
 * @property dialogErrorHoras Mensaje de error de validación de horas, null si no hay error.
 * @property guardando true mientras se ejecuta el PUT de modificación.
 * @property mensajeError Mensaje a mostrar en Snackbar ante cualquier error.
 * @property mensajeExito Mensaje a mostrar en Snackbar tras modificación exitosa.
 */
data class EstadoUiModificacionFichajes(
    val empleados: List<RespuestaEmpleadoDTO> = emptyList(),
    val cargandoEmpleados: Boolean = false,
    val empleadoSeleccionado: RespuestaEmpleadoDTO? = null,
    val fechaInicio: LocalDate = LocalDate.now(),
    val fechaFin: LocalDate = LocalDate.now(),
    val fichajes: List<RespuestaFichajeDTO> = emptyList(),
    val cargandoFichajes: Boolean = false,
    val fichajeEditando: RespuestaFichajeDTO? = null,
    val dialogEntrada: LocalDateTime? = null,
    val dialogSalida: LocalDateTime? = null,
    val dialogMotivo: String = "",
    val dialogErrorHoras: Int? = null,
    val guardando: Boolean = false,
    val mensajeError: MensajeUi? = null,
    val mensajeExito: MensajeUi? = null
) {
    val dialogAbierto: Boolean get() = fichajeEditando != null

    val dialogGuardarHabilitado: Boolean
        get() = dialogMotivo.isNotBlank() && !guardando && dialogErrorHoras == null
}
