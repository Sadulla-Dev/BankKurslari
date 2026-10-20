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
import com.example.bankkurslari.ui.theme.VsGround
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Tune
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
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsTile
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
    userLocation: Pair<Double, Double>?
) {

    var openNow by remember { mutableStateOf(false) }
    var bestOnly by remember { mutableStateOf(false) }
    var atmOn by remember { mutableStateOf(false) }
    var listMode by remember { mutableStateOf(false) }
    var labelMode by remember {
        // Every visit starts with bank name + rate; the switch only changes the current visit.
        mutableStateOf(MapLabel.BOTH)
    }
    var bankSel by remember { mutableStateOf<String?>(null) }       // bankKey
    var bankMenu by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf(false) }
    var cityName by remember { mutableStateOf<MapCity?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var sheetBank by remember { mutableStateOf<RankedBank?>(null) }
    var infoPin by remember { mutableStateOf<MapPin?>(null) }
    var map by remember { mutableStateOf<MapController?>(null) }
    var hasLocation by remember { mutableStateOf(hasLocationPermission()) }
    var didCenter by remember { mutableStateOf(false) }
    var tileFailed by remember { mutableStateOf(false) }

    val requestLocation = rememberLocationPermissionRequest { granted -> hasLocation = granted }
    LaunchedEffect(Unit) { if (!hasLocation) requestLocation() }

    // ----- ranking: SAME call as the Kurs tab (acceptance 6) -----
    val amount = (amountText.toLongOrNull() ?: 0L).toDouble()
    val ranked = remember(payload, direction, amount, userLocation, branches) {
        rankBanks(payload.banks, direction, amount, userLocation, branches)
    }
    val best = remember(ranked, direction) { mapBestRate(ranked, direction) }
    // Thousands of branches: build pins off the main thread.
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

    // ----- location sanity (spec 6.2: never present absurd distances) -----
    val pricedPins = remember(allPins) { allPins.filter { it.kind == MapPinKind.PRICED } }
    val nearestKm = remember(pricedPins) { pricedPins.mapNotNull { it.rawKm }.minOrNull() }
    val locationBad = userLocation == null || (nearestKm != null && nearestKm > MAP_FAR_KM)

    // ----- filters -----
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
            if (atmOn && !bestOnly) atmPins.forEach { if (bankSel == null || it.bankKey == bankSel) add(it) }
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

    val onPinTap: (MapPin) -> Unit = { p ->
        selectedId = p.id
        if (p.kind == MapPinKind.PRICED && p.rb != null) { sheetBank = p.rb; infoPin = null } else { infoPin = p; sheetBank = null }
        map?.animateTo(p.lat, p.lng)
    }

    // Default camera (spec 6.2): centred on the user with the radius of the 5th nearest priced pin (>= 5 pins in view).
    // Skipped entirely when the location is unusable (null, or nearest priced branch > 100 km) -> banner + city picker.
    // Keyed on the pin list itself: pins are built in the background and may first arrive without distances
    // (location not known yet). Centre only once real distances exist.
    LaunchedEffect(map, userLocation, pricedPins) {
        val mv = map ?: return@LaunchedEffect
        if (didCenter || userLocation == null || pricedPins.isEmpty() || locationBad) return@LaunchedEffect
        val ds = pricedPins.mapNotNull { it.rawKm }.sorted()
        if (ds.isEmpty()) return@LaunchedEffect
        didCenter = true
        val r = (ds.getOrNull(4) ?: ds.last()).coerceIn(0.5, MAP_FAR_KM) * 1.15
        val dLat = r / 111.0
        val dLng = r / (111.0 * cos(toRadians(userLocation.first)).coerceAtLeast(0.2))
        mv.fitBounds(userLocation.first + dLat, userLocation.second + dLng, userLocation.first - dLat, userLocation.second - dLng, 48, false)
    }

    val topInset = max(
        innerPadding.calculateTopPadding().value,
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value
    ).dp
    val bottomInset = innerPadding.calculateBottomPadding()
    val bankNames = remember(ranked) { ranked.map { it.rate.bank } }
    val shownCity = cityName ?: userLocation?.takeIf { !locationBad }?.let { u ->
        mapCities.minByOrNull { distanceMeters(u.first, u.second, it.lat, it.lng) }
            ?.takeIf { distanceMeters(u.first, u.second, it.lat, it.lng) < 60_000.0 }
    }

    // Peek card: the tapped bank, else the nearest best-priced branch. Hidden while the list is open.
    val peekPin = remember(selectedId, pricedPins, bankSel, openNow) {
        pricedPins.firstOrNull { it.id == selectedId }
            ?: pricedPins.filter { it.good && !it.closed && (bankSel == null || it.bankKey == bankSel) && !(openNow && it.closed) }
                .minByOrNull { it.rawKm ?: Double.MAX_VALUE }
    }
    val route = LocalRoute.current
    var filtersOpen by remember { mutableStateOf(false) }
    val showPeek = peekPin?.rb != null && !listMode
    val peekGap = if (showPeek) 112.dp else 16.dp

    Box(modifier = Modifier.fillMaxSize().background(VsBg)) {
      Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomInset).clipToBounds()) {
        BankMapView(
            modifier = Modifier.fillMaxSize(),
            darkMode = darkMode,
            pins = visiblePins,
            selectedId = selectedId,
            labelMode = labelMode,
            sell = direction == ConvertMode.SELL_USD,
            best = best,
            showMyLocation = hasLocation,
            onPinTap = onPinTap,
            onController = { map = it },
            onTileFailure = { tileFailed = it }
        )

        // ---------- Accessible equivalent of the pins: same order as the Kurs tab (rankBanks), row -> bank sheet ----------
        MapReveal(visible = listMode, fromTop = false) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(VsBg),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset + 68.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                items(listItems, key = { it.second.rate.bank + it.first }) { (rank, rb, good) ->
                    MapListRow(rank, rb, good) {
                        sheetBank = rb; infoPin = null
                        selectedId = null
                    }
                }
            }
        }

        // ---------- Floating header: currency capsule + filters ----------
        Column(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(top = topInset + 8.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MapCapsule(Currency.entries.map { it.code }, currency.ordinal) { onCurrency(Currency.entries[it]) }
                VsIconButton(
                    Icons.Outlined.Tune, tr("Filtrlar", "Фильтры", "Filters"),
                    onClick = { filtersOpen = !filtersOpen }, floating = true,
                    tint = if (filtersOpen || openNow || bestOnly || bankSel != null || atmOn || listMode) VsEmerald else VsInk
                )
            }
            // City pill (replaces the wide "location not found" banner): the city around you, or the one you picked.
            Box(Modifier.align(Alignment.Start)) {
                MapCityPill(
                    label = shownCity?.let { tr(it.uz, it.ru) } ?: tr("Shahar tanlang", "Выберите город", "Choose a city"),
                    onClick = { cityMenu = true }
                )
                DropdownMenu(
                    expanded = cityMenu, onDismissRequest = { cityMenu = false },
                    containerColor = VsCard, shape = VsShape.Stat
                ) {
                    mapCities.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(tr(c.uz, c.ru), style = VsType.Body, color = if (shownCity === c) VsEmerald else VsInk) },
                            onClick = {
                                cityName = c; cityMenu = false
                                map?.animateTo(c.lat, c.lng, 12.0)
                            }
                        )
                    }
                }
            }
            MapReveal(visible = filtersOpen, fromTop = true) {
                Column(
                    Modifier.fillMaxWidth().vsFloating(VsShape.Panel).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        Segmented(
                            options = listOf(tr("Kurs", "Курс", "Rate"), tr("Bank", "Банк", "Bank"), tr("Ikkalasi", "Оба", "Both")),
                            selected = labelMode.ordinal,
                            onSelect = { labelMode = MapLabel.entries[it] }
                        )
                    }
                    val chipScroll = rememberScrollState()
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(chipScroll).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapChip(tr("Ochiq hozir", "Открыто сейчас", "Open now"), openNow, { openNow = !openNow })
                        MapChip(tr("Eng yaxshi", "Лучшие", "Best"), bestOnly, { bestOnly = !bestOnly })
                        Box {
                            MapChip(
                                label = bankSel?.let { k -> bankNames.firstOrNull { bankKey(it) == k } } ?: tr("Bank", "Банк", "Bank"),
                                selected = bankSel != null,
                                onClick = { bankMenu = true },
                                trailing = Icons.Outlined.ExpandMore
                            )
                            DropdownMenu(
                                expanded = bankMenu, onDismissRequest = { bankMenu = false },
                                containerColor = VsCard, shape = VsShape.Stat
                            ) {
                                DropdownMenuItem(
                                    text = { Text(tr("Barcha banklar", "Все банки", "All banks"), style = VsType.Body, color = if (bankSel == null) VsEmerald else VsInk) },
                                    onClick = { bankSel = null; bankMenu = false }
                                )
                                bankNames.forEach { n ->
                                    val active = bankSel == bankKey(n)
                                    DropdownMenuItem(
                                        text = { Text(n, style = VsType.Body, color = if (active) VsEmerald else VsInk) },
                                        onClick = { bankSel = bankKey(n); bankMenu = false }
                                    )
                                }
                            }
                        }
                        MapChip(tr("Bankomat", "Банкомат", "ATMs"), atmOn, { atmOn = !atmOn }, loading = atmOn && atmsLoading)
                        MapChip(tr("Roʻyxat", "Список", "List"), listMode, { listMode = !listMode })
                    }
                    val legendDesc = tr("narx yaxshi, boshqa, yopiq", "цена хорошая, другие, закрыто", "good rate, other, closed")
                    Row(
                        Modifier.padding(horizontal = 16.dp).heightIn(min = 32.dp).semantics { contentDescription = legendDesc },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendSwatch(fill = VsEmerald, stroke = VsEmerald)
                        Text(tr("yaxshi narx", "лучшая цена", "best rate"), style = VsType.Meta, color = VsInk, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        LegendSwatch(fill = VsCard, stroke = VsHair)
                        Text(tr("boshqa", "другие", "other"), style = VsType.Meta, color = VsInk, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        LegendSwatch(fill = VsTile, stroke = VsHair, dashed = true)
                        Text(tr("yopiq", "закрыто", "closed"), style = VsType.Meta, color = VsInkSoft, maxLines = 1)
                    }
                }
            }
            MapReveal(visible = tileFailed, fromTop = true) {
                Text(
                    tr("Xarita yuklanmadi — qayta urinmoqda", "Карта не загрузилась — повторяем попытку", "Map failed to load — retrying"),
                    style = VsType.Meta, color = VsInk,
                    modifier = Modifier.vsFloating(VsShape.Pill).padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // ---------- Locate button: floating circle above the peek card ----------
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = peekGap),
            verticalAlignment = Alignment.Bottom
        ) {
            val fabSrc = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier.size(48.dp).pressScale(fabSrc).vsFloating(CircleShape)
                    .clickable(interactionSource = fabSrc, indication = null, role = Role.Button) {
                        val gp = userLocation ?: map?.myLocation()
                        if (gp != null) {
                            map?.animateTo(gp.first, gp.second, 15.0)
                        } else if (!hasLocation) {
                            requestLocation()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = tr("Mening joylashuvim", "Моё местоположение", "My location"), tint = VsInk, modifier = Modifier.size(20.dp))
            }
        }

        // ---------- Peek card: act without leaving the map ----------
        MapReveal(visible = showPeek, fromTop = false, modifier = Modifier.align(Alignment.BottomCenter)) {
            val pin = peekPin
            val rb = pin?.rb
            if (pin != null && rb != null) {
                val dist = mapDistanceText(pin.rawKm)
                val unit = tr("soʻm", "сум", "so'm")
                val amtText = kGroup(amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L)
                val sub = if (pin.good) {
                    tr("$amtText ${currency.code} uchun eng yaxshi", "Лучший курс для $amtText ${currency.code}", "Best for $amtText ${currency.code}")
                } else {
                    "${mapRateText(rb.perUnit)} $unit"
                }
                val nBranches = pricedPins.count { it.bankKey == pin.bankKey }
                val branchesText = if (nBranches > 1) pick(lang, "$nBranches filial", "$nBranches филиала", "$nBranches branches") else null
                val line = listOfNotNull(sub, branchesText ?: dist).joinToString(", ")
                val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .pressScale(src).vsFloating(VsShape.Panel)
                        .clickable(interactionSource = src, indication = null, role = Role.Button) { sheetBank = rb; infoPin = null }
                        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BankTile(mapInitials(pin.title), best = pin.good)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pin.title, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(line, style = VsType.Meta, color = VsInkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    val goSrc = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Box(
                        Modifier.heightIn(min = 44.dp).pressScale(goSrc).clip(VsShape.Pill).background(VsEmerald)
                            .clickable(interactionSource = goSrc, indication = null, role = Role.Button) {
                                route(RouteTarget(pin.lat, pin.lng, pin.title))
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tr("Yoʻnalish", "Маршрут", "Go"), style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), color = VsOnAccent)
                    }
                }
            }
        }
      }
    }

    sheetBank?.let { rb ->
        BankSheet(
            item = rb,
            currency = currency,
            amount = amount,
            direction = direction,
            updatedAt = payload.updatedAt,
            favorite = favorites.contains(rb.rate.bank),
            onToggleFavorite = { onToggleFavorite(rb.rate.bank) },
            onDismiss = { sheetBank = null; selectedId = null },
            best = ranked.firstOrNull()
        )
    }
    infoPin?.let { p -> MapInfoSheet(p) { infoPin = null; selectedId = null } }
}

