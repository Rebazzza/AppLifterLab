package com.example.lifterlab.data.repository

import com.example.lifterlab.data.model.Exercise
import com.example.lifterlab.data.network.ExerciseDbApiService
import com.example.lifterlab.data.network.ExerciseDbConfig
import com.example.lifterlab.data.network.ExerciseDbNetworkResponse
import com.example.lifterlab.data.network.ExerciseDbTranslator
import com.example.lifterlab.data.network.ExerciseImageCatalog
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Repositorio del catálogo global de ejercicios.
 *
 * Estrategia **Firestore-First** para consumir de forma cautelosa la cuota gratuita
 * de RapidAPI:
 *  1. Se lee siempre de `/exercises/{exerciseId}` (Firestore Offline Persistence
 *     sirve la caché local del dispositivo sin red).
 *  2. Solo se llama a ExerciseDB cuando la colección global está vacía (primera
 *     siembra) o cuando el usuario busca un ejercicio inexistente en el catálogo
 *     guardado (fetch bajo demanda que además se persiste para futuras sesiones).
 *
 * Todas las operaciones son `suspend` + `Result<T>` sobre `Dispatchers.IO`.
 */
class ExerciseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val api: ExerciseDbApiService = ExerciseDbApiService.getInstance(),
    private val translator: ExerciseDbTranslator = ExerciseDbTranslator(),
    private val imageCatalog: ExerciseImageCatalog = ExerciseImageCatalog()
) {

    companion object {
        private const val TAG = "ExerciseRepository"

        /** Colección global compartida por todos los atletas. */
        const val EXERCISES_COLLECTION = "exercises"

        /** Lote máximo por escritura: Firestore limita los batches a 500 operaciones. */
        private const val FIRESTORE_BATCH_LIMIT = 400

        /**
         * Doc reservado dentro de /exercises que guarda el idioma con el que fue
         * sembrado el catálogo (campo `lang`). Se filtra de las lecturas; si falta
         * o no coincide con [ExerciseDbConfig.LANG], getCatalog fuerza una
         * re-siembla para migrar catálogos sembrados en inglés.
         */
        private const val META_DOC_ID = "__catalog_meta"
        private const val META_FIELD_LANG = "lang"
        private const val META_FIELD_VERSION = "version"

        /** true cuando TODOS los nombres/instrucciones de la siembra salieron en español. */
        private const val META_FIELD_TRANSLATION_COMPLETE = "translationComplete"

        /** Intentos de siembra agotados con traducción incompleta (reintento acotado). */
        private const val META_FIELD_TRANSLATION_ATTEMPTS = "translationAttempts"

        /** Reintentos de siembra con traducción parcial antes de rendirse (cuida la cuota de RapidAPI). */
        private const val MAX_TRANSLATION_ATTEMPTS = 3

        /**
         * Tope de páginas por siembra. El plan actual devuelve 10 ejercicios por
         * página (ignora `limit` mayor), por lo que 100 ejercicios = 10 páginas;
         * 50 deja margen para planes con páginas más chicas.
         */
        private const val MAX_SEED_PAGES = 50

        /**
         * Versión del contenido del catálogo. Cada versión fuerza UNA re-siembla
         * que sobrescribe los docs existentes (upsert por exerciseId):
         *  - v1: contenido en inglés (sin traducción; lang=es lo ignora RapidAPI).
         *  - v2: traductor vía GET — las URLs de ~9 KB fallaban silenciosamente
         *        en móvil, dejando nombres/instrucciones en inglés.
         *  - v3: traductor vía POST, pero el parser usaba getJSONArray sobre la
         *        metadata final (nulls) y lanzaba "Value null at 1 ... cannot be
         *        converted to JSONArray" en todos los lotes.
         *  - v4: parser tolerante, pero solo re-siembraba la PRIMERA página (10
         *        ejercicios): el resto del catálogo quedaba en inglés.
         *  - v5: siembra paginada completa + marcador translationComplete para
         *        reintentar traducciones parciales sin subir la versión.
         *  - v6: fotos de demostración desde el dataset abierto Free-Exercise-DB
         *        (el plan actual de ExerciseDB eliminó gifUrl; matcher por nombre
         *        inglés resuelto antes de traducir).
         */
        private const val CATALOG_CONTENT_VERSION = 6

        /**
         * Keywords de los básicos SBD (case-insensitive), en inglés y español,
         * porque ExerciseDB traduce los nombres con `lang=es`
         * (p. ej. "Sentadilla con Barra", "Press De Banca", "Peso Muerto").
         */
        private val SBD_KEYWORDS = listOf(
            "squat", "sentadilla",
            "bench press", "press de banca", "press banca",
            "deadlift", "peso muerto"
        )

        /**
         * Los tres básicos (SBD) garantizados DENTRO de la BD del catálogo.
         * ExerciseDB (plan gratuito) no trae la sentadilla simple ("barbell
         * squat" solo aparece como variantes) ni el peso muerto convencional en
         * las primeras páginas, y el press de banca llega como "Press de Banca
         * con Barra"; por eso la app los asegura con IDs propios (upsert
         * idempotente, CERO llamadas a RapidAPI) y con foto del dataset abierto
         * Free-Exercise-DB. Los IDs empiezan con "0000-" para ordenar antes que
         * los ejercicios sembrados (0001…) y así quedar siempre dentro del
         * límite del fetch.
         */
        private val SBD_BASICS = listOf(
            SbdBasico(
                id = "0000-sentadilla-con-barra",
                nombre = "Sentadilla con Barra",
                musculos = "Cuádriceps, Glúteos",
                fotoOrigen = "Barbell Squat",
                instrucciones = listOf(
                    "Coloca la barra sobre los trapecios y sal del rack con los pies al ancho de hombros.",
                    "Baja flexionando caderas y rodillas hasta que los muslos queden paralelos, con el pecho arriba y la espalda neutra.",
                    "Empuja el suelo con todo el pie y vuelve a la posición inicial sin bloquear las rodillas."
                )
            ),
            SbdBasico(
                id = "0000-press-banca",
                nombre = "Press de Banca",
                musculos = "Pecho, Tríceps",
                fotoOrigen = "Barbell Bench Press - Medium Grip",
                instrucciones = listOf(
                    "Acuéstate con los ojos bajo la barra, pies firmes en el suelo y escápulas retraídas.",
                    "Baja la barra controlado hasta el centro del pecho con los codos a unos 45°.",
                    "Empuja hacia arriba hasta extender los brazos sin perder la retracción de las escápulas."
                )
            ),
            SbdBasico(
                id = "0000-peso-muerto",
                nombre = "Peso Muerto",
                musculos = "Espalda, Isquiotibiales",
                fotoOrigen = "Barbell Deadlift",
                instrucciones = listOf(
                    "Coloca los pies al ancho de las caderas y la barra sobre el medio del pie.",
                    "Agarra la barra, cierra la espalda y separa la barra del suelo empujando con las piernas.",
                    "Mantén la barra pegada al cuerpo y termina de pie con caderas y rodillas extendidas."
                )
            )
        )

        /**
         * Timeout defensivo de la escritura de los básicos: con Firestore sin
         * red la escritura no se confirma y no debe colgar la pantalla.
         */
        private const val SBD_WRITE_TIMEOUT_MS = 10_000L

        /** Básico SBD autogestionado por la app (no proviene de ExerciseDB). */
        private data class SbdBasico(
            val id: String,
            val nombre: String,
            val musculos: String,
            val fotoOrigen: String,
            val instrucciones: List<String>
        ) {
            fun toExercise(gifUrl: String) = Exercise(
                exerciseId = id,
                name = nombre,
                primaryMuscle = musculos,
                equipment = "barra",
                isSBD = true,
                gifUrl = gifUrl,
                instructions = instrucciones
            )
        }

        /** Páginas extra máximas para completar un catálogo que quedó corto. */
        private const val MAX_TOP_UP_PAGES = 10
    }

    /**
     * Obtiene el catálogo global (Firestore-First) asegurando al menos [target]
     * ejercicios en español:
     *  1. Lee la caché local de Firestore (offline persistence).
     *  2. Si está vacía, siembra completa desde ExerciseDB página por página.
     *  3. Si el marcador `__catalog_meta` lo indica (versión vieja, traducción
     *     incompleta o ausente), fuerza la re-siembla completa: los docs se
     *     re-escriben por el mismo exerciseId (upsert), migrando nombres e
     *     instrucciones al español. Reintentos acotados a
     *     [MAX_TRANSLATION_ATTEMPTS] para no agotar la cuota de RapidAPI.
     *  4. Si quedó incompleta, completa con páginas siguientes (offset paginado).
     *     El top-up es tolerante a fallos de red: si falla, se sirve la caché.
     *  5. Asegura la presencia de los tres básicos SBD ([SBD_BASICS]) con un
     *     upsert propio (sin llamadas a RapidAPI y tolerante a fallos).
     */
    suspend fun getCatalog(target: Int = 100): Result<List<Exercise>> = withContext(Dispatchers.IO) {
        runCatching {
            // +SBD_BASICS.size: margen para los básicos inyectados; sin él, al
            // añadirlos 3 ejercicios sembrados caerían fuera del límite del fetch.
            val fetchLimit = target + SBD_BASICS.size
            var cached = fetchCatalogFromFirestore(fetchLimit)

            if (cached.isEmpty()) {
                seedCatalogFromApi(target).getOrThrow()
                return@runCatching ensureSbdBasics(fetchCatalogFromFirestore(fetchLimit))
            }

            // Migración de contenido: re-siembla si la versión es vieja, si la
            // traducción quedó incompleta (algunos lotes cayeron a inglés) o si
            // el marcador aún no registra traducción (catálogos de la v1-v4: la
            // API free solo devuelve 10 por página y la v4 re-siembraba únicamente
            // la primera). Tolerante: si falla (offline), se sirve el catálogo actual.
            val meta = readCatalogMeta()
            val staleVersion = (meta?.version ?: 0) < CATALOG_CONTENT_VERSION
            val translationIncomplete = meta?.translationComplete != true
            val canRetry = (meta?.translationAttempts ?: 0) < MAX_TRANSLATION_ATTEMPTS
            if (staleVersion || (translationIncomplete && canRetry)) {
                Log.i(
                    TAG,
                    "Catálogo desactualizado (v${meta?.version ?: 0} < v$CATALOG_CONTENT_VERSION, " +
                        "traducción=${if (meta?.translationComplete == true) "ok" else "incompleta"}, " +
                        "intentos=${meta?.translationAttempts ?: 0}): re-siembrando…"
                )
                runCatching { seedCatalogFromApi(target) }.getOrNull()
                cached = fetchCatalogFromFirestore(fetchLimit)
            }

            // Top-up tolerante: nunca rompe la pantalla si la red falla aquí.
            runCatching {
                var offset = cached.size
                var pages = 0
                val knownIds = cached.mapTo(HashSet()) { it.exerciseId }
                while (cached.size < target && pages < MAX_TOP_UP_PAGES) {
                    val page = api.getExercises(limit = target - cached.size, offset = offset)
                    if (page.isEmpty()) break
                    val exercises = toSpanishWithMedia(page).map { it.toDomain() }
                    // Corta si la API no trae nada nuevo (evita bucles con offsets ignorados).
                    if (exercises.all { it.exerciseId in knownIds }) break
                    persistExercises(exercises)
                    exercises.mapTo(knownIds) { it.exerciseId }
                    cached = fetchCatalogFromFirestore(fetchLimit)
                    offset += page.size
                    pages++
                }
            }.getOrNull()

            // Básicos SBD: garantizados en BD aunque la siembra no los traiga.
            ensureSbdBasics(cached)
        }
    }

    /**
     * Asegura que los tres básicos SBD existan en `/exercises` (ver [SBD_BASICS]):
     *  - Si falta un doc, lo escribe con su foto resuelta del dataset gratuito.
     *  - Si un básico quedó sin foto (la fuente de imágenes no respondió en la
     *    primera escritura), reintenta completarla en esta apertura.
     * Idempotente y tolerante a fallos: cualquier error se registra en Logcat y
     * la lista se devuelve tal cual (en el selector de rutinas los básicos ya
     * están garantizados por la lista fija de la app).
     */
    private suspend fun ensureSbdBasics(existentes: List<Exercise>): List<Exercise> {
        runCatching { imageCatalog.load() }
        val porId = existentes.associateBy { it.exerciseId }
        val aEscribir = SBD_BASICS.mapNotNull { basico ->
            val foto = runCatching { imageCatalog.resolve(basico.fotoOrigen) }.getOrNull()
            val actual = porId[basico.id]
            when {
                actual == null -> basico.toExercise(foto.orEmpty())
                actual.gifUrl.isBlank() && foto != null -> basico.toExercise(foto)
                else -> null
            }
        }
        if (aEscribir.isEmpty()) return existentes

        val guardado = runCatching {
            withTimeoutOrNull(SBD_WRITE_TIMEOUT_MS) {
                persistExercises(aEscribir)
                true
            } ?: false
        }.getOrElse { false }
        if (!guardado) {
            Log.w(
                TAG,
                "Básicos SBD: Firestore no confirmó la escritura (¿offline?); " +
                    "se reintentará en la próxima apertura"
            )
            return existentes
        }

        Log.i(TAG, "Básicos SBD asegurados en el catálogo: ${aEscribir.joinToString { it.name }}")
        // Reemplaza los que ya existían (p. ej. con foto recién reparada) y agrega los nuevos.
        val reemplazados = existentes.map { actual ->
            aEscribir.firstOrNull { it.exerciseId == actual.exerciseId } ?: actual
        }
        val nuevos = aEscribir.filter { it.exerciseId !in porId }
        return reemplazados + nuevos
    }

    /**
     * Lee la colección global SIN disparar la siembra automática. Útil para que
     * los seeders verifiquen de forma explícita si el catálogo está vacío.
     */
    suspend fun getCatalogSnapshot(limit: Int = 500): Result<List<Exercise>> =
        withContext(Dispatchers.IO) {
            runCatching { fetchCatalogFromFirestore(limit) }
        }

    /**
     * Descarga el catálogo COMPLETO de ExerciseDB **página por página** (el plan
     * actual devuelve 10 ejercicios por página e ignora `limit` mayor), los
     * traduce al cliente al español (RapidAPI ignora lang=es en este plan) y los
     * persiste en `/exercises/{exerciseId}` (upsert idempotente — re-sembrar
     * sobrescribe los mismos docs, migrando el contenido a español).
     *
     * Cada página se persiste al terminar: si la red cae a mitad de siembra, el
     * avance no se pierde. Al final marca en `__catalog_meta` idioma, versión y
     * si la traducción quedó completa; si algún lote cayó a inglés se incrementa
     * `translationAttempts` y la próxima apertura reintenta (hasta
     * [MAX_TRANSLATION_ATTEMPTS] veces) sin intervención manual en Firestore.
     * Retorna la cantidad de ejercicios sincronizados.
     */
    suspend fun seedCatalogFromApi(limit: Int = 100): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var total = 0
            var offset = 0
            var pages = 0
            var untranslated = 0
            var conFoto = 0
            val seenIds = HashSet<String>()

            // Fuente de fotos (dataset abierto en GitHub): si no responde, la
            // siembra queda "incompleta" y se reintenta en la próxima apertura.
            val imagesSourceOk = imageCatalog.load()

            while (total < limit && pages < MAX_SEED_PAGES) {
                val page = api.getExercises(limit = limit - total, offset = offset)
                if (page.isEmpty()) break
                // Corta si la API repite items (offset ignorado en algún plan).
                val nuevos = page.filter { it.id !in seenIds }
                if (nuevos.isEmpty()) break

                val traducidos = toSpanishWithMedia(nuevos)
                conFoto += traducidos.count { it.gifUrl.isNotBlank() }
                // Un texto quedó sin traducir si su nombre o sus instrucciones
                // volvieron idénticos al original en inglés (las instrucciones
                // vacías no cuentan: no había nada que traducir).
                untranslated += traducidos.indices.count { i ->
                    traducidos[i].name == nuevos[i].name ||
                        (nuevos[i].instructions.isNotEmpty() &&
                            traducidos[i].instructions == nuevos[i].instructions)
                }

                val exercises = traducidos.map { it.toDomain() }
                persistExercises(exercises)
                exercises.mapTo(seenIds) { it.exerciseId }
                total += exercises.size
                offset += page.size
                pages++
            }

            // Mejora posible: si el marcador falla, el catálogo ya quedó persistido
            // y la próxima apertura reintenta escribirlo.
            val previousAttempts = readCatalogMeta()?.translationAttempts ?: 0
            val problemas = buildList {
                if (untranslated > 0) add("$untranslated textos en inglés")
                if (!imagesSourceOk) add("fuente de fotos no disponible")
            }
            val complete = problemas.isEmpty()
            runCatching {
                writeCatalogMeta(
                    translationComplete = complete,
                    translationAttempts = if (complete) 0 else previousAttempts + 1
                )
            }
            Log.i(
                TAG,
                if (complete) {
                    "Siembra completada: $total ejercicios en $pages página(s), " +
                        "traducción completa, fotos en $conFoto/$total (v$CATALOG_CONTENT_VERSION)"
                } else {
                    "Siembra INCOMPLETA: ${problemas.joinToString(", ")} " +
                        "(intento ${previousAttempts + 1}/$MAX_TRANSLATION_ATTEMPTS) — " +
                        "se reintentará en la próxima apertura"
                }
            )
            total
        }
    }

    /**
     * Detalle de un ejercicio: primero Firestore (caché offline), y solo si no
     * existe localmente se consulta ExerciseDB y se persiste el resultado.
     */
    suspend fun getExerciseById(exerciseId: String): Result<Exercise> = withContext(Dispatchers.IO) {
        runCatching {
            val local = firestore
                .collection(EXERCISES_COLLECTION)
                .document(exerciseId)
                .get()
                .await()
                .toObject(Exercise::class.java)

            if (local != null) return@runCatching local

            val remote = toSpanishWithMedia(listOf(api.getExerciseById(exerciseId)))
                .map { it.toDomain() }
            persistExercises(remote)
            remote.first()
        }
    }

    /**
     * Ejercicios de un grupo muscular (chest, upper legs, biceps…). Se sirve desde
     * Firestore si el grupo ya está cacheado; si no, consulta la API por bodyPart
     * y persiste el lote para agotar la cuota lo menos posible.
     */
    suspend fun getExercisesByBodyPart(bodyPart: String): Result<List<Exercise>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = bodyPart.trim().lowercase(Locale.ROOT)
                val local = fetchCatalogFromFirestore(500)
                    .filter { it.primaryMuscle.equals(normalized, ignoreCase = true) }
                if (local.isNotEmpty()) return@runCatching local

                val remote = toSpanishWithMedia(api.getExercisesByBodyPart(normalized))
                    .map { it.toDomain() }
                persistExercises(remote)
                remote
            }
        }

    /**
     * Búsqueda por nombre sobre el catálogo cacheado. Si no hay coincidencias
     * locales, hace fallback a la API (bajo demanda) y cachea lo nuevo.
     */
    suspend fun searchExercises(query: String): Result<List<Exercise>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = query.trim()
                if (normalized.isEmpty()) return@runCatching emptyList()

                val localHits = fetchCatalogFromFirestore(500).filter {
                    it.name.contains(normalized, ignoreCase = true)
                }
                if (localHits.isNotEmpty()) return@runCatching localHits

                // Fallback de red: el ejercicio buscado no está en el catálogo guardado.
                val remote = toSpanishWithMedia(api.getExercises(limit = 100, offset = 0))
                    .map { it.toDomain() }
                val remoteHits = remote.filter {
                    it.name.contains(normalized, ignoreCase = true)
                }
                persistExercises(remote)
                remoteHits
            }
        }

    /** Lectura cruda de la colección global (la caché offline de Firestore actúa de buffer). */
    private suspend fun fetchCatalogFromFirestore(limit: Int): List<Exercise> {
        // +1: la query incluye el doc __catalog_meta, que se filtra después;
        // sin el margen el catálogo quedaría un ejercicio corto.
        val snapshot = firestore
            .collection(EXERCISES_COLLECTION)
            .limit((limit + 1).toLong())
            .get()
            .await()
        return snapshot.documents
            .filter { it.id != META_DOC_ID }
            .mapNotNull { it.toObject(Exercise::class.java) }
    }

    /** Metadatos del catálogo guardados en `__catalog_meta`. */
    private data class CatalogMeta(
        val version: Int,
        val translationComplete: Boolean,
        val translationAttempts: Int
    )

    /**
     * Lee el marcador del catálogo, o null si no existe (catálogo heredado de una
     * versión anterior) o su lectura falla (offline sin caché del doc). Un campo
     * `translationComplete` ausente se interpreta como incompleto: los catálogos
     * sembrados hasta la v4 tienen nombres/instrucciones en inglés y deben
     * re-sembrarse. En todos esos casos [getCatalog] dispara la re-siembla tolerante.
     */
    private suspend fun readCatalogMeta(): CatalogMeta? = runCatching {
        val doc = firestore
            .collection(EXERCISES_COLLECTION)
            .document(META_DOC_ID)
            .get()
            .await()
        if (!doc.exists()) return@runCatching null
        CatalogMeta(
            version = doc.getLong(META_FIELD_VERSION)?.toInt() ?: 0,
            translationComplete = doc.getBoolean(META_FIELD_TRANSLATION_COMPLETE) ?: false,
            translationAttempts = doc.getLong(META_FIELD_TRANSLATION_ATTEMPTS)?.toInt() ?: 0
        )
    }.getOrNull()

    /** Marca idioma, versión y estado de la traducción tras una siembra. */
    private suspend fun writeCatalogMeta(translationComplete: Boolean, translationAttempts: Int) {
        firestore
            .collection(EXERCISES_COLLECTION)
            .document(META_DOC_ID)
            .set(
                mapOf(
                    META_FIELD_LANG to ExerciseDbConfig.LANG,
                    META_FIELD_VERSION to CATALOG_CONTENT_VERSION,
                    META_FIELD_TRANSLATION_COMPLETE to translationComplete,
                    META_FIELD_TRANSLATION_ATTEMPTS to translationAttempts
                )
            )
            .await()
    }

    /**
     * Enriquece cada DTO con la URL de su foto de demostración (resuelta por
     * nombre en INGLÉS, ANTES de traducir) y lo traduce al español. Si la fuente
     * de fotos no está disponible conserva `gifUrl` tal cual — combinado con el
     * omitempty de [persistExercises], una indisponibilidad de GitHub jamás borra
     * demostraciones ya persistidas. Todas las escrituras remotas pasan por aquí.
     */
    private suspend fun toSpanishWithMedia(
        dtos: List<ExerciseDbNetworkResponse>
    ): List<ExerciseDbNetworkResponse> {
        imageCatalog.load()
        val enriched = dtos.map { it.copy(gifUrl = imageCatalog.resolve(it.name) ?: it.gifUrl) }
        return translator.toSpanish(enriched)
    }

    /** Upsert por lote en `/exercises/{exerciseId}` — idempotente y reintentable.
     *  Los campos vacíos (`gifUrl` en blanco) NO se escriben: una siembra sin
     *  fotos disponibles nunca borra las demostraciones ya persistidas. */
    private suspend fun persistExercises(exercises: List<Exercise>) {
        val collection = firestore.collection(EXERCISES_COLLECTION)
        exercises.chunked(FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { exercise ->
                val docId = exercise.exerciseId.ifBlank { collection.document().id }
                // Debe coincidir 1:1 con las propiedades de Exercise (Firestore).
                val data = mutableMapOf<String, Any>(
                    "exerciseId" to docId,
                    "name" to exercise.name,
                    "primaryMuscle" to exercise.primaryMuscle,
                    "equipment" to exercise.equipment,
                    "isSBD" to exercise.isSBD,
                    "instructions" to exercise.instructions
                )
                if (exercise.gifUrl.isNotBlank()) data["gifUrl"] = exercise.gifUrl
                batch.set(collection.document(docId), data, SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    /**
     * Transformación ExerciseDB → modelo de Firestore:
     *  - `id` → `exerciseId`
     *  - `name` → Title Case
     *  - `target` (fallback `bodyPart`) → `primaryMuscle`
     *  - `equipment` → `equipment`
     *  - `gifUrl` → `gifUrl` (URL de la demostración resuelta por
     *    [ExerciseImageCatalog]; solo la cadena URL, nunca binarios/Base64)
     *  - `isSBD` → true si el nombre contiene "squat", "bench press" o "deadlift"
     */
    private fun ExerciseDbNetworkResponse.toDomain(): Exercise = Exercise(
        exerciseId = id,
        name = name.toTitleCase(),
        primaryMuscle = target.ifBlank { bodyPart },
        equipment = equipment,
        isSBD = isSbdName(name),
        gifUrl = gifUrl,
        instructions = instructions
    )

    private fun isSbdName(name: String): Boolean {
        val lowered = name.lowercase(Locale.ROOT)
        return SBD_KEYWORDS.any { lowered.contains(it) }
    }

    /**
     * Title Case en español: capitaliza la primera letra de cada palabra pero
     * deja artículos y preposiciones en minúscula ("Press de Banca con Barra",
     * no "Press De Banca Con Barra"). La primera palabra siempre va en mayúscula.
     */
    private fun String.toTitleCase(): String {
        val stopWords = setOf(
            "de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas",
            "y", "o", "a", "en", "con", "por", "para", "al", "su", "sus"
        )
        return lowercase(Locale.ROOT)
            .split(" ")
            .mapIndexed { index, word ->
                val lowered = word.lowercase(Locale.ROOT)
                if (index > 0 && lowered in stopWords) lowered
                else word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
            .joinToString(" ")
    }
}
