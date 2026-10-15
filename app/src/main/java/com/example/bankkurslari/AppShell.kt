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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.example.bankkurslari.ui.theme.VsType

private const val UI_PREFS = "valyutastat"

private fun uiPrefs(c: android.content.Context) = c.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyTrackerApp(
    themeMode: Int,
    onThemeMode: (Int) -> Unit,
    lang: Lang,
    onLang: (Lang) -> Unit
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var payload by remember { mutableStateOf<RatesPayload?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf(uiPrefs(context).getString("amount", null)?.takeIf { it.isNotBlank() } ?: "100") }
    var direction by remember {
        mutableStateOf(if (uiPrefs(context).getString("direction", "sell") == "buy") ConvertMode.BUY_USD else ConvertMode.SELL_USD)
    }
    var tab by remember { mutableStateOf(HomeTab.KURS) }
    var currency by remember {
        mutableStateOf(
            Currency.entries.firstOrNull { it.code == RateService.selectedCurrency(context) } ?: Currency.USD
        )
    }
    var atms by remember { mutableStateOf<List<Atm>>(emptyList()) }
    var atmsLoading by remember { mutableStateOf(false) }
    var osmBranches by remember { mutableStateOf<List<OsmBranch>>(emptyList()) }
    var analytics by remember { mutableStateOf<Analytics?>(null) }
    var analyticsLoading by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(RateService.favorites(context)) }
    var showSettings by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(lastKnownLocation(context)) }
    val scope = rememberCoroutineScope()
    val saveable = rememberSaveableStateHolder()
    // Ticker cards: CBU rate + 30-day history for every currency. Cache first, then network in background.
    var tickers by remember {
        mutableStateOf(Currency.entries.associateWith { c -> TickerData(cachedRates(context, c)?.cbu, cachedHistory(context, c)) })
    }
    LaunchedEffect(Unit) {
        Currency.entries.forEach { c ->
            launch {
                runCatching { fetchRates(context, c) }.getOrNull()?.let { r ->
                    tickers = tickers + (c to (tickers[c] ?: TickerData(null, emptyList())).copy(cbu = r.cbu))
                }
            }
            launch {
                runCatching { fetchHistory(context, c) }.getOrNull()?.takeIf { it.size >= 2 }?.let { h ->
                    tickers = tickers + (c to (tickers[c] ?: TickerData(null, emptyList())).copy(history = h))
                }
            }
        }
    }

    // Ask for location once (non-blocking); distances are simply omitted without it.
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        userLocation = lastKnownLocation(context)
    }
    LaunchedEffect(Unit) {
        val asked = uiPrefs(context).getBoolean("loc_asked", false)
        if (userLocation == null && !asked && !hasLocationPermission(context)) {
            uiPrefs(context).edit().putBoolean("loc_asked", true).apply()
            locationLauncher.launch(FINE_LOCATION)
        }
    }
    LaunchedEffect(tab) { userLocation = lastKnownLocation(context) ?: userLocation }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            runCatching { fetchRates(context, currency) }.fold(
                onSuccess = { p ->
                    if (p.banks.isEmpty()) error = s.noBankRates
                    else { payload = p; offline = false }
                },
                onFailure = {
                    // No connection: fall back to the last cached rates if available.
                    val cached = cachedRates(context, currency)
                    if (cached != null && cached.banks.isNotEmpty()) { payload = cached; offline = true }
                    else error = it.message ?: s.connectFail
                }
            )
            loading = false
        }
    }

    // First paint from cache (<300 ms), then refresh from the network in the background.
    LaunchedEffect(currency) {
        cachedRates(context, currency)?.let { if (it.banks.isNotEmpty()) payload = it }
        refresh()
    }

    // Branch hours/locations feed distance + "open now" in Kurs and the priced pins in Xarita.
    LaunchedEffect(Unit) {
        osmBranches = runCatching { fetchBranches(context) }.getOrDefault(emptyList())
    }

    // ATMs only matter for the map.
    LaunchedEffect(tab) {
        if (tab == HomeTab.MAP && atms.isEmpty() && !atmsLoading) {
            atmsLoading = true
            atms = runCatching { fetchAtms(context) }.getOrDefault(emptyList())
            atmsLoading = false
        }
    }

    LaunchedEffect(currency) { analytics = null }
    LaunchedEffect(tab, currency) {
        if (tab == HomeTab.TREND && analytics == null && !analyticsLoading) {
            analyticsLoading = true
            analytics = runCatching { fetchAnalytics(currency) }.getOrNull()
            analyticsLoading = false
        }
    }

    val onCurrency: (Currency) -> Unit = {
        currency = it
        RateService.setSelectedCurrency(context, it.code)
        RateWorker.enqueueNow(context)
    }

    val p = payload
    // The selected currency always shows the freshest CBU value from the main payload.
    val tickersView = if (p != null) tickers + (currency to (tickers[currency] ?: TickerData(null, emptyList())).copy(cbu = p.cbu)) else tickers
    RouteHost {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VsBg,
        bottomBar = { if (p != null) BottomBar(selected = tab, onSelect = { tab = it }) }
    ) { innerPadding ->
        when {
            p != null -> {
                Box(Modifier.fillMaxSize()) {
                    // Fade-through between tabs (Material): old fades out fast, new fades in with a slight scale settle.
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(210, delayMillis = 90, easing = Motion.Decelerate)) +
                                scaleIn(tween(210, delayMillis = 90, easing = Motion.Decelerate), initialScale = 0.98f)) togetherWith
                                fadeOut(tween(90))
                        },
                        label = "tabs"
                    ) { current ->
                    saveable.SaveableStateProvider(current) {
                    when (current) {
                        HomeTab.KURS -> KursScreen(
                            innerPadding = innerPadding,
                            currency = currency,
                            onCurrency = onCurrency,
                            lang = lang,
                            payload = p,
                            osmBranches = osmBranches,
                            offline = offline,
                            loading = loading,
                            favorites = favorites,
                            onToggleFavorite = { favorites = RateService.toggleFavorite(context, it) },
                            amountText = amountText,
                            onAmount = {
                                amountText = it.filter(Char::isDigit).take(9)
                                uiPrefs(context).edit().putString("amount", amountText).apply()
                            },
                            direction = direction,
                            onDirection = {
                                direction = it
                                uiPrefs(context).edit().putString("direction", if (it == ConvertMode.BUY_USD) "buy" else "sell").apply()
                            },
                            userLocation = userLocation,
                            tickers = tickersView,
                            onOpenSettings = { showSettings = true },
                            onRefresh = { refresh() }
                        )
                        HomeTab.MAP -> MapScreen(
                            innerPadding = innerPadding,
                            darkMode = isDarkTheme(themeMode),
                            currency = currency,
                            onCurrency = onCurrency,
                            payload = p,
                            branches = osmBranches,
                            atms = atms,
                            atmsLoading = atmsLoading,
                            favorites = favorites,
                            onToggleFavorite = { favorites = RateService.toggleFavorite(context, it) },
                            amountText = amountText,
                            direction = direction,
                            userLocation = userLocation
                        )
                        HomeTab.TREND -> TarixScreen(
                            innerPadding = innerPadding,
                            currency = currency,
                            onCurrency = onCurrency,
                            lang = lang,
                            payload = p,
                            analytics = analytics,
                            analyticsLoading = analyticsLoading,
                            tickers = tickersView,
                            offline = offline,
                            direction = direction,
                            onDirection = {
                                direction = it
                                uiPrefs(context).edit().putString("direction", if (it == ConvertMode.BUY_USD) "buy" else "sell").apply()
                            },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                    }
                    }
                    // Kurs has its own pull-to-refresh indicator; elsewhere a 2 dp hairline progress.
                    if (loading && tab != HomeTab.KURS) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(innerPadding).height(2.dp),
                            color = VsEmerald,
                            trackColor = VsLine
                        )
                    }
                }
            }
            else -> Crossfade(targetState = if (error != null && !loading) 1 else 0, animationSpec = tween(Motion.MEDIUM), label = "boot") { st ->
                if (st == 1) ErrorScreen(innerPadding, error ?: "", onRetry = { refresh() }) else LoadingScreen(innerPadding)
            }
        }
    }

    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            lang = lang,
            onLang = onLang,
            themeMode = themeMode,
            onThemeMode = onThemeMode,
            defaultCurrency = currency,
            onDefaultCurrency = onCurrency,
            onOpenAlerts = { showSettings = false; tab = HomeTab.TREND }
        )
    }
}

