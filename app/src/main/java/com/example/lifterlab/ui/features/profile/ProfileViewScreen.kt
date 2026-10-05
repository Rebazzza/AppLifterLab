package com.example.lifterlab.ui.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.SbdStatCard
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.SecondaryButton
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.formatSessionDate
import com.example.lifterlab.ui.formatVolumeKg
import com.example.lifterlab.ui.progressFraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HISTORIAL_LIMITE = 10

/**
 * Vista de perfil (solo lectura): foto, nombre de usuario, correo, acceso a la
 * edicion, estadisticas de ejercicios basicos e historial de entrenamientos.
 * La edicion vive en [ProfileEditScreen].
 */
@Composable
fun ProfileViewScreen(
    onNavigateToEdit: () -> Unit,
    onSignOut: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val email = remember { authRepository.getCurrentUser()?.email.orEmpty() }

    var perfil by remember { mutableStateOf<UserProfile?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var sesiones by remember { mutableStateOf<List<WorkoutSession>>(emptyList()) }
    var historialCargando by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isEmpty()) {
            cargando = false
            historialCargando = false
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            .onSuccess { cargando = false; perfil = it }
            .onFailure { cargando = false }

        withContext(Dispatchers.IO) { warmupRepository.getWorkoutSessions(userId, HISTORIAL_LIMITE) }
            .onSuccess { historialCargando = false; sesiones = it }
            .onFailure { historialCargando = false }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 104.dp)
        ) {
            ScreenTitle("Perfil", Modifier.padding(top = 8.dp))

            val current = perfil
            val nombreUsuario = current?.name?.ifEmpty { null } ?: email.substringBefore("@").ifEmpty { null }

            LifterCard(Modifier.padding(top = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        iniciales = inicialesDe(nombreUsuario),
                        modifier = Modifier.size(64.dp)
                    )
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(
                            text = nombreUsuario ?: "Atleta",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = email.ifEmpty { "Sin correo" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (current != null) {
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChippedTag("${current.streakDays} DIA STREAK")
                        ChippedTag("CICLO ${current.currentCycleWeek} - ${current.cyclePhase}")
                    }
                }
            }

            PrimaryButton(
                text = "Editar Perfil",
                onClick = { onNavigateToEdit() },
                modifier = Modifier.padding(top = 16.dp)
            )

            if (cargando) {
                CardCaption("CARGANDO PERFIL", Modifier.padding(top = 24.dp))
            }

            SbdStatCard(
                title = "Squat",
                badgeText = "1RM",
                badgeNeutral = true,
                weightText = formatKg(current?.squat1RM ?: 0.0),
                goalText = calculateGoalPercent(current?.squat1RM ?: 0.0, current?.squatGoalKg ?: 0.0),
                fraction = progressFraction(current?.squat1RM ?: 0.0, current?.squatGoalKg ?: 0.0),
                modifier = Modifier.padding(top = 16.dp)
            )
            SbdStatCard(
                title = "Bench",
                badgeText = "1RM",
                badgeNeutral = true,
                weightText = formatKg(current?.bench1RM ?: 0.0),
                goalText = calculateGoalPercent(current?.bench1RM ?: 0.0, current?.benchGoalKg ?: 0.0),
                fraction = progressFraction(current?.bench1RM ?: 0.0, current?.benchGoalKg ?: 0.0),
                modifier = Modifier.padding(top = 16.dp)
            )
            SbdStatCard(
                title = "Deadlift",
                badgeText = "1RM",
                badgeNeutral = true,
                weightText = formatKg(current?.deadlift1RM ?: 0.0),
                goalText = calculateGoalPercent(current?.deadlift1RM ?: 0.0, current?.deadliftGoalKg ?: 0.0),
                fraction = progressFraction(current?.deadlift1RM ?: 0.0, current?.deadliftGoalKg ?: 0.0),
                modifier = Modifier.padding(top = 16.dp)
            )

            LifterCard(Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardTitle("Historial de Entrenamientos", Modifier.weight(1f))
                    Text(
                        text = if (sesiones.isEmpty()) "" else "${sesiones.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                when {
                    historialCargando -> CardCaption("CARGANDO HISTORIAL", Modifier.padding(top = 16.dp))

                    sesiones.isEmpty() -> {
                        CardCaption("SIN SESIONES REGISTRADAS", Modifier.padding(top = 16.dp))
                        Text(
                            text = "Cuando completes un entrenamiento en el Modo Gimnasio " +
                                "veras aqui el detalle de cada sesion.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    else -> {
                        sesiones.forEachIndexed { index, sesion ->
                            SessionRow(
                                session = sesion,
                                mostrarSeparador = index < sesiones.lastIndex
                            )
                        }
                    }
                }
            }

            SecondaryButton(
                text = "Cerrar Sesión",
                onClick = { onSignOut() },
                modifier = Modifier.padding(top = 20.dp),
                minHeight = 52.dp
            )
        }
    }
}

/** Avatar predeterminado con las iniciales del usuario, centradas en el circulo. */
@Composable
private fun ProfileAvatar(iniciales: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            color = MaterialTheme.colorScheme.primaryContainer,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

/** Iniciales del nombre: "Mauricio Silva" -> "MS". */
private fun inicialesDe(nombre: String?): String {
    if (nombre.isNullOrBlank()) return "LF"
    val partes = nombre.trim().split(" ", "-", "_").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "LF"
        partes.size == 1 -> partes[0].take(2).uppercase()
        else -> (partes.first().take(1) + partes.last().take(1)).uppercase()
    }
}

/** Fila del historial: fecha, rutina, volumen, duracion y ejercicios completados. */
@Composable
private fun SessionRow(session: WorkoutSession, mostrarSeparador: Boolean) {
    Column(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.routineName.ifEmpty { "Sesion libre" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatSessionDate(session.date),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChippedTag(formatVolumeKg(session.totalVolumeKg))
            ChippedTag("${session.durationMins} MIN")
            ChippedTag("${session.exercises.size} EJERCICIOS")
        }

        if (mostrarSeparador) {
            Spacer(Modifier.height(16.dp))
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}