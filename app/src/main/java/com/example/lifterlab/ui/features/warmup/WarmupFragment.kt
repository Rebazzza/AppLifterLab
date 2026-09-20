package com.example.lifterlab.ui.features.warmup

import android.R as AndroidR
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lifterlab.*
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.ui.features.auth.LoginFragment
import com.example.lifterlab.ui.features.profile.ProfileFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.round

data class WarmupState(
    var weight: String = "",
    var reps: String = "",
    var exerciseName: String = "Generic Exercise",
    var estimated1RM: Double? = null,
    var percentages: List<Pair<Int, Double>> = emptyList(),
    var isLoading: Boolean = false,
    var errorMessage: String? = null,
    var isSavedSuccessfully: Boolean = false
)

class WarmupFragment : Fragment() {
    private val state = WarmupState()
    private val repository = WarmupRepository()
    private lateinit var etExerciseName: TextInputEditText
    private lateinit var etWeight: TextInputEditText
    private lateinit var etReps: TextInputEditText
    private lateinit var btnCalculate: MaterialButton
    private lateinit var btnSaveSession: MaterialButton
    private lateinit var tv1RMResult: TextView
    private lateinit var llPercentagesContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = ScrollView(requireContext()).apply {
            isFillViewport = true
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(BG_BACKGROUND)
        }
        val cl = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingH(dp(16f), dp(16f))
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        root.addView(cl)

