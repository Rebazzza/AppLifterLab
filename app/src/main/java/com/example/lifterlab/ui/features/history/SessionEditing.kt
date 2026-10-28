package com.example.lifterlab.ui.features.history

import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.ui.features.routines.normalizeRpe
import com.example.lifterlab.ui.features.tools.OneRepMax

/**
 * Reconstruye la sesión a partir de lo editado en el [SessionEditor].
 *
 * RF04 — se conservan TODAS las series de trabajo (ejecutadas y omitidas) numeradas
 * secuencialmente 1..n; la validación de peso/repeticiones solo aplica a las series
 * ejecutadas, porque las omitidas pueden quedar sin datos. RF05 — el RPE se normaliza
 * a la escala @6.0–@10.0 en pasos de 0.5. RF29 — se recalcula el 1RM estimado con
 * Brzycki sobre las series ejecutadas.
 */
fun buildEditedSession(
    original: WorkoutSession,
    exercises: List<RoutineExercise>
): Result<WorkoutSession> {
    val cleaned = mutableListOf<RoutineExercise>()

    for (exercise in exercises) {
        val trabajo = exercise.sets.filter { !it.isWarmup }
        if (trabajo.none { it.isCompleted }) continue

        val sets = trabajo.mapIndexed { index, set ->
            if (set.isCompleted) {
                val label = "Serie ${index + 1} de ${exercise.name}"
                if (set.weightKg <= 0.0 || set.weightKg > OneRepMax.MAX_WEIGHT_KG) {
                    return Result.failure(IllegalArgumentException("$label: el peso debe estar entre 0 y ${OneRepMax.MAX_WEIGHT_KG.toInt()} kg."))
                }
                if (set.reps !in 1..100) {
                    return Result.failure(IllegalArgumentException("$label: las repeticiones deben estar entre 1 y 100."))
                }
            }
            set.copy(
                setNumber = index + 1,
                rpe = normalizeRpe(set.rpe),
                est1RM = if (set.isCompleted && set.reps <= OneRepMax.MAX_REPS) {
                    OneRepMax.brzycki(set.weightKg, set.reps)
                } else {
                    0.0
                }
            )
        }
        cleaned.add(exercise.copy(sets = sets))
    }

    if (cleaned.isEmpty()) {
        return Result.failure(
            IllegalArgumentException("La sesión debe tener al menos una serie completada. Si no la necesitas, elimínala.")
        )
    }

    val ordered = cleaned.mapIndexed { index, exercise -> exercise.copy(executionOrder = index + 1) }
    val volume = ordered.sumOf { ex ->
        ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
    }
    return Result.success(original.copy(exercises = ordered, totalVolumeKg = volume))
}
