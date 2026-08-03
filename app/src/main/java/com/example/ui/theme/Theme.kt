package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleTvAccent,
    onPrimary = Color.White,
    primaryContainer = AppleTvSurfaceVariant,
    onPrimaryContainer = AppleTvTextPrimary,
    secondary = AppleTvSilver,
    onSecondary = Color.Black,
    background = AppleTvBackground,
    onBackground = AppleTvTextPrimary,
    surface = AppleTvSurface,
    onSurface = AppleTvTextPrimary,
    surfaceVariant = AppleTvSurfaceVariant,
    onSurfaceVariant = AppleTvTextSecondary,
    outline = AppleTvGlassBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark mode for Apple TV+ glass aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
