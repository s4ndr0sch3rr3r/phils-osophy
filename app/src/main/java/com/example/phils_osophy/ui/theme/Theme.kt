package com.example.phils_osophy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1B1B1B),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF55C900),
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
    outline = Color(0xFF666666)
)

@Composable
fun PhilsosophyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlackColorScheme,
        typography = Typography,
        content = content
    )
}
