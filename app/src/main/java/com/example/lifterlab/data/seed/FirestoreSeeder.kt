package com.example.lifterlab.data.seed

import com.example.lifterlab.data.model.*
import com.google.firebase.Timestamp
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
            // A. Colección Global: Ejercicios
            // ==========================================
            val exercisesCollection = db.collection("exercises")
            val globalExercises = listOf(
                Exercise("Squat", "Legs", "Barbell", isSBD = true),
                Exercise("Bench Press", "Chest", "Barbell", isSBD = true),
                Exercise("Deadlift", "Back & Legs", "Barbell", isSBD = true),
                Exercise("Pull-up", "Back", "Bodyweight", isSBD = false)
            )

            for (exercise in globalExercises) {
                // Al usar add(), Firestore genera automáticamente el ID del documento
                exercisesCollection.add(exercise).await()
            }

            // ==========================================
            // B. Subcolecciones Privadas del Atleta
            // Ruta base: /users/{userId}
            // ==========================================
            val userRef = db.collection("users").document(userId)

            // 1. Documento de Perfil
            val profile = UserProfile(
                name = "Atleta Demo",
                birthYear = 1995,
                gender = "Male",
                height = 1.75,
                currentWeightKg = 82.5,
                baseBarWeightKg = 20.0
            )
            userRef.set(profile).await()

            // 2. Subcolección Rutinas
            val routinesRef = userRef.collection("routines")
            val sampleRoutine = Routine(
                name = "Fuerza Base - Día 1",
                category = "Powerlifting",
                exercises = listOf(
                    RoutineExercise(
                        name = "Squat",
                        sets = listOf(
                            ExerciseSet(weightKg = 120.0, reps = 5, rpe = 7.0),
                            ExerciseSet(weightKg = 130.0, reps = 5, rpe = 8.0)
                        )
                    )
                )
            )
            routinesRef.add(sampleRoutine).await()

            // 3. Subcolección Sesiones de Entrenamiento
            val sessionsRef = userRef.collection("workout_sessions")
            val sampleSession = WorkoutSession(
                date = Timestamp.now(),
                routineName = "Fuerza Base - Día 1",
                totalVolumeKg = 1250.0,
                isFreeSession = false,
                exercises = listOf(
                    RoutineExercise(
                        name = "Squat",
                        sets = listOf(
                            ExerciseSet(weightKg = 130.0, reps = 5, rpe = 8.0, est1RM = 146.0, isPR = false),
                            ExerciseSet(weightKg = 140.0, reps = 3, rpe = 9.0, est1RM = 153.0, isPR = true)
                        )
                    )
                )
            )
            sessionsRef.add(sampleSession).await()

            // 4. Subcolección Competencias
            val competitionsRef = userRef.collection("competitions")
            val sampleCompetition = Competition(
                name = "Nacional de Powerlifting 2026",
                athleteWeightKg = 82.3,
                dotsScore = 412.5,
                attempts = mapOf(
                    "squat" to listOf(
                        CompetitionAttempt(180.0, true),
                        CompetitionAttempt(190.0, true),
                        CompetitionAttempt(200.0, false)
                    ),
                    "bench" to listOf(
                        CompetitionAttempt(120.0, true),
                        CompetitionAttempt(125.0, true),
                        CompetitionAttempt(130.0, false)
                    ),
                    "deadlift" to listOf(
                        CompetitionAttempt(220.0, true),
                        CompetitionAttempt(235.0, true),
                        CompetitionAttempt(250.0, true)
                    )
                )
            )
            competitionsRef.add(sampleCompetition).await()

            // 5. Subcolección Equipamiento y Sostenibilidad (ODS 12)
            val equipmentRef = userRef.collection("equipment")
            val sampleEquipment = EquipmentItem(
                name = "Rodilleras SBD",
                type = "Knee Sleeves",
                totalSessionsUsed = 48,
                maxSessionsThreshold = 50, // Notificará pronto que requiere mantenimiento
                needsMaintenance = false
            )
            equipmentRef.add(sampleEquipment).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
