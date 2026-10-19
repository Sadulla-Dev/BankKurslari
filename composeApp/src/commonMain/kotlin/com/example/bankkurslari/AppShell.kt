package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsTealTint
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import kotlinx.coroutines.launch




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyTrackerApp(
    themeMode: Int,
    onThemeMode: (Int) -> Unit,
    lang: Lang,
    onLang: (Lang) -> Unit
) {
    val noBankRates = tr("Bank kurslari hozircha mavjud emas. Keyinroq urinib koʻring.", "Курсы банков пока недоступны. Попробуйте позже.", "Bank rates are not available yet. Please try again later.")
    val connectFail = tr("Serverga ulanib boʻlmadi.", "Не удалось подключиться к серверу.", "Could not connect to the server.")
    var payload by remember { mutableStateOf<RatesPayload?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf(appStore().getString("amount")?.takeIf { it.isNotBlank() } ?: "100") }
    var direction by remember {
        mutableStateOf(if ((appStore().getString("direction") ?: "sell") == "buy") ConvertMode.BUY_USD else ConvertMode.SELL_USD)
    }
    var tab by remember { mutableStateOf(HomeTab.KURS) }
    var currency by remember {
        mutableStateOf(
            Currency.entries.firstOrNull { it.code == RateService.selectedCurrency() } ?: Currency.USD
        )
    }
    var atms by remember { mutableStateOf<List<Atm>>(emptyList()) }
    var atmsLoading by remember { mutableStateOf(false) }
    var osmBranches by remember { mutableStateOf<List<OsmBranch>>(emptyList()) }
    var analytics by remember { mutableStateOf<Analytics?>(null) }
    var analyticsLoading by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(RateService.favorites()) }
    var showSettings by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(lastKnownLocation()) }
    val scope = rememberCoroutineScope()
    val saveable = rememberSaveableStateHolder()
    // Ticker cards: CBU rate + 30-day history for every currency. Cache first, then network in background.
    var tickers by remember { mutableStateOf(Currency.entries.associateWith { TickerData(null, emptyList()) }) }
    LaunchedEffect(Unit) {
        // Cache first (off the main thread), then the network.
        Currency.entries.forEach { c ->
            val cbu = cachedRates(c)?.cbu
            val hist = cachedHistory(c)
            tickers = tickers + (c to TickerData(tickers[c]?.cbu ?: cbu, tickers[c]?.history?.ifEmpty { null } ?: hist))
        }
        Currency.entries.forEach { c ->
            launch {
                runCatching { fetchRates(c) }.getOrNull()?.let { r ->
                    tickers = tickers + (c to (tickers[c] ?: TickerData(null, emptyList())).copy(cbu = r.cbu))
                }
            }
            launch {
                runCatching { fetchHistory(c) }.getOrNull()?.takeIf { it.size >= 2 }?.let { h ->
                    tickers = tickers + (c to (tickers[c] ?: TickerData(null, emptyList())).copy(history = h))
                }
            }
        }
    }

    // Ask for location once (non-blocking); distances are simply omitted without it.
    val locationLauncher = rememberLocationPermissionRequest {
        userLocation = lastKnownLocation()
    }
    LaunchedEffect(Unit) {
        val asked = appStore().getBoolean("loc_asked", false)
        if (userLocation == null && !asked && !hasLocationPermission()) {
            appStore().putBoolean("loc_asked", true)
            locationLauncher()
        }
    }
    LaunchedEffect(tab) { userLocation = lastKnownLocation() ?: userLocation }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            runCatching { fetchRates(currency) }.fold(
                onSuccess = { p ->
                    if (p.banks.isEmpty()) error = noBankRates
                    else { payload = p; offline = false }
                },
                onFailure = {
                    // No connection: fall back to the last cached rates if available.
                    val cached = cachedRates(currency)
                    if (cached != null && cached.banks.isNotEmpty()) { payload = cached; offline = true }
                    else error = it.message ?: connectFail
                }
            )
            loading = false
        }
    }

    // First paint from cache (<300 ms), then refresh from the network in the background.
    LaunchedEffect(currency) {
        cachedRates(currency)?.let { if (it.banks.isNotEmpty()) payload = it }
        refresh()
    }

    // Branch hours/locations feed distance + "open now" in Kurs and the priced pins in Xarita.
    LaunchedEffect(Unit) {
        osmBranches = runCatching { fetchBranches() }.getOrDefault(emptyList())
    }

    // ATMs only matter for the map.
    LaunchedEffect(tab) {
        if (tab == HomeTab.MAP && atms.isEmpty() && !atmsLoading) {
            atmsLoading = true
            atms = runCatching { fetchAtms() }.getOrDefault(emptyList())
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
        RateService.setSelectedCurrency(it.code)
        PlatformHooks.onCurrencyChanged()
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
                            onToggleFavorite = { favorites = RateService.toggleFavorite(it) },
                            amountText = amountText,
                            onAmount = {
                                amountText = it.filter(Char::isDigit).take(9)
                                appStore().putString("amount", amountText)
                            },
                            direction = direction,
                            onDirection = {
                                direction = it
                                appStore().putString("direction", if (it == ConvertMode.BUY_USD) "buy" else "sell")
                            },
                            userLocation = userLocation,
                            tickers = tickersView,
                            onOpenSettings = { showSettings = true },
                            onShowOnMap = { tab = HomeTab.MAP },
                            onRefresh = { refresh() },
                            onRequestLocation = { locationLauncher() }
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
                            onToggleFavorite = { favorites = RateService.toggleFavorite(it) },
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
                            amount = amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0,
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

    // Settings: full-screen overlay above the tab bar; back / back button closes it.
    AnimatedVisibility(
        visible = showSettings,
        enter = slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 4 } + fadeIn(tween(Motion.MEDIUM)),
        exit = slideOutHorizontally(tween(Motion.SHORT)) { it / 4 } + fadeOut(tween(Motion.SHORT)),
        modifier = Modifier.fillMaxSize()
    ) {
        SettingsScreen(
            onDismiss = { showSettings = false },
            lang = lang,
            onLang = onLang,
            themeMode = themeMode,
            onThemeMode = onThemeMode,
            defaultCurrency = currency,
            onDefaultCurrency = onCurrency,
            amountText = amountText,
            onAmount = {
                amountText = it.filter(Char::isDigit).take(9)
                appStore().putString("amount", amountText)
            },
            onLocationGranted = { userLocation = lastKnownLocation() },
            onOpenAlerts = { showSettings = false; tab = HomeTab.TREND }
        )
    }
}

