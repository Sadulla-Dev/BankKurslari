package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
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
fun kursIsRu(): Boolean = LocalLang.current == Lang.RU

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

fun kSom(ru: Boolean) = pick(lang, "soʻm", "сум", "UZS")

fun kDelta(d: Long): String = when {
    d < 0 -> "$MINUS${kGroup(-d)}"
    d > 0 -> "+${kGroup(d)}"
    else -> "0"
}

fun kKm(km: Double, ru: Boolean): String {
    val m = (km * 1000).roundToInt()
    if (m < 1000) return "$m ${pick(lang, "m", "м", "m")}"
    val v = ((km * 10).roundToInt() / 10.0).toString()
    return (if (ru) v.replace('.', ',') else v) + " " + (pick(lang, "km", "км", "km"))
}

fun kRateFmt(v: Double, ru: Boolean): String =
    if (v >= 1000) kGroup(v.roundToLong())
    else v.fixed(2).let { if (ru) it.replace('.', ',') else it }

fun kHoursShort(o: OpenState, ru: Boolean): String = when {
    !o.known -> pick(lang, "Ish vaqti nomaʻlum", "Время работы неизвестно", "Opening hours unknown")
    o.open -> o.untilText?.let { pick(lang, "$it gacha", "до $it", "until $it") } ?: (pick(lang, "Ochiq", "Открыто", "Open"))
    else -> (pick(lang, "Yopiq", "Закрыто", "Closed")) +
        (o.untilText?.let { pick(lang, " · $it da ochiladi", " · откроется в $it", " · opens at $it") } ?: "")
}

fun kHoursLong(o: OpenState, ru: Boolean): String = when {
    !o.known -> pick(lang, "Ish vaqti nomaʻlum", "Время работы неизвестно", "Opening hours unknown")
    o.open -> (pick(lang, "Ochiq", "Открыто", "Open")) +
        (o.untilText?.let { pick(lang, " · $it gacha", " · до $it", " · until $it") } ?: "")
    else -> kHoursShort(o, ru)
}

private fun kSpokenHours(o: OpenState, ru: Boolean): String = when {
    !o.known -> pick(lang, "ish vaqti nomaʻlum", "время работы неизвестно", "opening hours unknown")
    o.open -> o.untilText?.let { pick(lang, "$it gacha ochiq", "открыто до $it", "open until $it") } ?: (pick(lang, "ochiq", "открыто", "open"))
    else -> pick(lang, "yopiq", "закрыто", "closed")
}

private fun kSpokenSom(n: Long, ru: Boolean): String {
    val m = n / 1_000_000
    val t = (n / 1000) % 1000
    val r = n % 1000
    val parts = mutableListOf<String>()
    if (m > 0) parts += pick(lang, "$m million", "$m млн", "$m million")
    if (t > 0) parts += pick(lang, "$t ming", "$t тыс.", "$t thousand")
    if (r > 0 || parts.isEmpty()) parts += "$r"
    return parts.joinToString(" ") + " " + kSom(ru)
}

private fun kSpokenKm(km: Double, ru: Boolean): String {
    val m = (km * 1000).roundToInt()
    return if (m < 1000) "$m ${pick(lang, "metr", "метров", "metres")}"
    else {
        val v = ((km * 10).roundToInt() / 10.0).toString().let { if (ru) it.replace('.', ',') else it }
        "$v ${pick(lang, "kilometr", "километра", "kilometres")}"
    }
}

private val FRACTION_RE = Regex("""\.\d+""")

/** Parses payload.updatedAt (ISO-like) to epoch millis, or null. Offset-less times are local. */
fun kParseMillis(updatedAt: String?): Long? {
    if (updatedAt.isNullOrBlank()) return null
    runCatching { return Instant.parse(updatedAt).toEpochMilliseconds() }
    val clean = updatedAt.replace(FRACTION_RE, "")
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
    val head = if (best) (pick(lang, "Eng yaxshi kurs", "Лучший курс", "Best rate")) else (pick(lang, "Kurs", "Курс", "Rate"))
    return "$head: ${kGroup(amt)} ${currency.code} → ${kGroup(total)} ${kSom(ru)} — ${item.rate.bank}$time"
}

fun kShare(text: String) = shareText(text)

/* ---------- small building blocks ---------- */

@Composable
fun KHairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(VsLine))
}

