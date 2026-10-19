package com.example.bankkurslari.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Three sizes only: Display (the one answer per screen), Body 17, Caption 13. No fourth size.
 * Secondary text is Caption/Body in VsInkSoft (ink at 55 %). Tabular lining figures everywhere so columns align.
 */
object VsType {
    private const val TNUM = "tnum, lnum"
    /** The one number answering the question. Heavy, tabular. */
    val Display = TextStyle(fontFamily = FontFamily.Default, fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, fontFeatureSettings = TNUM)
    val Body = TextStyle(fontFamily = FontFamily.Default, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)
    val Caption = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = TNUM)

    // Aliases kept for existing call sites; they map onto the three sizes above.
    val Hero = Display
    /** Sheet / section headings: Body size, bold weight. */
    val Title = Body.copy(fontWeight = FontWeight.SemiBold)
    val BodyMedium = Body
    val Meta = Caption
}
