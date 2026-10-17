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

    Text(if (ru) "Золотые слитки ЦБ" else "Markaziy bank oltin quymalari", style = VsType.Title, color = VsInk)
    val g = gold
    if (g == null || g.bars.isEmpty()) {
        Text(
            if (failed) (if (ru) "Цены на золото сейчас недоступны." else "Oltin narxlari hozircha mavjud emas.")
            else (if (ru) "Загрузка…" else "Yuklanmoqda…"),
            style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
        )
        return
    }
    val updated = listOfNotNull(g.date, g.time).joinToString(", ")
    if (updated.isNotEmpty()) {
        Text((if (ru) "Обновлено: " else "Yangilangan: ") + updated + " · cbu.uz", style = VsType.Meta, color = VsInkSoft)
    }

    // Per-gram price from the largest bar (cheapest per gram) and the round-trip loss on it.
    val ref = g.bars.maxBy { it.grams }
    val perGram = (ref.sell / ref.grams).roundToLong()
    val loss = ref.buyback?.let { (ref.sell - it).toDouble() / ref.sell * 100 }
    Text(
        (if (ru) "1 грамм ≈ " else "1 gramm ≈ ") + "${perGram.formatUzsL()} $unit" +
            (if (ru) " (в слитке ${gramsText(ref.grams)} г)" else " (${gramsText(ref.grams)} g quymada)"),
        style = VsType.BodyMedium, color = VsInk, modifier = Modifier.padding(top = 8.dp)
    )
    if (loss != null) {
        Text(
            if (ru) "Купить и сразу сдать обратно: потеря ≈ ${loss.fixed(1)}%"
            else "Sotib olib darhol qaytarsangiz: yo‘qotish ≈ ${loss.fixed(1)}%",
            style = VsType.Meta, color = VsInkSoft
        )
    }

    // Table header
    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(if (ru) "Вес" else "Og‘irlik", style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(0.8f))
        Text(if (ru) "Продажа" else "Sotish", style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
        Text(if (ru) "Обратный выкуп" else "Qaytarib olish", style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
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
                    (if (ru) "1 грамм: " else "1 gramm: ") + "${perGram.formatUzsL()} $unit",
                    style = VsType.Meta, color = VsInkSoft
                )
                bar.buybackDamaged?.let {
                    Row {
                        Text(
                            if (ru) "Выкуп при повреждённой упаковке: " else "Qadog‘i buzilgan bo‘lsa qaytarib olish: ",
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
