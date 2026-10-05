package com.example.lifterlab.data.model

/**
 * RF20 — Ejercicio personalizado dado de alta por el atleta.
 * Se persiste en la subcolección privada /users/{userId}/custom_exercises/{exerciseId}.
 */
data class CustomExercise(
    val id: String = "",
    val name: String = "",
    val primaryMuscle: String = "",
    val measurementType: String = MEASUREMENT_WEIGHT_REPS,
    val createdAt: Long = 0L
) {
    companion object {
        const val MEASUREMENT_WEIGHT_REPS = "PESO_Y_REPS"
        const val MEASUREMENT_REPS_ONLY = "SOLO_REPS"
        const val MEASUREMENT_TIME = "TIEMPO"

        val MUSCLE_CATEGORIES = listOf(
            "Cuádriceps",
            "Glúteos",
            "Isquiotibiales",
            "Pecho",
            "Espalda",
            "Hombros",
            "Bíceps",
            "Tríceps",
            "Core",
            "Gemelos"
        )

        val MEASUREMENT_OPTIONS = listOf(
            MEASUREMENT_WEIGHT_REPS to "Peso y Repeticiones",
            MEASUREMENT_REPS_ONLY to "Solo Repeticiones (Peso corporal)",
            MEASUREMENT_TIME to "Tiempo"
        )

        fun labelOf(measurementType: String): String =
            MEASUREMENT_OPTIONS.firstOrNull { it.first == measurementType }?.second ?: measurementType
    }
}
