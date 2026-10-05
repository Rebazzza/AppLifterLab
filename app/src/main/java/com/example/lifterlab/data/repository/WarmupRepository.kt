package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class WarmupRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun saveWorkoutSession(userId: String, session: WorkoutSession): Result<Boolean> {
        return try {
            val sessionsRef = firestore.collection("users")
                .document(userId)
                .collection("workout_sessions")
            
            sessionsRef.add(session).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Historial de sesiones del usuario en /users/{userId}/workout_sessions, de la más
     * reciente a la más antigua. Alimenta el historial (RF) y la referencia de pesos de
     * la sesión anterior en el Modo Gimnasio (RF04).
     */
    suspend fun getWorkoutSessions(userId: String, limit: Int = 5): Result<List<WorkoutSession>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("workout_sessions")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            Result.success(snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutSession::class.java)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}