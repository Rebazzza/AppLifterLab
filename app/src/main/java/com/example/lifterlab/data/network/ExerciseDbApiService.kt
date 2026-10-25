package com.example.lifterlab.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Constantes de conexión para la API externa ExerciseDB alojada en RapidAPI.
 *
 * NOTA DE SEGURIDAD: la API key vive aquí como último recurso de desarrollo.
 * Antes de publicar en Play Store debe migrarse a `local.properties` +
 * `BuildConfigField` (nunca commitearla al VCS en ramas públicas).
 */
object ExerciseDbConfig {
    const val BASE_URL = "https://exercisedb.p.rapidapi.com/"
    const val API_KEY = "1b006addc4msh8fe81ee8f6a5a6cp1cf327jsnf622de8b3f96"
    const val API_HOST = "exercisedb.p.rapidapi.com"

    /**
     * Idioma de la respuesta remota: ExerciseDB traduce `name` e `instructions`
     * al enviar el parámetro `lang=es` (p. ej. /exercises?lang=es&limit=100).
     */
    const val LANG = "es"
}

/**
 * Interceptor que inyecta los headers obligatorios de RapidAPI en cada petición:
 * `X-RapidAPI-Key` y `X-RapidAPI-Host`.
 */
private class RapidApiHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-RapidAPI-Key", ExerciseDbConfig.API_KEY)
            .addHeader("X-RapidAPI-Host", ExerciseDbConfig.API_HOST)
            .build()
        return chain.proceed(request)
    }
}

/**
 * Interfaz de Retrofit del catálogo ExerciseDB.
 * Todas las funciones son `suspend` para consumirse con Coroutines en `Dispatchers.IO`.
 */
interface ExerciseDbApiService {

    /** Catálogo paginado traducido a español (`lang=es`). */
    @GET("exercises")
    suspend fun getExercises(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("lang") lang: String = ExerciseDbConfig.LANG
    ): List<ExerciseDbNetworkResponse>

    /** Detalle de un ejercicio por su ID remoto (traducido). */
    @GET("exercises/exercise/{id}")
    suspend fun getExerciseById(
        @Path("id") id: String,
        @Query("lang") lang: String = ExerciseDbConfig.LANG
    ): ExerciseDbNetworkResponse

    /** Ejercicios filtrados por grupo muscular (bodyPart: chest, upper legs, etc.). */
    @GET("exercises/bodyPart/{bodyPart}")
    suspend fun getExercisesByBodyPart(
        @Path("bodyPart") bodyPart: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("lang") lang: String = ExerciseDbConfig.LANG
    ): List<ExerciseDbNetworkResponse>

    companion object {
        /** Singleton perezoso: un único cliente OkHttp/Retrofit para toda la app. */
        @Volatile
        private var instance: ExerciseDbApiService? = null

        fun getInstance(): ExerciseDbApiService = instance ?: synchronized(this) {
            instance ?: build().also { instance = it }
        }

        private fun build(): ExerciseDbApiService {
            val logging = HttpLoggingInterceptor().apply {
                // BODY en debug-only: en release solo logea para no filtrar headers.
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(RapidApiHeaderInterceptor())
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(ExerciseDbConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExerciseDbApiService::class.java)
        }
    }
}
