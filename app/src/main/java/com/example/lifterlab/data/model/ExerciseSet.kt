package com.example.lifterlab.data.model

data class ExerciseSet(
    val setNumber: Int = 1,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double = 0.0,
    val est1RM: Double = 0.0,
    // IMPORTANTE (Firestore): estos booleanos deben ser `var`, no `val`.
    // Kotlin expone el getter `isCompleted()` y Firestore deriva el campo
    // "completed"; al deserializar necesita un setter/field para ese nombre. Con
    // `val` no existe setter y el campo real se llama `isCompleted`, así que el
    // valor volvía siempre en `false` (por eso el récord personal nunca se
    // detectaba al leer el historial). Con `var` Kotlin genera `setCompleted()`
    // y el valor se conserva. No cambiar a `val`.
    var isPR: Boolean = false,
    var isCompleted: Boolean = false,
    /**
     * Serie de aproximación (calentamiento) generada por la calculadora. Solo existe
     * durante la sesión: no se refleja en la plantilla de la rutina cuando se termina
     * el entrenamiento.
     */
    var isWarmup: Boolean = false
)
