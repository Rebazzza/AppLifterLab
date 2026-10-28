package com.example.lifterlab.ui.features.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CustomExerciseRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.SecondaryButton
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.routines.ExerciseCard
import com.example.lifterlab.ui.features.routines.ExercisePickerDialog
import com.example.lifterlab.ui.features.routines.addExercise
import com.example.lifterlab.ui.features.routines.addSetToExercise
import com.example.lifterlab.ui.features.routines.moveExercise
import com.example.lifterlab.ui.features.routines.removeExercise
import com.example.lifterlab.ui.features.routines.updateSetData
import com.example.lifterlab.ui.formatSessionDate
import com.example.lifterlab.ui.formatShortDate
import com.example.lifterlab.ui.formatVolumeKg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HISTORY_LIMIT = 100

/**
 * RF17 — Historial de sesiones registradas: lista con detalle, edición de peso, repeticiones,
 * RPE y ejercicios de una sesión finalizada, y eliminación de la sesión.
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()

    var sessions by remember { mutableStateOf<List<WorkoutSession>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<WorkoutSession?>(null) }

    suspend fun cargar() {
        loading = true
        error = ""
        if (userId.isEmpty()) {
            error = "No hay una sesión iniciada."
        } else {
            withContext(Dispatchers.IO) { warmupRepository.getWorkoutSessions(userId, HISTORY_LIMIT) }
                .onSuccess { sessions = it }
                .onFailure { e -> error = e.message ?: "No se pudo cargar el historial." }
        }
        loading = false
    }

    LaunchedEffect(userId) { cargar() }

    BackHandler(enabled = selected != null) { selected = null }

    val current = selected
    if (current == null) {
        HistoryList(
            sessions = sessions,
            loading = loading,
            error = error,
            onBack = onBack,
            onOpen = { selected = it }
        )
    } else {
        // Detalle en solo lectura; el editor se abre desde el botón "Editar Sesión".
        SessionDetailFlow(
            session = current,
            userId = userId,
            warmupRepository = warmupRepository,
            onBack = { selected = null },
            onChanged = {
                selected = null
                scope.launch { cargar() }
            }
        )
    }
}

@Composable
private fun HistoryList(
    sessions: List<WorkoutSession>,
    loading: Boolean,
    error: String,
    onBack: () -> Unit,
    onOpen: (WorkoutSession) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = bottomBarClearance())
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle("Historial", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Toca una sesión para ver su detalle, corregirla o eliminarla.",
                Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            if (error.isNotEmpty()) {
                ErrorText(error, Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            when {
                loading -> CardCaption("CARGANDO SESIONES...", Modifier.padding(top = 12.dp), mono = true)
                sessions.isEmpty() && error.isEmpty() -> LifterCard(Modifier.padding(top = 12.dp)) {
                    CardCaption("SIN SESIONES REGISTRADAS")
                    Text(
                        text = "Termina un entrenamiento desde Rutinas para verlo aquí.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                else -> sessions.forEach { session ->
                    SessionRow(session, onClick = { onOpen(session) })
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onClick: () -> Unit) {
    LifterCard(Modifier.padding(top = 12.dp).clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(session.routineName.ifBlank { "Sesión" }, Modifier.weight(1f))
            Text(
                text = formatShortDate(session.date),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = if (session.isFreeSession) "SESIÓN EXPRESS" else "SESIÓN DE RUTINA",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "${formatVolumeKg(session.totalVolumeKg)} • ${session.exercises.size} ejercicios • " +
                "${session.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }} series • " +
                "${session.durationMins} min",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * RF17 — Editor de una sesión finalizada: corrige peso, repeticiones, RPE, ejercicios o
 * elimina la sesión. Se abre desde [SessionDetailFlow] cuando el atleta pide editar, con
 * todos los datos de la sesión ya cargados y las series marcadas.
 */
