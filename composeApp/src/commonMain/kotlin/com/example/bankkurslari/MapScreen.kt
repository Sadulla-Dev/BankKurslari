package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

/* ---------- Screen ---------- */

/**
 * "Which branch near me has a good rate?" One currency control (shared with Home), one filter button, the map.
 * Zoomed out each bank is one pin; zooming in reveals its branches. Tap a pin -> the same bank sheet as Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    innerPadding: PaddingValues,
    darkMode: Boolean,
    currency: Currency,
    onCurrency: (Currency) -> Unit,
    payload: RatesPayload,
    branches: List<OsmBranch>,
    atms: List<Atm>,
    atmsLoading: Boolean,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    amountText: String,
    direction: ConvertMode,
    userLocation: Pair<Double, Double>?,
    onRequestLocation: () -> Unit
) {
    var openNow by remember { mutableStateOf(false) }
    var bestOnly by remember { mutableStateOf(false) }
    var atmOn by remember { mutableStateOf(false) }
    var listMode by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var bankSel by remember { mutableStateOf<String?>(null) }       // bankKey
    var selectedId by remember { mutableStateOf<String?>(null) }
    var sheetBank by remember { mutableStateOf<RankedBank?>(null) }
    var infoPin by remember { mutableStateOf<MapPin?>(null) }
    var map by remember { mutableStateOf<MapController?>(null) }
    var hasLocation by remember { mutableStateOf(hasLocationPermission()) }
    var didCenter by remember { mutableStateOf(false) }
    var tileFailed by remember { mutableStateOf(false) }

    // First visit to the map is the moment a location makes sense: ask once, here.
    LaunchedEffect(Unit) {
        if (!hasLocation && !appStore().getBoolean("loc_asked", false)) onRequestLocation()
    }
    LaunchedEffect(userLocation) { hasLocation = hasLocationPermission() }

    // ----- ranking: SAME call as the Home tab -----
    val amount = (amountText.toLongOrNull() ?: 0L).toDouble()
    val ranked = remember(payload, direction, amount, userLocation, branches) {
        rankBanks(payload.banks, direction, amount, userLocation, branches)
    }
    val best = remember(ranked, direction) { mapBestRate(ranked, direction) }
    val allPins by produceState(emptyList<MapPin>(), ranked, branches, userLocation, best) {
        value = withContext(Dispatchers.Default) { mapBuildPins(ranked, branches, userLocation, best) }
    }
    val atmPins = remember(atms, userLocation) {
        atms.mapIndexed { i, a ->
            MapPin("a$i", MapPinKind.ATM, a.lat, a.lng, a.name.ifBlank { a.bank }, null, null, bankKey(a.bank),
                userLocation?.let { distanceMeters(it.first, it.second, a.lat, a.lng) / 1000.0 }, null, false, false)
        }
    }
    LaunchedEffect(ranked) {
        val sel = bankSel
        if (sel != null && ranked.none { bankKey(it.rate.bank) == sel }) bankSel = null
    }

    val pricedPins = remember(allPins) { allPins.filter { it.kind == MapPinKind.PRICED } }
    val nearestKm = remember(pricedPins) { pricedPins.mapNotNull { it.rawKm }.minOrNull() }
    val locationBad = userLocation == null || (nearestKm != null && nearestKm > MAP_FAR_KM)

    val visiblePins = remember(allPins, atmPins, openNow, bestOnly, atmOn, bankSel) {
        val sel = bankSel
        buildList {
            allPins.forEach { p ->
                if (openNow && p.closed) return@forEach
                if (sel != null && p.bankKey != sel) return@forEach
                when (p.kind) {
                    MapPinKind.PRICED -> if (!bestOnly || p.good) add(p)
                    MapPinKind.DOT -> if (!bestOnly) add(p)
                    MapPinKind.ATM -> {}
                }
            }
            if (atmOn && !bestOnly) atmPins.forEach { if (sel == null || it.bankKey == sel) add(it) }
        }
    }
    val listItems = remember(ranked, openNow, bestOnly, bankSel, best) {
        ranked.mapIndexedNotNull { i, rb ->
            val good = best != null && abs(rb.perUnit - best) <= MAP_BEST_TOLERANCE
            val ok = !(openNow && mapIsClosed(rb.open)) && (!bestOnly || good) &&
                (bankSel == null || bankKey(rb.rate.bank) == bankSel)
            if (ok) Triple(i + 1, rb, good) else null
        }
    }
    val activeFilters = listOf(openNow, bestOnly, atmOn, listMode, bankSel != null).count { it }

    val onPinTap: (MapPin) -> Unit = { p ->
        selectedId = p.id
        if (p.kind == MapPinKind.PRICED && p.rb != null) { sheetBank = p.rb; infoPin = null } else { infoPin = p; sheetBank = null }
        map?.animateTo(p.lat, p.lng)
    }

    // Default camera: the user's neighbourhood (about 1.5 km around). Without a usable location: the saved city's
    // centre at district level, never the whole city.
    LaunchedEffect(map, userLocation, pricedPins, locationBad) {
        val mv = map ?: return@LaunchedEffect
        if (didCenter || pricedPins.isEmpty()) return@LaunchedEffect
        if (locationBad || userLocation == null) {
            val city = mapCities.firstOrNull { it.uz == RateService.city() } ?: mapCities.first()
            mv.animateTo(city.lat, city.lng, 13.5)
            return@LaunchedEffect
        }
        didCenter = true
        val ds = pricedPins.mapNotNull { it.rawKm }.sorted()
        val r = (ds.getOrNull(2) ?: 1.5).coerceIn(0.8, 2.5)
        val dLat = r / 111.0
        val dLng = r / (111.0 * cos(toRadians(userLocation.first)).coerceAtLeast(0.2))
        mv.fitBounds(userLocation.first + dLat, userLocation.second + dLng, userLocation.first - dLat, userLocation.second - dLng, 48, false)
    }

    val topInset = max(
        innerPadding.calculateTopPadding().value,
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value
    ).dp
    val bottomInset = innerPadding.calculateBottomPadding()

    Column(modifier = Modifier.fillMaxSize().background(VsBg)) {
        // Header: currency + one filter button. Nothing else above the map.
        Row(
            Modifier.fillMaxWidth().background(VsBg).padding(top = topInset, start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencySegmented(currency, onCurrency, Modifier.weight(1f))
            Row(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button) { filters = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Tune, null, tint = VsInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    tr("Filtr", "Фильтр", "Filter") + if (activeFilters > 0) " · $activeFilters" else "",
                    style = VsType.Body, color = VsInk
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            BankMapView(
                modifier = Modifier.fillMaxSize(),
                darkMode = darkMode,
                pins = visiblePins,
                selectedId = selectedId,
                labelMode = MapLabel.BOTH,
                sell = direction == ConvertMode.SELL_USD,
                best = best,
                showMyLocation = hasLocation,
                onPinTap = onPinTap,
                onController = { map = it },
                onTileFailure = { tileFailed = it }
            )

            MapReveal(visible = tileFailed, fromTop = true, modifier = Modifier.align(Alignment.TopCenter)) {
                Text(
                    tr("Xarita yuklanmadi — qayta urinmoqda", "Карта не загрузилась — повторяем попытку", "Map failed to load — retrying"),
                    style = VsType.Caption, color = VsAmber,
                    modifier = Modifier.padding(top = 12.dp).clip(RoundedCornerShape(999.dp)).background(VsBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Locate me (or ask for location when there is none).
            val fabSrc = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottomInset + 16.dp)
                    .size(48.dp).pressScale(fabSrc).shadow(4.dp, CircleShape).clip(CircleShape).background(VsBg)
                    .clickable(interactionSource = fabSrc, indication = androidx.compose.material3.ripple(), role = Role.Button) {
                        val gp = userLocation ?: map?.myLocation()
                        if (gp != null) map?.animateTo(gp.first, gp.second, 15.0) else onRequestLocation()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = tr("Mening joylashuvim", "Моё местоположение", "My location"), tint = VsInk, modifier = Modifier.size(22.dp))
            }

            // List view: same order as Home (rankBanks), row -> bank sheet.
            MapReveal(visible = listMode, fromTop = false) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(VsBg),
                    contentPadding = PaddingValues(bottom = bottomInset + 16.dp)
                ) {
                    if (listItems.isEmpty()) {
                        item {
                            Text(
                                tr("Hech narsa topilmadi", "Ничего не найдено", "Nothing found"),
                                style = VsType.Body, color = VsInkSoft, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                            )
                        }
                    }
                    items(listItems, key = { it.second.rate.bank + it.first }) { (_, rb, good) ->
                        MapListRow(rb, good) { sheetBank = rb; infoPin = null; selectedId = null }
                    }
                }
            }
        }
    }

    if (filters) {
        MapFilterSheet(
            openNow = openNow, onOpenNow = { openNow = it },
            bestOnly = bestOnly, onBestOnly = { bestOnly = it },
            atmOn = atmOn, onAtm = { atmOn = it }, atmsLoading = atmsLoading,
            listMode = listMode, onListMode = { listMode = it },
            banks = ranked.map { it.rate.bank }, bankSel = bankSel, onBank = { bankSel = it },
            showCity = locationBad,
            onCity = { c -> map?.animateTo(c.lat, c.lng, 13.5); RateService.setCity(c.uz) },
            onDismiss = { filters = false }
        )
    }

    sheetBank?.let { rb ->
        BankSheet(
            item = rb, currency = currency, amount = amount, direction = direction,
            updatedAt = payload.updatedAt,
            favorite = favorites.contains(rb.rate.bank),
            onToggleFavorite = { onToggleFavorite(rb.rate.bank) },
            branches = branches, userLocation = userLocation, onRequestLocation = onRequestLocation,
            onDismiss = { sheetBank = null; selectedId = null }
        )
    }
    infoPin?.let { p -> MapInfoSheet(p) { infoPin = null; selectedId = null } }
}

/* ---------- Pieces ---------- */

