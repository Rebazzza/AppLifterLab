package com.example.lifterlab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.ui.features.auth.LoginView
import com.example.lifterlab.ui.features.auth.RegisterView
import com.example.lifterlab.ui.features.dashboard.DashboardView
import com.example.lifterlab.ui.features.profile.ProfileView
import com.example.lifterlab.ui.features.routines.RoutinesView

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = authRepository.getCurrentUser()
        if (user != null) {
            showDashboard()
        } else {
            showLogin()
        }
    }

    private fun showLogin() {
        setContentView(
            LoginView(
                context = this,
                onNavigateToRegister = ::showRegister,
                onLoginSuccess = { showDashboard() }
            )
        )
    }

    private fun showRegister() {
        setContentView(
            RegisterView(
                context = this,
                onNavigateToLogin = ::showLogin,
                onRegisterSuccess = { showDashboard() }
            )
        )
    }

    private fun showDashboard() {
        setContentView(
            DashboardView(
                context = this,
                onNavigateToRoutines = ::showRoutines,
                onNavigateToProfile = ::showProfile
            )
        )
    }

    private fun showRoutines() {
        setContentView(
            RoutinesView(
                context = this,
                onBack = ::showDashboard
            )
        )
    }

    private fun showProfile() {
        setContentView(
            ProfileView(
                context = this,
                onBack = ::showDashboard,
                onSignOut = {
                    authRepository.signOut()
                    showLogin()
                }
            )
        )
    }
}
