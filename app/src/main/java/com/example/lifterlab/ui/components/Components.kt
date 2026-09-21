package com.example.lifterlab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardCornerRadius = 12.dp
val FieldCornerRadius = 12.dp

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun ScreenSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp
    )
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun CardCaption(text: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp
    )
}

@Composable
fun LifterCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .padding(20.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(CardCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Text(text = text, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(CardCornerRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(text = text, fontSize = 15.sp)
    }
}

@Composable
fun LifterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(FieldCornerRadius),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType
        ),
        visualTransformation = if (isPassword && !passwordVisible) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "🙈" else "👁",
                        fontSize = 16.sp
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun RowScope.ChippedTag(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeColor: Color = MaterialTheme.colorScheme.outlineVariant,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, strokeColor), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}