package com.example.bankkurslari

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.roundToLong

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

    Text(pick(lang, "Markaziy bank oltin quymalari", "Золотые слитки ЦБ", "CBU gold bars"), style = VsType.Title, color = VsInk)
    val g = gold
    if (g == null || g.bars.isEmpty()) {
        Text(
            if (failed) (pick(lang, "Oltin narxlari hozircha mavjud emas.", "Цены на золото сейчас недоступны.", "Gold prices are not available yet."))
            else (pick(lang, "Yuklanmoqda…", "Загрузка…", "Loading…")),
            style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
        )
        return
    }
    val updated = listOfNotNull(g.date, g.time).joinToString(", ")
    if (updated.isNotEmpty()) {
        Text((pick(lang, "Yangilangan: ", "Обновлено: ", "Updated: ")) + updated + " · cbu.uz", style = VsType.Meta, color = VsInkSoft)
    }

    // Per-gram price from the largest bar (cheapest per gram) and the round-trip loss on it.
    val ref = g.bars.maxBy { it.grams }
    val perGram = (ref.sell / ref.grams).roundToLong()
    val loss = ref.buyback?.let { (ref.sell - it).toDouble() / ref.sell * 100 }
    Text(
        (pick(lang, "1 gramm ≈ ", "1 грамм ≈ ", "1 gram ≈ ")) + "${perGram.formatUzsL()} $unit" +
            (pick(lang, " (${gramsText(ref.grams)} g quymada)", " (в слитке ${gramsText(ref.grams)} г)", " (in a ${gramsText(ref.grams)} g bar)")),
        style = VsType.BodyMedium, color = VsInk, modifier = Modifier.padding(top = 8.dp)
    )
    if (loss != null) {
        Text(
            pick(lang, "Sotib olib darhol qaytarsangiz: yo‘qotish ≈ ${loss.fixed(1)}%",
                "Купить и сразу сдать обратно: потеря ≈ ${loss.fixed(1)}%",
                "Buy and sell straight back: loss ≈ ${loss.fixed(1)}%"),
            style = VsType.Meta, color = VsInkSoft
        )
    }

    GoldTrend(ru, unit)

    // Table header
    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(pick(lang, "Og‘irlik", "Вес", "Weight"), style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(0.8f))
        Text(pick(lang, "Sotish", "Продажа", "Sell"), style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
        Text(pick(lang, "Qaytarib olish", "Обратный выкуп", "Buy-back"), style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
    }
    HorizontalDivider(color = VsLine, modifier = Modifier.padding(top = 4.dp))
    g.bars.sortedBy { it.grams }.forEach { bar -> GoldRow(bar, ru, unit) }
}

@Composable
private fun GoldRow(bar: GoldBar, ru: Boolean, unit: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(role = Role.Button) { open = !open }
            .padding(vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("${gramsText(bar.grams)} g", style = VsType.BodyMedium, color = VsInk, modifier = Modifier.weight(0.8f).alignByBaseline())
            Text(bar.sell.formatUzsL(), style = VsType.BodyMedium, color = VsInk, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f).alignByBaseline())
            Text(bar.buyback?.formatUzsL() ?: "—", style = VsType.Body, color = VsInk, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f).alignByBaseline())
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
                            pick(lang, "Qadog‘i buzilgan bo‘lsa qaytarib olish: ", "Выкуп при повреждённой упаковке: ", "Buy-back with damaged packaging: "),
                            style = VsType.Meta, color = VsInkSoft
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${it.formatUzsL()} $unit", style = VsType.Meta, color = VsInk)
                    }
                }
            }
        }
    }
    HorizontalDivider(color = VsLine)
}


/** Price per gram over the days our server has collected; needs two days before it can draw anything. */
@Composable
private fun GoldTrend(ru: Boolean, unit: String) {
    var points by remember { mutableStateOf<List<Pair<String, Double>>?>(null) }
    LaunchedEffect(Unit) { points = runCatching { fetchGoldHistory() }.getOrNull() }
    val p = points ?: return
    if (p.size < 2) {
        Text(
            tr("Narx tarixi har kuni toʻplanmoqda — grafik ikkinchi kundan koʻrinadi.",
                "История цены собирается ежедневно — график появится со второго дня.",
                "Price history is collected daily — the chart appears from the second day."),
            style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
        )
        return
    }
    val first = p.first(); val last = p.last()
    val pct = (last.second - first.second) / first.second * 100
    Text(
        (if (pct >= 0) "▲ " else "▼ ") + "${kotlin.math.abs(pct).fixed(2)}% · " +
            "${formatShortDate(first.first)} – ${formatShortDate(last.first)}",
        style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
    )
    val ink = VsInk; val accent = com.example.bankkurslari.ui.theme.VsEmerald
    val lo = p.minOf { it.second }; val hi = p.maxOf { it.second }
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 56.dp)) {
        val pad = 4.dp.toPx()
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        fun x(i: Int) = pad + i * (size.width - 2 * pad) / (p.size - 1)
        fun y(v: Double) = pad + (1f - ((v - lo) / span).toFloat()) * (size.height - 2 * pad)
        val path = androidx.compose.ui.graphics.Path()
        p.forEachIndexed { i, (_, v) -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        drawPath(path, ink, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        drawCircle(accent, 3.dp.toPx(), androidx.compose.ui.geometry.Offset(x(p.lastIndex), y(last.second)))
    }
}
