package com.example.lifterlab.ui.features.history

import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.ui.features.tools.OneRepMax
fun buildEditedSession(
    original: WorkoutSession,
    exercises: List<RoutineExercise>
): Result<WorkoutSession> {
    val cleaned = mutableListOf<RoutineExercise>()

    for (exercise in exercises) {
        val kept = exercise.sets.filter { it.isCompleted }
        if (kept.isEmpty()) continue

        val sets = kept.mapIndexed { index, set ->
            val label = "Serie ${index + 1} de ${exercise.name}"
            if (set.weightKg <= 0.0 || set.weightKg > OneRepMax.MAX_WEIGHT_KG) {
                return Result.failure(IllegalArgumentException("$label: el peso debe estar entre 0 y ${OneRepMax.MAX_WEIGHT_KG.toInt()} kg."))
            }
            if (set.reps !in 1..100) {
                return Result.failure(IllegalArgumentException("$label: las repeticiones deben estar entre 1 y 100."))
            }
            if (set.rpe != 0.0 && set.rpe !in 1.0..10.0) {
                return Result.failure(IllegalArgumentException("$label: el RPE debe estar entre 1 y 10."))
            }
            set.copy(
                setNumber = index + 1,
                est1RM = if (set.reps <= OneRepMax.MAX_REPS) OneRepMax.brzycki(set.weightKg, set.reps) else 0.0
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
    val volume = ordered.sumOf { ex -> ex.sets.sumOf { it.weightKg * it.reps } }
    return Result.success(original.copy(exercises = ordered, totalVolumeKg = volume))
}
