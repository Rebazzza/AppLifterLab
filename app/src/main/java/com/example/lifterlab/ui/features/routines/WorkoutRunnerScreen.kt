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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
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
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.warmup.WarmupCalculatorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    routineRepository: RoutineRepository = remember { RoutineRepository() },
    context: android.content.Context = LocalContext.current
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val activeSessionRepository = remember(context) { ActiveSessionRepository(context) }
    val customExerciseRepository = remember { CustomExerciseRepository() }
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    // Sesión guardada en el dispositivo: se lee de forma SÍNCRONA en la primera
    // composición (es un JSON chico que ya vive en memoria) para que las series ya
    // marcadas y el inicio del cronómetro estén presentes ANTES de pintar la lista.
    // Si la sesión llegara en caliente, cada fila recordaría en memoria el estado
    // vacío y las marcas hechas parecerían borradas al salir y volver a entrar.
    val sesionGuardada = remember(routine, isExpress) {
        val guardada = activeSessionRepository.peekActiveSession()
        if (guardada != null && guardada.matchesRoutine(routine, isExpress)) guardada else null
    }

    // Restauración de la sesión en curso: ejercicios (con sus series marcadas) y punto
    // de arranque del cronómetro, todo presente desde el primer pintado.
    var activeRoutine by remember {
        mutableStateOf(sesionGuardada?.let { routine.copy(exercises = it.exercises) } ?: routine)
    }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Calculadora de Aproximación: peso de la barra base desde el perfil.
    var showWarmupCalculator by remember { mutableStateOf(false) }
    var baseBarWeightKg by remember { mutableStateOf(20.0) }

    // Sesión activa en curso: el cronómetro arranca en el instante en que se inicia la
    // rutina y, si se retoma una sesión guardada, continúa desde donde iba.
    var startedAt by remember { mutableStateOf(sesionGuardada?.startedAtMillis ?: System.currentTimeMillis()) }
    var sesionReanudada by remember { mutableStateOf(sesionGuardada != null) }
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }

    // Referencia de la sesión anterior para las tarjetas de series.
    var referenciaPrevia by remember { mutableStateOf<Map<String, List<ExerciseSet>>>(emptyMap()) }

    // RF25 — Récords históricos por nombre de ejercicio. Encienden la fila amarilla de
    // Récord Personal en el Modo Gimnasio en cuanto una serie completada los supera.
    var marcasRecord by remember { mutableStateOf<Map<String, MarcaDeEjercicio>>(emptyMap()) }

    // ejercicios propios del atleta, reutilizables durante el Modo Gimnasio.
    var customExercises by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }

    // Catálogo global de /exercises para el selector (sección "Catálogo global").
    var globalCatalog by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    val exerciseRepository = remember { ExerciseRepository() }

    // Temporizador de descanso en vivo (HU-12, RNF04 / RNF11)
    var timerTargetTimeMillis by remember { mutableStateOf<Long?>(null) }
    var timerExerciseName by remember { mutableStateOf("") }
    var timerRemainingSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Inicializar canal de notificaciones para descanso
    LaunchedEffect(Unit) {
        RestTimerNotifier.createNotificationChannel(context)
    }

    // Corrutina de cuenta regresiva resistente a pausas en segundo plano
    LaunchedEffect(timerTargetTimeMillis, isTimerRunning) {
        if (isTimerRunning && timerTargetTimeMillis != null) {
            while (isTimerRunning) {
                val now = System.currentTimeMillis()
                val diff = timerTargetTimeMillis!! - now
                val secs = (diff / 1000L).toInt()
                if (secs <= 0) {
                    timerRemainingSeconds = 0
                    isTimerRunning = false
                    timerTargetTimeMillis = null
                    RestTimerNotifier.notifyRestFinished(context, timerExerciseName)
                    break
                } else {
                    timerRemainingSeconds = secs
                }
                delay(500)
            }
        }
    }

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
        // Si no había sesión guardada para esta rutina, esta apertura es la que crea la
        // sesión activa (la que sostiene el panel flotante mientras el atleta entrena).
        if (sesionGuardada == null) persistirSesion()

        if (userId.isNotEmpty()) {
            // RF25 — Un único historial alimenta la referencia de la sesión anterior y los
            // récords en vivo. Se pide el mismo volumen que al cerrar la sesión (RF29) para
            // que la fila amarilla del Modo Gimnasio coincida con lo que quedará guardado.
            // Lectura opcional y no bloqueante: sin historial simplemente no hay marcas.
            val referencia = withContext(Dispatchers.IO) {
                warmupRepository.getWorkoutSessions(userId, SESIONES_PARA_RECORDS)
            }
            referencia.onSuccess { sesiones ->
                referenciaPrevia = previousSessionReference(sesiones)
                marcasRecord = marcasHistoricas(sesiones)
            }

            val propios = withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
            propios.onSuccess { customExercises = it }

            val perfil = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            perfil.onSuccess { profile ->
                if (profile.baseBarWeightKg > 0) baseBarWeightKg = profile.baseBarWeightKg
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

    // El reloj de la sesión avanza segundo a segundo: es lo que hace que el cronómetro
    // de la vista (y el del panel flotante) se vea corriendo mientras se entrena.
    LaunchedEffect(startedAt) {
        while (true) {
            delay(1000)
            ahora = System.currentTimeMillis()
        }
    }

    val volumenCompletado = calculateCompletedVolume(activeRoutine)
    val completedSets = activeRoutine.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val duracionSegs = ((ahora - startedAt) / 1000L).toInt().coerceAtLeast(0)
    val duracionMins = duracionSegs / 60

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
                    text = "$completedSets ${if (completedSets == 1) "serie completada" else "series completadas"} • ${formatElapsed(duracionSegs)}",
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
                                    // El repositorio ya hace su trabajo en IO: no hace falta
                                    // envolverlo, así el espejo de la sesión (y el panel
                                    // flotante) se actualiza en el hilo principal.
                                    activeSessionRepository.clearActiveSession()
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

                if (isTimerRunning || timerRemainingSeconds > 0) {
                    RestTimerWidget(
                        remainingSeconds = timerRemainingSeconds,
                        exerciseName = timerExerciseName,
                        onAdjustTime = { delta ->
                            val newSecs = (timerRemainingSeconds + delta).coerceAtLeast(0)
                            if (newSecs == 0) {
                                isTimerRunning = false
                                timerTargetTimeMillis = null
                                timerRemainingSeconds = 0
                            } else {
                                timerRemainingSeconds = newSecs
                                timerTargetTimeMillis = System.currentTimeMillis() + (newSecs * 1000L)
                                if (!isTimerRunning) isTimerRunning = true
                            }
                        },
                        onSkipRest = {
                            isTimerRunning = false
                            timerTargetTimeMillis = null
                            timerRemainingSeconds = 0
                        },
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                activeRoutine.exercises.forEachIndexed { exIndex, exercise ->
                    ReorderableExerciseItem(dragState, exIndex) {
                        ExerciseCard(
                            exercise = exercise,
                            reference = referenciaPrevia[exercise.name].orEmpty(),
                            records = marcasRecord,
                            gymMode = true,
                            isDragging = dragState.isDragging(exIndex),
                            dragHandleModifier = Modifier.dragHandleFor(dragState, exIndex),
                            canMoveUp = exIndex > 0,
                            canMoveDown = exIndex < activeRoutine.exercises.lastIndex,
                            onMoveUp = { actualizarSesion { r -> moveExercise(r, exIndex, exIndex - 1) } },
                            onMoveDown = { actualizarSesion { r -> moveExercise(r, exIndex, exIndex + 1) } },
                            onUpdateSet = { setIndex, weightKg, reps, rpe, isCompleted ->
                                if (isCompleted == true) {
                                    val setAct = exercise.sets.getOrNull(setIndex)
                                    val wasCompleted = setAct?.isCompleted == true
                                    if (!wasCompleted) {
                                        // Inicia el temporizador de descanso automáticamente al completar la serie (105s = 1:45)
                                        val defaultRest = 105
                                        timerExerciseName = exercise.name
                                        timerRemainingSeconds = defaultRest
                                        timerTargetTimeMillis = System.currentTimeMillis() + (defaultRest * 1000L)
                                        isTimerRunning = true
                                    }
                                }
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

                // Cancelar: cierra la ventana, borra la sesión activa (y con ella el
                // panel flotante) y NO escribe nada en el historial.
                SteelOutlineButton(
                    text = "Cancelar Rutina",
                    contentColor = RoutineSkin.Error,
                    leadingIcon = Icons.Filled.Close,
                    onClick = { if (!guardando) showCancelDialog = true }
                )

                Spacer(Modifier.height(12.dp))
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
                        text = "$completedSets series • $volumenCompletado • ${formatElapsed(duracionSegs)}",
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
                        plantilla = routine,
                        isExpress = isExpress,
                        durationMins = duracionMins.coerceAtLeast(1),
                        scope = scope,
                        context = context,
                        warmupRepository = warmupRepository,
                        activeSessionRepository = activeSessionRepository,
                        routineRepository = routineRepository,
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

    // Cancelar la rutina: cierra la ventana, borra la sesión activa (y con ella el panel
    // flotante) sin escribir nada en el historial.
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Rutina") },
            text = {
                Text(
                    "Se cerrará el entrenamiento y se descartará todo lo hecho en esta sesión: " +
                        "nada se guardará en tu historial."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    guardando = true
                    mensajeError = ""
                    scope.launch {
                        // Solo se limpia la sesión activa; no se consolida la sesión.
                        activeSessionRepository.clearActiveSession()
                        guardando = false
                        context.toast("Rutina cancelada")
                        onFinished()
                    }
                }) { Text("Sí, cancelar", color = RoutineSkin.Error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Seguir entrenando") }
            }
        )
    }
}

private fun ActiveSession.matchesRoutine(routine: Routine, isExpress: Boolean): Boolean {
    if (isFreeSession != isExpress) return false
    return if (routineId.isNotEmpty()) routineId == routine.id else routineName == routine.name
}

/** Tiempo transcurrido de la sesión en HH:MM:SS (el reloj avanza segundo a segundo). */
private fun formatElapsed(segs: Int): String {
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", segs / 3600, (segs % 3600) / 60, segs % 60)
}

private fun terminarSesion(
    userId: String,
    routine: Routine,
    plantilla: Routine,
    isExpress: Boolean,
    durationMins: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    warmupRepository: WarmupRepository,
    activeSessionRepository: ActiveSessionRepository,
    routineRepository: RoutineRepository,
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
    scope.launch {
        // RF29 — récords: se leen los máximos históricos del usuario ANTES de guardar
        // para poder marcar automáticamente las series que los superan. RNF03: esta
        // lectura es opcional y acotada, para que una red lenta jamás retrase el guardado
        // del entrenamiento (sin historial simplemente no se marcan récords).
        val previas = withTimeoutOrNull(8_000) {
            withContext(Dispatchers.IO) {
                warmupRepository.getWorkoutSessions(userId, SESIONES_PARA_RECORDS)
            }
        }?.getOrElse { emptyList() } ?: emptyList()
        val sesion = buildWorkoutSession(routine, isExpress, durationMins, marcasHistoricas(previas))

        val result = withContext(Dispatchers.IO) { warmupRepository.saveWorkoutSession(userId, sesion) }
        result
            .onSuccess {
                activeSessionRepository.clearActiveSession()

                // Refleja en la plantilla los pesos y repeticiones usados en la sesión, de
                // modo que la próxima vez que se arranque la rutina aparezcan esas cargas
                // (120 kg en lugar de los 100 kg con los que fue creada). Solo aplica a
                // rutinas guardadas (no a Express) y como mejor esfuerzo: un fallo aquí
                // no invalida el entrenamiento ya consolidado en el historial.
                if (!isExpress && plantilla.id.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        routineRepository.saveRoutine(userId, applySessionValues(plantilla, routine))
                    }
                }

                onSuccess(formatVolumeText(sesion.totalVolumeKg))
            }
            .onFailure { e -> onError(e.message ?: "No se pudo guardar la sesión.") }
    }
}

/** Sesiones del historial que se leen para decidir si una serie es Récord Personal. */
private const val SESIONES_PARA_RECORDS = 200

private fun formatVolumeText(volumeKg: Double): String {
    return if (volumeKg >= 1000) {
        String.format(java.util.Locale.ROOT, "%.1fk kg", volumeKg / 1000.0)
    } else {
        String.format(java.util.Locale.ROOT, "%.0f kg", volumeKg)
    }
}
