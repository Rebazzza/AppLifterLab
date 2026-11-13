package com.example.lifterlab.ui.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.ui.WeeklyConsistency
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.routines.MovimientoBasico
import com.example.lifterlab.ui.features.tools.ToolEntry
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.theme.PrimaryAccent

private val Ink = Color(0xFF171B2C)
private val Muted = Color(0xFF60667A)
private val Pale = Color(0xFFEEF0FF)
private val Canvas = Color(0xFFF8F9FD)
private val Outline = Color(0xFFE5E8F2)

private val dashboardColors = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryAccent,
    onPrimaryContainer = Color.White,
    secondaryContainer = Pale,
    onSecondaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Pale,
    onSurfaceVariant = Muted,
    outline = Outline,
    outlineVariant = Outline
)

@Composable
internal fun DashboardHome(
    profile: UserProfile,
    routine: Routine?,
    routinesLoaded: Boolean,
    consistency: WeeklyConsistency,
    consistencyLoaded: Boolean,
    records: Map<MovimientoBasico, Double>,
    weeklyVolumeKg: Double,
    sessionsLoaded: Boolean,
    recentSessions: List<WorkoutSession>,
    onNavigateToRoutines: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOneRepMax: () -> Unit,
    onNavigateToPlateLoader: () -> Unit,
    onNavigateToCompetition: () -> Unit,
    onNavigateToTool: (ToolEntry) -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onStartExpress: () -> Unit
) {
    MaterialTheme(colorScheme = dashboardColors) {
        Surface(color = Canvas, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(26.dp))
                    Text("LifterLab", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp).weight(1f))
                    Surface(
                        onClick = onNavigateToProfile,
                        shape = RoundedCornerShape(14.dp),
                        color = Pale
                    ) {
                        Icon(Icons.Filled.PersonOutline, contentDescription = "Abrir perfil", tint = PrimaryAccent,
                            modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = bottomBarClearance())
                ) {
                    val firstName = profile.name.trim().substringBefore(' ').takeIf { it.isNotBlank() }
                    Text(if (firstName == null) "¡Hola!" else "¡Hola, $firstName!",
                        color = Ink, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Listo para seguir progresando.", color = Muted, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp))

                    Button(
                        onClick = { if (routine != null) onStartRoutine(routine) else onStartExpress() },
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                        Text("Entrenar", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }

                    SuggestedRoutine(routine, routinesLoaded, onStartRoutine, onNavigateToRoutines, onStartExpress)
                    HomeConsistency(consistency, consistencyLoaded)
                    HomeRecords(records)

                    // Las funciones adicionales siguen accesibles al deslizar la pantalla.
                    WeeklyVolumeCard(weeklyVolumeKg, !sessionsLoaded)
                    RecentSessionsCard(recentSessions, !sessionsLoaded, onNavigateToHistory)
                    ToolsCard(onNavigateToOneRepMax, onNavigateToPlateLoader, onNavigateToCompetition, onNavigateToTool)
                }
            }
        }
    }
}

@Composable
private fun SuggestedRoutine(
    routine: Routine?,
    loaded: Boolean,
    onStartRoutine: (Routine) -> Unit,
    onNavigateToRoutines: () -> Unit,
    onStartExpress: () -> Unit
) {
    val title = when {
        !loaded -> "Buscando tu rutina…"
        routine != null -> routine.name
        else -> "Entrenamiento libre"
    }
    val detail = when {
        !loaded -> "Preparando tu próxima sesión"
        routine != null -> "${routine.exercises.size} ejercicios · ${routine.estimatedTimeMins} min"
        else -> "Empieza una rutina express"
    }
    Surface(
        onClick = {
            when {
                routine != null -> onStartRoutine(routine)
                loaded -> onStartExpress()
                else -> onNavigateToRoutines()
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        color = PrimaryAccent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RUTINA SUGERIDA", color = Color.White.copy(alpha = 0.84f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                Text(detail, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun HomeConsistency(consistency: WeeklyConsistency, loaded: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Consistencia semanal", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(if (loaded) "${consistency.currentWeekDays.size} días" else "…",
            color = PrimaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { index, day ->
            val active = index in consistency.currentWeekDays
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day, color = if (active) PrimaryAccent else Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Box(
                    Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp)
                        .background(if (active) PrimaryAccent else Pale, RoundedCornerShape(4.dp))
                )
            }
        }
    }
    if (loaded && consistency.currentWeekDays.isEmpty()) {
        Text("Tu semana empieza con la próxima sesión.", color = Muted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun HomeRecords(records: Map<MovimientoBasico, Double>) {
    Text("Récords personales", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 34.dp, bottom = 14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Sentadilla" to MovimientoBasico.SENTADILLA,
            "Press banca" to MovimientoBasico.PRESS_BANCA,
            "Peso muerto" to MovimientoBasico.PESO_MUERTO
        ).forEach { (label, movement) ->
            val value = records[movement] ?: 0.0
            Surface(
                modifier = Modifier.weight(1f).height(102.dp),
                shape = RoundedCornerShape(14.dp),
                color = Pale
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                    Text(label, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (value > 0) "${formatKg(value)} kg" else "—",
                        color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
