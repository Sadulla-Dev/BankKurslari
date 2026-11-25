package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsRoseText
import com.example.bankkurslari.ui.theme.VsRoseTint
import com.example.bankkurslari.ui.theme.VsTealTint
import com.example.bankkurslari.ui.theme.VsType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

private const val MIN_SPAN_DAYS = 5.0
private val CHART_H = 240.dp

/** Visible time window of the chart, in epoch days. Clamped to the data's [first]..[last]. */
@Stable
private class ChartViewport(val first: Double, val last: Double, start: Double, end: Double) {
    var start by mutableDoubleStateOf(start)
    var end by mutableDoubleStateOf(end)
    val span get() = end - start
    private val total get() = (last - first).coerceAtLeast(MIN_SPAN_DAYS)

    fun set(s: Double, e: Double) {
        val sp = (e - s).coerceIn(min(MIN_SPAN_DAYS, total), total)
        var ns = s
        if (ns < first) ns = first
        if (ns + sp > last) ns = last - sp
        start = ns
        end = ns + sp
    }

    /** Pinch around [anchor] (0..1 of the plot width) by [zoom], then shift by [panFraction] of the plot width. */
    fun transform(zoom: Float, anchor: Float, panFraction: Float) {
        val sp = (span / zoom).coerceIn(min(MIN_SPAN_DAYS, total), total)
        val tAnchor = start + anchor * span
        var ns = tAnchor - anchor * sp
        ns -= panFraction * sp
        set(ns, ns + sp)
    }
}

/** Presets: 7 / 30 / 90 days, 1 and 5 years ending today, or the whole history (null). */
private val PRESETS: List<Int?> = listOf(7, 30, 90, 365, 1825, null)

/**
 * Tarix chart: preset ranges, pinch-zoom + two-finger pan on the chart, one-finger scrub with a bubble, and an optional "my amount" mode (values = CBU rate × the Kurs amount).
 */
