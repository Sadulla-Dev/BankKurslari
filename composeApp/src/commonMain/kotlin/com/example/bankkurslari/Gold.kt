package com.example.bankkurslari

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.roundToLong
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

private fun gramsText(g: Double): String = if (g % 1.0 == 0.0) g.toLong().toString() else g.toString()

/**
 * CBU gold bars: sale and buy-back price per bar, price per gram and the round-trip loss.
 * Facts from cbu.uz only; cached so it also shows offline.
 */
@Composable
fun GoldSection(ru: Boolean, unit: String) {
    var gold by remember { mutableStateOf<GoldPrices?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        gold = cachedGold()
        runCatching { fetchGold() }.onSuccess { gold = it; failed = false }.onFailure { if (gold == null) failed = true }
    }

    val g = gold
    Column(
        Modifier.fillMaxWidth().vsCard(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pick(lang, "Markaziy bank oltin quymalari", "Золотые слитки ЦБ", "CBU gold bars"), style = VsType.Title.copy(fontSize = 19.sp, lineHeight = 24.sp), color = VsInk)
            val isToday = g?.date?.let { it == todayIso() || it.startsWith(todayIso()) || it == formatFullDate(todayIso()) } == true
            val stamp = when {
                g == null -> ""
                isToday && g.time != null -> pick(lang, "Bugun ${g.time} da narxlangan", "Цены на сегодня, ${g.time}", "Priced today at ${g.time}")
                else -> listOfNotNull(g.date, g.time).joinToString(", ").let { if (it.isEmpty()) "" else pick(lang, "$it da narxlangan", "Цены на $it", "Priced $it") }
            }
            if (stamp.isNotEmpty()) Text(stamp, style = VsType.Meta, color = VsInkSoft)
        }
        if (g == null || g.bars.isEmpty()) {
            Text(
                if (failed) (pick(lang, "Oltin narxi hozircha mavjud emas.", "Цены на золото сейчас недоступны.", "Gold prices are not available yet."))
                else (pick(lang, "Yuklanmoqda…", "Загрузка…", "Loading…")),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(bottom = 10.dp)
            )
            return@Column
        }

        // Per-gram price from the largest bar (cheapest per gram) and the round-trip loss on it.
        val ref = g.bars.maxBy { it.grams }
        val perGram = (ref.sell / ref.grams).roundToLong()
        val loss = ref.buyback?.let { (ref.sell - it).toDouble() / ref.sell * 100 }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text(perGram.formatUzsL(), style = VsType.Display.copy(fontSize = 36.sp, lineHeight = 38.sp), color = VsInk, maxLines = 1, modifier = Modifier.alignByBaseline())
            Text(
                pick(lang, "$unit, 1 gramm", "$unit, 1 грамм", "$unit, 1 gram"),
                style = VsType.Body.copy(fontSize = 15.sp), color = VsInkSoft, modifier = Modifier.alignByBaseline()
            )
        }
        Text(
            pick(lang, "${gramsText(ref.grams)} g quyma narxi boʻyicha", "по цене слитка ${gramsText(ref.grams)} г", "based on the ${gramsText(ref.grams)} g bar"),
            style = VsType.Meta, color = VsInkSoft
        )
        if (loss != null) {
            VsNote(
                pick(lang, "Darhol qaytarsangiz: −${loss.fixed(1)}%",
                    "При возврате сразу: −${loss.fixed(1)}%",
                    "Sold straight back: −${loss.fixed(1)}%")
            )
        }

        GoldTrend(ru, unit)

        // Table header
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(pick(lang, "Ogʻirlik", "Вес", "Bar"), style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(0.8f))
            Text(pick(lang, "Siz toʻlaysiz", "Вы платите", "You pay"), style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
            Text(pick(lang, "Bank qaytaradi", "Банк выкупит", "Bank pays back"), style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
        }
        VsDivider(Modifier.padding(top = 2.dp))
        val bars = g.bars.sortedBy { it.grams }
        bars.forEachIndexed { i, bar -> GoldRow(bar, ru, unit, last = i == bars.lastIndex) }
    }
}

