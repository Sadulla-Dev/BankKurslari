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
import com.example.bankkurslari.ui.theme.VsRoseText
import com.example.bankkurslari.ui.theme.VsTealTint
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
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
    best: RankedBank? = null,
    onShowOnMap: (() -> Unit)? = null
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
    var locationOn by remember { mutableStateOf(hasLocationPermission()) }
    val requestLocation = rememberLocationPermissionRequest { granted -> locationOn = granted }

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
            val isBest = best != null && best.rate.bank == item.rate.bank
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BankTile(sheetBankInitials(item.rate.bank), size = 44.dp, best = isBest)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.rate.bank, style = VsType.Title.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                        color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        sheetOpenLine(item.open), style = VsType.Meta,
                        color = if (item.open.known && item.open.open) VsEmerald else VsInkSoft
                    )
                }
                VsIconButton(
                    if (favorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (favorite) tr("Sevimlilardan olib tashlash", "Убрать из избранного", "Remove from favourites") else tr("Sevimlilarga qoʻshish", "Добавить в избранное", "Add to favourites"),
                    onToggleFavorite, tint = if (favorite) VsEmerald else VsInk
                )
            }

            // what you get / pay, then how it compares with the best bank
            if (amount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        Text(kGroup(item.total), style = VsType.Display.copy(fontSize = 40.sp, lineHeight = 40.sp), color = VsInk, maxLines = 1)
                        Text(som, style = VsType.BodyMedium.copy(fontSize = 18.sp), color = VsInkSoft, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    val amt = kGroup(amount.toLong())
                    val forText = pick(lang, "$amt ${currency.nameUz.lowercase()} uchun", "за $amt ${currency.code}", "for $amt ${currency.code}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isBest) {
                            Text(
                                tr("Eng yaxshi narx", "Лучший курс", "Best rate"),
                                style = VsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = VsEmerald,
                                modifier = Modifier.clip(VsShape.Pill).background(VsTealTint).padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(forText, style = VsType.Body, color = VsInkSoft)
                    }
                    if (best != null && !isBest) {
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

            // bank buys / sells; the side that matters for the user's direction is coloured, the other stays quiet
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val buyD = rateMove(buy, prev?.lastBuy, higherIsBetter = true, relevant = sell, isSellRate = false)
                val sellD = rateMove(sellRate, prev?.lastSell, higherIsBetter = false, relevant = !sell, isSellRate = true)
                VsStat(
                    tr("Bank oladi", "Банк покупает", "Bank buys"), buy?.let { kGroup(it) } ?: "—",
                    Modifier.weight(1f), delta = buyD?.first, deltaColor = buyD?.second ?: VsInkSoft
                )
                VsStat(
                    tr("Bank sotadi", "Банк продаёт", "Bank sells"), sellRate?.let { kGroup(it) } ?: "—",
                    Modifier.weight(1f), delta = sellD?.first, deltaColor = sellD?.second ?: VsInkSoft
                )
            }

            // branch
            val addr = item.address?.takeIf { it.isNotBlank() }?.let { splitAddress(it) }
            val street = addr?.first
            if (street != null || item.distanceKm != null) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Eng yaqin filial", "Ближайший филиал", "Nearest branch"), style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = VsInk, modifier = Modifier.weight(1f))
                        if (onShowOnMap != null) {
                            Text(
                                tr("Xaritada", "На карте", "On map"),
                                style = VsType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = VsEmerald,
                                modifier = Modifier.clip(VsShape.Pill).clickable(role = Role.Button, onClick = onShowOnMap).padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                    }
                    VsDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(street ?: item.rate.bank, style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = VsInk)
                            val sub = listOfNotNull(addr?.second, schedule?.takeIf { item.open.known }).joinToString(", ")
                            if (sub.isNotEmpty()) Text(sub, style = VsType.Meta, color = VsInkSoft)
                        }
                        val km = item.distanceKm
                        if (km != null) {
                            Text(kKm(km, ru), style = VsType.Value.copy(fontSize = 15.sp), color = VsEmerald)
                        } else if (!locationOn) {
                            Text(
                                tr("masofa: yoqish", "расстояние: включить", "distance: turn on"),
                                style = VsType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = VsEmerald,
                                modifier = Modifier.clip(VsShape.Pill).clickable(role = Role.Button, onClick = requestLocation).padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
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
            if (time != null) {
                Text(
                    pick(lang, "Kurs $time da yangilangan", "Курс обновлён в $time", "Rate updated at $time"),
                    style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
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

/**
 * Day move of one rate as (text, colour). Null while there is no earlier day to compare with.
 * Only the side that matters for the user's direction ([relevant]) is coloured: teal when it moved in their favour, rose when against.
 */
@Composable
private fun rateMove(now: Int?, then: Int?, higherIsBetter: Boolean, relevant: Boolean, isSellRate: Boolean): Pair<String, androidx.compose.ui.graphics.Color>? {
    if (now == null || then == null) return null
    val d = now - then
    if (d == 0) return tr("oʻzgarmadi", "без изменений", "unchanged") to VsInkSoft
    val n = kGroup(kotlin.math.abs(d).toLong())
    val som = kSom(kursIsRu())
    val up = d > 0
    val text = if (isSellRate) {
        if (up) pick(lang, "$n $som qimmatladi", "подорожал на $n", "$n $som pricier")
        else pick(lang, "$n $som arzonladi", "подешевел на $n", "$n $som cheaper")
    } else {
        if (up) pick(lang, "$n $som oshdi", "вырос на $n", "up $n $som")
        else pick(lang, "$n $som tushdi", "снизился на $n", "down $n $som")
    }
    val good = up == higherIsBetter
    val color = if (!relevant) VsInkSoft else if (good) VsEmerald else VsRoseText
    return text to color
}

/** "Ochiq, 18:00 gacha" / "Yopiq, 09:00 da ochiladi". */
@Composable
private fun sheetOpenLine(o: OpenState): String = when {
    !o.known -> pick(lang, "Ish vaqti nomaʼlum", "Часы работы неизвестны", "Opening hours unknown")
    o.open -> o.untilText?.let { pick(lang, "Ochiq, $it gacha", "Открыто до $it", "Open until $it") } ?: pick(lang, "Ochiq", "Открыто", "Open")
    else -> o.untilText?.let { pick(lang, "Yopiq, $it da ochiladi", "Закрыто, откроется в $it", "Closed, opens at $it") } ?: pick(lang, "Yopiq", "Закрыто", "Closed")
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

/**
 * Raw bank.uz / OSM address -> (street + house number, district). Drops the postal code and the city,
 * "ko`chasi, 2a uy" becomes "koʻchasi 2a", "Yashnobod tumani" becomes "Yashnobod".
 */
internal fun splitAddress(raw: String): Pair<String, String?> {
    val parts = raw.split(',').map { it.trim().replace('`', 'ʻ').replace('\'', 'ʻ') }.filter { it.isNotEmpty() }
    var district: String? = null
    val rest = ArrayList<String>()
    for (p in parts) {
        val low = p.lowercase()
        when {
            Regex("^\\d{5,6}$").matches(p) -> {}
            low.endsWith(" sh.") || low.endsWith(" shahar") || low.endsWith(" viloyat") || low.endsWith(" vil.") || low.endsWith(" obl.") -> {}
            low.endsWith(" tumani") || low.endsWith(" tuman") || low.endsWith(" р-н") || low.endsWith(" район") ->
                district = p.substringBeforeLast(' ')
            else -> rest += p
        }
    }
    if (rest.size >= 2 && Regex("^\\d+[A-Za-zА-Яа-я]?(/\\d+)?( uy| дом)?$", RegexOption.IGNORE_CASE).matches(rest.last())) {
        val house = rest.removeAt(rest.lastIndex).replace(Regex(" (uy|дом)$", RegexOption.IGNORE_CASE), "")
        rest[rest.lastIndex] = rest.last() + " " + house.uppercase()
    }
    val street = rest.joinToString(", ").replace(Regex(" uy$", RegexOption.IGNORE_CASE), "").ifBlank { raw }
    return street to district
}
