package com.example.lifterlab.ui.features.auth

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.lifterlab.BG_BACKGROUND
import com.example.lifterlab.ERROR_ACCENT
import com.example.lifterlab.PRIMARY_ACCENT
import com.example.lifterlab.TEXT_ON_BACKGROUND
import com.example.lifterlab.TEXT_ON_SURFACE_VARIANT
import com.example.lifterlab.bgSurfaceCard
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.dp
import com.example.lifterlab.fieldLabel
import com.example.lifterlab.primaryButton
import com.example.lifterlab.passwordToggleField
import com.example.lifterlab.setMargins
import com.example.lifterlab.setTextSizeSp
import com.example.lifterlab.setTypefaceMedium
import com.example.lifterlab.textField
import com.example.lifterlab.toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterView(
    context: Context,
    private val onNavigateToLogin: () -> Unit,
    private val onRegisterSuccess: (String) -> Unit,
    private val authRepository: AuthRepository = AuthRepository()
) : LinearLayout(context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val nameInput: EditText = context.textField(
        "Tu nombre",
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    )
    private val emailInput: EditText = context.textField(
        "tucorreo@example.com",
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    )
    private val passwordInput: TextInputLayout = context.passwordToggleField("Contraseña")
    private val birthInput: EditText = context.textField(
        "Ej. 1995",
        InputType.TYPE_CLASS_NUMBER
    ).apply { typeface = Typeface.MONOSPACE }
    private val genderInput: EditText = context.textField(
        "Masculino / Femenino / Otro",
        InputType.TYPE_CLASS_TEXT
    )
    private val heightInput: EditText = context.textField(
        "Altura en cm (Ej. 175)",
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    ).apply { typeface = Typeface.MONOSPACE }
    private val weightInput: EditText = context.textField(
        "Peso actual en kg (Ej. 80)",
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    ).apply { typeface = Typeface.MONOSPACE }
    private val errorText: TextView = TextView(context).apply {
        setTextColor(ERROR_ACCENT)
        setTextSizeSp(14f)
        visibility = GONE
    }
    private val registerButton: MaterialButton = context.primaryButton("Crear Cuenta")

    private val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(48), dp(20), dp(16))

        addView(TextView(context).apply {
            text = "Crea tu cuenta"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(28f)
            setTypefaceMedium()
        })

        addView(TextView(context).apply {
            text = "Configura tu perfil de atleta desde el inicio"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(14f)
            setPadding(0, dp(4), 0, dp(20))
        })

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        card.addField("Nombre", nameInput, topGap = 0)
        card.addField("Correo electrónico", emailInput)
        card.addView(passwordInput)
        passwordInput.setMargins(topDp = 16)
        card.addField("Año de nacimiento", birthInput)
        card.addField("Sexo", genderInput)
        card.addField("Altura (cm)", heightInput)
        card.addField("Peso corporal (kg)", weightInput)

        card.addView(errorText)
        errorText.setMargins(topDp = 8)

        registerButton.setOnClickListener { performRegister() }
        card.addView(
            registerButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { topMargin = dp(24) }
        )

        addView(card)

        addView(TextView(context).apply {
            text = "¿Ya tienes cuenta? Inicia sesión"
            textSize = 15f
            setTextColor(PRIMARY_ACCENT)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
            setOnClickListener { onNavigateToLogin() }
        })
    }

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(BG_BACKGROUND)

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun LinearLayout.addField(label: String, input: EditText, topGap: Int = 16) {
        val lbl = context.fieldLabel(label)
        addView(lbl)
        lbl.setMargins(topDp = topGap)
        addView(input)
        input.setMargins(topDp = 4)
    }

    private fun performRegister() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val pass = passwordInput.editText?.text.toString()
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Completa todos los campos.")
            return
        }
        if (pass.length < 6) {
            showError("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        setLoading(true)
        scope.launch {
            val profile = UserProfile(
                name = name,
                birthYear = birthInput.text.toString().toIntOrNull() ?: 0,
                gender = genderInput.text.toString().trim(),
                height = heightInput.text.toString().toDoubleOrNull() ?: 0.0,
                currentWeightKg = weightInput.text.toString().toDoubleOrNull() ?: 0.0,
                baseBarWeightKg = 20.0
            )
            val result = withContext(Dispatchers.IO) { authRepository.register(email, pass, profile) }
            result
                .onSuccess { user ->
                    toast("¡Cuenta creada!")
                    onRegisterSuccess(user.email ?: email)
                }
                .onFailure { e ->
                    setLoading(false)
                    showError(e.message ?: "No se pudo crear la cuenta.")
                }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        registerButton.isEnabled = !loading
        registerButton.text = if (loading) "Creando cuenta..." else "Crear Cuenta"
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
    }
}