package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/* ---------- shared formatting / actions (also used by BankSheet) ---------- */

@Composable
fun kursIsRu(): Boolean = LocalStrings.current === RuStrings

private const val NBSP = ' '
private const val MINUS = '−'

fun kGroup(n: Long): String {
    val s = abs(n).toString()
    val sb = StringBuilder()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(NBSP)
        sb.append(c)
    }
    return (if (n < 0) "$MINUS" else "") + sb
}

fun kGroup(n: Int): String = kGroup(n.toLong())

fun kSom(ru: Boolean) = if (ru) "сум" else "soʻm"

fun kDelta(d: Long): String = when {
    d < 0 -> "$MINUS${kGroup(-d)}"
    d > 0 -> "+${kGroup(d)}"
    else -> "0"
}

fun kKm(km: Double, ru: Boolean): String {
    val m = (km * 1000).roundToInt()
    if (m < 1000) return "$m ${if (ru) "м" else "m"}"
    val v = ((km * 10).roundToInt() / 10.0).toString()
    return (if (ru) v.replace('.', ',') else v) + " " + (if (ru) "км" else "km")
}

fun kRateFmt(v: Double, ru: Boolean): String =
    if (v >= 1000) kGroup(v.roundToLong())
    else v.fixed(2).let { if (ru) it.replace('.', ',') else it }

fun kHoursShort(o: OpenState, ru: Boolean): String = when {
    !o.known -> if (ru) "Время работы неизвестно" else "Ish vaqti nomaʻlum"
    o.open -> o.untilText?.let { if (ru) "до $it" else "$it gacha" } ?: (if (ru) "Открыто" else "Ochiq")
    else -> (if (ru) "Закрыто" else "Yopiq") +
        (o.untilText?.let { if (ru) " · откроется в $it" else " · $it da ochiladi" } ?: "")
}

fun kHoursLong(o: OpenState, ru: Boolean): String = when {
    !o.known -> if (ru) "Время работы неизвестно" else "Ish vaqti nomaʻlum"
    o.open -> (if (ru) "Открыто" else "Ochiq") +
        (o.untilText?.let { if (ru) " · до $it" else " · $it gacha" } ?: "")
    else -> kHoursShort(o, ru)
}

private fun kSpokenHours(o: OpenState, ru: Boolean): String = when {
    !o.known -> if (ru) "время работы неизвестно" else "ish vaqti nomaʻlum"
    o.open -> o.untilText?.let { if (ru) "открыто до $it" else "$it gacha ochiq" } ?: (if (ru) "открыто" else "ochiq")
    else -> if (ru) "закрыто" else "yopiq"
}

private fun kSpokenSom(n: Long, ru: Boolean): String {
    val m = n / 1_000_000
    val t = (n / 1000) % 1000
    val r = n % 1000
    val parts = mutableListOf<String>()
    if (m > 0) parts += if (ru) "$m млн" else "$m million"
    if (t > 0) parts += if (ru) "$t тыс." else "$t ming"
    if (r > 0 || parts.isEmpty()) parts += "$r"
    return parts.joinToString(" ") + " " + kSom(ru)
}

private fun kSpokenKm(km: Double, ru: Boolean): String {
    val m = (km * 1000).roundToInt()
    return if (m < 1000) "$m ${if (ru) "метров" else "metr"}"
    else {
        val v = ((km * 10).roundToInt() / 10.0).toString().let { if (ru) it.replace('.', ',') else it }
        "$v ${if (ru) "километра" else "kilometr"}"
    }
}

/** Parses payload.updatedAt (ISO-like) to epoch millis, or null. Offset-less times are local. */
fun kParseMillis(updatedAt: String?): Long? {
    if (updatedAt.isNullOrBlank()) return null
    runCatching { return Instant.parse(updatedAt).toEpochMilliseconds() }
    val clean = updatedAt.replace(Regex("""\.\d+"""), "")
    for (text in listOf(clean.take(19), clean.take(16))) {
        val ldt = runCatching { LocalDateTime.parse(text) }.getOrNull() ?: continue
        return ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    return null
}

private fun kAgeHours(updatedAt: String?): Double? =
    kParseMillis(updatedAt)?.let { (nowMillis() - it) / 3_600_000.0 }

@Composable
fun kShareText(item: RankedBank, currency: Currency, amount: Double, updatedAt: String?, best: Boolean): String {
    val ru = kursIsRu()
    val amt = if (amount > 0) amount.roundToLong() else 1L
    val total = if (amount > 0) item.total else item.perUnit.toLong()
    val time = updatedAt?.formatUpdatedAt()?.let { " ($it)" }.orEmpty()
    val head = if (best) (if (ru) "Лучший курс" else "Eng yaxshi kurs") else (if (ru) "Курс" else "Kurs")
    return "$head: ${kGroup(amt)} ${currency.code} → ${kGroup(total)} ${kSom(ru)} — ${item.rate.bank}$time"
}

fun kShare(text: String) = shareText(text)

/* ---------- small building blocks ---------- */

@Composable
fun KHairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(VsLine))
}

