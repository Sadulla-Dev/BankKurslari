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
import androidx.compose.material.icons.outlined.NearMe
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

/**
 * Bank detail, opened from a Home row or a map pin. Total for the user's amount, the bank's two rates,
 * the nearest branches (tap one to route there), then one primary action: directions to the nearest branch.
 * Location is asked here, in context, the first time the user wants distances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSheet(
    item: RankedBank,
    currency: Currency,
    amount: Double,
    direction: ConvertMode,
    updatedAt: String?,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    branches: List<OsmBranch> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    onRequestLocation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val route = LocalRoute.current
    val ru = kursIsRu()
    val sell = direction == ConvertMode.SELL_USD
    val som = kSom(ru)
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareText = kShareText(item, currency, amount, updatedAt, false)
    val near = remember(item.rate, branches, userLocation) { branchesOf(item.rate, branches, userLocation).take(3) }
    val target = near.firstOrNull()?.let { RouteTarget(it.lat, it.lng, item.rate.bank) }
        ?: item.lat?.let { la -> item.lng?.let { ln -> RouteTarget(la, ln, item.rate.bank) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.rate.bank, style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (favorite) tr("Tanlanganlardan olib tashlash", "Убрать из избранного", "Remove from favourites") else tr("Tanlanganlarga qoʻshish", "Добавить в избранное", "Add to favourites"),
                        tint = VsInk
                    )
                }
            }

            if (amount > 0) {
                val amt = kGroup(amount.toLong())
                Text("$amt ${currency.code} → ${kGroup(item.total)} $som", style = VsType.Body, color = VsInk, modifier = Modifier.padding(top = 8.dp))
            }
            val rates = listOfNotNull(
                item.rate.buy?.let { pick(lang, "Sotib oladi ${kGroup(it)}", "Покупает ${kGroup(it)}", "Buys at ${kGroup(it)}") },
                item.rate.sell?.let { pick(lang, "Sotadi ${kGroup(it)}", "Продаёт ${kGroup(it)}", "Sells at ${kGroup(it)}") }
            ).joinToString(" · ")
            if (rates.isNotEmpty()) Text(rates, style = VsType.Caption, color = VsInkSoft)
            YesterdayLine(item, currency, ru)

            // Nearest branches
            if (near.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    if (userLocation != null) tr("Eng yaqin filiallar", "Ближайшие отделения", "Nearest branches") else tr("Filiallar", "Отделения", "Branches"),
                    style = VsType.Caption, color = VsInkSoft
                )
                near.forEachIndexed { i, b ->
                    if (i > 0) KHairline()
                    BranchRow(b, ru) { route(RouteTarget(b.lat, b.lng, item.rate.bank)) }
                }
                if (userLocation == null && onRequestLocation != null) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onRequestLocation),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.NearMe, null, tint = VsInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Eng yaqin filialni koʻrsatish uchun joylashuvga ruxsat bering",
                                "Разрешите геолокацию, чтобы показать ближайшее отделение",
                                "Allow location to show the nearest branch"),
                            style = VsType.Caption, color = VsInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KButton(
                    text = tr("Yoʻnalish", "Маршрут", "Directions"), icon = Icons.Outlined.Directions, primary = true,
                    enabled = target != null, modifier = Modifier.weight(1f)
                ) { target?.let(route) }
                Spacer(Modifier.width(8.dp))
                KIconAction(Icons.Outlined.Share, tr("Ulashish", "Поделиться", "Share")) { kShare(shareText) }
            }
            updatedAgo(updatedAt)?.let {
                Text(it, style = VsType.Caption, color = VsInkSoft, modifier = Modifier.padding(top = 16.dp))
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun BranchRow(b: NearBranch, ru: Boolean, onClick: () -> Unit) {
    val meta = listOfNotNull(
        b.km?.takeIf { it <= 100 }?.let { kKm(it, ru) },
        b.open.takeIf { it.known }?.let { kHoursShort(it, ru) }
    ).joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = tr("Yoʻnalish", "Маршрут", "Directions"), onClick = onClick)
            .alpha(if (mapIsClosed(b.open)) 0.4f else 1f)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(b.address?.takeIf { it.isNotBlank() } ?: tr("Manzil koʻrsatilmagan", "Адрес не указан", "No address"), style = VsType.Body, color = VsInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (meta.isNotEmpty()) Text(meta, style = VsType.Caption, color = VsInkSoft)
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
