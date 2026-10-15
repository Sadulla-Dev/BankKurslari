package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val UNIT_UZ = "soʻm"
private const val UNIT_RU = "сум"

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
    direction: ConvertMode,
    onDirection: (ConvertMode) -> Unit,
    onOpenSettings: () -> Unit
) {
    val ru = lang == Lang.RU
    val unit = if (ru) UNIT_RU else UNIT_UZ
    var rangeDays by rememberSaveable { mutableIntStateOf(30) }
    var alerts by remember { mutableStateOf(Alerts.load()) }
    var showAdd by remember { mutableStateOf(false) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var selIndex by remember { mutableStateOf<Int?>(null) }

    val cur = payload.cbu.rate
    val todayEpoch = remember { epochDay(todayIso()) }
    val tickerHistory = tickers[currency]?.history.orEmpty()
    val series = remember(analytics, cur, tickerHistory) {
        buildSeries(analytics?.points.orEmpty(), cur, todayIso()).ifEmpty { buildSeries(tickerHistory, cur, todayIso()) }
    }

    val window = remember(series, rangeDays) { windowOf(series, rangeDays, todayEpoch) }
    val hasChart = window.size >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VsBg)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        // Ticker header (same as Kurs): the selected card already carries today's CBU rate and day change.
        TickerBar(
            currency = currency, onCurrency = { selIndex = null; onCurrency(it) }, tickers = tickers,
            updatedAt = payload.updatedAt, offline = offline, onOpenSettings = onOpenSettings
        )
        val periodDelta = if (hasChart) periodDeltaText(window, cur, rangeDays, todayEpoch, ru) else null
        Row(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(if (ru) "История курса ЦБ" else "MB kursi tarixi", style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f).alignByBaseline())
            AnimatedContent(periodDelta, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) }, label = "pd", modifier = Modifier.alignByBaseline()) { t ->
                if (t != null) Text(t, style = VsType.BodyMedium, color = VsInk)
            }
        }

        // Range selector sits directly on the chart it controls.
        val ranges = listOf(7 to (if (ru) "7д" else "7k"), 30 to (if (ru) "30д" else "30k"), 90 to (if (ru) "90д" else "90k"), 365 to (if (ru) "1г" else "1y"))
        Segmented(
            options = ranges.map { it.second },
            selected = ranges.indexOfFirst { it.first == rangeDays }.coerceAtLeast(0),
            onSelect = { selIndex = null; rangeDays = ranges[it].first },
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            height = 40.dp
        )
        if (hasChart) {
            val band = remember(series, todayEpoch) { bandOf(series, 30, todayEpoch) }
            HistoryChart(
                window = window,
                band = band,
                today = cur,
                days = rangeDays,
                selIndex = selIndex,
                onSelect = { selIndex = it },
                description = chartDescription(window, rangeDays, unit, ru),
                unit = unit
            )
            // Readout: min / max by default, the touched point while scrubbing (long-press + drag on the chart).
            val lo = window.minBy { it.rate }
            val hi = window.maxBy { it.rate }
            val dateFmt: (String) -> String = { if (rangeDays > 90) formatShortDate(it) + "." + it.substring(2, 4) else formatShortDate(it) }
            Text(
                "min ${lo.rate.roundToInt().formatUzs()} (${dateFmt(lo.date)})  ·  max ${hi.rate.roundToInt().formatUzs()} (${dateFmt(hi.date)})  ·  " +
                    (if (ru) "коснитесь графика" else "grafikka teging"),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        analyticsLoading -> if (ru) "История загружается…" else "Tarix yuklanmoqda…"
                        else -> if (ru) "История пока недоступна. Попробуйте позже." else "Tarix hozircha mavjud emas. Keyinroq urinib koʻring."
                    },
                    style = VsType.Meta, color = VsInkSoft
                )
            }
        }

        SectionDivider()

        // Daily change calendar: facts only, coloured from the user's side (sell / buy).
        ChangeCalendar(
            series = series, currency = currency, sell = direction == ConvertMode.SELL_USD,
            ru = ru, unit = unit, onDirection = onDirection
        )

        SectionDivider()

        // Bugun bozorda
        val name = currency.displayName(lang).let { if (ru) it.lowercase() else it }
        val time = payload.updatedAt?.formatUpdatedAt().orEmpty()
        val bestSell = payload.banks.filter { it.buy != null }.maxByOrNull { it.buy!! }
        val bestBuy = payload.banks.filter { it.sell != null }.minByOrNull { it.sell!! }
        Text(if (ru) "Сегодня на рынке" else "Bugun bozorda", style = VsType.Title, color = VsInk)
        MarketRow(
            title = if (ru) "Если продаёте $name — больше всех платит" else "$name sotsangiz — eng koʻp toʻlaydi",
            value = bestSell?.buy.formatUzs(), unit = unit,
            meta = listOfNotNull(bestSell?.bank, time.ifEmpty { null }).joinToString(" · ")
        )
        HorizontalDivider(color = VsLine)
        MarketRow(
            title = if (ru) "Если покупаете $name — дешевле всех продаёт" else "$name olsangiz — eng arzon sotadi",
            value = bestBuy?.sell.formatUzs(), unit = unit,
            meta = listOfNotNull(bestBuy?.bank, time.ifEmpty { null }).joinToString(" · ")
        )

        SectionDivider()

        // Alerts
        Text(if (ru) "Оповещения" else "Ogohlantirishlar", style = VsType.Title, color = VsInk)
        if (alerts.isEmpty()) {
            Text(
                if (ru) "Сообщим, когда курс достигнет нужного значения." else "Kurs kerakli qiymatga yetganda xabar beramiz.",
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
            )
        }
        alerts.forEach { a ->
            AlertRow(a, ru, unit) {
                Alerts.remove(a.id)
                alerts = Alerts.load()
            }
            HorizontalDivider(color = VsLine)
        }
        val full = alerts.size >= Alerts.MAX
        Spacer(Modifier.height(12.dp))
        KButton(
            text = if (ru) "Добавить оповещение" else "Ogohlantirish qoʻshish",
            icon = Icons.Outlined.NotificationsNone, primary = false, enabled = !full,
            modifier = Modifier.fillMaxWidth()
        ) { showAdd = true }
        if (full) {
            Text(
                if (ru) "Не больше ${Alerts.MAX} оповещений. Удалите одно, чтобы добавить новое."
                else "Eng koʻpi bilan ${Alerts.MAX} ta ogohlantirish. Yangisini qoʻshish uchun birini oʻchiring.",
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
            )
        }

        SectionDivider()

        // Sources
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { sourcesOpen = !sourcesOpen },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                (if (ru) "Источник: bank.uz, cbu.uz" else "Manba: bank.uz, cbu.uz") + if (time.isNotEmpty()) " · $time" else "",
                style = VsType.Meta, color = VsInkSoft
            )
        }
        if (sourcesOpen) {
            Text(
                if (ru) "bank.uz — курсы коммерческих банков (наличные).\ncbu.uz — официальный курс Центрального банка и его история."
                else "bank.uz — tijorat banklarining naqd kurslari.\ncbu.uz — Markaziy bankning rasmiy kursi va uning tarixi.",
                style = VsType.Meta, color = VsInkSoft
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

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = VsLine)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun MarketRow(title: String, value: String, unit: String, meta: String) {
    // Two baseline-aligned lines (same grid as the Kurs list): title · value, meta · unit.
    Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, style = VsType.Body, color = VsInk, modifier = Modifier.weight(1f).alignByBaseline())
            Spacer(Modifier.width(12.dp))
            Text(value, style = VsType.BodyMedium, color = VsInk, modifier = Modifier.alignByBaseline())
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(meta, style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f).alignByBaseline())
            Text(unit, style = VsType.Meta, color = VsInkSoft, modifier = Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun AlertRow(a: RateAlert, ru: Boolean, unit: String, onDelete: () -> Unit) {
    val cur = Currency.entries.firstOrNull { it.code == a.currency } ?: Currency.USD
    val name = if (ru) cur.nameRu.lowercase() else cur.nameUz
    val sell = a.side == AlertSide.SELL
    val op = if (sell) "≥" else "≤"
    val title = if (ru) "${if (sell) "Продаёте" else "Покупаете"} $name $op ${a.threshold.formatUzs()} $unit"
    else "$name ${if (sell) "sotsangiz" else "olsangiz"} $op ${a.threshold.formatUzs()} $unit"
    val scope = a.bank ?: if (ru) "Лучший на рынке" else "Bozordagi eng yaxshi"
    val status = if (a.fired) (if (ru) " · сработало" else " · ishga tushdi") else ""
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = VsType.Body, color = VsInk)
            Text(scope + status, style = VsType.Meta, color = VsInkSoft)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onDelete)
                .semantics { role = Role.Button; contentDescription = if (ru) "Удалить оповещение" else "Ogohlantirishni oʻchirish" },
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
    val unit = if (ru) UNIT_RU else UNIT_UZ
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
            Text(if (ru) "Новое оповещение" else "Yangi ogohlantirish", style = VsType.Title, color = VsInk)
            Spacer(Modifier.height(16.dp))
            SettingsSegmented(
                options = listOf(if (ru) "Я продаю" else "Men sotaman", if (ru) "Я покупаю" else "Men olaman"),
                selected = side.ordinal,
                onSelect = {
                    side = AlertSide.entries[it]
                    bank = null
                    refreshDefault(side, null)
                }
            )
            Text(
                if (side == AlertSide.SELL) {
                    if (ru) "Сообщим, когда за $name заплатят ${"≥"} указанной суммы." else "$name sotsangiz, kurs shu qiymatga yetganda yoki undan oshganda (≥) xabar beramiz."
                } else {
                    if (ru) "Сообщим, когда $name можно будет купить по курсу ≤ указанной суммы." else "$name olsangiz, kurs shu qiymatga tushganda yoki undan pasayganda (≤) xabar beramiz."
                },
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(if (ru) "Порог" else "Chegara", style = VsType.Meta, color = VsInkSoft)
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
                    if (ru) "Сейчас: ${now.formatUzs()} $unit" else "Hozir: ${now.formatUzs()} $unit",
                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (alreadyMet) {
                Text(
                    if (ru) "Условие уже выполнено. Сообщим, когда курс выйдет из условия и достигнет его снова."
                    else "Shart hozir bajarilgan. Kurs shartdan chiqib, qayta yetganda xabar beramiz.",
                    style = VsType.Meta, color = VsAmber, modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(if (ru) "Где" else "Qayerda", style = VsType.Meta, color = VsInkSoft)
            Spacer(Modifier.height(4.dp))
            SettingsSegmented(
                options = listOf(if (ru) "Лучший на рынке" else "Bozordagi eng yaxshi", if (ru) "Конкретный банк" else "Aniq bank"),
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
                            bank ?: (if (ru) "Выберите банк" else "Bankni tanlang"),
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
                Text(if (ru) "Сохранить" else "Saqlash", style = VsType.BodyMedium, color = if (valid) VsBg else VsInkSoft)
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

private const val CHART_HEIGHT_DP = 220

@Composable
private fun HistoryChart(
    window: List<HistoryPoint>,
    band: Pair<Double, Double>?,
    today: Double,
    days: Int,
    selIndex: Int?,
    onSelect: (Int?) -> Unit,
    description: String,
    unit: String
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val ink = VsInk
    val inkSoft = VsInkSoft
    val line = VsLine
    val bg = VsBg
    val accent = VsEmerald
    val bandColor = VsSurface
    val leftPx = with(density) { 52.dp.toPx() }
    val rightPx = with(density) { 8.dp.toPx() }
    val topPx = with(density) { 8.dp.toPx() }
    val bottomPx = with(density) { 24.dp.toPx() }
    val textPx = with(density) { 13.sp.toPx() }

    val t = remember(window) { window.map { epochDay(it.date).toDouble() } }
    // Line draws in left -> right whenever the range (window) changes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(window) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(600, easing = Motion.Decelerate))
    }
    // Crosshair fades in/out instead of popping.
    var lastSel by remember { mutableStateOf<Int?>(null) }
    if (selIndex != null) lastSel = selIndex
    val selA by animateFloatAsState(if (selIndex != null) 1f else 0f, tween(Motion.SHORT), label = "selA")
    // A light tick for every day the finger crosses.
    LaunchedEffect(selIndex) { if (selIndex != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    val lo0 = window.minOf { it.rate }
    val hi0 = window.maxOf { it.rate }
    val pad = if (hi0 - lo0 < 1e-6) hi0 * 0.01 else (hi0 - lo0) * 0.08
    val yMin = lo0 - pad
    val yMax = hi0 + pad
    val ticks = remember(window) { niceTicks(yMin, yMax) }
    val t0 = t.first()
    val t1 = t.last()

    fun xOf(w: Float, tv: Double) = leftPx + ((tv - t0) / (t1 - t0).coerceAtLeast(1.0)).toFloat() * (w - leftPx - rightPx)
    fun yOf(h: Float, v: Double) = topPx + (1f - ((v - yMin) / (yMax - yMin)).toFloat()) * (h - topPx - bottomPx)

    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 13.sp, color = inkSoft, fontFeatureSettings = "tnum")
    val bubbleStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT_DP.dp)
            .semantics { contentDescription = description }
            // Scrub: touch shows the value immediately; horizontal drag moves it; lifting the finger hides it.
            .pointerInput(window) {
                fun nearest(x: Float): Int {
                    var best = 0
                    var bd = Float.MAX_VALUE
                    for (i in t.indices) {
                        val d = abs(xOf(size.width.toFloat(), t[i]) - x)
                        if (d < bd) { bd = d; best = i }
                    }
                    return best
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onSelect(nearest(down.position.x))
                    var pointer = down.id
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == pointer } ?: break
                        if (!ch.pressed) break
                        val dx = ch.position.x - ch.previousPosition.x
                        val dy = ch.position.y - ch.previousPosition.y
                        // Mostly-horizontal movement belongs to the chart; vertical goes to the page scroll.
                        if (abs(dx) >= abs(dy)) ch.consume()
                        onSelect(nearest(ch.position.x))
                        pointer = ch.id
                    }
                    onSelect(null)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val hair = 1.dp.toPx()

        // 30-day min–max band (clipped to the visible value range)
        if (band != null) {
            val top = yOf(h, minOf(band.second, yMax))
            val bot = yOf(h, maxOf(band.first, yMin))
            if (bot > top) drawRect(bandColor, topLeft = Offset(leftPx, top), size = androidx.compose.ui.geometry.Size(w - leftPx - rightPx, bot - top))
        }

        // y grid + tick labels
        ticks.forEach { v ->
            val y = yOf(h, v)
            drawLine(line, Offset(leftPx, y), Offset(w - rightPx, y), strokeWidth = hair)
            drawTextAt(measurer, v.roundToLong().formatUzsL(), leftPx - 8.dp.toPx(), y + textPx * 0.35f, axisStyle, TextAnchor.RIGHT)
        }
        drawLine(line, Offset(leftPx, h - bottomPx), Offset(w - rightPx, h - bottomPx), strokeWidth = hair)

        // x labels
        for (k in 0..3) {
            val tv = t0 + (t1 - t0) * k / 3.0
            val x = xOf(w, tv)
            val anchor = when (k) { 0 -> TextAnchor.LEFT; 3 -> TextAnchor.RIGHT; else -> TextAnchor.CENTER }
            drawTextAt(measurer, dayLabel(tv.toLong(), days > 90), if (k == 0) leftPx else x, h - 4.dp.toPx(), axisStyle, anchor)
        }

        // today's level: dashed
        val ty = yOf(h, today)
        drawLine(
            inkSoft, Offset(leftPx, ty), Offset(w - rightPx, ty), strokeWidth = hair,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        // series + soft area under it
        val path = Path()
        window.forEachIndexed { i, p ->
            val x = xOf(w, t[i]); val y = yOf(h, p.rate)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(path)
            lineTo(xOf(w, t.last()), h - bottomPx)
            lineTo(xOf(w, t.first()), h - bottomPx)
            close()
        }
        val p = reveal.value
        clipRect(right = leftPx + (w - leftPx) * p) {
            drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0f)), startY = topPx, endY = h - bottomPx))
            drawPath(path, ink, style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // today dot (last point) appears once the line reaches it
        val lx = xOf(w, t.last()); val ly = yOf(h, window.last().rate)
        val dotA = ((p - 0.85f) / 0.15f).coerceIn(0f, 1f)
        drawCircle(bg, radius = 6.dp.toPx() * dotA, center = Offset(lx, ly))
        drawCircle(accent, radius = 4.dp.toPx() * dotA, center = Offset(lx, ly))

        // crosshair + bubble: date · rate · difference to today
        val si = lastSel
        if (si != null && si in window.indices && selA > 0f) {
            val sx = xOf(w, t[si]); val sy = yOf(h, window[si].rate)
            drawLine(inkSoft.copy(alpha = selA), Offset(sx, topPx), Offset(sx, h - bottomPx), strokeWidth = hair)
            drawCircle(bg.copy(alpha = selA), radius = 7.dp.toPx(), center = Offset(sx, sy))
            drawCircle(ink.copy(alpha = selA), radius = 4.5.dp.toPx(), center = Offset(sx, sy))

            val pt = window[si]
            val diff = today - pt.rate
            val l1 = "${formatFullDate(pt.date)}  ${pt.rate.formatRate()} $unit"
            val l2 = if (si == window.lastIndex) null else
                (if (unit == UNIT_RU) "→ сегодня " else "→ bugun ") + (if (diff >= 0) "+" else "−") + fmtSom(diff, today) + " $unit"
            val padB = 8.dp.toPx()
            val lineH = textPx * 1.3f
            val bw = maxOf(measurer.widthOf(l1, bubbleStyle), l2?.let { measurer.widthOf(it, axisStyle) } ?: 0f) + padB * 2
            val bh = lineH * (if (l2 == null) 1 else 2) + padB * 1.5f
            val bx = (sx - bw / 2).coerceIn(leftPx, w - rightPx - bw)
            val above = sy - bh - 12.dp.toPx() > topPx
            val by = if (above) sy - bh - 12.dp.toPx() else sy + 12.dp.toPx()
            drawRoundRect(
                ink.copy(alpha = 0.92f * selA), topLeft = Offset(bx, by),
                size = androidx.compose.ui.geometry.Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
            drawTextAt(measurer, l1, bx + padB, by + padB * 0.75f + textPx, bubbleStyle.copy(color = bg.copy(alpha = selA)))
            if (l2 != null) {
                drawTextAt(measurer, l2, bx + padB, by + padB * 0.75f + textPx + lineH, axisStyle.copy(color = bg.copy(alpha = 0.75f * selA)))
            }
        }
    }
}

/* ---------------- data helpers ---------------- */


fun epochDay(iso: String): Long =
    runCatching { LocalDate.parse(iso.take(10)).toEpochDays() }.getOrDefault(0L)

private fun todayIso(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

/** "dd.MM" or, with [withYear], "dd.MM.yy". */
private fun dayLabel(epochDay: Long, withYear: Boolean): String {
    val d = LocalDate.fromEpochDays(epochDay.toInt())
    val dm = "${d.day.toString().padStart(2, '0')}.${d.month.number.toString().padStart(2, '0')}"
    return if (withYear) "$dm.${(d.year % 100).toString().padStart(2, '0')}" else dm
}

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

private fun windowOf(series: List<HistoryPoint>, days: Int, todayEpoch: Long): List<HistoryPoint> {
    if (series.isEmpty()) return series
    val w = series.filter { epochDay(it.date) >= todayEpoch - days }
    return if (w.size >= 2) w else series.takeLast(2)
}

private fun bandOf(series: List<HistoryPoint>, days: Int, todayEpoch: Long): Pair<Double, Double>? {
    val w = series.filter { epochDay(it.date) >= todayEpoch - days }
    if (w.size < 2) return null
    return w.minOf { it.rate } to w.maxOf { it.rate }
}

private fun periodLabel(days: Int, ru: Boolean): String = when (days) {
    365 -> if (ru) "1 год" else "1 yil"
    else -> if (ru) "$days дн." else "$days kun"
}

private fun periodDeltaText(window: List<HistoryPoint>, cur: Double, days: Int, todayEpoch: Long, ru: Boolean): String? {
    val first = window.first()
    if (first.rate <= 0) return null
    val span = (todayEpoch - epochDay(first.date)).toInt()
    // Name the real period if history does not reach back as far as requested.
    val label = if (span >= days * 0.85) periodLabel(days, ru) else periodLabel(span.coerceAtLeast(1), ru).let { if (span >= 365) periodLabel(365, ru) else it }
    val pct = (cur - first.rate) / first.rate * 100.0
    val arrow = when { abs(pct) < 0.005 -> "–"; pct > 0 -> "▲"; else -> "▼" }
    return "$arrow ${abs(pct).fixed(2)}% · $label"
}

private fun dayDeltaText(diff: Double, unit: String, ru: Boolean): String? {
    if (abs(diff) < 0.005) return null
    val v = if (abs(diff) >= 1) diff.roundToInt().toDouble().formatSigned() else diff.formatSigned()
    return "$v $unit · ${if (ru) "вчера" else "kecha"}"
}

private fun chartDescription(window: List<HistoryPoint>, days: Int, unit: String, ru: Boolean): String {
    val lo = window.minBy { it.rate }; val hi = window.maxBy { it.rate }
    return (if (ru) "График курса ЦБ за ${periodLabel(days, ru)}. " else "MB kursi grafigi, ${periodLabel(days, ru)}. ") +
        "min ${lo.rate.roundToInt().formatUzs()} ${formatFullDate(lo.date)}, max ${hi.rate.roundToInt().formatUzs()} ${formatFullDate(hi.date)} $unit"
}

private fun niceTicks(lo: Double, hi: Double): List<Double> {
    fun ticksFor(parts: Double): List<Double> {
        val raw = (hi - lo) / parts
        if (raw <= 0) return emptyList()
        val mag = 10.0.pow(floor(log10(raw)))
        val norm = raw / mag
        val step = mag * when { norm <= 1 -> 1.0; norm <= 2 -> 2.0; norm <= 5 -> 5.0; else -> 10.0 }
        val out = mutableListOf<Double>()
        var v = ceil(lo / step) * step
        while (v <= hi + 1e-9) { out += v; v += step }
        return out
    }
    var t = ticksFor(3.0)
    if (t.size > 4) t = ticksFor(2.0)
    if (t.size < 2) t = listOf(lo, hi)
    return t
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
