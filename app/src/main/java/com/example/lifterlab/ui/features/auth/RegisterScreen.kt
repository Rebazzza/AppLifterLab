package com.example.lifterlab.ui.features.auth

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.FieldLabel
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.LifterTextField
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            ScreenTitle("Crea tu cuenta", Modifier.padding(top = 32.dp))
            ScreenSubtitle("Configura tu perfil de atleta desde el inicio", Modifier.padding(top = 4.dp, bottom = 20.dp))

            LifterCard {
                FormField("Nombre", value = nombre, onValueChange = { nombre = it }, hint = "Tu nombre", keyboardType = KeyboardType.Text)
                FormField("Correo electrónico", value = correo, onValueChange = { correo = it }, hint = "tucorreo@example.com", keyboardType = KeyboardType.Email)
                FormField("Contraseña", value = password, onValueChange = { password = it }, hint = "Contraseña", isPassword = true)
                FormField("Año de nacimiento", value = birthYear, onValueChange = { birthYear = it }, hint = "Ej. 1995", keyboardType = KeyboardType.Number)
                FormField("Sexo", value = genero, onValueChange = { genero = it }, hint = "Masculino / Femenino / Otro")
                FormField("Altura (cm)", value = altura, onValueChange = { altura = it }, hint = "Altura en cm (Ej. 175)", keyboardType = KeyboardType.Decimal)
                FormField("Peso corporal (kg)", value = peso, onValueChange = { peso = it }, hint = "Peso actual en kg (Ej. 80)", keyboardType = KeyboardType.Decimal)

                if (mensajeError.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ErrorText(mensajeError, Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = if (cargando) "Creando cuenta..." else "Crear Cuenta",
                    onClick = {
                        cargando = true
                        mensajeError = ""
                        postRegister(
                            nombre = nombre,
                            correo = correo,
                            password = password,
                            birthYear = birthYear,
                            genero = genero,
                            altura = altura,
                            peso = peso,
                            scope = scope,
                            context = context,
                            authRepository = authRepository,
                            onError = { msg ->
                                cargando = false
                                mensajeError = msg
                            },
                            onSuccess = {
                                cargando = false
                                context.toast("¡Cuenta creada!")
                                onRegisterSuccess()
                            }
                        )
                    },
                    enabled = !cargando
                )
            }

            Text(
                text = "¿Ya tienes cuenta? Inicia sesión",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onNavigateToLogin() }
                    .padding(top = 20.dp)
            )
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    FieldLabel(label, Modifier.padding(top = 16.dp))
    Spacer(Modifier.height(4.dp))
    LifterTextField(
        value = value,
        onValueChange = onValueChange,
        label = hint,
        keyboardType = keyboardType,
        isPassword = isPassword
    )
}

private fun postRegister(
    nombre: String,
    correo: String,
    password: String,
    birthYear: String,
    genero: String,
    altura: String,
    peso: String,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    authRepository: AuthRepository,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    val name = nombre.trim()
    val email = correo.trim()
    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
        onError("Completa todos los campos.")
        return
    }
    if (password.length < 6) {
        onError("La contraseña debe tener al menos 6 caracteres.")
        return
    }
    val profile = UserProfile(
        name = name,
        birthYear = birthYear.toIntOrNull() ?: 0,
        gender = genero.trim(),
        height = altura.toDoubleOrNull() ?: 0.0,
        currentWeightKg = peso.toDoubleOrNull() ?: 0.0,
        baseBarWeightKg = 20.0
    )
    scope.launch {
        val result = withContext(Dispatchers.IO) { authRepository.register(email, password, profile) }
        result
            .onSuccess { onSuccess() }
            .onFailure { e -> onError(e.message ?: "No se pudo crear la cuenta.") }
    }
}