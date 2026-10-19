package com.example.bankkurslari.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Two typefaces: Bricolage Grotesque for numbers and titles, Instrument Sans for everything else. */
@Immutable
class VsFonts(val display: FontFamily, val body: FontFamily)

val LocalVsFonts = staticCompositionLocalOf { VsFonts(FontFamily.Default, FontFamily.Default) }

/**
 * Redesign type scale (kurs-redesign). Tabular figures everywhere.
 *  Hero 46/700 result - Display 40/700 screen hero - Amount 36/600 input - Title 20/600 section
 *  Value 17/600 row number - Body 15 - Meta 13 secondary - Tab 12 nav label.
 * Styles are read inside composition because the font families come from resources.
 */
object VsType {
    private const val TNUM = "tnum"
    private val fonts: VsFonts @Composable @ReadOnlyComposable get() = LocalVsFonts.current

    /** The one number answering the question. */
    val Hero: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.display, fontSize = 46.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.025).em, fontFeatureSettings = TNUM)
    /** Screen hero, e.g. Trends rate. */
    val Display: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.display, fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.025).em, fontFeatureSettings = TNUM)
    /** Amount input. */
    val Amount: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.display, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em, fontFeatureSettings = TNUM)
    /** Section title, bank name in a tray. */
    val Title: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.display, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.015).em, fontFeatureSettings = TNUM)
    /** Number in a row. */
    val Value: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.display, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    /** Row title, inputs. */
    val Body: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.body, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)
    val BodyMedium: TextStyle @Composable @ReadOnlyComposable get() = Body.copy(fontWeight = FontWeight.Medium)
    /** Meta: time, address, distance (use VsInkSoft, contrast >= 4.5:1). */
    val Meta: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.body, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)
    /** Bottom navigation label (600 when selected). */
    val Tab: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = fonts.body, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
}
