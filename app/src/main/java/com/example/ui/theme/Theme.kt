package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = ObsidianDeep,
    primaryContainer = ObsidianCardElevated,
    onPrimaryContainer = ArcCyan,
    secondary = CoreGold,
    onSecondary = ObsidianDeep,
    secondaryContainer = Color(0xFF241B05),
    onSecondaryContainer = CoreGold,
    tertiary = EmeraldOnline,
    onTertiary = ObsidianDeep,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = CrimsonAlert,
    onError = ObsidianDeep
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = JarvisTypography,
        content = content
    )
}
