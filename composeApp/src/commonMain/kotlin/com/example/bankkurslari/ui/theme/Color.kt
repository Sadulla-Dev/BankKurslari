package com.example.bankkurslari.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Material3 color scheme seeds (system widgets only: sheets, menus, radio buttons).
val Green80 = Color(0xFF6CC4B8)
val Blue80 = Color(0xFF9FC4F2)
val Amber80 = Color(0xFFFFD27A)
val Green40 = Color(0xFF0F6B63)
val Blue40 = Color(0xFF4F46E5)
val Amber40 = Color(0xFFD1951D)

/**
 * Design tokens. One ink, one accent. Secondary text, hairlines and quiet fills are the ink at lower opacity
 * (no separate grey tokens). The accent appears once per screen: on the answer or on the primary action.
 * Field names are historical; read `emerald` as "accent".
 */
data class VsPalette(
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val emerald: Color,
    val emeraldDeep: Color,
    val emeraldSoft: Color,
    val indigo: Color,
    val indigoSoft: Color,
    val danger: Color,
    val amber: Color,
    val heroTop: Color,
    val heroMid: Color,
    val heroBot: Color
)

private val InkLight = Color(0xFF111111)
private val AccentLight = Color(0xFF0F6B63)   // ink-teal: desaturated, reads as ink with a tint

val LightVsPalette = VsPalette(
    bg = Color(0xFFFAFAF8),
    surface = InkLight.copy(alpha = 0.04f),
    ink = InkLight,
    inkSoft = InkLight.copy(alpha = 0.55f),
    line = InkLight.copy(alpha = 0.08f),
    emerald = AccentLight,
    emeraldDeep = AccentLight,
    emeraldSoft = AccentLight.copy(alpha = 0.08f),
    indigo = InkLight.copy(alpha = 0.55f),
    indigoSoft = InkLight.copy(alpha = 0.04f),
    danger = Color(0xFFB3261E),
    amber = Color(0xFF8A5A00),          // real errors only (offline, stale data)
    heroTop = AccentLight,
    heroMid = AccentLight,
    heroBot = AccentLight
)

private val InkDark = Color(0xFFF2F2EF)
private val AccentDark = Color(0xFF6CC4B8)

val DarkVsPalette = VsPalette(
    bg = Color(0xFF111211),
    surface = InkDark.copy(alpha = 0.06f),
    ink = InkDark,
    inkSoft = InkDark.copy(alpha = 0.55f),
    line = InkDark.copy(alpha = 0.10f),
    emerald = AccentDark,
    emeraldDeep = AccentDark,
    emeraldSoft = AccentDark.copy(alpha = 0.12f),
    indigo = InkDark.copy(alpha = 0.55f),
    indigoSoft = InkDark.copy(alpha = 0.06f),
    danger = Color(0xFFFF8A80),
    amber = Color(0xFFF2C14E),
    heroTop = AccentDark,
    heroMid = AccentDark,
    heroBot = AccentDark
)

val LocalVsPalette = staticCompositionLocalOf { LightVsPalette }

// Token accessors — each reads the active palette from the composition.
val VsBg: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.bg
val VsSurface: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.surface
val VsInk: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.ink
val VsInkSoft: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.inkSoft
val VsLine: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.line
val VsEmerald: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.emerald
val VsEmeraldDeep: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.emeraldDeep
val VsEmeraldSoft: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.emeraldSoft
val VsIndigo: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.indigo
val VsIndigoSoft: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.indigoSoft
val VsDanger: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.danger
val VsAmber: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.amber
val VsHeroTop: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.heroTop
val VsHeroMid: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.heroMid
val VsHeroBot: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.heroBot
