package com.example.lifterlab.data.model

data class RoutineExercise(
    val exerciseId: String = "", // Optional link to Global Exercise
    val name: String = "",
    val sets: List<ExerciseSet> = emptyList()
)
