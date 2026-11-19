package com.example.bankkurslari

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsRoseMark
import com.example.bankkurslari.ui.theme.VsRoseText
import com.example.bankkurslari.ui.theme.VsTile
import com.example.bankkurslari.ui.theme.VsType
import com.example.bankkurslari.generated.Res
import com.example.bankkurslari.generated.bank_agrobank
import com.example.bankkurslari.generated.bank_aloqabank
import com.example.bankkurslari.generated.bank_anorbank
import com.example.bankkurslari.generated.bank_apexbank
import com.example.bankkurslari.generated.bank_asakabank
import com.example.bankkurslari.generated.bank_asia_alliance
import com.example.bankkurslari.generated.bank_brb
import com.example.bankkurslari.generated.bank_davrbank
import com.example.bankkurslari.generated.bank_garant_bank
import com.example.bankkurslari.generated.bank_hamkorbank
import com.example.bankkurslari.generated.bank_hayot_bank
import com.example.bankkurslari.generated.bank_infinbank
import com.example.bankkurslari.generated.bank_ipak_yuli
import com.example.bankkurslari.generated.bank_ipoteka_bank
import com.example.bankkurslari.generated.bank_kapitalbank
import com.example.bankkurslari.generated.bank_kdb_bank
import com.example.bankkurslari.generated.bank_mkbank
import com.example.bankkurslari.generated.bank_nbu
import com.example.bankkurslari.generated.bank_octobank
import com.example.bankkurslari.generated.bank_openbank
import com.example.bankkurslari.generated.bank_orient_finans
import com.example.bankkurslari.generated.bank_poytaxt_bank
import com.example.bankkurslari.generated.bank_sqb
import com.example.bankkurslari.generated.bank_tenge_bank
import com.example.bankkurslari.generated.bank_trastbank
import com.example.bankkurslari.generated.bank_turonbank
import com.example.bankkurslari.generated.bank_universal_bank
import com.example.bankkurslari.generated.bank_xalq_bank
import com.example.bankkurslari.generated.bank_ziraat_bank
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/*
 * Single design system (kurs-redesign). Every screen builds from these:
 *  surfaces: VsCardBox/VsPanel (24) - VsStat (16) - tray (30 top)
 *  controls: VsPrimaryButton (54/16) - VsSecondaryButton (54/16) - VsIconButton (44 circle)
 *  data: BankTile - VsNote - VsStat
 * Depth has three levels only: ground, card (hairline + 1px shadow), floating (long soft shadow).
 */

/** Text/icon colour on a filled accent surface: white on the light teal, dark ink on the dark-theme mint. */
val VsOnAccent: Color @Composable get() = if (VsEmerald.luminance() > 0.45f) Color(0xFF0E1113) else Color.White

object VsShape {
    val Card = RoundedCornerShape(24.dp)
    val Panel = RoundedCornerShape(24.dp)
    val Stat = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(16.dp)
    val Tile = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val Tray = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
}

/** Resting card: hairline + 1px shadow. */
@Composable
fun Modifier.vsCard(shape: Shape = VsShape.Card): Modifier =
    this.shadow(1.dp, shape, clip = false, ambientColor = Color(0x14121A1F), spotColor = Color(0x14121A1F))
        .clip(shape).background(VsCard).border(BorderStroke(1.dp, VsHair), shape)

/** Floating things (map capsules, pins, peek card): soft long shadow. */
@Composable
fun Modifier.vsFloating(shape: Shape): Modifier =
    this.shadow(16.dp, shape, clip = false, ambientColor = Color(0x38121A1F), spotColor = Color(0x38121A1F))
        .clip(shape).background(VsCard).border(BorderStroke(1.dp, VsHair), shape)

@Composable
fun VsCardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.vsCard(VsShape.Card).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/** Grouped list container (rows separated by ground hairlines). */
@Composable
fun VsPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.vsCard(VsShape.Panel), content = content)
}

@Composable
fun VsDivider(modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(VsHair.copy(alpha = 0.7f)))
}

