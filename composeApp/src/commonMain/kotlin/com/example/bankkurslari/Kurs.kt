package com.example.bankkurslari

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.drawWithContent
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
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsRoseMark
import com.example.bankkurslari.ui.theme.VsRoseText
import com.example.bankkurslari.ui.theme.VsTealTint
import com.example.bankkurslari.ui.theme.VsTile
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
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

fun kSom(ru: Boolean) = pick(lang, "soʻm", "сум", "so'm")

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
    !o.known -> pick(lang, "Ish vaqti nomaʼlum", "Часы работы неизвестны", "Opening hours unknown")
    o.open -> o.untilText?.let { pick(lang, "$it gacha", "до $it", "until $it") } ?: (pick(lang, "Ochiq", "Открыто", "Open"))
    else -> (pick(lang, "Yopiq", "Закрыто", "Closed")) +
        (o.untilText?.let { pick(lang, " · $it da ochiladi", " · откроется в $it", " · opens at $it") } ?: "")
}

fun kHoursLong(o: OpenState, ru: Boolean): String = when {
    !o.known -> pick(lang, "Ish vaqti nomaʼlum", "Часы работы неизвестны", "Opening hours unknown")
    o.open -> (pick(lang, "Ochiq", "Открыто", "Open")) +
        (o.untilText?.let { pick(lang, " · $it gacha", " · до $it", " · until $it") } ?: "")
    else -> kHoursShort(o, ru)
}

private fun kSpokenHours(o: OpenState, ru: Boolean): String = when {
    !o.known -> pick(lang, "ish vaqti nomaʼlum", "часы работы неизвестны", "opening hours unknown")
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
    Box(modifier.fillMaxWidth().height(1.dp).background(VsHair))
}

/** Primary (teal) / tonal (teal tint) / outlined secondary button. Redesign: 18 dp radius, >= 54 dp. */
@Composable
fun KButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, tonal: Boolean = false, onClick: () -> Unit) {
    val shape = VsShape.Button
    val src = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .pressScale(src)
            .clip(shape)
            .then(
                when {
                    primary && tonal -> Modifier.background(VsTealTint)
                    primary -> Modifier.background(VsEmerald)
                    else -> Modifier.border(1.dp, VsHair, shape)
                }
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val c = when { primary && tonal -> VsEmerald; primary -> VsOnAccent; else -> VsInk }
        Icon(icon, null, tint = c, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = c, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Square 54 dp ground icon action that sits next to a primary [KButton]. */
@Composable
fun KIconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    VsSecondaryButton(icon, label, onClick)
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

/** Amount: VsType.Amount digits with a hairline that turns teal on focus. The unit is the currency name, spoken as a word. */
@Composable
private fun AmountField(currency: Currency, amountText: String, onAmount: (String) -> Unit) {
    val digits = amountText.filter { it.isDigit() }.trimStart('0').take(12)
    val label = tr("Summa, ${currency.code}", "Сумма, ${currency.code}", "Amount, ${currency.code}")
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val lineColor by animateColorAsState(if (focused) VsEmerald else VsHair, Motion.color(), label = "amtline")
    val style = VsType.Amount
    // The unit sits right after the number (one phrase); the hairline runs under the number only.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = digits,
            onValueChange = { onAmount(it.filter { c -> c.isDigit() }.trimStart('0').take(12)) },
            singleLine = true,
            textStyle = style.copy(color = VsInk),
            cursorBrush = SolidColor(VsEmerald),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = GroupingTransformation(),
            interactionSource = src,
            modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 96.dp, max = 240.dp).semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(Modifier.heightIn(min = 48.dp).drawBehindLine(lineColor), contentAlignment = Alignment.CenterStart) {
                    if (digits.isEmpty()) Text("0", style = style, color = VsInkSoft.copy(alpha = 0.5f))
                    inner()
                }
            }
        )
        Spacer(Modifier.width(10.dp))
        Text(currency.displayName(LocalLang.current).lowercase(), style = VsType.Body.copy(fontSize = 15.sp), color = VsInkSoft, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 11.dp))
    }
}

