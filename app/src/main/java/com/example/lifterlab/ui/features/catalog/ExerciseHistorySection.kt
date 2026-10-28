package com.example.lifterlab.ui.features.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ChippedTag
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.features.tools.OneRepMax
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.formatShortDate
import com.example.lifterlab.ui.formatVolumeKg
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/** Sesiones que se leen para analizar la progresión de un ejercicio (RF29). */
private const val SESIONES_PARA_ANALISIS = 100

/**
 * RF29 — Ficha técnica con historial del ejercicio.
 *
 * Vista analítica filtrada por un ejercicio del catálogo. Muestra:
 *  - curva de progresión del 1RM estimado (Brzycki) sesión a sesión,
 *  - récord personal (isPR) marcado automáticamente al cerrar cada sesión,
 *  - acumulado de carga: series ejecutadas y tonelaje (peso × repeticiones),
 *  - historial detallado de las últimas sesiones con sus series (peso/reps/RPE),
 *    destacando la serie con mayor peso o mayor volumen,
 *  - gráfico días–peso con el mejor peso por día.
 */
@Composable
fun ExerciseHistorySection(
    exerciseName: String,
    warmupRepository: WarmupRepository = remember { WarmupRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }

    var cargando by remember { mutableStateOf(true) }
    var puntos by remember { mutableStateOf<List<ExerciseHistoryPoint>>(emptyList()) }
    var detalle by remember { mutableStateOf<List<ExerciseSessionDetail>>(emptyList()) }
    var mensajeError by remember { mutableStateOf("") }

    LaunchedEffect(userId, exerciseName) {
        if (userId.isBlank()) {
            cargando = false
            return@LaunchedEffect
        }
        cargando = true
        mensajeError = ""
        val result = withContext(Dispatchers.IO) {
            warmupRepository.getWorkoutSessions(userId, SESIONES_PARA_ANALISIS)
        }
        result
            .onSuccess { sesiones ->
                puntos = buildExerciseHistory(exerciseName, sesiones)
                detalle = buildExerciseSessionDetails(exerciseName, sesiones)
            }
            .onFailure { e -> mensajeError = e.message ?: "No se pudo cargar el historial." }
        cargando = false
    }

    LifterCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardCaption("PROGRESIÓN DEL EJERCICIO", Modifier.weight(1f))
            val records = puntos.sumOf { it.prSets }
            if (records > 0) {
                ChippedTag(
                    "PR × $records",
                    textColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        CardCaption(
            "1RM estimado (Brzycki) y tonelaje acumulado",
            Modifier.padding(top = 4.dp)
        )

        when {
            cargando -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(22.dp)
                )
            }

            mensajeError.isNotEmpty() -> Text(
                text = mensajeError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            puntos.isEmpty() -> CardCaption(
                "Aún no hay sesiones registradas con este ejercicio.",
                Modifier.padding(top = 12.dp)
            )

            else -> ExerciseHistoryMetrics(puntos, detalle)
        }
    }
}

