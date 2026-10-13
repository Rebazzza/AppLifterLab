package com.example.lifterlab

import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.ui.features.history.buildEditedSession
import com.example.lifterlab.ui.features.tools.IpfPlate
import com.example.lifterlab.ui.features.tools.OneRepMax
import com.example.lifterlab.ui.features.tools.PlateCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthToolsTest {

    @Test
    fun oneRm_formulas() {
        assertEquals(100.0, OneRepMax.brzycki(100.0, 1), 1e-9)
        assertEquals(100.0, OneRepMax.epley(100.0, 1), 1e-9)
        assertEquals(100.0 * 36 / 32, OneRepMax.brzycki(100.0, 5), 1e-9)
        assertEquals(100.0 * (1 + 5 / 30.0), OneRepMax.epley(100.0, 5), 1e-9)
    }

    @Test
    fun oneRm_validation() {
        assertNull(OneRepMax.validate(100.0, 5))
        assertNotNull(OneRepMax.validate(0.0, 5))
        assertNotNull(OneRepMax.validate(null, 5))
        assertNotNull(OneRepMax.validate(100.0, 0))
        assertNotNull(OneRepMax.validate(100.0, 13))
        assertNotNull(OneRepMax.validate(5000.0, 5))
    }

    @Test
    fun plates_exactLoad() {
        val load = PlateCalculator.compute(140.0, withCollars = false)
        assertTrue(load.isExact)
        assertEquals(listOf(IpfPlate.RED_25, IpfPlate.RED_25, IpfPlate.GREEN_10), load.perSide)
        assertEquals(140.0, load.loadedKg, 1e-9)
    }

    @Test
    fun plates_withCollars() {
        val load = PlateCalculator.compute(105.0, withCollars = true)
        assertTrue(load.isExact)
        assertEquals(105.0, load.loadedKg, 1e-9)
        assertEquals(40.0, load.perSide.sumOf { it.kg }, 1e-9)
    }

    @Test
    fun plates_notLoadable() {
        val load = PlateCalculator.compute(102.0, withCollars = false)
        assertFalse(load.isExact)
        assertEquals(100.0, load.nearestLowerKg!!, 1e-9)
        assertEquals(102.5, load.nearestUpperKg!!, 1e-9)
    }

    @Test
    fun plates_belowBar() {
        val load = PlateCalculator.compute(10.0, withCollars = false)
        assertFalse(load.isExact)
        assertNull(load.nearestLowerKg)
        assertEquals(20.0, load.nearestUpperKg!!, 1e-9)
        assertTrue(load.perSide.isEmpty())
    }

    @Test
    fun editedSession_recalculatesVolumeAndDropsPendingSets() {
        val original = WorkoutSession(id = "s1", totalVolumeKg = 1.0)
        val exercises = listOf(
            RoutineExercise(
                name = "Press de Banca",
                sets = listOf(
                    ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, isCompleted = true),
                    ExerciseSet(setNumber = 2, weightKg = 90.0, reps = 5, isCompleted = false)
                )
            )
        )
        val edited = buildEditedSession(original, exercises).getOrThrow()
        assertEquals(500.0, edited.totalVolumeKg, 1e-9)
        assertEquals(1, edited.exercises.single().sets.size)
    }

    @Test
    fun editedSession_rejectsInvalidData() {
        val bad = listOf(
            RoutineExercise(name = "X", sets = listOf(ExerciseSet(weightKg = 0.0, reps = 5, isCompleted = true)))
        )
        assertTrue(buildEditedSession(WorkoutSession(id = "s1"), bad).isFailure)
        assertTrue(buildEditedSession(WorkoutSession(id = "s1"), emptyList()).isFailure)
    }
}
