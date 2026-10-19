package com.example.bankkurslari

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


/**
 * Tarix: "is today's rate normal?" The Central Bank rate as the hero with its chart, then the one insight History adds
 * over Home: today's best buy / best sell across banks and the gap between them. Gold and alerts below. No forecast.
 */
@Composable
fun TarixScreen(
    innerPadding: PaddingValues,
    currency: Currency,
    onCurrency: (Currency) -> Unit,
    lang: Lang,
    payload: RatesPayload,
    analytics: Analytics?,
    analyticsLoading: Boolean,
    tickers: Map<Currency, TickerData>,
    offline: Boolean
) {
    val ru = lang == Lang.RU
    val unit = somUnit()
    var alerts by remember { mutableStateOf(Alerts.load()) }
    var showAdd by remember { mutableStateOf(false) }
    var sourcesOpen by remember { mutableStateOf(false) }
    val hPad = 24.dp

    val cur = payload.cbu.rate
    val tickerHistory = tickers[currency]?.history.orEmpty()
    val series = remember(analytics, cur, tickerHistory) {
        buildSeries(analytics?.points.orEmpty(), cur, todayIso()).ifEmpty { buildSeries(tickerHistory, cur, todayIso()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VsBg)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        CurrencySegmented(currency, onCurrency, Modifier.padding(horizontal = hPad))
        Spacer(Modifier.height(16.dp))
        if (series.size >= 2) {
            HistoryBlock(series = series, currency = currency, ru = ru, hPad = hPad)
        } else {
            Box(Modifier.fillMaxWidth().height(320.dp).padding(horizontal = hPad), contentAlignment = Alignment.CenterStart) {
                Text(
                    if (analyticsLoading) pick(lang, "Tarix yuklanmoqda…", "История загружается…", "Loading history…")
                    else pick(lang, "Tarix hozircha mavjud emas.", "История пока недоступна.", "History is not available yet."),
                    style = VsType.Body, color = VsInkSoft
                )
            }
        }

        Column(Modifier.padding(horizontal = hPad)) {
            // Today across banks: the spread between the best buy and the best sell.
            val bestBuyFromBank = payload.banks.mapNotNull { it.sell }.minOrNull()   // cheapest place to buy
            val bestSellToBank = payload.banks.mapNotNull { it.buy }.maxOrNull()     // best place to sell
            if (bestBuyFromBank != null && bestSellToBank != null) {
                Spacer(Modifier.height(40.dp))
                Text(pick(lang, "Bugun banklarda", "Сегодня в банках", "Today at banks"), style = VsType.Caption, color = VsInkSoft)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    SpreadCell(pick(lang, "Eng arzon olish", "Купить дешевле всего", "Best buy"), kGroup(bestBuyFromBank), Modifier.weight(1f))
                    SpreadCell(pick(lang, "Eng qimmat sotish", "Продать дороже всего", "Best sell"), kGroup(bestSellToBank), Modifier.weight(1f))
                    SpreadCell(pick(lang, "Farq", "Разница", "Spread"), kGroup(bestBuyFromBank - bestSellToBank), Modifier.weight(0.7f))
                }
            }

            Spacer(Modifier.height(48.dp))
            GoldSection(ru = ru, unit = unit)

            Spacer(Modifier.height(48.dp))
            Text(pick(lang, "Ogohlantirishlar", "Оповещения", "Alerts"), style = VsType.Title, color = VsInk)
            if (alerts.isEmpty()) {
                Text(
                    pick(lang, "Kurs kerakli qiymatga yetganda xabar beramiz.", "Сообщим, когда курс достигнет нужного значения.", "We will notify you when the rate reaches your target."),
                    style = VsType.Caption, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
                )
            }
            alerts.forEachIndexed { i, a ->
                if (i > 0) HorizontalDivider(color = VsLine)
                AlertRow(a, ru, unit) {
                    Alerts.remove(a.id)
                    alerts = Alerts.load()
                }
            }
            val full = alerts.size >= Alerts.MAX
            Box(Modifier.offset(x = (-16).dp)) {
                KButton(
                    text = pick(lang, "Ogohlantirish qoʻshish", "Добавить оповещение", "Add alert"),
                    icon = Icons.Outlined.NotificationsNone, primary = false, enabled = !full
                ) { showAdd = true }
            }
            if (full) {
                Text(
                    pick(lang, "Eng koʻpi bilan ${Alerts.MAX} ta ogohlantirish. Yangisini qoʻshish uchun birini oʻchiring.",
                        "Не больше ${Alerts.MAX} оповещений. Удалите одно, чтобы добавить новое.",
                        "Up to ${Alerts.MAX} alerts. Delete one to add a new one."),
                    style = VsType.Caption, color = VsInkSoft
                )
            }

            Spacer(Modifier.height(40.dp))
            val time = payload.updatedAt?.formatUpdatedAt().orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { sourcesOpen = !sourcesOpen },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (pick(lang, "Manba: bank.uz, cbu.uz", "Источник: bank.uz, cbu.uz", "Source: bank.uz, cbu.uz")) + if (time.isNotEmpty()) " · $time" else "",
                    style = VsType.Caption, color = VsInkSoft
                )
            }
            if (sourcesOpen) {
                Text(
                    pick(lang, "bank.uz — tijorat banklarining naqd kurslari.\ncbu.uz — Markaziy bankning rasmiy kursi va uning tarixi.",
                        "bank.uz — курсы коммерческих банков (наличные).\ncbu.uz — официальный курс Центрального банка и его история.",
                        "bank.uz — commercial banks’ cash rates.\ncbu.uz — the Central Bank’s official rate and its history."),
                    style = VsType.Caption, color = VsInkSoft
                )
            }
        }
    }

    if (showAdd) {
        AddAlertSheet(
            currency = currency, lang = lang, payload = payload,
            onDismiss = { showAdd = false },
            onSave = { a ->
                Alerts.add(a)
                alerts = Alerts.load()
                PlatformHooks.onAlertsChanged()
                showAdd = false
            }
        )
    }
}

