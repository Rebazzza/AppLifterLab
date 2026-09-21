package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.SecondaryButton
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val catalogExercises = listOf(
    Pair("Barbell Squat", "Target: Quads, Glutes"),
    Pair("Bench Press", "Target: Chest, Triceps"),
    Pair("Deadlift", "Target: Back, Hamstrings"),
    Pair("Romanian Deadlift", "Target: Hamstrings, Glutes"),
    Pair("Overhead Press", "Target: Shoulders, Triceps"),
    Pair("Barbell Row", "Target: Lats, Upper Back"),
    Pair("Incline Dumbbell Press", "Target: Upper Chest, Shoulders"),
    Pair("Pull Ups", "Target: Lats, Biceps"),
    Pair("Dips", "Target: Chest, Triceps"),
    Pair("Leg Press", "Target: Quads"),
    Pair("Leg Curl", "Target: Hamstrings"),
    Pair("Calf Raise", "Target: Calves")
)

@Composable
fun RoutinesScreen(
    onBack: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    routineRepository: RoutineRepository = remember { RoutineRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeRoutine by remember {
        mutableStateOf(
            Routine(
                name = "Heavy Lower Body - A",
                category = "HYPERTROPHY",
                tags = listOf("HYPERTROPHY", "LEGS"),
                estimatedTimeMins = 60,
                exercises = mutableListOf(
                    RoutineExercise(
                        name = "Barbell Squat",
                        targetMuscles = "Target: Quads, Glutes",
                        sets = mutableListOf(
                            ExerciseSet(setNumber = 1, weightKg = 140.0, reps = 5, rpe = 8.0, isCompleted = true),
                            ExerciseSet(setNumber = 2, weightKg = 140.0, reps = 5, rpe = 8.5)
                        )
                    )
                )
            )
        )
    }

    var savedTemplates by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var epoch by remember { mutableIntStateOf(0) }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    val volume = calculateVolume(activeRoutine)

    LaunchedEffect(userId) {
        val result = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
        if (result.isSuccess) {
            savedTemplates = result.getOrThrow()
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 50.dp, vertical = 16.dp)
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack()

                }
            )

            ScreenTitle("Gestión de Rutinas", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Diseña, edita y gestiona tus protocolos de entrenamiento.",
                Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LifterCard {
                RoutineNameField(
                    value = activeRoutine.name,
                    onValueChange = { newName ->
                        activeRoutine = activeRoutine.copy(name = newName)
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tags = activeRoutine.tags.ifEmpty { listOf("HYPERTROPHY", "GENERAL") }
                    for (tag in tags) {
                        Text(
                            text = tag.uppercase(Locale.ROOT),
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            activeRoutine.exercises.forEachIndexed { exIndex, exercise ->
                ExerciseCard(
                    exIndex = exIndex,
                    exercise = exercise,
                    epoch = epoch,
                    onUpdateSet = { setIndex, weightKg, reps, rpe, isCompleted ->
                        activeRoutine = updateSetData(activeRoutine, exIndex, setIndex, weightKg, reps, rpe, isCompleted)
                    },
                    onDeleteSetCheck = { alreadyCompleted ->
                        // toggle handled inline
                    },
                    onAddSet = {
                        activeRoutine = addSetToExercise(activeRoutine, exIndex)
                        epoch++
                    },
                    onDeleteExercise = {
                        activeRoutine = activeRoutine.copy(exercises = activeRoutine.exercises.toMutableList().apply {
                            removeAt(exIndex)
                        })
                        epoch++
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            SecondaryButton(
                text = "+ Agregar Ejercicio",
                onClick = { showAddExerciseDialog = true },
                minHeight = 48.dp
            )

            LifterCard(Modifier.padding(top = 16.dp)) {
                CardCaption("VOLUMEN ESTIMADO (EST. VOLUME)")
                Text(
                    text = volume,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            LifterCard(Modifier.padding(top = 16.dp)) {
                CardTitle("🔖 Plantillas Guardadas (Saved Templates)")
                if (savedTemplates.isEmpty()) {
                    Text(
                        text = "No tienes plantillas guardadas aún. Crea una arriba y presiona Guardar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    savedTemplates.forEach { routine ->
                        TemplateRow(
                            routine = routine,
                            onLoad = {
                                activeRoutine = routine.copy()
                                epoch++
                                context.toast("Plantilla '${routine.name}' cargada en el editor")
                            },
                            onDelete = { routineToDelete = routine }
                        )
                    }
                }
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ErrorText(mensajeError, Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "● Editando: ${activeRoutine.name.ifBlank { "Sin Nombre" }}",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        guardando = true
                        mensajeError = ""
                        postGuardarRutina(
                            userId = userId,
                            activeRoutine = activeRoutine,
                            guardando = guardando,
                            scope = scope,
                            context = context,
                            routineRepository = routineRepository,
                            onError = { msg ->
                                guardando = false
                                mensajeError = msg
                            },
                            onSuccess = { name ->
                                guardando = false
                                mensajeError = ""
                                context.toast("¡Rutina '$name' guardada exitosamente! ✓")
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
                                    if (result.isSuccess) savedTemplates = result.getOrThrow()
                                }
                            }
                        )
                    },
                    enabled = !guardando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = if (guardando) "Guardando..." else "Guardar Rutina", fontSize = 14.sp)
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
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
                                .clickable {
                                    activeRoutine = activeRoutine.copy(
                                        exercises = activeRoutine.exercises.toMutableList().apply {
                                            add(
                                                RoutineExercise(
                                                    name = name,
                                                    targetMuscles = target,
                                                    sets = listOf(
                                                        ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, rpe = 8.0),
                                                        ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 5, rpe = 8.5)
                                                    )
                                                )
                                            )
                                        }
                                    )
                                    epoch++
                                    showAddExerciseDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancelar") }
            }
        )
    }

    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Eliminar Plantilla") },
            text = { Text("¿Estás seguro de eliminar la plantilla '${routine.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            routineRepository.deleteRoutine(userId, routine.id)
                        }
                        result.onSuccess {
                            context.toast("Plantilla eliminada")
                            val list = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
                            if (list.isSuccess) savedTemplates = list.getOrThrow()
                        }.onFailure { e ->
                            mensajeError = e.message ?: "No se pudo eliminar la plantilla."
                        }
                    }
                    routineToDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RoutineNameField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Nombre de la Rutina...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ExerciseCard(
    exIndex: Int,
    exercise: RoutineExercise,
    epoch: Int,
    onUpdateSet: (Int, Double?, Int?, Double?, Boolean?) -> Unit,
    onDeleteSetCheck: (Boolean) -> Unit,
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

            Text(
                text = "🗑",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
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
                exIndex = exIndex,
                setIndex = setIndex,
                set = set,
                epoch = epoch,
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
        SecondaryButton(
            text = "✓ Completar todos",
            onClick = {

            },
            minHeight = 42.dp
        )
    }
}

@Composable
private fun RowScope.HeaderCol(text: String, weight: Float, textAlign: TextAlign) {
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
private fun SetRow(
    exIndex: Int,
    setIndex: Int,
    set: ExerciseSet,
    epoch: Int,
    onUpdate: (Int, Double?, Int?, Double?, Boolean?) -> Unit
) {
    var kgText by remember { mutableStateOf(if (set.weightKg > 0) set.weightKg.toString() else "") }
    var repsText by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rpeText by remember { mutableStateOf(if (set.rpe > 0) set.rpe.toString() else "") }
    var completed by remember { mutableStateOf(set.isCompleted) }
    androidx.compose.runtime.key(exIndex, setIndex, epoch) {
        kgText = if (set.weightKg > 0) set.weightKg.toString() else ""
        repsText = if (set.reps > 0) set.reps.toString() else ""
        rpeText = if (set.rpe > 0) set.rpe.toString() else ""
        completed = set.isCompleted
    }

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
            text = "${setIndex + 1}",
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
            Text(
                text = if (completed) "✓" else "○",
                color = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                modifier = Modifier.clickable {
                    completed = !completed
                    onUpdate(setIndex, null, null, null, completed)
                }
            )
        }
    }
}

@Composable
private fun RowScope.InlineNumberField(
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
private fun TemplateRow(routine: Routine, onLoad: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = routine.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${routine.exercises.size} Ejercicios • ${routine.estimatedTimeMins} mins",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "▶ Cargar",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { onLoad() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )

        Text(
            text = "🗑",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clickable { onDelete() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

private fun postGuardarRutina(
    userId: String,
    activeRoutine: Routine,
    guardando: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    routineRepository: RoutineRepository,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) {
    val name = activeRoutine.name.trim()
    if (name.isBlank()) {
        onError("Ingresa un nombre para la rutina.")
        return
    }
    if (activeRoutine.exercises.isEmpty()) {
        onError("Agrega al menos un ejercicio antes de guardar.")
        return
    }
    if (userId.isEmpty()) {
        onError("No hay una sesión iniciada.")
        return
    }
    val routineToSave = activeRoutine.copy(name = name)
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            routineRepository.saveRoutine(userId, routineToSave)
        }
        result
            .onSuccess { onSuccess(name) }
            .onFailure { e -> onError(e.message ?: "Error al guardar la rutina.") }
    }
}

private fun updateSetData(
    routine: Routine,
    exIndex: Int,
    setIndex: Int,
    weightKg: Double? = null,
    reps: Int? = null,
    rpe: Double? = null,
    isCompleted: Boolean? = null
): Routine {
    val exercises = routine.exercises.toMutableList()
    if (exIndex !in exercises.indices) return routine
    val targetEx = exercises[exIndex]
    val sets = targetEx.sets.toMutableList()
    if (setIndex !in sets.indices) return routine

    val currentSet = sets[setIndex]
    val updatedSet = currentSet.copy(
        weightKg = weightKg ?: currentSet.weightKg,
        reps = reps ?: currentSet.reps,
        rpe = rpe ?: currentSet.rpe,
        isCompleted = isCompleted ?: currentSet.isCompleted
    )
    sets[setIndex] = updatedSet
    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

private fun addSetToExercise(routine: Routine, exIndex: Int): Routine {
    val exercises = routine.exercises.toMutableList()
    if (exIndex !in exercises.indices) return routine

    val targetEx = exercises[exIndex]
    val sets = targetEx.sets.toMutableList()
    val lastSet = sets.lastOrNull()

    val newSet = ExerciseSet(
        setNumber = sets.size + 1,
        weightKg = lastSet?.weightKg ?: 100.0,
        reps = lastSet?.reps ?: 5,
        rpe = lastSet?.rpe ?: 8.0,
        isCompleted = false
    )
    sets.add(newSet)
    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

private fun calculateVolume(routine: Routine): String {
    var totalVolume = 0.0
    for (ex in routine.exercises) {
        for (set in ex.sets) {
            totalVolume += (set.weightKg * set.reps)
        }
    }
    return if (totalVolume >= 1000) {
        String.format(Locale.ROOT, "%.1fk kg", totalVolume / 1000.0)
    } else {
        String.format(Locale.ROOT, "%.0f kg", totalVolume)
    }
}