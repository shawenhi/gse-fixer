package com.gse.fixer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF002F6B),
    primaryContainer = Color(0xFF004592),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFBAC5DB),
    onSecondary = Color(0xFF222E42),
    secondaryContainer = Color(0xFF3A485C),
    onSecondaryContainer = Color(0xFFD6E1F5),
    tertiary = Color(0xFFE7BDF1),
    onTertiary = Color(0xFF472758),
    tertiaryContainer = Color(0xFF623E73),
    onTertiaryContainer = Color(0xFFFFD9FA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFA6C8FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001D4A),
    secondary = Color(0xFF525E73),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E1F5),
    onSecondaryContainer = Color(0xFF0D1A2C),
    tertiary = Color(0xFF7B568D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9FA),
    onTertiaryContainer = Color(0xFF2D103B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF1A73E8)
)

@Composable
fun Theme(
    darkTheme: Boolean = false, // 使用系统默认
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}