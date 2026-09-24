package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF3D5A80),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E2F3),
    onPrimaryContainer = Color(0xFF12263F),
    secondary = Color(0xFF5E7260),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE8DC),
    onSecondaryContainer = Color(0xFF1C2A1E),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1D1B18),
    surface = Color(0xFFF7F5F0),
    onSurface = Color(0xFF1D1B18),
    surfaceVariant = Color(0xFFE9E5DC),
    onSurfaceVariant = Color(0xFF4A463F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2EFE8),
    surfaceContainer = Color(0xFFEDE9E1),
    surfaceContainerHigh = Color(0xFFE8E4DB),
    surfaceContainerHighest = Color(0xFFE2DED4),
    outline = Color(0xFF7C776D),
    outlineVariant = Color(0xFFCCC6BA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9C4E8),
    onPrimary = Color(0xFF0E2238),
    primaryContainer = Color(0xFF2B4262),
    onPrimaryContainer = Color(0xFFD6E2F3),
    secondary = Color(0xFFB5C8B3),
    onSecondary = Color(0xFF203020),
    secondaryContainer = Color(0xFF364535),
    onSecondaryContainer = Color(0xFFDDE8DC),
    background = Color(0xFF151412),
    onBackground = Color(0xFFE8E4DC),
    surface = Color(0xFF151412),
    onSurface = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF2E2B27),
    onSurfaceVariant = Color(0xFFC9C3B8),
    surfaceContainerLowest = Color(0xFF100F0D),
    surfaceContainerLow = Color(0xFF1C1B18),
    surfaceContainer = Color(0xFF211F1C),
    surfaceContainerHigh = Color(0xFF2B2926),
    surfaceContainerHighest = Color(0xFF363430),
    outline = Color(0xFF948E83),
    outlineVariant = Color(0xFF4A463F),
)

@Composable
fun VnutriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
