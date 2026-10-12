package com.example.lifterlab.ui.features.warmup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.WarmupPhase
import com.example.lifterlab.data.model.WarmupStep
import com.example.lifterlab.data.model.buildWarmupPlan
import com.example.lifterlab.data.model.intensityTable
import com.example.lifterlab.data.model.plateBreakdown
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.features.routines.RpeSelector
import com.example.lifterlab.ui.formatKg

/**
 * Panel de la Calculadora de Aproximación.
 *
 * A partir de la carga top, las repeticiones y el RPE objetivo genera la tabla de
 * intensidades (75%–95%) y la progresión de series de calentamiento, con el desglose de
 * discos IPF de cada serie. Cuando se pasa [onApply], muestra además el selector de
 * ejercicio y el botón de transferencia (Modo Gimnasio y editor de rutinas); sin
 * [onApply] funciona como herramienta de consulta desde el catálogo.
 */
@Composable
fun WarmupCalculatorPanel(
    baseBarWeightKg: Double = 20.0,
    exerciseNames: List<String> = emptyList(),
    onApply: ((exerciseIndex: Int, steps: List<WarmupStep>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var topKgText by remember { mutableStateOf("") }
    var repsText by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf(8.0) }
    var topKg by remember { mutableStateOf(0.0) }
    var plan by remember { mutableStateOf<List<WarmupStep>>(emptyList()) }
    var intensities by remember { mutableStateOf<List<WarmupStep>>(emptyList()) }
    var mensajeError by remember { mutableStateOf("") }

    var selectedExercise by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculatorField(
                label = "CARGA TOP (KG)",
                value = topKgText,
                onValueChange = { topKgText = it },
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            CalculatorField(
                label = "REPS",
                value = repsText,
                onValueChange = { repsText = it },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }

        CardCaption("RPE OBJETIVO", Modifier.padding(top = 14.dp))
        RpeSelector(
            currentRpe = rpe,
            onSelect = { nuevo -> rpe = nuevo ?: 8.0 },
            modifier = Modifier.padding(top = 6.dp)
        )

        PrimaryButton(
            text = "Calcular Series",
            onClick = {
                val kg = topKgText.toDoubleOrNull() ?: 0.0
                val reps = repsText.toIntOrNull() ?: 0
                if (kg <= 0.0 || reps <= 0) {
                    mensajeError = "Ingresa una carga top y repeticiones válidas."
                    plan = emptyList()
                } else {
                    mensajeError = ""
                    topKg = kg
                    plan = buildWarmupPlan(kg, reps, rpe, baseBarWeightKg)
                    intensities = intensityTable(kg)
                }
            },
            modifier = Modifier.padding(top = 14.dp)
        )

        if (mensajeError.isNotEmpty()) {
            Text(
                text = mensajeError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (plan.isNotEmpty()) {
            PlanTable(plan, Modifier.padding(top = 16.dp))
            IntensityTable(intensities, topKg, Modifier.padding(top = 14.dp))

            if (onApply != null && exerciseNames.isNotEmpty()) {
                ExercisePicker(
                    exerciseNames = exerciseNames,
                    selectedIndex = selectedExercise,
                    menuOpen = menuOpen,
                    onMenuOpenChange = { menuOpen = it },
                    onSelect = { selectedExercise = it },
                    modifier = Modifier.padding(top = 16.dp)
                )

                PrimaryButton(
                    text = "Agregar al Ejercicio Seleccionado",
                    onClick = { onApply(selectedExercise, plan) },
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                Text(
                    text = "Las series se muestran como guía: ábrela desde el Modo Gimnasio o el editor de rutinas para volcarlas a un ejercicio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }
    }
}

/**
 * Modal de la Calculadora de Aproximación.
 * Envuelve [WarmupCalculatorPanel] en un diálogo para usarla como acción contextual.
 */
@Composable
fun WarmupCalculatorDialog(
    exerciseNames: List<String>,
    baseBarWeightKg: Double = 20.0,
    onApply: (exerciseIndex: Int, steps: List<WarmupStep>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Calculadora de Aproximación",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            WarmupCalculatorPanel(
                baseBarWeightKg = baseBarWeightKg,
                exerciseNames = exerciseNames,
                onApply = { exIndex, steps ->
                    onApply(exIndex, steps)
                    onDismiss()
                },
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun CalculatorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}

/** Tabla de la progresión generada, con los discos IPF de cada serie. */
@Composable
private fun PlanTable(steps: List<WarmupStep>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            TableHeader("SERIE", 1f, TextAlign.Start)
            TableHeader("FASE", 2.2f, TextAlign.Start)
            TableHeader("KG", 1.4f, TextAlign.End)
            TableHeader("REPS", 1.2f, TextAlign.End)
        }

        steps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${step.stepNumber}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = step.phase.label,
                    color = phaseColor(step.phase),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(2.2f)
                )
                Text(
                    text = formatKg(step.weightKg),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = "${step.reps}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Integración con el visualizador de discos IPF.
            PlateRow(
                weightKg = step.weightKg,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Tabla de intensidades de referencia: 75% al 95% de la carga top. */
@Composable
private fun IntensityTable(rows: List<WarmupStep>, topKg: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        CardCaption(
            "TABLA DE INTENSIDADES · TOP ${formatKg(topKg)} KG",
            mono = true
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            TableHeader("%", 1.4f, TextAlign.Start)
            TableHeader("KG", 1.4f, TextAlign.End)
            TableHeader("REPS MÁX.", 2f, TextAlign.End)
        }
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Text(
                    text = "${(row.pctOfTop * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = formatKg(row.weightKg),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = "${row.reps}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

/** Discos por lado de la barra con la codificación de colores IPF. */
@Composable
private fun PlateRow(weightKg: Double, modifier: Modifier = Modifier) {
    val breakdown = plateBreakdown(weightKg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (breakdown.isEmpty()) {
            Text(
                text = "SIN DISCOS · SOLO BARRA",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        breakdown.forEach { (plateKg, count) ->
            val color = ipfColorFor(plateKg)
            val textColor = if (plateKg == 2.5) Color(0xFFE5E1E4) else Color(0xFF131315)
            Text(
                text = "${count}×${formatKg(plateKg)}",
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .background(color = color, shape = RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Codificación oficial IPF de colores por disco. */
private fun ipfColorFor(plateKg: Double): Color = when (plateKg) {
    25.0 -> Color(0xFFE53935)
    20.0 -> Color(0xFF1E88E5)
    15.0 -> Color(0xFFFDD835)
    10.0 -> Color(0xFF43A047)
    5.0 -> Color(0xFFFFB77D)
    2.5 -> Color(0xFF212121)
    else -> Color(0xFFFAFAFA)
}

@Composable
private fun phaseColor(phase: WarmupPhase): Color = when (phase) {
    WarmupPhase.ACTIVACION -> MaterialTheme.colorScheme.tertiary
    WarmupPhase.APROXIMACION -> MaterialTheme.colorScheme.secondary
    WarmupPhase.TRABAJO -> MaterialTheme.colorScheme.primary
}

@Composable
private fun ExercisePicker(
    exerciseNames: List<String>,
    selectedIndex: Int,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CardCaption("AGREGAR AL EJERCICIO")
        Box(Modifier.padding(top = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onMenuOpenChange(true) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseNames.getOrElse(selectedIndex) { "Selecciona un ejercicio" },
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Elegir ejercicio",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                exerciseNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontFamily = if (index == selectedIndex) FontFamily.Monospace else FontFamily.Default
                            )
                        },
                        onClick = {
                            onSelect(index)
                            onMenuOpenChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableHeader(text: String, weight: Float, textAlign: TextAlign) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        textAlign = textAlign,
        modifier = Modifier.weight(weight)
    )
}