/** Primary = the screen's one accent fill. Secondary = plain ink text, no outline. Both >= 48 dp. */
@Composable
fun KButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, tonal: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val src = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(src)
            .clip(shape)
            .then(if (primary && !tonal) Modifier.background(VsEmerald) else Modifier)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(interactionSource = src, indication = ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val c = if (primary && !tonal) VsBg else VsInk
        if (icon != null) {
            Icon(icon, null, tint = c, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = VsType.Body, color = c, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 48 dp icon action without a container. */
@Composable
fun KIconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.size(48.dp).pressScale(src).clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = src, indication = ripple(), role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = VsInk, modifier = Modifier.size(22.dp)) }
}

/** Number that rolls up when it grows and down when it shrinks (Stocks-style). */
@Composable
fun RollingNumber(value: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier, format: (Long) -> String = ::kGroup) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState >= initialState
            val t = tween<IntOffset>(Motion.MEDIUM, easing = Motion.Decelerate)
            ContentTransform(
                targetContentEnter = slideInVertically(t) { if (up) it / 2 else -it / 2 } + fadeIn(tween(Motion.MEDIUM)),
                initialContentExit = slideOutVertically(tween(Motion.SHORT)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(Motion.SHORT)),
                sizeTransform = SizeTransform(clip = true)
            )
        },
        modifier = modifier,
        label = "roll"
    ) { v -> Text(format(v), style = style, color = color, maxLines = 1, softWrap = false) }
}

internal fun quickAmounts(c: Currency): List<Long> =
    if (c == Currency.RUB) listOf(10_000, 50_000, 100_000, 500_000) else listOf(100, 500, 1_000, 5_000)

/** Closed banks keep their rank order but sink below the open ones. */
private fun openFirst(list: List<RankedBank>): List<RankedBank> =
    list.sortedBy { if (mapIsClosed(it.open)) 1 else 0 }

private const val TOP_ROWS = 4

/* ---------- screen ---------- */

/**
 * Home answers one question: "where do I get the most so'm for my money today?"
 * One sentence input ("I sell 100 USD"), one hero number, one bank, a short list. Everything else lives in sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenSettings: () -> Unit,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit
) {
    val ru = kursIsRu()
    val sell = direction == ConvertMode.SELL_USD
    val amount = amountText.filter { it.isDigit() }.take(12).toDoubleOrNull() ?: 0.0
    val hasAmount = amount > 0

    var sheetBank by remember { mutableStateOf<String?>(null) }
    var keypad by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val ranked = remember(payload.banks, direction, amount, userLocation, osmBranches) {
        openFirst(rankBanks(payload.banks, direction, amount, userLocation, osmBranches))
    }
    val best = ranked.firstOrNull()
    val rest = ranked.drop(1)
    val shownRest = if (expanded) rest else rest.take(TOP_ROWS)

    val ageH = kParseMillis(payload.updatedAt)?.let { (nowMillis() - it) / 3_600_000.0 }
    val stale = ageH != null && ageH > 12
    val noData = payload.banks.isEmpty()

    PullToRefreshBox(isRefreshing = loading && !noData, onRefresh = onRefresh) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(VsBg),
            contentPadding = PaddingValues(
                start = 24.dp, end = 24.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            // Freshness + settings: one quiet line.
            item(key = "status") {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    val time = payload.updatedAt?.formatUpdatedAt()
                    val (text, warn) = when {
                        offline -> pick(lang, "Internet yoʻq · ${time ?: ""} dagi kurslar", "Нет интернета · курсы на ${time ?: ""}", "Offline · rates from ${time ?: ""}") to true
                        else -> (updatedAgo(payload.updatedAt) ?: "") to stale
                    }
                    Text(text, style = VsType.Caption, color = if (warn) VsAmber else VsInkSoft, modifier = Modifier.weight(1f))
                    Box(Modifier.offset(x = 12.dp)) {
                        KIconAction(Icons.Outlined.Settings, tr("Sozlamalar", "Настройки", "Settings"), onOpenSettings)
                    }
                }
            }

            // The question, as one tappable sentence.
            item(key = "sentence") {
                AmountSentence(
                    sell = sell, amount = amount.toLong(), currency = currency,
                    onFlip = { onDirection(if (sell) ConvertMode.BUY_USD else ConvertMode.SELL_USD) },
                    onAmount = { keypad = true },
                    onCurrency = onCurrency,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            when {
                loading && noData -> item(key = "sk") { SkeletonHero() }
                noData || best == null -> item(key = "empty") {
                    Text(
                        tr("Bank kurslari hozircha mavjud emas.", "Курсы банков пока недоступны.", "Bank rates are not available yet."),
                        style = VsType.Body, color = VsInkSoft, modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
                else -> {
                    item(key = "hero") {
                        Hero(best, hasAmount, currency, ru, onClick = { sheetBank = best.rate.bank })
                    }
                    item(key = "gap") { Spacer(Modifier.height(48.dp)) }
                    items(shownRest.size, key = { shownRest[it].rate.bank }) { i ->
                        val b = shownRest[i]
                        Column(Modifier.animateItem(fadeInSpec = tween(Motion.MEDIUM), placementSpec = Motion.slide(), fadeOutSpec = tween(Motion.SHORT))) {
                            if (i > 0) KHairline()
                            BankRow(b, best, hasAmount, ru, onClick = { sheetBank = b.rate.bank })
                        }
                    }
                    if (rest.size > TOP_ROWS) item(key = "more") {
                        Column(Modifier.animateItem()) {
                            KHairline()
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { expanded = !expanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (expanded) tr("Kamroq", "Свернуть", "Show less")
                                    else pick(lang, "Barcha ${ranked.size} bank ›", "Все ${ranked.size} банков ›", "All ${ranked.size} banks ›"),
                                    style = VsType.Body, color = VsInkSoft
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (keypad) {
        AmountKeypadSheet(
            currency = currency, initial = amount.toLong(),
            onDone = { onAmount(it.toString()); keypad = false },
            onDismiss = { keypad = false }
        )
    }

    val sheetItem = sheetBank?.let { name -> ranked.firstOrNull { it.rate.bank == name } }
    if (sheetItem != null) {
        BankSheet(
            item = sheetItem, currency = currency, amount = amount, direction = direction,
            updatedAt = payload.updatedAt,
            favorite = sheetItem.rate.bank in favorites,
            onToggleFavorite = { onToggleFavorite(sheetItem.rate.bank) },
            branches = osmBranches, userLocation = userLocation, onRequestLocation = onRequestLocation,
            onDismiss = { sheetBank = null }
        )
    }
}

/**
 * "I sell 100 USD" — every word that can change is a tap target with a hairline underline:
 * the verb flips sell/buy, the amount opens the keypad, the currency opens USD / EUR / RUB.
 * Word order follows the language (Uzbek puts the verb last).
 */