/** Every map filter in one sheet: toggles as plain rows with a switch, bank and city as lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapFilterSheet(
    openNow: Boolean, onOpenNow: (Boolean) -> Unit,
    bestOnly: Boolean, onBestOnly: (Boolean) -> Unit,
    atmOn: Boolean, onAtm: (Boolean) -> Unit, atmsLoading: Boolean,
    listMode: Boolean, onListMode: (Boolean) -> Unit,
    banks: List<String>, bankSel: String?, onBank: (String?) -> Unit,
    showCity: Boolean, onCity: (MapCity) -> Unit,
    onDismiss: () -> Unit
) {
    var bankMenu by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            FilterSwitch(tr("Hozir ochiq", "Открыто сейчас", "Open now"), openNow, onOpenNow)
            KHairline()
            FilterSwitch(tr("Faqat eng yaxshi kurs", "Только лучший курс", "Best rate only"), bestOnly, onBestOnly)
            KHairline()
            FilterSwitch(tr("Bankomatlar", "Банкоматы", "ATMs") + if (atmOn && atmsLoading) " …" else "", atmOn, onAtm)
            KHairline()
            FilterSwitch(tr("Roʻyxat koʻrinishi", "Списком", "Show as list"), listMode, onListMode)
            KHairline()
            Box {
                FilterValue(tr("Bank", "Банк", "Bank"), bankSel?.let { k -> banks.firstOrNull { bankKey(it) == k } } ?: tr("Hammasi", "Все", "All")) { bankMenu = true }
                DropdownMenu(expanded = bankMenu, onDismissRequest = { bankMenu = false }, containerColor = VsBg, shape = RoundedCornerShape(12.dp)) {
                    DropdownMenuItem(text = { Text(tr("Hammasi", "Все", "All"), style = VsType.Body, color = VsInk) }, onClick = { onBank(null); bankMenu = false })
                    banks.forEach { n ->
                        DropdownMenuItem(
                            text = { Text(n, style = if (bankSel == bankKey(n)) VsType.Title else VsType.Body, color = VsInk) },
                            onClick = { onBank(bankKey(n)); bankMenu = false }
                        )
                    }
                }
            }
            if (showCity) {
                KHairline()
                Box {
                    val city = mapCities.firstOrNull { it.uz == RateService.city() }
                    FilterValue(tr("Shahar", "Город", "City"), city?.let { tr(it.uz, it.ru) } ?: tr("Toshkent", "Ташкент", "Tashkent")) { cityMenu = true }
                    DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }, containerColor = VsBg, shape = RoundedCornerShape(12.dp)) {
                        mapCities.forEach { c ->
                            DropdownMenuItem(text = { Text(tr(c.uz, c.ru), style = VsType.Body, color = VsInk) }, onClick = { onCity(c); cityMenu = false; onDismiss() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = VsType.Body, color = VsInk, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = VsInk, checkedThumbColor = VsBg, uncheckedTrackColor = VsLine, uncheckedThumbColor = VsInkSoft, uncheckedBorderColor = VsLine)
        )
    }
}

@Composable
private fun FilterValue(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = VsType.Body, color = VsInk, modifier = Modifier.weight(1f))
        Text("$value ›", style = VsType.Body, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Fade + short slide for overlays on the map (tile banner from the top, list panel from below). */
