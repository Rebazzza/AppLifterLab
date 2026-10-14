package com.example.lifterlab.ui.features.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.features.catalog.CatalogScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface RoutinesRoute {
    object List : RoutinesRoute
    data class Editor(val routine: Routine) : RoutinesRoute
    data class Runner(val routine: Routine, val isExpress: Boolean) : RoutinesRoute
    object Catalog : RoutinesRoute
}

@Composable
fun RoutinesScreen(
    onBack: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    routineRepository: RoutineRepository = remember { RoutineRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() }
) {
    var route by remember { mutableStateOf<RoutinesRoute>(RoutinesRoute.List) }

    // Boton atras nativo dentro del modulo: vuelve al listado y, desde el
    // listado, sale del modulo hacia el Inicio.
    BackHandler(enabled = route !is RoutinesRoute.List) { route = RoutinesRoute.List }
    BackHandler(enabled = route is RoutinesRoute.List) { onBack() }

    when (val current = route) {
        is RoutinesRoute.List -> RoutinesListScreen(
            authRepository = authRepository,
            routineRepository = routineRepository,
            onCreate = { route = RoutinesRoute.Editor(Routine(name = "Mi Nueva Rutina")) },
            onEdit = { route = RoutinesRoute.Editor(it) },
            onStart = { route = RoutinesRoute.Runner(it, isExpress = false) },
            onExpress = {
                route = RoutinesRoute.Runner(Routine(name = "Rutina Express"), isExpress = true)
            },
            onNavigateToCatalog = { route = RoutinesRoute.Catalog }
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
        RoutinesRoute.Catalog -> CatalogScreen(
            onBack = { route = RoutinesRoute.List }
        )
    }
}

@Composable
private fun RoutinesListScreen(
    authRepository: AuthRepository,
    routineRepository: RoutineRepository,
    onCreate: () -> Unit,
    onEdit: (Routine) -> Unit,
    onStart: (Routine) -> Unit,
    onExpress: () -> Unit,
    onNavigateToCatalog: () -> Unit
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

    Surface(color = RoutineSkin.Background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Vista raíz del menú: el panel no muestra botón de volver (la barra
            // flotante y el botón atrás nativo cubren la salida del módulo).
            RoutineTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .padding(bottom = 104.dp)
            ) {
                Text(
                    text = "Rutinas",
                    color = RoutineSkin.OnSurface,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Crea, edita e inicia tus entrenamientos.",
                    color = RoutineSkin.OnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(18.dp))
                AccentWideButton(
                    text = "Crear Nueva Rutina",
                    onClick = onCreate,
                    trailingIcon = Icons.Filled.Add
                )

                ExpressCard(onClick = onExpress)

                Spacer(Modifier.height(12.dp))
                SteelOutlineButton(
                    text = "Catálogos",
                    contentColor = RoutineSkin.OnSurfaceVariant,
                    onClick = onNavigateToCatalog,
                    leadingIcon = Icons.Filled.ChevronRight
                )

                MonoLabel(
                    "Mis Rutinas",
                    color = RoutineSkin.OnSurface,
                    fontSize = 13,
                    modifier = Modifier.padding(top = 24.dp)
                )

                if (savedTemplates.isEmpty()) {
                    SteelCard(Modifier.padding(top = 12.dp)) {
                        MonoLabel("Sin rutinas creadas", color = RoutineSkin.PrimaryText)
                        Text(
                            text = "Presiona \"Crear Nueva Rutina\" para empezar.",
                            color = RoutineSkin.OnSurfaceVariant,
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
                    Text(
                        text = mensajeError,
                        color = RoutineSkin.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .background(RoutineSkin.SteelSurface)
            .border(BorderStroke(1.dp, RoutineSkin.PrimaryText), shape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RoutineSkin.PrimaryText.copy(alpha = 0.12f))
                .border(BorderStroke(1.dp, RoutineSkin.PrimaryText), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = RoutineSkin.PrimaryText,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = "Rutina Express",
                color = RoutineSkin.PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Entrena libre: los datos se guardan, la rutina no.",
                color = RoutineSkin.OnSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = RoutineSkin.PrimaryText,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    SteelCard(Modifier.padding(top = 12.dp)) {
        Text(
            text = routine.name,
            color = RoutineSkin.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        MonoLabel(
            "${routine.exercises.size} Ejercicios • ${routine.estimatedTimeMins} mins",
            color = RoutineSkin.OnSurfaceVariant,
            fontSize = 11,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StartButton(text = "Iniciar", onClick = onStart, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(8.dp))
            RoundIconButton(
                icon = Icons.Filled.Edit,
                contentDescription = "Editar ${routine.name}",
                onClick = onEdit
            )
            Spacer(Modifier.size(8.dp))
            RoundIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = "Borrar ${routine.name}",
                onClick = onDelete,
                tint = RoutineSkin.Error
            )
        }
    }
}

@Composable
private fun StartButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(RoutineSkin.InversePrimary)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = RoutineSkin.OnPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            color = RoutineSkin.OnPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
