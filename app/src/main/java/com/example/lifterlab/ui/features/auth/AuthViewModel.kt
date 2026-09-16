package com.example.lifterlab.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isUserLoggedIn = repository.isUserLoggedIn(),
            userId = repository.getCurrentUser()?.uid
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, completa todos los campos.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

        viewModelScope.launch {
            val result = repository.login(trimmedEmail, pass)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isUserLoggedIn = true,
                        userId = user.uid,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = error.localizedMessage ?: "Error al iniciar sesión."
                    )
                }
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        confirmPass: String,
        bodyweightStr: String,
        barWeightStr: String
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, completa los campos requeridos.") }
            return
        }

        if (pass.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        if (pass != confirmPass) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden.") }
            return
        }

        val weight = bodyweightStr.toDoubleOrNull() ?: 0.0
        val barWeight = barWeightStr.toDoubleOrNull() ?: 20.0

        val profile = UserProfile(
            name = trimmedName,
            currentWeightKg = weight,
            baseBarWeightKg = barWeight
        )

        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

        viewModelScope.launch {
            val result = repository.register(trimmedEmail, pass, profile)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isUserLoggedIn = true,
                        userId = user.uid,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = error.localizedMessage ?: "Error al registrar la cuenta."
                    )
                }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.update {
            AuthUiState(
                isUserLoggedIn = false,
                userId = null,
                isSuccess = false
            )
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                isSuccess = false
            )
        }
    }
}
