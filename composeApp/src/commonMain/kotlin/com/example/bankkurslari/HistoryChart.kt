package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.math.roundToLong

private val CHART_H = 220.dp

/** 1 week, 1 month, 3 months, 1 year, everything since 2009. */
private val RANGES: List<Int?> = listOf(7, 30, 90, 365, null)

private fun rateText(v: Double, ru: Boolean): String =
    if (v >= 1000) kGroup(v.roundToLong()) else v.fixed(2).let { if (ru) it.replace('.', ',') else it }

private fun signed(d: Double, ref: Double, ru: Boolean): String {
    val sign = if (d < 0) "−" else "+"
    val body = if (ref >= 1000) kGroup(abs(d).roundToLong()) else abs(d).fixed(2).let { if (ru) it.replace('.', ',') else it }
    return sign + body
}

/**
 * Tarix hero, Apple Stocks style: the CBU rate as the one big number, a muted change line, a full-bleed line chart
 * (no frame, two y-labels) and text range chips. Dragging along the chart replaces the number with that day's value.
 * [hPad] = side padding for the text parts; the chart itself runs edge to edge.
 */
@Composable
fun HistoryBlock(series: List<HistoryPoint>, currency: Currency, ru: Boolean, hPad: androidx.compose.ui.unit.Dp) {
    val haptic = LocalHapticFeedback.current
    var rangeIdx by rememberSaveable { mutableIntStateOf(1) }
    val last = epochDay(series.last().date)
    val days = RANGES[rangeIdx]
    val slice = remember(series, rangeIdx) {
        if (days == null) series else series.filter { epochDay(it.date) >= last - days }.ifEmpty { series.takeLast(2) }
    }.let { if (it.size < 2) series.takeLast(2) else it }

    var sel by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(sel) { if (sel != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(slice) { sel = null }

    val first = slice.first()
    val end = slice.last()
    val shown = sel?.let { slice.getOrNull(it) } ?: end

    val periodLabel = when (rangeIdx) {
        0 -> pick(lang, "7 kun", "7 дней", "7 days")
        1 -> pick(lang, "30 kun", "30 дней", "30 days")
        2 -> pick(lang, "3 oy", "3 месяца", "3 months")
        3 -> pick(lang, "1 yil", "1 год", "1 year")
        else -> pick(lang, "${first.date.take(4)}-yildan", "с ${first.date.take(4)} г.", "since ${first.date.take(4)}")
    }
    val caption = if (sel != null) {
        val d = end.rate - shown.rate
        formatFullDate(shown.date) + if (shown !== end) " · " + pick(lang, "bugungacha ", "до сегодня ", "to today ") + signed(d, end.rate, ru) else ""
    } else {
        val d = end.rate - first.rate
        val pct = if (first.rate > 0) d / first.rate * 100 else 0.0
        "${signed(d, end.rate, ru)} · ${abs(pct).fixed(2).let { if (ru) it.replace('.', ',') else it }}% · $periodLabel"
    }

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = hPad)) {
            Text("${currency.code} · " + pick(lang, "Markaziy bank", "Центральный банк", "Central Bank"), style = VsType.Caption, color = VsInkSoft)
            RollingNumber(
                (shown.rate * 100).roundToLong(), VsType.Display, VsInk,
                format = { rateText(it / 100.0, ru) }
            )
            AnimatedContent(caption, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) }, label = "cap") {
                Text(it, style = VsType.Caption, color = VsInkSoft)
            }
        }
        Chart(slice, ru, sel, onSel = { sel = it }, modifier = Modifier.padding(top = 24.dp))
        Segmented(
            options = when (lang) {
                Lang.RU -> listOf("1Н", "1М", "3М", "1Г", "Всё")
                Lang.EN -> listOf("1W", "1M", "3M", "1Y", "All")
                Lang.UZ_CYRL -> listOf("1Ҳ", "1О", "3О", "1Й", "Ҳаммаси")
                Lang.UZ -> listOf("1H", "1O", "3O", "1Y", "Hammasi")
            },
            selected = rangeIdx,
            onSelect = { rangeIdx = it },
            modifier = Modifier.padding(horizontal = hPad, vertical = 8.dp)
        )
    }
}

