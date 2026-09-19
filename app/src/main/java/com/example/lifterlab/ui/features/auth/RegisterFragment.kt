package com.example.lifterlab.ui.features.auth

import android.R as AndroidR
import android.view.Gravity
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.content.res.ColorStateList
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.example.lifterlab.*
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.ui.features.warmup.WarmupFragment
import com.example.lifterlab.toast
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.seed.FirestoreSeeder
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private val repository = AuthRepository()
    private val seeder = FirestoreSeeder()
    private var isLoading = false
    private var errorMessage: String? = null
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var etWeight: TextInputEditText
    private lateinit var etBarWeight: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvGoToLogin: TextView
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
            gravity = Gravity.TOP
            setPadding(dp(20f), dp(48f), dp(20f), dp(32f))
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        root.addView(cl)

        val badge = TextView(requireContext()).apply {
            text = "LIFTERLAB • NUEVO ATLETA"
            setTextColor(SECONDARY_ACCENT)
            setTextSizeSp(11f)
            setTypefaceMedium()
            background = bgTagChip()
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(badge)

        val title = TextView(requireContext()).apply {
            text = "Registro de Atleta"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(26f)
            setTypefaceMedium()
            setMargins(0, 10, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(title)

        val subtitle = TextView(requireContext()).apply {
            text = "Configura tu perfil para registrar tus cargas y récords"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(13f)
            setMargins(0, 4, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(subtitle)

        val card = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(BG_SURFACE_CONTAINER_LOW)
            radius = 12f
            strokeColor = OUTLINE_VARIANT
            strokeWidth = 1
            setMargins(0, 20, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        cardLayout.addView(TextView(requireContext()).apply {
            text = "DATOS PERSONALES"
            setTextColor(PRIMARY_CONTAINER)
            setTextSizeSp(11f)
            setTypefaceMedium()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })

        val tilName = TextInputLayout(requireContext()).apply {
            hint = "Nombre Completo"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etName = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilName.addView(etName)
        cardLayout.addView(tilName)

        val tilEmail = TextInputLayout(requireContext()).apply {
            hint = "Correo Electrónico"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etEmail = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilEmail.addView(etEmail)
        cardLayout.addView(tilEmail)

        val tilPassword = TextInputLayout(requireContext()).apply {
            hint = "Contraseña"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etPassword = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilPassword.addView(etPassword)
        cardLayout.addView(tilPassword)

        val tilConfirm = TextInputLayout(requireContext()).apply {
            hint = "Confirmar Contraseña"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etConfirmPassword = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        tilConfirm.addView(etConfirmPassword)
        cardLayout.addView(tilConfirm)

        cardLayout.addView(TextView(requireContext()).apply {
            text = "MÉTRICAS BASE DE POTENCIA (OPCIONAL)"
            setTextColor(TERTIARY_ACCENT)
            setTextSizeSp(11f)
            setTypefaceMedium()
            setMargins(0, 16, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setMargins(0, 10, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val tilWeight = TextInputLayout(requireContext()).apply {
            hint = "Peso Corporal (kg)"
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
        row.addView(tilWeight)

        val tilBarWeight = TextInputLayout(requireContext()).apply {
            hint = "Peso de Barra (kg)"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        etBarWeight = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(16f)
            setText("20")
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        tilBarWeight.addView(etBarWeight)
        row.addView(tilBarWeight)
        cardLayout.addView(row)

        btnRegister = MaterialButton(requireContext()).apply {
            text = "REGISTRAR ATLETA"
            setAllCaps(true)
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(13f)
            setTypefaceMedium()
            setBackgroundResource(0)
            background = bgPrimaryButton()
            setMargins(0, 22, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52f))
        }
        btnRegister.setOnClickListener {
            register()
        }
        cardLayout.addView(btnRegister)
        card.addView(cardLayout)
        cl.addView(card)

        tvGoToLogin = TextView(requireContext()).apply {
            text = "¿Ya tienes una cuenta? Inicia sesión aquí"
            setTextColor(PRIMARY_CONTAINER)
            setTextSizeSp(14f)
            setTypefaceMedium()
            setMargins(0, 20, 0, 24)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setOnClickListener {
                val containerId = (requireView().parent as? View)?.id ?: AndroidR.id.content
                parentFragmentManager.beginTransaction()
                    .replace(containerId, LoginFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        cl.addView(tvGoToLogin)

        progressBar = ProgressBar(requireContext()).apply {
            visibility = View.GONE
            indeterminateTintList = ColorStateList.valueOf(PRIMARY_ACCENT)
            setMargins(0, 0, 0, 24)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(progressBar)
        return root
    }

    private fun register() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString()
        val confirmPass = etConfirmPassword.text.toString()
        val bodyweight = etWeight.text.toString()
        val barWeight = etBarWeight.text.toString()

        if (name.isBlank() || email.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
            errorMessage = "Por favor, completa los campos requeridos."
            renderState()
            return
        }
        if (pass.length < 6) {
            errorMessage = "La contraseña debe tener al menos 6 caracteres."
            renderState()
            return
        }
        if (pass != confirmPass) {
            errorMessage = "Las contraseñas no coinciden."
            renderState()
            return
        }

        isLoading = true
        errorMessage = null
        renderState()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.register(email, pass, UserProfile(
                name = name,
                currentWeightKg = bodyweight.toDoubleOrNull() ?: 0.0,
                baseBarWeightKg = barWeight.toDoubleOrNull() ?: 20.0
            ))
            result.onSuccess { user ->
                seeder.seedInitialData(user.uid)
                isLoading = false
                requireView().toast("¡Registro exitoso! Bienvenido a LifterLab.")
                val containerId = (view?.parent as? View)?.id ?: AndroidR.id.content
                parentFragmentManager.beginTransaction()
                    .replace(containerId, WarmupFragment())
                    .commit()
            }.onFailure { error ->
                isLoading = false
                errorMessage = error.localizedMessage ?: "Error al registrar la cuenta."
                renderState()
            }
        }
    }

    private fun renderState() {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
        tvGoToLogin.isEnabled = !isLoading
if (errorMessage != null) {
            val msg = errorMessage
            requireView().toast(msg!!)
            errorMessage = null
        }
    }
}

