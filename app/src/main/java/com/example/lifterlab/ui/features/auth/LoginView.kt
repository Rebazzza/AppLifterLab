package com.example.lifterlab.ui.features.auth

import android.content.Context
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

class LoginView(
    context: Context,
    private val onNavigateToRegister: () -> Unit,
    private val onLoginSuccess: (String) -> Unit,
    private val authRepository: AuthRepository = AuthRepository()
) : LinearLayout(context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val emailInput: EditText = context.textField(
        "tucorreo@example.com",
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    )
    private val passwordInput: TextInputLayout = context.passwordToggleField("Contraseña")
    private val errorText: TextView = TextView(context).apply {
        setTextColor(ERROR_ACCENT)
        setTextSizeSp(14f)
        visibility = GONE
    }
    private val loginButton: MaterialButton = context.primaryButton("Iniciar Sesión")

    private val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(48), dp(20), dp(16))

        addView(TextView(context).apply {
            text = "LifterLab"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(30f)
            setTypefaceMedium()
            gravity = Gravity.CENTER
        })

        addView(TextView(context).apply {
            text = "Inicia sesión para acceder a tu entrenamiento"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(24))
        })

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        card.addView(context.fieldLabel("Correo electrónico"))
        card.addView(emailInput)
        emailInput.setMargins(topDp = 4)

        card.addView(passwordInput)
        passwordInput.setMargins(topDp = 16)

        card.addView(errorText)
        errorText.setMargins(topDp = 8)

        loginButton.setOnClickListener { performLogin() }
        card.addView(
            loginButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { topMargin = dp(24) }
        )

        addView(card)

        addView(TextView(context).apply {
            text = "¿No tienes cuenta? Regístrate"
            textSize = 15f
            setTextColor(PRIMARY_ACCENT)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
            setOnClickListener { onNavigateToRegister() }
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

    private fun performLogin() {
        val email = emailInput.text.toString().trim()
        val pass = passwordInput.editText?.text.toString()
        if (email.isEmpty() || pass.isEmpty()) {
            showError("Ingresa tu correo y contraseña.")
            return
        }
        setLoading(true)
        scope.launch {
            val result = withContext(Dispatchers.IO) { authRepository.login(email, pass) }
            result
                .onSuccess { user ->
                    toast("¡Bienvenido de nuevo!")
                    onLoginSuccess(user.email ?: email)
                }
                .onFailure { e ->
                    setLoading(false)
                    showError(e.message ?: "Error al iniciar sesión.")
                }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        loginButton.text = if (loading) "Ingresando..." else "Iniciar Sesión"
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
    }
}