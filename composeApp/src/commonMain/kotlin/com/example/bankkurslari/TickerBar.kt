package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.math.roundToLong

/** What one ticker card shows. All CBU, so number, delta and sparkline describe the same series. */
data class TickerData(val cbu: CbuRate?, val history: List<HistoryPoint>)

private const val SPARK_DAYS = 30

private fun tickerRate(v: Double, ru: Boolean): String =
    if (v >= 1000) kGroup(v.roundToLong()) else v.fixed(2).let { if (ru) it.replace('.', ',') else it }

private fun tickerDelta(d: Double, rate: Double, ru: Boolean): String {
    val arrow = when { abs(d) < 0.005 -> "–"; d > 0 -> "▲"; else -> "▼" }
    val v = if (rate >= 1000) kGroup(abs(d).roundToLong()) else abs(d).fixed(2).let { if (ru) it.replace('.', ',') else it }
    return "$arrow $v"
}

/**
 * Kurs + Tarix header. Status line (freshness pulse + time + actions) and three CBU ticker cards.
 * The selected card grows and draws a 30-day sparkline; swipe the row sideways to switch currency.
 */
@Composable
fun TickerBar(
    currency: Currency,
    onCurrency: (Currency) -> Unit,
    tickers: Map<Currency, TickerData>,
    updatedAt: String?,
    offline: Boolean,
    onOpenSettings: () -> Unit,
    searchOpen: Boolean? = null,
    onToggleSearch: () -> Unit = {},
    onAllCurrencies: (() -> Unit)? = null
) {
    val ru = kursIsRu()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipePx = with(density) { 56.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    val dragShown by animateFloatAsState(drag, spring(stiffness = 600f), label = "drag")

    fun select(c: Currency) {
        if (c != currency) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCurrency(c)
        }
    }

    val currentCurrency = rememberUpdatedState(currency)
    val selectRef = rememberUpdatedState<(Currency) -> Unit>(::select)

    Column(Modifier.fillMaxWidth()) {
        // Status line: is the data fresh? + actions.
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            val ageMin = updatedAt?.let { kParseMillis(it) }?.let { (nowMillis() - it) / 60_000 }
            val fresh = !offline && ageMin != null && ageMin < 30
            FreshDot(fresh = fresh, stale = offline || (ageMin != null && ageMin > 6 * 60))
            Spacer(Modifier.width(8.dp))
            val time = updatedAt?.formatUpdatedAt()
            Text(
                when {
                    offline -> pick(lang, "Oflayn · ${time ?: "—"}", "Офлайн · ${time ?: "—"}", "Offline · ${time ?: "—"}")
                    time != null -> pick(lang, "$time yangilandi", "Обновлено в $time", "Updated at $time")
                    else -> ""
                },
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f)
            )
            // IconButtons carry 12 dp of internal padding; shift so the glyphs sit on the 16 dp content edge.
            Row(Modifier.offset(x = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (searchOpen != null) {
                IconButton(onClick = onToggleSearch) {
                    AnimatedContent(searchOpen, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) }, label = "srch") { open ->
                        Icon(if (open) Icons.Outlined.Close else Icons.Outlined.Search, tr("Qidirish", "Поиск", "Search"), tint = VsInk)
                    }
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, tr("Sozlamalar", "Настройки", "Settings"), tint = VsInk)
            }
            }
        }

        // Cards
        Row(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .graphicsLayer { translationX = dragShown * 0.18f }
                // Keyed on Unit: restarting on currency change would split one swipe into two.
                .pointerInput(Unit) {
                    var switched = false
                    detectHorizontalDragGestures(
                        onDragStart = { switched = false },
                        onDragEnd = { drag = 0f },
                        onDragCancel = { drag = 0f },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            drag += dx
                            // Switch as soon as the threshold is crossed (feels immediate), once per gesture.
                            if (!switched && abs(drag) > swipePx) {
                                val cur = currentCurrency.value
                                val i = Currency.entries.indexOf(cur)
                                val next = if (drag < 0) i + 1 else i - 1
                                if (next in Currency.entries.indices) selectRef.value(Currency.entries[next])
                                switched = true
                            }
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Currency.entries.forEach { c ->
                val sel = c == currency
                val w by animateFloatAsState(if (sel) 1.9f else 1f, spring(dampingRatio = 0.72f, stiffness = 380f), label = "w")
                TickerCard(
                    currency = c, selected = sel, data = tickers[c], ru = ru,
                    modifier = Modifier.weight(w).fillMaxHeight(),
                    onClick = { select(c) }
                )
            }
        }
        if (onAllCurrencies != null) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onAllCurrencies),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr("Boshqa valyutalar: yuan, tenge, lira, dirham…", "Другие валюты: юань, тенге, лира, дирхам…", "Other currencies: yuan, tenge, lira, dirham…"),
                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f)
                )
                Text("›", style = VsType.Title, color = VsInkSoft)
            }
        }
    }
}

