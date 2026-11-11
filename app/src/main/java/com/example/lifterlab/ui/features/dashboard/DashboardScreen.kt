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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.ui.calculateGoalPercent
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.SbdStatCard
import com.example.lifterlab.ui.components.SecondaryButton
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.features.routines.RoutineTopBar
import com.example.lifterlab.ui.features.routines.MovimientoBasico
import com.example.lifterlab.ui.features.routines.mejoresPesosPorMovimiento
import com.example.lifterlab.ui.features.tools.ToolEntry
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.formatSessionDate
import com.example.lifterlab.ui.formatVolumeKg
import com.example.lifterlab.ui.progressFraction
import com.example.lifterlab.ui.CONSISTENCY_HISTORY_LIMIT
import com.example.lifterlab.ui.WeeklyConsistency
import com.example.lifterlab.ui.buildWeeklyConsistency
import com.example.lifterlab.ui.dayIndexMonday
import com.example.lifterlab.ui.theme.ColorBench
import com.example.lifterlab.ui.theme.ColorDeadlift
import com.example.lifterlab.ui.theme.ColorSquat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Historial que alimenta la tarjeta de última sesión y los récords SBD. Se pide el mismo
 * volumen que al cerrar una sesión (RF29) para que el PR del Dashboard coincida con el
 * que marca el Modo Gimnasio.
 */
private const val SESIONES_DASHBOARD = 200

/**
 * Elige la rutina del acceso rápido: una guardada al azar, excluyendo la última
 * rutina que el atleta terminó (las sesiones Express no cuentan como rutina).
 */
internal fun pickQuickStartRoutine(
    routines: List<Routine>,
    ultimaRutinaCompletada: String?,
    random: kotlin.random.Random = kotlin.random.Random.Default
): Routine? {
    val ultima = ultimaRutinaCompletada?.trim()
    return routines
        .filter { it.name.trim() != ultima }
        .randomOrNull(random)
}