/** Filled primary / outlined secondary button, radius 12, >= 48 dp. */
@Composable
fun KButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, tonal: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val src = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(src)
            .clip(shape)
            .then(
                when {
                    primary && tonal -> Modifier.background(VsEmeraldSoft)
                    primary -> Modifier.background(VsEmerald)
                    else -> Modifier.border(1.dp, VsLine, shape)
                }
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(interactionSource = src, indication = ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val c = when { primary && tonal -> VsEmerald; primary -> VsBg; else -> VsInk }
        Icon(icon, null, tint = c, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = VsType.BodyMedium, color = c, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Square 48 dp outlined icon action that sits next to a primary [KButton]. */
@Composable
fun KIconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.size(48.dp).pressScale(src).clip(shape).border(1.dp, VsLine, shape)
            .clickable(interactionSource = src, indication = ripple(), role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = VsInk, modifier = Modifier.size(20.dp)) }
}

private class GroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val n = s.length
        val out = StringBuilder()
        val o2t = IntArray(n + 1)
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) out.append(NBSP)
            o2t[i] = out.length
            out.append(s[i])
        }
        o2t[n] = out.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = o2t[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int {
                var r = 0
                for (i in 0..n) if (o2t[i] <= offset) r = i
                return r
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

@Composable
private fun AmountField(currency: Currency, amountText: String, onAmount: (String) -> Unit) {
    val digits = amountText.filter { it.isDigit() }.trimStart('0').take(12)
    val label = tr("Summa, ${currency.code}", "Сумма, ${currency.code}")
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val lineColor by animateColorAsState(if (focused) VsEmerald else VsLine, Motion.color(), label = "amtline")
    val lineH by animateDpAsState(if (focused) 2.dp else 1.dp, tween(Motion.SHORT), label = "amth")
    val big = VsType.Title.copy(fontSize = 28.sp, lineHeight = 34.sp)
    BasicTextField(
        value = digits,
        onValueChange = { onAmount(it.filter { c -> c.isDigit() }.trimStart('0').take(12)) },
        singleLine = true,
        textStyle = big.copy(color = VsInk),
        cursorBrush = SolidColor(VsEmerald),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = GroupingTransformation(),
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { inner ->
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (digits.isEmpty()) Text("0", style = big, color = VsInkSoft.copy(alpha = 0.5f))
                        inner()
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(currency.code, style = VsType.Body, color = VsInkSoft)
                }
                Box(Modifier.fillMaxWidth().height(2.dp), contentAlignment = Alignment.BottomStart) {
                    Box(Modifier.fillMaxWidth().height(lineH).background(lineColor))
                }
            }
        }
    )
}

private fun quickAmounts(c: Currency): List<Long> =
    if (c == Currency.RUB) listOf(10_000, 50_000, 100_000, 500_000) else listOf(100, 500, 1_000, 5_000)

/* ---------- sort ---------- */

private enum class KursSort { SOM, DIST, RATE, OPEN }

@Composable
private fun sortLabel(s: KursSort): String = when (s) {
    KursSort.SOM -> tr("Soʻm", "Сумма")
    KursSort.DIST -> tr("Masofa", "Расстояние")
    KursSort.RATE -> tr("Kurs", "Курс")
    KursSort.OPEN -> tr("Ochiq hozir", "Открыто сейчас")
}

