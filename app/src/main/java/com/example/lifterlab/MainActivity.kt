package com.example.lifterlab

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lifterlab.BG_BACKGROUND
import com.example.lifterlab.TEXT_ON_BACKGROUND
import com.example.lifterlab.TEXT_ON_SURFACE_VARIANT
import com.example.lifterlab.bgSurfaceCard
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.dp
import com.example.lifterlab.primaryButton
import com.example.lifterlab.secondaryButton
import com.example.lifterlab.setTextSizeSp
import com.example.lifterlab.setTypefaceMedium
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
                this,
                onNavigateToRegister = { showRegister() },
                onLoginSuccess = { email -> showHome(email) }
            )
        )
    }

    private fun showRegister() {
        setContentView(
            RegisterView(
                this,
                onNavigateToLogin = { showLogin() },
                onRegisterSuccess = { email -> showHome(email) }
            )
        )
    }

    private fun showHome(email: String) {
        setContentView(homeView(email))
    }

    private fun showProfile() {
        val email = authRepository.getCurrentUser()?.email ?: ""
        setContentView(
            ProfileView(
                this,
                onBack = { showHome(email) },
                onSignOut = {
                    authRepository.signOut()
                    showLogin()
                }
            )
        )
    }

    private fun homeView(email: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(BG_BACKGROUND)
        setPadding(dp(32), dp(48), dp(32), dp(24))

        addView(TextView(this@MainActivity).apply {
            text = "LifterLab"
            setTextColor(TEXT_ON_BACKGROUND)
            setTextSizeSp(30f)
            setTypefaceMedium()
            gravity = Gravity.CENTER
        })

        addView(TextView(this@MainActivity).apply {
            text = "Bienvenido a tu entrenamiento"
            setTextColor(TEXT_ON_SURFACE_VARIANT)
            setTextSizeSp(15f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(48))
        })

        val card = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            background = bgSurfaceCard()
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(TextView(this@MainActivity).apply {
                text = "Sesión iniciada como:"
                setTextColor(TEXT_ON_SURFACE_VARIANT)
                setTextSizeSp(13f)
            })
            addView(TextView(this@MainActivity).apply {
                text = email
                setTextColor(TEXT_ON_BACKGROUND)
                setTextSizeSp(18f)
                setTypefaceMedium()
                setPadding(0, dp(4), 0, 0)
            })
        }
        addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val profileButton = primaryButton("Ver Perfil").apply {
            setOnClickListener { showProfile() }
        }
        addView(
            profileButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(20) }
        )

        val signOutButton = secondaryButton("Cerrar Sesión").apply {
            setOnClickListener {
                authRepository.signOut()
                showLogin()
            }
        }
        addView(
            signOutButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(12) }
        )
    }
}