@Composable
fun InteractiveHistory(
    series: List<HistoryPoint>,
    today: Double,
    currency: Currency,
    amount: Double,
    ru: Boolean,
    unit: String,
    /** Hero caption; the CBU rate by default. */
    title: String? = null,
    /** false for series where "× my amount" means nothing (gold per gram): hides the 1 / amount chip. */
    allowAmount: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val t = remember(series) { series.map { epochDay(it.date).toDouble() } }
    val first = t.first()
    val last = t.last()
    val vp = remember(series) { ChartViewport(first, last, max(first, last - 30), last) }
    var anim by remember { mutableStateOf<Job?>(null) }
    fun goTo(s: Double, e: Double) {
        anim?.cancel()
        val s0 = vp.start; val e0 = vp.end
        anim = scope.launch {
            animate(0f, 1f, animationSpec = tween(Motion.MEDIUM + 100, easing = Motion.Standard)) { f, _ ->
                vp.set(s0 + (s - s0) * f, e0 + (e - e0) * f)
            }
        }
    }
    fun stopAnim() { anim?.cancel(); anim = null }

    val amt = if (amount > 0) amount else 100.0
    var amountModePref by remember { mutableStateOf(appStore().getBoolean("chart_amount_mode", false)) }
    val amountMode = allowAmount && amountModePref
    val mult = if (amountMode) amt else 1.0
    var hintSeen by remember { mutableStateOf(appStore().getBoolean("chart_hint_seen", false)) }
    fun markInteracted() {
        if (!hintSeen) { hintSeen = true; appStore().putBoolean("chart_hint_seen", true) }
    }

    // Visible slice (+1 point either side so the line runs to the plot edges).
    val from = t.indexOfFirst { it >= vp.start }.let { if (it <= 0) 0 else it - 1 }
    val to = t.indexOfLast { it <= vp.end }.let { if (it < 0) t.lastIndex else min(t.lastIndex, it + 1) }
    val inside = (from..to).filter { t[it] >= vp.start - 1e-6 && t[it] <= vp.end + 1e-6 }.ifEmpty { listOf(from, to) }
    val lo = inside.minOf { series[it].rate }
    val hi = inside.maxOf { series[it].rate }

    // ---- presets (computed first: the hero names the period) ----
    val labels = when (lang) {
        Lang.RU -> listOf("1Н", "1М", "3М", "1Г", "5Л", "Всё")
        Lang.UZ_CYRL -> listOf("1Ҳ", "1О", "3О", "1Й", "5Й", "Ҳамма")
        Lang.EN -> listOf("1W", "1M", "3M", "1Y", "5Y", "All")
        Lang.UZ -> listOf("1H", "1O", "3O", "1Y", "5Y", "Hamma")
    }
    val presetIdx = PRESETS.indexOfFirst { p ->
        val want = if (p == null) last - first else min(p.toDouble(), last - first)
        abs(vp.end - last) < 0.5 && abs(vp.span - want) < 1.0
    }

    // ---- header: title + change over the visible window ----
    val a = series[inside.first()]
    val b = series[inside.last()]
    val pct = if (a.rate > 0) (b.rate - a.rate) / a.rate * 100 else 0.0
    val gain = b.rate >= a.rate
    val pctText = abs(pct).fixed(2).let { if (lang == Lang.EN) it else it.replace('.', ',') }
    val deltaText = if (amountMode) {
        val d = (b.rate - a.rate) * amt
        "${fmtSom(abs(d), 1000.0)} $unit · $pctText%"
    } else "${fmtSom(abs(b.rate - a.rate), today)} $unit · $pctText%"
    val periodText = when (presetIdx) {
        0 -> pick(lang, "oxirgi 7 kunda", "за 7 дней", "over 7 days")
        1 -> pick(lang, "oxirgi 30 kunda", "за 30 дней", "over 30 days")
        2 -> pick(lang, "oxirgi 3 oyda", "за 3 месяца", "over 3 months")
        3 -> pick(lang, "oxirgi 1 yilda", "за год", "over 1 year")
        4 -> pick(lang, "oxirgi 5 yilda", "за 5 лет", "over 5 years")
        5 -> pick(lang, "butun davr uchun", "за всё время", "over all time")
        else -> "${shortDay(a.date)} – ${shortDay(b.date)}"
    }
    val heroName = currency.displayName(lang).lowercase()

    TrendHero(
        label = title ?: pick(lang, "Markaziy bank kursi", "Курс ЦБ", "Central Bank rate"),
        today = today, unit = unit,
        delta = HeroDelta(deltaText, gain, flat = abs(pct) < 0.005, period = periodText),
        trailing = if (!allowAmount) null else ({
            UnitChip(
                one = "1 $heroName", many = "${kGroup(amt.roundToLong())} $heroName", amountMode = amountMode,
                onSelect = { m -> amountModePref = m; appStore().putBoolean("chart_amount_mode", m) }
            )
        })
    )

    // ---- main chart ----
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val ink = VsInk; val inkSoft = VsInkSoft; val line = VsHair; val bg = VsBg; val card = VsCard; val accent = VsEmerald
    val axisStyle = VsType.Meta.copy(fontSize = 12.sp, color = inkSoft)
    val bubbleStyle = VsType.Value.copy(fontSize = 13.sp)
    val noteStyle = VsType.Meta.copy(fontSize = 12.sp)
    val tagStyle = VsType.Value.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val todayLabel = pick(lang, "bugun", "сегодня", "today")

    // Y range follows the visible slice smoothly.
    val pad = if (hi - lo < 1e-6) hi * 0.01 else (hi - lo) * 0.1
    val yMinT = ((lo - pad) * mult).toFloat()
    val yMaxT = ((hi + pad) * mult).toFloat()
    val ySpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val yMin by animateFloatAsState(yMinT, ySpring, label = "yMin")
    val yMax by animateFloatAsState(yMaxT, ySpring, label = "yMax")

    // Draw-in when the data (currency) changes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(series) { reveal.snapTo(0f); reveal.animateTo(1f, tween(600, easing = Motion.Decelerate)) }

    var sel by remember { mutableStateOf<Int?>(null) }
    var lastSel by remember { mutableStateOf<Int?>(null) }
    if (sel != null) lastSel = sel
    val selA by animateFloatAsState(if (sel != null) 1f else 0f, tween(Motion.SHORT), label = "selA")
    LaunchedEffect(sel) { if (sel != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    val rightPx = with(density) { 8.dp.toPx() }
    val topPx = with(density) { 8.dp.toPx() }
    val bottomPx = with(density) { 22.dp.toPx() }
    val ticks = niceTicks(yMin.toDouble(), yMax.toDouble())
    val tickText: (Double) -> String = { v -> if (mult == 1.0 && today < 1000) v.fixed(2) else v.roundToLong().formatUzsL() }
    val leftPx = (ticks.maxOfOrNull { measurer.widthOf(tickText(it), axisStyle) } ?: 0f) + with(density) { 12.dp.toPx() }

    val desc = (pick(lang, "MB kursi grafigi, ", "График курса ЦБ, ", "CBU rate chart, ")) + periodText
    // The gesture coroutine outlives recompositions: read the current slice and axis width, not the first ones.
    val liveFrom by rememberUpdatedState(from)
    val liveTo by rememberUpdatedState(to)
    val liveLeft by rememberUpdatedState(leftPx)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(CHART_H)
            .semantics { contentDescription = desc }
            .pointerInput(series, mult) {
                fun plotW() = size.width - liveLeft - rightPx
                fun nearest(x: Float): Int {
                    val tv = vp.start + ((x - liveLeft) / plotW()).coerceIn(0f, 1f) * vp.span
                    var best = liveFrom; var bd = Double.MAX_VALUE
                    for (i in liveFrom..liveTo) { val d = abs(t[i] - tv); if (d < bd) { bd = d; best = i } }
                    return best
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    stopAnim()
                    var transforming = false
                    sel = nearest(down.position.x)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val pressed = ev.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            // Two fingers: pinch-zoom around the centroid + pan.
                            if (!transforming) { transforming = true; sel = null; markInteracted() }
                            val c = ev.calculateCentroid()
                            val anchor = ((c.x - liveLeft) / plotW()).coerceIn(0f, 1f)
                            vp.transform(ev.calculateZoom(), anchor, ev.calculatePan().x / plotW())
                            ev.changes.forEach { it.consume() }
                        } else if (transforming) {
                            // One finger left after a pinch: keep panning.
                            val ch = pressed[0]
                            vp.transform(1f, 0f, (ch.position.x - ch.previousPosition.x) / plotW())
                            ch.consume()
                        } else {
                            val ch = pressed[0]
                            val dx = ch.position.x - ch.previousPosition.x
                            val dy = ch.position.y - ch.previousPosition.y
                            // Mostly-horizontal movement belongs to the chart; vertical goes to the page scroll.
                            if (abs(dx) >= abs(dy)) ch.consume()
                            sel = nearest(ch.position.x)
                        }
                    }
                    sel = null
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val plotW = w - leftPx - rightPx
        val plotB = h - bottomPx
        val hair = 1.dp.toPx()
        fun xOf(tv: Double) = leftPx + ((tv - vp.start) / vp.span).toFloat() * plotW
        // The line keeps clear of the plot's top and bottom edge by a tag's height, so the value tags of the high and
        // the low fit above / below their markers instead of landing on the line.
        val tagRoom = 24.dp.toPx()
        fun yOf(v: Double) = topPx + tagRoom + (1f - ((v - yMin) / (yMax - yMin)).toFloat()) * (plotB - topPx - 2 * tagRoom)

        // y grid + labels
        ticks.forEach { v ->
            val y = yOf(v)
            if (y < topPx - 1 || y > plotB + 1) return@forEach
            drawLine(line, Offset(leftPx, y), Offset(w - rightPx, y), strokeWidth = hair)
            drawTextAt(measurer, tickText(v), leftPx - 8.dp.toPx(), y + 4.dp.toPx(), axisStyle, TextAnchor.RIGHT)
        }
        drawLine(line, Offset(leftPx, plotB), Offset(w - rightPx, plotB), strokeWidth = hair)

        // x labels: 4 evenly spaced dates across the window
        val withYear = vp.span > 120
        for (k in 0..3) {
            val tv = vp.start + vp.span * k / 3.0
            val anchor = when (k) { 0 -> TextAnchor.LEFT; 3 -> TextAnchor.RIGHT; else -> TextAnchor.CENTER }
            val txt = if (k == 3 && vp.end >= last - 0.5) todayLabel else dayLabelOf(tv.toLong(), withYear)
            drawTextAt(measurer, txt, xOf(tv), h - 4.dp.toPx(), axisStyle, anchor)
        }

        // line + soft area
        val path = Path()
        for (i in from..to) {
            val x = xOf(t[i]); val y = yOf(series[i].rate * mult)
            if (i == from) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(path)
            lineTo(xOf(t[to]), plotB)
            lineTo(xOf(t[from]), plotB)
            close()
        }
        clipRect(left = leftPx, top = 0f, right = leftPx + plotW * reveal.value, bottom = plotB) {
            drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0f)), startY = topPx, endY = plotB))
            drawPath(path, accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Markers: the low and the high of the visible window, and today. While idle the low and the high carry their
        // value as a light tag (the cards under the chart name them with dates); a dark chip appears only while scrubbing.
        val lowIdx = inside.minByOrNull { series[it].rate }
        val showToday = vp.end >= last - 0.5
        val dotA = ((reveal.value - 0.85f) / 0.15f).coerceIn(0f, 1f)
        if (lowIdx != null && !(showToday && lowIdx == series.lastIndex)) {
            val lx = xOf(t[lowIdx]); val ly = yOf(series[lowIdx].rate * mult)
            drawCircle(card, radius = 6.dp.toPx() * dotA, center = Offset(lx, ly))
            drawCircle(accent, radius = 4.dp.toPx() * dotA, center = Offset(lx, ly))
            val idleA = (1f - selA) * dotA
            if (idleA > 0f) {
                val v = series[lowIdx].rate
                val vText = if (amountMode) (v * amt).roundToLong().formatUzsL() else if (today >= 1000) v.roundToLong().formatUzsL() else v.formatRate()
                drawTag(measurer, vText, tagStyle.copy(color = inkSoft.copy(alpha = idleA)), lx, ly, above = false,
                    left = leftPx, right = w - rightPx, top = topPx, bottom = plotB, fill = card, border = line, alpha = idleA)
            }
        }
        // The high of the window, marked the same way as the low (chip above the line).
        val highIdx = inside.maxByOrNull { series[it].rate }
        if (highIdx != null && highIdx != lowIdx && !(showToday && highIdx == series.lastIndex)) {
            val hx = xOf(t[highIdx]); val hy = yOf(series[highIdx].rate * mult)
            drawCircle(card, radius = 6.dp.toPx() * dotA, center = Offset(hx, hy))
            drawCircle(accent, radius = 4.dp.toPx() * dotA, center = Offset(hx, hy))
            val idleA = (1f - selA) * dotA
            if (idleA > 0f) {
                val v = series[highIdx].rate
                val vText = if (amountMode) (v * amt).roundToLong().formatUzsL() else if (today >= 1000) v.roundToLong().formatUzsL() else v.formatRate()
                drawTag(measurer, vText, tagStyle.copy(color = inkSoft.copy(alpha = idleA)), hx, hy, above = true,
                    left = leftPx, right = w - rightPx, top = topPx, bottom = plotB, fill = card, border = line, alpha = idleA)
            }
        }
        if (showToday) {
            val lx = xOf(t.last()); val ly = yOf(series.last().rate * mult)
            drawCircle(card, radius = 6.dp.toPx() * dotA, center = Offset(lx, ly))
            drawCircle(accent, radius = 4.dp.toPx() * dotA, center = Offset(lx, ly))
        }

        // crosshair + bubble
        val si = lastSel
        if (si != null && si in series.indices && selA > 0f) {
            val sx = xOf(t[si]).coerceIn(leftPx, w - rightPx)
            val pt = series[si]
            val sy = yOf(pt.rate * mult)
            drawLine(ink.copy(alpha = selA), Offset(sx, topPx), Offset(sx, plotB), strokeWidth = hair,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            drawCircle(card.copy(alpha = selA), radius = 7.dp.toPx(), center = Offset(sx, sy))
            drawCircle(accent.copy(alpha = selA), radius = 5.dp.toPx(), center = Offset(sx, sy))

            val date = formatFullDate(pt.date)
            val l1 = if (amountMode) "$date · ${kGroup(amt.roundToLong())} ${currency.code} = ${(pt.rate * amt).roundToLong().formatUzsL()} $unit"
            else "$date  ${pt.rate.formatRate()} $unit"
            val diff = (today - pt.rate) * mult
            val dPct = if (pt.rate > 0) (today - pt.rate) / pt.rate * 100 else 0.0
            val l2 = if (si == series.lastIndex) (pick(lang, "bugun", "сегодня", "today")) else
                (pick(lang, "→ bugun ", "→ сегодня ", "→ today ")) + (if (diff >= 0) "+" else "−") +
                    fmtSom(diff, if (amountMode) 1000.0 else today) + " $unit (${if (dPct >= 0) "+" else "−"}${abs(dPct).fixed(2)}%)"
            drawChip(
                measurer,
                listOf(l1 to bubbleStyle.copy(color = bg.copy(alpha = selA)), l2 to noteStyle.copy(color = bg.copy(alpha = 0.75f * selA))),
                sx, sy, leftPx, w - rightPx, topPx, plotB, ink, selA
            )
        }
    }

    // ---- range presets ----
    Segmented(
        options = labels,
        selected = presetIdx,
        onSelect = { i ->
            val p = PRESETS[i]
            goTo(if (p == null) first else max(first, last - p), last)
        },
        modifier = Modifier.padding(top = 12.dp),
        height = 40.dp
    )


    WindowStats(series, inside, mult, today, unit)

    AnimatedVisibility(!hintSeen) {
        Text(
            pick(lang, "Barmoq: qiymat · ikki barmoq: kattalashtirish",
                "Палец — значение · два пальца — масштаб",
                "Drag: value · pinch: zoom"),
            style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** High, low, average and change over the visible window, as four quiet tiles (2 × 2). */
@Composable
private fun WindowStats(series: List<HistoryPoint>, inside: List<Int>, mult: Double, today: Double, unit: String) {
    if (inside.size < 2) return
    val pts = inside.map { series[it] }
    val hi = pts.maxBy { it.rate }; val lo = pts.minBy { it.rate }
    val avg = pts.sumOf { it.rate } / pts.size
    val a = pts.first().rate; val b = pts.last().rate
    val change = b - a
    val pct = if (a > 0) abs(change / a * 100) else 0.0
    fun v(x: Double) = if (mult == 1.0 && today < 1000) x.formatRate() else (x * mult).roundToLong().formatUzsL()
    val signed = (if (change > 0.004) "+" else if (change < -0.004) "−" else "±") + v(abs(change))
    val changeColor = if (abs(change) < 0.005) VsInk else if (change > 0) VsEmerald else VsRoseText
    @Composable
    fun Tile(label: String, value: String, note: String, color: Color, m: Modifier) {
        Column(m.clip(VsShape.Stat).background(VsCard).border(1.dp, VsHair, VsShape.Stat).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = VsType.Meta, color = VsInkSoft, maxLines = 1)
            Text(value, style = VsType.Value.copy(fontSize = 17.sp), color = color, maxLines = 1)
            Text(note, style = VsType.Meta.copy(fontSize = 12.sp), color = VsInkSoft, maxLines = 1)
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(pick(lang, "Eng yuqori", "Максимум", "High"), v(hi.rate), formatFullDate(hi.date), VsInk, Modifier.weight(1f))
            Tile(pick(lang, "Eng past", "Минимум", "Low"), v(lo.rate), formatFullDate(lo.date), VsInk, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(pick(lang, "Oʻrtacha", "Среднее", "Average"), v(avg), unit, VsInk, Modifier.weight(1f))
            Tile(pick(lang, "Oʻzgarish", "Изменение", "Change"), signed, "${pct.fixed(2).let { if (lang == Lang.EN) it else it.replace('.', ',') }}%", changeColor, Modifier.weight(1f))
        }
    }
}

/* ---------- hero ---------- */

class HeroDelta(val text: String, val gain: Boolean, val flat: Boolean, val period: String)

/** Screen hero: caption, the rate in the display face with its unit at the baseline, delta chip + period. */
@Composable
fun TrendHero(label: String, today: Double, unit: String, delta: HeroDelta?, trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = VsType.Body, color = VsInkSoft, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (today >= 1000) today.roundToLong().formatUzsL() else today.formatRate(), style = VsType.Display, color = VsInk, maxLines = 1, modifier = Modifier.alignByBaseline())
            Text(unit, style = VsType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = VsInkSoft, modifier = Modifier.alignByBaseline())
        }
        if (delta != null) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fg = if (delta.flat) VsInkSoft else if (delta.gain) VsEmerald else VsRoseText
                val bgc = if (delta.flat) VsGround else if (delta.gain) VsTealTint else VsRoseTint
                AnimatedContent(delta.text to delta.gain, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) }, label = "delta") { (txt, _) ->
                    Row(
                        Modifier.clip(CircleShape).background(bgc).padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!delta.flat) Icon(if (delta.gain) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = fg, modifier = Modifier.size(12.dp))
                        Text(txt, style = VsType.Value.copy(fontSize = 12.sp), color = fg, maxLines = 1)
                    }
                }
                Text(delta.period, style = VsType.Meta, color = VsInkSoft, maxLines = 1)
            }
        }
    }
}