/** Official bank mark, with initials as a fallback. */
@Composable
fun BankTile(bank: String, modifier: Modifier = Modifier, best: Boolean = false, size: Dp = 40.dp) {
    val shape = RoundedCornerShape(if (size >= 44.dp) 14.dp else if (size >= 40.dp) 12.dp else 11.dp)
    val logo = bankLogoResource(bank)
    Box(
        modifier.size(size).clip(shape)
            .background(if (logo != null) Color.White else if (best) VsEmerald else VsTile)
            .border(BorderStroke(if (best) 2.dp else 1.dp, if (best) VsEmerald else VsHair), shape),
        contentAlignment = Alignment.Center
    ) {
        if (logo != null) {
            Image(painterResource(logo), contentDescription = "$bank logo", contentScale = ContentScale.Fit, modifier = Modifier.size(size * 0.78f))
        } else {
            Text(bankInitials(bank), style = VsType.Meta.copy(fontFamily = VsType.Value.fontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (best) VsOnAccent else com.example.bankkurslari.ui.theme.VsInk.copy(alpha = 0.78f))
        }
    }
}

private fun bankInitials(name: String): String = name.trim().split(Regex("\\s+"))
    .filter(String::isNotBlank).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifEmpty { "?" }

private fun bankLogoResource(name: String): DrawableResource? {
    val key = name.lowercase().filter(Char::isLetterOrDigit)
    return when {
        "anor" in key -> Res.drawable.bank_anorbank
        "hayot" in key -> Res.drawable.bank_hayot_bank
        "tenge" in key -> Res.drawable.bank_tenge_bank
        "infin" in key -> Res.drawable.bank_infinbank
        "hamkor" in key -> Res.drawable.bank_hamkorbank
        "agro" in key -> Res.drawable.bank_agrobank
        "trast" in key || "trust" in key -> Res.drawable.bank_trastbank
        "universal" in key -> Res.drawable.bank_universal_bank
        "poytaxt" in key -> Res.drawable.bank_poytaxt_bank
        "ipoteka" in key -> Res.drawable.bank_ipoteka_bank
        "asiaalliance" in key -> Res.drawable.bank_asia_alliance
        "aloqa" in key -> Res.drawable.bank_aloqabank
        "garant" in key -> Res.drawable.bank_garant_bank
        "turon" in key -> Res.drawable.bank_turonbank
        "orientfinans" in key || "orientfinance" in key -> Res.drawable.bank_orient_finans
        "sanoatqurilish" in key || key == "sqb" -> Res.drawable.bank_sqb
        "mkbank" in key || "mikrokredit" in key -> Res.drawable.bank_mkbank
        "xalq" in key -> Res.drawable.bank_xalq_bank
        "ipakyuli" in key -> Res.drawable.bank_ipak_yuli
        key == "brb" -> Res.drawable.bank_brb
        "asaka" in key -> Res.drawable.bank_asakabank
        "davr" in key -> Res.drawable.bank_davrbank
        "apex" in key -> Res.drawable.bank_apexbank
        "milliy" in key || key == "nbu" -> Res.drawable.bank_nbu
        "openbank" in key -> Res.drawable.bank_openbank
        "ziraat" in key -> Res.drawable.bank_ziraat_bank
        "octo" in key -> Res.drawable.bank_octobank
        key.startsWith("kdb") -> Res.drawable.bank_kdb_bank
        "kapital" in key -> Res.drawable.bank_kapitalbank
        else -> null
    }
}

/** Round 44 dp icon button on a card surface with a hairline. */
@Composable
fun VsIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = VsInk, floating: Boolean = false) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier.size(44.dp).pressScale(src)
            .let { if (floating) it.vsFloating(CircleShape) else it.clip(CircleShape).background(VsCard).border(1.dp, VsHair, CircleShape) }
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
fun VsPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = 54.dp).pressScale(src).clip(VsShape.Button).background(VsEmerald)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, null, tint = VsOnAccent, modifier = Modifier.size(20.dp))
        Text(text, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = VsOnAccent, maxLines = 1)
    }
}

@Composable
fun VsSecondaryButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier.size(54.dp).pressScale(src).clip(VsShape.Button).background(VsGround)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = VsInk, modifier = Modifier.size(20.dp)) }
}

/** "1 500 less than Orient Finans": rose dot + text. Muted so it reads as "less", not "error". */
@Composable
fun VsNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(VsRoseMark))
        Text(text, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), color = VsRoseText)
    }
}

/** Ground tile: small label over a number with an optional delta line. */
@Composable
fun VsStat(label: String, value: String, modifier: Modifier = Modifier, delta: String? = null, deltaColor: Color = com.example.bankkurslari.ui.theme.VsEmerald) {
    Column(modifier.clip(VsShape.Stat).background(VsGround).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = VsType.Meta, color = VsInkSoft)
        Text(value, style = VsType.Title.copy(fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)), color = VsInk, maxLines = 1)
        if (delta != null) Text(delta, style = VsType.Meta.copy(fontFamily = VsType.Value.fontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)), color = deltaColor)
    }
}

/** Tray surface: 30 dp top radius, grab handle, long shadow. Content goes inside with 20 dp gutters. */
@Composable
fun VsTraySurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.shadow(20.dp, VsShape.Tray, clip = false, ambientColor = Color(0x59121A1F), spotColor = Color(0x59121A1F))
            .clip(VsShape.Tray).background(VsCard).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(VsHair))
        content()
    }
}

/** Light pill on the segmented-control track: green symbol circle + code + chevron. */
@Composable
fun VsCurrencyChip(symbol: String, code: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = 44.dp).pressScale(src).clip(CircleShape).background(com.example.bankkurslari.ui.theme.VsGround)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(start = 5.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(VsEmerald), contentAlignment = Alignment.Center) {
            Text(symbol, style = VsType.Value, color = VsOnAccent)
        }
        Text(code, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)), color = VsInk)
        Icon(androidx.compose.material.icons.Icons.Outlined.KeyboardArrowDown, null, tint = VsInkSoft, modifier = Modifier.size(16.dp))
    }
}

/** Compact teal action (40 dp) for in-row actions such as Directions. */
@Composable
fun VsPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val src = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.heightIn(min = 40.dp).pressScale(src).clip(shape).background(VsEmerald.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, null, tint = VsOnAccent, modifier = Modifier.size(18.dp))
        Text(text, style = VsType.Body.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = VsOnAccent, maxLines = 1)
    }
}

/** Dashed hairline outline for "empty / prompt" cards. */
@Composable
fun Modifier.vsDashedBorder(radius: Dp = 22.dp): Modifier {
    val c = VsHair
    return this.drawBehind {
        val sw = 1.5.dp.toPx()
        drawRoundRect(
            color = c, topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(sw, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        )
    }
}