private fun Modifier.drawBehindLine(color: Color): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val h = 2.dp.toPx()
        drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h))
    }
)

private fun quickAmounts(c: Currency): List<Long> =
    if (c == Currency.RUB) listOf(10_000, 50_000, 100_000, 500_000) else listOf(100, 500, 1_000, 5_000)

/* ---------- sort ---------- */

private enum class KursSort { SOM, DIST, RATE, OPEN }

@Composable
private fun sortLabel(s: KursSort): String = when (s) {
    KursSort.SOM -> tr("Summa", "Сумма", "Amount")
    KursSort.DIST -> tr("Masofa", "Расстояние", "Distance")
    KursSort.RATE -> tr("Kurs", "Курс", "Rate")
    KursSort.OPEN -> tr("Ochiq hozir", "Открыто сейчас", "Open now")
}

/** "OF" for "Orient Finans Bank", "BR" for "BRB", "MB" for "Milliy bank". */
private fun bankInitials(name: String): String {
    val words = name.trim().split(' ', '-', '.').filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        words.size == 1 -> words[0].take(2)
        else -> "?"
    }.uppercase()
}

private const val PREVIEW_BANKS = 4

/** The entrance plays once per launch; coming back to the tab shows the screen at rest. */
private var entrancePlayed = false

