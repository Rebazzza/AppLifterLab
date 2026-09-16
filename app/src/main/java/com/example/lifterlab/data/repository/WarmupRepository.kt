package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
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
}
