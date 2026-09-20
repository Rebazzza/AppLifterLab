package com.example.lifterlab.data.seed

import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.model.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper de Inicialización de Base de Datos (DatabaseSeeder)
 * Encargado de poblar la base de datos Firestore con datos ficticios iniciales (mock data).
 *
 * Este seeder es IDEMPOTENTE: verifica si el documento del usuario ya existe antes de
 * escribir cualquier dato, evitando duplicados en cada inicio de sesión.
 */
class FirestoreSeeder(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Siembra los datos iniciales para el usuario autenticado y sus plantillas de rutinas.
     * Solo ejecuta la escritura si el documento /users/{userId} NO existe aún.
     */
    suspend fun seedInitialData(userId: String): Result<Boolean> {
        return try {
            val db = firestore
            val userRef = db.collection("users").document(userId)

            val userSnapshot = userRef.get().await()
            if (userSnapshot.exists()) {
                return Result.success(false)
            }

            val profile = UserProfile(
                name = "Atleta LifterLab",
                currentWeightKg = 80.0,
                baseBarWeightKg = 20.0,
                squat1RM = 185.0,
                bench1RM = 120.0,
                deadlift1RM = 225.0,
                squatGoalKg = 200.0,
                benchGoalKg = 130.0,
                deadliftGoalKg = 235.0,
                streakDays = 4,
                currentCycleWeek = 4,
                cyclePhase = "PEAK"
            )
            userRef.set(profile).await()

            // Sesión inicial de entrenamiento
            val sessionsRef = userRef.collection("workout_sessions")
            val sampleSession = WorkoutSession(
                routineName = "Heavy Lower Body - A",
                totalVolumeKg = 12450.0,
                durationMins = 94,
                isFreeSession = false
            )
            sessionsRef.document().set(sampleSession).await()

            // Plantillas iniciales de rutinas
            val routinesRef = userRef.collection("routines")

            val routine1 = Routine(
                name = "Heavy Lower Body - A",
                category = "HYPERTROPHY",
                tags = listOf("HYPERTROPHY", "LEGS"),
                estimatedTimeMins = 60,
                exercises = listOf(
                    RoutineExercise(
                        name = "Barbell Squat",
                        targetMuscles = "Target: Quads, Glutes",
                        sets = listOf(
                            ExerciseSet(setNumber = 1, weightKg = 140.0, reps = 5, rpe = 8.0),
                            ExerciseSet(setNumber = 2, weightKg = 140.0, reps = 5, rpe = 8.5)
                        )
                    ),
                    RoutineExercise(
                        name = "Romanian Deadlift",
                        targetMuscles = "Target: Hamstrings, Glutes",
                        sets = listOf(
                            ExerciseSet(setNumber = 1, weightKg = 120.0, reps = 8, rpe = 8.0),
                            ExerciseSet(setNumber = 2, weightKg = 125.0, reps = 8, rpe = 9.0)
                        )
                    )
                )
            )

            val routine2 = Routine(
                name = "Push Day - Hypertrophy",
                category = "HYPERTROPHY",
                tags = listOf("HYPERTROPHY", "PUSH"),
                estimatedTimeMins = 60,
                exercises = listOf(
                    RoutineExercise(
                        name = "Bench Press",
                        targetMuscles = "Target: Chest, Triceps",
                        sets = listOf(
                            ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 8, rpe = 8.0),
                            ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 8, rpe = 8.5)
                        )
                    ),
                    RoutineExercise(
                        name = "Overhead Press",
                        targetMuscles = "Target: Shoulders, Triceps",
                        sets = listOf(
                            ExerciseSet(setNumber = 1, weightKg = 60.0, reps = 8, rpe = 8.0)
                        )
                    )
                )
            )

            val routine3 = Routine(
                name = "Pull Day - Strength",
                category = "STRENGTH",
                tags = listOf("STRENGTH", "PULL"),
                estimatedTimeMins = 75,
                exercises = listOf(
                    RoutineExercise(
                        name = "Deadlift",
                        targetMuscles = "Target: Back, Hamstrings",
                        sets = listOf(
                            ExerciseSet(setNumber = 1, weightKg = 180.0, reps = 5, rpe = 8.5)
                        )
                    ),
                    RoutineExercise(
                        name = "Barbell Row",
                        targetMuscles = "Target: Lats, Upper Back",
                        sets = listOf(
                            ExerciseSet(setNumber = 1, weightKg = 90.0, reps = 8, rpe = 8.0)
                        )
                    )
                )
            )

            val doc1 = routinesRef.document()
            doc1.set(routine1.copy(id = doc1.id)).await()

            val doc2 = routinesRef.document()
            doc2.set(routine2.copy(id = doc2.id)).await()

            val doc3 = routinesRef.document()
            doc3.set(routine3.copy(id = doc3.id)).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
