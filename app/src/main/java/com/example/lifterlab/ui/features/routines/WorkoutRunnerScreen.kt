package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.ActiveSession
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.model.Exercise
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.ActiveSessionRepository
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CustomExerciseRepository
import com.example.lifterlab.data.repository.ExerciseRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.warmup.WarmupCalculatorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modo Gimnasio: registro de series, peso y repeticiones.
 * Esfuerzo percibido: al completar una serie se despliega el selector de RPE
 * (@6.0 a @10.0) y el valor viaja como campo numérico en cada serie.
 * Reordenamiento: los ejercicios se pueden reordenar por arrastre o con las
 * flechas Subir/Bajar; el nuevo orden se guarda en la sesión activa e historial.
 * Genera la sesión activa en el estado local del dispositivo, muestra cada serie con la
 * referencia de la sesión anterior y, al cerrar, consolida el tonelaje y guarda la
 * sesión en /users/{userId}/workout_sessions.
 *
 * La interfaz sigue la estética de Ejemplo.html ("Live Training").
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
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var activeRoutine by remember { mutableStateOf(routine) }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // True en cuanto el atleta registra algo (serie, reorder, ejercicio). Evita que la
    // carga de la sesión guardada (asíncrona) sobrescriba esos cambios y "devuelva" los
    // ejercicios al lugar de antes.
    var usuarioInteractuo by remember { mutableStateOf(false) }

    // Calculadora de Aproximación: peso de la barra base desde el perfil.
    var showWarmupCalculator by remember { mutableStateOf(false) }
    var baseBarWeightKg by remember { mutableStateOf(20.0) }

    // Sesión activa en curso: estado local del dispositivo.
    var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var sesionReanudada by remember { mutableStateOf(false) }
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }

    // Referencia de la sesión anterior para las tarjetas de series.
    var referenciaPrevia by remember { mutableStateOf<Map<String, List<ExerciseSet>>>(emptyMap()) }

    // ejercicios propios del atleta, reutilizables durante el Modo Gimnasio.
    var customExercises by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }

    // Catálogo global de /exercises para el selector (sección "Catálogo global").
    var globalCatalog by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    val exerciseRepository = remember { ExerciseRepository() }

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
        usuarioInteractuo = true
        activeRoutine = cambio(activeRoutine)
        scope.launch { persistirSesion() }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val referencia = withContext(Dispatchers.IO) { warmupRepository.getWorkoutSessions(userId, 5) }
            referencia.onSuccess { sesiones -> referenciaPrevia = previousSessionReference(sesiones) }

            val propios = withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
            propios.onSuccess { customExercises = it }

            val perfil = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            perfil.onSuccess { profile ->
                if (profile.baseBarWeightKg > 0) baseBarWeightKg = profile.baseBarWeightKg
            }

            val guardada = withContext(Dispatchers.IO) { activeSessionRepository.getActiveSession() }
            guardada.onSuccess { saved ->
                // Si el atleta ya reordenó o registró series mientras cargaba la
                // sesión guardada, no se sobrescribe lo que hizo.
                if (usuarioInteractuo) return@onSuccess
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

    // El catálogo global se carga en paralelo: una primera siembra lenta no
    // debe demorar la restauración de la sesión ni la referencia anterior.
    LaunchedEffect(Unit) {
        exerciseRepository.getCatalog()
            .onSuccess { globalCatalog = it }
            .onFailure { /* sin catálogo: el selector conserva los básicos fijos */ }
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

    // Reordenar en vivo el orden de ejecución de los ejercicios de la sesión.
    val dragState = rememberExerciseDragState { fromIndex, toIndex ->
        actualizarSesion { r -> moveExercise(r, fromIndex, toIndex) }
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
                    text = if (isExpress) "Rutina Express" else activeRoutine.name,
                    color = RoutineSkin.OnSurface,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )

                Text(
                    text = if (isExpress)
                        "Modo libre: agrega ejercicios y al terminar solo se guardan los datos de la sesión."
                    else
                        "",
                    color = RoutineSkin.OnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "$completedSets ${if (completedSets == 1) "serie completada" else "series completadas"} • ${formatElapsed(duracionMins)}",
                    color = RoutineSkin.Tertiary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (sesionReanudada) {
                    SteelCard(Modifier.padding(top = 12.dp)) {
                        MonoLabel("Sesión en curso guardada en el dispositivo", color = RoutineSkin.PrimaryText)
                        Text(
                            text = "Retomaste el entrenamiento donde lo dejaste.",
                            color = RoutineSkin.OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                        )
                        SteelOutlineButton(
                            text = "Descartar y empezar de cero",
                            contentColor = RoutineSkin.OnSurfaceVariant,
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { activeSessionRepository.clearActiveSession() }
                                    usuarioInteractuo = true
                                    activeRoutine = routine
                                    startedAt = System.currentTimeMillis()
                                    sesionReanudada = false
                                    persistirSesion()
                                    context.toast("Sesión anterior descartada")
                                }
                            }
                        )
                    }
                }

                activeRoutine.exercises.forEachIndexed { exIndex, exercise ->
                    ReorderableExerciseItem(dragState, exIndex) {
                        ExerciseCard(
                            exercise = exercise,
                            reference = referenciaPrevia[exercise.name].orEmpty(),
                            gymMode = true,
                            isDragging = dragState.isDragging(exIndex),
                            dragHandleModifier = Modifier.dragHandleFor(dragState, exIndex),
                            canMoveUp = exIndex > 0,
                            canMoveDown = exIndex < activeRoutine.exercises.lastIndex,
                            onMoveUp = { actualizarSesion { r -> moveExercise(r, exIndex, exIndex - 1) } },
                            onMoveDown = { actualizarSesion { r -> moveExercise(r, exIndex, exIndex + 1) } },
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
                }

                Spacer(Modifier.height(24.dp))
                SteelOutlineButton(
                    text = "Agregar ejercicio",
                    onClick = { showAddExerciseDialog = true },
                    leadingIcon = Icons.Filled.Add
                )

                Spacer(Modifier.height(12.dp))
                SteelOutlineButton(
                    text = "Calculadora de aproximación",
                    contentColor = RoutineSkin.OnSurfaceVariant,
                    onClick = { showWarmupCalculator = true },
                    leadingIcon = Icons.Filled.Calculate
                )

                SteelCard(Modifier.padding(top = 20.dp)) {
                    MonoLabel("Volumen completado", color = RoutineSkin.PrimaryText)
                    Text(
                        text = volumenCompletado,
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
                    text = if (guardando) "Guardando..." else "Finalizar Entrenamiento",
                    onClick = { showFinishDialog = true },
                    enabled = !guardando,
                    trailingIcon = Icons.Filled.Flag
                )
            }
        }
    }

    // Mientras se arrastra, la lista sigue sola cuando la tarjeta toca los bordes.
    AutoScrollWhileDragging(dragState, scrollState)

    if (showAddExerciseDialog) {
        ExercisePickerDialog(
            customExercises = customExercises,
            globalCatalog = globalCatalog,
            onSelect = { name, target ->
                actualizarSesion { r -> addExercise(r, name, target) }
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
                actualizarSesion { r -> applyWarmupPlan(r, exIndex, steps) }
                context.toast("Series de aproximación agregadas al ejercicio")
            },
            onDismiss = { showWarmupCalculator = false }
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
                        color = RoutineSkin.Tertiary,
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
