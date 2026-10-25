package com.example.lifterlab.data.model

/**
 * Ejercicio del catálogo global, persistido en Cloud Firestore bajo la ruta
 * `/exercises/{exerciseId}` (colección compartida por todos los atletas).
 *
 * Regla crítica: [gifUrl] almacena ÚNICAMENTE la cadena de texto de la URL pública
 * remota de la demostración (foto JPG del dataset abierto Free-Exercise-DB, o GIF
 * si se incorpora una fuente de animación). Jamás se guardan binarios ni Base64
 * en Firestore.
 */
data class Exercise(
    val exerciseId: String = "",
    val name: String = "",
    val primaryMuscle: String = "",
    val equipment: String = "",
    val isSBD: Boolean = false,
    val gifUrl: String = "",
    val instructions: List<String> = emptyList()
)
