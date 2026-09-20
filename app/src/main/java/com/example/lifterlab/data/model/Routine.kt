package com.example.lifterlab.data.model

data class Routine(
    val id: String = "",
    val name: String = "Heavy Lower Body - A",
    val category: String = "HYPERTROPHY",
    val tags: List<String> = listOf("HYPERTROPHY", "LEGS"),
    val estimatedTimeMins: Int = 60,
    val exercises: List<RoutineExercise> = emptyList()
)
