package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.example.bankkurslari.ui.theme.VsDanger
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

/** Whole so'm for big-unit currencies (USD/EUR), two decimals for RUB-scale rates. */
internal fun fmtSom(v: Double, rate: Double): String =
    if (rate >= 1000) abs(v).roundToLong().formatUzsL() else String.format(Locale.US, "%.2f", abs(v))

private class CalDay(val epoch: Long, val date: String, val rate: Double, val change: Double)

private val MONTHS_UZ = listOf("Yan", "Fev", "Mar", "Apr", "May", "Iyun", "Iyul", "Avg", "Sen", "Okt", "Noy", "Dek")
private val MONTHS_RU = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
private val WEEKDAYS_UZ = listOf("dushanba", "seshanba", "chorshanba", "payshanba", "juma", "shanba", "yakshanba")
private val WEEKDAYS_RU = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")

/** Monday = 0 (1970-01-01 was a Thursday). */
private fun dowOf(epoch: Long): Int = (((epoch + 3) % 7) + 7).toInt() % 7

/** Trailing stretch of day-by-day points (older history is weekly/monthly and has no daily change). */
private fun dailyDays(series: List<HistoryPoint>): List<CalDay> {
    if (series.size < 2) return emptyList()
    val ep = series.map { epochDay(it.date) }
    var start = series.lastIndex
    while (start > 0 && ep[start] - ep[start - 1] == 1L) start--
    if (series.lastIndex - start < 2) return emptyList()
    return (start + 1..series.lastIndex).map { i ->
        CalDay(ep[i], series[i].date, series[i].rate, series[i].rate - series[i - 1].rate)
    }
}

/**
 * GitHub-style grid of daily CBU changes. Columns = weeks, rows = Mon..Sun.
 * Green = a good day for the user's side (selling: rate went up; buying: rate went down), red = a bad one.
 * Intensity = size of the move. Tap a day for its numbers.
 */
