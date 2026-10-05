package com.example.lifterlab.ui.features.dashboard

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.ui.calculateGoalPercent
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ChippedTag
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.SbdStatCard
import com.example.lifterlab.ui.components.SecondaryButton
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.formatSessionDate
import com.example.lifterlab.ui.formatVolumeKg
import com.example.lifterlab.ui.progressFraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    onNavigateToRoutines: () -> Unit,
    onNavigateToProfile: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    var profile by remember { mutableStateOf(UserProfile()) }

    // RF04: resumen de la ultima sesion registrada por el atleta en el historial.
    var lastSession by remember { mutableStateOf<WorkoutSession?>(null) }
    var sesionCargada by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        val result = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
        result.onSuccess { profile = it }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) { warmupRepository.getWorkoutSessions(userId, 1) }
            result.onSuccess { sesiones -> lastSession = sesiones.firstOrNull() }
        }
        sesionCargada = true
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
                .padding(bottom = 104.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    ScreenTitle("LIFTERLAB", Modifier.padding(top = 8.dp))
                    Text(
                        text = "CURRENT CYCLE: WEEK ${profile.currentCycleWeek} / ${profile.cyclePhase}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Perfil",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                        .clickable { onNavigateToProfile() }
                )
            }

            SbdStatCard(
                title = "Squat",
                badgeText = "\u2191 +2.5KG",
                badgeNeutral = false,
                weightText = formatKg(profile.squat1RM),
                goalText = calculateGoalPercent(profile.squat1RM, profile.squatGoalKg),
                fraction = progressFraction(profile.squat1RM, profile.squatGoalKg),
                modifier = Modifier.padding(top = 16.dp)
            )
            SbdStatCard(
                title = "Bench",
                badgeText = "-",
                badgeNeutral = true,
                weightText = formatKg(profile.bench1RM),
                goalText = calculateGoalPercent(profile.bench1RM, profile.benchGoalKg),
                fraction = progressFraction(profile.bench1RM, profile.benchGoalKg),
                modifier = Modifier.padding(top = 16.dp)
            )
            SbdStatCard(
                title = "Deadlift",
                badgeText = "\u2191 +5.0KG",
                badgeNeutral = false,
                weightText = formatKg(profile.deadlift1RM),
                goalText = calculateGoalPercent(profile.deadlift1RM, profile.deadliftGoalKg),
                fraction = progressFraction(profile.deadlift1RM, profile.deadliftGoalKg),
                modifier = Modifier.padding(top = 16.dp)
            )

            ConsistencyCard(streakDays = profile.streakDays)

            LastSessionCard(
                session = lastSession,
                isLoading = !sesionCargada,
                onNavigateToRoutines = onNavigateToRoutines
            )

            AlertBanner()
        }
    }
}

@Composable
private fun ConsistencyCard(streakDays: Int) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                CardTitle("Consistency")
                Text(
                    text = "Training Frequency (Last 14 Days)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$streakDays",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = " DAY\nSTREAK",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val activeDaysRow1 = listOf(false, true, true, false, true, false, true)
        val activeDaysRow2 = listOf(false, true, true, false, true, false, true)

        DayRow(activeDaysRow1, dayLabels)
        DayRow(activeDaysRow2, dayLabels)
    }
}

@Composable
private fun DayRow(activeFlags: List<Boolean>, dayLabels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 0 until 7) {
            val isActive = activeFlags.getOrElse(i) { false }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabels[i],
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StatBox(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(22.dp)
                .padding(end = 12.dp)
        )
        Column {
            CardCaption(label)
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun LastSessionCard(
    session: WorkoutSession?,
    isLoading: Boolean,
    onNavigateToRoutines: () -> Unit
) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardTitle("Last Session", Modifier.weight(1f))
            Text(
                text = when {
                    session != null -> formatSessionDate(session.date)
                    isLoading -> "..."
                    else -> "\u2014"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(12.dp))

        if (session == null) {
            CardCaption("SIN SESIONES REGISTRADAS")
            Text(
                text = "Completa un entrenamiento para ver aqu\u00ED el resumen de tu \u00FAltima sesi\u00F3n.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Text(
                text = session.routineName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (session.isFreeSession) "SESI\u00D3N EXPRESS" else "SESI\u00D3N DE RUTINA",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            StatBox(Icons.Filled.FitnessCenter, "TOTAL VOLUME", formatVolumeKg(session.totalVolumeKg))
            Spacer(Modifier.height(8.dp))
            StatBox(Icons.Filled.Schedule, "DURATION", "${session.durationMins} min")
            Spacer(Modifier.height(8.dp))
            StatBox(
                icon = Icons.Filled.List,
                label = "EJERCICIOS / SERIES",
                value = "${session.exercises.size} / ${session.exercises.sumOf { it.sets.size }}"
            )
        }

        Spacer(Modifier.height(14.dp))
        SecondaryButton(
            text = "VIEW LOG \u2192",
            onClick = { onNavigateToRoutines() },
            minHeight = 46.dp
        )
    }
}

@Composable
private fun AlertBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Alerta",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 12.dp)
        )
        Column {
            Text(
                text = "Heavy Squat Day Tomorrow",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Scheduled RPE 9 singles. Prioritize sleep & recovery.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
