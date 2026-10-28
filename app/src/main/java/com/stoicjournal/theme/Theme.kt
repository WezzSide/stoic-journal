package com.stoicjournal.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Amber700,
    onPrimary = Color.White,
    primaryContainer = Amber100,
    onPrimaryContainer = Amber900,
    secondary = Slate700,
    onSecondary = Color.White,
    secondaryContainer = Stone200,
    onSecondaryContainer = Stone900,
    tertiary = Slate600,
    onTertiary = Color.White,
    tertiaryContainer = Slate100,
    onTertiaryContainer = Slate900,
    background = Stone50,
    onBackground = Stone900,
    surface = Stone100,
    onSurface = Stone900,
    surfaceVariant = Stone200,
    onSurfaceVariant = Stone600,
    outline = Stone400
)

private val DarkColors = darkColorScheme(
    primary = Amber200,
    onPrimary = Stone900,
    primaryContainer = Amber800,
    onPrimaryContainer = Amber100,
    secondary = Stone200,
    onSecondary = Stone900,
    secondaryContainer = Stone600,
    onSecondaryContainer = Stone100,
    tertiary = Stone400,
    onTertiary = Stone900,
    tertiaryContainer = Stone800,
    onTertiaryContainer = Stone100,
    background = Stone900,
    onBackground = Stone100,
    surface = Stone800,
    onSurface = Stone100,
    surfaceVariant = Stone600,
    onSurfaceVariant = Stone200,
    outline = Stone400
)

@Composable
fun StoicJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StoicTypography,
        shapes = StoicShapes,
        content = content
    )
}
