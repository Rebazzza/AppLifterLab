package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para la gestión del perfil del atleta.
 * Todas las rutas se construyen desde /users/{userId} para cumplir
 * el aislamiento de datos por usuario (request.auth.uid == userId).
 */
class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Retorna el email del usuario actualmente autenticado, o null si no hay sesión. */
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    /**
     * Obtiene el [UserProfile] del atleta desde Firestore.
     * @return [Result.success] con el perfil, o [Result.failure] si no existe o hay error de red.
     */
    suspend fun getProfile(userId: String): Result<UserProfile> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .get()
                .await()

            val profile = snapshot.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Perfil no encontrado para el usuario $userId"))

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza los campos configurables del perfil del atleta en Firestore.
     * Solo persiste los campos que el usuario puede modificar desde la pantalla de perfil.
     * @return [Result.success] si la operación es exitosa, [Result.failure] en caso contrario.
     */
    suspend fun updateProfile(userId: String, profile: UserProfile): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Cierra la sesión del usuario actual en Firebase Auth. */
    fun signOut() {
        auth.signOut()
    }
}
