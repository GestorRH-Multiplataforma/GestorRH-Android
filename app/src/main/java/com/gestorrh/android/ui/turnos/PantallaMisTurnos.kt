package com.gestorrh.android.ui.turnos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestorrh.android.R
import com.gestorrh.android.core.ui.MensajeUi
import com.gestorrh.android.data.local.entity.AsignacionEntity
import com.gestorrh.android.data.network.asignacion.ModalidadAsignacion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ColorPresencial = Color(0xFF1A365D)
private val ColorTeletrabajo = Color(0xFF00A8E8)

private fun AsignacionEntity.fechaLocalDate(): LocalDate = LocalDate.parse(fecha)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisTurnos(
    contexto: android.content.Context = LocalContext.current,
    viewModel: MisTurnosViewModel = viewModel(
        factory = MisTurnosViewModel.factory(contexto.applicationContext)
    )
) {
    val estadoUi by viewModel.estadoUi.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    val textoReintentar = stringResource(id = R.string.turnos_reintentar)
    val textoSinConexion = stringResource(id = R.string.turnos_sin_conexion)

    LaunchedEffect(estadoUi.mensajeError) {
        estadoUi.mensajeError?.let { mensajeUi ->
            val textoMensaje = when (mensajeUi) {
                is MensajeUi.Recurso -> recursos.getString(mensajeUi.idRecurso)
                is MensajeUi.Dinamico -> mensajeUi.texto
            }
            val resultado = snackbarHostState.showSnackbar(
                message = textoMensaje,
                actionLabel = textoReintentar,
                duration = SnackbarDuration.Long
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                viewModel.cargarAsignaciones()
            }
            viewModel.errorMostrado()
        }
    }

    LaunchedEffect(estadoUi.sinConexion) {
        if (estadoUi.sinConexion) {
            snackbarHostState.showSnackbar(
                message = textoSinConexion,
                duration = SnackbarDuration.Short
            )
            viewModel.avisoSinConexionMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.turnos_titulo),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.cambiarVista(VistaActual.CALENDARIO) }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(id = R.string.turnos_cd_vista_calendario),
                            tint = if (estadoUi.vistaActual == VistaActual.CALENDARIO)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.cambiarVista(VistaActual.LISTA) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(id = R.string.turnos_cd_vista_lista),
                            tint = if (estadoUi.vistaActual == VistaActual.LISTA)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValores ->

        when {
            estadoUi.cargando && estadoUi.asignaciones.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            estadoUi.asignaciones.isEmpty() -> {
                EstadoVacio(modifier = Modifier.padding(paddingValores))
            }

            estadoUi.vistaActual == VistaActual.LISTA -> {
                VistaListaTurnos(
                    asignaciones = estadoUi.asignaciones,
                    modifier = Modifier.padding(paddingValores)
                )
            }

            else -> {
                VistaCalendario(
                    asignaciones = estadoUi.asignaciones,
                    diaSeleccionado = estadoUi.diaSeleccionado,
                    alSeleccionarDia = viewModel::seleccionarDia,
                    modifier = Modifier.padding(paddingValores)
                )
            }
        }
    }
}

private fun agruparPorSemana(asignaciones: List<AsignacionEntity>): Map<LocalDate, List<AsignacionEntity>> {
    return asignaciones.groupBy { asignacion ->
        val fecha = asignacion.fechaLocalDate()
        fecha.with(DayOfWeek.MONDAY)
    }.toSortedMap()
}

