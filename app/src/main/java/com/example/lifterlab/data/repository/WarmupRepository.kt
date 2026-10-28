package com.example.lifterlab.data.repository

import android.util.Log
import com.example.lifterlab.data.model.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de sesiones de entrenamiento.
 *
 * RNF03 / RNF01 — Persistencia dual (offline-first): toda escritura se encola primero en
 * el dispositivo y recién después viaja a la nube. El SDK de Firestore deja la mutación
 * en su caché local del teléfono de forma inmediata, así que un entrenamiento terminado
 * sin conectividad **no se pierde** y se sincroniza silenciosamente en /users/{userId}/
 * workout_sessions/{sessionId} en cuanto vuelve la red. Por eso ninguna escritura espera
 * a la respuesta del servidor (de lo contrario, sin red, la pantalla quedaría colgada).
 *
 * Lecturas: se pide primero al servidor y, si no hay conexión, se cae a la caché local.
 */
class WarmupRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * RNF03 — Guarda una sesión finalizada. El ID del documento se genera ANTES de
     * escribir para que la ruta del historial sea estable aunque no haya red.
     * Devuelve éxito en cuanto la escritura queda encolada en el dispositivo.
     */
    suspend fun saveWorkoutSession(userId: String, session: WorkoutSession): Result<Boolean> {
        return try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("workout_sessions")
                .document()

            docRef.set(session.copy(id = docRef.id))
                .addOnFailureListener { e ->
                    Log.e(TAG, "No se pudo sincronizar la sesión ${docRef.id}: ${e.message}")
                }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Historial de sesiones del usuario en /users/{userId}/workout_sessions, de la más
     * reciente a la más antigua. Alimenta el historial (RF), la referencia de pesos de la
     * sesión anterior en el Modo Gimnasio y los récords del RF29. Si no hay red, responde
     * con lo que haya en la caché local del dispositivo.
     */
    suspend fun getWorkoutSessions(userId: String, limit: Int = 5): Result<List<WorkoutSession>> {
        val query = firestore
            .collection("users")
            .document(userId)
            .collection("workout_sessions")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        return try {
            val snapshot = try {
                query.get().await()
            } catch (e: Exception) {
                // Sin conectividad: se sirve desde la caché local del dispositivo.
                query.get(Source.CACHE).await()
            }

            Result.success(snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutSession::class.java)?.copy(id = doc.id)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fechas de las sesiones terminadas del usuario, de la más reciente a la más antigua.
     *
     * Alimenta la tarjeta de Consistencia para calcular la racha semanal. Igual que
     * [getWorkoutSessions], si no hay red responde con la caché local del dispositivo.
     */
    suspend fun getSessionDates(userId: String, limit: Int = 400): Result<List<Long>> {
        val query = firestore
            .collection("users")
            .document(userId)
            .collection("workout_sessions")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        return try {
            val snapshot = try {
                query.get().await()
            } catch (e: Exception) {
                query.get(Source.CACHE).await()
            }

            Result.success(
                snapshot.documents.mapNotNull { doc ->
                    doc.getTimestamp("date")?.toDate()?.time
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * RF17 — Sobrescribe una sesión finalizada en /users/{userId}/workout_sessions/{sessionId}
     * con los pesos, repeticiones, RPE y ejercicios corregidos. Offline-first: la corrección
     * se aplica en el dispositivo y se sincroniza sola cuando haya red.
     */
    suspend fun updateWorkoutSession(userId: String, session: WorkoutSession): Result<Unit> {
        return try {
            require(session.id.isNotBlank()) { "La sesión no tiene identificador." }
            firestore.collection("users")
                .document(userId)
                .collection("workout_sessions")
                .document(session.id)
                .set(session)
                .addOnFailureListener { e ->
                    Log.e(TAG, "No se pudo sincronizar la sesión ${session.id}: ${e.message}")
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** RF17 — Elimina una sesión del historial (también encola localmente si no hay red). */
    suspend fun deleteWorkoutSession(userId: String, sessionId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("workout_sessions")
                .document(sessionId)
                .delete()
                .addOnFailureListener { e ->
                    Log.e(TAG, "No se pudo sincronizar el borrado de $sessionId: ${e.message}")
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "WarmupRepository"
    }
}
