package com.example.lifterlab.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object RestTimerState {
    var isRunning by mutableStateOf(false)
        private set
    var targetTimeMillis by mutableLongStateOf(0L)
        private set
    var exerciseName by mutableStateOf("")
        private set

    fun start(exercise: String, seconds: Int) {
        val secs = seconds.coerceAtLeast(0)
        exerciseName = exercise
        if (secs <= 0) {
            clear()
            return
        }
        targetTimeMillis = System.currentTimeMillis() + (secs * 1000L)
        isRunning = true
    }

    fun adjust(deltaSeconds: Int) {
        if (!isRunning) return
        val currentRemaining = ((targetTimeMillis - System.currentTimeMillis()) / 1000L).toInt()
        val newRemaining = (currentRemaining + deltaSeconds).coerceAtLeast(0)
        if (newRemaining <= 0) {
            clear()
            return
        }
        targetTimeMillis = System.currentTimeMillis() + (newRemaining * 1000L)
    }

    fun skip() {
        clear()
    }

    fun clear() {
        isRunning = false
        targetTimeMillis = 0L
        exerciseName = ""
    }

    fun remainingSeconds(): Int {
        if (!isRunning || targetTimeMillis <= 0L) return 0
        val diff = targetTimeMillis - System.currentTimeMillis()
        val secs = (diff / 1000L).toInt()
        return if (secs <= 0) 0 else secs
    }
}
