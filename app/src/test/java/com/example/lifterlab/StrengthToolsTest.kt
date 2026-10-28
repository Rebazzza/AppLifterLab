package com.example.lifterlab

import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.ui.features.catalog.buildExerciseHistory
import com.example.lifterlab.ui.features.history.buildEditedSession
<<<<<<< HEAD
import com.example.lifterlab.ui.features.routines.buildWorkoutSession
import com.example.lifterlab.ui.features.routines.MovimientoBasico
import com.example.lifterlab.ui.features.routines.marcasHistoricas
import com.example.lifterlab.ui.features.routines.mejoresPesosPorMovimiento
import com.example.lifterlab.ui.features.routines.movimientoBasicoDe
import com.example.lifterlab.ui.features.routines.normalizeRpe
=======
import com.example.lifterlab.ui.features.routines.formatRestTimerSeconds
>>>>>>> e5450248619abb3d252085665faf1290bb2681f4
import com.example.lifterlab.ui.features.tools.IpfPlate
import com.example.lifterlab.ui.features.tools.OneRepMax
import com.example.lifterlab.ui.features.tools.PlateCalculator
import com.google.firebase.Timestamp
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
    fun editedSession_recalculatesVolumeAndKeepsOmittedSets() {
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
        // RF04 — la omitida se conserva en el registro pero no suma tonelaje.
        assertEquals(500.0, edited.totalVolumeKg, 1e-9)
        val sets = edited.exercises.single().sets
        assertEquals(2, sets.size)
        assertEquals(listOf(1, 2), sets.map { it.setNumber })
        assertFalse(sets[1].isCompleted)
    }

    @Test
    fun editedSession_normalizesRpeAndEstimatesOneRm() {
        val exercises = listOf(
            RoutineExercise(
                name = "Sentadilla con Barra",
                sets = listOf(
                    ExerciseSet(
                        setNumber = 4,
                        weightKg = 120.0,
                        reps = 5,
                        rpe = 7.3,
                        isCompleted = true
                    )
                )
            )
        )
        val edited = buildEditedSession(WorkoutSession(id = "s1"), exercises).getOrThrow()
        val set = edited.exercises.single().sets.single()
        // RF05 — @7.3 se normaliza al paso de 0.5 más cercano.
        assertEquals(7.5, set.rpe, 1e-9)
        // RF04 — numeración secuencial 1..n dentro del ejercicio.
        assertEquals(1, set.setNumber)
        // RF29 — 1RM estimado con Brzycki persistido en la serie.
        assertEquals(OneRepMax.brzycki(120.0, 5), set.est1RM, 1e-9)
    }

    @Test
    fun editedSession_rejectsInvalidData() {
        val bad = listOf(
            RoutineExercise(name = "X", sets = listOf(ExerciseSet(weightKg = 0.0, reps = 5, isCompleted = true)))
        )
        assertTrue(buildEditedSession(WorkoutSession(id = "s1"), bad).isFailure)
        assertTrue(buildEditedSession(WorkoutSession(id = "s1"), emptyList()).isFailure)
    }

    @Test
