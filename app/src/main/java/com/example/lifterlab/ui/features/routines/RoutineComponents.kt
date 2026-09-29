package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.SecondaryButton

@Composable
fun ExerciseCard(
    exercise: RoutineExercise,
    onUpdateSet: (Int, Double?, Int?, Double?, Boolean?) -> Unit,
    onAddSet: () -> Unit,
    onDeleteExercise: () -> Unit
) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = exercise.targetMuscles.ifEmpty { "Target: General" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Eliminar ejercicio",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDeleteExercise() }
                    .padding(8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCol("SET", 1.5f, TextAlign.Start)
            HeaderCol("KG", 2.5f, TextAlign.Center)
            HeaderCol("REPS", 2.5f, TextAlign.Center)
            HeaderCol("RPE", 2f, TextAlign.Center)
            HeaderCol("STATUS", 2f, TextAlign.Center)
        }

        exercise.sets.forEachIndexed { setIndex, set ->
            SetRow(
                setIndex = setIndex,
                set = set,
                onUpdate = onUpdateSet
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(14.dp))
        SecondaryButton(
            text = "+ Add Set",
            onClick = onAddSet,
            minHeight = 42.dp
        )
    }
}

@Composable
fun RowScope.HeaderCol(text: String, weight: Float, textAlign: TextAlign) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign
    )
}

@Composable
fun SetRow(
    setIndex: Int,
    set: ExerciseSet,
    onUpdate: (Int, Double?, Int?, Double?, Boolean?) -> Unit
) {
    var kgText by remember { mutableStateOf(if (set.weightKg > 0) set.weightKg.toString() else "") }
    var repsText by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rpeText by remember { mutableStateOf(if (set.rpe > 0) set.rpe.toString() else "") }
    var completed by remember { mutableStateOf(set.isCompleted) }

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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${set.setNumber}",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1.5f)
        )

        InlineNumberField(
            value = kgText,
            onValueChange = { v ->
                kgText = v
                onUpdate(setIndex, v.toDoubleOrNull() ?: 0.0, null, null, null)
            },
            hint = "0",
            color = MaterialTheme.colorScheme.onBackground,
            weight = 2.5f
        )

        InlineNumberField(
            value = repsText,
            onValueChange = { v ->
                repsText = v
                onUpdate(setIndex, null, v.toIntOrNull() ?: 0, null, null)
            },
            hint = "0",
            color = MaterialTheme.colorScheme.onBackground,
            weight = 2.5f,
            keyboardType = KeyboardType.Number
        )

        InlineNumberField(
            value = rpeText,
            onValueChange = { v ->
                rpeText = v
                onUpdate(setIndex, null, null, v.toDoubleOrNull() ?: 0.0, null)
            },
            hint = "8.0",
            color = MaterialTheme.colorScheme.secondary,
            weight = 2f
        )

        Box(
            modifier = Modifier.weight(2f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (completed) "Completado" else "Pendiente",
                tint = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        completed = !completed
                        onUpdate(setIndex, null, null, null, completed)
                    }
            )
        }
    }
}

@Composable
fun RowScope.InlineNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    color: Color,
    weight: Float,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = color,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.weight(weight),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun ExercisePickerDialog(
    onSelect: (name: String, targetMuscles: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Ejercicio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                catalogExercises.forEach { (name, target) ->
                    Text(
                        text = "$name ($target)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(name, target) }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}