        val badge = TextView(requireContext()).apply {
            text = "LIFTERLAB • TITANIO Y ACERO"
            setTextColor(PRIMARY_CONTAINER)
            setTextSizeSp(11f)
            setTypefaceMedium()
            background = bgTagChip()
            setMargins(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(badge)

        val optionsBtn = ImageButton(requireContext()).apply {
            background = null
            setMargins(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dp(48f), dp(48f))
            setOnClickListener { showOptionsMenu() }
        }
        cl.addView(optionsBtn)

        val title = TextView(requireContext()).apply {
            text = "Series de Aproximación"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(24f)
            setTypefaceMedium()
            setMargins(0, 8, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(title)

        val subtitle = TextView(requireContext()).apply {
            text = "Calcula tu 1RM estimado y planifica la pirámide óptima de calentamiento."
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(13f)
            setMargins(0, 4, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(subtitle)

        val cardInputs = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 20, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardInputsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cardInputsLayout.addView(TextView(requireContext()).apply {
            text = "DATOS DEL LEVANTAMIENTO"
            setTextColor(SECONDARY_ACCENT)
            setTextSizeSp(11f)
            setTypefaceMedium()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })

        val tilName = TextInputLayout(requireContext()).apply {
            hint = "Nombre del Ejercicio"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etExerciseName = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilName.addView(etExerciseName)
        cardInputsLayout.addView(tilName)

        val row1 = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val tilWeight = TextInputLayout(requireContext()).apply {
            hint = "Peso (kg)"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setMargins(0, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        etWeight = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        tilWeight.addView(etWeight)
        row1.addView(tilWeight)

        val tilReps = TextInputLayout(requireContext()).apply {
            hint = "Repeticiones"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        etReps = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        tilReps.addView(etReps)
        row1.addView(tilReps)
        cardInputsLayout.addView(row1)

        btnCalculate = MaterialButton(requireContext()).apply {
            text = "CALCULAR 1RM Y APROXIMACIÓN"
            setAllCaps(true)
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(13f)
            setTypefaceMedium()
            setBackgroundResource(0)
            background = bgPrimaryButton()
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52f))
        }
        btnCalculate.setOnClickListener { calculate1RM() }
        cardInputsLayout.addView(btnCalculate)
        cardInputs.addView(cardInputsLayout)
        cl.addView(cardInputs)

        val cardResult = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardResultLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16f), dp(18f), dp(16f), dp(18f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cardResultLayout.addView(TextView(requireContext()).apply {
            text = "1RM ESTIMADO (FÓRMULA BRZYCKI)"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(11f)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })
        tv1RMResult = TextView(requireContext()).apply {
            text = "1RM Estimado: -- kg"
            setTextColor(SECONDARY_ACCENT)
            setTextSizeSp(22f)
            setTypefaceMedium()
            setMargins(0, 6, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cardResultLayout.addView(tv1RMResult)
        cardResult.addView(cardResultLayout)
        cl.addView(cardResult)

        val cardPyramid = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardPyramidLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cardPyramidLayout.addView(TextView(requireContext()).apply {
            text = "PIRÁMIDE DE APROXIMACIÓN"
            setTextColor(TERTIARY_ACCENT)
            setTextSizeSp(11f)
            setTypefaceMedium()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })
        llPercentagesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setMargins(0, 14, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cardPyramidLayout.addView(llPercentagesContainer)

        btnSaveSession = MaterialButton(requireContext()).apply {
            text = "GUARDAR SESIÓN DE APROXIMACIÓN"
            setAllCaps(true)
            setTextColor(BG_BACKGROUND)
            setTextSizeSp(13f)
            setTypefaceMedium()
            setBackgroundResource(0)
            background = bgPrimaryContainer()
            isEnabled = false
            setMargins(0, 14, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52f))
        }
        btnSaveSession.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
            saveSession(userId)
        }
        cardPyramidLayout.addView(btnSaveSession)
        cardPyramid.addView(cardPyramidLayout)
        cl.addView(cardPyramid)

        progressBar = ProgressBar(requireContext()).apply {
            visibility = View.GONE
            indeterminateTintList = ColorStateList.valueOf(PRIMARY_ACCENT)
            layoutParams = LinearLayout.LayoutParams(dp(48f), dp(48f))
        }
        cl.addView(progressBar)

        return root
    }

    private fun calculate1RM() {
        state.weight = etWeight.text?.toString()?.trim() ?: ""
        state.reps = etReps.text?.toString()?.trim() ?: ""
        state.exerciseName = etExerciseName.text?.toString()?.trim() ?: "Generic Exercise"

        val weight = state.weight.toDoubleOrNull()
        val reps = state.reps.toIntOrNull()
        if (weight == null || reps == null || weight <= 0 || reps <= 0) {
            state.errorMessage = "Por favor, ingresa valores válidos."
            renderState()
            return
        }
        val divisor = 1.0278 - (0.0278 * reps)
        if (divisor <= 0) {
            state.errorMessage = "Número de repeticiones muy alto."
            renderState()
            return
        }
        val estimated1RM = round((weight / divisor) * 10) / 10.0
        state.estimated1RM = estimated1RM
        state.percentages = listOf(70, 75, 80, 85, 90, 95).map { p ->
            Pair(p, round((estimated1RM * (p / 100.0)) * 10) / 10.0)
        }
        state.errorMessage = null
        renderState()
    }

    private fun saveSession(userId: String) {
        state.weight = etWeight.text?.toString()?.trim() ?: ""
        state.reps = etReps.text?.toString()?.trim() ?: ""
        state.exerciseName = etExerciseName.text?.toString()?.trim() ?: "Generic Exercise"

        val weight = state.weight.toDoubleOrNull()
        val reps = state.reps.toIntOrNull()
        val estimated1RM = state.estimated1RM
        if (weight == null || reps == null || estimated1RM == null) {
            state.errorMessage = "Calcula el 1RM primero."
            renderState()
            return
        }
        state.isLoading = true
        state.errorMessage = null
        renderState()
        viewLifecycleOwner.lifecycleScope.launch {
            val session = WorkoutSession(
                date = Timestamp.now(),
                routineName = "Sesión Libre de Aproximación",
                totalVolumeKg = weight * reps,
                isFreeSession = true,
                exercises = listOf(
                    RoutineExercise(
                        name = state.exerciseName.ifBlank { "Generic Exercise" },
                        sets = listOf(ExerciseSet(weightKg = weight, reps = reps, est1RM = estimated1RM, isPR = false))
                    )
                )
            )
            repository.saveWorkoutSession(userId, session).onSuccess {
                state.isLoading = false
                state.isSavedSuccessfully = true
                renderState()
            }.onFailure { e ->
                state.isLoading = false
                state.errorMessage = e.message ?: "Error al guardar."
                renderState()
            }
        }
    }

    private fun renderState() {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        btnCalculate.isEnabled = !state.isLoading
        btnSaveSession.isEnabled = !state.isLoading && state.estimated1RM != null
        if (state.estimated1RM != null) {
            tv1RMResult.text = "${state.estimated1RM} kg"
            renderPercentages(state.percentages)
        } else {
            tv1RMResult.text = "-- kg"
            llPercentagesContainer.removeAllViews()
        }
        if (state.errorMessage != null) {
            val msg = state.errorMessage
            state.errorMessage = null
            requireView().toast(msg!!)
        }
        if (state.isSavedSuccessfully) {
            requireView().toast("Sesión guardada exitosamente")
            etWeight.text?.clear()
            etReps.text?.clear()
            etExerciseName.text?.clear()
            state.weight = ""
            state.reps = ""
            state.exerciseName = "Generic Exercise"
            state.isSavedSuccessfully = false
            state.estimated1RM = null
            state.percentages = emptyList()
            renderState()
        }
    }

    private fun renderPercentages(percentages: List<Pair<Int, Double>>) {
        llPercentagesContainer.removeAllViews()
        for ((percent, weight) in percentages) {
            val innerLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }
            val card = MaterialCardView(requireContext()).apply {
                setCardBackgroundColor(BG_SURFACE_CONTAINER_HIGH)
                radius = 10f
                strokeColor = OUTLINE_VARIANT
                strokeWidth = 1
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }
            val percentageBadge = TextView(requireContext()).apply {
                text = "$percent%"
                setTextColor(PRIMARY_CONTAINER)
                setTextSizeSp(13f)
                setTypefaceMedium()
                setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
                background = bgPercentBadge()
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }
            val weightText = TextView(requireContext()).apply {
                text = "$weight kg"
                setTextColor(TEXT_ON_BACKGROUND)
                setTextSizeSp(17f)
                setTypefaceMedium()
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }
            val repsText = TextView(requireContext()).apply {
                text = when {
                    percent <= 70 -> "5 reps"
                    percent <= 75 -> "3-4 reps"
                    percent <= 80 -> "3 reps"
                    percent <= 85 -> "2 reps"
                    percent <= 90 -> "1 rep"
                    else -> "1 rep"
                }
                setTextColor(SECONDARY_ACCENT)
                setTextSizeSp(13f)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }
            val color = when {
                percent >= 90 -> ERROR_ACCENT
                percent >= 80 -> SECONDARY_ACCENT
                else -> PRIMARY_CONTAINER
            }
            percentageBadge.setTextColor(color)
            innerLayout.addView(percentageBadge)
            innerLayout.addView(weightText)
            innerLayout.addView(repsText)
            card.addView(innerLayout)
            llPercentagesContainer.addView(card)
        }
    }

    private fun showOptionsMenu() {
        val containerId = (view?.parent as? View)?.id ?: AndroidR.id.content
        AlertDialog.Builder(requireContext())
            .setItems(arrayOf("Ver Perfil", "Warmup", "Cerrar Sesión")) { _, which ->
                when (which) {
                    0 -> parentFragmentManager.beginTransaction()
                        .replace(containerId, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    1 -> {}
                    2 -> {
                        FirebaseAuth.getInstance().signOut()
                        parentFragmentManager.beginTransaction()
                            .replace(containerId, LoginFragment())
                            .commit()
                    }
                }
            }.show()
    }
}
