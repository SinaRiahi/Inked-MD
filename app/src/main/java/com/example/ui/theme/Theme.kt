package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InkedLightColorScheme = lightColorScheme(
    primary = MdAccent,
    onPrimary = Color.White,
    primaryContainer = MdAccentLight,
    onPrimaryContainer = MdAccentHover,
    secondary = Color(0xFF495057),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9ECEF),
    onSecondaryContainer = Color(0xFF212529),
    background = MdLightBg,
    onBackground = MdTextPrimaryLight,
    surface = MdLightSurface,
    onSurface = MdTextPrimaryLight,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = MdTextSecondaryLight,
    outline = MdLightBorder
)

private val InkedDarkColorScheme = darkColorScheme(
    primary = MdAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A3A6E),
    onPrimaryContainer = Color(0xFFD0DCFF),
    secondary = Color(0xFFA6A7AB),
    onSecondary = Color(0xFF141517),
    secondaryContainer = Color(0xFF2C2E33),
    onSecondaryContainer = Color(0xFFE9ECEF),
    background = MdDarkBg,
    onBackground = MdTextPrimaryDark,
    surface = MdDarkSurface,
    onSurface = MdTextPrimaryDark,
    surfaceVariant = Color(0xFF2C2E33),
    onSurfaceVariant = MdTextSecondaryDark,
    outline = MdDarkBorder
)

private val InkedOledColorScheme = darkColorScheme(
    primary = Color(0xFF748FFC),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1B2A58),
    onPrimaryContainer = Color(0xFFD0DCFF),
    secondary = Color(0xFFA0A2A6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF191A1D),
    onSecondaryContainer = Color(0xFFF1F3F5),
    background = MdOledBg,
    onBackground = Color(0xFFE1E2E5),
    surface = MdOledSurface,
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF141517),
    onSurfaceVariant = Color(0xFFA0A2A6),
    outline = MdOledBorder
)

@Composable
fun InkedMDTheme(
    themePreference: String = "system", // "light", "dark", "oled", "system"
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themePreference) {
        "oled" -> InkedOledColorScheme
        "dark" -> InkedDarkColorScheme
        "light" -> InkedLightColorScheme
        else -> if (systemDark) InkedDarkColorScheme else InkedLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
