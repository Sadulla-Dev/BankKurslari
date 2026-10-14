package com.example.bankkurslari

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.BankKurslariTheme
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsDanger
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldDeep
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsHeroBot
import com.example.bankkurslari.ui.theme.VsHeroMid
import com.example.bankkurslari.ui.theme.VsHeroTop
import com.example.bankkurslari.ui.theme.VsIndigo
import com.example.bankkurslari.ui.theme.VsIndigoSoft
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Background rate monitoring: widget refresh + change notifications.
        RateWorker.ensureChannel(this)
        RateWorker.schedulePeriodic(this)
        RateWorker.enqueueNow(this)
        maybeRequestNotificationPermission()

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            var dark by remember { mutableStateOf(RateService.darkMode(this) ?: systemDark) }
            var lang by remember {
                mutableStateOf(if (RateService.language(this) == "ru") Lang.RU else Lang.UZ)
            }
            BankKurslariTheme(darkTheme = dark, dynamicColor = false) {
                androidx.compose.runtime.CompositionLocalProvider(LocalStrings provides stringsFor(lang)) {
                    CurrencyTrackerApp(
                        darkMode = dark,
                        onToggleDark = {
                            dark = !dark
                            RateService.setDarkMode(this, dark)
                        },
                        lang = lang,
                        onToggleLang = {
                            lang = if (lang == Lang.UZ) Lang.RU else Lang.UZ
                            RateService.setLanguage(this, lang.code)
                        }
                    )
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, "android.permission.POST_NOTIFICATIONS"
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this, arrayOf("android.permission.POST_NOTIFICATIONS"), 1001
                )
            }
        }
    }
}

private data class Branch(val address: String, val lat: Double, val lng: Double)

private data class BankRate(
    val bank: String,
    val buy: Int?,
    val sell: Int?,
    val branch: Branch?
) {
    val spread: Int? = if (buy != null && sell != null) sell - buy else null
}

private data class CbuRate(val rate: Double, val diff: Double, val date: String)

private data class RatesPayload(val cbu: CbuRate, val banks: List<BankRate>, val updatedAt: String?)

private data class Atm(val bank: String, val name: String, val lat: Double, val lng: Double)

private enum class MapFilter { ALL, BRANCHES, ATMS }

private data class HistoryPoint(val date: String, val rate: Double)

private data class Seasonality(val start: Double, val mid: Double, val end: Double)

private data class Analytics(
    val enoughData: Boolean,
    val current: Double,
    val weekAgo: Double,
    val weekChangePct: Double,
    val trend: String,        // "up" | "down" | "flat"
    val forecastRate: Double,
    val forecastPct: Double,
    val seasonality: Seasonality,
    val bestRisePeriod: String, // "start" | "mid" | "end"
    val sellAdvice: String,     // "now" | "wait"
    val buyAdvice: String,      // "now" | "wait"
    val points: List<HistoryPoint>
)

private enum class HomeTab(val icon: ImageVector) {
    HOME(Icons.Outlined.Home),
    CALC(Icons.Outlined.Calculate),
    TREND(Icons.Outlined.ShowChart),
    BANKS(Icons.Outlined.SwapVert),
    MAP(Icons.Outlined.Map);

    fun label(s: AppStrings) = when (this) {
        HOME -> s.tabHome
        CALC -> s.tabCalc
        TREND -> s.tabTrend
        BANKS -> s.tabBanks
        MAP -> s.tabMap
    }
}

private enum class SortMode { BUY, SELL }

private enum class ConvertMode { SELL_USD, BUY_USD }

private enum class Currency(val code: String, val symbol: String, val nameUz: String, val nameRu: String) {
    USD("USD", "$", "Dollar", "Доллар"),
    EUR("EUR", "€", "Yevro", "Евро"),
    RUB("RUB", "₽", "Rubl", "Рубль");

    fun displayName(lang: Lang) = if (lang == Lang.RU) nameRu else nameUz
}

private val backendBase = BuildConfig.SERVER_BASE
private fun ratesUrl(cur: Currency) = "$backendBase/api/rates/${cur.code.lowercase()}"
private fun analyticsUrl(cur: Currency) = "$backendBase/api/analytics/${cur.code.lowercase()}"
private fun historyUrl(cur: Currency) = "$backendBase/api/history/${cur.code.lowercase()}"
private val backendAtmsUrl = "$backendBase/api/atms"

@Composable
fun CurrencyTrackerApp(darkMode: Boolean, onToggleDark: () -> Unit, lang: Lang, onToggleLang: () -> Unit) {
    val s = LocalStrings.current
    var payload by remember { mutableStateOf<RatesPayload?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("100") }
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.BUY) }
    var convertMode by remember { mutableStateOf(ConvertMode.SELL_USD) }
    val context = LocalContext.current
    var tab by remember { mutableStateOf(HomeTab.HOME) }
    var currency by remember {
        mutableStateOf(
            Currency.entries.firstOrNull { it.code == RateService.selectedCurrency(context) } ?: Currency.USD
        )
    }
    var atms by remember { mutableStateOf<List<Atm>>(emptyList()) }
    var atmsLoading by remember { mutableStateOf(false) }
    var analytics by remember { mutableStateOf<Analytics?>(null) }
    var analyticsLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            runCatching { fetchRates(currency) }.fold(
                onSuccess = { p ->
                    if (p.banks.isEmpty()) error = s.noBankRates
                    else payload = p
                },
                onFailure = { error = it.message ?: s.connectFail }
            )
            loading = false
        }
    }

    // Reload rates whenever the selected currency changes (also runs on first launch).
    LaunchedEffect(currency) { refresh() }

    // Lazily load ATM locations the first time the map tab is opened.
    LaunchedEffect(tab) {
        if (tab == HomeTab.MAP && atms.isEmpty() && !atmsLoading) {
            atmsLoading = true
            atms = runCatching { fetchAtms() }.getOrDefault(emptyList())
            atmsLoading = false
        }
    }

    // Reset cached analytics when the currency changes so the trend tab refetches.
    LaunchedEffect(currency) { analytics = null }

    // Load analytics when the trend tab is active (and after a currency switch).
    LaunchedEffect(tab, currency) {
        if (tab == HomeTab.TREND && analytics == null && !analyticsLoading) {
            analyticsLoading = true
            analytics = runCatching { fetchAnalytics(currency) }.getOrNull()
            analyticsLoading = false
        }
    }

    val p = payload
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VsBg,
        bottomBar = {
            if (p != null) BottomBar(selected = tab, onSelect = { tab = it })
        }
    ) { innerPadding ->
        when {
            p != null -> {
                // Keep existing data on screen during a refresh/currency switch — dim it
                // slightly and show a thin progress bar instead of blanking the whole UI.
                val contentAlpha by animateFloatAsState(
                    targetValue = if (loading) 0.55f else 1f,
                    animationSpec = tween(250), label = "contentAlpha"
                )
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.alpha(contentAlpha)) {
                        SuccessScreen(
                            innerPadding = innerPadding,
                            tab = tab,
                            currency = currency,
                            onCurrency = {
                                currency = it
                                RateService.setSelectedCurrency(context, it.code)
                                RateWorker.enqueueNow(context)
                            },
                            darkMode = darkMode,
                            onToggleDark = onToggleDark,
                            lang = lang,
                            onToggleLang = onToggleLang,
                            payload = p,
                            atms = atms,
                            atmsLoading = atmsLoading,
                            analytics = analytics,
                            analyticsLoading = analyticsLoading,
                            amountText = amountText,
                            onAmount = { amountText = it.filter(Char::isDigit).take(9) },
                            query = query,
                            onQuery = { query = it },
                            sortMode = sortMode,
                            onSort = { sortMode = it },
                            convertMode = convertMode,
                            onMode = { convertMode = it },
                            onRefresh = { refresh() }
                        )
                    }
                    if (loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(innerPadding),
                            color = VsEmerald,
                            trackColor = VsEmeraldSoft
                        )
                    }
                }
            }
            loading -> LoadingScreen(innerPadding)
            error != null -> ErrorScreen(innerPadding, error ?: "", onRetry = { refresh() })
            else -> LoadingScreen(innerPadding)
        }
    }
}

