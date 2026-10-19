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
    val heroBot: Color,
    // Redesign tokens (kurs-redesign): ground / card / hairline / teal tint / rose "less than best".
    val ground: Color,
    val card: Color,
    val hair: Color,
    val tile: Color,
    val tealTint: Color,
    val roseMark: Color,
    val roseText: Color,
    val roseTint: Color
)

val LightVsPalette = VsPalette(
    bg = Color(0xFFF1F3F5),
    surface = Color(0xFFF1F3F5),
    ink = Color(0xFF121A1F),
    inkSoft = Color(0xFF5A6670),
    line = Color(0xFFDDE2E7),
    emerald = Color(0xFF0A7C72),        // accent: best result / primary action / selected
    emeraldDeep = Color(0xFF0A7C72),
    emeraldSoft = Color(0xFFD3EEEA),    // subtle selected fill only, never a card background
    indigo = Color(0xFF565E64),
    indigoSoft = Color(0xFFF1F3F5),
    danger = Color(0xFFB4433A),         // worse than reference only
    amber = Color(0xFF8A5A00),          // warn: stale data
    heroTop = Color(0xFF0A7C72),
    heroMid = Color(0xFF0A7C72),
    heroBot = Color(0xFF0A7C72),
    ground = Color(0xFFF1F3F5),
    card = Color(0xFFFFFFFF),
    hair = Color(0xFFDDE2E7),
    tile = Color(0xFFE6EAEE),
    tealTint = Color(0xFFD3EEEA),
    roseMark = Color(0xFFE2857B),
    roseText = Color(0xFFB4433A),
    roseTint = Color(0xFFFBE6E3)
)

val DarkVsPalette = VsPalette(
    bg = Color(0xFF0E1113),
    surface = Color(0xFF171B1E),
    ink = Color(0xFFF2F4F5),
    inkSoft = Color(0xFFA3ACB2),
    line = Color(0xFF262C30),
    emerald = Color(0xFF3DD6A0),
    emeraldDeep = Color(0xFF3DD6A0),
    emeraldSoft = Color(0xFF12281F),
    indigo = Color(0xFFA3ACB2),
    indigoSoft = Color(0xFF171B1E),
    danger = Color(0xFFFF8A80),
    amber = Color(0xFFF2C14E),
    heroTop = Color(0xFF3DD6A0),
    heroMid = Color(0xFF3DD6A0),
    heroBot = Color(0xFF3DD6A0),
    ground = Color(0xFF0E1113),
    card = Color(0xFF171B1E),
    hair = Color(0xFF262C30),
    tile = Color(0xFF22282C),
    tealTint = Color(0xFF12332E),
    roseMark = Color(0xFFE2857B),
    roseText = Color(0xFFFF8A80),
    roseTint = Color(0xFF3A1F1C)
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