@Composable
private fun AmountSentence(
    sell: Boolean, amount: Long, currency: Currency,
    onFlip: () -> Unit, onAmount: () -> Unit, onCurrency: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val verb = if (sell) tr("sotaman", "Продаю", "I sell") else tr("olaman", "Покупаю", "I buy")
    val uzOrder = LocalLang.current == Lang.UZ || LocalLang.current == Lang.UZ_CYRL
    val style = VsType.Title
    @Composable fun Word(text: String, label: String, onClick: () -> Unit) {
        val lineColor = VsInkSoft
        Column(
            Modifier.width(IntrinsicSize.Max).heightIn(min = 48.dp)
                .drawBehind { drawLine(lineColor, Offset(0f, size.height - 10.dp.toPx()), Offset(size.width, size.height - 10.dp.toPx()), 1.dp.toPx()) }
                .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                role = Role.Button, onClickLabel = label, onClick = onClick
            ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text, style = style, color = VsInk, maxLines = 1, softWrap = false)
        }
    }
    val verbW: @Composable () -> Unit = { Word(verb, tr("Yoʻnalishni almashtirish", "Сменить направление", "Switch direction"), onFlip) }
    val amountW: @Composable () -> Unit = {
        Word(if (amount > 0) kGroup(amount) else "0", tr("Summani oʻzgartirish", "Изменить сумму", "Change amount"), onAmount)
    }
    val currencyW: @Composable () -> Unit = {
        Box {
            Word(currency.code, tr("Valyutani tanlash", "Выбрать валюту", "Choose currency")) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = VsBg, shape = RoundedCornerShape(12.dp)) {
                Currency.entries.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.code}  ${c.displayName(LocalLang.current)}", style = if (c == currency) VsType.Title else VsType.Body, color = VsInk) },
                        onClick = { onCurrency(c); menu = false }
                    )
                }
            }
        }
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (uzOrder) { amountW(); currencyW(); verbW() } else { verbW(); amountW(); currencyW() }
    }
}

