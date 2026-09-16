package com.example.lifterlab.ui.features.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val userId: String? = null
)