@Composable
private fun isDarkTheme(themeMode: Int): Boolean =
    when (themeMode) { 1 -> false; 2 -> true; else -> androidx.compose.foundation.isSystemInDarkTheme() }

/* ---------- Bottom navigation: flat, hairline top, 3 tabs ---------- */

@Composable
private fun BottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(VsBg).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(VsLine))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { item ->
                val active = item == selected
                val tint by animateColorAsState(if (active) VsEmerald else VsInkSoft, Motion.color(), label = "navtint")
                val iconScale by animateFloatAsState(if (active) 1f else 0.92f, Motion.slide(), label = "navscale")
                val label = item.label()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null, role = Role.Tab, onClick = { onSelect(item) }
                        )
                        .semantics { this.selected = active; contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(item.icon, null, tint = tint, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label, color = tint, style = VsType.Meta,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/* ---------- Loading / Error ---------- */

@Composable
private fun LoadingScreen(padding: PaddingValues) {
    val a by androidx.compose.animation.core.rememberInfiniteTransition(label = "boot").animateFloat(
        0.4f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(900, easing = Motion.Standard), androidx.compose.animation.core.RepeatMode.Reverse), label = "a"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).alpha(a),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skeleton rows instead of a spinner over a blank screen.
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface))
        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface))
        Box(Modifier.fillMaxWidth(0.6f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface))
        repeat(6) {
            Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface))
        }
    }
}

@Composable
private fun ErrorScreen(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = VsInkSoft, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(s.errorTitle, color = VsInk, style = VsType.Title)
        Spacer(Modifier.height(8.dp))
        Text(message, color = VsInkSoft, style = VsType.Meta, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VsEmerald)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Refresh, null, tint = VsBg, modifier = Modifier.size(20.dp))
            Text(s.retry, color = VsBg, style = VsType.BodyMedium)
        }
    }
}
