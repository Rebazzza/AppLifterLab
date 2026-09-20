package com.example.lifterlab.ui.features.warmup

data class WarmupState(
    var weight: String = "",
    var reps: String = "",
    var exerciseName: String = "Generic Exercise",
    var estimated1RM: Double? = null,
    var percentages: List<Pair<Int, Double>> = emptyList(),
    var isLoading: Boolean = false,
    var errorMessage: String? = null,
    var isSavedSuccessfully: Boolean = false
)
