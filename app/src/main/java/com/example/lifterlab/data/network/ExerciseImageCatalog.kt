package com.example.lifterlab.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Resolutor de fotos de demostración por nombre de ejercicio (fuente gratuita).
 *
 * Contexto: el plan actual de ExerciseDB (RapidAPI) dejó de entregar `gifUrl`
 * (migró a un dataset nuevo con `description/difficulty/category` sin medios) y
 * la única fuente oficial de GIFs animados (exercisedb.io) es de pago. Como
 * alternativa gratuita se usa el dataset abierto **Free-Exercise-DB** (GitHub),
 * que comparte el mismo vocabulario de nombres que ExerciseDB y publica fotos
 * JPG reales (2 ángulos por ejercicio) bajo `raw.githubusercontent.com`.
 *
 * Estrategia:
 *  - El índice `dist/exercises.json` (~876 entradas) se descarga UNA vez por
 *    sesión y se queda en memoria: ninguna foto se descarga aquí, solo se
 *    resuelven URLs que después Coil carga en la vista de detalle.
 *  - El emparejado es por nombre en INGLÉS normalizado (minúsculas y solo
 *    letras/dígitos), ANTES de traducir el catálogo al español: "3/4 Sit-Up"
 *    e "3_4_Sit-Up" normalizan a "34situp".
 *  - Si GitHub no responde, [load] devuelve false y no se reintenta en la
 *    sesión (la siembra queda "incompleta" y se reintenta en la próxima apertura).
 */
class ExerciseImageCatalog(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "ExerciseImageCatalog"
        private const val INDEX_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
        private const val IMAGE_BASE =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

        /**
         * Normalización para emparejar nombres entre datasets: minúsculas y solo
         * letras/dígitos ("3/4 Sit-Up" = "3_4_Sit-Up" = "34situp").
         */
        fun normalize(name: String): String =
            name.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    }

    /** Índice nombre-normalizado → URL de la primera foto. Memoria de la sesión. */
    @Volatile
    private var index: Map<String, String>? = null

    /** true si el índice falló esta sesión (evita reintentos bloqueantes repetidos). */
    @Volatile
    private var loadFailed = false

    /**
     * Carga (una sola vez por sesión) el índice de fotos desde GitHub.
     * false si la fuente no está disponible: las demostraciones quedan pendientes
     * y la próxima siembra reintenta.
     */
    suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        index?.let { return@withContext true }
        if (loadFailed) return@withContext false

        runCatching {
            val request = Request.Builder().url(INDEX_URL).build()
            val payload = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string() ?: error("body vacío")
            }
            val parsed = parseIndex(payload)
            index = parsed
            Log.i(TAG, "Índice de imágenes cargado: ${parsed.size} entradas")
        }.onFailure { e ->
            loadFailed = true
            Log.w(
                TAG,
                "Fuente de fotos no disponible (${e.message}); demostraciones quedan pendientes"
            )
        }
        index != null
    }

    /** URL de la foto de demostración para un nombre en INGLÉS, o null si no existe. */
    fun resolve(englishName: String): String? = index?.get(normalize(englishName))

    private fun parseIndex(payload: String): Map<String, String> {
        val array = JSONArray(payload)
        val map = HashMap<String, String>(array.length() * 2)
        for (i in 0 until array.length()) {
            val entry: JSONObject = array.optJSONObject(i) ?: continue
            val path = entry.optJSONArray("images")
                ?.optString(0)
                ?.takeIf { it.isNotBlank() }
                ?: continue
            val url = IMAGE_BASE + path
            // Se indexa por nombre y por id/slug: ambos normalizados por si difieren.
            for (key in listOf(entry.optString("name"), entry.optString("id"))) {
                if (key.isBlank()) continue
                val normalized = normalize(key)
                if (!map.containsKey(normalized)) map[normalized] = url
            }
        }
        return map
    }
}