@Composable
internal fun SessionEditor(
    session: WorkoutSession,
    userId: String,
    warmupRepository: WarmupRepository,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customExerciseRepository = remember { CustomExerciseRepository() }

    var exercises by remember(session.id) { mutableStateOf(session.exercises) }
    // Se incrementa en cambios estructurales para reiniciar el estado local de las filas de series.
    var version by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    // null = selector cerrado; -1 = agregar ejercicio; >= 0 = reemplazar ese ejercicio.
    var pickerTarget by remember { mutableStateOf<Int?>(null) }
    var customExercises by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
                .onSuccess { customExercises = it }
        }
    }

    fun editar(structural: Boolean = false, cambio: (Routine) -> Routine) {
        exercises = cambio(Routine(exercises = exercises)).exercises
        if (structural) version++
        error = ""
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = bottomBarClearance())
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle(session.routineName.ifBlank { "Sesión" }, Modifier.padding(top = 8.dp))
            Text(
                text = "${formatSessionDate(session.date)} • ${formatShortDate(session.date)} • ${session.durationMins} min",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
            ScreenSubtitle(
                "Corrige peso, repeticiones y RPE. Una serie desmarcada se guarda como omitida.",
                Modifier.padding(top = 8.dp)
            )

            exercises.forEachIndexed { exIndex, exercise ->
                key(version, exIndex) {
                    Column {
                        Text(
                            text = "CAMBIAR EJERCICIO",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .clickable { pickerTarget = exIndex }
                                .padding(vertical = 4.dp)
                        )
                        ExerciseCard(
                            exercise = exercise,
                            canMoveUp = exIndex > 0,
                            canMoveDown = exIndex < exercises.lastIndex,
                            onMoveUp = { editar(true) { r -> moveExercise(r, exIndex, exIndex - 1) } },
                            onMoveDown = { editar(true) { r -> moveExercise(r, exIndex, exIndex + 1) } },
                            onUpdateSet = { setIndex, weightKg, reps, rpe, isCompleted ->
                                editar { r -> updateSetData(r, exIndex, setIndex, weightKg, reps, rpe, isCompleted) }
                            },
                            onAddSet = {
                                editar { r ->
                                    val withSet = addSetToExercise(r, exIndex)
                                    val newIndex = withSet.exercises[exIndex].sets.lastIndex
                                    updateSetData(withSet, exIndex, newIndex, isCompleted = true)
                                }
                                version++
                            },
                            onDeleteExercise = { editar(true) { r -> removeExercise(r, exIndex) } }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SecondaryButton(
                text = "+ Agregar Ejercicio",
                onClick = { pickerTarget = -1 },
                minHeight = 48.dp
            )

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ErrorText(error, Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (guardando) "Guardando..." else "Guardar Cambios",
                enabled = !guardando,
                onClick = {
                    buildEditedSession(session, exercises)
                        .onFailure { e -> error = e.message ?: "Datos inválidos." }
                        .onSuccess { edited ->
                            guardando = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    warmupRepository.updateWorkoutSession(userId, edited)
                                }
                                guardando = false
                                result
                                    .onSuccess {
                                        context.toast("Sesión actualizada")
                                        onChanged()
                                    }
                                    .onFailure { e -> error = e.message ?: "No se pudo guardar la sesión." }
                            }
                        }
                }
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = "Eliminar Sesión",
                onClick = { showDelete = true },
                minHeight = 48.dp
            )
        }
    }

    pickerTarget?.let { target ->
        val currentExName = if (target >= 0 && target in exercises.indices) exercises[target].name else null
        ExercisePickerDialog(
            customExercises = customExercises,
            currentExerciseName = currentExName,
            onSelect = { name, muscles ->
                if (target < 0) {
                    editar(true) { r -> addExercise(r, name, muscles) }
                    // addExercise crea series sin completar: las marca para que cuenten al guardar.
                    exercises = exercises.mapIndexed { i, ex ->
                        if (i == exercises.lastIndex) ex.copy(sets = ex.sets.map { it.copy(isCompleted = true) }) else ex
                    }
                } else {
                    exercises = exercises.mapIndexed { i, ex ->
                        if (i == target) ex.copy(name = name, targetMuscles = muscles) else ex
                    }
                    version++
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
    }

    if (showDelete) {
        DeleteSessionConfirmationDialog(
            session = session,
            onConfirm = {
                showDelete = false
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        warmupRepository.deleteWorkoutSession(userId, session.id)
                    }
                    result
                        .onSuccess {
                            context.toast("Sesión eliminada")
                            onChanged()
                        }
                        .onFailure { e -> error = e.message ?: "No se pudo eliminar la sesión." }
                }
            },
            onDismiss = { showDelete = false }
        )
    }
}

/**
 * RF17 — Diálogo Modal de Confirmación de Eliminación según especificación visual Titanio y Acero.
 */
@Composable
fun DeleteSessionConfirmationDialog(
    session: WorkoutSession,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1B1D))
                    .border(2.dp, Color(0xFF2A2A2C), RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {}
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con Ícono de Alerta en Error Color
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFB4AB))
                            .border(1.dp, Color(0x4DFFB4AB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Alerta",
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "¿Eliminar esta sesión?",
                            color = Color(0xFFE6E1E6),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "Acción irreversible",
                            color = Color(0xFFC9C4CA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Tarjeta informativa con el Nombre, Fecha y Volumen de la sesión
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF201F22))
                        .border(1.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = session.routineName.ifBlank { "Sesión" },
                            color = Color(0xFFE6E1E6),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatShortDate(session.date),
                            color = Color(0xFFC9C4CA),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = formatVolumeKg(session.totalVolumeKg),
                        color = Color(0xFFC1C1FB),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Texto de confirmación
                Text(
                    text = "Esta acción no se puede deshacer. Se borrarán todas las series registradas, cálculos de volumen y marcas históricas asociadas a este día.",
                    color = Color(0xFFC9C4CA),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                // Botones de Acción: CANCELAR y ELIMINAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF201F22))
                            .border(2.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCELAR",
                            color = Color(0xFFE6E1E6),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFB4AB))
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Color(0xFF410002),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ELIMINAR",
                                color = Color(0xFF410002),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
