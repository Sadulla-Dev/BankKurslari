package com.example.bankkurslari.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark scheme seeds (Material3 color scheme)
val Green80 = Color(0xFF5FD3A0)
val Blue80 = Color(0xFFA5A6F6)
val Amber80 = Color(0xFFFFD27A)

// Light scheme seeds
val Green40 = Color(0xFF0B7A54)
val Blue40 = Color(0xFF5B5BD6)
val Amber40 = Color(0xFFF2B01E)

/** Qulay Kurs design tokens ("Mist"): cool-white ground, white cards, hairlines, banknote green accent,
 *  and a faint pastel wash at the top of each tab. Resolved through [LocalVsPalette] so the whole UI
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
    val heroBot: Color,
    // Redesign tokens (kurs-redesign): ground / card / hairline / teal tint / rose "less than best".
    val ground: Color,
    val card: Color,
    val hair: Color,
    val tile: Color,
    val tealTint: Color,
    val roseMark: Color,
    val roseText: Color,
    val roseTint: Color,
    // Pastel wash (sky -> lilac -> peach) behind the top of each tab, fading into ground.
    val washTop: Color,
    val washMid: Color,
    val washEnd: Color
)

val LightVsPalette = VsPalette(
    bg = Color(0xFFF5F6F8),
    surface = Color(0xFFF5F6F8),
    ink = Color(0xFF1C1D22),
    inkSoft = Color(0xFF5F6370),
    line = Color(0xFFE6E8EE),
    emerald = Color(0xFF0B7A54),        // accent: best result / primary action / selected
    emeraldDeep = Color(0xFF0B7A54),
    emeraldSoft = Color(0xFFE3F5EC),    // subtle selected fill only, never a card background
    indigo = Color(0xFF5F6370),
    indigoSoft = Color(0xFFF5F6F8),
    danger = Color(0xFFC2453A),         // worse than reference only
    amber = Color(0xFF8A5A00),          // warn: stale data
    heroTop = Color(0xFF0B7A54),
    heroMid = Color(0xFF0B7A54),
    heroBot = Color(0xFF0B7A54),
    ground = Color(0xFFF5F6F8),
    card = Color(0xFFFFFFFF),
    hair = Color(0xFFE6E8EE),
    tile = Color(0xFFEEF0F4),
    tealTint = Color(0xFFE3F5EC),
    roseMark = Color(0xFFEC8A7E),
    roseText = Color(0xFFC2453A),
    roseTint = Color(0xFFFCEBE8),
    washTop = Color(0xFFEAF2FF),
    washMid = Color(0xFFF1ECFF),
    washEnd = Color(0xFFFFF0EA)
)

val DarkVsPalette = VsPalette(
    bg = Color(0xFF111215),
    surface = Color(0xFF1A1B20),
    ink = Color(0xFFEDEEF2),
    inkSoft = Color(0xFFA3A6B1),
    line = Color(0xFF2A2C33),
    emerald = Color(0xFF5FD3A0),
    emeraldDeep = Color(0xFF5FD3A0),
    emeraldSoft = Color(0xFF173A2C),
    indigo = Color(0xFFA3A6B1),
    indigoSoft = Color(0xFF111215),
    danger = Color(0xFFFF8A80),
    amber = Color(0xFFF2C14E),
    heroTop = Color(0xFF5FD3A0),
    heroMid = Color(0xFF5FD3A0),
    heroBot = Color(0xFF5FD3A0),
    ground = Color(0xFF111215),
    card = Color(0xFF1A1B20),
    hair = Color(0xFF2A2C33),
    tile = Color(0xFF22242A),
    tealTint = Color(0xFF173A2C),
    roseMark = Color(0xFFE2857B),
    roseText = Color(0xFFFF8A80),
    roseTint = Color(0xFF3A1F1E),
    washTop = Color(0xFF16192A),
    washMid = Color(0xFF1C1730),
    washEnd = Color(0xFF24181A)
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

val VsGround: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.ground
val VsCard: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.card
val VsHair: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.hair
val VsTile: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.tile
val VsTealTint: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.tealTint
val VsRoseMark: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.roseMark
val VsRoseText: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.roseText
val VsRoseTint: Color @Composable @ReadOnlyComposable get() = LocalVsPalette.current.roseTint