/* ---------- Bottom navigation ---------- */

@Composable
private fun BottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    val barShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, barShape, clip = false, spotColor = VsInk)
            .clip(barShape)
            .background(VsSurface)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeTab.entries.forEach { item ->
            val active = item == selected
            val tint by animateColorAsState(if (active) VsEmerald else VsInkSoft, tween(220), label = "navtint")
            val indicatorW by animateDpAsState(if (active) 20.dp else 0.dp, tween(260), label = "navind")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(item) }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top indicator bar grows in when the tab becomes active.
                Box(
                    Modifier.width(indicatorW).height(3.dp)
                        .clip(RoundedCornerShape(50)).background(VsEmerald)
                )
                Icon(item.icon, null, tint = tint, modifier = Modifier.size(if (active) 25.dp else 22.dp))
                Text(
                    item.label(LocalStrings.current),
                    color = tint,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ---------- Loading / Error ---------- */

@Composable
private fun LoadingScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = VsEmerald, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text(LocalStrings.current.loading, color = VsInkSoft, fontSize = 14.sp)
    }
}

@Composable
private fun ErrorScreen(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(VsIndigoSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CloudOff, null, tint = VsIndigo, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(s.errorTitle, color = VsInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = VsInkSoft,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            s.errorHint,
            color = VsInkSoft,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(VsEmerald)
                .clickable(onClick = onRetry)
                .padding(horizontal = 26.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(s.retry, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ---------- Success ---------- */

@Composable
private fun SuccessScreen(
    innerPadding: PaddingValues,
    tab: HomeTab,
    currency: Currency,
    onCurrency: (Currency) -> Unit,
    darkMode: Boolean,
    onToggleDark: () -> Unit,
    lang: Lang,
    onToggleLang: () -> Unit,
    payload: RatesPayload,
    atms: List<Atm>,
    atmsLoading: Boolean,
    analytics: Analytics?,
    analyticsLoading: Boolean,
    amountText: String,
    onAmount: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    sortMode: SortMode,
    onSort: (SortMode) -> Unit,
    convertMode: ConvertMode,
    onMode: (ConvertMode) -> Unit,
    onRefresh: () -> Unit
) {
    val rates = payload.banks
    val buyRates = rates.filter { it.buy != null }
    val sellRates = rates.filter { it.sell != null }
    val bestBuy = buyRates.maxByOrNull { it.buy!! }
    val bestSell = sellRates.minByOrNull { it.sell!! }

    val visibleRates = remember(rates, query, sortMode) {
        rates
            .filter { query.isBlank() || it.bank.contains(query.trim(), ignoreCase = true) }
            .sortedWith(
                when (sortMode) {
                    SortMode.BUY -> compareByDescending { it.buy ?: Int.MIN_VALUE }
                    SortMode.SELL -> compareBy { it.sell ?: Int.MAX_VALUE }
                }
            )
    }

    val s = LocalStrings.current
    if (tab == HomeTab.MAP) {
        MapScreen(
            innerPadding = innerPadding,
            darkMode = darkMode,
            lang = lang,
            branches = rates,
            atms = atms,
            atmsLoading = atmsLoading
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TopBar(currency = currency, onCurrency = onCurrency, darkMode = darkMode, onToggleDark = onToggleDark, lang = lang, onToggleLang = onToggleLang, onRefresh = onRefresh) }
        when (tab) {
            HomeTab.MAP -> Unit
            HomeTab.HOME -> {
                item { CbuHeroCard(currency = currency, cbuRate = payload.cbu, updatedAt = payload.updatedAt) }
                item { SummaryRow(currency = currency, lang = lang, bestBuy = bestBuy, bestSell = bestSell) }
                item { SourceFooter() }
            }
            HomeTab.CALC -> {
                item {
                    ConverterCard(
                        currency = currency,
                        lang = lang,
                        amountText = amountText,
                        onAmountChange = onAmount,
                        mode = convertMode,
                        onModeChange = onMode,
                        buyRates = buyRates,
                        sellRates = sellRates
                    )
                }
                item { ProfitRankingSection(currency = currency, lang = lang, amountText = amountText, mode = convertMode, buyRates = buyRates, sellRates = sellRates) }
            }
            HomeTab.TREND -> {
                item { AnalyticsSection(currency = currency, lang = lang, analytics = analytics, loading = analyticsLoading) }
            }
            HomeTab.BANKS -> {
                item {
                    BankListHeader(
                        count = rates.size, query = query, onQuery = onQuery,
                        sortMode = sortMode, onSort = onSort
                    )
                }
                items(visibleRates, key = { it.bank }) { rate ->
                    BankRow(
                        rate = rate,
                        isBestBuy = bestBuy != null && rate.bank == bestBuy.bank,
                        isBestSell = bestSell != null && rate.bank == bestSell.bank
                    )
                }
                if (visibleRates.isEmpty()) {
                    item { Text(s.noBankFound(query), color = VsInkSoft, modifier = Modifier.padding(12.dp)) }
                }
                item { SourceFooter() }
            }
        }
    }
}

/* ---------- Analytics ---------- */

@Composable
private fun AnalyticsSection(currency: Currency, lang: Lang, analytics: Analytics?, loading: Boolean) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ShowChart, null, tint = VsEmerald, modifier = Modifier.size(20.dp))
            Text(s.analyticsTitle(currency.displayName(lang)), color = VsInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        when {
            loading && analytics == null -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(12.dp)) {
                    CircularProgressIndicator(color = VsEmerald, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text(s.analyticsLoading, color = VsInkSoft, fontSize = 13.sp)
                }
            }
            analytics == null -> Text(s.analyticsUnavailable, color = VsInkSoft, fontSize = 13.sp)
            !analytics.enoughData -> Text(s.analyticsNoHistory, color = VsInkSoft, fontSize = 13.sp)
            else -> {
                RecommendationCard(analytics)
                AnalyticsStatsRow(currency, analytics)
                if (analytics.points.size >= 2) HistoryChartCard(currency, lang, analytics.points)
                SeasonalityCard(analytics)
            }
        }
    }
}

@Composable
private fun RecommendationCard(a: Analytics) {
    val s = LocalStrings.current
    val up = a.trend == "up"
    val down = a.trend == "down"
    val accent = when { up -> VsEmerald; down -> VsDanger; else -> VsIndigo }
    val icon = when { up -> Icons.Outlined.TrendingUp; down -> Icons.Outlined.TrendingDown; else -> Icons.Outlined.TrendingFlat }
    val title = when { up -> s.trendUp; down -> s.trendDown; else -> s.trendFlat }
    val sellLine = s.sellAdvice(a.sellAdvice == "wait", down)
    val buyLine = s.buyAdvice(a.buyAdvice == "wait")

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = VsInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(s.forecastNext(a.forecastPct.formatPct()), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(sellLine, color = VsInk, fontSize = 13.sp)
        Text(buyLine, color = VsInkSoft, fontSize = 13.sp)
    }
}

@Composable
private fun AnalyticsStatsRow(currency: Currency, a: Analytics) {
    val s = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(s.statCurrent, a.current.formatRate(), "UZS", VsInk, Modifier.weight(1f))
        StatTile(s.statWeekly, a.weekChangePct.formatPct(), s.statChange, if (a.weekChangePct >= 0) VsEmeraldDeep else VsDanger, Modifier.weight(1f))
        StatTile(s.statForecast, a.forecastRate.formatRate(), s.statSevenDays, VsIndigo, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, hint: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(VsSurface)
            .border(1.dp, VsLine, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = VsInkSoft, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(hint, color = VsInkSoft, fontSize = 10.sp)
    }
}

private val CHART_RANGES = listOf(7, 30, 90, 365, Int.MAX_VALUE)

private fun rangeChipLabel(days: Int, s: AppStrings): String = when {
    days == Int.MAX_VALUE -> s.all
    days % 365 == 0 -> s.yearsChip(days / 365)
    else -> s.daysChip(days)
}

private fun windowByDays(points: List<HistoryPoint>, days: Int): List<HistoryPoint> {
    if (days == Int.MAX_VALUE || points.isEmpty()) return points
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun millis(d: String) = runCatching { fmt.parse(d)?.time }.getOrNull() ?: 0L
    val lastT = millis(points.last().date)
    val cutoff = lastT - days.toLong() * 86_400_000L
    return points.filter { millis(it.date) >= cutoff }.ifEmpty { points.takeLast(2) }
}

@Composable
private fun HistoryChartCard(currency: Currency, lang: Lang, points: List<HistoryPoint>) {
    val s = LocalStrings.current
    var rangeDays by remember { mutableStateOf(90) }
    var expanded by remember { mutableStateOf(false) }

    val window = remember(points, rangeDays) { windowByDays(points, rangeDays) }
    val minRate = window.minOf { it.rate }
    val maxRate = window.maxOf { it.rate }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(VsSurface)
            .border(1.dp, VsLine, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${s.tabTrend} • ${rangeChipLabel(rangeDays, s)}", color = VsInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(VsEmeraldSoft)
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.OpenInFull, s.zoomHint, tint = VsEmeraldDeep, modifier = Modifier.size(16.dp))
            }
        }

        LineChart(
            window = window, minRate = minRate, maxRate = maxRate,
            selectedIndex = null, lineColor = VsEmerald,
            modifier = Modifier.fillMaxWidth().height(150.dp)
                .pointerInput(Unit) { detectTapGestures { expanded = true } }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(window.firstOrNull()?.date?.let { formatShortDate(it) } ?: "", color = VsInkSoft, fontSize = 10.sp)
            Text("min ${minRate.formatRate()} • max ${maxRate.formatRate()}", color = VsInkSoft, fontSize = 10.sp)
            Text(window.lastOrNull()?.date?.let { formatShortDate(it) } ?: "", color = VsInkSoft, fontSize = 10.sp)
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CHART_RANGES.forEach { d ->
                RangeChip(rangeChipLabel(d, s), rangeDays == d) { rangeDays = d }
            }
        }
    }

    if (expanded) {
        FullScreenChartDialog(currency = currency, lang = lang, points = points, onDismiss = { expanded = false })
    }
}