@Composable
private fun Hero(best: RankedBank, hasAmount: Boolean, currency: Currency, ru: Boolean, onClick: () -> Unit) {
    val value = if (hasAmount) best.total else best.perUnit.toLong()
    val som = kSom(ru)
    val big = LocalDensity.current.fontScale > 1.5f
    val style = if (big) VsType.Display.copy(fontSize = 40.sp, lineHeight = 46.sp) else VsType.Display
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${kSpokenSom(value, ru)}, ${best.rate.bank}" }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(value, style, VsEmerald, Modifier.alignByBaseline())
            Text("$NBSP$som", style = VsType.Body, color = VsInkSoft, modifier = Modifier.alignByBaseline())
        }
        Spacer(Modifier.height(16.dp))
        Crossfade(best.rate.bank, animationSpec = tween(Motion.SHORT), label = "heroBank") { name ->
            Text(name, style = VsType.Body, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val meta = listOfNotNull(
            "${kGroup(best.perUnit)} ${kSom(ru)} / ${currency.code}".takeIf { hasAmount },
            best.distanceKm?.let { kKm(it, ru) },
            best.open.takeIf { it.known }?.let { kHoursShort(it, ru) }
        ).joinToString(" · ")
        if (meta.isNotEmpty()) Text(meta, style = VsType.Caption, color = VsInkSoft)
    }
}

/** Bank · total · difference from the best. Closed banks at 40 %. */
@Composable
private fun BankRow(item: RankedBank, best: RankedBank, hasAmount: Boolean, ru: Boolean, onClick: () -> Unit) {
    val shown = if (hasAmount) item.total else item.perUnit.toLong()
    val delta = if (hasAmount) item.total - best.total else (item.perUnit - best.perUnit).toLong()
    val closed = mapIsClosed(item.open)
    val spoken = buildString {
        append(item.rate.bank).append(", ").append(kSpokenSom(shown, ru))
        if (delta != 0L) append(", ").append(pick(lang, "farq ", "разница ", "difference ")).append(kDelta(delta))
        item.distanceKm?.let { append(", ").append(kSpokenKm(it, ru)) }
        append(", ").append(kSpokenHours(item.open, ru))
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(onClickLabel = tr("Batafsil", "Подробнее", "Details"), onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken }
            .alpha(if (closed) 0.4f else 1f)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.rate.bank, style = VsType.Body, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Text(kGroup(shown), style = VsType.Body, color = VsInk)
        Text(
            if (delta == 0L) "" else kDelta(delta), style = VsType.Caption, color = VsInkSoft,
            textAlign = TextAlign.End, modifier = Modifier.width(72.dp)
        )
    }
}

/* ---------- amount keypad ---------- */

/** Amount entry without the system keyboard: big number, quick amounts, a 3×4 keypad and one Done action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountKeypadSheet(currency: Currency, initial: Long, onDone: (Long) -> Unit, onDismiss: () -> Unit) {
    var digits by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    val haptic = LocalHapticFeedback.current
    val value = digits.toLongOrNull() ?: 0L
    fun press(k: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        digits = when (k) {
            "⌫" -> digits.dropLast(1)
            else -> (digits + k).trimStart('0').take(9)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                RollingNumber(value, VsType.Display, VsInk, Modifier.alignByBaseline())
                Text("$NBSP${currency.code}", style = VsType.Body, color = VsInkSoft, modifier = Modifier.alignByBaseline())
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                quickAmounts(currency).forEach { v ->
                    Box(
                        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { digits = v.toString() },
                        contentAlignment = Alignment.Center
                    ) { Text(kGroup(v), style = if (value == v) VsType.Title else VsType.Body, color = if (value == v) VsInk else VsInkSoft) }
                }
            }
            Spacer(Modifier.height(16.dp))
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫")).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { k ->
                        Box(
                            Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button, onClickLabel = if (k == "⌫") tr("Oʻchirish", "Стереть", "Delete") else null) { press(k) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "⌫") Icon(Icons.AutoMirrored.Outlined.Backspace, tr("Oʻchirish", "Стереть", "Delete"), tint = VsInk)
                            else Text(k, style = VsType.Display.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal), color = VsInk)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            KButton(tr("Tayyor", "Готово", "Done"), null, primary = true, enabled = value > 0, modifier = Modifier.fillMaxWidth()) { onDone(value) }
        }
    }
}

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
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).alpha(a), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(240.dp).height(48.dp).clip(RoundedCornerShape(6.dp)).background(VsSurface))
        Box(Modifier.width(160.dp).height(17.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface))
        Spacer(Modifier.height(32.dp))
        repeat(4) { Box(Modifier.fillMaxWidth().height(17.dp).clip(RoundedCornerShape(4.dp)).background(VsSurface)) }
    }
}