@Composable
fun DashboardScreen(
    onNavigateToRoutines: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOneRepMax: () -> Unit,
    onNavigateToPlateLoader: () -> Unit,
    onNavigateToCompetition: () -> Unit,
    onNavigateToTool: (ToolEntry) -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onStartExpress: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() },
    warmupRepository: WarmupRepository = remember { WarmupRepository() },
    routineRepository: RoutineRepository = remember { RoutineRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    var profile by remember { mutableStateOf(UserProfile()) }

    // Indicadores calculados solo con las sesiones consolidadas del historial.
    var recentSessions by remember { mutableStateOf<List<WorkoutSession>>(emptyList()) }
    var weeklyVolumeKg by remember { mutableStateOf(0.0) }
    var sesionCargada by remember { mutableStateOf(false) }

    // Última rutina guardada que se terminó (se ignoran las sesiones Express).
    var ultimaRutinaCompletada by remember { mutableStateOf<String?>(null) }

    // RF25/RF29 — Mejor peso realmente levantado por movimiento básico (SBD), leído del
    // historial. Alimenta el número grande de las tarjetas para que muestre el PR del
    // atleta y no solo el 1RM del perfil (que puede quedar por debajo de lo levantado).
    var recordsSbd by remember { mutableStateOf<Map<MovimientoBasico, Double>>(emptyMap()) }

    // Consistencia semanal: racha de semanas con al menos una sesión terminada y
    // los días entrenados de esta semana y la anterior.
    var consistencia by remember { mutableStateOf(WeeklyConsistency()) }
    var consistenciaCargada by remember { mutableStateOf(false) }

    // Acceso rápido: una rutina guardada al azar, excluyendo la última que se
    // terminó, y siempre con la opción de arrancar la Rutina Express.
    var rutinas by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var rutinasCargadas by remember { mutableStateOf(false) }
    var rutinaSugerida by remember { mutableStateOf<Routine?>(null) }

    LaunchedEffect(userId) {
        val result = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
        result.onSuccess { profile = it }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) {
                warmupRepository.getAllWorkoutSessions(userId)
            }
            result.onSuccess { sesiones ->
                recentSessions = sesiones.take(3)
                weeklyVolumeKg = volumenDeEstaSemana(sesiones)
                recordsSbd = mejoresPesosPorMovimiento(sesiones)
                ultimaRutinaCompletada = sesiones
                    .firstOrNull { !it.isFreeSession }
                    ?.routineName
                    ?.trim()
            }
        }
        sesionCargada = true
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val fechas = withContext(Dispatchers.IO) {
                warmupRepository.getSessionDates(userId, CONSISTENCY_HISTORY_LIMIT)
            }
            fechas.onSuccess { consistencia = buildWeeklyConsistency(it) }
        }
        consistenciaCargada = true
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
            result.onSuccess { rutinas = it }
        }
        rutinasCargadas = true
    }

    // Se espera a tener las rutinas y el historial para no sugerir justamente
    // la rutina guardada que el atleta acaba de terminar.
    LaunchedEffect(rutinas, ultimaRutinaCompletada, sesionCargada, rutinasCargadas) {
        if (!rutinasCargadas || !sesionCargada) return@LaunchedEffect
        rutinaSugerida = pickQuickStartRoutine(rutinas, ultimaRutinaCompletada)
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Vista raíz del menú: panel de marca sin botón de volver.
            RoutineTopBar()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
                    .padding(bottom = bottomBarClearance())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "CICLO ACTUAL: SEMANA ${profile.currentCycleWeek} / ${profile.cyclePhase}",
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

            // RF18/RF29 — El número grande proviene exclusivamente de series reales
            // consolidadas en el historial, nunca de un 1RM manual del perfil.
            val sentadillaPr = recordsSbd[MovimientoBasico.SENTADILLA] ?: 0.0
            val bancaPr = recordsSbd[MovimientoBasico.PRESS_BANCA] ?: 0.0
            val muertoPr = recordsSbd[MovimientoBasico.PESO_MUERTO] ?: 0.0
            val sentadillaPeso = sentadillaPr
            val bancaPeso = bancaPr
            val muertoPeso = muertoPr

            SbdStatCard(
                title = "Sentadilla",
                badgeText = if (sentadillaPr > 0) "\u2605 PR" else "SIN REGISTRO",
                badgeNeutral = sentadillaPr <= 0,
                weightText = formatKg(sentadillaPeso),
                goalText = calculateGoalPercent(sentadillaPeso, profile.squatGoalKg),
                fraction = progressFraction(sentadillaPeso, profile.squatGoalKg),
                modifier = Modifier.padding(top = 16.dp),
                accentColor = ColorSquat
            )
            SbdStatCard(
                title = "Press Banca",
                badgeText = if (bancaPr > 0) "\u2605 PR" else "SIN REGISTRO",
                badgeNeutral = bancaPr <= 0,
                weightText = formatKg(bancaPeso),
                goalText = calculateGoalPercent(bancaPeso, profile.benchGoalKg),
                fraction = progressFraction(bancaPeso, profile.benchGoalKg),
                modifier = Modifier.padding(top = 16.dp),
                accentColor = ColorBench
            )
            SbdStatCard(
                title = "Peso Muerto",
                badgeText = if (muertoPr > 0) "\u2605 PR" else "SIN REGISTRO",
                badgeNeutral = muertoPr <= 0,
                weightText = formatKg(muertoPeso),
                goalText = calculateGoalPercent(muertoPeso, profile.deadliftGoalKg),
                fraction = progressFraction(muertoPeso, profile.deadliftGoalKg),
                modifier = Modifier.padding(top = 16.dp),
                accentColor = ColorDeadlift
            )

            ConsistencyCard(
                consistency = consistencia,
                isLoading = !consistenciaCargada,
                todayIndex = dayIndexMonday()
            )

            WeeklyVolumeCard(
                volumeKg = weeklyVolumeKg,
                isLoading = !sesionCargada
            )

            RecentSessionsCard(
                sessions = recentSessions,
                isLoading = !sesionCargada,
                onNavigateToHistory = onNavigateToHistory
            )

            NextSessionCard(
                routine = rutinaSugerida,
                isLoading = !rutinasCargadas || !sesionCargada,
                hasRoutines = rutinas.isNotEmpty(),
                onStartRoutine = onStartRoutine,
                onStartExpress = onStartExpress
            )

            ToolsCard(
                onNavigateToOneRepMax = onNavigateToOneRepMax,
                onNavigateToPlateLoader = onNavigateToPlateLoader,
                onNavigateToCompetition = onNavigateToCompetition,
                onNavigateToTool = onNavigateToTool
            )
            }
        }
    }
}

