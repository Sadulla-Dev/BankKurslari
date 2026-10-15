package com.example.bankkurslari

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType

/* ---------- motion tokens (spec 5.4) ---------- */

internal object Motion {
    const val SHORT = 150
    const val MEDIUM = 250
    /** Material "emphasized decelerate": elements entering / settling. */
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Material "standard": state changes in place. */
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> color() = tween<T>(SHORT, easing = Standard)
    /** Indicator slide: quick, settles without overshoot. */
    fun <T> slide() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/** Subtle press feedback (0.97 scale) shared by buttons and chips. Ripple stays for rows. */
internal fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/* ---------- segmented control with a sliding thumb ---------- */

/**
 * Surface track + one sliding thumb. [cellWidth] = null spreads cells evenly across the width,
 * otherwise each cell is that wide and the control hugs its content.
 */
@Composable
internal fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cellWidth: Dp? = null,
    height: Dp = 44.dp
) {
    val pad = 3.dp
    val trackMod = if (cellWidth == null) modifier.fillMaxWidth() else modifier.width(cellWidth * options.size + pad * 2)
    BoxWithConstraints(
        trackMod.height(height).clip(RoundedCornerShape(12.dp)).background(VsSurface).padding(pad)
    ) {
        val w = cellWidth ?: (maxWidth / options.size)
        val x by animateDpAsState(w * selected.coerceIn(0, options.lastIndex), Motion.slide(), label = "thumb")
        Box(
            Modifier.offset(x = x).width(w).fillMaxHeight()
                .clip(RoundedCornerShape(10.dp)).background(VsBg)
                .border(1.dp, VsLine, RoundedCornerShape(10.dp))
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                val c by animateColorAsState(if (sel) VsInk else VsInkSoft, Motion.color(), label = "seg")
                Box(
                    Modifier.width(w).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) }
                        .semantics { role = Role.Tab; this.selected = sel },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, style = VsType.BodyMedium.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium),
                        color = c, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Shared by Kurs / Xarita / Tarix. USD · EUR · RUB as header tabs with a sliding accent underline (spec 4). */
@Composable
internal fun CurrencySegmented(currency: Currency, onCurrency: (Currency) -> Unit, modifier: Modifier = Modifier) {
    val cell = 72.dp
    val density = LocalDensity.current
    val idx = Currency.entries.indexOf(currency)
    val widths = remember { mutableStateMapOf<Int, Dp>() }
    val x by animateDpAsState(cell * idx, Motion.slide(), label = "ccy")
    val w by animateDpAsState(widths[idx] ?: 40.dp, Motion.slide(), label = "ccyw")
    Box(modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
        Row {
            Currency.entries.forEachIndexed { i, c ->
                val sel = c == currency
                val color by animateColorAsState(if (sel) VsInk else VsInkSoft, Motion.color(), label = "ccyc")
                Box(
                    Modifier.width(cell).height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCurrency(c) }
                        .semantics { role = Role.Tab; selected = sel },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        c.code, style = VsType.Title, color = color,
                        onTextLayout = { r -> widths[i] = with(density) { r.size.width.toDp() } }
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = x).padding(bottom = 6.dp)
                .width(w).height(2.dp).clip(RoundedCornerShape(1.dp)).background(VsEmerald)
        )
    }
}

/* ---------- chip ---------- */

/** Filter / quick-value chip: hairline outline, selected = accent outline + accent text. No fill, no check glyph. */
@Composable
internal fun VsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val src = remember { MutableInteractionSource() }
    val border by animateColorAsState(if (selected) VsEmerald else VsLine, Motion.color(), label = "chipb")
    val fg by animateColorAsState(if (selected) VsEmerald else VsInk, Motion.color(), label = "chipf")
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .pressScale(src)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(Color.Transparent)
                .border(if (selected) 1.5.dp else 1.dp, border, shape)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text, style = VsType.Meta.copy(fontWeight = FontWeight.Medium), color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            trailingContent?.invoke()
            if (trailing != null) Icon(trailing, null, tint = VsInkSoft, modifier = Modifier.size(16.dp))
        }
    }
}
