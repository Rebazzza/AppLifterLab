package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.model.Exercise
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.ui.theme.ColorBench
import com.example.lifterlab.ui.theme.ColorDeadlift
import com.example.lifterlab.ui.theme.ColorSquat
import java.util.Locale
import kotlin.math.abs

/**
 * Tarjeta de ejercicio del Ejemplo.html: encabezado con el nombre/objetivo y acciones,
 * y debajo el registro (log) en forma de tabla "steel" con columnas Set / KG / Reps /
 * RPE / Done y el botón Add Set al pie.
 */
@Composable
fun ExerciseCard(
    exercise: RoutineExercise,
    reference: List<ExerciseSet> = emptyList(),
    /**
     * RF25 — Récords históricos por nombre de ejercicio. Solo el Modo Gimnasio los
     * recibe: con ellos, una serie completada que supere la marca enciende la fila
     * amarilla de Récord Personal.
     */
    records: Map<String, MarcaDeEjercicio> = emptyMap(),
    gymMode: Boolean = false,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onUpdateSet: (Int, Double?, Int?, Double?, Boolean?) -> Unit,
    onAddSet: () -> Unit,
    onDeleteExercise: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    Column(Modifier.padding(top = 24.dp)) {

        // Encabezado del ejercicio.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Agarre de arrastre: mantener presionado y mover reordena el ejercicio.
            Box(
                modifier = dragHandleModifier
                    .size(width = 30.dp, height = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Reordenar ${exercise.name}",
                    tint = if (isDragging) RoutineSkin.PrimaryText else RoutineSkin.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    text = exercise.name,
                    color = RoutineSkin.OnSurface,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp
                )
                Text(
                    text = exercise.targetMuscles.ifEmpty { "Objetivo: General" },
                    color = RoutineSkin.OnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Indicador cromático SBD (ui-design-system.md §3): subrayado del
                // movimiento básico cuando el ejercicio es Sentadilla / Banca / Peso Muerto.
                sbdAccent(exercise.name)?.let { accent ->
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(width = 30.dp, height = 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                }
            }

            if (canMoveUp) {
                RoundIconButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Subir ${exercise.name}",
                    onClick = onMoveUp
                )
            }
            if (canMoveUp && canMoveDown) Spacer(Modifier.size(6.dp))
            if (canMoveDown) {
                RoundIconButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Bajar ${exercise.name}",
                    onClick = onMoveDown
                )
            }
            if (canMoveUp || canMoveDown) Spacer(Modifier.size(6.dp))
            RoundIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = "Eliminar ejercicio",
                onClick = onDeleteExercise,
                tint = RoutineSkin.Error
            )
        }

        Spacer(Modifier.height(12.dp))

        // El log (tabla de series). Va de borde a borde: el encabezado y el pie
        // pinta su propio fondo, por eso no lleva padding interno.
        SteelCard(contentPadding = PaddingValues(0.dp)) {
            LogHeaderRow()

            // La primera serie sin completar se resalta como la serie activa.
            val activeIndex = exercise.sets.indexOfFirst { !it.isCompleted }

            // RF25 — Récord histórico del movimiento (si el ejercicio ya tiene historial).
            val marca = records[exercise.name]

            exercise.sets.forEachIndexed { setIndex, set ->
                SetRow(
                    setIndex = setIndex,
                    set = set,
                    reference = reference.getOrNull(setIndex),
                    gymMode = gymMode,
                    isActive = gymMode && setIndex == activeIndex,
                    // RF25: fila amarilla mientras la serie esté completada y supere el
                    // récord; al desmarcarla deja de ser récord y el color se apaga.
                    isRecord = gymMode && esRecordPersonal(set, marca),
                    alternate = setIndex % 2 == 0,
                    onUpdate = onUpdateSet
                )
                RowDivider()
            }

            AddSetFooter(onAddSet)
        }
    }
}

/** Fila de títulos del log: Set / KG / Reps / RPE / Done. */
@Composable
private fun LogHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoutineSkin.SurfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoLabel("Set", Modifier.weight(ColSet), RoutineSkin.OnSurfaceVariant, 10, TextAlign.Center)
        MonoLabel("KG", Modifier.weight(ColKg), RoutineSkin.OnSurfaceVariant, 10, TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        MonoLabel("Reps", Modifier.weight(ColReps), RoutineSkin.OnSurfaceVariant, 10, TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        MonoLabel("RPE", Modifier.weight(ColRpe), RoutineSkin.OnSurfaceVariant, 10, TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        MonoLabel("Done", Modifier.weight(ColDone), RoutineSkin.OnSurfaceVariant, 10, TextAlign.Center)
    }
    RowDivider()
}

private const val ColSet = 1.5f
private const val ColKg = 2.6f
private const val ColReps = 2.2f
private const val ColRpe = 2.2f
private const val ColDone = 1.7f

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RoutineSkin.OutlineVariant.copy(alpha = 0.5f))
    )
}

