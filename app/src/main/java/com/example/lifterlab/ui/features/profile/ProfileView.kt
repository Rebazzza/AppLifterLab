package com.example.lifterlab.ui.features.profile

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
import com.example.lifterlab.TEXT_ON_BACKGROUND
import com.example.lifterlab.TEXT_ON_SURFACE_VARIANT
import com.example.lifterlab.bgSurfaceCard
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.dp
import com.example.lifterlab.fieldLabel
import com.example.lifterlab.primaryButton
import com.example.lifterlab.secondaryButton
import com.example.lifterlab.setMargins
import com.example.lifterlab.setTextSizeSp
import com.example.lifterlab.setTypefaceMedium
import com.example.lifterlab.textField
import com.example.lifterlab.toast
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileView(
    context: Context,
    private val onBack: () -> Unit,
    private val onSignOut: () -> Unit,
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : LinearLayout(context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val userId: String = authRepository.getCurrentUser()?.uid.orEmpty()
    private val email: String = authRepository.getCurrentUser()?.email.orEmpty()

    private fun valueView(): TextView = TextView(context).apply {
        setTextColor(TEXT_ON_BACKGROUND)
        setTextSizeSp(16f)
        setTypefaceMedium()
    }

    private val nameValue: TextView = valueView()
    private val emailValue: TextView = valueView()
    private val birthValue: TextView = valueView()
    private val genderValue: TextView = valueView()

    private val heightInput: EditText = context.textField(
        "Ej. 175 cm",
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    ).apply { typeface = Typeface.MONOSPACE }
    private val weightInput: EditText = context.textField(
        "Ej. 80 kg",
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    ).apply { typeface = Typeface.MONOSPACE }

    private val errorText: TextView = TextView(context).apply {
        setTextColor(ERROR_ACCENT)
        setTextSizeSp(14f)
        visibility = GONE
    }
    private val loadingText: TextView = TextView(context).apply {
        text = "Cargando perfil..."
        setTextColor(TEXT_ON_SURFACE_VARIANT)
        setTextSizeSp(14f)
        gravity = Gravity.CENTER
        setPadding(0, dp(24), 0, dp(24))
    }
    private val saveButton: MaterialButton = context.primaryButton("Guardar Cambios")

    private fun LinearLayout.addInfoRow(label: String, valueView: TextView, labelTopGap: Int = 16) {
        val lbl = context.fieldLabel(label)
        addView(lbl)
        lbl.setMargins(topDp = labelTopGap)
        addView(valueView)
        valueView.setMargins(topDp = 4)
    }

    private val infoCard: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = bgSurfaceCard()
        setPadding(dp(20), dp(20), dp(20), dp(20))
        addInfoRow("Nombre", nameValue, labelTopGap = 0)
        addInfoRow("Correo", emailValue)
        addInfoRow("Año de nacimiento", birthValue)
        addInfoRow("Sexo", genderValue)
    }

    private val editCard: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = bgSurfaceCard()
        setPadding(dp(20), dp(20), dp(20), dp(20))

        addView(TextView(context).apply {
            text = "Medidas (editable)"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(17f)
            setTypefaceMedium()
        })

        addView(context.fieldLabel("Altura (cm)"))
        addView(heightInput)
        heightInput.setMargins(topDp = 4)

        val weightLabel = context.fieldLabel("Peso corporal (kg)")
        addView(weightLabel)
        weightLabel.setMargins(topDp = 16)
        addView(weightInput)
        weightInput.setMargins(topDp = 4)
    }

    private val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(48), dp(20), dp(16))

        addView(TextView(context).apply {
            text = "← Volver"
            textSize = 15f
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setOnClickListener { onBack() }
        })

        addView(TextView(context).apply {
            text = "Perfil"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(28f)
            setTypefaceMedium()
            setPadding(0, dp(8), 0, 0)
        })

        addView(loadingText)
        addView(infoCard)
        addView(editCard)

        addView(errorText)
        errorText.setMargins(topDp = 12)

        saveButton.setOnClickListener { saveChanges() }
        addView(
            saveButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(20) }
        )

        val signOutButton = context.secondaryButton("Cerrar Sesión").apply {
            setOnClickListener { onSignOut() }
        }
        addView(
            signOutButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(12) }
        )
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

        loadProfile()
    }

    private fun loadProfile() {
        if (userId.isEmpty()) {
            loadingText.text = "No hay una sesión iniciada."
            loadingText.visibility = VISIBLE
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            result
                .onSuccess { profile ->
                    loadingText.visibility = GONE
                    nameValue.text = profile.name.ifEmpty { "Sin nombre" }
                    emailValue.text = email.ifEmpty { "—" }
                    birthValue.text = if (profile.birthYear > 0) profile.birthYear.toString() else "—"
                    genderValue.text = profile.gender.ifEmpty { "—" }
                    heightInput.setText(profile.height.takeIf { it > 0 }?.toInt().toString())
                    weightInput.setText(profile.currentWeightKg.takeIf { it > 0 }?.toString().orEmpty())
                }
                .onFailure { e ->
                    loadingText.text = "No se pudo cargar el perfil."
                    showError(e.message ?: "Error de conexión o perfil inexistente.")
                }
        }
    }

    private fun saveChanges() {
        val height = heightInput.text.toString().trim().toDoubleOrNull()
        val weight = weightInput.text.toString().trim().toDoubleOrNull()
        if (height == null || height <= 0 || weight == null || weight <= 0) {
            showError("Ingresa una altura y un peso válidos.")
            return
        }
        if (userId.isEmpty()) {
            showError("No hay una sesión iniciada.")
            return
        }
        setSaving(true)
        scope.launch {
            val current = withContext(Dispatchers.IO) {
                profileRepository.getProfile(userId).getOrNull()
            }
            val updated = UserProfile(
                name = current?.name.orEmpty(),
                birthYear = current?.birthYear ?: 0,
                gender = current?.gender.orEmpty(),
                height = height,
                currentWeightKg = weight,
                baseBarWeightKg = current?.baseBarWeightKg ?: 20.0
            )
            val result = withContext(Dispatchers.IO) {
                profileRepository.updateProfile(userId, updated)
            }
            result
                .onSuccess {
                    setSaving(false)
                    errorText.visibility = GONE
                    toast("Perfil actualizado")
                }
                .onFailure { e ->
                    setSaving(false)
                    showError(e.message ?: "No se pudo guardar los cambios.")
                }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = VISIBLE
    }

    private fun setSaving(saving: Boolean) {
        saveButton.isEnabled = !saving
        saveButton.text = if (saving) "Guardando..." else "Guardar Cambios"
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
    }
}