@Composable
private fun RangeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (active) VsEmerald else VsBg)
            .border(1.dp, if (active) VsEmerald else VsLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (active) VsSurface else VsInkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Shared line renderer. If [selectedIndex] is set, draws a guide line + highlighted dot. */
@Composable
private fun LineChart(
    window: List<HistoryPoint>,
    minRate: Double,
    maxRate: Double,
    selectedIndex: Int?,
    lineColor: Color,
    modifier: Modifier
) {
    val dotColor = VsEmeraldDeep
    val guideColor = VsInkSoft
    Canvas(modifier = modifier) {
        if (window.size < 2) return@Canvas
        val range = (maxRate - minRate).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (window.size - 1)
        fun yFor(rate: Double) = size.height - ((rate - minRate) / range * size.height).toFloat() * 0.86f - size.height * 0.07f

        val line = Path()
        window.forEachIndexed { i, pt ->
            val x = stepX * i
            val y = yFor(pt.rate)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(stepX * (window.size - 1), size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.02f))))
        drawPath(line, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

        val idx = selectedIndex ?: (window.size - 1)
        val px = stepX * idx
        val py = yFor(window[idx].rate)
        if (selectedIndex != null) {
            drawLine(guideColor.copy(alpha = 0.4f), Offset(px, 0f), Offset(px, size.height), strokeWidth = 2f)
        }
        drawCircle(lineColor.copy(alpha = 0.25f), radius = 12f, center = Offset(px, py))
        drawCircle(dotColor, radius = 5.5f, center = Offset(px, py))
    }
}