@Composable
private fun SpreadCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = VsType.Body, color = VsInk)
        Text(label, style = VsType.Caption, color = VsInkSoft, maxLines = 2)
    }
}

@Composable
private fun AlertRow(a: RateAlert, ru: Boolean, unit: String, onDelete: () -> Unit) {
    val cur = Currency.entries.firstOrNull { it.code == a.currency } ?: Currency.USD
    val sell = a.side == AlertSide.SELL
    val op = if (sell) "≥" else "≤"
    val v = "$op ${a.threshold.formatUzs()} $unit"
    val title = pick(lang,
        "${cur.nameUz} ${if (sell) "sotsangiz" else "olsangiz"} $v",
        "${if (sell) "Продаёте" else "Покупаете"} ${cur.nameRu.lowercase()} $v",
        "${if (sell) "Selling" else "Buying"} ${cur.nameEn.lowercase()} $v")
    val scope = a.bank ?: pick(lang, "Bozordagi eng yaxshi", "Лучший на рынке", "Best in the market")
    val status = if (a.fired) (pick(lang, " · ishga tushdi", " · сработало", " · triggered")) else ""
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = VsType.Body, color = VsInk)
            Text(scope + status, style = VsType.Meta, color = VsInkSoft)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onDelete)
                .semantics { role = Role.Button; contentDescription = pick(lang, "Ogohlantirishni oʻchirish", "Удалить оповещение", "Delete alert") },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Delete, contentDescription = null, tint = VsInkSoft) }
    }
}

