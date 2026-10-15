package com.example.bankkurslari

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

internal data class MapPlace(
    val name: String,
    val address: String?,
    val hours: String?,
    val isAtm: Boolean,
    val lat: Double,
    val lng: Double
)

internal const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"

internal fun hasLocationPermission(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

/* ---------- Map model ---------- */

private const val MAP_BEST_TOLERANCE = 5          // spec 6.2: accent when within 5 so'm of best
private const val MAP_FAR_KM = 100.0              // spec 6.2 / acceptance 7

private enum class MapPinKind { PRICED, DOT, ATM }

/** What a priced pin shows. Persisted per device. */
private enum class MapLabel { RATE, BANK, BOTH }

private val bankSuffix = Regex("""(?i)[\s"«»]*(aksiyadorlik\s+)?(tijorat\s+)?(banki|bank)["«»\s]*$""")

/** "Asia Alliance Bank" -> "Asia Alliance", "Oʻzbekiston Milliy banki" -> "Oʻzbekiston Milliy"; single-word names kept. */
private fun shortBankName(name: String): String {
    val t = name.trim().replace(bankSuffix, "").trim().ifEmpty { name.trim() }
    return if (t.length > 18) t.take(17).trimEnd() + "…" else t
}

private class MapPin(
    val id: String,
    val kind: MapPinKind,
    val lat: Double,
    val lng: Double,
    val title: String,
    val address: String?,
    val hours: String?,
    val bankKey: String,
    val rawKm: Double?,        // true distance from the user (only used for camera decisions, never displayed)
    val rb: RankedBank?,
    val good: Boolean,
    val closed: Boolean
)

private class MapCity(val uz: String, val ru: String, val lat: Double, val lng: Double)

private val mapCities = listOf(
    MapCity("Toshkent", "Ташкент", 41.311, 69.279),
    MapCity("Samarqand", "Самарканд", 39.654, 66.959),
    MapCity("Buxoro", "Бухара", 39.768, 64.421),
    MapCity("Andijon", "Андижан", 40.783, 72.344),
    MapCity("Namangan", "Наманган", 40.998, 71.672),
    MapCity("Fargʻona", "Фергана", 40.384, 71.784),
    MapCity("Qarshi", "Карши", 38.861, 65.789),
    MapCity("Nukus", "Нукус", 42.460, 59.603),
    MapCity("Urganch", "Ургенч", 41.550, 60.631),
    MapCity("Navoiy", "Навои", 40.084, 65.379),
    MapCity("Jizzax", "Джизак", 40.115, 67.842),
    MapCity("Termiz", "Термез", 37.224, 67.278),
    MapCity("Guliston", "Гулистан", 40.489, 68.784)
)

private fun mapRateText(v: Int): String = v.toLong().formatUzsL()

private fun mapIsClosed(o: OpenState) = o.known && !o.open

/** Which side of the market the user cares about: sell -> highest rate is best, buy -> lowest. */
private fun mapBestRate(ranked: List<RankedBank>, direction: ConvertMode): Int? =
    if (direction == ConvertMode.SELL_USD) ranked.maxOfOrNull { it.perUnit } else ranked.minOfOrNull { it.perUnit }

/**
 * Specialises the per-bank rankBanks() result to individual OSM branches (coordinates, hours, distance of THAT branch).
 * Ordering of the list / pins is still defined by rankBanks (the `ranked` list itself); this only adds geometry.
 */
private fun mapBuildPins(
    ranked: List<RankedBank>,
    branches: List<OsmBranch>,
    user: Pair<Double, Double>?,
    best: Int?
): List<MapPin> {
    val byKey = HashMap<String, RankedBank>()
    ranked.forEach { byKey.putIfAbsent(bankKey(it.rate.bank), it) }
    val withBranch = HashSet<String>()
    val out = ArrayList<MapPin>(branches.size + 16)
    fun km(lat: Double, lng: Double) = user?.let { distanceMeters(it.first, it.second, lat, lng) / 1000.0 }
    branches.forEachIndexed { i, br ->
        val key = bankKey(br.bank)
        val base = byKey[key]
        val raw = km(br.lat, br.lng)
        if (base != null) {
            withBranch.add(key)
            val hours = br.hours ?: base.hours
            val st = openState(hours)
            val rb = base.copy(
                distanceKm = raw?.takeIf { it <= MAP_FAR_KM },
                address = br.address ?: base.address,
                hours = hours,
                open = st,
                lat = br.lat,
                lng = br.lng
            )
            out.add(
                MapPin("p$i", MapPinKind.PRICED, br.lat, br.lng, base.rate.bank, rb.address, hours, key, raw, rb,
                    best != null && abs(base.perUnit - best) <= MAP_BEST_TOLERANCE, mapIsClosed(st))
            )
        } else {
            val st = openState(br.hours)
            out.add(
                MapPin("d$i", MapPinKind.DOT, br.lat, br.lng, br.bank.ifBlank { br.name }, br.address, br.hours, key, raw,
                    null, false, mapIsClosed(st))
            )
        }
    }
    // Banks that have a rate but no OSM branch: fall back to the coordinate the rate source gave us.
    ranked.forEach { r ->
        val key = bankKey(r.rate.bank)
        val la = r.lat
        val ln = r.lng
        if (key !in withBranch && la != null && ln != null) {
            val raw = km(la, ln)
            val rb = r.copy(distanceKm = raw?.takeIf { it <= MAP_FAR_KM })
            out.add(
                MapPin("r$key", MapPinKind.PRICED, la, ln, r.rate.bank, r.address, r.hours, key, raw, rb,
                    best != null && abs(r.perUnit - best) <= MAP_BEST_TOLERANCE, mapIsClosed(r.open))
            )
        }
    }
    return out
}

/* ---------- Pin bitmaps (custom drawn, cached) ---------- */

private class MapPalette(
    val accent: Int, val onAccent: Int, val surface: Int, val ink: Int, val inkSoft: Int, val line: Int, val bg: Int
)

private enum class MapPinStyle { GOOD, NEUTRAL, CLOSED }

private class MapIcons(private val ctx: android.content.Context, private val p: MapPalette) {
    private val d = ctx.resources.displayMetrics.density
    private val cache = HashMap<String, Drawable>()

    /**
     * Rounded label; optional second line ([sub]) under it (bank name over rate).
     * Padded with transparent pixels to a 48 dp hit area (marker hit-test uses the bitmap bounds).
     */
    fun pill(text: String, style: MapPinStyle, selected: Boolean, tip: Boolean, sub: String? = null): Drawable {
        val key = "$text|$sub|$style|$selected|$tip"
        cache[key]?.let { return it }
        if (cache.size > 600) cache.clear()
        val two = sub != null
        val main = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (if (two) (if (selected) 13f else 11f) else (if (selected) 16f else 13f)) * d
            typeface = Typeface.create(if (two) "sans-serif" else "sans-serif-medium", Typeface.NORMAL)
            fontFeatureSettings = "tnum"
        }
        val second = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (if (selected) 15f else 13f) * d
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            fontFeatureSettings = "tnum"
        }
        val padX = 10 * d
        val gap = 1 * d
        val lineMain = main.descent() - main.ascent()
        val lineSub = if (two) second.descent() - second.ascent() else 0f
        val pillH = if (two) lineMain + lineSub + gap + 10 * d else (if (selected) 34f else 28f) * d
        val textW = max(main.measureText(text), if (two) second.measureText(sub) else 0f)
        val pillW = textW + 2 * padX
        val strokeW = (if (selected) 2.5f else 1f) * d
        val tipH = if (tip) 6 * d else 0f
        val touch = 48 * d
        val bw = max(pillW + 2 * strokeW, touch).toInt()
        val bh = max(pillH + tipH + 2 * strokeW, touch).toInt()
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val left = (bw - pillW) / 2f
        val bottom = if (tip) bh - tipH - strokeW else (bh + pillH) / 2f
        val top = bottom - pillH
        val fill: Int; val stroke: Int; val textColor: Int; val softColor: Int
        when (style) {
            MapPinStyle.GOOD -> { fill = p.accent; stroke = p.accent; textColor = p.onAccent; softColor = p.onAccent }
            MapPinStyle.NEUTRAL -> { fill = p.bg; stroke = p.inkSoft; textColor = p.ink; softColor = p.inkSoft }
            MapPinStyle.CLOSED -> { fill = p.line; stroke = p.inkSoft; textColor = p.inkSoft; softColor = p.inkSoft }
        }
        val shape = Paint(Paint.ANTI_ALIAS_FLAG)
        if (tip) {
            val path = android.graphics.Path().apply {
                moveTo(bw / 2f - 5 * d, bottom - 1 * d)
                lineTo(bw / 2f, bottom + tipH)
                lineTo(bw / 2f + 5 * d, bottom - 1 * d)
                close()
            }
            shape.style = Paint.Style.FILL; shape.color = if (selected) p.ink else stroke
            c.drawPath(path, shape)
        }
        val r = (if (two) 10 else 12) * d
        shape.style = Paint.Style.FILL; shape.color = fill
        c.drawRoundRect(left, top, left + pillW, bottom, r, r, shape)
        shape.style = Paint.Style.STROKE
        shape.strokeWidth = strokeW
        shape.color = if (selected) p.ink else stroke
        if (style == MapPinStyle.CLOSED) shape.pathEffect = DashPathEffect(floatArrayOf(4 * d, 3 * d), 0f)
        c.drawRoundRect(left, top, left + pillW, bottom, r, r, shape)
        main.textAlign = Paint.Align.CENTER
        second.textAlign = Paint.Align.CENTER
        if (two) {
            val y1 = top + 5 * d - main.ascent()
            main.color = softColor
            c.drawText(text, bw / 2f, y1, main)
            second.color = textColor
            c.drawText(sub!!, bw / 2f, y1 + main.descent() + gap - second.ascent(), second)
        } else {
            main.color = textColor
            val ty = (top + bottom) / 2f - (main.descent() + main.ascent()) / 2f
            c.drawText(text, bw / 2f, ty, main)
        }
        return BitmapDrawable(ctx.resources, bmp).also { cache[key] = it }
    }

    /** Small neutral marker: filled dot = branch without a rate, ring = ATM. */
    fun dot(atm: Boolean, closed: Boolean): Drawable {
        val key = "dot|$atm|$closed"
        cache[key]?.let { return it }
        val size = (48 * d).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = size / 2f
        if (atm) {
            paint.style = Paint.Style.FILL; paint.color = p.bg
            c.drawCircle(cx, cx, 6 * d, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * d; paint.color = p.inkSoft
            c.drawCircle(cx, cx, 6 * d, paint)
        } else {
            paint.style = Paint.Style.FILL; paint.color = p.bg
            c.drawCircle(cx, cx, 6.5f * d, paint)
            paint.color = if (closed) p.line else p.inkSoft
            c.drawCircle(cx, cx, 5 * d, paint)
        }
        return BitmapDrawable(ctx.resources, bmp).also { cache[key] = it }
    }
}

