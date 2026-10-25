package com.example.lifterlab.ui.features.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.Exercise
import com.example.lifterlab.data.repository.ExerciseRepository
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ChippedTag
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.customexercises.CustomExercisesPanel
import com.example.lifterlab.ui.features.routines.RoutineSkin
import com.example.lifterlab.ui.features.routines.RoutineTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CatalogTab {
    Global,
    Personalizados
}

/**
 * Módulo Catálogos: explora el catálogo global de ejercicios y los ejercicios
 * personalizados del atleta. El panel de pestañas permanece visible mientras se
 * cambia de un catálogo a otro.
 * Las herramientas secundarias viven en el módulo Herramientas de la barra
 * flotante, no aquí.
 */
@Composable
fun CatalogScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(CatalogTab.Global) }

    // Ruta interna del módulo: al tocar un ejercicio se abre su vista de detalle
    // (única pantalla que descarga la demostración visual).
    var exerciseIdSeleccionado by remember { mutableStateOf<String?>(null) }

    exerciseIdSeleccionado?.let { exerciseId ->
        ExerciseDetailScreen(
            exerciseId = exerciseId,
            onBack = { exerciseIdSeleccionado = null }
        )
        return
    }

    Surface(color = RoutineSkin.Background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            RoutineTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                ScreenTitle("Catálogos")
                ScreenSubtitle(
                    "Catálogo global y tus ejercicios personalizados.",
                    Modifier.padding(top = 4.dp)
                )
            }

            CatalogTabPanel(
                current = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            when (tab) {
                CatalogTab.Global -> GlobalCatalogPanel(
                    modifier = Modifier.weight(1f),
                    onExerciseClick = { exerciseId -> exerciseIdSeleccionado = exerciseId }
                )
                CatalogTab.Personalizados -> CustomExercisesPanel(
                    modifier = Modifier.weight(1f),
                    scrollTail = bottomBarClearance()
                )
            }
        }
    }
}

@Composable
private fun CatalogTabPanel(
    current: CatalogTab,
    onSelect: (CatalogTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CatalogTabItem(
            text = "Catálogo Global",
            isActive = current == CatalogTab.Global,
            onClick = { onSelect(CatalogTab.Global) },
            modifier = Modifier.weight(1f)
        )
        CatalogTabItem(
            text = "Personalizados",
            isActive = current == CatalogTab.Personalizados,
            onClick = { onSelect(CatalogTab.Personalizados) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CatalogTabItem(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun GlobalCatalogPanel(
    modifier: Modifier = Modifier,
    onExerciseClick: (String) -> Unit
) {
    val repository = remember { ExerciseRepository() }
    val scope = rememberCoroutineScope()

    var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }

    // Firestore-First: lee el catálogo cacheado (offline persistence) y solo
    // ExerciseDB es contactado por el repository cuando la colección está vacía.
    suspend fun recargar() {
        cargando = true
        val result = withContext(Dispatchers.IO) { repository.getCatalog() }
        result
            .onSuccess { exercises = it }
            .onFailure { e -> mensajeError = e.message ?: "No se pudo cargar el catálogo." }
        cargando = false
    }

    LaunchedEffect(Unit) { recargar() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = bottomBarClearance()
        )
    ) {
        item {
            LifterCard {
                CardCaption("CATÁLOGO GLOBAL · ${exercises.size} EJERCICIOS")
                Text(
                    text = "Estos ejercicios están disponibles en todas las plantillas de rutina.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        if (cargando) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (mensajeError.isNotEmpty()) {
            item {
                LifterCard(Modifier.padding(top = 12.dp)) {
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
                            .clickable {
                                mensajeError = ""
                                scope.launch { recargar() }
                            }
                    )
                }
            }
        }

        items(exercises, key = { it.exerciseId }) { exercise ->
            ExerciseCatalogCard(
                exercise = exercise,
                onClick = { onExerciseClick(exercise.exerciseId) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * Tarjeta individual del catálogo global: vista liviana SIN imágenes (la
 * demostración visual solo se descarga en la vista de detalle del ejercicio,
 * para no gastar ancho de banda en cada scroll). Al tocarla abre el detalle.
 */
@Composable
private fun ExerciseCatalogCard(
    exercise: Exercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LifterCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (exercise.isSBD) {
                        ChippedTag(
                            text = "SBD",
                            textColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Text(
                    text = "Objetivo: ${exercise.primaryMuscle} · ${exercise.equipment}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "Ver detalle ›",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