@Composable
private fun Chart(points: List<HistoryPoint>, ru: Boolean, sel: Int?, onSel: (Int?) -> Unit, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val ink = VsInk; val inkSoft = VsInkSoft; val bg = VsBg; val accent = VsEmerald
    val labelStyle = VsType.Caption.copy(color = inkSoft)
    val lo = points.minOf { it.rate }
    val hi = points.maxOf { it.rate }
    val ySpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val yMin by animateFloatAsState(lo.toFloat(), ySpring, label = "yMin")
    val yMax by animateFloatAsState(hi.toFloat(), ySpring, label = "yMax")
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(points) { reveal.snapTo(0f); reveal.animateTo(1f, tween(500, easing = Motion.Decelerate)) }
    val selA by animateFloatAsState(if (sel != null) 1f else 0f, tween(Motion.SHORT), label = "selA")
    var lastSel by remember { mutableStateOf<Int?>(null) }
    if (sel != null) lastSel = sel
    val livePoints by rememberUpdatedState(points)
    val desc = pick(lang, "Markaziy bank kursi grafigi", "График курса Центрального банка", "Central Bank rate chart") +
        ", ${formatFullDate(points.first().date)} – ${formatFullDate(points.last().date)}"

    Canvas(
        modifier.fillMaxWidth().height(CHART_H)
            .semantics { contentDescription = desc }
            .pointerInput(Unit) {
                fun idx(x: Float): Int {
                    val n = livePoints.size
                    return ((x / size.width) * (n - 1)).toInt().coerceIn(0, n - 1).let { i ->
                        if (i + 1 < n && (x / size.width) * (n - 1) - i > 0.5f) i + 1 else i
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onSel(idx(down.position.x))
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        val dx = ch.position.x - ch.previousPosition.x
                        val dy = ch.position.y - ch.previousPosition.y
                        // Horizontal drags scrub; vertical ones stay with the page scroll.
                        if (abs(dx) >= abs(dy)) ch.consume()
                        onSel(idx(ch.position.x))
                    }
                    onSel(null)
                }
            }
    ) {
        val w = size.width
        val top = 24.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val span = (yMax - yMin).takeIf { it > 1e-6f } ?: 1f
        fun x(i: Int) = if (points.size < 2) 0f else i * w / (points.size - 1)
        fun y(v: Double) = top + (1f - ((v.toFloat() - yMin) / span)) * (bottom - top)

        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(i), y(p.rate)) else path.lineTo(x(i), y(p.rate)) }
        clipRect(right = w * reveal.value) {
            drawPath(path, accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Two y-labels: the window's high and low, at the right edge.
        val pad = 16.dp.toPx()
        drawTextAt(measurer, rateText(hi, ru), w - pad, top - 8.dp.toPx(), labelStyle, TextAnchor.RIGHT)
        drawTextAt(measurer, rateText(lo, ru), w - pad, bottom + 18.dp.toPx(), labelStyle, TextAnchor.RIGHT)

        val si = lastSel
        if (si != null && si in points.indices && selA > 0f) {
            val sx = x(si)
            val sy = y(points[si].rate)
            drawLine(inkSoft.copy(alpha = selA * inkSoft.alpha), Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(bg.copy(alpha = selA), radius = 7.dp.toPx(), center = Offset(sx, sy))
            drawCircle(ink.copy(alpha = selA), radius = 4.5.dp.toPx(), center = Offset(sx, sy))
        } else if (reveal.value > 0.95f) {
            drawCircle(bg, radius = 6.dp.toPx(), center = Offset(x(points.lastIndex), y(points.last().rate)))
            drawCircle(accent, radius = 4.dp.toPx(), center = Offset(x(points.lastIndex), y(points.last().rate)))
        }
    }
}
