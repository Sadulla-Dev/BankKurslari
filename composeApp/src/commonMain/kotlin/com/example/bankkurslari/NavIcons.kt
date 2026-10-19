package com.example.bankkurslari

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Bottom-navigation icons drawn on a 24x24 grid (same geometry as the kurs-redesign SVGs, stroke 1.9).
 * [play] runs 0..1 once each time the tab becomes selected; at 1 the icon is at rest.
 *  - Rates: the coin flips about its vertical axis, the side ticks pop in.
 *  - Map: the pin drops in and bounces, the dot inside pulses.
 *  - Trends: the line draws itself left to right, the baseline fades up.
 */
@Composable
fun NavIcon(tab: HomeTab, tint: Color, play: Float, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(k, k, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (tab) {
                HomeTab.KURS -> rates(tint, stroke, play)
                HomeTab.MAP -> pin(tint, stroke, play)
                HomeTab.TREND -> trends(tint, stroke, play)
            }
        }
    }
}

private fun DrawScope.rates(c: Color, st: Stroke, t: Float) {
    drawRoundRect(c, Offset(3f, 6f), Size(18f, 12f), CornerRadius(3f), style = st)
    // Coin: full turn about its own vertical axis (scaleX cos), with a small hop.
    val turn = cosTurn(t)
    scale(turn, 1f, pivot = Offset(12f, 12f)) {
        drawCircle(c, 2.6f, Offset(12f, 12f), style = st)
    }
    // Ticks pop in from the middle after the coin has started turning.
    val pop = ((t - 0.25f) / 0.75f).coerceIn(0f, 1f)
    val len = easeOutBack(pop)
    val a = if (t >= 1f) 1f else (0.35f + 0.65f * pop)
    for (x in floatArrayOf(6.5f, 17.5f)) {
        scale(1f, len.coerceAtLeast(0.01f), pivot = Offset(x, 12f)) {
            drawLine(c.copy(alpha = c.alpha * a), Offset(x, 9.5f), Offset(x, 14.5f), st.width, StrokeCap.Round)
        }
    }
}

private fun cosTurn(t: Float): Float = kotlin.math.cos(t.coerceIn(0f, 1f) * 2 * PI).toFloat().let { if (kotlin.math.abs(it) < 0.06f) 0.06f * (if (it < 0) -1 else 1) else it }

private fun DrawScope.pin(c: Color, st: Stroke, t: Float) {
    // Drop from above with a damped bounce.
    val dy = if (t >= 1f) 0f else -9f * (1f - easeOutCubic((t / 0.55f).coerceIn(0f, 1f))) +
        if (t > 0.55f) -2.2f * sin(((t - 0.55f) / 0.45f) * PI).toFloat() else 0f
    val body = Path().apply {
        moveTo(12f, 21f)
        cubicTo(12f, 21f, 18f, 15.4f, 18f, 10f)
        arcTo(Rect(Offset(12f, 10f) - Offset(6f, 6f), Size(12f, 12f)), 0f, -180f, false)
        cubicTo(6f, 15.4f, 12f, 21f, 12f, 21f)
        close()
    }
    translate(0f, dy) {
        drawPath(body, c, style = st)
        val pulse = if (t >= 1f) 1f else 1f + 0.35f * sin(t * PI).toFloat()
        scale(pulse, pulse, pivot = Offset(12f, 10f)) {
            drawCircle(c, 2.4f, Offset(12f, 10f), style = st)
        }
    }
    // Ground shadow dot appears as the pin lands.
    val land = if (t >= 1f) 0f else easeOutCubic((t / 0.55f).coerceIn(0f, 1f)) * (1f - ((t - 0.55f) / 0.45f).coerceIn(0f, 1f))
    if (land > 0f) drawOval(c.copy(alpha = c.alpha * 0.35f * land), Offset(12f - 3.5f * land, 22.2f), Size(7f * land, 1.4f))
}

private fun DrawScope.trends(c: Color, st: Stroke, t: Float) {
    val line = Path().apply { moveTo(3f, 17f); lineTo(8f, 11f); lineTo(12f, 14f); lineTo(16f, 8f); lineTo(21f, 12f) }
    val base = Path().apply { moveTo(3f, 21f); lineTo(21f, 21f) }
    val draw = easeOutCubic(t.coerceIn(0f, 1f))
    val a = if (t >= 1f) 1f else (0.4f + 0.6f * t)
    drawPath(base, c.copy(alpha = c.alpha * a), style = st)
    if (draw >= 1f) {
        drawPath(line, c, style = st)
    } else {
        val m = PathMeasure().apply { setPath(line, false) }
        val seg = Path()
        m.getSegment(0f, m.length * draw, seg, true)
        drawPath(seg, c, style = st)
    }
}

private fun easeOutCubic(x: Float): Float { val i = 1f - x; return 1f - i * i * i }
private fun easeOutBack(x: Float): Float { val c1 = 1.70158f; val c3 = c1 + 1f; val i = x - 1f; return 1f + c3 * i * i * i + c1 * i * i }
