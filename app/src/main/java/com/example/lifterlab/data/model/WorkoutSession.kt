package com.example.lifterlab.data.model

import com.google.firebase.Timestamp

data class WorkoutSession(
    val date: Timestamp = Timestamp.now(),
    val routineName: String = "",
    val totalVolumeKg: Double = 0.0,
    val durationMins: Int = 94,
    val isFreeSession: Boolean = false,
    val exercises: List<RoutineExercise> = emptyList()
)
