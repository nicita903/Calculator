package com.example.calculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    ), typography = Typography(
        displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    ), content = content)
}
