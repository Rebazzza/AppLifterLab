package com.example.lifterlab.ui.features.auth

import android.R as AndroidR
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
import com.example.lifterlab.BG_BACKGROUND
import com.example.lifterlab.BG_SURFACE_CONTAINER
import com.example.lifterlab.BG_SURFACE_CONTAINER_LOW
import com.example.lifterlab.OUTLINE_VARIANT
import com.example.lifterlab.PRIMARY_ACCENT
import com.example.lifterlab.PRIMARY_CONTAINER
import com.example.lifterlab.SECONDARY_ACCENT
import com.example.lifterlab.TEXT_ON_BACKGROUND
import com.example.lifterlab.TEXT_ON_SURFACE_VARIANT
import com.example.lifterlab.dp
import com.example.lifterlab.setTextSizeSp
import com.example.lifterlab.setTypefaceMedium
import com.example.lifterlab.setMargins
import com.example.lifterlab.toast
import com.example.lifterlab.bgTagChip
import com.example.lifterlab.bgPrimaryButton
import com.example.lifterlab.bgPrimaryContainer
import com.example.lifterlab.setPaddingH
import com.example.lifterlab.ui.features.warmup.WarmupFragment
import com.example.lifterlab.data.repository.AuthRepository
import android.view.Gravity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private val repository = AuthRepository()
    private var isLoading = false
    private var errorMessage: String? = null
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvGoToRegister: TextView
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
            setPadding(dp(24f), dp(48f), dp(24f), dp(32f))
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        root.addView(cl)

        val badge = TextView(requireContext()).apply {
            text = "LIFTERLAB • ACCESO"
            setTextColor(PRIMARY_CONTAINER)
            setTextSizeSp(11f)
            setTypefaceMedium()
            background = bgTagChip()
            setMargins(0, 24, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(badge)

        val title = TextView(requireContext()).apply {
            text = "Iniciar Sesión"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(26f)
            setTypefaceMedium()
            setMargins(0, 12, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(title)

        val subtitle = TextView(requireContext()).apply {
            text = "Bienvenido de vuelta a tu laboratorio de fuerza"
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
            setMargins(0, 28, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val cardLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        cardLayout.addView(TextView(requireContext()).apply {
            text = "CREDENCIALES DE ATLETA"
            setTextColor(SECONDARY_ACCENT)
            setTextSizeSp(11f)
            setTypefaceMedium()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        })

        val tilEmail = TextInputLayout(requireContext()).apply {
            hint = "Correo Electrónico"
            setBackgroundColor(BG_SURFACE_CONTAINER)
            boxStrokeColor = PRIMARY_ACCENT
            hintTextColor = ColorStateList.valueOf(TEXT_ON_SURFACE_VARIANT)
            setMargins(0, 16, 0, 0)
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
            setMargins(0, 14, 0, 0)
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

        btnLogin = MaterialButton(requireContext()).apply {
            text = "INICIAR SESIÓN"
            setAllCaps(true)
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(13f)
            setTypefaceMedium()
            setBackgroundResource(0)
            background = bgPrimaryButton()
            setMargins(0, 22, 0, 0)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52f))
        }
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            login(email, pass)
        }
        cardLayout.addView(btnLogin)
        card.addView(cardLayout)
        cl.addView(card)

        tvGoToRegister = TextView(requireContext()).apply {
            text = "¿No tienes una cuenta? Regístrate aquí"
            setTextColor(PRIMARY_CONTAINER)
            setTextSizeSp(14f)
            setTypefaceMedium()
            setMargins(0, 24, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setOnClickListener {
                val containerId = (requireView().parent as? View)?.id ?: AndroidR.id.content
                parentFragmentManager.beginTransaction()
                    .replace(containerId, RegisterFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        cl.addView(tvGoToRegister)

        progressBar = ProgressBar(requireContext()).apply {
            visibility = View.GONE
            indeterminateTintList = ColorStateList.valueOf(PRIMARY_ACCENT)
            setMargins(0, 8, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        cl.addView(progressBar)
        return root
    }

    private fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            errorMessage = "Por favor, completa todos los campos."
            renderState()
            return
        }
        isLoading = true
        errorMessage = null
        renderState()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.login(trimmedEmail, pass)
            result.onSuccess {
                isLoading = false
                navigateToWarmup()
            }.onFailure { error ->
                isLoading = false
                errorMessage = error.localizedMessage ?: "Error al iniciar sesión."
                renderState()
            }
        }
    }

    private fun navigateToWarmup() {
        val containerId = (view?.parent as? View)?.id ?: AndroidR.id.content
        parentFragmentManager.beginTransaction()
            .replace(containerId, WarmupFragment())
            .commit()
    }

    private fun renderState() {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
        tvGoToRegister.isEnabled = !isLoading
if (errorMessage != null) { val msg = errorMessage; requireView().toast(msg!!); errorMessage = null }
    }
}