/**
 * Fila de una serie: número, peso, repeticiones, RPE y estado. Conserva toda la
 * lógica previa (actualización de campos, marcado de completado) y el resumen de la
 * sesión anterior.
 */
@Composable
fun SetRow(
    setIndex: Int,
    set: ExerciseSet,
    reference: ExerciseSet? = null,
    gymMode: Boolean = false,
    isActive: Boolean = false,
    /** RF25 — la serie completada supera el récord histórico: la fila se pinta amarilla. */
    isRecord: Boolean = false,
    alternate: Boolean = false,
    onUpdate: (Int, Double?, Int?, Double?, Boolean?) -> Unit
) {
    var kgText by remember { mutableStateOf(if (set.weightKg > 0) formatKg(set.weightKg) else "") }
    var repsText by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rpeText by remember { mutableStateOf(if (set.rpe > 0) formatKg(set.rpe) else "") }
    var completed by remember { mutableStateOf(set.isCompleted) }

    // La fila adopta lo que venga de fuera (sesión restaurada al abrir, sesión
    // descartada, reordenamiento): sin esto, cada fila conservaría en memoria lo que
    // vio al componerse y las series hechas parecerían borradas. Solo se reescribe el
    // texto cuando el valor del modelo difiere del escrito, para no pisar lo que el
    // atleta está tipeando en ese momento.
    LaunchedEffect(set.isCompleted) { completed = set.isCompleted }
    LaunchedEffect(set.weightKg) {
        val actual = kgText.toDoubleOrNull()
        if (actual != null && actual != set.weightKg) kgText = formatKg(set.weightKg)
    }
    LaunchedEffect(set.reps) {
        val actual = repsText.toIntOrNull()
        if (actual != null && actual != set.reps) repsText = set.reps.toString()
    }
    LaunchedEffect(set.rpe) {
        val actual = rpeText.toDoubleOrNull()
        if (actual != null && actual != set.rpe) rpeText = formatKg(set.rpe)
    }

    // RF25 — El récord manda sobre el rayado alterno y sobre la fila activa (una serie
    // récord siempre está completada, así que nunca es la "siguiente por hacer").
    val background = when {
        isRecord -> RoutineSkin.RecordYellowContainer
        isActive -> RoutineSkin.SurfaceContainerLow
        alternate -> RoutineSkin.RowAlt
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                when {
                    isRecord -> drawRect(
                        color = RoutineSkin.RecordYellow,
                        size = Size(4.dp.toPx(), size.height)
                    )
                    isActive -> drawRect(
                        color = RoutineSkin.Primary,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val ref = reference?.takeIf { it.weightKg > 0 || it.reps > 0 }
        if (ref != null || isRecord) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ref != null) {
                    Text(
                        text = "Última vez: ${formatKg(ref.weightKg)} kg × ${ref.reps}",
                        color = RoutineSkin.OnSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 2.dp)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (isRecord) PersonalRecordBadge()
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${set.setNumber}",
                color = if (isActive) RoutineSkin.PrimaryText else RoutineSkin.OnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(ColSet)
            )

            DarkNumberField(
                value = kgText,
                onValueChange = { v ->
                    kgText = v
                    onUpdate(setIndex, v.toDoubleOrNull() ?: 0.0, null, null, null)
                },
                hint = "0",
                weight = ColKg,
                textColor = if (set.isCompleted) RoutineSkin.PrimaryText else RoutineSkin.OnSurface
            )

            Spacer(Modifier.size(6.dp))

            DarkNumberField(
                value = repsText,
                onValueChange = { v ->
                    repsText = v
                    onUpdate(setIndex, null, v.toIntOrNull() ?: 0, null, null)
                },
                hint = "0",
                weight = ColReps,
                keyboardType = KeyboardType.Number,
                textColor = if (set.isCompleted) RoutineSkin.PrimaryText else RoutineSkin.OnSurface
            )

            Spacer(Modifier.size(6.dp))

            if (gymMode) {
                // Modo Gimnasio: el @ es un selector. Tocarlo despliega la lista completa
                // (@6 a @10 en pasos de 0.5) sin esperar a completar la serie.
                RpeSelectBox(
                    currentRpe = set.rpe,
                    onSelect = { nuevoRpe -> onUpdate(setIndex, null, null, nuevoRpe, null) },
                    weight = ColRpe
                )
            } else {
                DarkNumberField(
                    value = rpeText,
                    onValueChange = { v ->
                        rpeText = v
                        onUpdate(setIndex, null, null, v.toDoubleOrNull() ?: 0.0, null)
                    },
                    hint = "8.0",
                    weight = ColRpe,
                    textColor = RoutineSkin.Secondary
                )
            }

            Spacer(Modifier.size(6.dp))

            Box(
                modifier = Modifier.weight(ColDone),
                contentAlignment = Alignment.Center
            ) {
                CompleteSetButton(
                    completed = completed,
                    onClick = {
                        completed = !completed
                        onUpdate(setIndex, null, null, null, completed)
                    }
                )
            }
        }
    }
}