/* ---------- Pieces ---------- */

@Composable
private fun MapChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: ImageVector? = null,
    loading: Boolean = false
) = VsChip(
    text = label, selected = selected, onClick = onClick, trailing = trailing,
    trailingContent = if (loading) { { CircularProgressIndicator(color = VsEmerald, strokeWidth = 2.dp, modifier = Modifier.size(14.dp)) } } else null
)

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

@Composable
private fun LegendSwatch(fill: androidx.compose.ui.graphics.Color, stroke: androidx.compose.ui.graphics.Color, dashed: Boolean = false) {
    androidx.compose.foundation.Canvas(Modifier.size(width = 16.dp, height = 10.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        drawRoundRect(fill, cornerRadius = r)
        drawRoundRect(
            stroke, cornerRadius = r,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.dp.toPx(),
                pathEffect = if (dashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())) else null
            )
        )
    }
}

/** "1.2 km" / "350 m"; null for unknown or > 100 km (never an exact absurd value). */
@Composable
private fun mapDistanceText(km: Double?): String? {
    if (km == null || km > MAP_FAR_KM) return null
    return if (km < 1.0) "${(km * 1000).toInt()} " + tr("m", "м", "m") else "${(km * 10).toInt() / 10.0} " + tr("km", "км", "km")
}

