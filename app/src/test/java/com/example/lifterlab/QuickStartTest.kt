package com.example.lifterlab

import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.ui.features.dashboard.pickQuickStartRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuickStartTest {

    private val routines = listOf(
        Routine(id = "1", name = "Torso A"),
        Routine(id = "2", name = "Pierna B"),
        Routine(id = "3", name = "Empuje C")
    )

    @Test
    fun quickStart_nuncaEligeLaUltimaRutinaCompletada() {
        repeat(50) { seed ->
            val elegida = pickQuickStartRoutine(routines, "Torso A", Random(seed))
            assertNotEquals("Torso A", elegida?.name)
        }
    }

    @Test
    fun quickStart_sinHistorialEligeUnaRutinaGuardada() {
        val elegida = pickQuickStartRoutine(routines, null, Random(1))
        assertTrue(elegida in routines)
    }

    @Test
    fun quickStart_siSoloQuedaLaUltimaRutinaNoSugiereNada() {
        val unica = listOf(Routine(id = "1", name = "Torso A"))
        assertNull(pickQuickStartRoutine(unica, "Torso A", Random(1)))
    }

    @Test
    fun quickStart_ignoraEspaciosAlCompararNombres() {
        val elegida = pickQuickStartRoutine(
            routines = listOf(
                Routine(id = "1", name = " Torso A "),
                Routine(id = "2", name = "Pierna B")
            ),
            ultimaRutinaCompletada = "Torso A",
            random = Random(1)
        )
        assertEquals("Pierna B", elegida?.name)
    }
}
