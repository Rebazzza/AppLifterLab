package com.example.lifterlab

import com.example.lifterlab.ui.buildWeeklyConsistency
import com.example.lifterlab.ui.dayIndexMonday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ConsistencyTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    // Miércoles 11 de marzo de 2026: su semana va del lunes 9 al domingo 15.
    private val now = millis(2026, Calendar.MARCH, 11)

    @Test
    fun consistency_marcaDiasDeEstaSemanaYDeLaAnterior() {
        val result = buildWeeklyConsistency(
            sessionDatesMillis = listOf(
                millis(2026, Calendar.MARCH, 9),   // lunes de esta semana
                millis(2026, Calendar.MARCH, 11),  // miércoles de esta semana
                millis(2026, Calendar.MARCH, 3),   // martes de la semana pasada
                millis(2026, Calendar.MARCH, 8),   // domingo de la semana pasada
                millis(2026, Calendar.FEBRUARY, 25) // dos semanas atrás
            ),
            nowMillis = now
        )

        assertEquals(setOf(0, 2), result.currentWeekDays)
        assertEquals(setOf(1, 6), result.previousWeekDays)
        assertEquals(2, result.currentWeekSessions)
        assertEquals(2, result.previousWeekSessions)
        assertEquals(3, result.streakWeeks)
    }

    @Test
    fun consistency_cuentaSemanasConsecutivasNoDias() {
        val result = buildWeeklyConsistency(
            sessionDatesMillis = listOf(
                millis(2026, Calendar.MARCH, 11),
                millis(2026, Calendar.MARCH, 4),
                millis(2026, Calendar.FEBRUARY, 25),
                millis(2026, Calendar.FEBRUARY, 18)
            ),
            nowMillis = now
        )

        // Cuatro semanas seguidas con entrenamiento, aunque haya días sin ir.
        assertEquals(4, result.streakWeeks)
    }

    @Test
    fun consistency_conservaLaRachaSiLaSemanaEnCursoAunNoTieneSesiones() {
        val result = buildWeeklyConsistency(
            sessionDatesMillis = listOf(
                millis(2026, Calendar.MARCH, 4),
                millis(2026, Calendar.FEBRUARY, 25)
            ),
            nowMillis = now
        )

        assertTrue(result.currentWeekDays.isEmpty())
        assertEquals(0, result.currentWeekSessions)
        assertEquals(2, result.streakWeeks)
    }

    @Test
    fun consistency_cortaLaRachaCuandoUnaSemanaCompletaQuedaVacia() {
        val result = buildWeeklyConsistency(
            sessionDatesMillis = listOf(
                millis(2026, Calendar.FEBRUARY, 25)
            ),
            nowMillis = now
        )

        // La semana pasada (2 al 8 de marzo) no tuvo sesiones.
        assertEquals(0, result.streakWeeks)
    }

    @Test
    fun consistency_ignoraFechasFuturas() {
        val result = buildWeeklyConsistency(
            sessionDatesMillis = listOf(
                millis(2026, Calendar.MARCH, 18)
            ),
            nowMillis = now
        )

        assertEquals(0, result.streakWeeks)
        assertTrue(result.currentWeekDays.isEmpty())
    }

    @Test
    fun dayIndex_empiezaEnLunesYTerminaEnDomingo() {
        assertEquals(0, dayIndexMonday(millis(2026, Calendar.MARCH, 9)))
        assertEquals(6, dayIndexMonday(millis(2026, Calendar.MARCH, 15)))
    }
}
