package com.example.bankkurslari.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = Blue80,
    tertiary = Amber80
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = Blue40,
    tertiary = Amber40
)

@Composable
fun BankKurslariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand palette is intentionally fixed (no dynamic color) for a consistent look.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val vsPalette = if (darkTheme) DarkVsPalette else LightVsPalette

    CompositionLocalProvider(LocalVsPalette provides vsPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
