package com.example.lifterlab.ui.features.customexercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.CustomExercise
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CustomExerciseRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.FieldLabel
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.LifterTextField
import com.example.lifterlab.ui.components.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RF20 — Creación de Ejercicios Personalizados.
 * Permite al atleta dar de alta ejercicios propios no presentes en el catálogo global
 * y reutilizarlos al armar plantillas de rutina (RF03).
 *
 * Se expone como panel reutilizable (formulario + listado) para poder montarlo dentro
 * de otras pantallas —pestaña "Personalizados" del módulo Catálogos— sin duplicar lógica.
 */
@Composable
fun CustomExercisesPanel(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = remember { AuthRepository() },
    customExerciseRepository: CustomExerciseRepository = remember { CustomExerciseRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CustomExercise.MUSCLE_CATEGORIES.first()) }
    var medicion by remember { mutableStateOf(CustomExercise.MEASUREMENT_WEIGHT_REPS) }

    var ejercicios by remember { mutableStateOf<List<CustomExercise>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var ejercicioAEliminar by remember { mutableStateOf<CustomExercise?>(null) }

    suspend fun recargar() {
        val result = withContext(Dispatchers.IO) { customExerciseRepository.getCustomExercises(userId) }
        result.onSuccess { ejercicios = it }
            .onFailure { e -> mensajeError = e.message ?: "No se pudieron cargar tus ejercicios." }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) recargar()
        cargando = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp)
    ) {
        LifterCard {
            CardCaption("NUEVO EJERCICIO")

                Spacer(Modifier.height(14.dp))
                FieldLabel("Nombre del ejercicio *")
                Spacer(Modifier.height(6.dp))
                LifterTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "ej. Sentadilla Búlgara"
                )

                Spacer(Modifier.height(16.dp))
                FieldLabel("Categoría muscular principal")
                Spacer(Modifier.height(6.dp))
                LifterDropdown(
                    options = CustomExercise.MUSCLE_CATEGORIES,
                    selected = categoria,
                    onSelect = { categoria = it }
                )

                Spacer(Modifier.height(16.dp))
                FieldLabel("Tipo de medición")
                Spacer(Modifier.height(6.dp))
                LifterDropdown(
                    options = CustomExercise.MEASUREMENT_OPTIONS.map { it.second },
                    selected = CustomExercise.labelOf(medicion),
                    onSelect = { label ->
                        medicion = CustomExercise.MEASUREMENT_OPTIONS.first { it.second == label }.first
                    }
                )

                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = if (guardando) "Guardando..." else "Guardar Ejercicio",
                    enabled = !guardando,
                    onClick = {
                        mensajeError = ""
                        val trimmed = nombre.trim()
                        if (trimmed.isBlank()) {
                            mensajeError = "El nombre del ejercicio es obligatorio."
                            return@PrimaryButton
                        }
                        if (userId.isEmpty()) {
                            mensajeError = "No hay una sesión iniciada."
                            return@PrimaryButton
                        }
                        guardando = true
                        scope.launch {
                            val exercise = CustomExercise(
                                name = trimmed,
                                primaryMuscle = categoria,
                                measurementType = medicion,
                                createdAt = System.currentTimeMillis()
                            )
                            val result = withContext(Dispatchers.IO) {
                                customExerciseRepository.saveCustomExercise(userId, exercise)
                            }
                            result
                                .onSuccess {
                                    context.toast("Ejercicio '$trimmed' guardado")
                                    nombre = ""
                                    recargar()
                                }
                                .onFailure { e -> mensajeError = e.message ?: "Error al guardar el ejercicio." }
                            guardando = false
                        }
                    }
                )
            }

            if (mensajeError.isNotEmpty()) {
                ErrorText(mensajeError, Modifier.padding(top = 12.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Mis Ejercicios",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            if (cargando) {
                LifterCard(Modifier.padding(top = 12.dp)) {
                    CardCaption("CARGANDO...")
                }
            } else if (ejercicios.isEmpty()) {
                LifterCard(Modifier.padding(top = 12.dp)) {
                    CardCaption("SIN EJERCICIOS PERSONALIZADOS")
                    Text(
                        text = "Los que crees aquí aparecerán junto al catálogo al editar una rutina.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                ejercicios.forEach { exercise ->
                    CustomExerciseRow(
                        exercise = exercise,
                        onDelete = { ejercicioAEliminar = exercise }
                    )
                }
            }
    }

    ejercicioAEliminar?.let { exercise ->
        AlertDialog(
            onDismissRequest = { ejercicioAEliminar = null },
            title = { Text("Eliminar Ejercicio") },
            text = { Text("¿Estás seguro de eliminar '${exercise.name}' de tus ejercicios personalizados?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            customExerciseRepository.deleteCustomExercise(userId, exercise.id)
                        }
                        result
                            .onSuccess {
                                context.toast("Ejercicio eliminado")
                                recargar()
                            }
                            .onFailure { e ->
                                mensajeError = e.message ?: "No se pudo eliminar el ejercicio."
                            }
                    }
                    ejercicioAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { ejercicioAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun CustomExerciseRow(
    exercise: CustomExercise,
    onDelete: () -> Unit
) {
    LifterCard(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${exercise.primaryMuscle} • ${CustomExercise.labelOf(exercise.measurementType)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Eliminar ejercicio",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDelete() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun LifterDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (expanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(12.dp)
                )
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
