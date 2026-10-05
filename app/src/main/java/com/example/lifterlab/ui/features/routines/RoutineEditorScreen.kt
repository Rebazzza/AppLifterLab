package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CustomExerciseRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.SecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // RF20: los ejercicios propios del atleta se muestran junto al catálogo global al armar la rutina.
    var customExercises by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
            result.onSuccess { customExercises = it }
        }
    }

    val volume = calculateVolume(activeRoutine)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )

            ScreenTitle(if (routine.id.isBlank()) "Nueva Rutina" else "Editar Rutina", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Configura el nombre, ejercicios, series y repeticiones.",
                Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            RoutineNameField(
                value = activeRoutine.name,
                onValueChange = { newName ->
                    activeRoutine = activeRoutine.copy(name = newName)
                }
            )

            activeRoutine.exercises.forEachIndexed { exIndex, exercise ->
                ExerciseCard(
                    exercise = exercise,
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

            if (mensajeError.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ErrorText(mensajeError, Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
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
                enabled = !guardando
            )
        }
    }

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
}

@Composable
private fun RoutineNameField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Nombre de la Rutina...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Text(
                text = "✎ Editar nombre",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }
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