/* ---------- screen ---------- */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KursScreen(
    innerPadding: PaddingValues,
    currency: Currency,
    onCurrency: (Currency) -> Unit,
    lang: Lang,
    payload: RatesPayload,
    osmBranches: List<OsmBranch>,
    offline: Boolean,
    loading: Boolean,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    amountText: String,
    onAmount: (String) -> Unit,
    direction: ConvertMode,
    onDirection: (ConvertMode) -> Unit,
    userLocation: Pair<Double, Double>?,
    tickers: Map<Currency, TickerData>,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val route = LocalRoute.current
    val ru = kursIsRu()
    val sell = direction == ConvertMode.SELL_USD
    val amount = amountText.filter { it.isDigit() }.take(12).toDoubleOrNull() ?: 0.0
    val hasAmount = amount > 0

    var sort by rememberSaveable { mutableStateOf(KursSort.SOM) }
    var onlyOpen by rememberSaveable { mutableStateOf(false) }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var locChipDismissed by rememberSaveable { mutableStateOf(false) }
    var sheetBank by remember { mutableStateOf<String?>(null) }

    val ranked = remember(payload.banks, direction, amount, userLocation, osmBranches) {
        rankBanks(payload.banks, direction, amount, userLocation, osmBranches)
    }
    val flagged = remember(ranked, onlyOpen, onlyFav, favorites) {
        ranked.filter { (!onlyOpen || !(it.open.known && !it.open.open)) && (!onlyFav || it.rate.bank in favorites) }
    }
    val best = flagged.firstOrNull()
    val displayed = remember(flagged, sort, query) {
        val q = query.trim()
        val l = if (q.isEmpty()) flagged else flagged.filter { it.rate.bank.contains(q, ignoreCase = true) }
        when (sort) {
            KursSort.SOM, KursSort.RATE -> l
            KursSort.DIST -> l.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            KursSort.OPEN -> l.sortedBy { if (it.open.known && !it.open.open) 1 else 0 }
        }
    }

    val ageH = kAgeHours(payload.updatedAt)
    val stale6 = ageH != null && ageH > 6
    val stale12 = ageH != null && ageH > 12
    val timeText = payload.updatedAt?.formatUpdatedAt()
    val noData = payload.banks.isEmpty()
    val showSkeleton = loading && noData
    val anyDistance = ranked.any { it.distanceKm != null }
    val cbu = payload.cbu.rate
    val locMsg = when {
        userLocation == null -> tr("Joylashuvni yoqing", "Включите геолокацию")
        !anyDistance -> tr("Joylashuv aniqlanmadi", "Местоположение не определено")
        else -> null
    }

    PullToRefreshBox(
        isRefreshing = loading && !noData,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().background(VsBg),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            // Ticker header: freshness + currency cards (tap or swipe to switch)
            item(key = "top") {
                TickerBar(
                    currency = currency, onCurrency = onCurrency, tickers = tickers,
                    updatedAt = payload.updatedAt, offline = offline,
                    onOpenSettings = onOpenSettings,
                    searchOpen = searchOpen,
                    onToggleSearch = { searchOpen = !searchOpen; if (!searchOpen) query = "" }
                )
            }
            if (searchOpen) {
                item(key = "search") {
                    val fr = remember { FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = VsType.Body.copy(color = VsInk), cursorBrush = SolidColor(VsEmerald),
                        modifier = Modifier.animateItem().fillMaxWidth().padding(top = 4.dp).focusRequester(fr),
                        decorationBox = { inner ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Search, null, tint = VsInkSoft, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f)) {
                                    if (query.isEmpty()) Text(tr("Bank nomi", "Название банка"), style = VsType.Body, color = VsInkSoft)
                                    inner()
                                }
                            }
                        }
                    )
                }
            }
            // Direction
            item(key = "dir") {
                Segmented(
                    options = listOf(tr("Men sotaman", "Я продаю"), tr("Men olaman", "Я покупаю")),
                    selected = if (sell) 0 else 1,
                    onSelect = { onDirection(if (it == 0) ConvertMode.SELL_USD else ConvertMode.BUY_USD) },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            // Amount + quick amounts
            item(key = "amount") {
                Box(Modifier.padding(top = 16.dp)) { AmountField(currency, amountText, onAmount) }
            }
            item(key = "quick") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAmounts(currency).forEach { v ->
                        VsChip(kGroup(v), selected = amount == v.toDouble(), onClick = { onAmount(v.toString()) })
                    }
                }
            }
            // Status lines
            if (offline) {
                item(key = "offline") {
                    KBanner(Icons.Outlined.CloudOff, VsInk,
                        if (ru) "Последние данные ${timeText?.let { "в $it" } ?: ""}, нет интернета".replace("  ", " ")
                        else "Oxirgi maʻlumot ${timeText ?: ""}, internet yoʻq".replace("  ", " "),
                        Modifier.animateItem())
                }
            } else if (stale12) {
                item(key = "stale") {
                    val h = ageH!!.toInt()
                    KBanner(Icons.Outlined.Schedule, VsAmber,
                        if (ru) "Данные обновлены $h ч назад" else "Maʻlumot $h soat oldin yangilangan",
                        Modifier.animateItem())
                }
            }

            when {
                showSkeleton -> {
                    item(key = "sk-hero") { SkeletonHero() }
                    items(6, key = { "sk$it" }) { SkeletonRow() }
                }
                noData -> item(key = "empty") {
                    Text(
                        tr("Bank kurslari hozircha mavjud emas.", "Курсы банков пока недоступны."),
                        style = VsType.Body, color = VsInkSoft, modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
                else -> {
                    // Hero
                    item(key = "hero") {
                        Box(Modifier.animateItem()) {
                            if (best != null) {
                                HeroBlock(
                                    best = best, amount = amount, currency = currency, sell = sell, ru = ru,
                                    timeText = timeText,
                                    onRoute = { best.lat?.let { la -> best.lng?.let { ln -> route(RouteTarget(la, ln, best.rate.bank)) } } },
                                    shareText = kShareText(best, currency, amount, payload.updatedAt, true),
                                )
                            } else {
                                Text(
                                    tr("Mos bank topilmadi", "Подходящих банков нет"),
                                    style = VsType.Body, color = VsInkSoft, modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                    }
                    // Location hint: one quiet line, dismissible, never blocking.
                    if (!locChipDismissed) {
                        val msg = locMsg
                        if (msg != null) item(key = "loc") {
                            Row(
                                Modifier.animateItem().fillMaxWidth().padding(top = 8.dp).heightIn(min = 40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.LocationOff, null, tint = VsInkSoft, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    msg + tr(" — masofa koʻrsatilmaydi", " — расстояние не показывается"),
                                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f)
                                )
                                Box(
                                    Modifier.size(40.dp).clip(RoundedCornerShape(999.dp))
                                        .clickable(role = Role.Button) { locChipDismissed = true }
                                        .semantics { contentDescription = if (ru) "Закрыть" else "Yopish" },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Outlined.Close, null, tint = VsInkSoft, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                    // List header
                    item(key = "header") {
                        Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.weight(1f)) {
                                    Text(tr("Banklar", "Банки"), style = VsType.Title, color = VsInk, modifier = Modifier.alignByBaseline())
                                    Spacer(Modifier.width(8.dp))
                                    Text("${displayed.size}", style = VsType.Meta, color = VsInkSoft, modifier = Modifier.alignByBaseline())
                                }
                                Box {
                                    Row(
                                        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                                            .clickable(role = Role.Button) { sortMenu = true }.padding(start = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Outlined.Sort, null, tint = VsInkSoft, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(sortLabel(sort), style = VsType.Meta.copy(fontWeight = FontWeight.Medium), color = VsInk)
                                    }
                                    DropdownMenu(
                                        expanded = sortMenu, onDismissRequest = { sortMenu = false },
                                        containerColor = VsBg, shape = RoundedCornerShape(12.dp)
                                    ) {
                                        KursSort.entries.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(sortLabel(opt), style = VsType.Body, color = if (opt == sort) VsEmerald else VsInk) },
                                                trailingIcon = { if (opt == sort) Icon(Icons.Outlined.Check, null, tint = VsEmerald) },
                                                onClick = { sort = opt; sortMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VsChip(tr("Ochiq hozir", "Открыто сейчас"), onlyOpen, { onlyOpen = !onlyOpen })
                                if (favorites.isNotEmpty()) VsChip(tr("Tanlanganlar", "Избранное"), onlyFav, { onlyFav = !onlyFav })
                            }
                            Spacer(Modifier.height(4.dp))
                            KHairline()
                        }
                    }
                    if (displayed.isEmpty()) {
                        item(key = "none") {
                            Text(
                                tr("Mos bank topilmadi", "Подходящих банков нет"),
                                style = VsType.Body, color = VsInkSoft, modifier = Modifier.animateItem().padding(vertical = 24.dp)
                            )
                        }
                    }
                    items(displayed.size, key = { displayed[it].rate.bank }) { idx ->
                        val b = displayed[idx]
                        Column(Modifier.animateItem(fadeInSpec = tween(Motion.MEDIUM), placementSpec = Motion.slide(), fadeOutSpec = tween(Motion.SHORT))) {
                            BankListRow(
                                rank = idx + 1, item = b, best = best, hasAmount = hasAmount,
                                favorite = b.rate.bank in favorites, stale = stale6, ageH = ageH, ru = ru,
                                showUnknownHours = onlyOpen,
                                onClick = { sheetBank = b.rate.bank },
                                onLongClick = { onToggleFavorite(b.rate.bank) }
                            )
                            KHairline()
                        }
                    }
                    item(key = "cbu") {
                        Text(
                            (if (ru) "Центральный банк: " else "Markaziy bank: ") + kRateFmt(cbu, ru) + " ${kSom(ru)} · " + (timeText ?: payload.cbu.date),
                            style = VsType.Meta, color = VsInkSoft,
                            modifier = Modifier.animateItem().fillMaxWidth().padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }

    val sheetItem = sheetBank?.let { name -> ranked.firstOrNull { it.rate.bank == name } }
    if (sheetItem != null) {
        BankSheet(
            item = sheetItem, currency = currency, amount = amount, direction = direction,
            updatedAt = payload.updatedAt,
            favorite = sheetItem.rate.bank in favorites,
            onToggleFavorite = { onToggleFavorite(sheetItem.rate.bank) },
            onDismiss = { sheetBank = null }
        )
    }
}

@Composable
private fun KBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface)
            .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = VsType.Meta, color = if (tint == VsAmber) VsAmber else VsInk)
    }
}

@Composable
private fun HeroBlock(
    best: RankedBank, amount: Double, currency: Currency, sell: Boolean, ru: Boolean,
    timeText: String?, onRoute: () -> Unit, shareText: String
) {
    val has = amount > 0
    val big = LocalDensity.current.fontScale > 1.5f
    val heroStyle = VsType.Hero.copy(fontSize = if (big) 28.sp else 32.sp, lineHeight = if (big) 34.sp else 38.sp)
    val label = if (!has) (if (ru) "Курс за 1 ${currency.code}" else "1 ${currency.code} uchun")
    else if (sell) (if (ru) "Вы получите" else "Qoʻlingizga tegadi")
    else (if (ru) "Вам нужно заплатить" else "Toʻlashingiz kerak")
    val value = if (has) best.total else best.perUnit.toLong()
    val som = kSom(ru)
    val spoken = if (has) "$label, ${kSpokenSom(value, ru)}" else "$label, ${kSpokenSom(value, ru)}"

    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        // Data age lives in the ticker status line above.
        Text(label, style = VsType.Meta, color = VsInkSoft)
        Row(modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(Motion.SHORT, easing = Motion.Standard) })
                },
                label = "hero"
                , modifier = Modifier.alignByBaseline()
            ) { v ->
                Text(kGroup(v), style = heroStyle, color = VsEmerald)
            }
            Text("$NBSP$som", style = VsType.Body, color = VsInkSoft, modifier = Modifier.alignByBaseline())
        }
        Spacer(Modifier.height(12.dp))
        Crossfade(best.rate.bank, animationSpec = tween(Motion.SHORT), label = "heroBank") { name ->
            Text(name, style = VsType.Title, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val meta = listOfNotNull(
            if (has) "1 ${currency.code} = ${kGroup(best.perUnit)} $som" else null,
            best.distanceKm?.let { kKm(it, ru) },
            best.open.takeIf { it.known }?.let { kHoursShort(it, ru) }
        ).joinToString("  ·  ")
        if (meta.isNotEmpty()) Text(meta, style = VsType.Meta, color = VsInkSoft)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton(
                text = tr("Yoʻnalish", "Маршрут"), icon = Icons.Outlined.Directions, primary = true,
                enabled = best.lat != null && best.lng != null, modifier = Modifier.weight(1f),
                // Tonal, not filled: the currency cards on top are the first thing the eye should land on.
                tonal = true, onClick = onRoute
            )
            KIconAction(Icons.Outlined.Share, tr("Ulashish", "Поделиться")) { kShare(shareText) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BankListRow(
    rank: Int, item: RankedBank, best: RankedBank?, hasAmount: Boolean,
    favorite: Boolean, stale: Boolean, ageH: Double?, ru: Boolean, showUnknownHours: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isBest = best != null && item.rate.bank == best.rate.bank
    val shown = if (hasAmount) item.total else item.perUnit.toLong()
    val delta = if (!isBest && best != null) {
        if (hasAmount) item.total - best.total else (item.perUnit - best.perUnit).toLong()
    } else null

    // Unknown hours are the norm for most banks; repeating it on every row is noise. Shown only when filtering by "open now".
    val hours = if (item.open.known || showUnknownHours) kHoursShort(item.open, ru) else null
    val meta = listOfNotNull(
        if (hasAmount) "${kGroup(item.perUnit)} ${kSom(ru)}" else null,
        item.distanceKm?.let { kKm(it, ru) },
        hours
    ).joinToString("  ·  ")
    val ageText = ageH?.toInt()?.let { if (ru) "$it ч назад" else "$it s oldin" }

    val spoken = buildString {
        append(if (ru) "$rank-е место, " else "$rank-oʻrin, ")
        append(item.rate.bank).append(", ")
        append(kSpokenSom(shown, ru))
        item.distanceKm?.let { append(", ").append(kSpokenKm(it, ru)) }
        append(", ").append(kSpokenHours(item.open, ru))
        if (delta != null && delta != 0L) append(", ").append(if (ru) "разница " else "farq ").append(kDelta(delta))
        if (favorite) append(", ").append(if (ru) "в избранном" else "tanlangan")
        if (stale && ageText != null) append(", ").append(ageText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = tr("Batafsil", "Подробнее"),
                onLongClickLabel = tr("Tanlanganlarga qoʻshish yoki olib tashlash", "Добавить в избранное или убрать"),
                onClick = onClick,
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() }
            )
            .clearAndSetSemantics { contentDescription = spoken }
            .heightIn(min = 64.dp)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Line 1: rank · name · total — one shared baseline.
        Row(Modifier.fillMaxWidth()) {
            Text(
                "$rank", style = VsType.Body,
                color = if (isBest) VsEmerald else VsInkSoft,
                modifier = Modifier.width(RANK_W).alignByBaseline()
            )
            Row(Modifier.weight(1f).alignByBaseline(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.rate.bank, style = VsType.BodyMedium, color = VsInk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (favorite) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Star, null, tint = VsInkSoft, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                shown, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) },
                label = "rowv", modifier = Modifier.alignByBaseline()
            ) { v ->
                Text(
                    kGroup(v), style = VsType.BodyMedium.copy(fontWeight = if (isBest) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isBest) VsEmerald else VsInk, textAlign = TextAlign.End
                )
            }
        }
        // Line 2: meta · delta — indented to the name column, shared baseline.
        if (meta.isNotEmpty() || stale || delta != null) {
            Row(Modifier.fillMaxWidth().padding(start = RANK_W, top = 2.dp)) {
                Row(Modifier.weight(1f).alignByBaseline(), verticalAlignment = Alignment.CenterVertically) {
                    if (meta.isNotEmpty()) Text(meta, style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (stale && ageText != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Schedule, null, tint = VsAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(ageText, style = VsType.Meta, color = VsAmber)
                    }
                }
                if (delta != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(kDelta(delta), style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End, modifier = Modifier.alignByBaseline())
                }
            }
        }
    }
}

private val RANK_W = 28.dp

/* ---------- skeleton ---------- */

@Composable
private fun skeletonAlpha(): Float {
    val t = rememberInfiniteTransition(label = "sk")
    val a by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900, easing = Motion.Standard), RepeatMode.Reverse), label = "a")
    return a
}

@Composable
private fun SkeletonHero() {
    val a = skeletonAlpha()
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).alpha(a), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(120.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
        Box(Modifier.width(220.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
        Box(Modifier.width(160.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface))
        Spacer(Modifier.height(8.dp))
        KHairline()
    }
}

@Composable
private fun SkeletonRow() {
    val a = skeletonAlpha()
    Column(Modifier.fillMaxWidth().alpha(a)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(140.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
                Box(Modifier.width(200.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
            }
            Box(Modifier.width(80.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
        }
        KHairline()
    }
}
