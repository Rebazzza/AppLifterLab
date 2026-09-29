package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.data.repository.WarmupRepository
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

private sealed interface RoutinesRoute {
    object List : RoutinesRoute
    data class Editor(val routine: Routine) : RoutinesRoute
    data class Runner(val routine: Routine, val isExpress: Boolean) : RoutinesRoute
}

@Composable
fun RoutinesScreen(
    onBack: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    routineRepository: RoutineRepository = remember { RoutineRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() }
) {
    var route by remember { mutableStateOf<RoutinesRoute>(RoutinesRoute.List) }

    when (val current = route) {
        is RoutinesRoute.List -> RoutinesListScreen(
            onBack = onBack,
            authRepository = authRepository,
            routineRepository = routineRepository,
            onCreate = { route = RoutinesRoute.Editor(Routine(name = "Mi Nueva Rutina")) },
            onEdit = { route = RoutinesRoute.Editor(it) },
            onStart = { route = RoutinesRoute.Runner(it, isExpress = false) },
            onExpress = {
                route = RoutinesRoute.Runner(Routine(name = "Rutina Express"), isExpress = true)
            }
        )
        is RoutinesRoute.Editor -> RoutineEditorScreen(
            routine = current.routine,
            onBack = { route = RoutinesRoute.List },
            onSaved = { route = RoutinesRoute.List },
            authRepository = authRepository,
            routineRepository = routineRepository
        )
        is RoutinesRoute.Runner -> WorkoutRunnerScreen(
            routine = current.routine,
            isExpress = current.isExpress,
            onBack = { route = RoutinesRoute.List },
            onFinished = { route = RoutinesRoute.List },
            authRepository = authRepository,
            warmupRepository = warmupRepository
        )
    }
}

@Composable
private fun RoutinesListScreen(
    onBack: () -> Unit,
    authRepository: AuthRepository,
    routineRepository: RoutineRepository,
    onCreate: () -> Unit,
    onEdit: (Routine) -> Unit,
    onStart: (Routine) -> Unit,
    onExpress: () -> Unit
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var savedTemplates by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }
    var mensajeError by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        val result = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
        result.onSuccess { savedTemplates = it }
            .onFailure { e -> mensajeError = e.message ?: "No se pudieron cargar las rutinas." }
    }

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

            ScreenTitle("Rutinas", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Crea, edita e inicia tus entrenamientos.",
                Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            PrimaryButton(
                text = "Crear Nueva Rutina",
                onClick = onCreate
            )

            ExpressCard(onClick = onExpress)

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Mis Rutinas",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            if (savedTemplates.isEmpty()) {
                LifterCard(Modifier.padding(top = 12.dp)) {
                    CardCaption("SIN RUTINAS CREADAS")
                    Text(
                        text = "Presiona \"Crear Nueva Rutina\" para empezar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                savedTemplates.forEach { routine ->
                    RoutineRow(
                        routine = routine,
                        onEdit = { onEdit(routine) },
                        onStart = { onStart(routine) },
                        onDelete = { routineToDelete = routine }
                    )
                }
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ErrorText(mensajeError, Modifier.fillMaxWidth())
            }
        }
    }

    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Eliminar Rutina") },
            text = { Text("¿Estás seguro de eliminar la rutina '${routine.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            routineRepository.deleteRoutine(userId, routine.id)
                        }
                        result.onSuccess {
                            context.toast("Rutina eliminada")
                            val list = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
                            if (list.isSuccess) savedTemplates = list.getOrThrow()
                        }.onFailure { e ->
                            mensajeError = e.message ?: "No se pudo eliminar la rutina."
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
private fun ExpressCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                text = "Rutina Express",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Entrena libre: los datos se guardan, la rutina no.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    LifterCard(Modifier.padding(top = 12.dp)) {
        Column {
            Text(
                text = routine.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${routine.exercises.size} Ejercicios • ${routine.estimatedTimeMins} mins",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StartButton("Iniciar", onClick = onStart, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(8.dp))
            ActionButton(Icons.Filled.Edit, "Editar", onClick = onEdit, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(8.dp))
            ActionButton(Icons.Filled.Delete, "Borrar", onClick = onDelete, modifier = Modifier.weight(1f), danger = true)
        }
    }
}

@Composable
private fun RowScope.StartButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RowScope.ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    danger: Boolean = false
) {
    val contentColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}