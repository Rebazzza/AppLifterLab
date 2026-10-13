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
     * la sesión anterior en el Modo Gimnasio.
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
                doc.toObject(WorkoutSession::class.java)?.copy(id = doc.id)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * RF17 — Sobrescribe una sesión finalizada en /users/{userId}/workout_sessions/{sessionId}
     * con los pesos, repeticiones, RPE y ejercicios corregidos.
     */
    suspend fun updateWorkoutSession(userId: String, session: WorkoutSession): Result<Unit> {
        return try {
            require(session.id.isNotBlank()) { "La sesión no tiene identificador." }
            firestore.collection("users")
                .document(userId)
                .collection("workout_sessions")
                .document(session.id)
                .set(session)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** RF17 — Elimina una sesión del historial. */
    suspend fun deleteWorkoutSession(userId: String, sessionId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("workout_sessions")
                .document(sessionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}