@Composable
private fun isDarkTheme(themeMode: Int): Boolean =
    when (themeMode) { 1 -> false; 2 -> true; else -> androidx.compose.foundation.isSystemInDarkTheme() }

/* ---------- Bottom navigation: card surface, hairline top, teal-tint pill behind the selected icon ---------- */

@Composable
private fun BottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(VsCard).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(VsHair))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HomeTab.entries.forEach { item ->
                val active = item == selected
                val source = remember { MutableInteractionSource() }
                val tint by animateColorAsState(if (active) VsEmerald else VsInkSoft, Motion.color(), label = "navtint")
                // Icon animation: replays from 0 each time the tab becomes selected, rests at 1.
                val play = remember { Animatable(1f) }
                LaunchedEffect(active) {
                    if (active) { play.snapTo(0f); play.animateTo(1f, tween(520, easing = LinearEasing)) }
                }
                val pill by animateFloatAsState(if (active) 1f else 0f, tween(Motion.MEDIUM, easing = Motion.Decelerate), label = "navpill")
                val label = item.label()
                Column(
                    modifier = Modifier
                        .width(84.dp)
                        .pressScale(source)
                        .clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = { onSelect(item) })
                        .semantics { this.selected = active; contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.width(56.dp).height(32.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.matchParentSize()
                                .graphicsLayer { scaleX = 0.5f + 0.5f * pill; alpha = pill }
                                .clip(RoundedCornerShape(50)).background(VsTealTint)
                        )
                        NavIcon(item, tint, play.value)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label, color = tint, style = VsType.Tab,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
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
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = VsInkSoft, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(tr("Maʼlumot olinmadi", "Не удалось получить данные", "Could not load data"), color = VsInk, style = VsType.Title)
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
            Text(tr("Qayta urinish", "Повторить", "Retry"), color = VsBg, style = VsType.BodyMedium)
        }
    }
}
