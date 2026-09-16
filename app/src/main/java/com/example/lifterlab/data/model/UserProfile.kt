package com.example.lifterlab.data.model

data class UserProfile(
    val name: String = "",
    val birthYear: Int = 0,
    val gender: String = "",
    val height: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val baseBarWeightKg: Double = 20.0
)
