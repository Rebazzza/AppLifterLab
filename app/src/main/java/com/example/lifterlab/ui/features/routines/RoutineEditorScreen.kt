package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CustomExerciseRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.warmup.WarmupCalculatorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Crear / Editar una rutina. La interfaz sigue la estética de Ejemplo.html
 * ("steel cards", campos input-dark y botón de acento), conservando toda la
 * funcionalidad previa (nombre, series, RPE, reordenamiento, calculadora, etc.).
 */
@Composable
fun RoutineEditorScreen(
    routine: Routine,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    routineRepository: RoutineRepository = remember { RoutineRepository() },
    customExerciseRepository: CustomExerciseRepository = remember { CustomExerciseRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeRoutine by remember { mutableStateOf(routine) }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    // Calculadora de Aproximación disponible al armar la plantilla.
    var showWarmupCalculator by remember { mutableStateOf(false) }
    var baseBarWeightKg by remember { mutableStateOf(20.0) }
    val profileRepository = remember { ProfileRepository() }

    // los ejercicios propios del atleta se muestran junto al catálogo global al armar la rutina.
    var customExercises by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
            result.onSuccess { customExercises = it }

            val perfil = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            perfil.onSuccess { profile ->
                if (profile.baseBarWeightKg > 0) baseBarWeightKg = profile.baseBarWeightKg
            }
        }
    }

    val volume = calculateVolume(activeRoutine)

    // Reordenamiento por arrastre (drag-and-drop) y alternativa con flechas.
    val dragState = rememberExerciseDragState { fromIndex, toIndex ->
        activeRoutine = moveExercise(activeRoutine, fromIndex, toIndex)
    }
    LaunchedEffect(activeRoutine.exercises.size) {
        dragState.syncSize(activeRoutine.exercises.size)
    }

    // Lista con scroll: sirve para posicionar la región visible (auto-scroll al arrastrar).
    val scrollState = rememberScrollState()

    Surface(color = RoutineSkin.Background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            RoutineTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .onGloballyPositioned { coords ->
                        val top = coords.positionInWindow()
                        dragState.setViewport(top.y, coords.size.height.toFloat())
                    }
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .padding(bottom = bottomBarClearance())
            ) {
                Text(
                    text = if (routine.id.isBlank()) "Nueva Rutina" else "Editar Rutina",
                    color = RoutineSkin.OnSurface,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Configura el nombre, ejercicios, series y repeticiones.",
                    color = RoutineSkin.OnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                RoutineNameField(
                    value = activeRoutine.name,
                    onValueChange = { newName ->
                        activeRoutine = activeRoutine.copy(name = newName)
                    },
                    modifier = Modifier.padding(top = 18.dp)
                )

                activeRoutine.exercises.forEachIndexed { exIndex, exercise ->
                    ReorderableExerciseItem(dragState, exIndex) {
                        ExerciseCard(
                            exercise = exercise,
                            isDragging = dragState.isDragging(exIndex),
                            dragHandleModifier = Modifier.dragHandleFor(dragState, exIndex),
                            canMoveUp = exIndex > 0,
                            canMoveDown = exIndex < activeRoutine.exercises.lastIndex,
                            onMoveUp = { activeRoutine = moveExercise(activeRoutine, exIndex, exIndex - 1) },
                            onMoveDown = { activeRoutine = moveExercise(activeRoutine, exIndex, exIndex + 1) },
                            onUpdateSet = { setIndex, weightKg, reps, rpe, isCompleted ->
                                activeRoutine = updateSetData(activeRoutine, exIndex, setIndex, weightKg, reps, rpe, isCompleted)
                            },
                            onAddSet = {
                                activeRoutine = addSetToExercise(activeRoutine, exIndex)
                            },
                            onDeleteExercise = {
                                activeRoutine = removeExercise(activeRoutine, exIndex)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SteelOutlineButton(
                    text = "Agregar ejercicio",
                    onClick = { showAddExerciseDialog = true },
                    leadingIcon = Icons.Filled.Add
                )

                if (activeRoutine.exercises.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SteelOutlineButton(
                        text = "Calculadora de aproximación",
                        contentColor = RoutineSkin.OnSurfaceVariant,
                        onClick = { showWarmupCalculator = true },
                        leadingIcon = Icons.Filled.Calculate
                    )
                }

                SteelCard(Modifier.padding(top = 20.dp)) {
                    MonoLabel("Volumen estimado", color = RoutineSkin.PrimaryText)
                    Text(
                        text = volume,
                        color = RoutineSkin.PrimaryContainer,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (mensajeError.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = mensajeError,
                        color = RoutineSkin.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(28.dp))
                AccentWideButton(
                    text = if (guardando) "Guardando..." else "Guardar Rutina",
                    onClick = {
                        guardando = true
                        mensajeError = ""
                        postGuardarRutina(
                            userId = userId,
                            routine = activeRoutine,
                            scope = scope,
                            context = context,
                            routineRepository = routineRepository,
                            onError = { msg ->
                                guardando = false
                                mensajeError = msg
                            },
                            onSuccess = { name ->
                                guardando = false
                                context.toast("¡Rutina '$name' guardada!")
                                onSaved()
                            }
                        )
                    },
                    enabled = !guardando,
                    trailingIcon = Icons.Filled.Save
                )
            }
        }
    }

    // Mientras se arrastra, la lista sigue sola cuando la tarjeta toca los bordes.
    AutoScrollWhileDragging(dragState, scrollState)

    if (showAddExerciseDialog) {
        ExercisePickerDialog(
            customExercises = customExercises,
            onSelect = { name, target ->
                activeRoutine = addExercise(activeRoutine, name, target)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    if (showWarmupCalculator) {
        WarmupCalculatorDialog(
            exerciseNames = activeRoutine.exercises.map { it.name },
            baseBarWeightKg = baseBarWeightKg,
            onApply = { exIndex, steps ->
                activeRoutine = applyWarmupPlan(activeRoutine, exIndex, steps)
                context.toast("Series de aproximación agregadas a la rutina")
            },
            onDismiss = { showWarmupCalculator = false }
        )
    }
}

/** Campo de nombre de la rutina con el estilo "input-dark" del Ejemplo.html. */
@Composable
private fun RoutineNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SteelCard(modifier) {
        MonoLabel("Nombre de la rutina", color = RoutineSkin.PrimaryText)

        val fieldShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(RoutineSkin.InputBackground, fieldShape)
                .border(BorderStroke(1.dp, RoutineSkin.SteelBorder), fieldShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = RoutineSkin.OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Nombre de la Rutina...",
                                color = RoutineSkin.OnSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Text(
            text = "✎ Editar nombre",
            color = RoutineSkin.PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

private fun postGuardarRutina(
    userId: String,
    routine: Routine,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    routineRepository: RoutineRepository,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) {
    val name = routine.name.trim()
    if (name.isBlank()) {
        onError("Ingresa un nombre para la rutina.")
        return
    }
    if (routine.exercises.isEmpty()) {
        onError("Agrega al menos un ejercicio antes de guardar.")
        return
    }
    if (userId.isEmpty()) {
        onError("No hay una sesión iniciada.")
        return
    }
    val routineToSave = routine.copy(name = name)
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            routineRepository.saveRoutine(userId, routineToSave)
        }
        result
            .onSuccess { onSuccess(name) }
            .onFailure { e -> onError(e.message ?: "Error al guardar la rutina.") }
    }
}
