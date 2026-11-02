package ru.chsu.calories.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF1F3A6B)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F4),
    onPrimaryContainer = Navy,
    secondaryContainer = Color(0xFFE2E8F4),
    onSecondaryContainer = Navy,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F7),
    onSurfaceVariant = Color(0xFF5D6A7E),
    surfaceContainer = Color(0xFFF4F6F9),
    outline = Color(0xFFB4BDCA),
    outlineVariant = Color(0xFFD3D9E2),
    error = Color(0xFFB8352A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DAEEB),
    onPrimary = Color(0xFF0E1520),
    primaryContainer = Color(0xFF233149),
    onPrimaryContainer = Color(0xFFD9E3F7),
    secondaryContainer = Color(0xFF233149),
    onSecondaryContainer = Color(0xFFD9E3F7),
    background = Color(0xFF161D27),
    surface = Color(0xFF161D27),
    surfaceVariant = Color(0xFF1F2834),
    onSurfaceVariant = Color(0xFF94A1B4),
    surfaceContainer = Color(0xFF1B2330),
    outline = Color(0xFF475468),
    outlineVariant = Color(0xFF2C3746),
    error = Color(0xFFEE7468)
)

// цвета для БЖУ, воды и предупреждений
val ProteinColor = Color(0xFF3E6FC9)
val FatColor = Color(0xFFC98A1C)
val CarbsColor = Color(0xFF2F9A6E)
val WaterColor = Color(0xFF2F86C9)
val WarnColor = Color(0xFFC77A12)
val OkColor = Color(0xFF2C8452)

@Composable
fun CaloriesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