/**
 * RF25 — Distintivo "PR" (Personal Record): chip amarillo con estrella que aparece en la
 * fila de la serie cuando el peso o el tonelaje superan la marca histórica del movimiento.
 */
@Composable
private fun PersonalRecordBadge() {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(RoutineSkin.RecordYellow)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = RoutineSkin.OnRecordYellow,
            modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = "PR",
            color = RoutineSkin.OnRecordYellow,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/** Botón "Done" de la serie: check en caja redondeada, con glow cuando está completo. */
@Composable
private fun CompleteSetButton(completed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .then(
                if (completed) Modifier.routineGlow(shape, RoutineSkin.Tertiary, 8.dp)
                else Modifier
            )
            .clip(shape)
            .background(
                if (completed) RoutineSkin.Tertiary.copy(alpha = 0.2f)
                else RoutineSkin.SurfaceContainer
            )
            .border(
                BorderStroke(1.dp, if (completed) RoutineSkin.Tertiary else RoutineSkin.OutlineVariant),
                shape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (completed) "Completado" else "Pendiente",
            tint = if (completed) RoutineSkin.Tertiary else RoutineSkin.OnSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Pie "Add Set": botón ancho para añadir una serie. */
@Composable
private fun AddSetFooter(onAddSet: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoutineSkin.SteelSurface)
            .clickable { onAddSet() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = RoutineSkin.PrimaryText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        MonoLabel("Agregar Serie", color = RoutineSkin.PrimaryText, fontSize = 11)
    }
}

/** Escala de esfuerzo percibido basada en RIR: de @6.0 a @10.0 en pasos de 0.5. */
val rpeScale: List<Double> = (12..20).map { it / 2.0 }

private fun rpeLabel(rpe: Double): String = if (rpe > 0) "@${formatKg(rpe)}" else "—"

/**
 * Selector horizontal de RPE. Un toque fija el valor; tocar el valor ya elegido
 * lo limpia, ya que el registro del esfuerzo es opcional.
 */
@Composable
fun RpeSelector(
    currentRpe: Double,
    onSelect: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoLabel("RPE", fontSize = 10)

        rpeScale.forEach { option ->
            val seleccionado = abs(currentRpe - option) < 0.001
            val shape = RoundedCornerShape(50)
            Text(
                text = "@${option}",
                color = if (seleccionado) RoutineSkin.OnPrimary else RoutineSkin.OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (seleccionado) RoutineSkin.Primary else RoutineSkin.InputBackground
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (seleccionado) RoutineSkin.Primary else RoutineSkin.OutlineVariant
                        ),
                        shape
                    )
                    .clickable { onSelect(if (seleccionado) null else option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Selector de esfuerzo percibido del Modo Gimnasio: caja con el valor actual
 * ("—" si aún no se registra) que, al tocarla, despliega la lista de @6 a @10
 * en pasos de 0.5 (@6, @6.5, @7 … @10). Elegir "Sin registro" limpia el RPE.
 */
@Composable
fun RowScope.RpeSelectBox(
    currentRpe: Double,
    onSelect: (Double?) -> Unit,
    weight: Float
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val registrado = currentRpe > 0

    Box(Modifier.weight(weight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(RoutineSkin.InputBackground)
                .border(
                    BorderStroke(1.dp, if (expanded) RoutineSkin.Primary else RoutineSkin.SteelBorder),
                    shape
                )
                .clickable { expanded = true }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rpeLabel(currentRpe),
                color = if (registrado) RoutineSkin.Secondary else RoutineSkin.OnSurfaceVariant,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Elegir esfuerzo percibido",
                tint = RoutineSkin.OnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(RoutineSkin.SurfaceContainer)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Sin registro",
                        color = RoutineSkin.OnSurfaceVariant,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            rpeScale.forEach { option ->
                val activo = abs(currentRpe - option) < 0.001
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "@${formatKg(option)}",
                            color = if (activo) RoutineSkin.PrimaryText else RoutineSkin.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

private fun formatKg(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.ROOT, "%.1f", value)
    }
}

/** Color de acento del básico (SBD) según el nombre del ejercicio; null si no aplica. */
private fun sbdAccent(exerciseName: String): Color? {
    val name = exerciseName.lowercase(Locale.ROOT)
    return when {
        name.contains("sentadilla") || name.contains("squat") -> ColorSquat
        name.contains("banca") || name.contains("bench") -> ColorBench
        name.contains("muerto") || name.contains("deadlift") -> ColorDeadlift
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialog(
    customExercises: List<CustomExercise> = emptyList(),
    globalCatalog: List<Exercise> = emptyList(),
    currentExerciseName: String? = null,
    onSelect: (name: String, targetMuscles: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1B1D),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFF2A2A2C), CircleShape)
            )
        },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cambiar ejercicio",
                        color = Color(0xFFE6E1E6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecciona el nuevo movimiento para la serie",
                        color = Color(0xFFC9C4CA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF201F22))
                        .border(1.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFFC9C4CA),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Buscador
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF201F22))
                        .border(2.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color(0xFFC9C4CA),
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFFE6E1E6),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar ejercicio...",
                                    color = Color(0xFF52525B),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Lista de ejercicios
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val query = searchQuery.trim().lowercase(Locale.ROOT)

                val filteredCustom = customExercises.filter {
                    query.isEmpty() || it.name.lowercase(Locale.ROOT).contains(query) ||
                        it.primaryMuscle.lowercase(Locale.ROOT).contains(query)
                }

                val filteredBasics = basicExercises.filter { (name, target) ->
                    query.isEmpty() || name.lowercase(Locale.ROOT).contains(query) ||
                        target.lowercase(Locale.ROOT).contains(query)
                }

                // Catálogo global (Firestore): al final, en orden alfabético y sin
                // repetir los básicos fijos de la sección anterior.
                val basicNames = basicExercises.mapTo(HashSet()) { it.first.lowercase(Locale.ROOT) }
                val filteredGlobal = globalCatalog
                    .filter { exercise ->
                        exercise.name.lowercase(Locale.ROOT) !in basicNames &&
                            (query.isEmpty() ||
                                exercise.name.lowercase(Locale.ROOT).contains(query) ||
                                exercise.primaryMuscle.lowercase(Locale.ROOT).contains(query))
                    }
                    .sortedBy { it.name.lowercase(Locale.ROOT) }

                if (filteredCustom.isEmpty() && filteredBasics.isEmpty() && filteredGlobal.isEmpty()) {
                    Text(
                        text = "No se encontraron ejercicios con '$searchQuery'",
                        color = Color(0xFFC9C4CA),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    if (filteredCustom.isNotEmpty()) {
                        PickerSectionLabel("Tus ejercicios")
                        filteredCustom.forEach { exercise ->
                            val isCurrent = currentExerciseName != null &&
                                exercise.name.equals(currentExerciseName, ignoreCase = true)
                            ExerciseSheetRow(
                                name = exercise.name,
                                categoryTag = "Propio · ${exercise.primaryMuscle}",
                                isCurrent = isCurrent,
                                onClick = { onSelect(exercise.name, "Objetivo: ${exercise.primaryMuscle}") }
                            )
                        }
                    }

                    if (filteredBasics.isNotEmpty()) {
                        PickerSectionLabel("Básicos · SBD")
                        filteredBasics.forEach { (name, target) ->
                            val isCurrent = currentExerciseName != null &&
                                name.equals(currentExerciseName, ignoreCase = true)
                            val categoryTag = getCategoryTag(name, target)
                            ExerciseSheetRow(
                                name = name,
                                categoryTag = categoryTag,
                                isCurrent = isCurrent,
                                onClick = { onSelect(name, target) }
                            )
                        }
                    }

                    if (filteredGlobal.isNotEmpty()) {
                        PickerSectionLabel("Catálogo global")
                        filteredGlobal.forEach { exercise ->
                            val isCurrent = currentExerciseName != null &&
                                exercise.name.equals(currentExerciseName, ignoreCase = true)
                            val target = "Objetivo: ${exercise.primaryMuscle}"
                            ExerciseSheetRow(
                                name = exercise.name,
                                categoryTag = getCategoryTag(exercise.name, "$target ${exercise.equipment}"),
                                isCurrent = isCurrent,
                                onClick = { onSelect(exercise.name, target) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Footer informativo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF201F22))
                    .border(1.dp, Color(0xFF2A2A2C))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = Color(0xFFC1C1FB),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Las series se conservan; solo cambia el ejercicio. El peso, repeticiones y RPE actuales se mantendrán intactos.",
                    color = Color(0xFFC9C4CA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ExerciseSheetRow(
    name: String,
    categoryTag: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isCurrent) Color(0xFFC1C1FB) else Color(0xFF2A2A2C)
    val bgColor = if (isCurrent) Color(0x1AC1C1FB) else Color(0xFF201F22)
    val borderWidth = if (isCurrent) 2.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFC1C1FB), CircleShape)
                )
            }
            Text(
                text = name,
                color = if (isCurrent) Color(0xFFC1C1FB) else Color(0xFFE6E1E6),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFC1C1FB))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ACTUAL",
                    color = Color(0xFF131315),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = categoryTag,
                color = Color(0xFFC9C4CA),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/** Encabezado de sección dentro del selector (Monospace tenue). */
@Composable
private fun PickerSectionLabel(title: String) {
    Text(
        text = title,
        color = Color(0xFF52525B),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

private fun getCategoryTag(name: String, target: String): String {
    val lower = name.lowercase(Locale.ROOT)
    val targetLower = target.lowercase(Locale.ROOT)
    return when {
        lower.contains("sentadilla") || lower.contains("banca") || (lower.contains("peso muerto") && !lower.contains("rumano")) -> "IPF Comp"
        lower.contains("prensa") || lower.contains("curl") || targetLower.contains("máquina") -> "Máquina"
        lower.contains("dominadas") || lower.contains("fondos") || targetLower.contains("calistenia") -> "Calistenia"
        else -> "Accesorio"
    }
}

/**
 * HU-12 (RNF04 / RNF11) — Widget del Temporizador de Descanso ("Rest Timer")
 * Siguiendo el maquetado exacto de Ejemplo.html ("Titanio y Acero"):
 * - Encabezado con título REST TIMER e ícono de temporizador.
 * - Botones circulares de ajuste rápido -30 y +30 con display numérico MM:SS en acento primario.
 * - Botón "Skip Rest" / "Saltar Descanso" para omitir o cerrar el temporizador activo.
 */
@Composable
fun RestTimerWidget(
    remainingSeconds: Int,
    exerciseName: String = "",
    onAdjustTime: (secondsDelta: Int) -> Unit,
    onSkipRest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .routineGlow(shape, RoutineSkin.PrimaryText, 8.dp)
            .clip(shape)
            .background(Color(0xFF141417))
            .border(1.dp, Color(0xFF242427), shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cabecera del Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (exerciseName.isNotBlank()) "REST TIMER • $exerciseName" else "REST TIMER",
                color = RoutineSkin.OnSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Temporizador",
                tint = RoutineSkin.Tertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Display de tiempo y botones -30 / +30
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Botón -30s
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RoutineSkin.SurfaceContainer)
                    .border(1.dp, RoutineSkin.OutlineVariant, CircleShape)
                    .clickable { onAdjustTime(-30) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-30",
                    color = RoutineSkin.OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Display de tiempo MM:SS
            Text(
                text = formatRestTimerSeconds(remainingSeconds),
                color = RoutineSkin.PrimaryText,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )

            // Botón +30s
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RoutineSkin.SurfaceContainer)
                    .border(1.dp, RoutineSkin.OutlineVariant, CircleShape)
                    .clickable { onAdjustTime(+30) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+30",
                    color = RoutineSkin.OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botón Skip Rest
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RoutineSkin.SurfaceContainer)
                .border(1.dp, RoutineSkin.OutlineVariant, RoundedCornerShape(8.dp))
                .clickable { onSkipRest() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Skip Rest",
                color = RoutineSkin.OnSurface,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatRestTimerSeconds(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    val mins = total / 60
    val secs = total % 60
    return String.format(Locale.ROOT, "%02d:%02d", mins, secs)
}
