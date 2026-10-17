package com.example.bankkurslari

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

/** Bottom sheet shared by Kurs list rows and Xarita pins (spec 6.4). Primary action = route. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BankSheet(
    item: RankedBank,
    currency: Currency,
    amount: Double,
    direction: ConvertMode,
    updatedAt: String?,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val route = LocalRoute.current
    val ru = kursIsRu()
    val sell = direction == ConvertMode.SELL_USD
    val som = kSom(ru)
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var explain by remember { mutableStateOf(false) }
    val buy = item.rate.buy
    val sellRate = item.rate.sell
    val spread = item.rate.spread
    val time = updatedAt?.formatUpdatedAt()
    val shareText = kShareText(item, currency, amount, updatedAt, false)
    val schedule = formatHours(item.hours, ru) ?: item.hours?.takeIf { it.isNotBlank() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.rate.bank, style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (favorite) tr("Tanlanganlardan olib tashlash", "Убрать из избранного", "Remove from favourites") else tr("Tanlanganlarga qoʻshish", "Добавить в избранное", "Add to favourites"),
                        tint = if (favorite) VsEmerald else VsInkSoft
                    )
                }
            }
            val addrLine = listOfNotNull(item.address?.takeIf { it.isNotBlank() }, item.distanceKm?.let { kKm(it, ru) }).joinToString(" · ")
            if (addrLine.isNotEmpty()) Text(addrLine, style = VsType.Meta, color = VsInkSoft)
            Text(
                kHoursLong(item.open, ru),
                style = if (item.open.known) VsType.Body else VsType.Meta,
                color = when { !item.open.known -> VsInkSoft; item.open.open -> VsEmerald; else -> VsInk }
            )
            if (schedule != null && item.open.known) Text(schedule, style = VsType.Meta, color = VsInkSoft)

            KHairline()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SideCell(
                    label = tr("Sotib olish", "Покупка", "Buy"), hint = tr("bank sotib oladi", "банк покупает", "bank buys"),
                    value = buy, highlight = sell, modifier = Modifier.weight(1f)
                )
                SideCell(
                    label = tr("Sotish", "Продажа", "Sell"), hint = tr("bank sotadi", "банк продаёт", "bank sells"),
                    value = sellRate, highlight = !sell, modifier = Modifier.weight(1f)
                )
            }
            if (spread != null) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).combinedClickable(
                        onClickLabel = tr("Izoh", "Пояснение", "Explanation"),
                        onLongClickLabel = tr("Izoh", "Пояснение", "Explanation"),
                        onClick = { explain = !explain },
                        onLongClick = { explain = !explain }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val diff = (pick(lang, "Farq ", "Разница ", "Spread ")) + "${kGroup(spread)} $som"
                    Text(diff + (time?.let { " · " + (pick(lang, "Yangilandi $it", "Обновлено $it", "Updated $it")) } ?: ""), style = VsType.Meta, color = VsInkSoft)
                }
                if (explain) {
                    Text(
                        tr("Sotish va sotib olish orasidagi farq", "Разница между продажей и покупкой", "Difference between the sell and buy rates"),
                        style = VsType.Meta, color = VsInkSoft
                    )
                }
            } else if (time != null) {
                Text(pick(lang, "Yangilandi $time", "Обновлено $time", "Updated $time"), style = VsType.Meta, color = VsInkSoft)
            }

            YesterdayLine(item, currency, ru)

            if (amount > 0) {
                val amt = kGroup(amount.toLong())
                val head = if (sell) (pick(lang, "Sizning $amt ${currency.code}", "Ваши $amt ${currency.code}", "Your $amt ${currency.code}"))
                else (pick(lang, "$amt ${currency.code} uchun toʻlov", "Оплата за $amt ${currency.code}", "Payment for $amt ${currency.code}"))
                Text("$head: ${kGroup(item.total)} $som", style = VsType.BodyMedium, color = VsInk)
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton(
                    text = tr("Yoʻnalish", "Маршрут", "Directions"), icon = Icons.Outlined.Directions, primary = true,
                    enabled = item.lat != null && item.lng != null,
                    modifier = Modifier.weight(1f)
                ) {
                    val la = item.lat
                    val ln = item.lng
                    if (la != null && ln != null) route(RouteTarget(la, ln, item.rate.bank))
                }
                KIconAction(Icons.Outlined.Share, tr("Ulashish", "Поделиться", "Share")) { kShare(shareText) }
            }
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun SideCell(label: String, hint: String, value: Int?, highlight: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = VsType.Meta, color = VsInkSoft)
        Text(
            value?.let { kGroup(it) } ?: "—",
            style = VsType.Title, color = if (highlight) VsEmerald else VsInk
        )
        Text(hint, style = VsType.Meta, color = VsInkSoft)
    }
}


/** "Kecha: 12 020 (+20) · 12 100 (−10)" from our own collected history; hidden until a previous day exists. */
@Composable
private fun YesterdayLine(item: RankedBank, currency: Currency, ru: Boolean) {
    var prev by remember(item.rate.bank, currency) { mutableStateOf<BankDay?>(null) }
    LaunchedEffect(item.rate.bank, currency) {
        val hist = runCatching { fetchBankHistory(currency, 7) }.getOrNull().orEmpty()
        val today = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        prev = hist.lastOrNull { it.first < today }?.second?.get(item.rate.bank)
    }
    val p = prev ?: return
    fun part(now: Int?, then: Int?): String? {
        if (now == null || then == null) return null
        val d = now - then
        val sign = when { d > 0 -> "+"; d < 0 -> "−"; else -> "±" }
        return "${kGroup(then)} ($sign${kGroup(kotlin.math.abs(d))})"
    }
    val buy = part(item.rate.buy, p.lastBuy)
    val sell = part(item.rate.sell, p.lastSell)
    if (buy == null && sell == null) return
    Text(
        tr("Kecha: ", "Вчера: ", "Yesterday: ") +
            listOfNotNull(buy?.let { tr("sotib olish ", "покупка ", "buy ") + it }, sell?.let { tr("sotish ", "продажа ", "sell ") + it }).joinToString(" · "),
        style = VsType.Meta, color = VsInkSoft
    )
}
