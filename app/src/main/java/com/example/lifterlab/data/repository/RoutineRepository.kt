package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.Routine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RoutineRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Obtiene todas las plantillas de rutinas guardadas para el usuario autenticado desde /users/{userId}/routines.
     */
    suspend fun getRoutines(userId: String): Result<List<Routine>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("routines")
                .get()
                .await()

            val routines = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Routine::class.java)?.copy(id = doc.id)
            }
            Result.success(routines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Guarda o actualiza una plantilla de rutina en /users/{userId}/routines/{routineId}.
     * Retorna el ID del documento guardado.
     */
    suspend fun saveRoutine(userId: String, routine: Routine): Result<String> {
        return try {
            val routinesRef = firestore
                .collection("users")
                .document(userId)
                .collection("routines")

            val docRef = if (routine.id.isBlank()) {
                routinesRef.document()
            } else {
                routinesRef.document(routine.id)
            }


            val routineToSave = routine.copy(id = docRef.id)
            docRef.set(routineToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una plantilla de rutina de /users/{userId}/routines/{routineId}.
     */
    suspend fun deleteRoutine(userId: String, routineId: String): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("routines")
                .document(routineId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
