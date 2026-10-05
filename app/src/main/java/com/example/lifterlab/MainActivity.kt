package com.example.lifterlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.ui.components.LifterBottomNavBar
import com.example.lifterlab.ui.components.LifterTab
import com.example.lifterlab.ui.features.auth.LoginScreen
import com.example.lifterlab.ui.features.auth.RegisterScreen
import com.example.lifterlab.ui.features.dashboard.DashboardScreen
import com.example.lifterlab.ui.features.profile.ProfileScreen
import com.example.lifterlab.ui.features.routines.RoutinesScreen
import com.example.lifterlab.ui.theme.LifterLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifterLabTheme {
                AppNavigator()
            }
        }
    }
}

private sealed class AppScreen {
    object Login : AppScreen()
    object Register : AppScreen()
    object Dashboard : AppScreen()
    object Routines : AppScreen()
    object Profile : AppScreen()
}

@Composable
private fun AppNavigator() {
    val authRepository = remember { AuthRepository() }
    var screen by remember {
        mutableStateOf<AppScreen>(
            if (authRepository.isUserLoggedIn()) AppScreen.Dashboard else AppScreen.Login
        )
    }

    // La barra inferior es global: no se oculta al cambiar de módulo.
    val mostrarBarra = screen !is AppScreen.Login && screen !is AppScreen.Register
    val tabActual: LifterTab? = when (screen) {
        AppScreen.Dashboard -> LifterTab.Inicio
        AppScreen.Routines -> LifterTab.Rutinas
        AppScreen.Profile -> LifterTab.Perfil
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            when (screen) {
                AppScreen.Login -> LoginScreen(
                    onNavigateToRegister = { screen = AppScreen.Register },
                    onLoginSuccess = { screen = AppScreen.Dashboard }
                )
                AppScreen.Register -> RegisterScreen(
                    onNavigateToLogin = { screen = AppScreen.Login },
                    onRegisterSuccess = { screen = AppScreen.Dashboard }
                )
                AppScreen.Dashboard -> DashboardScreen(
                    onNavigateToRoutines = { screen = AppScreen.Routines },
                    onNavigateToProfile = { screen = AppScreen.Profile }
                )
                AppScreen.Routines -> RoutinesScreen(onBack = { screen = AppScreen.Dashboard })
                AppScreen.Profile -> ProfileScreen(
                    onBack = { screen = AppScreen.Dashboard },
                    onSignOut = {
                        authRepository.signOut()
                        screen = AppScreen.Login
                    }
                )
            }
        }

        if (mostrarBarra) {
            LifterBottomNavBar(
                currentTab = tabActual,
                onNavigateToDashboard = { screen = AppScreen.Dashboard },
                onNavigateToRoutines = { screen = AppScreen.Routines },
                onNavigateToProfile = { screen = AppScreen.Profile },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}