/** Pill dropdown beside the hero caption: show the chart for 1 unit or for the Kurs amount. */
@Composable
private fun UnitChip(one: String, many: String, amountMode: Boolean, onSelect: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.vsCard(CircleShape).clip(CircleShape).clickable(role = Role.Button) { open = true }
                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(if (amountMode) many else one, style = VsType.BodyMedium.copy(fontSize = 14.sp), color = VsInk, maxLines = 1)
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = VsInkSoft, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = VsCard, shape = VsShape.Panel) {
            listOf(false to one, true to many).forEach { (m, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = VsType.Body, color = if (m == amountMode) VsEmerald else VsInk) },
                    onClick = { open = false; onSelect(m) }
                )
            }
        }
    }
}

/* ---------- helpers ---------- */

/** Dark tooltip chip with 1..n lines, placed above [cx],[cy] (below when there is no room) and kept inside the bounds. */
private fun DrawScope.drawChip(
    measurer: TextMeasurer, lines: List<Pair<String, TextStyle>>, cx: Float, cy: Float,
    left: Float, right: Float, top: Float, bottom: Float, fill: Color, alpha: Float
) {
    val padH = 9.dp.toPx(); val padV = 6.dp.toPx(); val gap = 12.dp.toPx()
    val rs = lines.map { measurer.measure(it.first, it.second) }
    val bw = rs.maxOf { it.size.width } + padH * 2
    val bh = rs.sumOf { it.size.height } + padV * 2
    val bx = (cx - bw / 2).coerceIn(left, max(left, right - bw))
    val above = cy - bh - gap > top
    val by = if (above) cy - bh - gap else min(cy + gap, bottom - bh)
    drawRoundRect(fill.copy(alpha = alpha), Offset(bx, by), Size(bw, bh), CornerRadius(10.dp.toPx()))
    var y = by + padV
    rs.forEach { r -> drawText(r, topLeft = Offset(bx + padH, y)); y += r.size.height }
}