<<<<<<< HEAD
    fun rpe_isNormalizedToHalfPointScale() {
        assertEquals(0.0, normalizeRpe(0.0), 1e-9)
        assertEquals(6.0, normalizeRpe(3.0), 1e-9)
        assertEquals(10.0, normalizeRpe(12.0), 1e-9)
        assertEquals(7.5, normalizeRpe(7.3), 1e-9)
        assertEquals(8.0, normalizeRpe(8.24), 1e-9)
        assertEquals(9.5, normalizeRpe(9.5), 1e-9)
    }

    @Test
    fun workoutSession_keepsOmittedSetsAndSkipsWarmups() {
        val routine = Routine(
            id = "r1",
            name = "Tren Inferior",
            exercises = listOf(
                RoutineExercise(
                    name = "Sentadilla con Barra",
                    sets = listOf(
                        ExerciseSet(setNumber = 1, weightKg = 60.0, reps = 5, isWarmup = true, isCompleted = true),
                        ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 5, isCompleted = true),
                        ExerciseSet(setNumber = 3, weightKg = 100.0, reps = 5, isCompleted = false)
                    )
                ),
                RoutineExercise(
                    name = "Peso Muerto",
                    sets = listOf(
                        ExerciseSet(setNumber = 1, weightKg = 140.0, reps = 3, isCompleted = false)
                    )
                )
            )
        )

        val session = buildWorkoutSession(routine, isExpress = false, durationMins = 50)

        // El ejercicio sin ninguna serie ejecutada no entra en el historial.
        assertEquals(1, session.exercises.size)
        val sets = session.exercises.single().sets
        // La aproximación no se guarda y la numeración vuelve a empezar en 1.
        assertEquals(2, sets.size)
        assertEquals(listOf(1, 2), sets.map { it.setNumber })
        assertFalse(sets.any { it.isWarmup })
        assertTrue(sets[0].isCompleted)
        assertFalse(sets[1].isCompleted)
        // El tonelaje solo suma la serie ejecutada.
        assertEquals(500.0, session.totalVolumeKg, 1e-9)
        // RF29 — 1RM de Brzycki por serie ejecutada.
        assertEquals(OneRepMax.brzycki(100.0, 5), sets[0].est1RM, 1e-9)
        assertEquals(0.0, sets[1].est1RM, 1e-9)
        // Sin historial previo no hay récord que superar.
        assertFalse(sets.any { it.isPR })
    }

    @Test
    fun workoutSession_marksPersonalRecordsAgainstHistory() {
        val previa = WorkoutSession(
            id = "s0",
            exercises = listOf(
                RoutineExercise(
                    name = "Press de Banca",
                    sets = listOf(
                        ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, isCompleted = true),
                        ExerciseSet(setNumber = 2, weightKg = 90.0, reps = 8, isCompleted = true)
                    )
                )
            )
        )
        val marcas = marcasHistoricas(listOf(previa))
        assertEquals(100.0, marcas.getValue("Press de Banca").maxWeightKg, 1e-9)
        // 90 kg × 8 reps es la serie con más tonelaje (720 kg).
        assertEquals(720.0, marcas.getValue("Press de Banca").maxSetVolumeKg, 1e-9)

        val routine = Routine(
            id = "r1",
            name = "Empuje",
            exercises = listOf(
                RoutineExercise(
                    name = "Press de Banca",
                    sets = listOf(
                        // Igual peso, más repeticiones: supera el tonelaje de serie → PR.
                        ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 8, isCompleted = true),
                        // Mismo peso y repeticiones que el récord: no lo supera.
                        ExerciseSet(setNumber = 2, weightKg = 90.0, reps = 8, isCompleted = true),
                        // Más peso que el récord → PR.
                        ExerciseSet(setNumber = 3, weightKg = 105.0, reps = 1, isCompleted = true)
                    )
                )
            )
        )

        val session = buildWorkoutSession(routine, isExpress = false, durationMins = 40, records = marcas)
        val sets = session.exercises.single().sets
        assertTrue(sets[0].isPR)
        assertFalse(sets[1].isPR)
        assertTrue(sets[2].isPR)
    }

    @Test
    fun exerciseHistory_summarizesEachSessionChronologically() {
        val vieja = WorkoutSession(
            id = "s1",
            date = Timestamp(1_700_000_000, 0),
            exercises = listOf(
                RoutineExercise(
                    name = "Sentadilla con Barra",
                    sets = listOf(
                        ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, isCompleted = true, est1RM = 112.5),
                        ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 5, isCompleted = false)
                    )
                )
            )
        )
        val nueva = WorkoutSession(
            id = "s2",
            date = Timestamp(1_700_100_000, 0),
            exercises = listOf(
                RoutineExercise(
                    name = "Sentadilla con Barra",
                    sets = listOf(
                        ExerciseSet(
                            setNumber = 1,
                            weightKg = 120.0,
                            reps = 5,
                            isCompleted = true,
                            isPR = true
                        )
                    )
                ),
                RoutineExercise(name = "Press de Banca", sets = emptyList())
            )
        )

        val puntos = buildExerciseHistory("Sentadilla con Barra", listOf(nueva, vieja))

        assertEquals(2, puntos.size)
        // Orden cronológico ascendente, aunque el repositorio devuelva lo más nuevo primero.
        assertEquals("s1", puntos[0].sessionId)
        assertEquals("s2", puntos[1].sessionId)
        assertEquals(1, puntos[0].completedSets)
        assertEquals(500.0, puntos[0].volumeKg, 1e-9)
        assertEquals(112.5, puntos[0].bestEst1RM, 1e-9)
        // Sin est1RM persistido (registros antiguos) se recalcula con Brzycki.
        assertEquals(OneRepMax.brzycki(120.0, 5), puntos[1].bestEst1RM, 1e-9)
        assertEquals(1, puntos[1].prSets)
        assertEquals(120.0, puntos[1].bestWeightKg, 1e-9)
    }

    @Test
    fun movimientoBasicoDe_reconoceNombresEnEspanolEIngles() {
        assertEquals(MovimientoBasico.SENTADILLA, movimientoBasicoDe("Sentadilla con Barra"))
        assertEquals(MovimientoBasico.SENTADILLA, movimientoBasicoDe("Barbell Squat"))
        assertEquals(MovimientoBasico.PRESS_BANCA, movimientoBasicoDe("Press de Banca"))
        assertEquals(MovimientoBasico.PRESS_BANCA, movimientoBasicoDe("Bench Press"))
        assertEquals(MovimientoBasico.PESO_MUERTO, movimientoBasicoDe("Peso Muerto"))
        assertEquals(MovimientoBasico.PESO_MUERTO, movimientoBasicoDe("Deadlift"))
        // Un accesorio no pertenece a ningún básico.
        assertNull(movimientoBasicoDe("Remo con Barra"))
    }

    @Test
    fun mejoresPesosPorMovimiento_tomaElMaximoHistoricoEjecutado() {
        val sesiones = listOf(
            WorkoutSession(
                exercises = listOf(
                    RoutineExercise(
                        name = "Press de Banca",
                        sets = listOf(
                            ExerciseSet(weightKg = 100.0, reps = 5, isCompleted = true),
                            // Es el peso más alto del historial y debe ser el PR.
                            ExerciseSet(weightKg = 200.0, reps = 5, isCompleted = true),
                            // Serie omitida: no cuenta aunque tenga más peso.
                            ExerciseSet(weightKg = 500.0, reps = 5, isCompleted = false)
                        )
                    ),
                    RoutineExercise(
                        name = "Sentadilla con Barra",
                        sets = listOf(
                            ExerciseSet(weightKg = 180.0, reps = 3, isCompleted = true)
                        )
                    )
                )
            )
        )

        val marcas = mejoresPesosPorMovimiento(sesiones)

        assertEquals(200.0, marcas.getValue(MovimientoBasico.PRESS_BANCA), 1e-9)
        assertEquals(180.0, marcas.getValue(MovimientoBasico.SENTADILLA), 1e-9)
        // Sin registros del movimiento, no aparece en el mapa.
        assertNull(marcas[MovimientoBasico.PESO_MUERTO])
=======
    fun restTimer_formatter() {
        assertEquals("01:45", formatRestTimerSeconds(105))
        assertEquals("00:00", formatRestTimerSeconds(0))
        assertEquals("00:00", formatRestTimerSeconds(-5))
        assertEquals("02:30", formatRestTimerSeconds(150))
>>>>>>> e5450248619abb3d252085665faf1290bb2681f4
    }
}
