package com.example.lifterlab.ui.features.warmup

data class WarmupUiState(
    val weight: String = "",
    val reps: String = "",
    val exerciseName: String = "Generic Exercise",
    val estimated1RM: Double? = null,
    val percentages: List<Pair<Int, Double>> = emptyList(), // Pair of (Percentage, Weight)
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)