/**
 * Light value tag for a marker at [cx],[cy]: above it ([above]) or below it, beside it when neither fits, always inside
 * the bounds. Card-coloured with a hairline, so it stays readable over the grid without weighing on the line.
 */
private fun DrawScope.drawTag(
    measurer: TextMeasurer, text: String, style: TextStyle, cx: Float, cy: Float, above: Boolean,
    left: Float, right: Float, top: Float, bottom: Float, fill: Color, border: Color, alpha: Float
) {
    val r = measurer.measure(text, style)
    val padH = 6.dp.toPx(); val padV = 2.dp.toPx(); val gap = 8.dp.toPx()
    val bw = r.size.width + padH * 2
    val bh = r.size.height + padV * 2
    val fitsAbove = cy - gap - bh >= top
    val fitsBelow = cy + gap + bh <= bottom
    // The preferred side first; then beside the marker (the other side is where the line goes); the other side last.
    val (bx, by) = when {
        above && fitsAbove -> (cx - bw / 2) to (cy - gap - bh)
        !above && fitsBelow -> (cx - bw / 2) to (cy + gap)
        cx + gap + bw <= right -> (cx + gap) to (cy - bh / 2)
        cx - gap - bw >= left -> (cx - gap - bw) to (cy - bh / 2)
        fitsAbove -> (cx - bw / 2) to (cy - gap - bh)
        else -> (cx - bw / 2) to (cy + gap)
    }
    val x = bx.coerceIn(left, max(left, right - bw))
    val y = by.coerceIn(top, max(top, bottom - bh))
    val corner = CornerRadius(6.dp.toPx())
    drawRoundRect(fill.copy(alpha = 0.92f * alpha), Offset(x, y), Size(bw, bh), corner)
    drawRoundRect(border.copy(alpha = alpha), Offset(x, y), Size(bw, bh), corner, style = Stroke(width = 1.dp.toPx()))
    drawText(r, topLeft = Offset(x + padH, y + padV))
}

