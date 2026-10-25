package com.example.lifterlab.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Traductor cliente del catálogo ExerciseDB → español.
 *
 * Motivo de existir: en el plan actual de RapidAPI, ExerciseDB IGNORA el
 * parámetro `lang=es` (verificado empíricamente: responde en inglés aunque se
 * solicite español). Para que el catálogo se vea en español, las traducciones
 * se resuelven aquí, UNA SOLA VEZ durante la siembra, y Firestore persiste
 * directamente el texto traducido (nunca se vuelve a traducir en lectura).
 *
 * Estrategia de red:
 *  - POST con form-body (NO GET): una URL de ~9 KB con instrucciones provocaba
 *    fallos silenciosos en móviles (proxies de operadora / timeouts), mientras
 *    que el POST con el mismo payload responde de forma confiable.
 *  - El endpoint público (gtx) acepta varias frases en una sola "q" separadas
 *    por "###" y devuelve el separador intacto: ~4000 caracteres por petición.
 *  - Reintentos por lote (3 intentos con espera creciente). Si un lote agota
 *    los intentos, se registra en Logcat y ese trozo queda en inglés: la
 *    siembra NUNCA se rompe por culpa de la traducción.
 *
 * El vocabulario técnico cerrado de ExerciseDB (bodyPart, target, equipment,
 * secondaryMuscles) se traduce con diccionario local: es instantáneo, funciona
 * offline y evita errores de traducción automática en términos técnicos.
 */
