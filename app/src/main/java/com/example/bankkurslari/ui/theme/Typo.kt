package com.example.bankkurslari.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Four sizes only (spec 5.1): 32 / 20 / 16 / 13. Tabular figures on everything so digits align in columns. */
object VsType {
    private const val TNUM = "tnum"
    /** The one number answering the question. */
    val Hero = TextStyle(fontFamily = FontFamily.Default, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    /** Bank name, section title. */
    val Title = TextStyle(fontFamily = FontFamily.Default, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM)
    /** Row data, inputs. */
    val Body = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)
    val BodyMedium = Body.copy(fontWeight = FontWeight.Medium)
    /** Meta: time, address, distance (use VsInkSoft, contrast >= 4.5:1). */
    val Meta = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)
}