/** Staggered rise for the screen's blocks: fade + 16 dp lift, index = order of appearance. */
private fun Modifier.rise(e: Animatable<Float, *>, index: Int): Modifier = graphicsLayer {
    val t = (e.value * 1.6f - index * 0.12f).coerceIn(0f, 1f)
    alpha = t
    translationY = (1f - easeOut(t)) * 16.dp.toPx()
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
    onRefresh: () -> Unit,
    onRequestLocation: () -> Unit = {},
    onShowOnMap: () -> Unit = {}
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
    var ccyMenu by remember { mutableStateOf(false) }
    var locChipDismissed by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var sheetBank by remember { mutableStateOf<String?>(null) }
    var showAll by remember { mutableStateOf(false) }

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
    // The best bank lives in the exchange slip; the list below compares everyone else against it.
    val others = remember(displayed, best) { displayed.filter { best == null || it.rate.bank != best.rate.bank } }

    val ageH = kAgeHours(payload.updatedAt)
    val stale6 = ageH != null && ageH > 6
    val stale12 = ageH != null && ageH > 12
    val timeText = payload.updatedAt?.formatUpdatedAt()
    val noData = payload.banks.isEmpty()
    val showSkeleton = loading && noData
    val anyDistance = ranked.any { it.distanceKm != null }
    val cbu = payload.cbu.rate
    val locMsg = when {
        userLocation == null -> tr("Masofa uchun joylashuvni yoqing", "Включите геолокацию для расстояний", "Turn on location for distances")
        !anyDistance -> tr("Joylashuv aniqlanmadi", "Местоположение не найдено", "Location not found")
        else -> null
    }

    val pullState = rememberPullToRefreshState()
    val refreshing = loading && !noData
    val pullHaptic = LocalHapticFeedback.current

    // One choreography: pull (screen yields) -> measure (numbers dim, bars re-measure) -> settle (numbers land, news).
    val settle = remember { Animatable(if (entrancePlayed) 1f else 0f) }
    val entrance = remember { Animatable(if (entrancePlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!entrancePlayed) {
            entrancePlayed = true
            launch { settle.animateTo(1f, tween(1100, easing = Motion.Decelerate)) }
            entrance.animateTo(1f, tween(900, easing = LinearEasing))
        }
    }
    // Only a pull by the user plays the refresh choreography; background and currency refreshes stay quiet.
    var user by remember { mutableStateOf(false) }
    var hold by remember { mutableStateOf(false) }
    var kick by remember { mutableIntStateOf(0) }
    var before by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var moved by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var news by remember { mutableStateOf<String?>(null) }
    var wasShown by remember { mutableStateOf(false) }
    val measuring = user && (refreshing || hold)
    // Even an instant reply is held ~1 s, so the user sees the comparison happen.
    LaunchedEffect(kick) { if (kick > 0) { hold = true; delay(1100); hold = false } }
    val newsNone = tr("Oʻzgarish yoʻq", "Без изменений", "No changes")
    LaunchedEffect(measuring) {
        val was = wasShown
        wasShown = measuring
        if (was && !measuring) {
            user = false
            val diffs = HashMap<String, Int>()
            for (r in ranked) {
                val old = before[r.rate.bank] ?: continue
                val d = r.perUnit - old
                if (abs(d) >= 0.5) diffs[r.rate.bank] = if (d > 0) 1 else -1
            }
            moved = diffs
            launch { settle.snapTo(0f); settle.animateTo(1f, tween(1000, easing = Motion.Decelerate)) }
            if (!offline) {
                news = if (diffs.isEmpty()) newsNone
                else pick(lang, "${diffs.size} ta kurs oʻzgardi", "Изменилось курсов: ${diffs.size}", "${diffs.size} rates changed")
                if (diffs.isNotEmpty()) pullHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            delay(2800)
            news = null
        }
    }
    LaunchedEffect(pullState) {
        snapshotFlow { pullState.distanceFraction >= 1f }.distinctUntilChanged().collect {
            if (it) pullHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val fx = remember(measuring) { RefreshFx(pull = { pullState.distanceFraction }, measuring = measuring, settle = settle.asState()) }

    PullToRefreshBox(
        isRefreshing = measuring,
        onRefresh = { user = true; kick++; before = ranked.associate { it.rate.bank to it.perUnit.toDouble() }; onRefresh() },
        state = pullState,
        indicator = { BestBarsIndicator(pullState, measuring, innerPadding.calculateTopPadding()) }
    ) {
        LazyColumn(
            modifier = Modifier
                .graphicsLayer { translationY = pullState.distanceFraction.coerceIn(0f, 1.25f) * RefreshShift.toPx() }
                .fillMaxWidth().background(VsBg),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = innerPadding.calculateTopPadding() + 6.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Topbar: live dot + "Rates at 13:34" + search + settings.
            item(key = "top") {
                Box(Modifier.rise(entrance, 0)) { TodayTopBar(
                    updatedAt = payload.updatedAt, offline = offline, news = news,
                    searchOpen = searchOpen,
                    onToggleSearch = { searchOpen = !searchOpen; if (!searchOpen) query = "" },
                    onOpenSettings = onOpenSettings
                ) }
            }
            if (searchOpen) {
                item(key = "search") {
                    val fr = remember { FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = VsType.Body.copy(color = VsInk), cursorBrush = SolidColor(VsEmerald),
                        modifier = Modifier.animateItem().fillMaxWidth().focusRequester(fr),
                        decorationBox = { inner ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).vsCard(VsShape.Pill).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Search, null, tint = VsInkSoft, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f)) {
                                    if (query.isEmpty()) Text(tr("Bank nomi", "Название банка", "Bank name"), style = VsType.Body, color = VsInkSoft)
                                    inner()
                                }
                            }
                        }
                    )
                }
            }
            // Status lines
            if (offline) {
                item(key = "offline") {
                    KBanner(Icons.Outlined.CloudOff, VsInk,
                        pick(lang, "Internet yoʻq. Oxirgi kurslar: ${timeText ?: "—"}",
                            "Нет сети. Последние курсы: ${timeText ?: "—"}",
                            "No connection. Last rates: ${timeText ?: "—"}"),
                        Modifier.animateItem())
                }
            } else if (stale12) {
                item(key = "stale") {
                    val h = ageH.toInt()
                    KBanner(Icons.Outlined.Schedule, VsAmber,
                        pick(lang, "Kurslar $h soat eskirgan", "Курсы устарели на $h ч", "Rates are $h h old"),
                        Modifier.animateItem())
                }
            }

            when {
                showSkeleton -> {
                    item(key = "sk-hero") { SkeletonSlip() }
                    item(key = "sk-list") { SkeletonList() }
                }
                noData -> item(key = "empty") {
                    Text(
                        tr("Kurslar yuklanmadi. Yangilash uchun pastga torting.", "Курсы не загрузились. Потяните вниз, чтобы обновить.", "Rates didn't load. Pull down to retry."),
                        style = VsType.Body, color = VsInkSoft, modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
                else -> {
                    // Exchange slip: direction, currency, amount, result, best bank.
                    item(key = "slip") {
                        Box(Modifier.animateItem().rise(entrance, 1)) {
                            VsCardBox {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    SegmentedHug(
                                        options = listOf(tr("Sotish", "Продать", "Sell"), tr("Olish", "Купить", "Buy")),
                                        selected = if (sell) 0 else 1,
                                        onSelect = { onDirection(if (it == 0) ConvertMode.SELL_USD else ConvertMode.BUY_USD) }
                                    )
                                    Box {
                                        VsCurrencyChip(
                                            symbol = currency.symbol, code = currency.code,
                                            description = tr("Valyuta: ${currency.code}, oʻzgartirish", "Валюта: ${currency.code}, изменить", "Currency: ${currency.code}, change"),
                                            onClick = { ccyMenu = true }
                                        )
                                        DropdownMenu(
                                            expanded = ccyMenu, onDismissRequest = { ccyMenu = false },
                                            containerColor = VsCard, shape = VsShape.Panel
                                        ) {
                                            Currency.entries.forEach { c ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text("${c.symbol}  ${c.code}  ·  ${c.displayName(lang)}", style = VsType.Body, color = if (c == currency) VsEmerald else VsInk)
                                                    },
                                                    trailingIcon = { if (c == currency) Icon(Icons.Outlined.Check, null, tint = VsEmerald) },
                                                    onClick = { ccyMenu = false; onCurrency(c) }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text(tr("Boshqa valyutalar…", "Другие валюты…", "Other currencies…"), style = VsType.Body, color = VsInkSoft) },
                                                onClick = { ccyMenu = false; showAll = true }
                                            )
                                        }
                                    }
                                }
                                AmountField(currency, amountText, onAmount)
                                val quick = quickAmounts(currency)
                                Segmented(
                                    options = quick.map { kGroup(it) },
                                    selected = quick.indexOfFirst { it.toDouble() == amount },
                                    onSelect = { onAmount(quick[it].toString()) },
                                    height = 40.dp
                                )
                                SlipDivider()
                                if (best != null) {
                                    SlipResult(
                                        best = best, amount = amount, currency = currency, sell = sell, ru = ru,
                                        bankCount = displayed.size,
                                        locationOff = userLocation == null, fx = fx,
                                        onBank = { sheetBank = best.rate.bank },
                                        onRoute = { best.lat?.let { la -> best.lng?.let { ln -> route(RouteTarget(la, ln, best.rate.bank)) } } }
                                    )
                                } else {
                                    Text(
                                        tr("Mos bank topilmadi", "Подходящих банков нет", "No matching banks"),
                                        style = VsType.Body, color = VsInkSoft, modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    // Location prompt: one quiet card, never blocking. Disappears once a position is known.
                    if (!locChipDismissed && locMsg != null) {
                        item(key = "loc") {
                            LocationPrompt(
                                text = locMsg,
                                action = if (userLocation == null) tr("Yoqish", "Включить", "Turn on") else null,
                                onAction = { locChipDismissed = true; onRequestLocation() },
                                onDismiss = { locChipDismissed = true },
                                modifier = Modifier.animateItem().rise(entrance, 2)
                            )
                        }
                    }
                    // Section head + sort / filters
                    item(key = "header") {
                        Column(Modifier.fillMaxWidth().rise(entrance, 3).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                                Text(tr("Boshqa banklar", "Другие банки", "Other banks"), style = VsType.Title, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    tr("eng yaxshidan", "от лучшего", "vs best"),
                                    style = VsType.Meta, color = VsInkSoft, maxLines = 1
                                )
                            }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box {
                                    VsChip(sortLabel(sort), selected = false, onClick = { sortMenu = true }, trailing = Icons.Outlined.KeyboardArrowDown)
                                    DropdownMenu(
                                        expanded = sortMenu, onDismissRequest = { sortMenu = false },
                                        containerColor = VsCard, shape = VsShape.Panel
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
                                VsChip(tr("Ochiq hozir", "Открыто сейчас", "Open now"), onlyOpen, { onlyOpen = !onlyOpen })
                                if (favorites.isNotEmpty()) VsChip(tr("Sevimlilar", "Избранное", "Favourites"), onlyFav, { onlyFav = !onlyFav })
                            }
                        }
                    }
                    if (others.isEmpty()) {
                        item(key = "none") {
                            Text(
                                tr("Mos bank topilmadi", "Подходящих банков нет", "No matching banks"),
                                style = VsType.Body, color = VsInkSoft, modifier = Modifier.animateItem().padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        item(key = "banks") {
                            val full = expanded || query.isNotBlank() || others.size <= PREVIEW_BANKS + 1
                            val shown = if (full) others else others.take(PREVIEW_BANKS)
                            val maxLoss = shown.maxOfOrNull { b -> best?.let { abs(lossOf(b, it, hasAmount)) } ?: 0L } ?: 0L
                            val showFooter = !full || (others.size > PREVIEW_BANKS + 1 && query.isBlank())
                            VsPanel(Modifier.animateItem().rise(entrance, 4)) {
                                shown.forEachIndexed { i, b ->
                                    BankListRow(
                                        rank = displayed.indexOfFirst { it.rate.bank == b.rate.bank } + 1, item = b, best = best,
                                        hasAmount = hasAmount, sell = sell, maxLoss = maxLoss,
                                        favorite = b.rate.bank in favorites, stale = stale6, ageH = ageH, ru = ru,
                                        index = i, fx = fx, moved = moved[b.rate.bank] ?: 0,
                                        onClick = { sheetBank = b.rate.bank },
                                        onLongClick = { onToggleFavorite(b.rate.bank) }
                                    )
                                    if (i < shown.lastIndex || showFooter) VsDivider()
                                }
                                if (showFooter) {
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            .clickable(role = Role.Button) { expanded = !full }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            if (!full) pick(lang, "Barcha ${displayed.size} bank", "Все ${displayed.size} банков", "All ${displayed.size} banks")
                                            else pick(lang, "Kamroq koʻrsatish", "Свернуть", "Show fewer"),
                                            style = VsType.BodyMedium, color = VsEmerald
                                        )
                                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = VsEmerald, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    item(key = "cbu") {
                        Text(
                            (pick(lang, "Markaziy bank: ", "ЦБ: ", "Central Bank: ")) + kRateFmt(cbu, ru) + " ${kSom(ru)} · " + (timeText ?: payload.cbu.date),
                            style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateItem().rise(entrance, 5).fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
        // Scrolled content fades out under the status bar instead of colliding with the clock and icons.
        val top = innerPadding.calculateTopPadding()
        if (top > 0.dp) Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(top + 6.dp)
                .background(Brush.verticalGradient(0f to VsBg, top.value / (top.value + 6f) to VsBg, 1f to VsBg.copy(alpha = 0f)))
        )
    }

    if (showAll) AllCurrenciesSheet(onDismiss = { showAll = false })

    val sheetItem = sheetBank?.let { name -> ranked.firstOrNull { it.rate.bank == name } }
    if (sheetItem != null) {
        BankSheet(
            item = sheetItem, currency = currency, amount = amount, direction = direction,
            updatedAt = payload.updatedAt,
            favorite = sheetItem.rate.bank in favorites,
            onToggleFavorite = { onToggleFavorite(sheetItem.rate.bank) },
            best = best,
            onShowOnMap = { sheetBank = null; onShowOnMap() },
            onDismiss = { sheetBank = null }
        )
    }
}

/** Signed so'm difference vs the best bank (per-unit when no amount is typed). */
private fun lossOf(item: RankedBank, best: RankedBank, hasAmount: Boolean): Long =
    if (hasAmount) item.total - best.total else (item.perUnit - best.perUnit).toLong()

@Composable
private fun TodayTopBar(
    updatedAt: String?, offline: Boolean, news: String?, searchOpen: Boolean,
    onToggleSearch: () -> Unit, onOpenSettings: () -> Unit
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        val ageMin = updatedAt?.let { kParseMillis(it) }?.let { (nowMillis() - it) / 60_000 }
        val fresh = !offline && (news != null || (ageMin != null && ageMin < 30))
        FreshDot(fresh = fresh, stale = offline || (ageMin != null && ageMin > 6 * 60))
        Spacer(Modifier.width(10.dp))
        val time = updatedAt?.formatUpdatedAt()
        val status = news ?: when {
            offline -> pick(lang, "Oflayn · ${time ?: "—"}", "Офлайн · ${time ?: "—"}", "Offline · ${time ?: "—"}")
            time != null -> pick(lang, "$time yangilandi", "Обновлено в $time", "Updated at $time")
            else -> ""
        }
        Crossfade(status, animationSpec = tween(Motion.MEDIUM), label = "status", modifier = Modifier.weight(1f)) { t ->
            Text(
                t, style = VsType.BodyMedium.copy(fontSize = 14.sp),
                color = if (news != null) VsEmerald else VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        VsIconButton(
            icon = if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
            description = tr("Qidirish", "Поиск", "Search"), onClick = onToggleSearch
        )
        Spacer(Modifier.width(8.dp))
        VsIconButton(Icons.Outlined.Tune, tr("Sozlamalar", "Настройки", "Settings"), onOpenSettings)
    }
}

@Composable
private fun SlipDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(VsHair))
        Box(Modifier.size(28.dp).clip(CircleShape).background(VsTealTint), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = VsEmerald, modifier = Modifier.size(14.dp))
        }
        Box(Modifier.weight(1f).height(1.dp).background(VsHair))
    }
}

@Composable
private fun KBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().vsCard(VsShape.Panel)
            .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = VsType.Meta, color = if (tint == VsAmber) VsAmber else VsInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Result of the slip: what you get, per-unit line, the best bank, and the two actions. */
@Composable
private fun SlipResult(
    best: RankedBank, amount: Double, currency: Currency, sell: Boolean, ru: Boolean, bankCount: Int,
    locationOff: Boolean, fx: RefreshFx, onBank: () -> Unit, onRoute: () -> Unit
) {
    val has = amount > 0
    val bigFont = LocalDensity.current.fontScale > 1.5f
    val label = if (!has) (pick(lang, "1 ${currency.code} uchun", "Курс за 1 ${currency.code}", "Rate for 1 ${currency.code}"))
    else if (sell) (pick(lang, "Qoʻlingizga tegadi", "Вы получите", "You get"))
    else (pick(lang, "Toʻlashingiz kerak", "Вы заплатите", "You pay"))
    val value = if (has) best.total else best.perUnit.toLong()
    val som = kSom(ru)
    val spoken = "$label, ${kSpokenSom(value, ru)}"
    val text = kGroup(value)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Fixed-height line: the number shrinks smoothly to fit, never wraps, so nothing below it moves while typing.
        val heroAlpha by animateFloatAsState(if (fx.measuring) 0.4f else 1f, tween(Motion.MEDIUM), label = "heroA")
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(50.dp).graphicsLayer {
                // New number lands: a small settle from slightly larger.
                val k = 1f - fx.settle.value
                scaleX = 1f + 0.05f * k; scaleY = scaleX
                transformOrigin = TransformOrigin(0f, 1f)
                alpha = heroAlpha
            }
        ) {
            val unitW = 56.dp
            val digits = text.count { it.isDigit() }
            val spaces = text.length - digits
            val em = digits * 0.6f + spaces * 0.28f
            val fit = (maxWidth - unitW).value / em
            val size = fit.coerceIn(26f, if (bigFont) 38f else 46f)
            val heroStyle = VsType.Hero.copy(fontSize = size.sp, lineHeight = 46.sp)
            Row(
                Modifier.fillMaxSize().semantics(mergeDescendants = true) { contentDescription = spoken },
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text, style = heroStyle, color = VsEmerald, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().weight(1f, fill = false))
                Text("$NBSP$som", style = VsType.BodyMedium.copy(fontSize = 18.sp), color = VsInkSoft, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline().padding(start = 4.dp))
            }
        }
        Text(
            pick(
                lang,
                "1 ${currency.code} = ${kGroup(best.perUnit)} $som, $bankCount bankdan eng yaxshisi",
                "1 ${currency.code} = ${kGroup(best.perUnit)} $som, лучший из $bankCount банков",
                "1 ${currency.code} = ${kGroup(best.perUnit)} $som, best of $bankCount banks"
            ),
            style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
    // Best bank row: tile, name + meta, compact Directions pill.
    val meta = listOfNotNull(
        best.distanceKm?.let { kKm(it, ru) },
        best.open.takeIf { it.known }?.let { kHoursShort(it, ru) }
    ).joinToString(" · ")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.weight(1f).clip(VsShape.Panel)
                .clickable(role = Role.Button, onClickLabel = tr("Batafsil", "Подробнее", "Details"), onClick = onBank)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BankTile(bankInitials(best.rate.bank), best = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(best.rate.bank, style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotEmpty()) Text(meta, style = VsType.Meta, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        VsPillButton(
            text = tr("Yoʻnalish", "Маршрут", "Directions"), icon = Icons.Outlined.Directions,
            enabled = best.lat != null && best.lng != null, onClick = onRoute
        )
    }
}

/** Location prompt card: dashed hairline, pin icon, text and one teal action (or a dismiss cross when there is no action). */
@Composable
private fun LocationPrompt(text: String, action: String?, onAction: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().vsDashedBorder(22.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = VsInkSoft, modifier = Modifier.size(20.dp))
        Text(text, style = VsType.Body, color = VsInk, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action, style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = VsEmerald,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onAction).heightIn(min = 40.dp).wrapContentHeight(Alignment.CenterVertically).padding(horizontal = 6.dp)
            )
        } else {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss)
                    .semantics { contentDescription = pick(lang, "Yopish", "Закрыть", "Close") },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Close, null, tint = VsInkSoft, modifier = Modifier.size(16.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BankListRow(
    rank: Int, item: RankedBank, best: RankedBank?, hasAmount: Boolean, sell: Boolean, maxLoss: Long,
    favorite: Boolean, stale: Boolean, ageH: Double?, ru: Boolean,
    index: Int, fx: RefreshFx, moved: Int,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shown = if (hasAmount) item.total else item.perUnit.toLong()
    val loss = if (best != null) abs(lossOf(item, best, hasAmount)) else 0L
    val lessText = if (loss <= 0L) null else if (sell) kDelta(-loss) else kDelta(loss)
    val spokenLess = if (loss <= 0L) null else if (sell)
        pick(lang, "${kGroup(loss)} kam", "на ${kGroup(loss)} меньше", "${kGroup(loss)} less")
    else pick(lang, "${kGroup(loss)} koʻp", "на ${kGroup(loss)} больше", "${kGroup(loss)} more")
    val frac = if (loss <= 0L || maxLoss <= 0L) 0f else (loss.toFloat() / maxLoss).coerceIn(0.08f, 1f)
    val ageText = ageH?.toInt()?.let { pick(lang, "$it soat oldin", "$it ч назад", "$it h ago") }

    val spoken = buildString {
        append(pick(lang, "$rank-oʻrin, ", "$rank-е место, ", "#$rank, "))
        append(item.rate.bank).append(", ")
        append(kSpokenSom(shown, ru))
        item.distanceKm?.let { append(", ").append(kSpokenKm(it, ru)) }
        append(", ").append(kSpokenHours(item.open, ru))
        if (spokenLess != null) append(", ").append(spokenLess)
        if (favorite) append(", ").append(pick(lang, "sevimli", "в избранном", "favourite"))
        if (stale && ageText != null) append(", ").append(ageText)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = tr("Batafsil", "Подробнее", "Details"),
                onLongClickLabel = tr("Sevimlilarga qoʻshish yoki olib tashlash", "Добавить в избранное или убрать", "Add to or remove from favourites"),
                onClick = onClick,
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() }
            )
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BankTile(bankInitials(item.rate.bank), size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.rate.bank, style = VsType.BodyMedium, color = VsInk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (favorite) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Star, null, tint = VsInkSoft, modifier = Modifier.size(14.dp))
                }
                if (stale && ageText != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.Schedule, null, tint = VsAmber, modifier = Modifier.size(14.dp))
                }
            }
            CompareBar(frac, index, fx)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // A rate that moved flashes once: teal if it got better for you, rose if worse, then settles to ink.
            val better = (moved > 0) == sell
            val flash = if (moved == 0) 0f else 1f - fx.settle.value
            val valueColor = if (flash <= 0f) VsInk else androidx.compose.ui.graphics.lerp(VsInk, if (better) VsEmerald else VsRoseText, flash)
            val valueAlpha by animateFloatAsState(if (fx.measuring) 0.4f else 1f, tween(Motion.MEDIUM), label = "valA")
            Text(kGroup(shown), style = VsType.Value, color = valueColor, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.graphicsLayer { alpha = valueAlpha })
            // Always one line tall, so a row never changes height when the gap appears or disappears.
            Text(
                lessText ?: NBSP.toString(),
                style = VsType.Meta.copy(fontFamily = VsType.Value.fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = VsRoseText, textAlign = TextAlign.End, maxLines = 1, softWrap = false
            )
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
private fun SkeletonSlip() {
    val a = skeletonAlpha()
    VsCardBox(Modifier.alpha(a)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(170.dp).height(40.dp).clip(CircleShape).background(VsGround))
            Box(Modifier.width(100.dp).height(44.dp).clip(CircleShape).background(VsTile))
        }
        Box(Modifier.width(140.dp).height(40.dp).clip(RoundedCornerShape(8.dp)).background(VsGround))
        Box(Modifier.width(220.dp).height(46.dp).clip(RoundedCornerShape(8.dp)).background(VsTealTint))
        Box(Modifier.fillMaxWidth().height(56.dp).clip(VsShape.Panel).background(VsGround))
    }
}

@Composable
private fun SkeletonList() {
    val a = skeletonAlpha()
    VsPanel(Modifier.alpha(a)) {
        repeat(5) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(VsTile))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(110.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(VsTile))
                    Box(Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(VsGround))
                }
                Box(Modifier.width(72.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(VsTile))
            }
            VsDivider()
        }
    }
}

/** First-launch placeholder with the same geometry as the Kurs screen, so content replaces it without a jump. */
@Composable
fun KursBootSkeleton(innerPadding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().background(VsBg).padding(
            start = 20.dp, end = 20.dp,
            top = innerPadding.calculateTopPadding() + 6.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(VsTile))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(VsTile))
        }
        SkeletonSlip()
        SkeletonList()
    }
}
