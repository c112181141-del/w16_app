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
    primary = OceanDeepBlue,
    secondary = OceanTeal,
    tertiary = SeafoamGreen,
    background = BackgroundDark,
    surface = BackgroundDark,
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = OceanDeepBlue,
    secondary = OceanTeal,
    tertiary = SeafoamGreen,
    background = BackgroundLight,
    surface = SurfaceCardLight,
    onPrimary = SurfaceCardLight,
    onSecondary = SurfaceCardLight,
    onBackground = TextDark,
    onSurface = TextDark,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce our custom high-fidelity Ocean Theme
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