private fun mapMarker(map: MapView, lat: Double, lng: Double, icon: Drawable, tipBottom: Boolean, onTap: () -> Unit): Marker =
    Marker(map).apply {
        position = GeoPoint(lat, lng)
        setAnchor(Marker.ANCHOR_CENTER, if (tipBottom) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER)
        this.icon = icon
        setInfoWindow(null)
        setOnMarkerClickListener { _, _ -> onTap(); true }
    }

/* ---------- Screen ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapScreen(
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
    val context = LocalContext.current
    val s = LocalStrings.current

    var openNow by remember { mutableStateOf(false) }
    var bestOnly by remember { mutableStateOf(false) }
    var atmOn by remember { mutableStateOf(false) }
    var listMode by remember { mutableStateOf(false) }
    var legendOpen by remember { mutableStateOf(true) }
    val labelPrefs = remember { context.getSharedPreferences("valyutastat", android.content.Context.MODE_PRIVATE) }
    var labelMode by remember {
        mutableStateOf(MapLabel.entries.firstOrNull { it.name == labelPrefs.getString("map_label", null) } ?: MapLabel.RATE)
    }
    var bankSel by remember { mutableStateOf<String?>(null) }       // bankKey
    var bankMenu by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf(false) }
    var cityName by remember { mutableStateOf<MapCity?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var sheetBank by remember { mutableStateOf<RankedBank?>(null) }
    var infoPin by remember { mutableStateOf<MapPin?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasLocation by remember { mutableStateOf(hasLocationPermission(context)) }
    var locOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var didCenter by remember { mutableStateOf(false) }
    var tileFailed by remember { mutableStateOf(false) }
    // Only render markers inside the current viewport (dataset is thousands of points).
    var bbox by remember { mutableStateOf<BoundingBox?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocation = granted
    }
    LaunchedEffect(Unit) { if (!hasLocation) permLauncher.launch(FINE_LOCATION) }

    // ----- ranking: SAME call as the Kurs tab (acceptance 6) -----
    val amount = (amountText.toLongOrNull() ?: 0L).toDouble()
    val ranked = remember(payload, direction, amount, userLocation, branches) {
        rankBanks(payload.banks, direction, amount, userLocation, branches)
    }
    val best = remember(ranked, direction) { mapBestRate(ranked, direction) }
    val allPins = remember(ranked, branches, userLocation, best) { mapBuildPins(ranked, branches, userLocation, best) }
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

    // ----- icons -----
    val palette = MapPalette(
        VsEmerald.toArgb(), VsBg.toArgb(), VsSurface.toArgb(), VsInk.toArgb(), VsInkSoft.toArgb(), VsLine.toArgb(), VsBg.toArgb()
    )
    val icons = remember(palette.accent, palette.surface, palette.ink, palette.inkSoft, palette.line, palette.bg) { MapIcons(context, palette) }
    val surfaceArgb = palette.surface
    val lineArgb = palette.line

    val onPinTap: (MapPin) -> Unit = { p ->
        selectedId = p.id
        if (p.kind == MapPinKind.PRICED && p.rb != null) { sheetBank = p.rb; infoPin = null } else { infoPin = p; sheetBank = null }
        mapView?.controller?.animateTo(GeoPoint(p.lat, p.lng))
    }

    // my-location overlay
    LaunchedEffect(mapView, hasLocation) {
        val mv = mapView
        if (mv != null && hasLocation && locOverlay == null) {
            val px = (26 * context.resources.displayMetrics.density).toInt()
            val dot = ContextCompat.getDrawable(context, R.drawable.ic_location_dot)!!.toBitmap(px, px)
            locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mv).apply {
                setPersonIcon(dot)
                setDirectionIcon(dot)
                setPersonAnchor(0.5f, 0.5f)
                setDirectionAnchor(0.5f, 0.5f)
                enableMyLocation()
            }
            mv.invalidate()
        }
    }

    // Default camera (spec 6.2): centred on the user with the radius of the 5th nearest priced pin (>= 5 pins in view).
    // Skipped entirely when the location is unusable (null, or nearest priced branch > 100 km) -> banner + city picker.
    LaunchedEffect(mapView, userLocation, pricedPins.size) {
        val mv = mapView ?: return@LaunchedEffect
        if (didCenter || userLocation == null || pricedPins.isEmpty() || locationBad) return@LaunchedEffect
        didCenter = true
        val ds = pricedPins.mapNotNull { it.rawKm }.sorted()
        val r = (ds.getOrNull(4) ?: ds.last()).coerceIn(0.5, MAP_FAR_KM) * 1.15
        val dLat = r / 111.0
        val dLng = r / (111.0 * cos(Math.toRadians(userLocation.first)).coerceAtLeast(0.2))
        val box = BoundingBox(userLocation.first + dLat, userLocation.second + dLng, userLocation.first - dLat, userLocation.second - dLng)
        mv.zoomToBoundingBox(box, false, (48 * context.resources.displayMetrics.density).toInt())
    }

    // Tile failures: retry by re-invalidating (failed tiles are not cached, so the next draw requests them again).
    LaunchedEffect(tileFailed, mapView) {
        while (tileFailed) {
            delay(6000)
            mapView?.invalidate()
        }
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
                MapChip(tr("Ochiq hozir", "Открыто сейчас"), openNow, { openNow = !openNow })
                MapChip(tr("Eng yaxshi", "Лучшие"), bestOnly, { bestOnly = !bestOnly })
                Box {
                    MapChip(
                        label = bankSel?.let { k -> bankNames.firstOrNull { bankKey(it) == k } } ?: tr("Bank", "Банк"),
                        selected = bankSel != null,
                        onClick = { bankMenu = true },
                        trailing = Icons.Outlined.ExpandMore
                    )
                    DropdownMenu(
                        expanded = bankMenu, onDismissRequest = { bankMenu = false },
                        containerColor = VsBg, shape = RoundedCornerShape(12.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(tr("Barcha banklar", "Все банки"), style = VsType.Body, color = if (bankSel == null) VsEmerald else VsInk) },
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
                MapChip(tr("Bankomat", "Банкомат"), atmOn, { atmOn = !atmOn }, loading = atmOn && atmsLoading)
                MapChip(tr("Roʻyxat", "Список"), listMode, { listMode = !listMode })
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
                        tr("Joylashuv aniqlanmadi", "Местоположение не определено"),
                        style = VsType.Meta, color = VsAmber, modifier = Modifier.weight(1f)
                    )
                    Box {
                        MapChip(
                            label = cityName?.let { tr(it.uz, it.ru) } ?: tr("Shahar tanlang", "Выберите город"),
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
                                        mapView?.controller?.apply { setZoom(12.0); animateTo(GeoPoint(c.lat, c.lng)) }
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
                        tag = darkMode
                        setMultiTouchControls(true)
                        minZoomLevel = 4.0
                        maxZoomLevel = esriMaxZoom(darkMode).toDouble()
                        controller.setZoom(11.0)
                        controller.setCenter(GeoPoint(41.311, 69.279))
                        // Flat surface-coloured placeholder instead of grey blocks while tiles load / fail.
                        overlayManager.tilesOverlay.loadingBackgroundColor = surfaceArgb
                        overlayManager.tilesOverlay.loadingLineColor = lineArgb
                        // Tile failure signal: osmdroid posts MAPTILE_FAIL_ID / MAPTILE_SUCCESS_ID to these handlers.
                        var fails = 0
                        tileProvider.tileRequestCompleteHandlers.add(Handler(Looper.getMainLooper()) { msg ->
                            when (msg.what) {
                                MapTileProviderBase.MAPTILE_FAIL_ID -> { fails++; if (fails >= 3) tileFailed = true }
                                MapTileProviderBase.MAPTILE_SUCCESS_ID -> { fails = 0; if (tileFailed) tileFailed = false }
                            }
                            true
                        })
                        addMapListener(DelayedMapListener(object : MapListener {
                            override fun onScroll(e: ScrollEvent?): Boolean { bbox = boundingBox; return true }
                            override fun onZoom(e: ZoomEvent?): Boolean { bbox = boundingBox; return true }
                        }, 140))
                        post { bbox = boundingBox }
                        mapView = this
                    }
                },
                update = { map ->
                    if (map.tag != darkMode) {
                        map.setTileSource(esriTiles(darkMode))
                        map.maxZoomLevel = esriMaxZoom(darkMode).toDouble()
                        map.tag = darkMode
                    }
                    map.overlayManager.tilesOverlay.loadingBackgroundColor = surfaceArgb
                    map.overlayManager.tilesOverlay.loadingLineColor = lineArgb
                    map.overlays.clear()
                    val b = bbox
                    val inView = if (b == null) emptyList() else visiblePins.filter { b.contains(it.lat, it.lng) }
                    val selPin = selectedId?.let { id -> visiblePins.firstOrNull { it.id == id } }
                    fun styleOf(p: MapPin) = if (p.closed) MapPinStyle.CLOSED else if (p.good) MapPinStyle.GOOD else MapPinStyle.NEUTRAL
                    fun addPin(p: MapPin, selected: Boolean) {
                        val icon = when (p.kind) {
                            MapPinKind.PRICED -> {
                                val rate = mapRateText(p.rb!!.perUnit)
                                when (labelMode) {
                                    MapLabel.RATE -> icons.pill(rate, styleOf(p), selected, true)
                                    MapLabel.BANK -> icons.pill(shortBankName(p.title), styleOf(p), selected, true)
                                    MapLabel.BOTH -> icons.pill(shortBankName(p.title), styleOf(p), selected, true, sub = rate)
                                }
                            }
                            MapPinKind.DOT -> icons.dot(false, p.closed)
                            MapPinKind.ATM -> icons.dot(true, false)
                        }
                        map.overlays.add(mapMarker(map, p.lat, p.lng, icon, p.kind == MapPinKind.PRICED) { onPinTap(p) })
                    }
                    // Neutral/closed first so best-priced pins draw on top.
                    val priced = inView.filter { it.kind == MapPinKind.PRICED && it.id != selectedId }
                        .sortedBy { if (it.good && !it.closed) 1 else 0 }
                    val extras = inView.filter { it.kind != MapPinKind.PRICED && it.id != selectedId }
                    val zoom = map.zoomLevelDouble
                    // Rate-less branches only when zoomed in close; ATMs (opt-in chip) from city zoom.
                    extras.filter { if (it.kind == MapPinKind.ATM) zoom >= 12.0 else zoom >= 15.0 }.take(300).forEach { addPin(it, false) }
                    // Screen-space greedy clustering: labels never overlap. Best-priced pins claim space first,
                    // a cluster shows the BEST rate inside + count ("11 905 · 40").
                    val dens = map.context.resources.displayMetrics.density
                    val boxW = (if (labelMode == MapLabel.RATE) 92 else 150) * dens
                    val boxH = (if (labelMode == MapLabel.BOTH) 54 else 38) * dens
                    val proj = map.projection
                    val ordered = priced.sortedWith(
                        compareByDescending<MapPin> { it.good && !it.closed }
                            .thenBy { if (direction == ConvertMode.SELL_USD) -it.rb!!.perUnit else it.rb!!.perUnit }
                    )
                    class Group(val x: Float, val y: Float, val pins: MutableList<MapPin>)
                    val groups = ArrayList<Group>()
                    ordered.forEach { p ->
                        val pt = proj.toPixels(GeoPoint(p.lat, p.lng), null)
                        val g = groups.firstOrNull { abs(it.x - pt.x) < boxW && abs(it.y - pt.y) < boxH }
                        if (g != null) g.pins.add(p) else groups.add(Group(pt.x.toFloat(), pt.y.toFloat(), mutableListOf(p)))
                    }
                    // Draw lowest priority first so the best labels end up on top.
                    groups.asReversed().forEach { g ->
                        val group = g.pins
                        val lead = group[0]
                        if (group.size == 1) addPin(lead, false) else {
                            val rates = group.map { it.rb!!.perUnit }
                            val groupBest = if (direction == ConvertMode.SELL_USD) rates.max() else rates.min()
                            val style = when {
                                group.all { it.closed } -> MapPinStyle.CLOSED
                                best != null && abs(groupBest - best) <= MAP_BEST_TOLERANCE -> MapPinStyle.GOOD
                                else -> MapPinStyle.NEUTRAL
                            }
                            // Lead = best-priced pin of the group (ordering above), so its name matches the shown rate.
                            val more = "+${group.size - 1}"
                            val icon = when (labelMode) {
                                MapLabel.RATE -> icons.pill("${mapRateText(groupBest)} · ${group.size}", style, false, false)
                                MapLabel.BANK -> icons.pill("${shortBankName(lead.title)} $more", style, false, false)
                                MapLabel.BOTH -> icons.pill("${shortBankName(lead.title)} $more", style, false, false, sub = mapRateText(groupBest))
                            }
                            map.overlays.add(mapMarker(map, lead.lat, lead.lng, icon, false) {
                                val n = group.maxOf { it.lat }; val sLat = group.minOf { it.lat }
                                val e = group.maxOf { it.lng }; val w = group.minOf { it.lng }
                                if (n - sLat < 1e-4 && e - w < 1e-4) map.controller.animateTo(GeoPoint(lead.lat, lead.lng), zoom + 2.0, 500L)
                                else map.zoomToBoundingBox(BoundingBox(n, e, sLat, w), true, (64 * dens).toInt())
                            })
                        }
                    }
                    selPin?.let { addPin(it, true) }
                    locOverlay?.let { map.overlays.add(it) }
                    map.invalidate()
                },
                onRelease = { it.onDetach() }
            )

            // Pin label switch: what each pin shows.
            Box(
                Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(VsBg)
                    .border(1.dp, VsLine, RoundedCornerShape(12.dp))
            ) {
                Segmented(
                    options = listOf(tr("Kurs", "Курс"), tr("Bank", "Банк"), tr("Ikkalasi", "Оба")),
                    selected = labelMode.ordinal,
                    onSelect = {
                        labelMode = MapLabel.entries[it]
                        labelPrefs.edit().putString("map_label", labelMode.name).apply()
                    },
                    cellWidth = 76.dp,
                    height = 40.dp
                )
            }

            MapReveal(visible = tileFailed, fromTop = true, modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp)) {
                Text(
                    tr("Xarita yuklanmadi — qayta urinmoqda", "Карта не загрузилась — повторяем попытку"),
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
                val legendText = tr("narx yaxshi, boshqa, yopiq", "цена хорошая, другие, закрыто")
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
                                Text(tr("yaxshi narx", "лучшая цена"), style = VsType.Meta, color = VsInk, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                LegendSwatch(fill = VsSurface, stroke = VsInkSoft)
                                Text(tr("boshqa", "другие"), style = VsType.Meta, color = VsInk, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                LegendSwatch(fill = VsLine, stroke = VsInkSoft, dashed = true)
                                Text(tr("yopiq", "закрыто"), style = VsType.Meta, color = VsInkSoft, maxLines = 1)
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
                            val gp = userLocation?.let { GeoPoint(it.first, it.second) } ?: locOverlay?.myLocation
                            if (gp != null) {
                                mapView?.controller?.animateTo(gp, 15.0, 600L)
                            } else if (!hasLocation) {
                                permLauncher.launch(FINE_LOCATION)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = s.myLocation, tint = VsInk, modifier = Modifier.size(22.dp))
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
                                tr("Hech narsa topilmadi", "Ничего не найдено"),
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
    return if (km < 1.0) "${(km * 1000).toInt()} " + tr("m", "м") else "${(km * 10).toInt() / 10.0} " + tr("km", "км")
}

@Composable
private fun mapOpenText(o: OpenState): String? = when {
    !o.known -> tr("Ish vaqti nomaʻlum", "Время работы неизвестно")
    o.open -> o.untilText ?: tr("Ochiq", "Открыто")
    else -> tr("Yopiq", "Закрыто")
}

@Composable
private fun MapListRow(rank: Int, rb: RankedBank, good: Boolean, onClick: () -> Unit) {
    val dist = mapDistanceText(rb.distanceKm)
    val open = mapOpenText(rb.open)
    val meta = listOfNotNull(dist, open).joinToString(" · ")
    val unit = tr("soʻm", "сум")
    val desc = tr("$rank-oʻrin", "место $rank") + ", ${rb.rate.bank}, ${mapRateText(rb.perUnit)} $unit" +
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
    val s = LocalStrings.current
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
                if (pin.kind == MapPinKind.ATM) tr("Bankomat", "Банкомат") else tr("Kurs maʻlumoti yoʻq", "Нет данных о курсе"),
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
                Text(tr("Yoʻnalish", "Маршрут"), style = VsType.BodyMedium, color = VsBg)
            }
        }
    }
}

/* ---------- Helpers kept from the previous implementation ---------- */

// Max native zoom per Esri service (zooming past this shows "map data not yet available").
internal fun esriMaxZoom(dark: Boolean) = if (dark) 16 else 19

// Esri ArcGIS Online tiles: no API key, no watermark. Tile path order is z/y/x.
internal fun esriTiles(dark: Boolean): XYTileSource {
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

/** Haversine distance in metres. */
internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}

internal fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else "${(meters / 100).toInt() / 10.0} km"

