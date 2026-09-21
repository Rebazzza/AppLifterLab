package com.example.lifterlab.ui.features.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ChippedTag
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.SecondaryButton
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    onNavigateToRoutines: () -> Unit,
    onNavigateToProfile: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    var profile by remember { mutableStateOf(UserProfile()) }

    LaunchedEffect(userId) {
        val result = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
        result.onSuccess { profile = it }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    CardTitle("Dashboard", Modifier)
                    Text(
                        text = "CURRENT CYCLE: WEEK ${profile.currentCycleWeek} / ${profile.cyclePhase}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "👤",
                    fontSize = 20.sp,
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

            SbdCard(
                title = "Squat",
                badgeText = "↑ +2.5KG",
                badgeNeutral = false,
                weightText = formatKg(profile.squat1RM),
                goalText = calculateGoalPercent(profile.squat1RM, profile.squatGoalKg),
                fraction = progressFraction(profile.squat1RM, profile.squatGoalKg)
            )
            SbdCard(
                title = "Bench",
                badgeText = "-",
                badgeNeutral = true,
                weightText = formatKg(profile.bench1RM),
                goalText = calculateGoalPercent(profile.bench1RM, profile.benchGoalKg),
                fraction = progressFraction(profile.bench1RM, profile.benchGoalKg)
            )
            SbdCard(
                title = "Deadlift",
                badgeText = "↑ +5.0KG",
                badgeNeutral = false,
                weightText = formatKg(profile.deadlift1RM),
                goalText = calculateGoalPercent(profile.deadlift1RM, profile.deadliftGoalKg),
                fraction = progressFraction(profile.deadlift1RM, profile.deadliftGoalKg)
            )

            ConsistencyCard(streakDays = profile.streakDays)

            LastSessionCard(onNavigateToRoutines = onNavigateToRoutines)

            AlertBanner()

            BottomNavBar(onNavigateToRoutines = onNavigateToRoutines, onNavigateToProfile = onNavigateToProfile)
        }
    }
}

@Composable
private fun SbdCard(
    title: String,
    badgeText: String,
    badgeNeutral: Boolean,
    weightText: String,
    goalText: String,
    fraction: Float
) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardTitle(title, Modifier.weight(1f))
            val badgeColor =
                if (badgeNeutral) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiary
            val badgeTextColor =
                if (badgeNeutral) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF003824)
            Text(
                text = badgeText,
                modifier = Modifier
                    .background(color = badgeColor, shape = RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, badgeColor), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = badgeTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = weightText,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " KG",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        ProgressBar(fraction)

        Text(
            text = goalText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val clamped = fraction.coerceIn(0f, 1f)
    val fillWeight = clamped.coerceAtLeast(0.001f)
    val emptyWeight = (1f - clamped).coerceAtLeast(0.001f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            Modifier
                .weight(fillWeight)
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Box(
            Modifier
                .weight(emptyWeight)
                .fillMaxSize()
        )
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
private fun StatBox(icon: String, label: String, value: String) {
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
        Text(icon, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
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
private fun LastSessionCard(onNavigateToRoutines: () -> Unit) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardTitle("Last Session", Modifier.weight(1f))
            Text(
                text = "Yesterday",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(12.dp))
        StatBox("🏋", "TOTAL VOLUME", "12,450 kg")
        Spacer(Modifier.height(8.dp))
        StatBox("⏱", "DURATION", "94 min")

        Spacer(Modifier.height(14.dp))
        SecondaryButton(
            text = "VIEW LOG →",
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
        Text("⚠️", fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
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

@Composable
private fun BottomNavBar(onNavigateToRoutines: () -> Unit, onNavigateToProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(CardCornerRadiusForNav())
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        NavItem("Home", "🏠", isActive = true, onClick = {})
        NavItem("Routines", "🏋", isActive = false, onClick = onNavigateToRoutines)
        NavItem("Profile", "👤", isActive = false, onClick = onNavigateToProfile)
    }
}

private fun CardCornerRadiusForNav() = 12.dp

@Composable
private fun RowScope.NavItem(label: String, icon: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 18.sp)
        Text(
            text = label,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun formatKg(valKg: Double): String {
    return if (valKg % 1.0 == 0.0) {
        valKg.toInt().toString()
    } else {
        String.format(Locale.ROOT, "%.1f", valKg)
    }
}

private fun calculateGoalPercent(current: Double, goal: Double): String {
    if (goal <= 0) return "0% Goal"
    val percent = (current / goal * 100).toInt().coerceIn(0, 100)
    return "$percent% to ${goal.toInt()}kg Goal"
}

private fun progressFraction(current: Double, goal: Double): Float {
    if (goal <= 0) return 0f
    return (current / goal).toFloat().coerceIn(0f, 1f)
}