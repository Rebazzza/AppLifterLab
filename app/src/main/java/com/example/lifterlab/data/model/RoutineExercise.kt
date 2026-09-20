package com.example.lifterlab.data.model

data class RoutineExercise(
    val exerciseId: String = "",
    val name: String = "Barbell Squat",
    val targetMuscles: String = "Target: Quads, Glutes",
    val sets: List<ExerciseSet> = emptyList()
)
