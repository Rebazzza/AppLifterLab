package com.example.lifterlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lifterlab.data.model.ActiveSession
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.ui.components.ActiveSessionPanel
import com.example.lifterlab.ui.components.LifterBottomNavBar
import com.example.lifterlab.ui.components.LifterTab
import com.example.lifterlab.ui.features.auth.LoginScreen
import com.example.lifterlab.ui.features.auth.RegisterScreen
import com.example.lifterlab.ui.features.dashboard.DashboardScreen
import com.example.lifterlab.ui.features.history.HistoryScreen
import com.example.lifterlab.ui.features.tools.OneRepMaxScreen
import com.example.lifterlab.ui.features.tools.PlateLoaderScreen
import com.example.lifterlab.ui.features.tools.ToolEntry
import com.example.lifterlab.ui.features.profile.ProfileEditScreen
import com.example.lifterlab.ui.features.profile.ProfileViewScreen
import com.example.lifterlab.ui.features.routines.RoutinesScreen
import com.example.lifterlab.ui.features.tools.ToolsScreen
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
    object Tools : AppScreen()
    object Profile : AppScreen()
    object ProfileEdit : AppScreen()
    object History : AppScreen()
    object OneRepMax : AppScreen()
    object PlateLoader : AppScreen()

    // Entrenamiento en curso: se abre desde el panel flotante de sesión activa.
    object SesionEnCurso : AppScreen()
}

