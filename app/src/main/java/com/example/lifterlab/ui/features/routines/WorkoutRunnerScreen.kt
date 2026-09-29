package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.SecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WorkoutRunnerScreen(
    routine: Routine,
    isExpress: Boolean,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeRoutine by remember { mutableStateOf(routine) }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    val volume = calculateVolume(activeRoutine)
    val completedSets = activeRoutine.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

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

            Text(
                text = if (isExpress) "Rutina Express" else activeRoutine.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = if (isExpress)
                    "Modo libre: agrega ejercicios y al terminar solo se guardan los datos de la sesión."
                else
                    "Registra cada serie. Los cambios no modifican la rutina guardada.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            Text(
                text = "$completedSets ${if (completedSets == 1) "serie completada" else "series completadas"}",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
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
                CardCaption("VOLUMEN COMPLETADO")
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
                text = if (guardando) "Guardando..." else "Terminar Entrenamiento",
                onClick = { showFinishDialog = true },
                enabled = !guardando
            )
        }
    }

    if (showAddExerciseDialog) {
        ExercisePickerDialog(
            onSelect = { name, target ->
                activeRoutine = addExercise(activeRoutine, name, target)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Terminar Entrenamiento") },
            text = {
                Text(
                    if (isExpress)
                        "Se guardarán los datos de la sesión (series y pesos) pero la rutina NO se guardará."
                    else
                        "Se guardarán los datos de la sesión. La rutina ya guardada no se modifica."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    guardando = true
                    mensajeError = ""
                    terminarSesion(
                        userId = userId,
                        routine = activeRoutine,
                        isExpress = isExpress,
                        scope = scope,
                        context = context,
                        warmupRepository = warmupRepository,
                        onError = { msg ->
                            guardando = false
                            mensajeError = msg
                        },
                        onSuccess = {
                            guardando = false
                            context.toast(if (isExpress) "Sesión express guardada" else "Entrenamiento registrado")
                            onFinished()
                        }
                    )
                }) { Text("Guardar y Terminar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun terminarSesion(
    userId: String,
    routine: Routine,
    isExpress: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    warmupRepository: WarmupRepository,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    val completed = routine.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    if (completed == 0) {
        onError("Marca al menos una serie como completada antes de terminar.")
        return
    }
    if (userId.isEmpty()) {
        onError("No hay una sesión iniciada.")
        return
    }
    val session = buildWorkoutSession(routine, isExpress, 45)
    scope.launch {
        val result = withContext(Dispatchers.IO) { warmupRepository.saveWorkoutSession(userId, session) }
        result
            .onSuccess { onSuccess() }
            .onFailure { e -> onError(e.message ?: "No se pudo guardar la sesión.") }
    }
}