@Composable
internal fun ChangeCalendar(
    series: List<HistoryPoint>,
    currency: Currency,
    sell: Boolean,
    ru: Boolean,
    unit: String,
    onDirection: (ConvertMode) -> Unit
) {
    val days = remember(series) { dailyDays(series) }
    val haptic = LocalHapticFeedback.current
    var sel by remember(days) { mutableStateOf(days.lastIndex) }

    Text(if (ru) "Изменения по дням" else "Kunlik oʻzgarishlar", style = VsType.Title, color = VsInk)
    if (days.size < 7) {
        Text(
            if (ru) "Недостаточно ежедневных данных." else "Kunlik maʻlumot hali yetarli emas.",
            style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
        )
        return
    }

    // Summary: counts + biggest move, all from the grid's own data.
    val up = days.count { it.change > 0.005 }
    val down = days.count { it.change < -0.005 }
    val biggest = days.maxBy { abs(it.change) }
    val today = days.last().rate
    Text(
        if (ru) "${days.size} дн.: рост $up, снижение $down · самое большое: ${formatShortDate(biggest.date)} ${sign(biggest.change)}${fmtSom(biggest.change, today)} $unit"
        else "${days.size} kunda: $up kun oʻsdi, $down kun pasaydi · eng katta: ${formatShortDate(biggest.date)} ${sign(biggest.change)}${fmtSom(biggest.change, today)} $unit",
        style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
    )

    Segmented(
        options = listOf(if (ru) "Я продаю" else "Men sotaman", if (ru) "Я покупаю" else "Men olaman"),
        selected = if (sell) 0 else 1,
        onSelect = { onDirection(if (it == 0) ConvertMode.SELL_USD else ConvertMode.BUY_USD) },
        modifier = Modifier.padding(top = 12.dp),
        height = 40.dp
    )

    // ----- grid geometry -----
    val firstMonday = days.first().epoch - dowOf(days.first().epoch)
    val cols = ((days.last().epoch - firstMonday) / 7 + 1).toInt()
    val byEpoch = remember(days) { days.withIndex().associate { (i, d) -> d.epoch to i } }
    val maxAbs = days.maxOf { abs(it.change) }.takeIf { it > 1e-9 } ?: 1.0

    val good = VsEmerald
    val bad = VsDanger
    val empty = VsLine
    val ink = VsInk
    val inkSoft = VsInkSoft
    val density = LocalDensity.current
    val labelW = with(density) { 24.dp.toPx() }
    val topLabelH = with(density) { 18.dp.toPx() }
    val gap = with(density) { 4.dp.toPx() }
    val textPx = with(density) { 11.sp.toPx() }
    val paint = remember(textPx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = textPx }
    }

    // Wave-in: columns appear left -> right; replays when the data (currency) changes.
    val wave = remember { Animatable(0f) }
    LaunchedEffect(days) {
        wave.snapTo(0f)
        wave.animateTo(1f, tween(700, easing = Motion.Decelerate))
    }

    fun colorFor(d: CalDay): Color {
        if (abs(d.change) < 0.005) return empty
        val favourable = if (sell) d.change > 0 else d.change < 0
        val q = ceil(abs(d.change) / maxAbs * 4).coerceIn(1.0, 4.0)
        val alpha = listOf(0.3f, 0.5f, 0.75f, 1f)[q.toInt() - 1]
        return (if (favourable) good else bad).copy(alpha = alpha)
    }

    val desc = if (ru) "Календарь изменений курса ЦБ за ${days.size} дней" else "MB kursi kunlik oʻzgarishlari kalendari, ${days.size} kun"
    // Cells fill the width (capped at 40 dp so a short history does not turn into giant squares).
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
    val cellDp = ((maxWidth - 24.dp) / cols).coerceAtMost(40.dp)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(cellDp * 7 + 18.dp)
            .semantics { contentDescription = desc }
            .pointerInput(days, cols) {
                detectTapGestures { pos ->
                    val cell = minOf((size.width - labelW) / cols, (size.height - topLabelH) / 7f)
                    val c = ((pos.x - labelW) / cell).toInt()
                    val r = ((pos.y - topLabelH) / cell).toInt()
                    if (c in 0 until cols && r in 0..6) {
                        byEpoch[firstMonday + c * 7L + r]?.let {
                            if (it != sel) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sel = it
                        }
                    }
                }
            }
    ) {
        val cell = minOf((size.width - labelW) / cols, (size.height - topLabelH) / 7f)
        val box = cell - gap
        val native = drawContext.canvas.nativeCanvas

        // weekday labels: Du / Ch / Ju (Пн / Ср / Пт)
        paint.color = inkSoft.toArgb()
        paint.textAlign = android.graphics.Paint.Align.LEFT
        val wl = if (ru) listOf(0 to "Пн", 2 to "Ср", 4 to "Пт") else listOf(0 to "Du", 2 to "Ch", 4 to "Ju")
        wl.forEach { (r, l) -> native.drawText(l, 0f, topLabelH + r * cell + box / 2 + textPx * 0.35f, paint) }

        // month labels where a month starts
        var lastMonth = -1
        for (c in 0 until cols) {
            val e = firstMonday + c * 7L
            val m = dayMonth(e)
            if (m != lastMonth) {
                native.drawText((if (ru) MONTHS_RU else MONTHS_UZ)[m], labelW + c * cell, textPx, paint)
                lastMonth = m
            }
        }

        for (c in 0 until cols) {
            // each column fades + grows in a little after the previous one
            val t = ((wave.value * (cols + 3)) - c).coerceIn(0f, 1f)
            if (t <= 0f) continue
            for (r in 0..6) {
                val i = byEpoch[firstMonday + c * 7L + r] ?: continue
                val d = days[i]
                val s = box * (0.6f + 0.4f * t)
                val x = labelW + c * cell + (box - s) / 2
                val y = topLabelH + r * cell + (box - s) / 2
                val col = colorFor(d)
                drawRoundRect(col.copy(alpha = col.alpha * t), Offset(x, y), Size(s, s), CornerRadius(4.dp.toPx()))
                if (i == sel) {
                    drawRoundRect(
                        ink, Offset(labelW + c * cell - 1.5.dp.toPx(), topLabelH + r * cell - 1.5.dp.toPx()),
                        Size(box + 3.dp.toPx(), box + 3.dp.toPx()), CornerRadius(5.dp.toPx()),
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
        }
    }

    }

    // Selected day readout
    val d = days[sel.coerceIn(0, days.lastIndex)]
    AnimatedContent(d, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) }, label = "calday") { day ->
        val wd = (if (ru) WEEKDAYS_RU else WEEKDAYS_UZ)[dowOf(day.epoch)]
        val pct = if (day.rate - day.change > 0) day.change / (day.rate - day.change) * 100 else 0.0
        val arrow = when { day.change > 0.005 -> "▲"; day.change < -0.005 -> "▼"; else -> "–" }
        Column(Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("${formatFullDate(day.date)}, $wd", style = VsType.BodyMedium, color = VsInk, modifier = Modifier.weight(1f).alignByBaseline())
                Text("${day.rate.formatRate()} $unit", style = VsType.BodyMedium, color = VsInk, modifier = Modifier.alignByBaseline())
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                val favourable = if (sell) day.change > 0 else day.change < 0
                Text(
                    when {
                        abs(day.change) < 0.005 -> if (ru) "без изменений" else "oʻzgarishsiz"
                        favourable -> if (ru) "выгодный день для вас" else "siz uchun foydali kun"
                        else -> if (ru) "невыгодный день для вас" else "siz uchun noqulay kun"
                    },
                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f).alignByBaseline()
                )
                Text(
                    "$arrow ${sign(day.change)}${fmtSom(day.change, day.rate)} $unit (${sign(day.change)}${String.format(Locale.US, "%.2f", abs(pct))}%)",
                    style = VsType.Meta.copy(fontWeight = FontWeight.Medium), color = VsInk, modifier = Modifier.alignByBaseline()
                )
            }
        }
    }

    // Legend
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Swatch(good)
        Text(if (ru) "выгодно" else "foydali", style = VsType.Meta, color = VsInkSoft)
        Spacer(Modifier.width(8.dp))
        Swatch(bad)
        Text(if (ru) "невыгодно" else "noqulay", style = VsType.Meta, color = VsInkSoft)
        Spacer(Modifier.width(8.dp))
        Text(if (ru) "ярче = сильнее" else "toʻqroq = kattaroq oʻzgarish", style = VsType.Meta, color = VsInkSoft)
    }
}

@Composable
private fun Swatch(c: Color) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c))
}

private fun sign(v: Double) = if (v < 0) "−" else if (v > 0) "+" else ""

/** 0-based month of an epoch day (UTC). */
private fun dayMonth(epoch: Long): Int {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = epoch * 86_400_000L
    return cal.get(java.util.Calendar.MONTH)
}
