package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MonoColorScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoPanelGray,
    onPrimaryContainer = MonoWhite,
    secondary = MonoLightGray,
    onSecondary = MonoBlack,
    secondaryContainer = MonoDarkGray,
    onSecondaryContainer = MonoLightGray,
    tertiary = MonoMidGray,
    onTertiary = MonoWhite,
    background = MonoBlack,
    onBackground = MonoWhite,
    surface = MonoDarkGray,
    onSurface = MonoWhite,
    surfaceVariant = MonoPanelGray,
    onSurfaceVariant = MonoLightGray,
    outline = MonoMidGray,
    outlineVariant = MonoPanelGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonoColorScheme,
        typography = Typography,
        content = content
    )
}
