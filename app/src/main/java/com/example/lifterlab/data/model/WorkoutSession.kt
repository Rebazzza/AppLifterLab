package com.example.lifterlab.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class WorkoutSession(
    /** RF17 — ID del documento en Firestore; no se persiste dentro del documento. */
    @get:Exclude val id: String = "",
    val date: Timestamp = Timestamp.now(),
    val routineName: String = "",
    val totalVolumeKg: Double = 0.0,
    val durationMins: Int = 94,
    // `var` a propósito: ver la nota de Firestore en ExerciseSet.isCompleted.
    var isFreeSession: Boolean = false,
    val exercises: List<RoutineExercise> = emptyList()
)