@Composable
private fun AppNavigator() {
    val authRepository = remember { AuthRepository() }

    // Herramienta que el módulo Herramientas abre directamente (acceso desde el Inicio).
    var herramientaInicial: ToolEntry? by remember { mutableStateOf(null) }

    // Arranque directo de un entrenamiento desde el acceso rápido del Inicio:
    // una rutina elegida al azar o la Rutina Express. El módulo Rutinas los consume
    // una sola vez y avisa con [onArranqueConsumido] para no repetirlos al volver.
    var rutinaParaIniciar: Routine? by remember { mutableStateOf(null) }
    var iniciarExpressDirecto by remember { mutableStateOf(false) }

    // Sesión que abre el panel flotante, y un contador que obliga a que cada apertura
    // sea una composición fresca (así el entrenamiento arranca con la sesión actual).
    var sesionParaRetomar: ActiveSession? by remember { mutableStateOf(null) }
    var aperturaSesion by remember { mutableStateOf(0) }

    // Pila de navegacion: el boton atras nativo retrocede en esta pila.
    val pila = remember {
        mutableStateListOf<AppScreen>(
            if (authRepository.isUserLoggedIn()) AppScreen.Dashboard else AppScreen.Login
        )
    }
    val screen = pila.last()

    fun navegarA(destino: AppScreen) {
        if (pila.last() != destino) pila.add(destino)
    }

    fun irAModulo(destino: AppScreen, tool: ToolEntry? = null) {
        if (pila.last() == destino && tool == null) return
        herramientaInicial = tool
        pila.clear()
        pila.add(destino)
    }

    fun volverA(destino: AppScreen) {
        while (pila.size > 1 && pila.last() != destino) pila.removeAt(pila.lastIndex)
        if (pila.last() != destino) {
            pila.clear()
            pila.add(destino)
        }
    }

    fun cerrarSesion() {
        authRepository.signOut()
        pila.clear()
        pila.add(AppScreen.Login)
    }

    // Abre el entrenamiento en curso desde el panel flotante. Siempre en una
    // composición fresca: así el runner arranca con la sesión que se está haciendo
    // ahora y no con la que pudo haberse visto la última vez.
    fun abrirSesionEnCurso(sesion: ActiveSession) {
        sesionParaRetomar = sesion
        aperturaSesion++
        if (pila.last() != AppScreen.SesionEnCurso) pila.add(AppScreen.SesionEnCurso)
    }

    // Acceso rápido del Inicio: abre el módulo Rutinas directamente en el entrenamiento.
    fun iniciarRutinaDesdeInicio(routine: Routine) {
        rutinaParaIniciar = routine
        iniciarExpressDirecto = false
        irAModulo(AppScreen.Routines)
    }

    fun iniciarExpressDesdeInicio() {
        rutinaParaIniciar = null
        iniciarExpressDirecto = true
        irAModulo(AppScreen.Routines)
    }

    fun limpiarArranqueDirecto() {
        rutinaParaIniciar = null
        iniciarExpressDirecto = false
    }

    // Boton atras nativo: retrocede en la pila; en la raiz, cierra la app.
    BackHandler(enabled = pila.size > 1) { pila.removeAt(pila.lastIndex) }

    // La barra inferior es global: no se oculta al cambiar de modulo.
    val mostrarBarra = screen !is AppScreen.Login && screen !is AppScreen.Register
    val tabActual: LifterTab? = when (screen) {
        AppScreen.Dashboard, AppScreen.History, AppScreen.OneRepMax, AppScreen.PlateLoader -> LifterTab.Inicio
        AppScreen.Routines, AppScreen.SesionEnCurso -> LifterTab.Rutinas
        AppScreen.Tools -> LifterTab.Herramientas
        AppScreen.Profile, AppScreen.ProfileEdit -> LifterTab.Perfil
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            when (screen) {
                AppScreen.Login -> LoginScreen(
                    onNavigateToRegister = { navegarA(AppScreen.Register) },
                    onLoginSuccess = { irAModulo(AppScreen.Dashboard) }
                )
                AppScreen.Register -> RegisterScreen(
                    onNavigateToLogin = { volverA(AppScreen.Login) },
                    onRegisterSuccess = { irAModulo(AppScreen.Dashboard) }
                )
                AppScreen.Dashboard -> DashboardScreen(
                    onNavigateToRoutines = { irAModulo(AppScreen.Routines) },
                    onNavigateToProfile = { irAModulo(AppScreen.Profile) },
                    onNavigateToHistory = { navegarA(AppScreen.History) },
                    onNavigateToOneRepMax = { navegarA(AppScreen.OneRepMax) },
                    onNavigateToPlateLoader = { navegarA(AppScreen.PlateLoader) },
                    onNavigateToTool = { tool -> irAModulo(AppScreen.Tools, tool) },
                    onStartRoutine = { routine -> iniciarRutinaDesdeInicio(routine) },
                    onStartExpress = { iniciarExpressDesdeInicio() }
                )
                AppScreen.History -> HistoryScreen(
                    // Se abre desde Inicio o desde Perfil: retrocede un nivel en la pila.
                    onBack = { if (pila.size > 1) pila.removeAt(pila.lastIndex) }
                )
                AppScreen.OneRepMax -> OneRepMaxScreen(onBack = { volverA(AppScreen.Dashboard) })
                AppScreen.PlateLoader -> PlateLoaderScreen(onBack = { volverA(AppScreen.Dashboard) })
                AppScreen.Routines -> RoutinesScreen(
                    onBack = { irAModulo(AppScreen.Dashboard) },
                    rutinaParaIniciar = rutinaParaIniciar,
                    iniciarExpress = iniciarExpressDirecto,
                    onArranqueConsumido = { limpiarArranqueDirecto() }
                )
                AppScreen.SesionEnCurso -> key(aperturaSesion) {
                    // Abierto desde el panel flotante: arranca directo en el
                    // entrenamiento en curso; al retroceder se sale del módulo.
                    RoutinesScreen(
                        onBack = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
                        sesionActiva = sesionParaRetomar
                    )
                }
                AppScreen.Tools -> ToolsScreen(
                    initialTool = herramientaInicial,
                    onBack = { irAModulo(AppScreen.Dashboard) }
                )
                AppScreen.Profile -> ProfileViewScreen(
                    onNavigateToEdit = { navegarA(AppScreen.ProfileEdit) },
                    onNavigateToHistory = { navegarA(AppScreen.History) },
                    onSignOut = { cerrarSesion() }
                )
                AppScreen.ProfileEdit -> ProfileEditScreen(
                    onBack = { volverA(AppScreen.Profile) }
                )
            }
        }

        if (mostrarBarra) {
            LifterBottomNavBar(
                currentTab = tabActual,
                onNavigateToDashboard = { irAModulo(AppScreen.Dashboard) },
                onNavigateToRoutines = { irAModulo(AppScreen.Routines) },
                onNavigateToTools = { irAModulo(AppScreen.Tools) },
                onNavigateToProfile = { irAModulo(AppScreen.Profile) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Panel flotante de la sesión en curso: se monta ENCIMA del menú inferior y se
        // ve en cualquier pantalla mientras haya una rutina iniciada; desaparece en el
        // momento en que esa sesión se termina o se descarta.
        if (mostrarBarra) {
            ActiveSessionPanel(
                onOpenSession = { sesion -> abrirSesionEnCurso(sesion) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}