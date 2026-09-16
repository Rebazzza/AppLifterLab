package com.example.lifterlab.ui.features.warmup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.WarmupRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round

class WarmupViewModel(
    private val repository: WarmupRepository = WarmupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarmupUiState())
    val uiState: StateFlow<WarmupUiState> = _uiState.asStateFlow()

    fun updateWeight(weight: String) {
        _uiState.update { it.copy(weight = weight, errorMessage = null, isSavedSuccessfully = false) }
    }

    fun updateReps(reps: String) {
        _uiState.update { it.copy(reps = reps, errorMessage = null, isSavedSuccessfully = false) }
    }

    fun updateExerciseName(name: String) {
        _uiState.update { it.copy(exerciseName = name, isSavedSuccessfully = false) }
    }

    fun calculate1RM() {
        val weight = _uiState.value.weight.toDoubleOrNull()
        val reps = _uiState.value.reps.toIntOrNull()

        if (weight == null || reps == null || weight <= 0 || reps <= 0) {
            _uiState.update { it.copy(errorMessage = "Por favor, ingresa valores válidos para peso y repeticiones.") }
            return
        }

        // Brzycki formula: 1RM = Weight / (1.0278 - 0.0278 * Reps)
        val divisor = 1.0278 - (0.0278 * reps)
        if (divisor <= 0) {
            _uiState.update { it.copy(errorMessage = "Número de repeticiones muy alto para la fórmula.") }
            return
        }

        val estimated1RM = round(weight / divisor * 10) / 10.0 // Round to 1 decimal place
        
        val targetPercentages = listOf(70, 75, 80, 85, 90, 95)
        val calculatedPercentages = targetPercentages.map { percent ->
            val targetWeight = round((estimated1RM * (percent / 100.0)) * 10) / 10.0
            Pair(percent, targetWeight)
        }

        _uiState.update {
            it.copy(
                estimated1RM = estimated1RM,
                percentages = calculatedPercentages,
                errorMessage = null
            )
        }
    }

    fun saveSession(userId: String) {
        val currentState = _uiState.value
        val weight = currentState.weight.toDoubleOrNull()
        val reps = currentState.reps.toIntOrNull()
        val estimated1RM = currentState.estimated1RM

        if (weight == null || reps == null || estimated1RM == null) {
            _uiState.update { it.copy(errorMessage = "Calcula el 1RM primero antes de guardar.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val session = WorkoutSession(
                date = Timestamp.now(),
                routineName = "Sesión Libre de Aproximación",
                totalVolumeKg = weight * reps,
                isFreeSession = true,
                exercises = listOf(
                    RoutineExercise(
                        name = currentState.exerciseName.ifBlank { "Generic Exercise" },
                        sets = listOf(
                            ExerciseSet(
                                weightKg = weight,
                                reps = reps,
                                est1RM = estimated1RM,
                                isPR = false
                            )
                        )
                    )
                )
            )

            val result = repository.saveWorkoutSession(userId, session)
            
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSavedSuccessfully = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Error al guardar la sesión.") }
            }
        }
    }
    
    fun resetState() {
        _uiState.update { WarmupUiState() }
    }
}
