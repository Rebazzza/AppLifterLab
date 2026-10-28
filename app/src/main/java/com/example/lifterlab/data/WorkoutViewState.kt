package com.example.lifterlab.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object WorkoutViewState {
    var isInWorkoutRunner by mutableStateOf(false)
        private set

    fun enter() {
        isInWorkoutRunner = true
    }

    fun leave() {
        isInWorkoutRunner = false
    }
}
