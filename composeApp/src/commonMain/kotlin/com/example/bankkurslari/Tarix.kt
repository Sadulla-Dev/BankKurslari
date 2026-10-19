package com.example.bankkurslari

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsRoseText
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


/** Tarix (spec 6.3): "Is today normal?" History, facts and alerts. No forecast, no advice. */
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
    offline: Boolean,
    amount: Double,
    onOpenSettings: () -> Unit
) {
    val ru = lang == Lang.RU
    val unit = somUnit()
    var alerts by remember { mutableStateOf(Alerts.load()) }
    var showAdd by remember { mutableStateOf(false) }
    var sourcesOpen by remember { mutableStateOf(false) }

    val cur = payload.cbu.rate
    val tickerHistory = tickers[currency]?.history.orEmpty()
    val series = remember(analytics, cur, tickerHistory) {
        buildSeries(analytics?.points.orEmpty(), cur, todayIso()).ifEmpty { buildSeries(tickerHistory, cur, todayIso()) }
    }


    val name = currency.displayName(lang).let { if (ru) it.lowercase() else it }
    val time = payload.updatedAt?.formatUpdatedAt().orEmpty()
    val bestSell = payload.banks.filter { it.buy != null }.maxByOrNull { it.buy!! }
    val bestBuy = payload.banks.filter { it.sell != null }.minByOrNull { it.sell!! }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VsBg)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Topbar: currency pill + share (+ settings, kept so it stays reachable from this tab).
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Segmented(
                options = Currency.entries.map { it.code },
                selected = Currency.entries.indexOf(currency),
                onSelect = { onCurrency(Currency.entries[it]) },
                cellWidth = 64.dp
            )
            Spacer(Modifier.weight(1f))
            val shareDesc = pick(lang, "Grafikni ulashish", "Поделиться графиком", "Share chart")
            VsIconButton(Icons.Outlined.IosShare, shareDesc, onClick = {
                shareText("${currency.code} · MB / CBU: ${cur.formatRate()} ${unit}" + if (time.isNotEmpty()) " · $time" else "")
            })
        }

        if (series.size >= 2) {
            InteractiveHistory(series = series, today = cur, currency = currency, amount = amount, ru = ru, unit = unit)
        } else {
            TrendHero(label = pick(lang, "Markaziy bank kursi", "Курс ЦБ", "Central Bank rate"), today = cur, unit = unit, delta = null)
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(VsShape.Panel).background(VsCard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        analyticsLoading -> pick(lang, "Tarix yuklanmoqda…", "История загружается…", "Loading history…")
                        else -> pick(lang, "Tarix hozircha mavjud emas. Keyinroq urinib koʻring.", "История пока недоступна. Попробуйте позже.", "History is not available yet. Please try again later.")
                    },
                    style = VsType.Meta, color = VsInkSoft
                )
            }
        }

        // At banks today
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(pick(lang, "Bugun banklarda", "Сегодня в банках", "At banks today"), style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f))
            Text(
                pick(lang, "${payload.banks.size} ta bank", "${payload.banks.size} банков", "${payload.banks.size} banks"),
                style = VsType.Meta, color = VsInkSoft
            )
        }
        val spread = if (bestBuy?.sell != null && bestSell?.buy != null && bestBuy.sell!! > bestSell.buy!!) bestBuy.sell!! - bestSell.buy!! else null
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrendStat(pick(lang, "Bank oladi", "Банк покупает", "Best buy"), bestSell?.buy.formatUzs(), bestSell?.bank?.let(::shortBank), Modifier.weight(1f))
            TrendStat(pick(lang, "Bank sotadi", "Банк продаёт", "Best sell"), bestBuy?.sell.formatUzs(), bestBuy?.bank?.let(::shortBank), Modifier.weight(1f))
            if (spread != null) TrendStat(pick(lang, "Farq", "Разница", "Spread"), spread.toLong().formatUzsL(), unit, Modifier.weight(1f))
        }

        GoldSection(ru = ru, unit = unit)

        // Alerts
        Text(pick(lang, "Ogohlantirishlar", "Оповещения", "Alerts"), style = VsType.Title, color = VsInk, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        val full = alerts.size >= Alerts.MAX
        VsPanel {
            if (alerts.isEmpty()) {
                Text(
                    pick(lang, "Kurs kerakli qiymatga yetganda xabar beramiz.", "Сообщим, когда курс достигнет нужного значения.", "We will notify you when the rate reaches your target."),
                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
                VsDivider()
            }
            alerts.forEach { a ->
                AlertRow(a, ru, unit) {
                    Alerts.remove(a.id)
                    alerts = Alerts.load()
                }
                VsDivider()
            }
            val addText = pick(lang, "Ogohlantirish qoʻshish", "Добавить оповещение", "Add alert")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .clickable(enabled = !full, role = Role.Button) { showAdd = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(addText, style = VsType.BodyMedium, color = if (full) VsInkSoft else VsEmerald, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.NotificationsNone, null, tint = if (full) VsInkSoft else VsEmerald, modifier = Modifier.size(20.dp))
            }
        }
        if (full) {
            Text(
                pick(lang, "Eng koʻpi bilan ${Alerts.MAX} ta ogohlantirish. Yangisini qoʻshish uchun birini oʻchiring.",
                    "Не больше ${Alerts.MAX} оповещений. Удалите одно, чтобы добавить новое.",
                    "Up to ${Alerts.MAX} alerts. Delete one to add a new one."),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Sources
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { sourcesOpen = !sourcesOpen }.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                (pick(lang, "Manba: bank.uz, cbu.uz", "Источник: bank.uz, cbu.uz", "Source: bank.uz, cbu.uz")) + if (time.isNotEmpty()) " · $time" else "",
                style = VsType.Meta, color = VsInkSoft
            )
        }
        if (sourcesOpen) {
            Text(
                pick(lang, "bank.uz — tijorat banklarining naqd kurslari.\ncbu.uz — Markaziy bankning rasmiy kursi va uning tarixi.",
                    "bank.uz — курсы коммерческих банков (наличные).\ncbu.uz — официальный курс Центрального банка и его история.",
                    "bank.uz — commercial banks’ cash rates.\ncbu.uz — the Central Bank’s official rate and its history."),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(horizontal = 4.dp)
            )
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

/** White stat card for the three-up "At banks today" row: label, figure, grey sub line. */
@Composable
private fun TrendStat(label: String, value: String, note: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.clip(shape).background(VsCard).border(1.dp, VsHair, shape).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = VsType.Title.copy(fontSize = 24.sp, lineHeight = 30.sp), color = VsInk, maxLines = 1)
        if (note != null) Text(note, style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "Asia Alliance Bank" -> "Asia Alliance", "Xalq Banki" -> "Xalq". */
private fun shortBank(name: String): String =
    name.replace(Regex("\\s+(Bank|Banki|bank|banki|Банк)$"), "").ifBlank { name }

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
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = VsType.BodyMedium, color = VsInk)
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
        shape = VsShape.Tray,
        containerColor = VsCard,
        contentColor = VsInk,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(VsHair))
            Spacer(Modifier.height(18.dp))
            Text(pick(lang, "Yangi ogohlantirish", "Новое оповещение", "New alert"), style = VsType.Title, color = VsInk)
            Spacer(Modifier.height(16.dp))
            SettingsSegmented(
                options = listOf(pick(lang, "Sotish", "Продать", "Sell"), pick(lang, "Olish", "Купить", "Buy")),
                selected = side.ordinal,
                onSelect = {
                    side = AlertSide.entries[it]
                    bank = null
                    refreshDefault(side, null)
                }
            )
            Text(
                if (side == AlertSide.SELL) {
                    pick(lang, "$name sotsangiz: kurs shu qiymatga yetganda yoki oshganda (≥) xabar beramiz.", "Если продаёте $name: сообщим, когда курс достигнет этого значения или выше (≥).", "Selling ${name.lowercase()}: we notify you when the rate reaches or exceeds (≥) this value.")
                } else {
                    pick(lang, "$name olsangiz: kurs shu qiymatga tushganda yoki pasayganda (≤) xabar beramiz.", "Если покупаете $name: сообщим, когда курс опустится до этого значения или ниже (≤).", "Buying ${name.lowercase()}: we notify you when the rate drops to or below (≤) this value.")
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
                    .clip(VsShape.Stat)
                    .background(VsGround)
                    .padding(horizontal = 14.dp),
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
                        "Условие уже выполнено. Сообщим, когда курс выйдет из диапазона и снова достигнет значения.",
                        "Already met. We’ll notify you when the rate leaves this range and reaches it again."),
                    style = VsType.Meta, color = VsRoseText, modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(pick(lang, "Qayerda", "Где", "Where"), style = VsType.Meta, color = VsInkSoft)
            Spacer(Modifier.height(4.dp))
            SettingsSegmented(
                options = listOf(pick(lang, "Bozordagi eng yaxshi", "Лучший на рынке", "Best in the market"), pick(lang, "Muayyan bank", "Конкретный банк", "Specific bank")),
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
                            .clip(VsShape.Stat)
                            .background(VsGround)
                            .clickable { bankMenu = true }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            bank ?: (pick(lang, "Bankni tanlang", "Выберите банк", "Choose a bank")),
                            style = VsType.Body, color = if (bank == null) VsInkSoft else VsInk
                        )
                    }
                    DropdownMenu(expanded = bankMenu, onDismissRequest = { bankMenu = false }, containerColor = VsCard) {
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
            val saveText = pick(lang, "Saqlash", "Сохранить", "Save")
            if (valid) {
                VsPrimaryButton(
                    text = saveText,
                    onClick = {
                        requestNotifications()
                        onSave(
                            RateAlert(
                                id = Alerts.newId(), currency = currency.code, side = side, threshold = threshold,
                                bank = if (specificBank) bank else null,
                                fired = alreadyMet, lastFiredAt = 0L
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(54.dp).clip(VsShape.Button).background(VsGround),
                    contentAlignment = Alignment.Center
                ) { Text(saveText, style = VsType.BodyMedium, color = VsInkSoft) }
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

internal fun todayIso(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

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
