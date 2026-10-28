package com.example.lifterlab.ui.features.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lifterlab.data.model.Exercise
import com.example.lifterlab.data.repository.ExerciseRepository
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ChippedTag
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.routines.RoutineSkin
import com.example.lifterlab.ui.features.routines.RoutineTopBar
import com.example.lifterlab.ui.theme.BgSurfaceContainer
import com.example.lifterlab.ui.theme.OutlineVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vista de Detalle del Ejercicio del catálogo global.
 *
 * Es la ÚNICA vista donde se descarga la demostración visual del ejercicio (foto
 * JPG del dataset abierto Free-Exercise-DB; Coil con coil-gif además sabría
 * animar un GIF si en el futuro se incorpora una fuente de animación): la lista
 * del catálogo no carga imágenes para no gastar ancho de banda ni cuota de
 * RapidAPI en cada scroll.
 *
 * El dato se resuelve desde Firestore (caché offline primero); solo si el ejercicio
 * no está cacheado, ExerciseRepository consulta ExerciseDB y persiste el resultado.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    repository: ExerciseRepository = remember { ExerciseRepository() }
) {
    val scope = rememberCoroutineScope()

    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }

    suspend fun cargar() {
        cargando = true
        mensajeError = ""
        val result = withContext(Dispatchers.IO) { repository.getExerciseById(exerciseId) }
        result
            .onSuccess { exercise = it }
            .onFailure { e -> mensajeError = e.message ?: "No se pudo cargar el ejercicio." }
        cargando = false
    }

    LaunchedEffect(exerciseId) { cargar() }

    Surface(color = RoutineSkin.Background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            RoutineTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .padding(bottom = bottomBarClearance())
            ) {
                when {
                    cargando -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    mensajeError.isNotEmpty() -> LifterCard {
                        CardCaption("ERROR DE CONEXIÓN")
                        Text(
                            text = mensajeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = "Reintentar",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clickable { scope.launch { cargar() } }
                        )
                    }

                    exercise != null -> ExerciseDetailContent(exercise!!)
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: Exercise) {
    // Contenedor de la demostración: superficie oscura #1C1B1D, esquinas 12dp y
    // borde suave 1dp outline_variant (Design System "Titanio y Acero").
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurfaceContainer)
            .border(
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Si la URL existe pero la carga falla (p. ej. sin red), se muestra el
        // mismo placeholder que cuando el dataset no tiene foto del ejercicio.
        var imagenNoCargada by remember(exercise.gifUrl) { mutableStateOf(false) }

        if (exercise.gifUrl.isNotBlank() && !imagenNoCargada) {
            // Coil carga la foto (y animaría el GIF si hubiera una fuente de
            // animación); crossfade suaviza la aparición y el contenedor oscuro
            // actúa de placeholder natural.
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(exercise.gifUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Demostración de ${exercise.name}",
                contentScale = ContentScale.Fit,
                onSuccess = { imagenNoCargada = false },
                onError = { imagenNoCargada = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Text(
                text = "SIN DEMOSTRACIÓN DISPONIBLE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = exercise.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        if (exercise.isSBD) {
            ChippedTag(
                text = "SBD",
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    LifterCard {
        CardCaption("FICHA TÉCNICA")
        DetailInfoRow(label = "Objetivo", value = exercise.primaryMuscle)
        DetailInfoRow(label = "Equipamiento", value = exercise.equipment)
        DetailInfoRow(
            label = "Categoría",
            value = if (exercise.isSBD) "Básico SBD" else "Accesorio"
        )
    }

    // RF29 — historial analítico del movimiento: curva de 1RM, récords y tonelaje.
    Spacer(Modifier.height(12.dp))
    ExerciseHistorySection(exerciseName = exercise.name)

    if (exercise.instructions.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        LifterCard {
            CardCaption("INSTRUCCIONES")
            exercise.instructions.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = step,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value.replaceFirstChar { it.uppercase() },
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