@Composable
private fun VistaListaTurnos(
    asignaciones: List<AsignacionEntity>,
    modifier: Modifier = Modifier
) {
    val hoy = remember { LocalDate.now() }
    val grupos = remember(asignaciones) { agruparPorSemana(asignaciones) }
    val listState = rememberLazyListState()

    LaunchedEffect(grupos) {
        var indice = 0
        var contador = 0
        for ((inicioSemana, turnosSemana) in grupos) {
            val finSemana = inicioSemana.plusDays(6)
            if (!finSemana.isBefore(hoy)) {
                indice = contador
                break
            }
            contador += 1 + turnosSemana.size
        }
        if (indice > 0) listState.scrollToItem(indice)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grupos.forEach { (inicioSemana, turnosSemana) ->
            item(key = "semana_${inicioSemana}") {
                SeparadorSemana(inicioSemana = inicioSemana)
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(turnosSemana, key = { it.idAsignacion }) { asignacion ->
                val fecha = asignacion.fechaLocalDate()
                TarjetaAsignacion(
                    asignacion = asignacion,
                    esHoy = fecha == hoy,
                    esPasado = fecha.isBefore(hoy)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SeparadorSemana(inicioSemana: LocalDate) {
    val formatoCorto = remember { DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()) }
    val finSemana = inicioSemana.plusDays(6)
    val etiqueta = "${inicioSemana.format(formatoCorto)} — ${finSemana.format(formatoCorto)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun TarjetaAsignacion(
    asignacion: AsignacionEntity,
    esHoy: Boolean = false,
    esPasado: Boolean = false
) {
    val patronFecha = stringResource(id = R.string.turnos_formato_fecha_tarjeta)
    val fechaFormateada = remember(asignacion.fecha, patronFecha) {
        val locale = Locale.getDefault()
        asignacion.fechaLocalDate().format(DateTimeFormatter.ofPattern(patronFecha, locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (esPasado) 0.5f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (esHoy)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (esHoy) BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary
        ) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fechaFormateada,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (esHoy)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = asignacion.descripcionTurno,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esHoy)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (asignacion.horaInicio != null && asignacion.horaFin != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${asignacion.horaInicio} - ${asignacion.horaFin}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (esHoy)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            ChipModalidad(modalidad = asignacion.modalidad)
        }
    }
}

@Composable
private fun ChipModalidad(modalidad: String) {
    val (colorFondo, textoRes) = when (modalidad) {
        ModalidadAsignacion.PRESENCIAL.name -> ColorPresencial to R.string.turnos_modalidad_presencial
        ModalidadAsignacion.TELETRABAJO.name -> ColorTeletrabajo to R.string.turnos_modalidad_teletrabajo
        else -> ColorPresencial to R.string.turnos_modalidad_presencial
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = colorFondo
    ) {
        Text(
            text = stringResource(id = textoRes),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VistaCalendario(
    asignaciones: List<AsignacionEntity>,
    diaSeleccionado: LocalDate,
    alSeleccionarDia: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var mesActual by remember { mutableStateOf(YearMonth.now()) }

    val diasConTurno = remember(asignaciones, mesActual) {
        asignaciones
            .map { it to it.fechaLocalDate() }
            .filter { YearMonth.from(it.second) == mesActual }
            .associate { it.second to it.first }
    }

    val asignacionSeleccionada = remember(diaSeleccionado, asignaciones) {
        asignaciones.find { it.fechaLocalDate() == diaSeleccionado }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CabeceraMes(
            mes = mesActual,
            alRetroceder = { mesActual = mesActual.minusMonths(1) },
            alAvanzar = { mesActual = mesActual.plusMonths(1) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilaDiasSemana()

        Spacer(modifier = Modifier.height(4.dp))

        CuadriculaDias(
            mes = mesActual,
            diasConTurno = diasConTurno.keys,
            diaSeleccionado = diaSeleccionado,
            alClickDia = alSeleccionarDia
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(modifier = Modifier.height(16.dp))

        if (asignacionSeleccionada != null) {
            DetalleAsignacion(asignacion = asignacionSeleccionada)
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.turnos_sin_turno_dia),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CabeceraMes(
    mes: YearMonth,
    alRetroceder: () -> Unit,
    alAvanzar: () -> Unit
) {
    val locale = Locale.getDefault()
    val nombreMes = remember(mes, locale) {
        val texto = mes.month.getDisplayName(TextStyle.FULL, locale)
        texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = alRetroceder) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(id = R.string.turnos_cd_mes_anterior)
            )
        }

        Text(
            text = "$nombreMes ${mes.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = alAvanzar) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(id = R.string.turnos_cd_mes_siguiente)
            )
        }
    }
}

@Composable
private fun FilaDiasSemana() {
    val locale = Locale.getDefault()
    val diasOrdenados = remember(locale) {
        DayOfWeek.entries.map { dia ->
            dia.getDisplayName(TextStyle.NARROW, locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        diasOrdenados.forEach { dia ->
            Text(
                text = dia,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CuadriculaDias(
    mes: YearMonth,
    diasConTurno: Set<LocalDate>,
    diaSeleccionado: LocalDate,
    alClickDia: (LocalDate) -> Unit
) {
    val primerDia = mes.atDay(1)
    val offsetInicio = (primerDia.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val diasEnMes = mes.lengthOfMonth()
    val totalCeldas = offsetInicio + diasEnMes
    val filas = (totalCeldas + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (fila in 0 until filas) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val indice = fila * 7 + col
                    val numeroDia = indice - offsetInicio + 1

                    if (numeroDia < 1 || numeroDia > diasEnMes) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val fecha = mes.atDay(numeroDia)
                        val tieneTurno = fecha in diasConTurno
                        val esHoy = fecha == LocalDate.now()

                        val esSeleccionado = fecha == diaSeleccionado

                        CeldaDia(
                            numero = numeroDia,
                            esHoy = esHoy,
                            esSeleccionado = esSeleccionado,
                            tieneTurno = tieneTurno,
                            alClick = { alClickDia(fecha) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CeldaDia(
    numero: Int,
    esHoy: Boolean,
    esSeleccionado: Boolean,
    tieneTurno: Boolean,
    alClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorFondo = when {
        esHoy && esSeleccionado -> MaterialTheme.colorScheme.primaryContainer
        esSeleccionado -> MaterialTheme.colorScheme.secondaryContainer
        esHoy -> MaterialTheme.colorScheme.surfaceVariant
        tieneTurno -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val opacidad = if (tieneTurno || esHoy || esSeleccionado) 1f else 0.4f

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(colorFondo)
            .alpha(opacidad)
            .clickable(onClick = alClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = numero.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                esHoy -> MaterialTheme.colorScheme.onPrimaryContainer
                esSeleccionado -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (esHoy || tieneTurno || esSeleccionado) FontWeight.Bold else FontWeight.Normal
        )
        if (tieneTurno) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun DetalleAsignacion(
    asignacion: AsignacionEntity,
    modifier: Modifier = Modifier
) {
    val patronFecha = stringResource(id = R.string.turnos_formato_fecha_detalle)
    val locale = Locale.getDefault()
    val fechaFormateada = remember(asignacion.fecha, patronFecha) {
        asignacion.fechaLocalDate().format(DateTimeFormatter.ofPattern(patronFecha, locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.turnos_detalle_titulo),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        FilaDetalle(
            etiqueta = stringResource(id = R.string.turnos_detalle_fecha),
            valor = fechaFormateada
        )
        FilaDetalle(
            etiqueta = stringResource(id = R.string.turnos_detalle_turno),
            valor = asignacion.descripcionTurno
        )
        if (asignacion.horaInicio != null && asignacion.horaFin != null) {
            FilaDetalle(
                etiqueta = stringResource(id = R.string.turnos_detalle_horario),
                valor = "${asignacion.horaInicio} - ${asignacion.horaFin}"
            )
        }
        FilaDetalle(
            etiqueta = stringResource(id = R.string.turnos_detalle_modalidad),
            valor = stringResource(
                id = when (asignacion.modalidad) {
                    ModalidadAsignacion.PRESENCIAL.name -> R.string.turnos_modalidad_presencial
                    ModalidadAsignacion.TELETRABAJO.name -> R.string.turnos_modalidad_teletrabajo
                    else -> R.string.turnos_modalidad_presencial
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FilaDetalle(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.turnos_vacio_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.turnos_vacio_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
