package com.example.lifterlab.ui

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Peso sin decimales innecesarios: 185 -> "185", 187.5 -> "187.5". */
fun formatKg(valKg: Double): String {
    return if (valKg % 1.0 == 0.0) {
        valKg.toInt().toString()
    } else {
        String.format(Locale.ROOT, "%.1f", valKg)
    }
}

/** Volumen total con separador de miles: 12345.0 -> "12,345 kg". */
fun formatVolumeKg(volumeKg: Double): String {
    return String.format(Locale.ROOT, "%,.0f kg", volumeKg)
}

/** Etiqueta relativa de la fecha de la sesión: HOY / AYER / HACE N DÍAS / fecha. */
fun formatSessionDate(timestamp: Timestamp): String {
    val sessionDate = timestamp.toDate()
    val daysDiff = daysBetween(startOfDay(Calendar.getInstance()), startOfDay(sessionDate.time))
    return when {
        daysDiff == 0L -> "HOY"
        daysDiff == 1L -> "AYER"
        daysDiff in 2..6 -> "HACE $daysDiff DÍAS"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.ROOT).format(sessionDate).uppercase(Locale.ROOT)
    }
}

/** Fecha corta para listados de historial: 08/03/2026. */
fun formatShortDate(timestamp: Timestamp): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(timestamp.toDate())
}

private fun startOfDay(calendar: Calendar): Calendar =
    (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

private fun startOfDay(millis: Long): Calendar =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

private fun daysBetween(from: Calendar, to: Calendar): Long {
    val diff = from.timeInMillis - to.timeInMillis
    return Math.round(diff / 86_400_000.0)
}

/** Texto de progreso hacia la meta: "72% to 200kg Goal". */
fun calculateGoalPercent(current: Double, goal: Double): String {
    if (goal <= 0) return "0% Goal"
    val percent = (current / goal * 100).toInt().coerceIn(0, 100)
    return "$percent% to ${goal.toInt()}kg Goal"
}

/** Fracción 0f..1f del progreso hacia la meta. */
fun progressFraction(current: Double, goal: Double): Float {
    if (goal <= 0) return 0f
    return (current / goal).toFloat().coerceIn(0f, 1f)
}