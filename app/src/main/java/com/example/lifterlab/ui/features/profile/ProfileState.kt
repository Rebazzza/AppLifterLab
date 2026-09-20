package com.example.lifterlab.ui.features.profile

data class ProfileState(
    var name: String = "",
    var email: String = "",
    var birthYear: Int = 0,
    var gender: String = "",
    var heightCm: Double = 0.0,
    var currentWeightKg: Double = 0.0,
    var baseBarWeightKg: Double = 20.0,
    var isLoading: Boolean = false,
    var errorMessage: String? = null,
    var isSavedSuccessfully: Boolean = false
)
