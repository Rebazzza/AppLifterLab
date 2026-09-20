package com.example.lifterlab

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.ui.features.auth.LoginView
import com.example.lifterlab.ui.features.auth.RegisterView
import com.example.lifterlab.ui.features.profile.ProfileView

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = authRepository.getCurrentUser()
        if (user != null) {
            showHome(user.email ?: "")
        } else {
            showLogin()
        }
    }

    private fun showLogin() {
        setContentView(
            LoginView(
                context = this,
                onNavigateToRegister = ::showRegister,
                onLoginSuccess = ::showHome
            )
        )
    }

    private fun showRegister() {
        setContentView(
            RegisterView(
                context = this,
                onNavigateToLogin = ::showLogin,
                onRegisterSuccess = ::showHome
            )
        )
    }

    private fun showHome(email: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_BACKGROUND)
            setPadding(dp(20), dp(48), dp(20), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "LifterLab"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(28f)
            setTypefaceMedium()
        })

        root.addView(TextView(this).apply {
            text = "Bienvenido, $email"
            textSize = 22f
            setTextColor(TEXT_ON_BACKGROUND)
            setPadding(0, dp(8), 0, 0)
        })

        root.addView(TextView(this).apply {
            text = "Tu plataforma para planificar, registrar y analizar tus entrenamientos."
            textSize = 14f
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setPadding(0, dp(4), 0, dp(20))
        })

        val btnVerPerfil = primaryButton("Ver Perfil")
        btnVerPerfil.setOnClickListener { showProfile() }
        root.addView(btnVerPerfil, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52)
        ))

        val btnCerrarSesion = secondaryButton("Cerrar Sesión")
        btnCerrarSesion.setOnClickListener {
            authRepository.signOut()
            showLogin()
        }
        root.addView(btnCerrarSesion, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52)
        ).apply { topMargin = dp(12) })

        root.addView(TextView(this).apply {
            text = "Recuerda que llevar una buena alimentación e hidratación es tan importante como el entrenamiento."
            textSize = 13f
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })

        setContentView(root)
    }

    private fun showProfile() {
        setContentView(
            ProfileView(
                context = this,
                onBack = ::showHomeBackPress,
                onSignOut = {
                    authRepository.signOut()
                    showLogin()
                }
            )
        )
    }

    private fun showHomeBackPress() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            showHome(user.email ?: "")
        } else {
            showLogin()
        }
    }
}