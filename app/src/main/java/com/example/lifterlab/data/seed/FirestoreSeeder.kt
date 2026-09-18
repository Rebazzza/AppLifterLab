package com.example.lifterlab.data.seed

import com.example.lifterlab.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper de Inicialización de Base de Datos (DatabaseSeeder)
 * Encargado de poblar la base de datos Firestore con datos ficticios iniciales (mock data).
 * Esto permite forzar la creación de colecciones y subcolecciones en la consola de Firebase.
 *
 * Este seeder es IDEMPOTENTE: verifica si el documento del usuario ya existe antes de
 * escribir cualquier dato, evitando duplicados en cada inicio de sesión.
 */
class FirestoreSeeder(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Siembra los datos iniciales para el usuario autenticado y el catálogo global.
     * Solo ejecuta la escritura si el documento /users/{userId} NO existe aún.
     *
     * @param userId El ID único del usuario autenticado (request.auth.uid).
     * @return Result<Boolean> — true si se sembró, false si ya existían datos (sin error).
     */
    suspend fun seedInitialData(userId: String): Result<Boolean> {
        return try {
            val db = firestore

            // ==========================================
            // GUARDIA DE IDEMPOTENCIA
            // Si el documento del usuario ya existe, no sembrar de nuevo.
            // ==========================================
            val userSnapshot = db.collection("users").document(userId).get().await()
            if (userSnapshot.exists()) {
                return Result.success(false) // Datos ya inicializados, omitir seed
            }

            // ==========================================
            // B. Perfil del Atleta (Aislamiento OBLIGATORIO)
            // Ruta base: /users/{userId}
            // ==========================================
            val userRef = db.collection("users").document(userId)

            // 1. Documento de Perfil Básico
            val profile = UserProfile(
                name = "Nuevo Atleta",
                currentWeightKg = 0.0,
                baseBarWeightKg = 20.0
            )
            userRef.set(profile).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