@Composable
private fun ConsistencyCard(
    consistency: WeeklyConsistency,
    isLoading: Boolean,
    todayIndex: Int
) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                CardTitle("Consistencia")
                Text(
                    text = "Semanas con al menos una sesión terminada (lun–dom)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLoading) "—" else "${consistency.streakWeeks}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = " SEMANAS\nDE RACHA",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        WeekRowHeader(
            label = "ESTA SEMANA",
            sessions = consistency.currentWeekSessions,
            isLoading = isLoading
        )
        DayRow(
            activeDays = consistency.currentWeekDays,
            todayIndex = todayIndex
        )

        WeekRowHeader(
            label = "SEMANA PASADA",
            sessions = consistency.previousWeekSessions,
            isLoading = isLoading
        )
        DayRow(
            activeDays = consistency.previousWeekDays,
            todayIndex = -1
        )

        if (!isLoading && consistency.currentWeekSessions == 0 && consistency.previousWeekSessions == 0) {
            Text(
                text = "Completa una sesión para empezar tu racha semanal.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun WeekRowHeader(label: String, sessions: Int, isLoading: Boolean) {
    val count = when {
        isLoading -> "CARGANDO..."
        sessions == 1 -> "1 SESIÓN"
        else -> "$sessions SESIONES"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CardCaption(label, mono = true)
        CardCaption(count, mono = true)
    }
}

@Composable
private fun DayRow(activeDays: Set<Int>, todayIndex: Int) {
    val dayLabels = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in dayLabels.indices) {
            val isActive = i in activeDays
            val isToday = i == todayIndex
            val textColor = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isActive || isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabels[i],
                    color = textColor,
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
private fun WeeklyVolumeCard(volumeKg: Double, isLoading: Boolean) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        CardTitle("Volumen semanal")
        Text(
            text = if (isLoading) "…" else formatVolumeKg(volumeKg),
            color = MaterialTheme.colorScheme.primaryContainer,
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp)
        )
        CardCaption("Carga completada desde el lunes hasta hoy", Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RecentSessionsCard(
    sessions: List<WorkoutSession>,
    isLoading: Boolean,
    onNavigateToHistory: () -> Unit
) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CardTitle("Últimas sesiones", Modifier.weight(1f))
            CardCaption(if (isLoading) "…" else "${sessions.size} RECIENTES", mono = true)
        }
        Spacer(Modifier.height(12.dp))
        if (sessions.isEmpty()) {
            CardCaption("SIN SESIONES REGISTRADAS")
            Text(
                text = "Completa un entrenamiento para ver aquí tu actividad reciente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            sessions.forEachIndexed { index, session ->
                RecentSessionRow(session)
                if (index < sessions.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        SecondaryButton(text = "VER REGISTRO →", onClick = onNavigateToHistory, minHeight = 46.dp)
    }
}

@Composable
private fun RecentSessionRow(session: WorkoutSession) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(session.routineName, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "${formatSessionDate(session.date)} · ${if (session.isFreeSession) "EXPRESS" else "RUTINA"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatVolumeKg(session.totalVolumeKg), color = MaterialTheme.colorScheme.primaryContainer, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Text("${session.durationMins} min", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Volumen ejecutado desde el lunes a las 00:00, en la zona horaria local. */
internal fun volumenDeEstaSemana(sessions: List<WorkoutSession>, nowMillis: Long = System.currentTimeMillis()): Double {
    val start = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return sessions.filter { it.date.toDate().time >= start }.sumOf { it.totalVolumeKg }
}

@Composable
private fun ToolsCard(
    onNavigateToOneRepMax: () -> Unit,
    onNavigateToPlateLoader: () -> Unit,
    onNavigateToCompetition: () -> Unit,
    onNavigateToTool: (ToolEntry) -> Unit
) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        CardTitle("Herramientas")
        Text(
            text = "Calcula tu 1RM, arma la barra, prepara tus aproximaciones y administra tus ejercicios.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )
        SecondaryButton(
            text = "CALCULADORA 1RM →",
            onClick = onNavigateToOneRepMax,
            minHeight = 46.dp
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "CARGA DE BARRA IPF →",
            onClick = onNavigateToPlateLoader,
            minHeight = 46.dp
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "CALCULADORA DE APROXIMACIÓN →",
            onClick = { onNavigateToTool(ToolEntry.WarmupCalculator) },
            minHeight = 46.dp
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "EJERCICIOS PERSONALIZADOS →",
            onClick = { onNavigateToTool(ToolEntry.CustomExercises) },
            minHeight = 46.dp
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "COMPETENCIA →",
            onClick = onNavigateToCompetition,
            minHeight = 46.dp
        )
    }
}

@Composable
private fun NextSessionCard(
    routine: Routine?,
    isLoading: Boolean,
    hasRoutines: Boolean,
    onStartRoutine: (Routine) -> Unit,
    onStartExpress: () -> Unit
) {
    LifterCard(Modifier.padding(top = 16.dp), featured = true) {
        Text(
            text = "Próxima Sesión",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Acceso rápido a una rutina al azar, distinta a la última que completaste.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(14.dp))

        when {
            isLoading -> {
                CardCaption("BUSCANDO RUTINA...", mono = true)
            }
            routine != null -> {
                Text(
                    text = routine.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                CardCaption(
                    text = "${routine.exercises.size} EJERCICIOS • ${routine.estimatedTimeMins} MIN",
                    modifier = Modifier.padding(top = 4.dp),
                    mono = true
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    text = "INICIAR ESTA RUTINA",
                    onClick = { onStartRoutine(routine) }
                )
            }
            else -> {
                Text(
                    text = if (hasRoutines) {
                        "No hay otra rutina distinta a la última que completaste. Crea una nueva o entrena libre."
                    } else {
                        "Aún no tienes rutinas guardadas. Crea una desde Rutinas o entrena libre."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "INICIAR RUTINA EXPRESS",
            onClick = onStartExpress,
            minHeight = 46.dp
        )

    }
}