@Composable
private fun mapOpenText(o: OpenState): String? = when {
    !o.known -> tr("Ish vaqti nomaʼlum", "Время работы неизвестно", "Opening hours unknown")
    o.open -> o.untilText ?: tr("Ochiq", "Открыто", "Open")
    else -> tr("Yopiq", "Закрыто", "Closed")
}

@Composable
private fun MapListRow(rank: Int, rb: RankedBank, good: Boolean, onClick: () -> Unit) {
    val dist = mapDistanceText(rb.distanceKm)
    val open = mapOpenText(rb.open)
    val meta = listOfNotNull(dist, open).joinToString(" · ")
    val unit = tr("soʻm", "сум", "so'm")
    val desc = tr("$rank-oʻrin", "место $rank", "#$rank") + ", ${rb.rate.bank}, ${mapRateText(rb.perUnit)} $unit" +
        (dist?.let { ", $it" } ?: "") + (open?.let { ", $it" } ?: "")
    Row(
        modifier = Modifier.fillMaxWidth().vsCard(VsShape.Panel)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BankTile(mapInitials(rb.rate.bank), best = good, size = 36.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(rb.rate.bank, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.isNotEmpty()) Text(meta, style = VsType.Meta, color = VsInkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(mapRateText(rb.perUnit), style = VsType.Value, color = if (good) VsEmerald else VsInk)
            Text(unit, style = VsType.Meta, color = VsInkSoft)
        }
    }
}

/** Two-letter tile mark: "Orient Finans Bank" -> "OF", "NBU" -> "NB". */
private fun mapInitials(name: String): String {
    val words = name.trim().replace(Regex("(?i)\\s*(aksiyadorlik|tijorat|banki|bank)\\b"), "").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        words.size == 1 -> words[0].take(2)
        else -> name.take(2)
    }.uppercase()
}

