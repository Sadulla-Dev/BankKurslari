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
    androidx.compose.runtime.LaunchedEffect(item.rate.bank) { debugLog("BankSheet shown: ${item.rate.bank}") }
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
                        contentDescription = if (favorite) tr("Tanlanganlardan olib tashlash", "Убрать из избранного") else tr("Tanlanganlarga qoʻshish", "Добавить в избранное"),
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
                    label = tr("Sotib olish", "Покупка"), hint = tr("bank sotib oladi", "банк покупает"),
                    value = buy, highlight = sell, modifier = Modifier.weight(1f)
                )
                SideCell(
                    label = tr("Sotish", "Продажа"), hint = tr("bank sotadi", "банк продаёт"),
                    value = sellRate, highlight = !sell, modifier = Modifier.weight(1f)
                )
            }
            if (spread != null) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).combinedClickable(
                        onClickLabel = tr("Izoh", "Пояснение"),
                        onLongClickLabel = tr("Izoh", "Пояснение"),
                        onClick = { explain = !explain },
                        onLongClick = { explain = !explain }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val diff = (if (ru) "Разница " else "Farq ") + "${kGroup(spread)} $som"
                    Text(diff + (time?.let { " · " + (if (ru) "Обновлено $it" else "Yangilandi $it") } ?: ""), style = VsType.Meta, color = VsInkSoft)
                }
                if (explain) {
                    Text(
                        tr("Sotish va sotib olish orasidagi farq", "Разница между продажей и покупкой"),
                        style = VsType.Meta, color = VsInkSoft
                    )
                }
            } else if (time != null) {
                Text(if (ru) "Обновлено $time" else "Yangilandi $time", style = VsType.Meta, color = VsInkSoft)
            }

            if (amount > 0) {
                val amt = kGroup(amount.toLong())
                val head = if (sell) (if (ru) "Ваши $amt ${currency.code}" else "Sizning $amt ${currency.code}")
                else (if (ru) "Оплата за $amt ${currency.code}" else "$amt ${currency.code} uchun toʻlov")
                Text("$head: ${kGroup(item.total)} $som", style = VsType.BodyMedium, color = VsInk)
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton(
                    text = tr("Yoʻnalish", "Маршрут"), icon = Icons.Outlined.Directions, primary = true,
                    enabled = item.lat != null && item.lng != null,
                    modifier = Modifier.weight(1f)
                ) {
                    val la = item.lat
                    val ln = item.lng
                    if (la != null && ln != null) route(RouteTarget(la, ln, item.rate.bank))
                }
                KIconAction(Icons.Outlined.Share, tr("Ulashish", "Поделиться")) { kShare(shareText) }
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
