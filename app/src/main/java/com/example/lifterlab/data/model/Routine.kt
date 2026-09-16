package com.example.lifterlab.data.model

data class Routine(
    val name: String = "",
    val category: String = "",
    val exercises: List<RoutineExercise> = emptyList()
)
