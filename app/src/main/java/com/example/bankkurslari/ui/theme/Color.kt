package com.example.bankkurslari.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark scheme seeds (Material3 color scheme)
val Green80 = Color(0xFF8FD8BD)
val Blue80 = Color(0xFF9FC4F2)
val Amber80 = Color(0xFFFFD27A)

// Light scheme seeds
val Green40 = Color(0xFF0E9F6E)
val Blue40 = Color(0xFF4F46E5)
val Amber40 = Color(0xFFD1951D)

/** ValyutaStat design tokens. Resolved through [LocalVsPalette] so the whole UI
 *  can switch between light and dark without touching call sites. */
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

val LightVsPalette = VsPalette(
    bg = Color(0xFFF3F4F6),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF14161B),
    inkSoft = Color(0xFF6B7280),
    line = Color(0xFFEBEDF0),
    emerald = Color(0xFF0E9F6E),
    emeraldDeep = Color(0xFF0A7A54),
    emeraldSoft = Color(0xFFE6F6EF),
    indigo = Color(0xFF4F46E5),
    indigoSoft = Color(0xFFECEBFD),
    danger = Color(0xFFE5484D),
    amber = Color(0xFFF59E0B),
    heroTop = Color(0xFF0B3B36),
    heroMid = Color(0xFF117A5E),
    heroBot = Color(0xFF17A77E)
)

val DarkVsPalette = VsPalette(
    bg = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    ink = Color(0xFFE6EDF3),
    inkSoft = Color(0xFF9DA7B3),
    line = Color(0xFF273040),
    emerald = Color(0xFF2BD39A),
    emeraldDeep = Color(0xFF59E3B4),
    emeraldSoft = Color(0xFF14342A),
    indigo = Color(0xFF8B86F5),
    indigoSoft = Color(0xFF221F3D),
    danger = Color(0xFFFF6B6B),
    amber = Color(0xFFF5B942),
    heroTop = Color(0xFF0A2E2A),
    heroMid = Color(0xFF0F5C47),
    heroBot = Color(0xFF138A68)
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