/* ---------------- add alert ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlertSheet(
    currency: Currency,
    lang: Lang,
    payload: RatesPayload,
    onDismiss: () -> Unit,
    onSave: (RateAlert) -> Unit
) {
    val ru = lang == Lang.RU
    val unit = somUnit()
    val name = currency.displayName(lang).let { if (ru) it.lowercase() else it }
    var side by remember { mutableStateOf(AlertSide.SELL) }
    var specificBank by remember { mutableStateOf(false) }
    var bank by remember { mutableStateOf<String?>(null) }
    var bankMenu by remember { mutableStateOf(false) }
    var edited by remember { mutableStateOf(false) }

    fun probe(s: AlertSide, b: String?) = RateAlert("", currency.code, s, 0, b)
    fun nowValue(s: AlertSide, b: String?): Int? = Alerts.currentValue(probe(s, b), payload.banks)
    fun defaultThreshold(s: AlertSide, b: String?): String {
        val v = nowValue(s, b) ?: return ""
        // Round to the next/previous 50 so the default is a meaningful step away from today.
        val t = if (s == AlertSide.SELL) (v / 50 + 1) * 50 else ((v - 1) / 50) * 50
        return t.toString()
    }
    var text by remember { mutableStateOf(defaultThreshold(AlertSide.SELL, null)) }
    fun refreshDefault(s: AlertSide, b: String?) { if (!edited) text = defaultThreshold(s, b) }

    val bankNames = remember(payload, side) {
        payload.banks.filter { if (side == AlertSide.SELL) it.buy != null else it.sell != null }.map { it.bank }.sorted()
    }
    val threshold = text.toIntOrNull() ?: 0
    val now = nowValue(side, if (specificBank) bank else null)
    val valid = threshold > 0 && (!specificBank || bank != null)
    val alreadyMet = now != null && threshold > 0 && Alerts.isMet(side, now, threshold)

    val requestNotifications = rememberNotificationPermissionRequest()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = VsBg,
        contentColor = VsInk,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(pick(lang, "Yangi ogohlantirish", "Новое оповещение", "New alert"), style = VsType.Title, color = VsInk)
            Spacer(Modifier.height(16.dp))
            SettingsSegmented(
                options = listOf(pick(lang, "Men sotaman", "Я продаю", "I sell"), pick(lang, "Men olaman", "Я покупаю", "I buy")),
                selected = side.ordinal,
                onSelect = {
                    side = AlertSide.entries[it]
                    bank = null
                    refreshDefault(side, null)
                }
            )
            Text(
                if (side == AlertSide.SELL) {
                    pick(lang, "$name sotsangiz, kurs shu qiymatga yetganda yoki undan oshganda (≥) xabar beramiz.", "Сообщим, когда за $name заплатят ${"≥"} указанной суммы.", "Selling ${name.lowercase()}: we notify you when the rate reaches or exceeds (≥) this value.")
                } else {
                    pick(lang, "$name olsangiz, kurs shu qiymatga tushganda yoki undan pasayganda (≤) xabar beramiz.", "Сообщим, когда $name можно будет купить по курсу ≤ указанной суммы.", "Buying ${name.lowercase()}: we notify you when the rate drops to or below (≤) this value.")
                },
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(pick(lang, "Chegara", "Порог", "Threshold"), style = VsType.Meta, color = VsInkSoft)
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VsSurface)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { v -> edited = true; text = v.filter { it.isDigit() }.take(7) },
                    textStyle = VsType.Body.copy(color = VsInk),
                    cursorBrush = SolidColor(VsEmerald),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsTransformation,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                )
                Text(unit, style = VsType.Meta, color = VsInkSoft)
            }
            if (now != null) {
                Text(
                    pick(lang, "Hozir: ${now.formatUzs()} $unit", "Сейчас: ${now.formatUzs()} $unit", "Now: ${now.formatUzs()} $unit"),
                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (alreadyMet) {
                Text(
                    pick(lang, "Shart hozir bajarilgan. Kurs shartdan chiqib, qayta yetganda xabar beramiz.",
                        "Условие уже выполнено. Сообщим, когда курс выйдет из условия и достигнет его снова.",
                        "Already met. We’ll notify you when the rate leaves this range and reaches it again."),
                    style = VsType.Meta, color = VsAmber, modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(pick(lang, "Qayerda", "Где", "Where"), style = VsType.Meta, color = VsInkSoft)
            Spacer(Modifier.height(4.dp))
            SettingsSegmented(
                options = listOf(pick(lang, "Bozordagi eng yaxshi", "Лучший на рынке", "Best in the market"), pick(lang, "Aniq bank", "Конкретный банк", "Specific bank")),
                selected = if (specificBank) 1 else 0,
                onSelect = {
                    specificBank = it == 1
                    if (!specificBank) { bank = null; refreshDefault(side, null) }
                }
            )
            if (specificBank) {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VsSurface)
                            .clickable { bankMenu = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            bank ?: (pick(lang, "Bankni tanlang", "Выберите банк", "Choose a bank")),
                            style = VsType.Body, color = if (bank == null) VsInkSoft else VsInk
                        )
                    }
                    DropdownMenu(expanded = bankMenu, onDismissRequest = { bankMenu = false }, containerColor = VsBg) {
                        bankNames.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, style = VsType.Body, color = VsInk) },
                                onClick = { bank = b; bankMenu = false; refreshDefault(side, b) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (valid) VsEmerald else VsSurface)
                    .then(
                        if (!valid) Modifier else Modifier.clickable {
                            requestNotifications()
                            onSave(
                                RateAlert(
                                    id = Alerts.newId(), currency = currency.code, side = side, threshold = threshold,
                                    bank = if (specificBank) bank else null,
                                    fired = alreadyMet, lastFiredAt = 0L
                                )
                            )
                        }
                    )
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center
            ) {
                Text(pick(lang, "Saqlash", "Сохранить", "Save"), style = VsType.BodyMedium, color = if (valid) VsBg else VsInkSoft)
            }
        }
    }
}

/** Shows digits grouped by spaces (12 000) while the stored text stays plain digits. */
private val ThousandsTransformation = VisualTransformation { input ->
    val raw = input.text
    val out = StringBuilder()
    raw.forEachIndexed { i, c ->
        if (i > 0 && (raw.length - i) % 3 == 0) out.append(' ')
        out.append(c)
    }
    val map = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var seps = 0
            for (i in 1 until offset) if ((raw.length - i) % 3 == 0) seps++
            return offset + seps
        }
        override fun transformedToOriginal(offset: Int): Int {
            val s = out.toString()
            var spaces = 0
            for (i in 0 until minOf(offset, s.length)) if (s[i] == ' ') spaces++
            return (offset - spaces).coerceIn(0, raw.length)
        }
    }
    TransformedText(AnnotatedString(out.toString()), map)
}

/* ---------------- chart ---------------- */

/* ---------------- data helpers ---------------- */


fun epochDay(iso: String): Long =
    runCatching { LocalDate.parse(iso.take(10)).toEpochDays() }.getOrDefault(0L)

private fun todayIso(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

/** History points + today's official rate as the last point; empty when the points do not belong to [cur]. */
private fun buildSeries(points: List<HistoryPoint>, cur: Double, todayIso: String): List<HistoryPoint> {
    if (points.size < 2 || cur <= 0) return emptyList()
    val sorted = points.sortedBy { it.date }
    // Guard against stale points of another currency during a currency switch.
    if (abs(sorted.last().rate - cur) / cur > 0.3) return emptyList()
    val last = sorted.last()
    return when {
        last.date.take(10) == todayIso -> sorted.dropLast(1) + HistoryPoint(todayIso, cur)
        last.date.take(10) < todayIso -> sorted + HistoryPoint(todayIso, cur)
        else -> sorted
    }
}

fun formatShortDate(iso: String): String {
    // "2026-10-13" -> "13.10"
    val parts = iso.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}" else iso
}

fun formatFullDate(iso: String): String {
    // "2026-10-13" -> "13.10.2026"
    val parts = iso.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
}
