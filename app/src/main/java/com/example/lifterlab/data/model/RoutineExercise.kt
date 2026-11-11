package com.example.lifterlab.data.model

data class RoutineExercise(
    val exerciseId: String = "",
    val name: String = "Sentadilla con Barra",
    val targetMuscles: String = "Objetivo: Cuádriceps, Glúteos",
    /** posición de ejecución (1..n) recalculada al reordenar por arrastre. */
    val executionOrder: Int = 1,
    /** Descanso propio del ejercicio, configurable en pasos de 15 segundos. */
    val restSeconds: Int = 105,
    val sets: List<ExerciseSet> = emptyList()
)
