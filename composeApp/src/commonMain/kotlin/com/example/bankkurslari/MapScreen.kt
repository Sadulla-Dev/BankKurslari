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
    var legendOpen by remember { mutableStateOf(true) }
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

    Column(modifier = Modifier.fillMaxSize().background(VsBg)) {
        // ---------- Header: below the status bar, never over the map ----------
        Column(
            modifier = Modifier.fillMaxWidth().background(VsBg)
                .padding(top = topInset + 8.dp, bottom = 8.dp)
        ) {
            CurrencySegmented(
                currency, onCurrency,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 48.dp)
            )
            val chipScroll = rememberScrollState()
            Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(chipScroll).padding(horizontal = 16.dp).heightIn(min = 52.dp),
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
                        containerColor = VsBg, shape = RoundedCornerShape(12.dp)
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
            // Fade on the trailing edge while more chips are off-screen: tells the row scrolls.
            val fadeA by androidx.compose.animation.core.animateFloatAsState(
                if (chipScroll.value < chipScroll.maxValue) 1f else 0f, tween(Motion.SHORT), label = "chipfade"
            )
            Box(
                Modifier.align(Alignment.CenterEnd).width(40.dp).height(52.dp).alpha(fadeA)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(VsBg.copy(alpha = 0f), VsBg)))
            )
            }
            if (locationBad) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("Joylashuv aniqlanmadi", "Местоположение не определено", "Location not found"),
                        style = VsType.Meta, color = VsAmber, modifier = Modifier.weight(1f)
                    )
                    Box {
                        MapChip(
                            label = cityName?.let { tr(it.uz, it.ru) } ?: tr("Shahar tanlang", "Выберите город", "Choose a city"),
                            selected = cityName != null,
                            onClick = { cityMenu = true },
                            trailing = Icons.Outlined.ExpandMore
                        )
                        DropdownMenu(
                            expanded = cityMenu, onDismissRequest = { cityMenu = false },
                            containerColor = VsBg, shape = RoundedCornerShape(12.dp)
                        ) {
                            mapCities.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(tr(c.uz, c.ru), style = VsType.Body, color = if (cityName === c) VsEmerald else VsInk) },
                                    onClick = {
                                        cityName = c; cityMenu = false
                                        map?.animateTo(c.lat, c.lng, 12.0)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------- Map / list ----------
        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
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

            // Pin label switch: what each pin shows.
            Box(
                Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(VsBg)
                    .border(1.dp, VsLine, RoundedCornerShape(12.dp))
            ) {
                Segmented(
                    options = listOf(tr("Kurs", "Курс", "Rate"), tr("Bank", "Банк", "Bank"), tr("Ikkalasi", "Оба", "Both")),
                    selected = labelMode.ordinal,
                    onSelect = {
                        labelMode = MapLabel.entries[it]
                    },
                    cellWidth = 76.dp,
                    height = 40.dp
                )
            }

            MapReveal(visible = tileFailed, fromTop = true, modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp)) {
                Text(
                    tr("Xarita yuklanmadi — qayta urinmoqda", "Карта не загрузилась — повторяем попытку", "Map failed to load — retrying"),
                    style = VsType.Meta, color = VsInk,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(999.dp)).background(VsBg)
                        .border(1.dp, VsLine, RoundedCornerShape(999.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Legend (one line, collapsible) + the single FAB.
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomInset + 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val legendText = tr("narx yaxshi, boshqa, yopiq", "цена хорошая, другие, закрыто", "good rate, other, closed")
                Box(
                    modifier = Modifier.heightIn(min = 48.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null, role = Role.Button
                        ) { legendOpen = !legendOpen }
                        .semantics { contentDescription = legendText },
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        legendOpen,
                        transitionSpec = {
                            (fadeIn(tween(Motion.MEDIUM, easing = Motion.Decelerate)) + expandHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate), expandFrom = Alignment.Start)) togetherWith
                                (fadeOut(tween(Motion.SHORT)) + shrinkHorizontally(tween(Motion.MEDIUM), shrinkTowards = Alignment.Start))
                        },
                        label = "legend"
                    ) { open ->
                        if (open) {
                            Row(
                                Modifier.clip(RoundedCornerShape(999.dp)).background(VsBg)
                                    .border(1.dp, VsLine, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LegendSwatch(fill = VsEmerald, stroke = VsEmerald)
                                Text(tr("yaxshi narx", "лучшая цена", "best rate"), style = VsType.Meta, color = VsInk, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                LegendSwatch(fill = VsSurface, stroke = VsInkSoft)
                                Text(tr("boshqa", "другие", "other"), style = VsType.Meta, color = VsInk, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                LegendSwatch(fill = VsLine, stroke = VsInkSoft, dashed = true)
                                Text(tr("yopiq", "закрыто", "closed"), style = VsType.Meta, color = VsInkSoft, maxLines = 1)
                            }
                        } else {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(VsBg).border(1.dp, VsLine, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Outlined.Info, null, tint = VsInkSoft, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Locate-me: neutral surface (accent is reserved for "best" / primary action).
                val fabSrc = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Box(
                    modifier = Modifier.size(52.dp).pressScale(fabSrc).clip(RoundedCornerShape(14.dp)).background(VsBg)
                        .border(1.dp, VsLine, RoundedCornerShape(14.dp))
                        .clickable(interactionSource = fabSrc, indication = androidx.compose.material3.ripple(), role = Role.Button) {
                            val gp = userLocation ?: map?.myLocation()
                            if (gp != null) {
                                map?.animateTo(gp.first, gp.second, 15.0)
                            } else if (!hasLocation) {
                                requestLocation()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = tr("Mening joylashuvim", "Моё местоположение", "My location"), tint = VsInk, modifier = Modifier.size(22.dp))
                }
            }

            // Accessible equivalent of the pins: same order as the Kurs tab (rankBanks), row -> bank sheet.
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
                    items(listItems, key = { it.second.rate.bank + it.first }) { (rank, rb, good) ->
                        MapListRow(rank, rb, good) {
                            sheetBank = rb; infoPin = null
                            selectedId = null
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
            onDismiss = { sheetBank = null; selectedId = null }
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
    !o.known -> tr("Ish vaqti nomaʻlum", "Время работы неизвестно", "Opening hours unknown")
    o.open -> o.untilText ?: tr("Ochiq", "Открыто", "Open")
    else -> tr("Yopiq", "Закрыто", "Closed")
}

@Composable
private fun MapListRow(rank: Int, rb: RankedBank, good: Boolean, onClick: () -> Unit) {
    val dist = mapDistanceText(rb.distanceKm)
    val open = mapOpenText(rb.open)
    val meta = listOfNotNull(dist, open).joinToString(" · ")
    val unit = tr("soʻm", "сум", "UZS")
    val desc = tr("$rank-oʻrin", "место $rank", "#$rank") + ", ${rb.rate.bank}, ${mapRateText(rb.perUnit)} $unit" +
        (dist?.let { ", $it" } ?: "") + (open?.let { ", $it" } ?: "")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = desc }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("$rank", style = VsType.Meta, color = VsInkSoft, modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rb.rate.bank, style = VsType.BodyMedium, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotEmpty()) Text(meta, style = VsType.Meta, color = VsInkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(mapRateText(rb.perUnit), style = VsType.BodyMedium, color = if (good) VsEmerald else VsInk)
                Text(unit, style = VsType.Meta, color = VsInkSoft)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(VsLine))
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
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
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
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(VsEmerald)
                    .clickable(role = Role.Button) {
                        route(RouteTarget(pin.lat, pin.lng, pin.title))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(tr("Yoʻnalish", "Маршрут", "Directions"), style = VsType.BodyMedium, color = VsBg)
            }
        }
    }
}

