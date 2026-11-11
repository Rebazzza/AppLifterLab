package com.example.lifterlab.ui.features.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.customexercises.CustomExercisesPanel
import com.example.lifterlab.ui.features.competition.CompetitionScreen
import com.example.lifterlab.ui.features.routines.RoutineTopBar
import com.example.lifterlab.ui.features.warmup.WarmupCalculatorPanel
import com.example.lifterlab.ui.theme.ColorDeadlift
import com.example.lifterlab.ui.theme.ColorSquat
import com.example.lifterlab.ui.theme.PrimaryContainer
import com.example.lifterlab.ui.theme.SecondaryAccent
import com.example.lifterlab.ui.theme.TertiaryAccent
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
    OneRepMaxCalculator(
        title = "Calculadora de 1RM",
        description = "Estima tu repetición máxima (Brzycki y Epley) y tu tabla de zonas de carga."
    ),
    PlateLoader(
        title = "Carga de Barra IPF",
        description = "Discos IPF por lado con colores oficiales y opción de seguros."
    ),
    CustomExercises(
        title = "Ejercicios Personalizados",
        description = "Crea y administra tus propios ejercicios con su tipo de medición."
    ),
    Competition(
        title = "Competencia",
        description = "Registra los tres intentos de cada levantamiento y calcula tu total SBD y DOTS."
    )
}

private sealed interface ToolsRoute {
    object Index : ToolsRoute
    data class Detail(val tool: ToolEntry) : ToolsRoute
}

/**
 * Módulo Herramientas: índice de las herramientas secundarias de la app (al estilo del
 * catálogo). El listado permanece visible mientras se navega dentro de cada herramienta.
 *
 * @param initialTool herramienta que se abre directamente al entrar (acceso directo desde el
 * Inicio). Si es null, el módulo arranca mostrando el índice.
 */
@Composable
fun ToolsScreen(
    initialTool: ToolEntry? = null,
    onBack: () -> Unit,
    onNavigateToGuide: ()-> Unit
) {
    var route by remember {
        mutableStateOf<ToolsRoute>(
            if (initialTool != null) ToolsRoute.Detail(initialTool) else ToolsRoute.Index
        )
    }

    // Botón atrás nativo: dentro de una herramienta vuelve al índice, desde el índice
    // sale del módulo hacia el Inicio.
    BackHandler(enabled = route !is ToolsRoute.Index) { route = ToolsRoute.Index }
    BackHandler(enabled = route is ToolsRoute.Index) { onBack() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when (val current = route) {
            is ToolsRoute.Index -> ToolsIndexPanel(
                onOpen = { route = ToolsRoute.Detail(it) },
                onNavigateToGuide = onNavigateToGuide,
            )
            is ToolsRoute.Detail -> {
                if (current.tool == ToolEntry.Competition) {
                    CompetitionScreen(onBack = { route = ToolsRoute.Index })
                } else {
                    ToolDetailPanel(tool = current.tool, onBack = { route = ToolsRoute.Index })
                }
            }
        }
    }
}

@Composable
private fun ToolsIndexPanel(
    onOpen: (ToolEntry) -> Unit,
    onNavigateToGuide: ()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Vista raíz del menú: panel de marca sin botón de volver.
        RoutineTopBar()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            ScreenTitle("Herramientas", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Herramientas secundarias para planificar y registrar tu entrenamiento.",
                Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = bottomBarClearance())
            ) {
                ToolEntry.entries.forEach { tool ->
                    ToolCard(tool = tool, onClick = { onOpen(tool) })
                }
                GuideCard(onClick = onNavigateToGuide)
            }
        }
    }
}

@Composable
private fun GuideCard(onClick: () -> Unit) {
    val accent = PrimaryContainer
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.35f)), shape)
            .clickable(onClickLabel = "Abrir guía 1RM", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).background(accent, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null,
                tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text("Guía 1RM", color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Aprende a interpretar y aplicar tu repetición máxima.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = accent, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ToolCard(tool: ToolEntry, onClick: () -> Unit) {
    val accent: Color
    val icon: ImageVector
    when (tool) {
        ToolEntry.WarmupCalculator -> { accent = PrimaryContainer; icon = Icons.Filled.Tune }
        ToolEntry.OneRepMaxCalculator -> { accent = TertiaryAccent; icon = Icons.Filled.Calculate }
        ToolEntry.PlateLoader -> { accent = SecondaryAccent; icon = Icons.Filled.FitnessCenter }
        ToolEntry.CustomExercises -> { accent = ColorSquat; icon = Icons.Filled.ListAlt }
        ToolEntry.Competition -> { accent = ColorDeadlift; icon = Icons.Filled.EmojiEvents }
    }
    val featured = tool == ToolEntry.WarmupCalculator
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(BorderStroke(1.dp, accent.copy(alpha = if (featured) 0.65f else 0.35f)), shape)
            .clickable(onClickLabel = "Abrir ${tool.title}", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (featured) 23.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (featured) 56.dp else 48.dp)
                .background(accent, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(tool.title, color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (featured) 20.sp else 17.sp, fontWeight = FontWeight.Bold,
                lineHeight = if (featured) 24.sp else 21.sp)
            Text(tool.description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = accent, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ToolDetailPanel(tool: ToolEntry, onBack: () -> Unit) {
    // Peso de barra base del perfil para las calculadoras que cargan barra.
    var baseBarWeightKg by remember { mutableStateOf(PlateCalculator.BAR_KG) }
    if (tool == ToolEntry.WarmupCalculator || tool == ToolEntry.PlateLoader) {
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
    ) {
        // Módulo secundario (se abrió desde el índice): sí muestra botón de volver.
        RoutineTopBar(onBack = onBack)

        ScreenTitle(
            text = tool.title,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
        )

        // El contenido de cada herramienta administra su propio scroll: anidar un
        // verticalScroll dentro de otro mide el hijo con altura infinita y rompe Compose.
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
                .padding(bottom = bottomBarClearance())
        ) {
            when (tool) {
                ToolEntry.WarmupCalculator -> WarmupCalculatorPanel(
                    baseBarWeightKg = baseBarWeightKg,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                )
                ToolEntry.OneRepMaxCalculator -> OneRepMaxPanel(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                )
                ToolEntry.PlateLoader -> PlateLoaderPanel(
                    barKg = baseBarWeightKg,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                )
                // Este panel trae su propio scroll y su padding horizontal: solo
                // necesita un alto acotado, que le da el Box que lo contiene.
                ToolEntry.CustomExercises -> CustomExercisesPanel(Modifier.fillMaxSize())
                ToolEntry.Competition -> Unit
            }
        }
    }
}
