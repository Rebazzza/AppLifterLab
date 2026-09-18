package com.example.lifterlab.ui.features.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.content.res.ColorStateList
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.example.lifterlab.*
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.ui.features.auth.LoginFragment
import com.example.lifterlab.ui.features.warmup.WarmupFragment
import com.example.lifterlab.toast
import com.example.lifterlab.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class ProfileState(
    var name: String = "",
    var email: String = "",
    var birthYear: Int = 0,
    var gender: String = "",
    var heightCm: Double = 0.0,
    var currentWeightKg: Double = 0.0,
    var baseBarWeightKg: Double = 20.0,
    var isLoading: Boolean = false,
    var errorMessage: String? = null,
    var isSavedSuccessfully: Boolean = false
)

class ProfileFragment : Fragment() {
    private val state = ProfileState()
    private val repository = ProfileRepository()
    private val currentUserId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private lateinit var tvAthleteFullName: TextView
    private lateinit var tvAthleteEmail: TextView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var tvGenderValue: TextView
    private lateinit var tvBirthYearValue: TextView
    private lateinit var etBodyWeight: TextInputEditText
    private lateinit var etBarWeight: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnSignOut: MaterialButton
    private lateinit var progressBarProfile: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = ScrollView(requireContext()).apply {
            setFillViewport(true)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(BG_BACKGROUND)
        }
        val cl = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        cl.addView(View(requireContext()).apply {
            setBackgroundColor(BG_BACKGROUND)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(220f))
            background = bgProfileHeader()
        })

        val optionsBtn = ImageButton(requireContext()).apply {
            background = null
            setMargins(0, 16, 16, 0)
            layoutParams = LinearLayout.LayoutParams(dp(48f), dp(48f))
            setOnClickListener { showOptionsMenu() }
        }
        cl.addView(optionsBtn)

        val avatarCard = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(ColorStateList.valueOf(PRIMARY_ACCENT))
            radius = 44f
            layoutParams = LinearLayout.LayoutParams(dp(88f), dp(88f))
            setMargins(0, 56, 0, 0)
        }
        tvAvatarInitial = TextView(requireContext()).apply {
            text = "?"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(36f)
            setTypefaceMedium()
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        avatarCard.addView(tvAvatarInitial)
        cl.addView(avatarCard)

        tvAthleteFullName = TextView(requireContext()).apply {
            text = "Atleta"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(22f)
            setTypefaceMedium()
            gravity = Gravity.CENTER
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cl.addView(tvAthleteFullName)

        tvAthleteEmail = TextView(requireContext()).apply {
            text = ""
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(13f)
            gravity = Gravity.CENTER
            setMargins(0, 4, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cl.addView(tvAthleteEmail)

        cl.addView(View(requireContext()).apply {
            setBackgroundColor(OUTLINE_VARIANT)
            setMargins(0, 24, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 1)
        })

        cl.addView(TextView(requireContext()).apply {
            text = "Datos del Atleta"
            setTextColor(PRIMARY_ACCENT)
            setTextSizeSp(12f)
            setTypefaceMedium()
            setMargins(0, 24, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })

        val cardInfoFixed = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardInfoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val genderRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setMargins(0, 0, 0, 12)
        }
        genderRow.addView(TextView(requireContext()).apply {
            text = "Género"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(14f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        tvGenderValue = TextView(requireContext()).apply {
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(14f)
            setTypefaceMedium()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        genderRow.addView(tvGenderValue)
        cardInfoLayout.addView(genderRow)
        cardInfoLayout.addView(View(requireContext()).apply {
            setBackgroundColor(OUTLINE_VARIANT)
            setMargins(0, 0, 0, 12)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 1)
        })
        val birthRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setMargins(0, 0, 0, 12)
        }
        birthRow.addView(TextView(requireContext()).apply {
            text = "Año de nacimiento"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(14f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        tvBirthYearValue = TextView(requireContext()).apply {
            text = "—"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(14f)
            setTypefaceMedium()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        birthRow.addView(tvBirthYearValue)
        cardInfoLayout.addView(birthRow)
        cardInfoFixed.addView(cardInfoLayout)
        cl.addView(cardInfoFixed)

        val cardWeight = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val tilBodyWeight = TextInputLayout(requireContext()).apply {
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setPadding(dp(16f), dp(4f), dp(16f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etBodyWeight = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilBodyWeight.addView(etBodyWeight)
        cardWeight.addView(tilBodyWeight)
        cl.addView(cardWeight)

        val cardBarWeight = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val tilBarWeight = TextInputLayout(requireContext()).apply {
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setPadding(dp(16f), dp(4f), dp(16f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etBarWeight = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilBarWeight.addView(etBarWeight)
        cardBarWeight.addView(tilBarWeight)
        cl.addView(cardBarWeight)

        btnSaveProfile = MaterialButton(requireContext()).apply {
            text = "Guardar Cambios"
            setAllCaps(true)
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(15f)
            setTypefaceMedium()
            setBackgroundResource(0)
            background = bgPrimaryButton()
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52f))
        }
        btnSaveProfile.setOnClickListener {
            saveProfileChanges(currentUserId, etBodyWeight.text.toString(), etBarWeight.text.toString())
        }
        cl.addView(btnSaveProfile)

        progressBarProfile = ProgressBar(requireContext()).apply {
            visibility = View.GONE
            indeterminateTintList = ColorStateList.valueOf(PRIMARY_ACCENT)
            setMargins(0, 8, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(progressBarProfile)

        cl.addView(TextView(requireContext()).apply {
            text = "Configuración"
            setTextColor(PRIMARY_ACCENT)
            setTextSizeSp(12f)
            setTypefaceMedium()
            setMargins(0, 32, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })

        val cardConfig = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardConfigLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val unitRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setMargins(0, 0, 0, 12)
        }
        unitRow.addView(TextView(requireContext()).apply {
            text = "Unidad de Peso"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(14f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        unitRow.addView(TextView(requireContext()).apply {
            text = "Kilogramos (kg)"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(12f)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })
        unitRow.addView(SwitchMaterial(requireContext()).apply {
            setMargins(0, 0, 0, 0)
        })
        cardConfigLayout.addView(unitRow)
        cardConfig.addView(cardConfigLayout)
        cl.addView(cardConfig)

        btnSignOut = MaterialButton(requireContext()).apply {
            text = "Cerrar Sesión"
            setAllCaps(true)
            setTextColor(ERROR_ACCENT)
            setTextSizeSp(15f)
            setTypefaceMedium()
            setBackgroundResource(0)
            strokeColor = ColorStateList.valueOf(ERROR_ACCENT)
            strokeWidth = 1
            setMargins(0, 24, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52f))
        }
        btnSignOut.setOnClickListener { signOut() }
        cl.addView(btnSignOut)

        root.addView(cl)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (currentUserId.isNotEmpty()) loadProfile(currentUserId)
    }

    private fun loadProfile(userId: String) {
        state.isLoading = true
        state.errorMessage = null
        renderState()
        viewLifecycleOwner.lifecycleScope.launch {
            val email = repository.getCurrentUserEmail() ?: ""
            repository.getProfile(userId).onSuccess { profile ->
                state.isLoading = false
                state.name = profile.name
                state.email = email
                state.birthYear = profile.birthYear
                state.gender = profile.gender
                state.heightCm = profile.height
                state.currentWeightKg = profile.currentWeightKg
                state.baseBarWeightKg = profile.baseBarWeightKg
                renderState()
            }.onFailure { error ->
                state.isLoading = false
                state.errorMessage = error.localizedMessage ?: "Error al cargar el perfil."
                renderState()
            }
        }
    }

    private fun saveProfileChanges(userId: String, newWeightStr: String, newBarWeightStr: String) {
        val newWeight = newWeightStr.toDoubleOrNull()
        val newBarWeight = newBarWeightStr.toDoubleOrNull()
        if (newWeight == null || newWeight <= 0.0) {
            state.errorMessage = "Ingresa un peso corporal válido."
            renderState()
            return
        }
        if (newBarWeight == null || newBarWeight < 0.0) {
            state.errorMessage = "Ingresa un peso de barra válido."
            renderState()
            return
        }
        state.isLoading = true
        state.errorMessage = null
        renderState()
        viewLifecycleOwner.lifecycleScope.launch {
            repository.updateProfile(userId, UserProfile(
                name = state.name, birthYear = state.birthYear, gender = state.gender,
                height = state.heightCm, currentWeightKg = newWeight, baseBarWeightKg = newBarWeight
            )).onSuccess {
                state.isLoading = false
                state.currentWeightKg = newWeight
                state.baseBarWeightKg = newBarWeight
                state.isSavedSuccessfully = true
                renderState()
            }.onFailure { error ->
                state.isLoading = false
                state.errorMessage = error.localizedMessage ?: "Error al guardar."
                renderState()
            }
        }
    }

    private fun signOut() {
        repository.signOut()
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, LoginFragment())
            .commit()
    }

    private fun renderState() {
        progressBarProfile.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        btnSaveProfile.isEnabled = !state.isLoading
        btnSignOut.isEnabled = !state.isLoading
        tvAthleteFullName.text = state.name.ifEmpty { "Atleta" }
        tvAthleteEmail.text = state.email
        tvAvatarInitial.text = state.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        tvGenderValue.text = when (state.gender.lowercase()) {
            "male", "masculino", "m" -> "Masculino"
            "female", "femenino", "f" -> "Femenino"
            else -> state.gender.ifEmpty { "—" }
        }
        tvBirthYearValue.text = if (state.birthYear > 0) state.birthYear.toString() else "—"
        if (etBodyWeight.text.isNullOrEmpty()) {
            etBodyWeight.setText(if (state.currentWeightKg > 0.0)
                state.currentWeightKg.toBigDecimal().stripTrailingZeros().toPlainString() else "")
        }
        if (etBarWeight.text.isNullOrEmpty()) {
            etBarWeight.setText(if (state.baseBarWeightKg >= 0.0)
                state.baseBarWeightKg.toBigDecimal().stripTrailingZeros().toPlainString() else "")
        }
if (state.errorMessage != null) {
            val msg = state.errorMessage
            requireView().toast(msg!!)
            state.errorMessage = null
            renderState()
        }
        if (state.isSavedSuccessfully) {
            requireView().toast("Perfil actualizado correctamente ✓")
            state.isSavedSuccessfully = false
            renderState()
        }
    }

    private fun showOptionsMenu() {
        AlertDialog.Builder(requireContext())
            .setItems(arrayOf("Ver Perfil", "Warmup", "Cerrar Sesión")) { _, which ->
                when (which) {
                    0 -> {}
                    1 -> parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, WarmupFragment())
                        .commit()
                    2 -> signOut()
                }
            }.show()
    }
}