private fun shortDay(iso: String): String {
    val p = iso.take(10).split("-")
    return if (p.size == 3) "${p[2]}.${p[1]}.${p[0].takeLast(2)}" else iso
}

/** "dd.MM" or, with [withYear], "dd.MM.yy". */
private fun dayLabelOf(epochDay: Long, withYear: Boolean): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    val dm = "${d.day.toString().padStart(2, '0')}.${d.month.number.toString().padStart(2, '0')}"
    return if (withYear) "$dm.${(d.year % 100).toString().padStart(2, '0')}" else dm
}

private fun niceTicks(lo: Double, hi: Double): List<Double> {
    fun ticksFor(parts: Double): List<Double> {
        val raw = (hi - lo) / parts
        if (raw <= 0 || raw.isNaN()) return emptyList()
        val mag = 10.0.pow(floor(log10(raw)))
        val norm = raw / mag
        val step = mag * when { norm <= 1 -> 1.0; norm <= 2 -> 2.0; norm <= 5 -> 5.0; else -> 10.0 }
        val out = mutableListOf<Double>()
        var v = ceil(lo / step) * step
        while (v <= hi + 1e-9 && out.size < 12) { out += v; v += step }
        return out
    }
    var t = ticksFor(3.0)
    if (t.size > 4) t = ticksFor(2.0)
    if (t.size < 2) t = listOf(lo, hi)
    return t
}
