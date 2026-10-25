package com.example.lifterlab.ui.features.routines

import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.model.WarmupStep
import java.util.Locale

/**
 * Ejercicios FIJOS del selector de rutinas: no dependen de la BD, así que los
 * básicos (Sentadilla con Barra, Press de Banca, Peso Muerto…) siempre están
 * disponibles para armar rutinas. ExerciseRepository inyecta además esos
 * mismos nombres en `/exercises` (SBD_BASICS) para que también aparezcan en
 * la pantalla de Catálogo con detalle y foto; el selector detecta el nombre
 * exacto y no los duplica.
 */
val basicExercises = listOf(
    Pair("Sentadilla con Barra", "Objetivo: Cuádriceps, Glúteos"),
    Pair("Press de Banca", "Objetivo: Pecho, Tríceps"),
    Pair("Peso Muerto", "Objetivo: Espalda, Isquiotibiales"),
    Pair("Peso Muerto Rumano", "Objetivo: Isquiotibiales, Glúteos"),
    Pair("Press Militar", "Objetivo: Hombros, Tríceps"),
    Pair("Remo con Barra", "Objetivo: Dorsales, Espalda Alta"),
    Pair("Press Inclinado con Mancuernas", "Objetivo: Pecho Superior, Hombros"),
    Pair("Dominadas", "Objetivo: Dorsales, Bíceps"),
    Pair("Fondos", "Objetivo: Pecho, Tríceps"),
    Pair("Prensa de Piernas", "Objetivo: Cuádriceps"),
    Pair("Curl de Piernas", "Objetivo: Isquiotibiales"),
    Pair("Elevación de Talones", "Objetivo: Pantorrillas")
)

fun addExercise(routine: Routine, name: String, targetMuscles: String): Routine {
    val exercises = routine.exercises.toMutableList()
    exercises.add(
        RoutineExercise(
            name = name,
            targetMuscles = targetMuscles,
            executionOrder = exercises.size + 1,
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
    return routine.copy(exercises = exercises.renumberExecutionOrder())
}

/**
 * Reordenamiento de ejercicios (drag-and-drop o flechas Subir/Bajar).
 * Mueve el ejercicio de [fromIndex] a [toIndex] dentro de la rutina o sesión activa y
 * recalcula la numeración de ejecución secuencial (1..n) de todos los ejercicios.
 */
fun moveExercise(routine: Routine, fromIndex: Int, toIndex: Int): Routine {
    if (fromIndex == toIndex) return routine
    if (fromIndex !in routine.exercises.indices || toIndex !in routine.exercises.indices) return routine
    val exercises = routine.exercises.toMutableList()
    val movido = exercises.removeAt(fromIndex)
    exercises.add(toIndex, movido)
    return routine.copy(exercises = exercises.renumberExecutionOrder())
}

/** Recalcula executionOrder (1, 2, 3…) según la posición actual de cada ejercicio. */
private fun List<RoutineExercise>.renumberExecutionOrder(): List<RoutineExercise> =
    mapIndexed { index, exercise ->
        if (exercise.executionOrder == index + 1) exercise
        else exercise.copy(executionOrder = index + 1)
    }

/**
 * Transfiere las series de aproximación calculadas al ejercicio indicado,
 * insertándolas ANTES de las series existentes para respetar el orden de ejecución
 * (calentamiento → trabajo). Las series quedan pendientes de marcar como completadas.
 */
fun applyWarmupPlan(routine: Routine, exIndex: Int, steps: List<WarmupStep>): Routine {
    if (steps.isEmpty()) return routine
    if (exIndex !in routine.exercises.indices) return routine

    val exercises = routine.exercises.toMutableList()
    val targetEx = exercises[exIndex]

    val warmupSets = steps.map { step ->
        ExerciseSet(
            setNumber = 0,
            weightKg = step.weightKg,
            reps = step.reps,
            rpe = step.suggestedRpe ?: 0.0,
            isCompleted = false
        )
    }

    val sets = (warmupSets + targetEx.sets).mapIndexed { index, set ->
        if (set.setNumber == index + 1) set else set.copy(setNumber = index + 1)
    }

    exercises[exIndex] = targetEx.copy(sets = sets)
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
    return formatVolume(totalVolume)
}

/**
 * Volumen en vivo: solo cuenta el tonelaje de las series ya verificadas
 * (peso x repeticiones), que es el que se consolida al cerrar el entrenamiento.
 */
fun calculateCompletedVolume(routine: Routine): String {
    var totalVolume = 0.0
    for (ex in routine.exercises) {
        for (set in ex.sets) {
            if (set.isCompleted) totalVolume += (set.weightKg * set.reps)
        }
    }
    return formatVolume(totalVolume)
}

private fun formatVolume(totalVolume: Double): String {
    return if (totalVolume >= 1000) {
        String.format(Locale.ROOT, "%.1fk kg", totalVolume / 1000.0)
    } else {
        String.format(Locale.ROOT, "%.0f kg", totalVolume)
    }
}

/**
 * Referencia de la última sesión: mapea nombre de ejercicio -> series
 * registradas, para mostrarlas como referencia en las tarjetas del Modo Gimnasio.
 */
fun previousSessionReference(sessions: List<WorkoutSession>): Map<String, List<ExerciseSet>> {
    val lastSession = sessions.firstOrNull { it.exercises.isNotEmpty() } ?: return emptyMap()
    return lastSession.exercises.associate { exercise -> exercise.name to exercise.sets }
}

fun buildWorkoutSession(routine: Routine, isExpress: Boolean, durationMins: Int): WorkoutSession {
    val exercises = routine.exercises.mapNotNull { ex ->
        val sets = ex.sets.filter { it.isCompleted && it.weightKg > 0 }
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