package com.example.lifterlab.data.model

data class ExerciseSet(
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double = 0.0,
    val est1RM: Double = 0.0,
    val isPR: Boolean = false
)
