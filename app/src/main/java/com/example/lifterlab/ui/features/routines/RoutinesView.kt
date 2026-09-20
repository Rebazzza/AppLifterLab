package com.example.lifterlab.ui.features.routines

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.lifterlab.BG_BACKGROUND
import com.example.lifterlab.BG_SURFACE_CONTAINER_HIGH
import com.example.lifterlab.BG_SURFACE_CONTAINER_LOW
import com.example.lifterlab.ERROR_ACCENT
import com.example.lifterlab.OUTLINE_VARIANT
import com.example.lifterlab.PRIMARY_ACCENT
import com.example.lifterlab.PRIMARY_CONTAINER
import com.example.lifterlab.SECONDARY_ACCENT
import com.example.lifterlab.TERTIARY_ACCENT
import com.example.lifterlab.TEXT_ON_BACKGROUND
import com.example.lifterlab.TEXT_ON_SURFACE_VARIANT
import com.example.lifterlab.bgSurfaceCard
import com.example.lifterlab.bgTagChip
import com.example.lifterlab.bgTextField
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.RoutineRepository
import com.example.lifterlab.dp
import com.example.lifterlab.primaryButton
import com.example.lifterlab.secondaryButton
import com.example.lifterlab.setMargins
import com.example.lifterlab.setTextSizeSp
import com.example.lifterlab.setTypefaceMedium
import com.example.lifterlab.toast
import com.example.lifterlab.ui.FormView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RoutinesView(
    context: Context,
    private val onBack: () -> Unit,
    private val authRepository: AuthRepository = AuthRepository(),
    private val routineRepository: RoutineRepository = RoutineRepository()
) : FormView(context) {

    private val userId: String = authRepository.getCurrentUser()?.uid.orEmpty()

    // Estado local de la rutina activa en edición
    private var activeRoutine = Routine(
        name = "Heavy Lower Body - A",
        category = "HYPERTROPHY",
        tags = listOf("HYPERTROPHY", "LEGS"),
        estimatedTimeMins = 60,
        exercises = mutableListOf(
            RoutineExercise(
                name = "Barbell Squat",
                targetMuscles = "Target: Quads, Glutes",
                sets = mutableListOf(
                    ExerciseSet(setNumber = 1, weightKg = 140.0, reps = 5, rpe = 8.0, isCompleted = true),
                    ExerciseSet(setNumber = 2, weightKg = 140.0, reps = 5, rpe = 8.5)
                )
            )
        )
    )

    private var savedTemplates: List<Routine> = emptyList()

    // Componentes de la UI
    private val routineNameInput: EditText = EditText(context).apply {
        hint = "Nombre de la Rutina..."
        setTextColor(TEXT_ON_BACKGROUND)
        setHintTextColor(TEXT_ON_SURFACE_VARIANT)
        setTextSizeSp(22f)
        setTypefaceMedium()
        background = null
        setPadding(0, dp(8), 0, dp(8))
        setSingleLine(true)
    }

    private val tagsContainer: LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        setMargins(topDp = 8)
    }

    private val exercisesContainer: LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    private val templatesContainer: LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
        setMargins(topDp = 12)
    }

    private val volumeValueText: TextView = TextView(context).apply {
        text = "0 kg"
        setTextColor(PRIMARY_CONTAINER)
        setTextSizeSp(32f)
        setTypefaceMedium()
        typeface = Typeface.MONOSPACE
    }

    private val statusText: TextView = TextView(context).apply {
        text = "● Editando: Heavy Lower Body - A"
        setTextColor(TERTIARY_ACCENT)
        setTextSizeSp(13f)
        setTypefaceMedium()
    }

    private val saveButton: MaterialButton = context.primaryButton("Guardar Rutina")

    private val catalogExercises = listOf(
        Pair("Barbell Squat", "Target: Quads, Glutes"),
        Pair("Bench Press", "Target: Chest, Triceps"),
        Pair("Deadlift", "Target: Back, Hamstrings"),
        Pair("Romanian Deadlift", "Target: Hamstrings, Glutes"),
        Pair("Overhead Press", "Target: Shoulders, Triceps"),
        Pair("Barbell Row", "Target: Lats, Upper Back"),
        Pair("Incline Dumbbell Press", "Target: Upper Chest, Shoulders"),
        Pair("Pull Ups", "Target: Lats, Biceps"),
        Pair("Dips", "Target: Chest, Triceps"),
        Pair("Leg Press", "Target: Quads"),
        Pair("Leg Curl", "Target: Hamstrings"),
        Pair("Calf Raise", "Target: Calves")
    )

    private val content: LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(20), dp(36), dp(20), dp(32))

        // Botón Volver
        addView(TextView(context).apply {
            text = "← Volver"
            textSize = 15f
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setOnClickListener { onBack() }
        })

        // Título de Gestión de Rutinas
        addView(TextView(context).apply {
            text = "Gestión de Rutinas"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(28f)
            setTypefaceMedium()
            setPadding(0, dp(8), 0, 0)
        })

        addView(TextView(context).apply {
            text = "Diseña, edita y gestiona tus protocolos de entrenamiento."
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(14f)
            setPadding(0, dp(4), 0, dp(16))
        })

        // 1. Tarjeta Encabezado de la Rutina Activa
        val activeRoutineCard = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))

            addView(routineNameInput)
            addView(tagsContainer)
        }
        addView(activeRoutineCard)

        // 2. Contenedor de Ejercicios
        addView(exercisesContainer)

        // 3. Botón Agregar Ejercicio
        val addExerciseBtn = context.secondaryButton("+ Agregar Ejercicio").apply {
            strokeColor = ColorStateList.valueOf(PRIMARY_ACCENT)
            setTextColor(PRIMARY_ACCENT)
            setOnClickListener { showAddExerciseDialog() }
        }
        addView(
            addExerciseBtn,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(16) }
        )

        // 4. Tarjeta de Volumen Estimado
        val volumeCard = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))

            addView(TextView(context).apply {
                text = "VOLUMEN ESTIMADO (EST. VOLUME)"
                setTextColor(TEXT_ON_SURFACE_VARIANT)
                setTextSizeSp(11f)
                setTypefaceMedium()
            })
            addView(volumeValueText)
            volumeValueText.setMargins(topDp = 4)
        }
        addView(
            volumeCard,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
        )

        // 5. Plantillas Guardadas
        val templatesCard = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))

            addView(TextView(context).apply {
                text = "🔖 Plantillas Guardadas (Saved Templates)"
                setTextColor(TEXT_ON_BACKGROUND)
                setTextSizeSp(16f)
                setTypefaceMedium()
            })
            addView(templatesContainer)
        }
        addView(
            templatesCard,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
        )

        // 6. Mensaje de error / estado
        addView(errorText)
        errorText.setMargins(topDp = 12)

        // 7. Barra Flotante de Guardado
        val saveCard = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(16), dp(12), dp(16), dp(12))

            val statusWrapper = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                addView(statusText)
            }
            addView(statusWrapper)

            saveButton.setOnClickListener { saveActiveRoutine() }
            addView(
                saveButton,
                LayoutParams(LayoutParams.WRAP_CONTENT, dp(44))
            )
        }
        addView(
            saveCard,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(20) }
        )
    }

    init {
        scrollContent(content)
        setupListeners()
        renderActiveRoutine()
        loadSavedTemplates()
    }

    private fun setupListeners() {
        routineNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newName = s?.toString().orEmpty().ifBlank { "Sin Nombre" }
                activeRoutine = activeRoutine.copy(name = newName)
                statusText.text = "● Editando: $newName"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderActiveRoutine() {
        routineNameInput.setText(activeRoutine.name)
        statusText.text = "● Editando: ${activeRoutine.name}"

        // Renderizar tags
        tagsContainer.removeAllViews()
        val tags = activeRoutine.tags.ifEmpty { listOf("HYPERTROPHY", "GENERAL") }
        for (tag in tags) {
            val chip = TextView(context).apply {
                text = tag.uppercase(Locale.ROOT)
                setTextColor(PRIMARY_ACCENT)
                setTextSizeSp(11f)
                setTypefaceMedium()
                background = bgTagChip(BG_SURFACE_CONTAINER_HIGH, PRIMARY_ACCENT)
                setPadding(dp(10), dp(4), dp(10), dp(4))
            }
            tagsContainer.addView(chip)
            chip.setMargins(endDp = 8)
        }

        // Renderizar ejercicios
        exercisesContainer.removeAllViews()
        for ((exIndex, exercise) in activeRoutine.exercises.withIndex()) {
            val exCard = createExerciseCard(exIndex, exercise)
            exercisesContainer.addView(exCard)
            exCard.setMargins(topDp = 16)
        }

        calculateVolume()
    }

    private fun createExerciseCard(exIndex: Int, exercise: RoutineExercise): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        // Encabezado del Ejercicio
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val infoWrapper = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text = exercise.name
                    setTextColor(TEXT_ON_BACKGROUND)
                    setTextSizeSp(18f)
                    setTypefaceMedium()
                })

                addView(TextView(context).apply {
                    text = exercise.targetMuscles.ifEmpty { "Target: General" }
                    setTextColor(TEXT_ON_SURFACE_VARIANT)
                    setTextSizeSp(12f)
                    typeface = Typeface.MONOSPACE
                })
            }
            addView(infoWrapper)

            // Botón eliminar ejercicio
            val deleteExBtn = TextView(context).apply {
                text = "🗑"
                setTextSizeSp(18f)
                setTextColor(ERROR_ACCENT)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener {
                    removeExercise(exIndex)
                }
            }
            addView(deleteExBtn)
        }
        card.addView(headerRow)

        // Tabla de Sets (Header)
        val tableHeaderRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setMargins(topDp = 14, bottomDp = 6)

            fun headerCol(text: String, weight: Float, gravityAlign: Int = Gravity.CENTER) = TextView(context).apply {
                this.text = text
                setTextColor(TEXT_ON_SURFACE_VARIANT)
                setTextSizeSp(11f)
                setTypefaceMedium()
                gravity = gravityAlign
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, weight)
            }

            addView(headerCol("SET", 1.5f, Gravity.START))
            addView(headerCol("KG", 2.5f))
            addView(headerCol("REPS", 2.5f))
            addView(headerCol("RPE", 2f))
            addView(headerCol("STATUS", 2f))
        }
        card.addView(tableHeaderRow)

        // Lista de Sets
        for ((setIndex, set) in exercise.sets.withIndex()) {
            val setRow = createSetRow(exIndex, setIndex, set)
            card.addView(setRow)
            setRow.setMargins(topDp = 6)
        }

        // Botón + Add Set
        val addSetBtn = context.secondaryButton("+ Add Set").apply {
            textSize = 13f
            setOnClickListener {
                addSetToExercise(exIndex)
            }
        }
        card.addView(
            addSetBtn,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(42)).apply { topMargin = dp(14) }
        )

        return card
    }

    private fun createSetRow(exIndex: Int, setIndex: Int, set: ExerciseSet): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgTextField()
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        // Número de Set
        val numText = TextView(context).apply {
            text = "${setIndex + 1}"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(14f)
            setTypefaceMedium()
            typeface = Typeface.MONOSPACE
            gravity = Gravity.START
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.5f)
        }
        row.addView(numText)

        // KG Input
        val kgInput = EditText(context).apply {
            setText(if (set.weightKg > 0) set.weightKg.toString() else "")
            hint = "0"
            setTextColor(TEXT_ON_BACKGROUND)
            setHintTextColor(TEXT_ON_SURFACE_VARIANT)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextSizeSp(14f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = null
            setSingleLine(true)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2.5f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val w = s?.toString()?.toDoubleOrNull() ?: 0.0
                    updateSetData(exIndex, setIndex, weightKg = w)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        row.addView(kgInput)

        // REPS Input
        val repsInput = EditText(context).apply {
            setText(if (set.reps > 0) set.reps.toString() else "")
            hint = "0"
            setTextColor(TEXT_ON_BACKGROUND)
            setHintTextColor(TEXT_ON_SURFACE_VARIANT)
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextSizeSp(14f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = null
            setSingleLine(true)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2.5f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val r = s?.toString()?.toIntOrNull() ?: 0
                    updateSetData(exIndex, setIndex, reps = r)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        row.addView(repsInput)

        // RPE Input
        val rpeInput = EditText(context).apply {
            setText(if (set.rpe > 0) set.rpe.toString() else "")
            hint = "8.0"
            setTextColor(SECONDARY_ACCENT)
            setHintTextColor(TEXT_ON_SURFACE_VARIANT)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextSizeSp(14f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = null
            setSingleLine(true)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val rpeVal = s?.toString()?.toDoubleOrNull() ?: 0.0
                    updateSetData(exIndex, setIndex, rpe = rpeVal)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        row.addView(rpeInput)

        // Check completion toggle button
        val checkBtn = TextView(context).apply {
            text = if (set.isCompleted) "✓" else "○"
            setTextColor(if (set.isCompleted) TERTIARY_ACCENT else TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(16f)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                val newCompleted = !set.isCompleted
                updateSetData(exIndex, setIndex, isCompleted = newCompleted)
                text = if (newCompleted) "✓" else "○"
                setTextColor(if (newCompleted) TERTIARY_ACCENT else TEXT_ON_SURFACE_VARIANT)
            }
        }
        row.addView(checkBtn)

        return row
    }

    private fun updateSetData(
        exIndex: Int,
        setIndex: Int,
        weightKg: Double? = null,
        reps: Int? = null,
        rpe: Double? = null,
        isCompleted: Boolean? = null
    ) {
        val exercises = activeRoutine.exercises.toMutableList()
        if (exIndex !in exercises.indices) return

        val targetEx = exercises[exIndex]
        val sets = targetEx.sets.toMutableList()
        if (setIndex !in sets.indices) return

        val currentSet = sets[setIndex]
        val updatedSet = currentSet.copy(
            weightKg = weightKg ?: currentSet.weightKg,
            reps = reps ?: currentSet.reps,
            rpe = rpe ?: currentSet.rpe,
            isCompleted = isCompleted ?: currentSet.isCompleted
        )
        sets[setIndex] = updatedSet
        exercises[exIndex] = targetEx.copy(sets = sets)
        activeRoutine = activeRoutine.copy(exercises = exercises)

        calculateVolume()
    }

    private fun addSetToExercise(exIndex: Int) {
        val exercises = activeRoutine.exercises.toMutableList()
        if (exIndex !in exercises.indices) return

        val targetEx = exercises[exIndex]
        val sets = targetEx.sets.toMutableList()
        val lastSet = sets.lastOrNull()

        val newSet = ExerciseSet(
            setNumber = sets.size + 1,
            weightKg = lastSet?.weightKg ?: 100.0,
            reps = lastSet?.reps ?: 5,
            rpe = lastSet?.rpe ?: 8.0,
            isCompleted = false
        )
        sets.add(newSet)
        exercises[exIndex] = targetEx.copy(sets = sets)
        activeRoutine = activeRoutine.copy(exercises = exercises)

        renderActiveRoutine()
    }

    private fun removeExercise(exIndex: Int) {
        val exercises = activeRoutine.exercises.toMutableList()
        if (exIndex in exercises.indices) {
            exercises.removeAt(exIndex)
            activeRoutine = activeRoutine.copy(exercises = exercises)
            renderActiveRoutine()
        }
    }

    private fun showAddExerciseDialog() {
        val items = catalogExercises.map { "${it.first} (${it.second})" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Seleccionar Ejercicio")
            .setItems(items) { _, which ->
                val selected = catalogExercises[which]
                val newExercise = RoutineExercise(
                    name = selected.first,
                    targetMuscles = selected.second,
                    sets = listOf(
                        ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, rpe = 8.0),
                        ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 5, rpe = 8.5)
                    )
                )
                val exercises = activeRoutine.exercises.toMutableList()
                exercises.add(newExercise)
                activeRoutine = activeRoutine.copy(exercises = exercises)
                renderActiveRoutine()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun calculateVolume() {
        var totalVolume = 0.0
        for (ex in activeRoutine.exercises) {
            for (set in ex.sets) {
                totalVolume += (set.weightKg * set.reps)
            }
        }
        volumeValueText.text = if (totalVolume >= 1000) {
            String.format(Locale.ROOT, "%.1fk kg", totalVolume / 1000.0)
        } else {
            String.format(Locale.ROOT, "%.0f kg", totalVolume)
        }
    }

    private fun loadSavedTemplates() {
        if (userId.isEmpty()) return

        scope.launch {
            val result = withContext(Dispatchers.IO) { routineRepository.getRoutines(userId) }
            result.onSuccess { routines ->
                savedTemplates = routines
                renderSavedTemplates()
            }.onFailure {
                renderSavedTemplates()
            }
        }
    }

    private fun renderSavedTemplates() {
        templatesContainer.removeAllViews()

        if (savedTemplates.isEmpty()) {
            templatesContainer.addView(TextView(context).apply {
                text = "No tienes plantillas guardadas aún. Crea una arriba y presiona Guardar."
                setTextColor(TEXT_ON_SURFACE_VARIANT)
                setTextSizeSp(13f)
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }

        for (routine in savedTemplates) {
            val itemRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = bgTextField()
                setPadding(dp(14), dp(12), dp(14), dp(12))

                val infoWrapper = LinearLayout(context).apply {
                    orientation = VERTICAL
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                    addView(TextView(context).apply {
                        text = routine.name
                        setTextColor(TEXT_ON_BACKGROUND)
                        setTextSizeSp(15f)
                        setTypefaceMedium()
                    })

                    val exerciseCount = routine.exercises.size
                    addView(TextView(context).apply {
                        text = "$exerciseCount Ejercicios • ${routine.estimatedTimeMins} mins"
                        setTextColor(TEXT_ON_SURFACE_VARIANT)
                        setTextSizeSp(12f)
                        typeface = Typeface.MONOSPACE
                    })
                }
                addView(infoWrapper)

                // Botón Cargar / Editar
                val loadBtn = TextView(context).apply {
                    text = "▶ Cargar"
                    setTextColor(PRIMARY_ACCENT)
                    setTextSizeSp(13f)
                    setTypefaceMedium()
                    setPadding(dp(8), dp(6), dp(8), dp(6))
                    setOnClickListener {
                        activeRoutine = routine.copy()
                        renderActiveRoutine()
                        context.toast("Plantilla '${routine.name}' cargada en el editor")
                    }
                }
                addView(loadBtn)

                // Botón Eliminar
                val deleteBtn = TextView(context).apply {
                    text = "🗑"
                    setTextSizeSp(16f)
                    setTextColor(ERROR_ACCENT)
                    setPadding(dp(8), dp(6), dp(8), dp(6))
                    setOnClickListener {
                        deleteTemplate(routine)
                    }
                }
                addView(deleteBtn)
            }
            templatesContainer.addView(itemRow)
            itemRow.setMargins(topDp = 8)
        }
    }

    private fun saveActiveRoutine() {
        val name = routineNameInput.text.toString().trim()
        if (name.isBlank()) {
            showError("Ingresa un nombre para la rutina.")
            return
        }
        if (userId.isEmpty()) {
            showError("No hay una sesión iniciada.")
            return
        }

        activeRoutine = activeRoutine.copy(name = name)
        saveButton.isEnabled = false
        saveButton.text = "Guardando..."

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                routineRepository.saveRoutine(userId, activeRoutine)
            }
            result.onSuccess { docId ->
                saveButton.isEnabled = true
                saveButton.text = "Guardar Rutina"
                errorText.visibility = GONE
                activeRoutine = activeRoutine.copy(id = docId)
                context.toast("¡Rutina '$name' guardada exitosamente! ✓")
                loadSavedTemplates()
            }.onFailure { e ->
                saveButton.isEnabled = true
                saveButton.text = "Guardar Rutina"
                showError(e.message ?: "Error al guardar la rutina.")
            }
        }
    }

    private fun deleteTemplate(routine: Routine) {
        if (routine.id.isBlank()) return

        AlertDialog.Builder(context)
            .setTitle("Eliminar Plantilla")
            .setMessage("¿Estás seguro de eliminar la plantilla '${routine.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        routineRepository.deleteRoutine(userId, routine.id)
                    }
                    result.onSuccess {
                        context.toast("Plantilla eliminada")
                        loadSavedTemplates()
                    }.onFailure { e ->
                        showError(e.message ?: "No se pudo eliminar la plantilla.")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
