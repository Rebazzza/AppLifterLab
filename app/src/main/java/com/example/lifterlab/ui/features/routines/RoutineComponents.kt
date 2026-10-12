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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

            exercise.sets.forEachIndexed { setIndex, set ->
                SetRow(
                    setIndex = setIndex,
                    set = set,
                    reference = reference.getOrNull(setIndex),
                    gymMode = gymMode,
                    isActive = gymMode && setIndex == activeIndex,
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
    alternate: Boolean = false,
    onUpdate: (Int, Double?, Int?, Double?, Boolean?) -> Unit
) {
    var kgText by remember { mutableStateOf(if (set.weightKg > 0) formatKg(set.weightKg) else "") }
    var repsText by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rpeText by remember { mutableStateOf(if (set.rpe > 0) formatKg(set.rpe) else "") }
    var completed by remember { mutableStateOf(set.isCompleted) }

    val background = when {
        isActive -> RoutineSkin.SurfaceContainerLow
        alternate -> RoutineSkin.RowAlt
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                if (isActive) {
                    drawRect(
                        color = RoutineSkin.Primary,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (reference != null && (reference.weightKg > 0 || reference.reps > 0)) {
            Text(
                text = "Última vez: ${formatKg(reference.weightKg)} kg × ${reference.reps}",
                color = RoutineSkin.OnSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp)
            )
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
                DarkValueBox(
                    text = rpeLabel(set.rpe),
                    weight = ColRpe,
                    textColor = if (set.rpe > 0) RoutineSkin.Secondary else RoutineSkin.OnSurfaceVariant
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

        // Modo Gimnasio: el selector de esfuerzo se despliega solo cuando la
        // serie está completada, en una sola fila desplazable para no saturar la tarjeta.
        if (gymMode && completed) {
            RpeSelector(
                currentRpe = set.rpe,
                onSelect = { nuevoRpe -> onUpdate(setIndex, null, null, nuevoRpe ?: 0.0, null) },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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

@Composable
fun ExercisePickerDialog(
    customExercises: List<CustomExercise> = emptyList(),
    onSelect: (name: String, targetMuscles: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Ejercicio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (customExercises.isNotEmpty()) {
                    MonoLabel(
                        "Personalizados",
                        color = RoutineSkin.PrimaryText,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    customExercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(exercise.name, "Objetivo: ${exercise.primaryMuscle}")
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "${exercise.name} (Objetivo: ${exercise.primaryMuscle})",
                                    color = RoutineSkin.OnSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Medición: ${CustomExercise.labelOf(exercise.measurementType)}",
                                    color = RoutineSkin.OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            MonoLabel("Propio", color = RoutineSkin.Tertiary, fontSize = 10)
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(1.dp)
                            .background(RoutineSkin.OutlineVariant)
                    )
                }

                MonoLabel("Catálogo Global", color = RoutineSkin.PrimaryText)
                catalogExercises.forEach { (name, target) ->
                    Text(
                        text = "$name ($target)",
                        color = RoutineSkin.OnSurface,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(name, target) }
                            .padding(vertical = 10.dp, horizontal = 0.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
