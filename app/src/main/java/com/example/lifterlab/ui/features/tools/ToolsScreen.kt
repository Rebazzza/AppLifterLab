package com.example.lifterlab.ui.features.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.features.customexercises.CustomExercisesPanel
import com.example.lifterlab.ui.features.warmup.WarmupCalculatorPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Herramienta secundaria disponible desde el módulo Herramientas.
 * Cada entrada abre su propio panel a pantalla completa dentro del módulo.
 */
enum class ToolEntry(
    val title: String,
    val description: String
) {
    WarmupCalculator(
        title = "Calculadora de Aproximación",
        description = "Progresión de series de calentamiento y tabla de intensidades (75%–95%) con discos IPF."
    ),
    CustomExercises(
        title = "Ejercicios Personalizados",
        description = "Crea y administra tus propios ejercicios con su tipo de medición."
    )
}

private sealed interface ToolsRoute {
    object Index : ToolsRoute
    data class Detail(val tool: ToolEntry) : ToolsRoute
}

/**
 * Módulo Herramientas: índice de las herramientas secundarias de la app (al estilo del
 * catálogo). El listado permanece visible mientras se navega dentro de cada herramienta.
 */
@Composable
fun ToolsScreen(onBack: () -> Unit) {
    var route by remember { mutableStateOf<ToolsRoute>(ToolsRoute.Index) }

    // Botón atrás nativo: dentro de una herramienta vuelve al índice, desde el índice
    // sale del módulo hacia el Inicio.
    BackHandler(enabled = route !is ToolsRoute.Index) { route = ToolsRoute.Index }
    BackHandler(enabled = route is ToolsRoute.Index) { onBack() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when (val current = route) {
            is ToolsRoute.Index -> ToolsIndexPanel(
                onBack = onBack,
                onOpen = { route = ToolsRoute.Detail(it) }
            )
            is ToolsRoute.Detail -> ToolDetailPanel(
                tool = current.tool,
                onBack = { route = ToolsRoute.Index }
            )
        }
    }
}

@Composable
private fun ToolsIndexPanel(
    onBack: () -> Unit,
    onOpen: (ToolEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "← Volver",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.clickable { onBack() }
        )
        ScreenTitle("Herramientas", Modifier.padding(top = 8.dp))
        ScreenSubtitle(
            "Herramientas secundarias para planificar y registrar tu entrenamiento.",
            Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 104.dp)
        ) {


            ToolEntry.entries.forEach { tool ->
                LifterCard(Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(tool) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = tool.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = tool.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Abrir ${tool.title}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolDetailPanel(tool: ToolEntry, onBack: () -> Unit) {
    // Peso de barra base del perfil para la calculadora de aproximación.
    var baseBarWeightKg by remember { mutableStateOf(20.0) }
    if (tool == ToolEntry.WarmupCalculator) {
        val authRepository = remember { AuthRepository() }
        val profileRepository = remember { ProfileRepository() }
        val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }

        LaunchedEffect(userId) {
            if (userId.isNotEmpty()) {
                val perfil = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
                perfil.onSuccess { profile ->
                    if (profile.baseBarWeightKg > 0) baseBarWeightKg = profile.baseBarWeightKg
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "← Volver a Herramientas",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle(tool.title, Modifier.padding(top = 8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 104.dp)
        ) {
            when (tool) {
                ToolEntry.WarmupCalculator -> WarmupCalculatorPanel(baseBarWeightKg = baseBarWeightKg)
                ToolEntry.CustomExercises -> CustomExercisesPanel()
            }
        }
    }
}
