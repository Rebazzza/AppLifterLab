package com.example.lifterlab.ui.features.profile

/**
 * Estado inmutable de la UI del módulo de perfil.
 * Representa todos los posibles estados de la pantalla.
 */
data class ProfileUiState(
    // Campos de datos del atleta (espejo de UserProfile)
    val name: String = "",
    val email: String = "",
    val birthYear: Int = 0,
    val gender: String = "",
    val heightCm: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val baseBarWeightKg: Double = 20.0,

    // Campos de estado de la pantalla
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false
)
