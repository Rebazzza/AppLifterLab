package com.example.lifterlab.data.repository

import android.util.Log
import com.example.lifterlab.data.model.Competition
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Repositorio del módulo de competencia (RF09/RF10).
 *
 * RNF01 — Persistencia dual (offline-first): la competencia se encola primero en el
 * dispositivo y se sincroniza sola cuando vuelve la red, en
 * /users/{userId}/competitions/{competitionId}.
 */
class CompetitionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * RF09 — Guarda una competencia con sus intentos y el puntaje DOTS. El ID del
     * documento se genera antes de escribir para que la ruta quede estable sin red.
     */
    suspend fun saveCompetition(userId: String, competition: Competition): Result<String> {
        return try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("competitions")
                .document()

            docRef.set(competition.copy(id = docRef.id))
                .addOnFailureListener { e ->
                    Log.e(TAG, "No se pudo sincronizar la competencia ${docRef.id}: ${e.message}")
                }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Competencias del usuario, de la más reciente a la más antigua. Si no hay red
     * responde con lo que haya en la caché local del dispositivo.
     */
    suspend fun getCompetitions(userId: String, limit: Int = 50): Result<List<Competition>> {
        val query = firestore
            .collection("users")
            .document(userId)
            .collection("competitions")
            .orderBy("dotsScore", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        return try {
            val snapshot = try {
                query.get().await()
            } catch (e: Exception) {
                query.get(Source.CACHE).await()
            }

            Result.success(snapshot.documents.mapNotNull { doc ->
                doc.toObject(Competition::class.java)?.copy(id = doc.id)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "CompetitionRepository"
    }
}