@Composable
private fun MapReveal(visible: Boolean, fromTop: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(Motion.MEDIUM, easing = Motion.Decelerate)) +
            slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { if (fromTop) -it else it / 12 },
        exit = fadeOut(tween(Motion.SHORT)) + slideOutVertically(tween(Motion.SHORT)) { if (fromTop) -it else it / 12 }
    ) { content() }
}

/** "1.2 km" / "350 m"; null for unknown or > 100 km (never an exact absurd value). */
@Composable
private fun mapDistanceText(km: Double?): String? {
    if (km == null || km > MAP_FAR_KM) return null
    return if (km < 1.0) "${(km * 1000).toInt()} " + tr("m", "м", "m") else "${(km * 10).toInt() / 10.0} " + tr("km", "км", "km")
}

@Composable
private fun mapOpenText(o: OpenState): String? = when {
    !o.known -> tr("Ish vaqti nomaʻlum", "Время работы неизвестно", "Opening hours unknown")
    o.open -> o.untilText ?: tr("Ochiq", "Открыто", "Open")
    else -> tr("Yopiq", "Закрыто", "Closed")
}

@Composable
private fun MapListRow(rb: RankedBank, good: Boolean, onClick: () -> Unit) {
    val dist = mapDistanceText(rb.distanceKm)
    val open = mapOpenText(rb.open)
    val meta = listOfNotNull(dist, open).joinToString(" · ")
    val desc = "${rb.rate.bank}, ${mapRateText(rb.perUnit)} " + tr("soʻm", "сум", "UZS") +
        (dist?.let { ", $it" } ?: "") + (open?.let { ", $it" } ?: "")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = desc }
                .alpha(if (mapIsClosed(rb.open)) 0.4f else 1f)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rb.rate.bank, style = VsType.Body, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotEmpty()) Text(meta, style = VsType.Caption, color = VsInkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(mapRateText(rb.perUnit), style = if (good) VsType.Title else VsType.Body, color = VsInk)
        }
        KHairline()
    }
}

/** Simple info sheet for pins without a rate (ATMs, branches of banks missing from the rate table). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapInfoSheet(pin: MapPin, onDismiss: () -> Unit) {
    val route = LocalRoute.current
    val dist = mapDistanceText(pin.rawKm)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(pin.title, style = VsType.Title, color = VsInk)
            Text(
                if (pin.kind == MapPinKind.ATM) tr("Bankomat", "Банкомат", "ATMs") else tr("Kurs maʻlumoti yoʻq", "Нет данных о курсе", "No rate data"),
                style = VsType.Meta, color = VsInkSoft
            )
            pin.address?.let { Text(it, style = VsType.Meta, color = VsInkSoft) }
            dist?.let { Text(it, style = VsType.Meta, color = VsInkSoft) }
            pin.hours?.let { Text(it, style = VsType.Meta, color = VsInkSoft) }
            Spacer(Modifier.height(8.dp))
            KButton(tr("Yoʻnalish", "Маршрут", "Directions"), Icons.Outlined.Directions, primary = true, enabled = true, modifier = Modifier.fillMaxWidth()) {
                route(RouteTarget(pin.lat, pin.lng, pin.title))
            }
        }
    }
}

