package com.gestorrh.android.ui.equipo.cuadrante

import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.network.asignacion.ModalidadAsignacion
import com.gestorrh.android.data.network.asignacion.RespuestaAsignacionTurnoDTO
import com.gestorrh.android.data.network.empleado.RespuestaEmpleadoDTO
import com.gestorrh.android.data.network.turno.RespuestaTurnoDTO
import java.time.LocalDate

/**
 * Estado inmutable que consume [PantallaCuadranteDepartamento].
 *
 * @property cargando true mientras se obtienen las asignaciones del equipo al inicializar.
 * @property asignaciones Lista completa de asignaciones del departamento sin filtrar.
 * @property asignacionesFiltradas Subconjunto de [asignaciones] para [fechaSeleccionada],
 *           calculado reactivamente en el ViewModel sin nueva llamada de red.
 * @property fechaSeleccionada Fecha actualmente visible en el cuadrante. Por defecto hoy.
 * @property empleados Catálogo de empleados del departamento, cargado al abrir el BottomSheet.
 * @property turnos Catálogo de plantillas de turno, cargado al abrir el BottomSheet.
 * @property modalidades Valores disponibles del enum de modalidad.
 * @property mostrarBottomSheet Controla la visibilidad del BottomSheet de asignación.
 * @property cargandoCatalogos true mientras se cargan empleados y turnos.
 * @property estaAsignando true mientras hay una petición POST o PUT en curso.
 * @property idAsignacionEditando Id de la asignación en edición, null en modo creación.
 * @property empleadoSeleccionado Empleado elegido en el dropdown del BottomSheet.
 * @property turnoSeleccionado Turno elegido en el dropdown del BottomSheet.
 * @property fechaAsignacion Fecha elegida en el DatePicker del BottomSheet.
 * @property modalidadSeleccionada Modalidad elegida en el dropdown del BottomSheet.
 * @property motivoCambio Texto del campo motivo, obligatorio en modo edición.
 * @property asignacionAEliminar Asignación pendiente de confirmar eliminación, null si no hay.
 * @property eliminando true mientras hay una petición DELETE en curso.
 * @property mensajeError Mensaje a mostrar en Snackbar. Null si no hay error pendiente.
 * @property mensajeExito Mensaje a mostrar en Snackbar tras operación exitosa.
 */
data class EstadoUiCuadranteDepartamento(
    val cargando: Boolean = true,
    val asignaciones: List<RespuestaAsignacionTurnoDTO> = emptyList(),
    val asignacionesFiltradas: List<RespuestaAsignacionTurnoDTO> = emptyList(),
    val fechaSeleccionada: LocalDate = LocalDate.now(),
    val empleados: List<RespuestaEmpleadoDTO> = emptyList(),
    val turnos: List<RespuestaTurnoDTO> = emptyList(),
    val modalidades: List<ModalidadAsignacion> = ModalidadAsignacion.entries,
    val mostrarBottomSheet: Boolean = false,
    val cargandoCatalogos: Boolean = false,
    val estaAsignando: Boolean = false,
    val idAsignacionEditando: Long? = null,
    val empleadoSeleccionado: RespuestaEmpleadoDTO? = null,
    val turnoSeleccionado: RespuestaTurnoDTO? = null,
    val fechaAsignacion: LocalDate = LocalDate.now(),
    val modalidadSeleccionada: ModalidadAsignacion? = null,
    val motivoCambio: String = "",
    val asignacionAEliminar: RespuestaAsignacionTurnoDTO? = null,
    val eliminando: Boolean = false,
    val mensajeError: MensajeUi? = null,
    val mensajeExito: MensajeUi? = null,
    val idSupervisor: Long = -1L,
) {
    val modoEdicion: Boolean
        get() = idAsignacionEditando != null

    val formularioValido: Boolean
        get() = empleadoSeleccionado != null &&
                turnoSeleccionado != null &&
                !fechaAsignacion.isBefore(LocalDate.now()) &&
                modalidadSeleccionada != null &&
                (!modoEdicion || motivoCambio.isNotBlank())
}
