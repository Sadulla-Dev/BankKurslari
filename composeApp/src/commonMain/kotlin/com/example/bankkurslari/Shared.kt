package com.example.bankkurslari

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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

object Motion {
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
fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/* ---------- text tabs with a sliding underline ---------- */

/**
 * Options as plain text; the active one is ink with a short underline, the rest ink at 55 %. No track, no thumb box.
 * [cellWidth] = null spreads cells evenly across the width, otherwise each cell is that wide and the row hugs its content.
 * selected = -1: nothing matches -> the underline fades out.
 */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cellWidth: Dp? = null,
    height: Dp = 48.dp
) {
    val trackMod = if (cellWidth == null) modifier.fillMaxWidth() else modifier.width(cellWidth * options.size)
    BoxWithConstraints(trackMod.height(height)) {
        val w = cellWidth ?: (maxWidth / options.size)
        val x by animateDpAsState(w * selected.coerceIn(0, options.lastIndex), Motion.slide(), label = "thumb")
        val thumbA by animateFloatAsState(if (selected in options.indices) 1f else 0f, tween(Motion.SHORT), label = "thumbA")
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                val c by animateColorAsState(if (sel) VsInk else VsInkSoft, Motion.color(), label = "seg")
                Box(
                    Modifier.width(w).fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) }
                        .semantics { role = Role.Tab; this.selected = sel },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = VsType.Body, color = c, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = x + w / 2 - 12.dp).padding(bottom = 6.dp)
                .width(24.dp).height(2.dp).graphicsLayer { alpha = thumbA }
                .clip(RoundedCornerShape(1.dp)).background(VsInk)
        )
    }
}

/** USD · EUR · RUB as plain text; active = ink, others ink at 55 %. */
@Composable
fun CurrencySegmented(currency: Currency, onCurrency: (Currency) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Currency.entries.forEach { c ->
            val sel = c == currency
            val color by animateColorAsState(if (sel) VsInk else VsInkSoft, Motion.color(), label = "ccyc")
            Box(
                Modifier.heightIn(min = 48.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCurrency(c) }
                    .semantics { role = Role.Tab; selected = sel },
                contentAlignment = Alignment.Center
            ) {
                Text(c.code, style = if (sel) VsType.Title else VsType.Body, color = color)
            }
        }
    }
}

/* ---------- chip ---------- */

/** Quiet text toggle: selected = ink + semibold, otherwise ink at 55 %. No outline, no fill. */
@Composable
fun VsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val src = remember { MutableInteractionSource() }
    val fg by animateColorAsState(if (selected) VsInk else VsInkSoft, Motion.color(), label = "chipf")
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .pressScale(src)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text, style = if (selected) VsType.Title else VsType.Body, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailingContent?.invoke()
        if (trailing != null) Icon(trailing, null, tint = VsInkSoft, modifier = Modifier.size(16.dp))
    }
}

/* ---------- canvas text (replaces android.graphics.Paint text) ---------- */

enum class TextAnchor { LEFT, CENTER, RIGHT }

/** Draws [text] with its baseline at [baselineY], anchored horizontally at [x]. */
fun DrawScope.drawTextAt(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    baselineY: Float,
    style: TextStyle,
    anchor: TextAnchor = TextAnchor.LEFT
) {
    val r = measurer.measure(text, style)
    val left = when (anchor) {
        TextAnchor.LEFT -> x
        TextAnchor.CENTER -> x - r.size.width / 2f
        TextAnchor.RIGHT -> x - r.size.width
    }
    drawText(r, topLeft = Offset(left, baselineY - r.firstBaseline))
}

fun TextMeasurer.widthOf(text: String, style: TextStyle): Float = measure(text, style).size.width.toFloat()
