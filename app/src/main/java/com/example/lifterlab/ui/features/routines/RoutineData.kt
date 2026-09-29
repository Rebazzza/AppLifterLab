package com.example.lifterlab.ui.features.routines

import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import java.util.Locale

val catalogExercises = listOf(
    Pair("Barbell Squat", "Target: Quads, Glutes"),
    Pair("Bench Press", "Target: Chest, Triceps"),
    Pair("Deadlift", "Target: Back, Hamstrings"),
    Pair("Romanian Deadlift", "Target: Hamstrings, Glutes"),
    Pair("Overhead Press", "Target: Shoulders, Triceps"),
    Pair("Barbell Row", "Target: Lats, Upper Back"),
    Pair("Incline Dumbbell Press", "Target: Upper Chest, Shoulders"),
    Pair("Pull Ups", "Target: Lats, Biceps"),
    Pair("Dips", "Target: Chest, Triceps"),
    Pair("Leg Press", "Target: Quads"),
    Pair("Leg Curl", "Target: Hamstrings"),
    Pair("Calf Raise", "Target: Calves")
)

fun addExercise(routine: Routine, name: String, targetMuscles: String): Routine {
    val exercises = routine.exercises.toMutableList()
    exercises.add(
        RoutineExercise(
            name = name,
            targetMuscles = targetMuscles,
            sets = listOf(
                ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, rpe = 8.0),
                ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 5, rpe = 8.5)
            )
        )
    )
    return routine.copy(exercises = exercises)
}

fun removeExercise(routine: Routine, exIndex: Int): Routine {
    if (exIndex !in routine.exercises.indices) return routine
    val exercises = routine.exercises.toMutableList()
    exercises.removeAt(exIndex)
    return routine.copy(exercises = exercises)
}

fun updateSetData(
    routine: Routine,
    exIndex: Int,
    setIndex: Int,
    weightKg: Double? = null,
    reps: Int? = null,
    rpe: Double? = null,
    isCompleted: Boolean? = null
): Routine {
    val exercises = routine.exercises.toMutableList()
    if (exIndex !in exercises.indices) return routine
    val targetEx = exercises[exIndex]
    val sets = targetEx.sets.toMutableList()
    if (setIndex !in sets.indices) return routine

    val currentSet = sets[setIndex]
    val updatedSet = currentSet.copy(
        weightKg = weightKg ?: currentSet.weightKg,
        reps = reps ?: currentSet.reps,
        rpe = rpe ?: currentSet.rpe,
        isCompleted = isCompleted ?: currentSet.isCompleted
    )
    sets[setIndex] = updatedSet
    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

fun addSetToExercise(routine: Routine, exIndex: Int): Routine {
    val exercises = routine.exercises.toMutableList()
    if (exIndex !in exercises.indices) return routine

    val targetEx = exercises[exIndex]
    val sets = targetEx.sets.toMutableList()
    val lastSet = sets.lastOrNull()

    sets.add(
        ExerciseSet(
            setNumber = sets.size + 1,
            weightKg = lastSet?.weightKg ?: 100.0,
            reps = lastSet?.reps ?: 5,
            rpe = lastSet?.rpe ?: 8.0,
            isCompleted = false
        )
    )
    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

fun calculateVolume(routine: Routine): String {
    var totalVolume = 0.0
    for (ex in routine.exercises) {
        for (set in ex.sets) {
            totalVolume += (set.weightKg * set.reps)
        }
    }
    return if (totalVolume >= 1000) {
        String.format(Locale.ROOT, "%.1fk kg", totalVolume / 1000.0)
    } else {
        String.format(Locale.ROOT, "%.0f kg", totalVolume)
    }
}

fun buildWorkoutSession(routine: Routine, isExpress: Boolean, durationMins: Int): WorkoutSession {
    val exercises = routine.exercises.mapNotNull { ex ->
        val sets = ex.sets.filter { it.weightKg > 0 && it.reps > 0 }
        if (sets.isEmpty()) null else ex.copy(sets = sets)
    }
    val volume = exercises.sumOf { ex -> ex.sets.sumOf { it.weightKg * it.reps } }
    return WorkoutSession(
        routineName = if (isExpress) "Rutina Express" else routine.name,
        totalVolumeKg = volume,
        durationMins = durationMins,
        isFreeSession = isExpress,
        exercises = exercises
    )
}