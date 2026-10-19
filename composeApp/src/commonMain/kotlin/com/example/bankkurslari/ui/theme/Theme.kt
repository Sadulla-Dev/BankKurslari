package com.example.bankkurslari.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.bankkurslari.generated.Res
import com.example.bankkurslari.generated.bricolage_bold
import com.example.bankkurslari.generated.bricolage_medium
import com.example.bankkurslari.generated.bricolage_semibold
import com.example.bankkurslari.generated.instrument_medium
import com.example.bankkurslari.generated.instrument_regular
import com.example.bankkurslari.generated.instrument_semibold
import org.jetbrains.compose.resources.Font

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

    val display = FontFamily(
        Font(Res.font.bricolage_medium, FontWeight.Medium),
        Font(Res.font.bricolage_semibold, FontWeight.SemiBold),
        Font(Res.font.bricolage_bold, FontWeight.Bold)
    )
    val body = FontFamily(
        Font(Res.font.instrument_regular, FontWeight.Normal),
        Font(Res.font.instrument_medium, FontWeight.Medium),
        Font(Res.font.instrument_semibold, FontWeight.SemiBold)
    )
    val fonts = remember(display, body) { VsFonts(display, body) }
    val typography = remember(body) { Typography().withFamily(body) }

    CompositionLocalProvider(LocalVsPalette provides vsPalette, LocalVsFonts provides fonts) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

private fun Typography.withFamily(f: FontFamily) = copy(
    displayLarge = displayLarge.fam(f), displayMedium = displayMedium.fam(f), displaySmall = displaySmall.fam(f),
    headlineLarge = headlineLarge.fam(f), headlineMedium = headlineMedium.fam(f), headlineSmall = headlineSmall.fam(f),
    titleLarge = titleLarge.fam(f), titleMedium = titleMedium.fam(f), titleSmall = titleSmall.fam(f),
    bodyLarge = bodyLarge.fam(f), bodyMedium = bodyMedium.fam(f), bodySmall = bodySmall.fam(f),
    labelLarge = labelLarge.fam(f), labelMedium = labelMedium.fam(f), labelSmall = labelSmall.fam(f)
)

private fun TextStyle.fam(f: FontFamily) = copy(fontFamily = f)
