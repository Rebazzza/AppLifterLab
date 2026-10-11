package com.example.lifterlab.data.model

data class Routine(
    val id: String = "",
    val name: String = "Tren Inferior Pesado - A",
    val category: String = "HIPERTROFIA",
    val tags: List<String> = listOf("HIPERTROFIA", "PIERNAS"),
    val estimatedTimeMins: Int = 60,
    val exercises: List<RoutineExercise> = emptyList()
)