@Composable
private fun GoldRow(bar: GoldBar, ru: Boolean, unit: String, last: Boolean) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .clickable(role = Role.Button) { open = !open }
            .padding(vertical = 4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Row(Modifier.weight(0.8f).alignByBaseline(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldBarIcon(bar.grams)
                Text("${gramsText(bar.grams)} g", style = VsType.Value.copy(fontSize = 15.sp), color = VsInk)
            }
            Text(bar.sell.formatUzsL(), style = VsType.Value.copy(fontSize = 15.sp), color = VsInk, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f).alignByBaseline())
            Text(bar.buyback?.formatUzsL() ?: "—", style = VsType.Value.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = VsInk, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f).alignByBaseline())
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(top = 4.dp)) {
                val perGram = (bar.sell / bar.grams).roundToLong()
                Text(
                    (pick(lang, "1 gramm: ", "1 грамм: ", "1 gram: ")) + "${perGram.formatUzsL()} $unit",
                    style = VsType.Meta, color = VsInkSoft
                )
                bar.buybackDamaged?.let {
                    Row {
                        Text(
                            pick(lang, "Qadogʻi buzilgan boʻlsa: ", "Выкуп при повреждённой упаковке: ", "Buy-back with damaged packaging: "),
                            style = VsType.Meta, color = VsInkSoft
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${it.formatUzsL()} $unit", style = VsType.Meta, color = VsInk)
                    }
                }
            }
        }
    }
    if (!last) VsDivider()
}


private val BAR_WEIGHTS = listOf(5.0, 10.0, 20.0, 50.0, 100.0)

/**
 * A minted gold bar standing upright (same drawing as the website's table): polished rim, an inner stamped panel and a
 * shine. Heavier bars are drawn taller inside a fixed slot, so the weights still line up.
 */
@Composable
private fun GoldBarIcon(grams: Double) {
    val i = BAR_WEIGHTS.indexOf(grams).coerceAtLeast(0)
    val h = (20 + i * 3).dp
    Box(Modifier.size(width = 20.dp, height = 32.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.size(width = h * 0.62f, height = h)) {
            val w = size.width; val hh = size.height
            val rim = Brush.linearGradient(
                listOf(Color(0xFFFFF0B3), Color(0xFFF2C95A), Color(0xFFC98E22), Color(0xFFF5D27A), Color(0xFFB07A16)),
                start = Offset.Zero, end = Offset(w, hh)
            )
            val panel = Brush.linearGradient(listOf(Color(0xFFF9DE8C), Color(0xFFE6B649), Color(0xFFD19B2E)), start = Offset.Zero, end = Offset(w, hh))
            val r = w * 0.16f
            drawRoundRect(rim, cornerRadius = CornerRadius(r))
            drawRoundRect(Color(0xFF8C5F12), cornerRadius = CornerRadius(r), style = Stroke(width = w * 0.03f))
            val inset = w * 0.1f
            drawRoundRect(panel, topLeft = Offset(inset, inset), size = Size(w - 2 * inset, hh - 2 * inset), cornerRadius = CornerRadius(r * 0.6f))
            drawRoundRect(Color(0xFFFFF2C2), topLeft = Offset(inset, inset), size = Size(w - 2 * inset, hh - 2 * inset), cornerRadius = CornerRadius(r * 0.6f), style = Stroke(width = w * 0.035f))
            // stamp lines and a diagonal shine
            drawLine(Color(0x739A6612), Offset(w * 0.3f, hh * 0.45f), Offset(w * 0.7f, hh * 0.45f), strokeWidth = w * 0.05f)
            drawLine(Color(0x739A6612), Offset(w * 0.3f, hh * 0.58f), Offset(w * 0.7f, hh * 0.58f), strokeWidth = w * 0.035f)
            drawLine(Color(0x99FFFFFF), Offset(w * 0.2f, hh * 0.32f), Offset(w * 0.45f, hh * 0.1f), strokeWidth = w * 0.06f)
        }
    }
}

/** Price per gram over the days our server has collected; needs two days before it can draw anything. */
@Composable
private fun GoldTrend(ru: Boolean, unit: String) {
    var points by remember { mutableStateOf<List<Pair<String, Double>>?>(null) }
    LaunchedEffect(Unit) { points = runCatching { fetchGoldHistory() }.getOrNull() }
    val p = points ?: return
    if (p.size < 2) {
        Text(
            tr("Tarix har kuni yigʻiladi. Grafik 2-kundan boshlanadi.",
                "История копится ежедневно. График — со 2-го дня.",
                "History builds daily. Chart starts on day 2."),
            style = VsType.Meta, color = VsInkSoft
        )
        return
    }
    val first = p.first(); val last = p.last()
    val pct = (last.second - first.second) / first.second * 100
    Text(
        (if (pct >= 0) "▲ " else "▼ ") + "${kotlin.math.abs(pct).fixed(2)}% · " +
            "${formatShortDate(first.first)} – ${formatShortDate(last.first)}",
        style = VsType.Meta, color = VsInkSoft
    )
}
