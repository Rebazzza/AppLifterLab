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
import com.example.lifterlab.data.model.ActiveSession
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.ActiveSessionRepository
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CustomExerciseRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.SecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RF04 — Modo Gimnasio: registro de series, peso y repeticiones.
 * Genera la sesión activa en el estado local del dispositivo, muestra cada serie con la
 * referencia de la sesión anterior y, al cerrar, consolida el tonelaje y guarda la
 * sesión en /users/{userId}/workout_sessions.
 */
@Composable
fun WorkoutRunnerScreen(
    routine: Routine,
    isExpress: Boolean,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() },
    context: android.content.Context = LocalContext.current
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val activeSessionRepository = remember(context) { ActiveSessionRepository(context) }
    val customExerciseRepository = remember { CustomExerciseRepository() }
    val scope = rememberCoroutineScope()

    var activeRoutine by remember { mutableStateOf(routine) }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Sesión activa en curso (RF04: estado local del dispositivo).
    var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var sesionReanudada by remember { mutableStateOf(false) }
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }

    // Referencia de la sesión anterior para las tarjetas de series.
    var referenciaPrevia by remember { mutableStateOf<Map<String, List<ExerciseSet>>>(emptyMap()) }

    // RF20: ejercicios propios del atleta, reutilizables durante el Modo Gimnasio.
    var customExercises by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }

    suspend fun persistirSesion() {
        if (userId.isEmpty()) return
        activeSessionRepository.saveActiveSession(
            ActiveSession(
                routineId = routine.id,
                routineName = activeRoutine.name,
                isFreeSession = isExpress,
                startedAtMillis = startedAt,
                exercises = activeRoutine.exercises
            )
        )
    }

    fun actualizarSesion(cambio: (Routine) -> Routine) {
        activeRoutine = cambio(activeRoutine)
        scope.launch { persistirSesion() }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val referencia = withContext(Dispatchers.IO) { warmupRepository.getWorkoutSessions(userId, 5) }
            referencia.onSuccess { sesiones -> referenciaPrevia = previousSessionReference(sesiones) }

            val propios = withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
            propios.onSuccess { customExercises = it }

            val guardada = withContext(Dispatchers.IO) { activeSessionRepository.getActiveSession() }
            guardada.onSuccess { saved ->
                if (saved != null && saved.matchesRoutine(routine, isExpress)) {
                    activeRoutine = activeRoutine.copy(exercises = saved.exercises)
                    startedAt = saved.startedAtMillis
                    sesionReanudada = true
                } else {
                    persistirSesion()
                }
            }
        }
    }

    LaunchedEffect(startedAt) {
        while (true) {
            delay(30000)
            ahora = System.currentTimeMillis()
        }
    }

    val volumenCompletado = calculateCompletedVolume(activeRoutine)
    val completedSets = activeRoutine.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val duracionMins = ((ahora - startedAt) / 60000L).toInt().coerceAtLeast(0)

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
                text = "$completedSets ${if (completedSets == 1) "serie completada" else "series completadas"} • ${formatElapsed(duracionMins)}",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (sesionReanudada) {
                LifterCard(Modifier.padding(bottom = 8.dp)) {
                    CardCaption("SESIÓN EN CURSO GUARDADA EN EL DISPOSITIVO")
                    Text(
                        text = "Retomaste el entrenamiento donde lo dejaste.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )
                    SecondaryButton(
                        text = "Descartar y Empezar de Cero",
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { activeSessionRepository.clearActiveSession() }
                                activeRoutine = routine
                                startedAt = System.currentTimeMillis()
                                sesionReanudada = false
                                persistirSesion()
                                context.toast("Sesión anterior descartada")
                            }
                        },
                        minHeight = 44.dp
                    )
                }
            }

            activeRoutine.exercises.forEachIndexed { exIndex, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    reference = referenciaPrevia[exercise.name].orEmpty(),
                    onUpdateSet = { setIndex, weightKg, reps, rpe, isCompleted ->
                        actualizarSesion { r -> updateSetData(r, exIndex, setIndex, weightKg, reps, rpe, isCompleted) }
                    },
                    onAddSet = {
                        actualizarSesion { r -> addSetToExercise(r, exIndex) }
                    },
                    onDeleteExercise = {
                        actualizarSesion { r -> removeExercise(r, exIndex) }
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
                    text = volumenCompletado,
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
            customExercises = customExercises,
            onSelect = { name, target ->
                actualizarSesion { r -> addExercise(r, name, target) }
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
                Column {
                    Text(
                        if (isExpress)
                            "Se guardarán los datos de la sesión (series y pesos) pero la rutina NO se guardará."
                        else
                            "Se guardarán los datos de la sesión. La rutina ya guardada no se modifica."
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "$completedSets series • $volumenCompletado • ${formatElapsed(duracionMins)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
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
                        durationMins = duracionMins.coerceAtLeast(1),
                        scope = scope,
                        context = context,
                        warmupRepository = warmupRepository,
                        activeSessionRepository = activeSessionRepository,
                        onError = { msg ->
                            guardando = false
                            mensajeError = msg
                        },
                        onSuccess = { volumen ->
                            guardando = false
                            context.toast("Entrenamiento registrado • $volumen")
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

private fun ActiveSession.matchesRoutine(routine: Routine, isExpress: Boolean): Boolean {
    if (isFreeSession != isExpress) return false
    return if (routineId.isNotEmpty()) routineId == routine.id else routineName == routine.name
}

private fun formatElapsed(mins: Int): String {
    return String.format(java.util.Locale.ROOT, "%02d:%02d", mins / 60, mins % 60)
}

private fun terminarSesion(
    userId: String,
    routine: Routine,
    isExpress: Boolean,
    durationMins: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    warmupRepository: WarmupRepository,
    activeSessionRepository: ActiveSessionRepository,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
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
    val session = buildWorkoutSession(routine, isExpress, durationMins)
    scope.launch {
        val result = withContext(Dispatchers.IO) { warmupRepository.saveWorkoutSession(userId, session) }
        result
            .onSuccess {
                withContext(Dispatchers.IO) { activeSessionRepository.clearActiveSession() }
                onSuccess(formatVolumeText(session.totalVolumeKg))
            }
            .onFailure { e -> onError(e.message ?: "No se pudo guardar la sesión.") }
    }
}

private fun formatVolumeText(volumeKg: Double): String {
    return if (volumeKg >= 1000) {
        String.format(java.util.Locale.ROOT, "%.1fk kg", volumeKg / 1000.0)
    } else {
        String.format(java.util.Locale.ROOT, "%.0f kg", volumeKg)
    }
}