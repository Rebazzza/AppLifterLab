package com.example.lifterlab.data.model

/**
 * RF04 — Sesión de entrenamiento activa.
 *
 * Se genera al iniciar una plantilla de rutina (RF03) y se conserva en el estado local
 * del dispositivo para poder retomarla si la app se cierra a mitad del entrenamiento.
 * Al finalizar se consolida en un [WorkoutSession] que va al historial del usuario.
 */
data class ActiveSession(
    val routineId: String = "",
    val routineName: String = "",
    val isFreeSession: Boolean = false,
    val startedAtMillis: Long = 0L,
    val exercises: List<RoutineExercise> = emptyList()
)