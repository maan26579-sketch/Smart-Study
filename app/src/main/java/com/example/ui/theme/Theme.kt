package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPurplePrimary,
    onPrimary = DarkPurpleOnPrimary,
    primaryContainer = DarkPurplePrimaryContainer,
    onPrimaryContainer = DarkPurpleOnPrimaryContainer,
    secondary = DarkLavenderSecondary,
    onSecondary = DarkLavenderOnSecondary,
    secondaryContainer = DarkLavenderSecondaryContainer,
    onSecondaryContainer = DarkLavenderOnSecondaryContainer,
    tertiary = DarkRoseTertiary,
    onTertiary = DarkRoseOnTertiary,
    tertiaryContainer = DarkRoseTertiaryContainer,
    onTertiaryContainer = DarkRoseOnTertiaryContainer,
    background = DarkThemeBackground,
    onBackground = DarkThemeOnBackground,
    surface = DarkThemeSurface,
    onSurface = DarkThemeOnSurface,
    surfaceVariant = DarkThemeSurfaceVariant,
    onSurfaceVariant = DarkThemeOnSurfaceVariant,
    outline = DarkThemeOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    secondary = LavenderSecondary,
    onSecondary = LavenderOnSecondary,
    secondaryContainer = LavenderSecondaryContainer,
    onSecondaryContainer = LavenderOnSecondaryContainer,
    tertiary = RoseTertiary,
    onTertiary = RoseOnTertiary,
    tertiaryContainer = RoseTertiaryContainer,
    onTertiaryContainer = RoseOnTertiaryContainer,
    background = ThemeBackground,
    onBackground = ThemeOnBackground,
    surface = ThemeSurface,
    onSurface = ThemeOnSurface,
    surfaceVariant = ThemeSurfaceVariant,
    onSurfaceVariant = ThemeOnSurfaceVariant,
    outline = ThemeOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
