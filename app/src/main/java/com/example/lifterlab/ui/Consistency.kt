package com.example.lifterlab.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Semanas de historial que se leen para calcular la consistencia (una por sesión). */
const val CONSISTENCY_HISTORY_LIMIT = 400

/**
 * Consistencia semanal del atleta.
 *
 * El atleta no entrena todos los días, así que la racha se mide en semanas:
 * una semana cuenta cuando tiene al menos una sesión terminada. Las sesiones
 * terminadas son las únicas que quedan en el historial, por lo que una sesión
 * iniciada y abandonada no suma.
 *
 * Los días se manejan con índice 0 = lunes … 6 = domingo, y cada semana se
 * toma de lunes a domingo en la zona horaria del dispositivo.
 */
data class WeeklyConsistency(
    val streakWeeks: Int = 0,
    val currentWeekDays: Set<Int> = emptySet(),
    val previousWeekDays: Set<Int> = emptySet(),
    val currentWeekSessions: Int = 0,
    val previousWeekSessions: Int = 0
)

/**
 * Calcula la racha semanal y los días entrenados de esta semana y la anterior.
 *
 * La semana en curso todavía no termina: si aún no hay sesiones, la racha de
 * las semanas anteriores se conserva hasta que la semana cierre. Solo se corta
 * cuando una semana completa se queda sin entrenamientos.
 */
fun buildWeeklyConsistency(
    sessionDatesMillis: List<Long>,
    nowMillis: Long = System.currentTimeMillis()
): WeeklyConsistency {
    val currentWeek = startOfWeek(nowMillis)
    val previousWeek = (currentWeek.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -DAYS_PER_WEEK)
    }
    val currentWeekKey = weekKey(currentWeek)
    val previousWeekKey = weekKey(previousWeek)

    val weeksWithSessions = mutableSetOf<String>()
    val currentDays = mutableSetOf<Int>()
    val previousDays = mutableSetOf<Int>()
    var currentSessions = 0
    var previousSessions = 0

    sessionDatesMillis
        // Ignora fechas inválidas o adelantadas por un reloj desajustado.
        .filter { it in 1..nowMillis }
        .forEach { millis ->
            val key = weekKey(startOfWeek(millis))
            weeksWithSessions += key

            when (key) {
                currentWeekKey -> {
                    currentDays += dayIndexMonday(millis)
                    currentSessions++
                }
                previousWeekKey -> {
                    previousDays += dayIndexMonday(millis)
                    previousSessions++
                }
            }
        }

    // La racha se cuenta hacia atrás desde esta semana. Si la semana en curso
    // todavía no tiene sesiones, se arranca desde la semana pasada para no
    // cortar la racha antes de que la semana termine.
    var streak = 0
    var cursor = if (currentWeekKey in weeksWithSessions) currentWeek else previousWeek
    while (weekKey(cursor) in weeksWithSessions) {
        streak++
        cursor = (cursor.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -DAYS_PER_WEEK)
        }
    }

    return WeeklyConsistency(
        streakWeeks = streak,
        currentWeekDays = currentDays,
        previousWeekDays = previousDays,
        currentWeekSessions = currentSessions,
        previousWeekSessions = previousSessions
    )
}

/** Índice del día de la semana según la app: 0 = lunes … 6 = domingo. */
fun dayIndexMonday(millis: Long = System.currentTimeMillis()): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    return (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
}

private const val DAYS_PER_WEEK = 7

private fun startOfWeek(millis: Long): Calendar {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    calendar.add(Calendar.DAY_OF_YEAR, -dayIndexMonday(millis))
    return calendar
}

private fun weekKey(calendar: Calendar): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(calendar.time)
