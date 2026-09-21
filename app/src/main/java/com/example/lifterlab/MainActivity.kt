package com.example.lifterlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lifterlab.data.repository.AuthRepository
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