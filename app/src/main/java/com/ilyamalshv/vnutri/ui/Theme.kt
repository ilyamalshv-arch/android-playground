package com.ilyamalshv.vnutri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

/** Always the splash's world: dark velvet, pearl text, warm gold accents. */
private val Velvet = darkColorScheme(
    primary = Color(0xFFE9C9A0),
    onPrimary = Color(0xFF2A0F10),
    primaryContainer = Color(0xCC3A1518),
    onPrimaryContainer = Color(0xFFF3E9DA),
    secondary = Color(0xFFCDB8A6),
    onSecondary = Color(0xFF2A0F10),
    secondaryContainer = Color(0x992A1A22),
    onSecondaryContainer = Color(0xFFF3E9DA),
    background = Color(0xFF140708),
    onBackground = Color(0xFFF3E9DA),
    surface = Color(0xFF1B0A0C),
    onSurface = Color(0xFFF3E9DA),
    surfaceVariant = Color(0xFF2E1518),
    onSurfaceVariant = Color(0xFFCDB8A6),
    surfaceContainerLowest = Color(0x66100506),
    surfaceContainerLow = Color(0x9924100F),
    surfaceContainer = Color(0xAA2A1214),
    surfaceContainerHigh = Color(0xCC331618),
    surfaceContainerHighest = Color(0xE63A1A1D),
    outline = Color(0x88F3E9DA),
    outlineVariant = Color(0x33F3E9DA),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF2A0F10),
)

private val base = Typography()
private fun TextStyle.serif() = copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)

private val LodgeType = base.copy(
    displayLarge = base.displayLarge.serif(),
    displayMedium = base.displayMedium.serif(),
    displaySmall = base.displaySmall.serif(),
    headlineLarge = base.headlineLarge.serif(),
    headlineMedium = base.headlineMedium.serif(),
    headlineSmall = base.headlineSmall.serif(),
    titleLarge = base.titleLarge.serif(),
    titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif),
)

@Composable
fun VnutriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Velvet, typography = LodgeType, content = content)
}
