package com.example.calculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TerminalColors {
    val Background = Color.Black
    val Blue = Color(0xFF2196FF)
    val DeepBlue = Color(0xFF10243A)
    val Text = Color.White
    val Secondary = Color(0xFF9299A5)
    val Button = Color(0xFF15171B)
    val Surface = Color(0xFF11151B)
    val Border = Color(0xFF263140)
}

@Composable
fun TerminalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = TerminalColors.Blue, background = TerminalColors.Background,
        surface = TerminalColors.Surface, onSurface = TerminalColors.Text,
        onBackground = TerminalColors.Text, error = TerminalColors.Blue,
    ), content = content)
}
