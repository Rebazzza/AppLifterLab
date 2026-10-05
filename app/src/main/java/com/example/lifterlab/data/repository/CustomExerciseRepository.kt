package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.CustomExercise
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * RF20 — Repositorio de ejercicios personalizados.
 *
 * Persistencia: subcolección privada /users/{userId}/custom_exercises/{exerciseId}.
 * Firestore mantiene su caché offline por defecto, por lo que cada ejercicio queda
 * almacenado de manera local (dispositivo) y en la nube (Firestore).
 */
class CustomExerciseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Obtiene los ejercicios personalizados del usuario, más recientes primero.
     */
    suspend fun getCustomExercises(userId: String): Result<List<CustomExercise>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("custom_exercises")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val exercises = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CustomExercise::class.java)?.copy(id = doc.id)
            }
            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Guarda o actualiza un ejercicio personalizado en
     * /users/{userId}/custom_exercises/{exerciseId}. Retorna el ID del documento.
     */
    suspend fun saveCustomExercise(userId: String, exercise: CustomExercise): Result<String> {
        return try {
            val exercisesRef = firestore
                .collection("users")
                .document(userId)
                .collection("custom_exercises")

            val docRef = if (exercise.id.isBlank()) {
                exercisesRef.document()
            } else {
                exercisesRef.document(exercise.id)
            }

            docRef.set(exercise.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un ejercicio personalizado de /users/{userId}/custom_exercises/{exerciseId}.
     */
    suspend fun deleteCustomExercise(userId: String, exerciseId: String): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("custom_exercises")
                .document(exerciseId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
