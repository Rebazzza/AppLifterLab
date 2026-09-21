package com.example.lifterlab.ui.features.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.FieldLabel
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.LifterTextField
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.SecondaryButton
import com.example.lifterlab.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var mensajecorreo by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LifterLab",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = "Inicia sesión para acceder a tu entrenamiento",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            LifterCard {
                FieldLabel("Correo electrónico")
                Spacer(Modifier.height(4.dp))
                LifterTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = "tucorreo@example.com",
                    keyboardType = KeyboardType.Email

                )


                Spacer(Modifier.height(16.dp))
                LifterTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    isPassword = true
                )

                if (mensajeError.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ErrorText(mensajeError, Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = if (cargando) "Ingresando..." else "Iniciar Sesión",
                    onClick = {
                        cargando = true
                        mensajeError = ""
                        postLogin(
                            correo = correo,
                            password = password,
                            scope = scope,
                            context = context,
                            authRepository = authRepository,
                            onError = { emp ->
                                cargando = false
                                mensajeError = emp
                            },
                            onSuccess = {
                                cargando = false
                                context.toast("¡Bienvenido de nuevo!")
                                onLoginSuccess()
                            }
                        )
                    },
                    enabled = !cargando
                )

                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = "¿Olvidaste tu contraseña?",
                    onClick = { context.toast("Pronto podrás recuperar tu contraseña") }
                )
            }

            Text(
                text = "¿No tienes cuenta? Regístrate",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onNavigateToRegister() }
                    .padding(top = 20.dp)
            )


        }
    }
}

private fun postLogin(
    correo: String,
    password: String,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    authRepository: AuthRepository,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    if(correo.endsWith("@gmail.com")){
    }else{
        onError("El correo tiene que ser gmail")
        return
    }

    if (correo.isBlank() || password.isBlank()) {
        onError("Ingresa tu correo y contraseña.")
        return
    }
    scope.launch {
        val result = withContext(Dispatchers.IO) { authRepository.login(correo, password) }
        result
            .onSuccess { onSuccess() }
            .onFailure { e -> onError(e.message ?: "Error al iniciar sesión.") }
    }
}