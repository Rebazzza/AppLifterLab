package com.example.lifterlab.data.model

data class UserProfile(
    val name: String = "",
    val birthYear: Int = 0 ,
    val gender: String = "",
    val height: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val baseBarWeightKg: Double = 20.0,
    val squat1RM: Double = 185.0,
    val bench1RM: Double = 120.0,
    val deadlift1RM: Double = 225.0,
    val squatGoalKg: Double = 200.0,
    val benchGoalKg: Double = 130.0,
    val deadliftGoalKg: Double = 235.0,
    val streakDays: Int = 4,
    val currentCycleWeek: Int = 4,
    val cyclePhase: String = "PEAK"
)
