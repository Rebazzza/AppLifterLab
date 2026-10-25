package com.example.lifterlab.data.seeder

import com.example.lifterlab.data.repository.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeder del catálogo global (DatabaseSeeder de ExerciseDB).
 *
 * Popular `/exercises/{exerciseId}` en Cloud Firestore consumiendo la API externa
 * ExerciseDB (RapidAPI). Es **idempotente**: solo hace la llamada a la red cuando
 * la colección global está vacía; si ya hay ejercicios cacheados no gasta cuota.
 *
 * Uso típico (una sola vez, p. ej. en el primer arranque autenticado):
 *
 * ```kotlin
 * scope.launch {
 *     ExerciseCatalogSeeder().seedCatalogFromApi()
 * }
 * ```
 */
class ExerciseCatalogSeeder(
    private val repository: ExerciseRepository = ExerciseRepository()
) {

    /**
     * Siembra el catálogo inicial desde RapidAPI hacia Firestore.
     *
     * @param limit Cantidad de ejercicios a importar (100 por defecto).
     * @return `Result.success(true)` si siembró, `Result.success(false)` si el
     *         catálogo ya estaba poblado, o `Result.failure` si falló la red.
     */
    suspend fun seedCatalogFromApi(limit: Int = 100): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Reutiliza la lógica Firestore-first del repository: si la
                // colección ya tiene datos, no vuelve a golpear la API.
                val current = repository.getCatalogSnapshot(limit).getOrThrow()
                if (current.isNotEmpty()) return@runCatching false

                val seeded = repository.seedCatalogFromApi(limit).getOrThrow()
                seeded > 0
            }
        }
}
