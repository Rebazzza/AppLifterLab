package com.example.lifterlab.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del módulo de Perfil de Usuario.
 * Gestiona la carga, edición y persistencia del perfil del atleta,
 * además del cierre de sesión.
 */
class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Carga el perfil del atleta desde Firestore y actualiza el estado de la UI.
     * @param userId UID del usuario autenticado en Firebase Auth.
     */
    fun loadProfile(userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Cargar email del usuario autenticado (disponible localmente, sin red)
        val email = repository.getCurrentUserEmail() ?: ""

        viewModelScope.launch {
            val result = repository.getProfile(userId)
            result.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = profile.name,
                        email = email,
                        birthYear = profile.birthYear,
                        gender = profile.gender,
                        heightCm = profile.height,
                        currentWeightKg = profile.currentWeightKg,
                        baseBarWeightKg = profile.baseBarWeightKg
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Error al cargar el perfil."
                    )
                }
            }
        }
    }

    /**
     * Persiste los campos editables del perfil (peso corporal y peso de barra) en Firestore.
     * @param userId UID del usuario autenticado.
     * @param newWeightStr Texto del campo de peso corporal ingresado por el usuario.
     * @param newBarWeightStr Texto del campo de peso de barra ingresado por el usuario.
     */
    fun saveProfileChanges(userId: String, newWeightStr: String, newBarWeightStr: String) {
        val newWeight = newWeightStr.toDoubleOrNull()
        val newBarWeight = newBarWeightStr.toDoubleOrNull()

        if (newWeight == null || newWeight <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Ingresa un peso corporal válido.") }
            return
        }
        if (newBarWeight == null || newBarWeight < 0.0) {
            _uiState.update { it.copy(errorMessage = "Ingresa un peso de barra válido.") }
            return
        }

        val currentState = _uiState.value
        val updatedProfile = UserProfile(
            name = currentState.name,
            birthYear = currentState.birthYear,
            gender = currentState.gender,
            height = currentState.heightCm,
            currentWeightKg = newWeight,
            baseBarWeightKg = newBarWeight
        )

        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSavedSuccessfully = false) }

        viewModelScope.launch {
            val result = repository.updateProfile(userId, updatedProfile)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentWeightKg = newWeight,
                        baseBarWeightKg = newBarWeight,
                        isSavedSuccessfully = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Error al guardar los cambios."
                    )
                }
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual en Firebase Auth.
     * El Fragment debe navegar al loginFragment tras recibir este evento.
     */
    fun signOut() {
        repository.signOut()
        _uiState.update { ProfileUiState() }
    }

    /** Limpia los mensajes transitorios de éxito o error para evitar que se muestren de nuevo. */
    fun resetTransientState() {
        _uiState.update {
            it.copy(errorMessage = null, isSavedSuccessfully = false)
        }
    }
}