@Composable
private fun FullScreenChartDialog(currency: Currency, lang: Lang, points: List<HistoryPoint>, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var rangeDays by remember { mutableStateOf(90) }
    var selected by remember { mutableStateOf<Int?>(null) }

    // Entrance animation.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(260), label = "appear")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxSize().background(VsBg)
                .graphicsLayer { alpha = appear; scaleX = 0.97f + 0.03f * appear; scaleY = 0.97f + 0.03f * appear }
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val window = windowByDays(points, rangeDays)
            selected = selected?.coerceIn(0, window.size - 1)
            val sel = selected?.let { window.getOrNull(it) } ?: window.last()
            val base = window.first().rate
            val changePct = if (base != 0.0) (sel.rate - base) / base * 100 else 0.0
            val up = changePct >= 0

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${currency.code}/UZS", color = VsInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(VsSurface)
                        .border(1.dp, VsLine, CircleShape).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, "close", tint = VsInk, modifier = Modifier.size(20.dp))
                }
            }

            // Big price readout + change for the selected (or latest) point.
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(sel.rate.formatRate(), color = if (up) VsEmeraldDeep else VsDanger, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(changePct.formatPct(), color = if (up) VsEmeraldDeep else VsDanger, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
            }
            Text(formatFullDate(sel.date), color = VsInkSoft, fontSize = 12.sp)

            PriceChart(
                window = window,
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.fillMaxWidth().height(340.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CHART_RANGES.forEach { d ->
                    RangeChip(rangeChipLabel(d, s), rangeDays == d) { rangeDays = d; selected = null }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Binance-style price chart: right-side price grid labels, area line, and a crosshair at the
 *  selected point with a price pill and a time label. */
@Composable
private fun PriceChart(
    window: List<HistoryPoint>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    val lineColor = VsEmerald
    val dotColor = VsEmeraldDeep
    val gridArgb = VsLine.toArgb()
    val textArgb = VsInkSoft.toArgb()
    val crossArgb = VsInk.toArgb()
    val pillArgb = VsEmerald.toArgb()
    val rightPad = with(LocalDensity.current) { 58.dp.toPx() }
    val bottomPad = with(LocalDensity.current) { 22.dp.toPx() }

    Canvas(
        modifier = modifier
            .pointerInput(window.size) {
                val plotW = size.width - rightPad
                val step = plotW / (window.size - 1).coerceAtLeast(1)
                detectTapGestures { pos -> onSelect((pos.x / step).roundToInt().coerceIn(0, window.size - 1)) }
            }
            .pointerInput(window.size) {
                val plotW = size.width - rightPad
                val step = plotW / (window.size - 1).coerceAtLeast(1)
                detectHorizontalDragGestures { change, _ ->
                    onSelect((change.position.x / step).roundToInt().coerceIn(0, window.size - 1))
                }
            }
    ) {
        if (window.size < 2) return@Canvas
        val minRate = window.minOf { it.rate }
        val maxRate = window.maxOf { it.rate }
        val range = (maxRate - minRate).takeIf { it > 0 } ?: 1.0
        val plotW = size.width - rightPad
        val plotH = size.height - bottomPad
        val topMargin = plotH * 0.08f
        val usableH = plotH - topMargin * 2
        fun yFor(rate: Double) = topMargin + (usableH - ((rate - minRate) / range * usableH).toFloat())
        val stepX = plotW / (window.size - 1)

        val labelPaint = android.graphics.Paint().apply {
            color = textArgb; isAntiAlias = true; textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.LEFT
        }

        // Horizontal price grid + right-axis labels.
        val gridLines = 4
        for (g in 0..gridLines) {
            val t = g.toFloat() / gridLines
            val y = topMargin + usableH * t
            val price = maxRate - t * (maxRate - minRate)
            drawLine(Color(gridArgb), Offset(0f, y), Offset(plotW, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(price.formatRate(), plotW + 8f, y + 4f, labelPaint)
        }

        // Area + line.
        val line = Path()
        window.forEachIndexed { i, pt ->
            val x = stepX * i; val y = yFor(pt.rate)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply { addPath(line); lineTo(stepX * (window.size - 1), plotH); lineTo(0f, plotH); close() }
        drawPath(fill, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)), endY = plotH))
        drawPath(line, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

        // Crosshair at the selected (or latest) point.
        val idx = selected ?: (window.size - 1)
        val px = stepX * idx
        val py = yFor(window[idx].rate)
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        if (selected != null) {
            drawLine(Color(crossArgb).copy(alpha = 0.45f), Offset(px, 0f), Offset(px, plotH), strokeWidth = 1.5f, pathEffect = dash)
            drawLine(Color(crossArgb).copy(alpha = 0.45f), Offset(0f, py), Offset(plotW, py), strokeWidth = 1.5f, pathEffect = dash)
            // Price pill on the right axis.
            val pillH = 20.sp.toPx()
            drawRoundRect(
                Color(pillArgb),
                topLeft = Offset(plotW + 2f, py - pillH / 2),
                size = androidx.compose.ui.geometry.Size(rightPad - 2f, pillH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            val pill = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE; isAntiAlias = true; textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(window[idx].rate.formatRate(), plotW + rightPad / 2, py + 4f, pill)
            // Time label at the bottom.
            val timePaint = android.graphics.Paint().apply {
                color = textArgb; isAntiAlias = true; textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(formatShortDate(window[idx].date), px.coerceIn(20f, plotW - 20f), size.height - 4f, timePaint)
        }
        drawCircle(lineColor.copy(alpha = 0.25f), radius = 12f, center = Offset(px, py))
        drawCircle(dotColor, radius = 5.5f, center = Offset(px, py))
    }
}

private fun formatShortDate(iso: String): String {
    // "2026-10-13" -> "13.10"
    val parts = iso.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}" else iso
}

private fun formatFullDate(iso: String): String {
    // "2026-10-13" -> "13.10.2026"
    val parts = iso.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
}

@Composable
private fun SeasonalityCard(a: Analytics) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(VsSurface)
            .border(1.dp, VsLine, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(s.seasonTitle, color = VsInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SeasonBucket(s.seasonStart, "1–10", a.seasonality.start, a.bestRisePeriod == "start", Modifier.weight(1f))
            SeasonBucket(s.seasonMid, "11–20", a.seasonality.mid, a.bestRisePeriod == "mid", Modifier.weight(1f))
            SeasonBucket(s.seasonEnd, "21–31", a.seasonality.end, a.bestRisePeriod == "end", Modifier.weight(1f))
        }
        val bestLabel = when (a.bestRisePeriod) { "start" -> s.periodStart; "mid" -> s.periodMid; else -> s.periodEnd }
        Text(s.bestRiseSentence(bestLabel), color = VsInkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun SeasonBucket(title: String, days: String, avgChange: Double, best: Boolean, modifier: Modifier = Modifier) {
    val up = avgChange >= 0
    val accent = if (up) VsEmeraldDeep else VsDanger
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(if (best) VsEmeraldSoft else VsBg)
            .border(1.dp, if (best) VsEmerald.copy(alpha = 0.4f) else VsLine, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = VsInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(days, color = VsInkSoft, fontSize = 10.sp)
        Text("${if (up) "▲" else "▼"} ${avgChange.formatSigned()}", color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/* ---------- Map ---------- */

private data class MapPlace(
    val name: String,
    val address: String?,
    val isAtm: Boolean,
    val lat: Double,
    val lng: Double
)

private const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"

private fun hasLocationPermission(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapScreen(
    innerPadding: PaddingValues,
    darkMode: Boolean,
    lang: Lang,
    branches: List<BankRate>,
    atms: List<Atm>,
    atmsLoading: Boolean
) {
    val context = LocalContext.current
    val s = LocalStrings.current

    var filter by remember { mutableStateOf(MapFilter.ALL) }
    var filterOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MapPlace?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasLocation by remember { mutableStateOf(hasLocationPermission(context)) }
    var locOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocation = granted
    }
    // Ask for location the moment the map screen opens.
    LaunchedEffect(Unit) { if (!hasLocation) permLauncher.launch(FINE_LOCATION) }

    val branchArgb = VsEmerald.toArgb()
    val atmArgb = VsIndigo.toArgb()
    val branchPin = remember(branchArgb) { pinDrawable(context, branchArgb) }
    val atmPin = remember(atmArgb) { pinDrawable(context, atmArgb) }

    val branchPlaces = remember(branches) {
        branches.mapNotNull { b -> b.branch?.let { MapPlace(b.bank, it.address, false, it.lat, it.lng) } }
    }
    val atmPlaces = remember(atms) { atms.map { MapPlace(it.name, null, true, it.lat, it.lng) } }
    val onSelect: (MapPlace) -> Unit = { selected = it }

    // Create the my-location overlay once the map exists and permission is granted.
    LaunchedEffect(mapView, hasLocation) {
        val mv = mapView
        if (mv != null && hasLocation && locOverlay == null) {
            val dot = ContextCompat.getDrawable(context, R.drawable.ic_location_dot)!!
                .toBitmap((26 * context.resources.displayMetrics.density).toInt(),
                          (26 * context.resources.displayMetrics.density).toInt())
            locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mv).apply {
                // Replace osmdroid's default arrow with a clean blue location dot.
                setPersonIcon(dot)
                setDirectionIcon(dot)
                setPersonAnchor(0.5f, 0.5f)
                setDirectionAnchor(0.5f, 0.5f)
                enableMyLocation()
            }
            mv.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().apply {
                    load(ctx, ctx.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
                    userAgentValue = ctx.packageName
                    osmdroidBasePath = ctx.cacheDir
                    osmdroidTileCache = File(ctx.cacheDir, "osm-tiles")
                }
                MapView(ctx).apply {
                    setTileSource(esriTiles(darkMode))
                    setMultiTouchControls(true)
                    minZoomLevel = 4.0
                    maxZoomLevel = esriMaxZoom(darkMode).toDouble()
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(41.311, 69.279))
                    mapView = this
                }
            },
            update = { map ->
                map.setTileSource(esriTiles(darkMode))
                map.maxZoomLevel = esriMaxZoom(darkMode).toDouble()
                map.overlays.clear()
                if (filter != MapFilter.ATMS) branchPlaces.forEach { map.overlays.add(placeMarker(map, it, branchPin, onSelect)) }
                if (filter != MapFilter.BRANCHES) atmPlaces.forEach { map.overlays.add(placeMarker(map, it, atmPin, onSelect)) }
                locOverlay?.let { map.overlays.add(it) }
                map.invalidate()
            },
            onRelease = { it.onDetach() }
        )

        // Top filter dropdown — tap to open, pick one option.
        Box(
            modifier = Modifier.align(Alignment.TopStart)
                .padding(top = innerPadding.calculateTopPadding() + 12.dp, start = 14.dp)
        ) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(VsSurface)
                    .border(1.dp, VsLine, RoundedCornerShape(50))
                    .shadow(0.dp)
                    .clickable { filterOpen = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.FilterList, null, tint = VsEmerald, modifier = Modifier.size(18.dp))
                Text(filterLabel(filter, s), color = VsInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Outlined.ExpandMore, null, tint = VsInkSoft, modifier = Modifier.size(18.dp))
                if (atmsLoading) CircularProgressIndicator(color = VsEmerald, strokeWidth = 2.dp, modifier = Modifier.size(13.dp))
            }
            DropdownMenu(
                expanded = filterOpen,
                onDismissRequest = { filterOpen = false },
                containerColor = VsSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MapFilter.entries.forEach { f ->
                        SortChip(filterLabel(f, s), f == filter) { filter = f; filterOpen = false }
                    }
                }
            }
        }

        // My-location button.
        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = innerPadding.calculateBottomPadding() + 20.dp)
                .size(52.dp).clip(CircleShape).background(VsSurface)
                .border(1.dp, VsLine, CircleShape)
                .clickable {
                    val gp = locOverlay?.myLocation
                    if (gp != null) {
                        mapView?.controller?.animateTo(gp)
                        mapView?.controller?.setZoom(15.0)
                    } else if (!hasLocation) {
                        permLauncher.launch(FINE_LOCATION)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = s.myLocation, tint = VsEmerald, modifier = Modifier.size(24.dp))
        }
    }

    selected?.let { place ->
        PlaceSheet(place = place, onDismiss = { selected = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSheet(place: MapPlace, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val accent = if (place.isAtm) VsIndigo else VsEmerald
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = VsSurface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Place, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Text(
                    if (place.isAtm) s.atmMarker else s.branchMarker,
                    color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(place.name, color = VsInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            place.address?.let {
                Text(it, color = VsInkSoft, fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Schedule, null, tint = VsInkSoft, modifier = Modifier.size(18.dp))
                Column {
                    Text(s.hoursLabel, color = VsInkSoft, fontSize = 11.sp)
                    Text(if (place.isAtm) s.atmHours else s.bankHours, color = VsInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VsEmerald)
                    .clickable { openRouteChooser(context, place, s.route) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Directions, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(s.route, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun filterLabel(filter: MapFilter, s: AppStrings) = when (filter) {
    MapFilter.ALL -> s.mapAll
    MapFilter.BRANCHES -> s.mapBranches
    MapFilter.ATMS -> s.mapAtms
}

private fun openRouteChooser(context: android.content.Context, place: MapPlace, chooserTitle: String) {
    // geo: intent without a package → Android shows the app chooser (Google Maps, Yandex, Waze…).
    val uri = Uri.parse("geo:${place.lat},${place.lng}?q=${place.lat},${place.lng}(${Uri.encode(place.name)})")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(Intent.createChooser(intent, chooserTitle)) }
}

// Max native zoom per Esri service (zooming past this shows "map data not yet available").
private fun esriMaxZoom(dark: Boolean) = if (dark) 16 else 19

// Esri ArcGIS Online tiles: no API key, no watermark. Tile path order is z/y/x.
private fun esriTiles(dark: Boolean): XYTileSource {
    val service = if (dark) "Canvas/World_Dark_Gray_Base" else "World_Street_Map"
    val bases = arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/$service/MapServer/tile/")
    return object : XYTileSource(
        if (dark) "EsriDark" else "EsriStreet",
        0, esriMaxZoom(dark), 256, "", bases, "© Esri, © OpenStreetMap contributors"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$z/$y/$x"
        }
    }
}

private fun pinDrawable(context: android.content.Context, argb: Int): Drawable {
    val d = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)!!.mutate()
    DrawableCompat.setTint(d, argb)
    return d
}

private fun placeMarker(map: MapView, place: MapPlace, markerIcon: Drawable, onSelect: (MapPlace) -> Unit): Marker =
    Marker(map).apply {
        position = GeoPoint(place.lat, place.lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        icon = markerIcon
        setInfoWindow(null)
        setOnMarkerClickListener { _, _ -> onSelect(place); true }
    }

/* ---------- Top bar ---------- */

@Composable
private fun TopBar(currency: Currency, onCurrency: (Currency) -> Unit, darkMode: Boolean, onToggleDark: () -> Unit, lang: Lang, onToggleLang: () -> Unit, onRefresh: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(VsHeroTop, VsHeroBot))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("ValyutaStat", color = VsInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                Text(s.subtitle(currency.displayName(lang)), color = VsInkSoft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Language toggle (UZ / RU).
            Box(
                modifier = Modifier.height(36.dp).clip(CircleShape).background(VsBg)
                    .border(1.dp, VsLine, CircleShape)
                    .clickable(onClick = onToggleLang)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(lang.code.uppercase(), color = VsInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(VsBg)
                    .border(1.dp, VsLine, CircleShape)
                    .clickable(onClick = onToggleDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = s.themeToggle,
                    tint = VsInk, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50)).background(VsEmeraldSoft)
                    .clickable(onClick = onRefresh).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(VsEmerald))
                Text(s.live, color = VsEmeraldDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Outlined.Refresh, null, tint = VsEmeraldDeep, modifier = Modifier.size(15.dp))
            }
        }
        CurrencyChips(currency = currency, onCurrency = onCurrency)
    }
}

@Composable
private fun CurrencyChips(currency: Currency, onCurrency: (Currency) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Currency.entries.forEach { c ->
            val selected = c == currency
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (selected) VsEmerald else VsSurface)
                    .border(1.dp, if (selected) VsEmerald else VsLine, RoundedCornerShape(50))
                    .clickable { onCurrency(c) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(c.symbol, color = if (selected) VsSurface else VsEmeraldDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(c.code, color = if (selected) VsSurface else VsInkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ---------- CBU hero ---------- */

@Composable
private fun CbuHeroCard(currency: Currency, cbuRate: CbuRate, updatedAt: String?) {
    val s = LocalStrings.current
    val up = cbuRate.diff >= 0
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(26.dp), spotColor = VsHeroTop)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(VsHeroTop, VsHeroMid, VsHeroBot)))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.cbuTitle, color = Color(0xFFCDE9DF), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (up) Color(0x3321D27A) else Color(0x33FF6B6B))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(if (up) "▲" else "▼", color = if (up) Color(0xFFB8F5D6) else Color(0xFFFFC2C2), fontSize = 11.sp)
                Text(cbuRate.diff.formatSigned(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedContent(
                targetState = cbuRate.rate,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "heroRate"
            ) { rate ->
                Text(rate.formatRate(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(8.dp))
            Text("UZS", color = Color(0xFFBFE3D8), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 7.dp))
        }
        Text(s.oneUnitDate(currency.code, cbuRate.date), color = Color(0xFFBFE3D8), fontSize = 12.sp)
        updatedAt?.let { Text(s.updated(it.formatUpdatedAt()), color = Color(0xFF9FD3C4), fontSize = 11.sp) }
    }
}
/* ---------- Summary ---------- */

@Composable
private fun SummaryRow(currency: Currency, lang: Lang, bestBuy: BankRate?, bestSell: BankRate?) {
    val s = LocalStrings.current
    val name = currency.displayName(lang)
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(s.bestBuyTitle, s.sellHint(name), bestBuy?.buy.formatUzs(), bestBuy?.bank ?: "—", VsEmerald, VsEmeraldSoft, Modifier.weight(1f).fillMaxHeight())
        SummaryCard(s.bestSellTitle, s.buyHint(name), bestSell?.sell.formatUzs(), bestSell?.bank ?: "—", VsIndigo, VsIndigoSoft, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun SummaryCard(title: String, hint: String, value: String, bank: String, accent: Color, soft: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(VsSurface)
            .border(1.dp, VsLine, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(soft).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = VsInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(4.dp))
            Text(LocalStrings.current.som, color = VsInkSoft, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text(bank, color = VsInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Text(hint, color = VsInkSoft, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ---------- Converter ---------- */

@Composable
private fun ConverterCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    mode: ConvertMode,
    onModeChange: (ConvertMode) -> Unit,
    buyRates: List<BankRate>,
    sellRates: List<BankRate>,
    currency: Currency,
    lang: Lang
) {
    val s = LocalStrings.current
    val name = currency.displayName(lang)
    val amount = amountText.toLongOrNull() ?: 0L
    val sellUsd = mode == ConvertMode.SELL_USD
    val bestBuy = buyRates.maxByOrNull { it.buy!! }
    val worstBuy = buyRates.minByOrNull { it.buy!! }
    val bestSell = sellRates.minByOrNull { it.sell!! }
    val worstSell = sellRates.maxByOrNull { it.sell!! }

    val bestOut: String
    val savings: String
    val bestBank: String
    if (sellUsd) {
        val b = bestBuy?.buy; val w = worstBuy?.buy
        bestOut = if (b != null) "${(amount * b).formatUzsL()} ${s.som}" else "—"
        savings = if (b != null && w != null) "${(amount * (b - w)).formatUzsL()} ${s.som}" else "—"
        bestBank = bestBuy?.bank ?: "—"
    } else {
        val b = bestSell?.sell; val w = worstSell?.sell
        bestOut = if (b != null) "${(amount.toDouble() / b).formatUsd()} ${currency.symbol}" else "—"
        savings = if (b != null && w != null) "${(amount.toDouble() / b - amount.toDouble() / w).formatUsd()} ${currency.symbol}" else "—"
        bestBank = bestSell?.bank ?: "—"
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(VsSurface)
            .border(1.dp, VsLine, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Bolt, null, tint = VsEmerald, modifier = Modifier.size(20.dp))
            Text(s.calcTitle, color = VsInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(VsBg).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegTab(s.sellMode(name), sellUsd, Modifier.weight(1f)) { onModeChange(ConvertMode.SELL_USD) }
            SegTab(s.buyMode(name), !sellUsd, Modifier.weight(1f)) { onModeChange(ConvertMode.BUY_USD) }
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (sellUsd) s.amountForeign(currency.code) else s.amountUzs) },
            suffix = { Text(if (sellUsd) currency.code else "UZS") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VsEmerald, focusedLabelColor = VsEmerald, cursorColor = VsEmerald)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultTile(s.getResult, bestOut, VsEmerald, VsEmeraldSoft, Modifier.weight(1f))
            ResultTile(s.savings, savings, VsIndigo, VsIndigoSoft, Modifier.weight(1f))
        }
        Text(s.bestBank(bestBank), color = VsInkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun SegTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) VsSurface else Color.Transparent, label = "segbg")
    Box(
        modifier = modifier.clip(RoundedCornerShape(11.dp)).background(bg)
            .then(if (selected) Modifier.border(1.dp, VsLine, RoundedCornerShape(11.dp)) else Modifier)
            .clickable(onClick = onClick).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) VsInk else VsInkSoft, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun ResultTile(label: String, value: String, accent: Color, soft: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(soft).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = VsInkSoft, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ---------- Profit ranking + nearest branch ---------- */

private data class ProfitItem(
    val rank: Int,
    val bank: String,
    val outLabel: String,
    val profitLabel: String,
    val branch: Branch?
)

@Composable
private fun ProfitRankingSection(
    currency: Currency,
    lang: Lang,
    amountText: String,
    mode: ConvertMode,
    buyRates: List<BankRate>,
    sellRates: List<BankRate>
) {
    val s = LocalStrings.current
    val name = currency.displayName(lang)
    val som = s.som
    val symbol = currency.symbol
    val amount = amountText.toLongOrNull() ?: 0L
    val sellUsd = mode == ConvertMode.SELL_USD

    val ranked: List<ProfitItem> = remember(amountText, mode, buyRates, sellRates, currency, som) {
        if (sellUsd) {
            val sorted = buyRates.sortedByDescending { it.buy!! }
            val worst = sorted.lastOrNull()?.buy ?: 0
            sorted.mapIndexed { i, r ->
                ProfitItem(
                    rank = i + 1,
                    bank = r.bank,
                    outLabel = "${(amount * r.buy!!).formatUzsL()} $som",
                    profitLabel = "+${(amount * (r.buy - worst)).formatUzsL()} $som",
                    branch = r.branch
                )
            }
        } else {
            val sorted = sellRates.sortedBy { it.sell!! }
            val worst = sorted.lastOrNull()?.sell ?: 1
            sorted.mapIndexed { i, r ->
                val got = amount.toDouble() / r.sell!!
                val worstGot = amount.toDouble() / worst
                ProfitItem(
                    rank = i + 1,
                    bank = r.bank,
                    outLabel = "${got.formatUsd()} $symbol",
                    profitLabel = "+${(got - worstGot).formatUsd()} $symbol",
                    branch = r.branch
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.EmojiEvents, null, tint = VsAmber, modifier = Modifier.size(20.dp))
            Text(s.profitTitle, color = VsInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            if (sellUsd) s.profitSell(name) else s.profitBuy(name),
            color = VsInkSoft, fontSize = 12.sp
        )
        ranked.take(5).forEach { ProfitRow(it) }
    }
}

@Composable
private fun ProfitRow(item: ProfitItem) {
    val context = LocalContext.current
    val top = item.rank == 1
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (top) VsEmeraldSoft else VsSurface)
            .border(1.dp, if (top) VsEmerald.copy(alpha = 0.4f) else VsLine, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(if (top) VsEmerald else VsBg),
            contentAlignment = Alignment.Center
        ) {
            Text("${item.rank}", color = if (top) Color.White else VsInkSoft, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.bank, color = VsInk, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.outLabel, color = VsInk, fontSize = 12.sp)
            item.branch?.let { Text(it.address, color = VsInkSoft, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.profitLabel, color = VsEmeraldDeep, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            item.branch?.let { b ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50)).background(VsIndigo)
                        .clickable { openNavigation(context, b, item.bank) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Directions, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text(LocalStrings.current.route, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun openNavigation(context: android.content.Context, branch: Branch, bankName: String) {
    // Opens Google Maps navigation from the user's current location — no Maps API key required.
    val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${branch.lat},${branch.lng}"))
        .apply { setPackage("com.google.android.apps.maps") }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${branch.lat},${branch.lng}")
    )
    runCatching { context.startActivity(navIntent) }
        .onFailure { runCatching { context.startActivity(webIntent) } }
}

/* ---------- Bank list ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankListHeader(count: Int, query: String, onQuery: (String) -> Unit, sortMode: SortMode, onSort: (SortMode) -> Unit) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.SwapVert, null, tint = VsEmerald, modifier = Modifier.size(20.dp))
            Text(s.banks, color = VsInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.clip(RoundedCornerShape(50)).background(VsEmeraldSoft).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("$count", color = VsEmeraldDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(s.searchBank, color = VsInkSoft) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = VsInkSoft) },
            singleLine = true, shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VsEmerald, unfocusedBorderColor = VsLine, cursorColor = VsEmerald,
                focusedContainerColor = VsSurface, unfocusedContainerColor = VsSurface
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(s.sortByBuy, sortMode == SortMode.BUY) { onSort(SortMode.BUY) }
            SortChip(s.sortBySell, sortMode == SortMode.SELL) { onSort(SortMode.SELL) }
        }
    }
}

@Composable
private fun SortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) VsEmerald else VsSurface)
            .border(1.dp, if (selected) VsEmerald else VsLine, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) VsSurface else VsInkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val avatarColors = listOf(
    Color(0xFF0E9F6E), Color(0xFF4F46E5), Color(0xFFE5484D), Color(0xFFF59E0B),
    Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899)
)

@Composable
private fun BankRow(rate: BankRate, isBestBuy: Boolean, isBestSell: Boolean) {
    val s = LocalStrings.current
    val avatarColor = avatarColors[(rate.bank.hashCode() and 0x7FFFFFFF) % avatarColors.size]
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(VsSurface)
            .border(1.dp, VsLine, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(avatarColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(rate.bank.take(2).uppercase(), color = avatarColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(rate.bank, color = VsInk, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (rate.branch != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.clickable { openNavigation(context, rate.branch, rate.bank) }
                ) {
                    Icon(Icons.Outlined.Place, null, tint = VsIndigo, modifier = Modifier.size(12.dp))
                    Text(s.route, color = VsIndigo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    rate.spread?.let { Text(s.spreadShort(it), color = VsInkSoft, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                }
            } else {
                Text(rate.spread?.let { s.spread(it) } ?: s.source, color = VsInkSoft, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RateLine(s.colBuy, rate.buy, isBestBuy, VsEmerald, VsEmeraldSoft)
            RateLine(s.colSell, rate.sell, isBestSell, VsIndigo, VsIndigoSoft)
        }
    }
}

@Composable
private fun RateLine(label: String, value: Int?, best: Boolean, accent: Color, soft: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            (if (best) "★ " else "") + label,
            color = if (best) accent else VsInkSoft, fontSize = 10.sp, maxLines = 1, softWrap = false
        )
        Text(
            value.formatUzs(),
            color = if (best) accent else VsInk, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            maxLines = 1, softWrap = false,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (best) soft else Color.Transparent).padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/* ---------- footer ---------- */

@Composable
private fun SourceFooter() {
    Text(
        LocalStrings.current.footer,
        color = VsInkSoft, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

/* ---------- formatting ---------- */

private fun Int?.formatUzs(): String =
    if (this == null) "—" else DecimalFormat("#,###").format(this).replace(",", " ")

private fun Long.formatUzsL(): String = DecimalFormat("#,###").format(this).replace(",", " ")

private fun Double.formatUsd(): String =
    DecimalFormat("#,##0.00").format(this).replace(",", "X").replace(".", ",").replace("X", " ")

private fun Double.formatRate(): String = DecimalFormat("#,##0.00").format(this).replace(",", " ")

private fun Double.formatPct(): String {
    val formatted = DecimalFormat("0.00").format(kotlin.math.abs(this))
    return if (this >= 0) "+$formatted%" else "-$formatted%"
}

private fun Double.formatSigned(): String {
    val formatted = DecimalFormat("#,##0.##").format(kotlin.math.abs(this)).replace(",", " ")
    return if (this >= 0) "+$formatted" else "-$formatted"
}

private fun String.formatUpdatedAt(): String {
    val timePart = substringAfter('T', missingDelimiterValue = "")
    return if (timePart.isEmpty()) this else timePart.take(5)
}

/* ---------- network ---------- */

private suspend fun fetchRates(cur: Currency): RatesPayload = withContext(Dispatchers.IO) {
    val connection = (URL(ratesUrl(cur)).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = 15000
    }
    try {
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseRatesPayload(body)
    } finally {
        connection.disconnect()
    }
}

private fun parseRatesPayload(body: String): RatesPayload {
    val root = JSONObject(body)
    val cbuJson = root.getJSONObject("cbu")
    val banksJson = root.getJSONArray("banks")
    val banks = buildList {
        for (index in 0 until banksJson.length()) {
            val item = banksJson.getJSONObject(index)
            val buy = if (item.isNull("buy")) null else item.getInt("buy")
            val sell = if (item.isNull("sell")) null else item.getInt("sell")
            if (buy == null && sell == null) continue
            val branchJson = item.optJSONObject("branch")
            val branch = branchJson?.let {
                Branch(
                    address = it.optString("address"),
                    lat = it.optDouble("lat"),
                    lng = it.optDouble("lng")
                )
            }
            add(BankRate(bank = item.getString("bank"), buy = buy, sell = sell, branch = branch))
        }
    }
    return RatesPayload(
        cbu = CbuRate(
            rate = cbuJson.getDouble("rate"),
            diff = cbuJson.optDouble("diff", 0.0),
            date = cbuJson.optString("date")
        ),
        banks = banks,
        updatedAt = root.optString("updatedAt").ifEmpty { null }
    )
}

private suspend fun fetchAtms(): List<Atm> = withContext(Dispatchers.IO) {
    val connection = (URL(backendAtmsUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = 20000
    }
    try {
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseAtms(body)
    } finally {
        connection.disconnect()
    }
}

private fun parseAtms(body: String): List<Atm> {
    val arr = JSONObject(body).getJSONArray("atms")
    return buildList {
        for (index in 0 until arr.length()) {
            val item = arr.getJSONObject(index)
            val lat = item.optDouble("lat", Double.NaN)
            val lng = item.optDouble("lng", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            add(
                Atm(
                    bank = item.optString("bank").ifEmpty { "Bankomat" },
                    name = item.optString("name").ifEmpty { "Bankomat" },
                    lat = lat,
                    lng = lng
                )
            )
        }
    }
}

private suspend fun fetchAnalytics(cur: Currency): Analytics = withContext(Dispatchers.IO) {
    val analyticsBody = httpGet(analyticsUrl(cur), 20000)
    val historyBody = runCatching { httpGet(historyUrl(cur), 20000) }.getOrNull()
    parseAnalytics(analyticsBody, historyBody)
}

private fun httpGet(url: String, readTimeoutMs: Int): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = readTimeoutMs
    }
    return try {
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun parseAnalytics(body: String, historyBody: String?): Analytics {
    val root = JSONObject(body)
    val enough = root.optBoolean("enoughData", false)
    val seasonalityJson = root.optJSONObject("seasonality")
    val recJson = root.optJSONObject("recommendation")
    val points = buildList {
        val arr = historyBody?.let { JSONObject(it).optJSONArray("points") }
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val rate = p.optDouble("rate", Double.NaN)
                if (!rate.isNaN()) add(HistoryPoint(p.optString("date"), rate))
            }
        }
    }
    return Analytics(
        enoughData = enough,
        current = root.optDouble("current", 0.0),
        weekAgo = root.optDouble("weekAgo", 0.0),
        weekChangePct = root.optDouble("weekChangePct", 0.0),
        trend = root.optString("trend", "flat"),
        forecastRate = root.optDouble("forecastRate", 0.0),
        forecastPct = root.optDouble("forecastPct", 0.0),
        seasonality = Seasonality(
            start = seasonalityJson?.optDouble("start", 0.0) ?: 0.0,
            mid = seasonalityJson?.optDouble("mid", 0.0) ?: 0.0,
            end = seasonalityJson?.optDouble("end", 0.0) ?: 0.0
        ),
        bestRisePeriod = root.optString("bestRisePeriod", "start"),
        sellAdvice = recJson?.optString("sell", "now") ?: "now",
        buyAdvice = recJson?.optString("buy", "now") ?: "now",
        points = points
    )
}

@Preview(showBackground = true)
@Composable
fun CurrencyTrackerPreview() {
    val demo = RatesPayload(
        cbu = CbuRate(11899.09, 1.19, "13.10.2026"),
        banks = listOf(
            BankRate("MKBank", 11870, 11940, Branch("Toshkent, Nukus 3", 41.2946, 69.2417)),
            BankRate("Milliy Bank", 11860, 11940, Branch("Toshkent, Amir Temur 101", 41.3306, 69.2847)),
            BankRate("Asia Alliance", 11845, 11905, null),
            BankRate("Anorbank", 11840, 11910, null)
        ),
        updatedAt = "2026-10-13T09:10:00.000Z"
    )
    BankKurslariTheme(dynamicColor = false) {
        SuccessScreen(
            innerPadding = PaddingValues(0.dp), tab = HomeTab.HOME, currency = Currency.USD, onCurrency = {},
            darkMode = false, onToggleDark = {}, lang = Lang.UZ, onToggleLang = {},
            payload = demo, atms = emptyList(), atmsLoading = false,
            analytics = null, analyticsLoading = false,
            amountText = "100", onAmount = {},
            query = "", onQuery = {}, sortMode = SortMode.BUY, onSort = {},
            convertMode = ConvertMode.SELL_USD, onMode = {}, onRefresh = {}
        )
    }
}