@Composable
private fun TickerCard(currency: Currency, selected: Boolean, data: TickerData?, ru: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    // Selected must read at a glance in both themes: accent outline + accent-tinted fill + accent code.
    val bg by animateColorAsState(if (selected) VsEmeraldSoft else VsBg, Motion.color(), label = "tbg")
    val border by animateColorAsState(if (selected) VsEmerald else VsLine, Motion.color(), label = "tbd")
    val borderW by animateDpAsState(if (selected) 2.dp else 1.dp, tween(Motion.SHORT), label = "tbw")
    val codeColor by animateColorAsState(if (selected) VsEmerald else VsInkSoft, Motion.color(), label = "tcode")
    val numColor by animateColorAsState(if (selected) VsInk else VsInkSoft, Motion.color(), label = "tnum")
    val cbu = data?.cbu
    val spoken = buildString {
        append(currency.code)
        if (cbu != null) {
            append(", ").append(pick(lang, "MB kursi ", "курс ЦБ ", "CBU rate ")).append(tickerRate(cbu.rate, ru)).append(" ").append(kSom(ru))
            append(", ").append(pick(lang, "kecha bilan farq ", "за день ", "day change ")).append(tickerDelta(cbu.diff, cbu.rate, ru))
        }
    }
    Column(
        modifier
            .pressScale(src)
            .clip(shape)
            .background(bg)
            .border(borderW, border, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Tab; this.selected = selected; contentDescription = spoken }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currency.code, style = VsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = codeColor, modifier = Modifier.weight(1f))
            if (cbu != null && selected) {
                Text(
                    tickerDelta(cbu.diff, cbu.rate, ru) + (pick(lang, " · kecha", " · вчера", " · yesterday")),
                    style = VsType.Meta, color = VsInkSoft, maxLines = 1, softWrap = false
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RollingNumber(
                value = cbu?.rate,
                text = cbu?.let { tickerRate(it.rate, ru) } ?: "—",
                selected = selected,
                color = numColor
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Sparkline(
                    points = data?.history.orEmpty().takeLast(SPARK_DAYS),
                    modifier = Modifier.weight(1f).height(28.dp)
                )
            }
        }
        Text(
            if (selected) (pick(lang, "MB, ${kSom(ru)} · 30 kun", "ЦБ, ${kSom(ru)} · 30 дн.", "CBU, ${kSom(ru)} · 30 days"))
            else cbu?.let { tickerDelta(it.diff, it.rate, ru) } ?: " ",
            style = VsType.Meta, color = VsInkSoft, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip
        )
    }
}

/** Number that rolls up when it grows and down when it shrinks (no count-up, spec 5.4). */
@Composable
private fun RollingNumber(value: Double?, text: String, selected: Boolean, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value to text,
        transitionSpec = {
            val up = (targetState.first ?: 0.0) >= (initialState.first ?: 0.0)
            val t = tween<androidx.compose.ui.unit.IntOffset>(Motion.MEDIUM, easing = Motion.Decelerate)
            ContentTransform(
                targetContentEnter = slideInVertically(t) { if (up) it / 2 else -it / 2 } + fadeIn(tween(Motion.MEDIUM)),
                initialContentExit = slideOutVertically(tween(Motion.SHORT)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(Motion.SHORT)),
                sizeTransform = SizeTransform(clip = true)
            )
        },
        modifier = modifier,
        label = "roll"
    ) { (_, t) ->
        Text(
            t,
            style = if (selected) VsType.Title else VsType.BodyMedium,
            color = color, maxLines = 1, softWrap = false
        )
    }
}

/** 30-day CBU sparkline; draws in left -> right whenever it appears or the series changes. */
@Composable
private fun Sparkline(points: List<HistoryPoint>, modifier: Modifier) {
    val ink = VsInk
    val accent = VsEmerald
    val bg = VsEmeraldSoft
    val line = VsLine
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(points) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(700, delayMillis = 120, easing = Motion.Decelerate))
    }
    Canvas(modifier) {
        if (points.size < 2) {
            // No history yet: a quiet baseline instead of an empty gap.
            drawLine(line, Offset(0f, size.height / 2), Offset(size.width * reveal.value, size.height / 2), strokeWidth = 1.dp.toPx())
            return@Canvas
        }
        val lo = points.minOf { it.rate }
        val hi = points.maxOf { it.rate }
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        val pad = 3.dp.toPx()
        fun x(i: Int) = pad + i * (size.width - 2 * pad) / (points.size - 1)
        fun y(v: Double) = pad + (1f - ((v - lo) / span).toFloat()) * (size.height - 2 * pad)
        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(i), y(p.rate)) else path.lineTo(x(i), y(p.rate)) }
        clipRect(right = size.width * reveal.value) {
            drawPath(path, ink, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val a = ((reveal.value - 0.85f) / 0.15f).coerceIn(0f, 1f)
        val end = Offset(x(points.lastIndex), y(points.last().rate))
        drawCircle(bg, radius = 4.dp.toPx() * a, center = end)
        drawCircle(accent, radius = 2.75.dp.toPx() * a, center = end)
    }
}

/** Accent dot with a soft expanding ring while data is fresh; amber when stale/offline; grey otherwise. */
@Composable
private fun FreshDot(fresh: Boolean, stale: Boolean) {
    val color = when { stale -> VsAmber; fresh -> VsEmerald; else -> VsInkSoft }
    // 8 dp box so the dot itself sits on the content edge; the ring grows outside it (graphicsLayer does not clip).
    Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
        if (fresh) {
            val t = rememberInfiniteTransition(label = "pulse")
            val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = Motion.Decelerate), RepeatMode.Restart), label = "p")
            Box(
                Modifier.size(8.dp)
                    .graphicsLayer { scaleX = 1f + p * 1.6f; scaleY = 1f + p * 1.6f; alpha = (1f - p) * 0.45f }
                    .clip(CircleShape).background(color)
            )
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}
