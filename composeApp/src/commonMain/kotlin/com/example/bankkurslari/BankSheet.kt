package com.example.bankkurslari

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

/**
 * Bank tray shared by Kurs list rows and Xarita pins: tile + name, what you get, what you lose against [best],
 * buy / sell tiles, branch line, primary route action. [best] is optional; without it the "less than" note is hidden.
 */
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
    onDismiss: () -> Unit,
    best: RankedBank? = null
) {
    val route = LocalRoute.current
    val ru = kursIsRu()
    val sell = direction == ConvertMode.SELL_USD
    val som = kSom(ru)
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val buy = item.rate.buy
    val sellRate = item.rate.sell
    val time = updatedAt?.formatUpdatedAt()
    val shareText = kShareText(item, currency, amount, updatedAt, false)
    val schedule = formatHours(item.hours, ru) ?: item.hours?.takeIf { it.isNotBlank() }
    val prev = rememberBankYesterday(item, currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = VsCard,
        shape = VsShape.Tray,
        tonalElevation = 0.dp,
        scrimColor = VsInk.copy(alpha = 0.42f),
        dragHandle = { VsSheetHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // header: tile, name, open line, save
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BankTile(sheetBankInitials(item.rate.bank), size = 44.dp, best = best != null && best.rate.bank == item.rate.bank)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.rate.bank, style = VsType.Title, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        kHoursLong(item.open, ru), style = VsType.Meta,
                        color = if (item.open.known && item.open.open) VsEmerald else VsInkSoft
                    )
                }
                VsIconButton(
                    if (favorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (favorite) tr("Sevimlilardan olib tashlash", "Убрать из избранного", "Remove from favourites") else tr("Sevimlilarga qoʻshish", "Добавить в избранное", "Add to favourites"),
                    onToggleFavorite, tint = if (favorite) VsEmerald else VsInk
                )
            }

            // what you get / pay, and what you lose against the best bank
            if (amount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        Text(kGroup(item.total), style = VsType.Display.copy(fontSize = 36.sp, lineHeight = 36.sp), color = VsInk, maxLines = 1)
                        val amt = kGroup(amount.toLong())
                        Text(
                            if (sell) pick(lang, "$som, $amt ${currency.code} uchun", "$som за $amt ${currency.code}", "$som for $amt ${currency.code}")
                            else pick(lang, "$som, $amt ${currency.code} uchun", "$som за $amt ${currency.code}", "$som for $amt ${currency.code}"),
                            style = VsType.BodyMedium, color = VsInkSoft, modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    if (best != null && best.rate.bank != item.rate.bank) {
                        val diff = kotlin.math.abs(best.total - item.total)
                        if (diff > 0) {
                            val further = if (item.distanceKm != null && best.distanceKm != null && item.distanceKm > best.distanceKm)
                                item.distanceKm - best.distanceKm else null
                            val base = if (sell) pick(lang, "${kGroup(diff)} $som kam, ${best.rate.bank}ga nisbatan", "на ${kGroup(diff)} $som меньше, чем в ${best.rate.bank}", "${kGroup(diff)} $som less than ${best.rate.bank}")
                            else pick(lang, "${kGroup(diff)} $som koʻp, ${best.rate.bank}ga nisbatan", "на ${kGroup(diff)} $som больше, чем в ${best.rate.bank}", "${kGroup(diff)} $som more than ${best.rate.bank}")
                            VsNote(if (further != null) base + pick(lang, ", ${kKm(further, ru)} uzoqroq", ", на ${kKm(further, ru)} дальше", ", ${kKm(further, ru)} further") else base)
                        }
                    }
                }
            }

            // bank buys / sells
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VsStat(
                    tr("Bank oladi", "Банк покупает", "Bank buys"), buy?.let { kGroup(it) } ?: "—",
                    Modifier.weight(1f), delta = todayDelta(buy, prev?.lastBuy)
                )
                VsStat(
                    tr("Bank sotadi", "Банк продаёт", "Bank sells"), sellRate?.let { kGroup(it) } ?: "—",
                    Modifier.weight(1f), delta = todayDelta(sellRate, prev?.lastSell)
                )
            }
            if (time != null) Text(pick(lang, "Yangilandi $time", "Обновлено $time", "Updated $time"), style = VsType.Meta, color = VsInkSoft)

            // branch
            val street = item.address?.takeIf { it.isNotBlank() }
            if (street != null || item.distanceKm != null) {
                Column {
                    Text(tr("Eng yaqin filial", "Ближайший филиал", "Nearest branch"), style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = VsInk, modifier = Modifier.padding(bottom = 8.dp))
                    VsDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(street ?: item.rate.bank, style = VsType.BodyMedium, color = VsInk)
                            if (schedule != null && item.open.known) Text(schedule, style = VsType.Meta, color = VsInkSoft)
                        }
                        item.distanceKm?.let { Text(kKm(it, ru), style = VsType.Value.copy(fontSize = 15.sp), color = VsInk) }
                    }
                }
            }

            // actions
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val canRoute = item.lat != null && item.lng != null
                VsPrimaryButton(
                    tr("Yoʻnalish", "Маршрут", "Directions"),
                    onClick = {
                        val la = item.lat
                        val ln = item.lng
                        if (la != null && ln != null) route(RouteTarget(la, ln, item.rate.bank))
                    },
                    modifier = Modifier.weight(1f).alpha(if (canRoute) 1f else 0.4f),
                    icon = Icons.Outlined.Directions
                )
                VsSecondaryButton(Icons.Outlined.Share, tr("Ulashish", "Поделиться", "Share"), { kShare(shareText) })
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private fun sheetBankInitials(name: String): String {
    val words = name.trim().split(' ', '-', '.').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2)
        else -> "${words[0].first()}${words[1].first()}"
    }.uppercase()
}

@Composable
private fun todayDelta(now: Int?, then: Int?): String? {
    if (now == null || then == null) return null
    val d = now - then
    if (d == 0) return null
    return kDelta(d.toLong()) + " " + tr("bugun", "за сегодня", "today")
}

/** Last stored day before today for this bank (our own collected history); null until one exists. */
@Composable
private fun rememberBankYesterday(item: RankedBank, currency: Currency): BankDay? {
    var prev by remember(item.rate.bank, currency) { mutableStateOf<BankDay?>(null) }
    LaunchedEffect(item.rate.bank, currency) {
        val hist = runCatching { fetchBankHistory(currency, 7) }.getOrNull().orEmpty()
        val today = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        prev = hist.lastOrNull { it.first < today }?.second?.get(item.rate.bank)
    }
    return prev
}

/** Grab handle for every tray-style ModalBottomSheet (40 x 5, hairline colour). */
@Composable
internal fun VsSheetHandle() {
    Box(Modifier.padding(top = 10.dp, bottom = 8.dp).width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(VsHair))
}