@Composable
private fun ExerciseHistoryMetrics(
    puntos: List<ExerciseHistoryPoint>,
    detalle: List<ExerciseSessionDetail>
) {
    val ultimo = puntos.last()
    val mejor1rm = puntos.maxOf { it.bestEst1RM }
    val mejorPeso = puntos.maxOf { it.bestWeightKg }
    val seriesTotales = puntos.sumOf { it.completedSets }
    val tonelaje = puntos.sumOf { it.volumeKg }

    Spacer(Modifier.height(14.dp))

    Row(Modifier.fillMaxWidth()) {
        HistorialMetric("SESIONES", puntos.size.toString(), Modifier.weight(1f))
        HistorialMetric("SERIES", seriesTotales.toString(), Modifier.weight(1f))
        HistorialMetric("TONELAJE", formatVolumeKg(tonelaje).uppercase(), Modifier.weight(1.3f))
    }

    Spacer(Modifier.height(16.dp))

    if (puntos.size < 2) {
        CardCaption("Completa otra sesión para ver la curva de progresión.", mono = true)
    } else {
        ProgressionChart(puntos)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardCaption(formatShortDate(puntos.first().date), Modifier.weight(1f), mono = true)
            CardCaption("MÁX ${formatKg(mejor1rm)} KG", mono = true)
            Text(
                text = formatShortDate(ultimo.date),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth()) {
        CardCaption("MEJOR 1RM ${formatKg(mejor1rm)} KG", Modifier.weight(1f), mono = true)
        CardCaption("MEJOR PESO ${formatKg(mejorPeso)} KG", mono = true)
    }

    if (detalle.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        CardCaption("HISTORIAL POR SESIÓN", mono = true)
        CardCaption(
            "Serie destacada = mayor peso o mayor volumen (peso × reps).",
            Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(10.dp))
        detalle.forEach { sessionDetail ->
            SessionDetailHistoryCard(sessionDetail)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        CardCaption("PROGRESO DÍAS – PESO", mono = true)
        CardCaption(
            "Mejor peso ejecutado por día (serie completada).",
            Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(10.dp))
        DaysWeightChart(detalle)
    }
}

@Composable
private fun HistorialMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Curva de progresión: un punto por sesión con el mejor 1RM estimado de ese día. */
@Composable
private fun ProgressionChart(puntos: List<ExerciseHistoryPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val recordColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val max = puntos.maxOf { it.bestEst1RM }
    val min = puntos.minOf { it.bestEst1RM }
    val rango = (max - min).takeIf { it > 0.0 } ?: 1.0

    Canvas(
        modifier
            .fillMaxWidth()
            .height(132.dp)
    ) {
        val padV = 10.dp.toPx()
        val alto = size.height - padV * 2

        repeat(3) { i ->
            val y = padV + alto * i / 2f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        val paso = if (puntos.size > 1) size.width / (puntos.size - 1) else size.width
        fun x(i: Int): Float = if (puntos.size > 1) paso * i else size.width / 2f
        fun y(v: Double): Float = padV + alto * (1f - ((v - min) / rango).toFloat())

        for (i in 1 until puntos.size) {
            drawLine(
                color = lineColor,
                start = Offset(x(i - 1), y(puntos[i - 1].bestEst1RM)),
                end = Offset(x(i), y(puntos[i].bestEst1RM)),
                strokeWidth = 2.dp.toPx()
            )
        }

        puntos.forEachIndexed { index, punto ->
            drawCircle(
                color = if (punto.prSets > 0) recordColor else lineColor,
                radius = 3.5.dp.toPx(),
                center = Offset(x(index), y(punto.bestEst1RM))
            )
        }
    }
}

data class ExerciseSetDetail(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double,
    val isCompleted: Boolean,
    val isPR: Boolean
)

data class ExerciseSessionDetail(
    val sessionId: String,
    val date: Timestamp,
    val sets: List<ExerciseSetDetail>,
    val bestWeightKg: Double,
    val bestVolumeKg: Double
)

/** Gráfico días–peso: mejor peso por día de las sesiones del ejercicio. */
@Composable
private fun DaysWeightChart(detalle: List<ExerciseSessionDetail>, modifier: Modifier = Modifier) {
    val puntos = detalle.sortedBy { it.date.seconds }
    if (puntos.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val maxPeso = puntos.maxOf { it.bestWeightKg }
    val minPeso = puntos.minOf { it.bestWeightKg }
    val rango = (maxPeso - minPeso).takeIf { it > 0.0 } ?: 1.0

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            val padV = 10.dp.toPx()
            val alto = size.height - padV * 2

            repeat(3) { i ->
                val y = padV + alto * i / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            val paso = if (puntos.size > 1) size.width / (puntos.size - 1) else size.width
            fun x(i: Int): Float = if (puntos.size > 1) paso * i else size.width / 2f
            fun y(v: Double): Float = padV + alto * (1f - ((v - minPeso) / rango).toFloat())

            for (i in 1 until puntos.size) {
                drawLine(
                    color = lineColor,
                    start = Offset(x(i - 1), y(puntos[i - 1].bestWeightKg)),
                    end = Offset(x(i), y(puntos[i].bestWeightKg)),
                    strokeWidth = 2.dp.toPx()
                )
            }

            puntos.forEachIndexed { index, punto ->
                drawCircle(
                    color = lineColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x(index), y(punto.bestWeightKg))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CardCaption(formatShortDate(puntos.first().date), mono = true)
            CardCaption("MAX ${formatKg(maxPeso)} KG", mono = true)
            CardCaption(formatShortDate(puntos.last().date), mono = true)
        }
    }
}

@Composable
private fun SessionDetailHistoryCard(detail: ExerciseSessionDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardCaption(formatShortDate(detail.date), mono = true, modifier = Modifier.weight(1f))
            CardCaption("MEJOR ${formatKg(detail.bestWeightKg)} KG × ${if (detail.bestVolumeKg >= detail.bestWeightKg * 1.0) "MAX VOLUME" else ""}".trim().ifEmpty { "MEJOR PESO" }, mono = true)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardCaption("SERIE", Modifier.weight(1f), mono = true)
            CardCaption("KG", Modifier.weight(1.2f), mono = true)
            CardCaption("REPS", Modifier.weight(1f), mono = true)
            CardCaption("RPE", Modifier.weight(1f), mono = true)
        }

        detail.sets.forEachIndexed { index, set ->
            val volumen = set.weightKg * set.reps
            val esDestacada = set.weightKg == detail.bestWeightKg || volumen == detail.bestVolumeKg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .background(
                        if (set.isPR) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        else if (esDestacada && set.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${set.setNumber}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (set.weightKg > 0) formatKg(set.weightKg) else "—",
                    color = if (esDestacada && set.isCompleted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (esDestacada && set.isCompleted) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = if (set.reps > 0) "${set.reps}" else "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (set.rpe > 0) "@${formatKg(set.rpe)}" else "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index < detail.sets.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
        }

        if (detail.sets.any { it.isPR }) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PR",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Récord personal en esta sesión",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** Un punto del historial de un ejercicio: resumen de una sesión concreta (RF29). */
data class ExerciseHistoryPoint(
    val sessionId: String,
    val date: Timestamp,
    val bestEst1RM: Double,
    val bestWeightKg: Double,
    val volumeKg: Double,
    val completedSets: Int,
    val prSets: Int
)

/**
 * RF29 — Filtra el historial por un ejercicio del catálogo y resume cada sesión en la que
 * aparece: mejor 1RM estimado, mejor peso, tonelaje y series ejecutadas. Devuelve los
 * puntos en orden cronológico ascendente para dibujar la curva de progresión.
 */
fun buildExerciseHistory(
    exerciseName: String,
    sesiones: List<WorkoutSession>
): List<ExerciseHistoryPoint> {
    val clave = exerciseName.trim()
    if (clave.isEmpty()) return emptyList()

    return sesiones
        .sortedBy { it.date.seconds }
        .mapNotNull { sesion ->
            val ejercicio = sesion.exercises
                .firstOrNull { it.name.equals(clave, ignoreCase = true) }
                ?: return@mapNotNull null

            val ejecutadas = ejercicio.sets.filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
            if (ejecutadas.isEmpty()) return@mapNotNull null

            ExerciseHistoryPoint(
                sessionId = sesion.id,
                date = sesion.date,
                bestEst1RM = ejecutadas.maxOf { it.est1rmEstimado() },
                bestWeightKg = ejecutadas.maxOf { it.weightKg },
                volumeKg = ejecutadas.sumOf { it.weightKg * it.reps },
                completedSets = ejecutadas.size,
                prSets = ejecutadas.count { it.isPR }
            )
        }
}

/**
 * Historial detallado por sesión: todas las series ejecutadas con peso, repeticiones,
 * RPE y marca de PR, ordenado del más reciente al más antiguo para mostrarlo tal como
 * se pide (últimos pesos y series).
 */
fun buildExerciseSessionDetails(
    exerciseName: String,
    sesiones: List<WorkoutSession>
): List<ExerciseSessionDetail> {
    val clave = exerciseName.trim()
    if (clave.isEmpty()) return emptyList()

    return sesiones
        .sortedByDescending { it.date.seconds }
        .mapNotNull { sesion ->
            val ejercicio = sesion.exercises
                .firstOrNull { it.name.equals(clave, ignoreCase = true) }
                ?: return@mapNotNull null

            val ejecutadas = ejercicio.sets
                .filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
                .sortedBy { it.setNumber }

            if (ejecutadas.isEmpty()) return@mapNotNull null

            val bestWeightKg = ejecutadas.maxOf { it.weightKg }
            val bestVolumeKg = ejecutadas.maxOf { it.weightKg * it.reps }

            ExerciseSessionDetail(
                sessionId = sesion.id,
                date = sesion.date,
                sets = ejecutadas.map { set ->
                    ExerciseSetDetail(
                        setNumber = set.setNumber,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        rpe = set.rpe,
                        isCompleted = set.isCompleted,
                        isPR = set.isPR
                    )
                },
                bestWeightKg = bestWeightKg,
                bestVolumeKg = bestVolumeKg
            )
        }
}

/**
 * 1RM de la serie: se usa el valor persistido con Brzycki al cerrar la sesión (RF29) y,
 * para registros anteriores a ese guardado, se recalcula con la misma fórmula.
 */
internal fun ExerciseSet.est1rmEstimado(): Double = when {
    est1RM > 0.0 -> est1RM
    reps in 1..OneRepMax.MAX_REPS -> OneRepMax.brzycki(weightKg, reps)
    else -> 0.0
}