class ExerciseDbTranslator(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "ExerciseDbTranslator"

        // POST sin query: el payload viaja en el body (sin límite de URL).
        private const val TRANSLATE_URL =
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t"
        private const val SEPARATOR = "###"
        private const val MAX_CHARS_PER_REQUEST = 4000
        private const val MAX_ATTEMPTS = 3

        // --- Vocabulario cerrado de ExerciseDB (diccionario en/es) ---

        private val VOCAB_BODY_PART = mapOf(
            "back" to "espalda",
            "cardio" to "cardio",
            "chest" to "pecho",
            "lower arms" to "antebrazos",
            "lower legs" to "pantorrillas",
            "middle back" to "espalda media",
            "lower back" to "zona lumbar",
            "neck" to "cuello",
            "shoulders" to "hombros",
            "upper arms" to "brazos",
            "upper legs" to "muslos",
            "waist" to "cintura"
        )

        private val VOCAB_TARGET = mapOf(
            "abductors" to "abductores",
            "abs" to "abdominales",
            "adductors" to "aductores",
            "biceps" to "bíceps",
            "calves" to "pantorrillas",
            "delts" to "deltoides",
            "forearms" to "antebrazos",
            "glutes" to "glúteos",
            "hamstrings" to "isquiotibiales",
            "lats" to "dorsales",
            "levator scapulae" to "elevador de la escápula",
            "pectorals" to "pectorales",
            "quads" to "cuádriceps",
            "quadriceps" to "cuádriceps",
            "serratus anterior" to "serrato anterior",
            "spine" to "columna",
            "traps" to "trapecios",
            "triceps" to "tríceps"
        )

        private val VOCAB_EQUIPMENT = mapOf(
            "assisted" to "asistido",
            "band" to "banda",
            "barbell" to "barra",
            "body weight" to "peso corporal",
            "bosu ball" to "bola bosu",
            "cable" to "polea",
            "dumbbell" to "mancuerna",
            "kettlebell" to "peso ruso",
            "machine" to "máquina",
            "medicine ball" to "balón medicinal",
            "olympic barbell" to "barra olímpica",
            "resistance band" to "banda elástica",
            "roller" to "rodillo",
            "rope" to "cuerda",
            "smith machine" to "máquina Smith",
            "stability ball" to "pelota de estabilidad",
            "stairmill" to "escalera",
            "suspension trainer" to "entrenador de suspensión",
            "swing" to "balanceo",
            "weighted" to "con peso"
        )
    }

    /**
     * Devuelve una copia traducida al español de cada DTO:
     *  - `name` e `instructions` → traducción automática por lotes.
     *  - `bodyPart`, `target`, `equipment` y `secondaryMuscles` → diccionario.
     * Cualquier fallo de red deja el texto original en inglés (nunca lanza).
     */
    suspend fun toSpanish(
        items: List<ExerciseDbNetworkResponse>
    ): List<ExerciseDbNetworkResponse> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext items

        val names = translateTexts(items.map { it.name })
        val allInstructions = items.flatMap { it.instructions }
        val sentences = translateTexts(allInstructions)

        // Resumen visible en Logcat (nivel I): cuenta los textos que quedaron
        // idénticos al original = traducciones que fallaron y cayeron en inglés.
        val nombresSinTraducir = names.filterIndexed { i, t -> t == items[i].name }.size
        val instruccionesSinTraducir =
            sentences.filterIndexed { i, t -> t == allInstructions[i] }.size
        Log.i(
            TAG,
            "Traducción: ${items.size} nombres ($nombresSinTraducir sin traducir), " +
                "${allInstructions.size} instrucciones ($instruccionesSinTraducir sin traducir)"
        )

        var cursor = 0
        items.mapIndexed { index, dto ->
            val instructionCount = dto.instructions.size
            dto.copy(
                name = names[index],
                bodyPart = VOCAB_BODY_PART[dto.bodyPart] ?: dto.bodyPart,
                target = VOCAB_TARGET[dto.target] ?: dto.target,
                equipment = VOCAB_EQUIPMENT[dto.equipment] ?: dto.equipment,
                secondaryMuscles = dto.secondaryMuscles.map { VOCAB_TARGET[it] ?: it },
                instructions = sentences
                    .subList(cursor, cursor + instructionCount)
                    .toList()
                    .also { cursor += instructionCount }
            )
        }
    }

    /**
     * Traduce una lista de textos preservando posiciones. Los textos en blanco
     * se copian tal cual; los lotes que agoten los reintentos quedan en inglés
     * (con advertencia en Logcat, tag [TAG]).
     */
    private suspend fun translateTexts(texts: List<String>): List<String> {
        val result = texts.toMutableList()
        val pending = texts.indices.filter { texts[it].isNotBlank() }
        if (pending.isEmpty()) return result

        var chunk = mutableListOf<Int>()
        var chunkChars = 0

        suspend fun flush() {
            if (chunk.isEmpty()) return
            val joined = chunk.joinToString(SEPARATOR) { texts[it] }
            val pieces = translateChunk(joined, chunk.size)
            if (pieces != null) {
                chunk.forEachIndexed { position, originalIndex ->
                    result[originalIndex] = pieces[position]
                }
            }
            chunk = mutableListOf()
            chunkChars = 0
        }

        for (index in pending) {
            val length = texts[index].length
            if (chunkChars + length > MAX_CHARS_PER_REQUEST && chunk.isNotEmpty()) flush()
            chunk.add(index)
            chunkChars += length + SEPARATOR.length
        }
        flush()
        return result
    }

    /**
     * Traduce un lote unido por [SEPARATOR] vía POST. Reintenta hasta
     * [MAX_ATTEMPTS] veces con espera creciente; null si definitivamente no se
     * pudo (ese lote queda en inglés y se registra en Logcat).
     */
    private suspend fun translateChunk(joined: String, expectedPieces: Int): List<String>? {
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { performRequest(joined, expectedPieces) }

            val pieces = result.getOrNull()
            if (pieces != null) {
                Log.i(TAG, "Lote traducido (${joined.length} chars, intento ${attempt + 1})")
                return pieces
            }

            val error = result.exceptionOrNull()?.message ?: "error desconocido"
            Log.w(TAG, "Lote fallido (intento ${attempt + 1}/$MAX_ATTEMPTS): $error")
            if (attempt < MAX_ATTEMPTS - 1) delay(400L * (attempt + 1))
        }

        Log.w(TAG, "Lote abandonado, se mantiene en inglés (${joined.length} chars)")
        return null
    }

    /** Una petición POST de traducción; lanza con descripción si la respuesta no es utilizable. */
    private fun performRequest(joined: String, expectedPieces: Int): List<String> {
        val body = FormBody.Builder().add("q", joined).build()
        val request = Request.Builder().url(TRANSLATE_URL).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")

            val payload = response.body?.string() ?: error("body vacío")
            val root = JSONArray(payload)

            // La respuesta es [[segmentos...], null, null, 1]: el elemento 0
            // contiene las traducciones y el resto es metadata (nulls/números).
            // getJSONArray lanzaba "Value null at 1 ... cannot be converted to
            // JSONArray" al tocar esa metadata; se usa optJSONArray por seguridad.
            val segments = root.optJSONArray(0) ?: error("respuesta sin segmentos")
            val translated = buildString {
                for (i in 0 until segments.length()) {
                    val piece = segments.optJSONArray(i) ?: continue
                    append(piece.optString(0, ""))
                }
            }

            val pieces = translated.split(SEPARATOR).map { it.trim() }
            require(pieces.size == expectedPieces) {
                "pieces=${pieces.size}/esperadas=$expectedPieces"
            }
            return pieces
        }
    }
}
