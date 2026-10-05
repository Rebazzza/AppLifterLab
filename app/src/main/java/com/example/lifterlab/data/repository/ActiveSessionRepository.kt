package com.example.lifterlab.data.repository

import android.content.Context
import com.example.lifterlab.data.model.ActiveSession
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * RF04 — Persistencia local de la sesión activa ("Modo Gimnasio").
 *
 * Guarda una única sesión en curso en las preferencias del dispositivo para que el
 * atleta pueda cerrar la app y retomarla sin perder las series ya registradas.
 * Al cerrar el entrenamiento se elimina; el resumen definitivo va a Firestore
 * mediante [WarmupRepository.saveWorkoutSession].
 */
class ActiveSessionRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Devuelve la sesión activa guardada en el dispositivo, o null si no hay ninguna.
     */
    suspend fun getActiveSession(): Result<ActiveSession?> {
        return withContext(Dispatchers.IO) {
            try {
                val raw = prefs.getString(KEY_ACTIVE_SESSION, null)
                if (raw.isNullOrBlank()) {
                    Result.success(null)
                } else {
                    Result.success(activeSessionFromJson(raw))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Guarda (o sobrescribe) la sesión activa en curso.
     */
    suspend fun saveActiveSession(session: ActiveSession): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().putString(KEY_ACTIVE_SESSION, activeSessionToJson(session).toString()).apply()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina la sesión activa local (al terminar o descartar el entrenamiento).
     */
    suspend fun clearActiveSession(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun activeSessionToJson(session: ActiveSession): JSONObject {
        val exercisesJson = JSONArray()
        session.exercises.forEach { exercise ->
            val setsJson = JSONArray()
            exercise.sets.forEach { set ->
                setsJson.put(
                    JSONObject()
                        .put("setNumber", set.setNumber)
                        .put("weightKg", set.weightKg)
                        .put("reps", set.reps)
                        .put("rpe", set.rpe)
                        .put("est1RM", set.est1RM)
                        .put("isPR", set.isPR)
                        .put("isCompleted", set.isCompleted)
                )
            }
            exercisesJson.put(
                JSONObject()
                    .put("exerciseId", exercise.exerciseId)
                    .put("name", exercise.name)
                    .put("targetMuscles", exercise.targetMuscles)
                    .put("sets", setsJson)
            )
        }
        return JSONObject()
            .put("routineId", session.routineId)
            .put("routineName", session.routineName)
            .put("isFreeSession", session.isFreeSession)
            .put("startedAtMillis", session.startedAtMillis)
            .put("exercises", exercisesJson)
    }

    private fun activeSessionFromJson(raw: String): ActiveSession {
        val root = JSONObject(raw)
        val exercisesJson = root.optJSONArray("exercises") ?: JSONArray()
        val exercises = mutableListOf<RoutineExercise>()

        for (i in 0 until exercisesJson.length()) {
            val exerciseJson = exercisesJson.getJSONObject(i)
            val setsJson = exerciseJson.optJSONArray("sets") ?: JSONArray()
            val sets = mutableListOf<ExerciseSet>()

            for (j in 0 until setsJson.length()) {
                val setJson = setsJson.getJSONObject(j)
                sets.add(
                    ExerciseSet(
                        setNumber = setJson.optInt("setNumber", j + 1),
                        weightKg = setJson.optDouble("weightKg", 0.0),
                        reps = setJson.optInt("reps", 0),
                        rpe = setJson.optDouble("rpe", 0.0),
                        est1RM = setJson.optDouble("est1RM", 0.0),
                        isPR = setJson.optBoolean("isPR", false),
                        isCompleted = setJson.optBoolean("isCompleted", false)
                    )
                )
            }

            exercises.add(
                RoutineExercise(
                    exerciseId = exerciseJson.optString("exerciseId", ""),
                    name = exerciseJson.optString("name", ""),
                    targetMuscles = exerciseJson.optString("targetMuscles", ""),
                    sets = sets
                )
            )
        }

        return ActiveSession(
            routineId = root.optString("routineId", ""),
            routineName = root.optString("routineName", ""),
            isFreeSession = root.optBoolean("isFreeSession", false),
            startedAtMillis = root.optLong("startedAtMillis", 0L),
            exercises = exercises
        )
    }

    private companion object {
        const val PREFS_NAME = "lifterlab_active_session"
        const val KEY_ACTIVE_SESSION = "active_session"
    }
}