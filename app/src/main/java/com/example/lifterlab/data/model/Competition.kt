package com.example.lifterlab.data.model

data class Competition(
    val name: String = "",
    val athleteWeightKg: Double = 0.0,
    val dotsScore: Double = 0.0,
    val attempts: Map<String, List<CompetitionAttempt>> = emptyMap() 
    // Keys should be: "squat", "bench", "deadlift"
)
