package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("No se pudo obtener el usuario autenticado.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String, profile: UserProfile): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Error al crear el usuario en Firebase Auth.")
            
            // Persistir perfil del atleta en /users/{userId} cumpliendo aislamiento
            firestore.collection("users")
                .document(user.uid)
                .set(profile)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