/** Floating currency capsule: white pill, the selected cell is a ground thumb with a hairline shadow. */
@Composable
private fun MapCapsule(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.vsFloating(VsShape.Pill).padding(3.dp).semantics { contentDescription = "Currency" },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val bg by androidx.compose.animation.animateColorAsState(if (sel) VsGround else androidx.compose.ui.graphics.Color.Transparent, Motion.color(), label = "capbg")
            val fg by androidx.compose.animation.animateColorAsState(if (sel) VsInk else VsInkSoft, Motion.color(), label = "capfg")
            Box(
                Modifier.heightIn(min = 40.dp).clip(VsShape.Pill).background(bg)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null, role = Role.Tab
                    ) { onSelect(i) }
                    .semantics { this.selected = sel }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), color = fg, maxLines = 1)
            }
        }
    }
}

/** Floating city pill: pin icon + city name + chevron. */
@Composable
private fun MapCityPill(label: String, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier.heightIn(min = 44.dp).pressScale(src).vsFloating(VsShape.Pill)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = VsInk, modifier = Modifier.size(18.dp))
        Text(label, style = VsType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)), color = VsInk, maxLines = 1)
        Icon(Icons.Outlined.ExpandMore, null, tint = VsInkSoft, modifier = Modifier.size(18.dp))
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
        containerColor = VsCard,
        shape = VsShape.Tray
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(pin.title, style = VsType.Title, color = VsInk)
            Text(
                if (pin.kind == MapPinKind.ATM) tr("Bankomat", "Банкомат", "ATMs") else tr("Kurs maʼlumoti yoʻq", "Нет данных о курсе", "No rate data"),
                style = VsType.Meta, color = VsInkSoft
            )
            pin.address?.let { Text(it, style = VsType.Meta, color = VsInkSoft) }
            dist?.let { Text(it, style = VsType.Meta, color = VsInkSoft) }
            pin.hours?.let { Text(it, style = VsType.Meta, color = VsInkSoft) }
            Spacer(Modifier.height(8.dp))
            VsPrimaryButton(
                tr("Yoʻnalish", "Маршрут", "Directions"),
                onClick = { route(RouteTarget(pin.lat, pin.lng, pin.title)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

