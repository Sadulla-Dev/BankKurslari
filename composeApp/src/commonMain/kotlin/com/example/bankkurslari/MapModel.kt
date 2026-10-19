package com.example.bankkurslari

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/* ---------- Map model (shared by the Android and iOS map views) ---------- */

const val MAP_BEST_TOLERANCE = 5          // spec 6.2: accent when within 5 so'm of best
const val MAP_FAR_KM = 100.0              // spec 6.2 / acceptance 7

enum class MapPinKind { PRICED, DOT, ATM }

/** What a priced pin shows. Persisted per device. */
enum class MapLabel { RATE, BANK, BOTH }

enum class MapPinStyle { GOOD, NEUTRAL, CLOSED }

class MapPin(
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

class MapCity(val uz: String, val ru: String, val lat: Double, val lng: Double)

val mapCities = listOf(
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

private val bankSuffix = Regex("""(?i)[\s"«»]*(aksiyadorlik\s+)?(tijorat\s+)?(banki|bank)["«»\s]*$""")

// Map labels: the short names people actually use for the long state-bank names.
private val shortNames = listOf(
    Regex("(?i)milliy") to "NBU",
    Regex("(?i)sanoatqurilish|промстрой") to "SQB",
    Regex("(?i)mikrokredit|mkbank") to "MKBank",
    Regex("(?i)orient\\s*finans") to "Orient Finans",
    Regex("(?i)asia\\s*alliance") to "Asia Alliance",
    Regex("(?i)ipak\\s*yo") to "Ipak Yo‘li"
)

/** Short map label: "Oʻzbekiston Milliy banki" -> "NBU", "Asia Alliance Bank" -> "Asia Alliance", "Hamkorbank" -> "Hamkorbank". */
fun shortBankName(name: String): String {
    shortNames.firstOrNull { it.first.containsMatchIn(name) }?.let { return it.second }
    val t = name.trim().replace(bankSuffix, "").trim().ifEmpty { name.trim() }
    return if (t.length > 14) t.take(13).trimEnd() + "…" else t
}

fun mapRateText(v: Int): String = v.toLong().formatUzsL()

fun mapIsClosed(o: OpenState) = o.known && !o.open

/** Which side of the market the user cares about: sell -> highest rate is best, buy -> lowest. */
fun mapBestRate(ranked: List<RankedBank>, direction: ConvertMode): Int? =
    if (direction == ConvertMode.SELL_USD) ranked.maxOfOrNull { it.perUnit } else ranked.minOfOrNull { it.perUnit }

/**
 * Specialises the per-bank rankBanks() result to individual OSM branches (coordinates, hours, distance of THAT branch).
 * Ordering of the list / pins is still defined by rankBanks (the `ranked` list itself); this only adds geometry.
 */
fun mapBuildPins(
    ranked: List<RankedBank>,
    branches: List<OsmBranch>,
    user: Pair<Double, Double>?,
    best: Int?
): List<MapPin> {
    val byKey = HashMap<String, RankedBank>()
    ranked.forEach { if (bankKey(it.rate.bank) !in byKey) byKey[bankKey(it.rate.bank)] = it }
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

/** Haversine distance in metres. */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon / 2).let { it * it }
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/* ---------- markers: what to draw where (clustering is shared, projection is the platform's) ---------- */

enum class MarkerKind { PILL, DOT, ATM }

/**
 * One thing to put on the map. [pin] set -> tapping selects it; [cluster] set -> tapping zooms to its bounds.
 * [anchorBottom] = the image's bottom-centre is the coordinate (pills with a tip); otherwise its centre.
 */
class MarkerSpec(
    val kind: MarkerKind,
    val lat: Double,
    val lng: Double,
    val text: String = "",
    val sub: String? = null,
    val style: MapPinStyle = MapPinStyle.NEUTRAL,
    val selected: Boolean = false,
    val tip: Boolean = false,
    val closed: Boolean = false,
    val pin: MapPin? = null,
    val cluster: List<MapPin>? = null
) {
    val anchorBottom: Boolean get() = tip
    /** Pins hidden behind this one (cluster badge), 0 for a single pin. */
    val more: Int get() = (cluster?.size ?: 1) - 1
    val imageKey: String get() = "$kind|$text|$sub|$style|$selected|$tip|$closed|$more"
}

private fun styleOf(p: MapPin) = if (p.closed) MapPinStyle.CLOSED else if (p.good) MapPinStyle.GOOD else MapPinStyle.NEUTRAL

private fun pinSpec(p: MapPin, selected: Boolean, labelMode: MapLabel): MarkerSpec = when (p.kind) {
    MapPinKind.PRICED -> {
        val rate = mapRateText(p.rb!!.perUnit)
        when (labelMode) {
            MapLabel.RATE -> MarkerSpec(MarkerKind.PILL, p.lat, p.lng, rate, null, styleOf(p), selected, true, pin = p)
            MapLabel.BANK -> MarkerSpec(MarkerKind.PILL, p.lat, p.lng, shortBankName(p.title), null, styleOf(p), selected, true, pin = p)
            MapLabel.BOTH -> MarkerSpec(MarkerKind.PILL, p.lat, p.lng, shortBankName(p.title), rate, styleOf(p), selected, true, pin = p)
        }
    }
    MapPinKind.DOT -> MarkerSpec(MarkerKind.DOT, p.lat, p.lng, closed = p.closed, pin = p)
    MapPinKind.ATM -> MarkerSpec(MarkerKind.ATM, p.lat, p.lng, pin = p)
}

/** Below this zoom each bank is one pin (its nearest / best branch); from here on every branch shows. */
const val MAP_BRANCH_ZOOM = 14.0

/**
 * Markers for the pins currently in view, back-to-front. Zoomed out, each bank collapses to one pin (nearest branch),
 * so the map shows which bank is where instead of fifteen identical labels. Pins are then greedily placed in screen
 * space, best-priced first; a pin whose label would overlap one already placed is left out (zooming in brings it back).
 * No count badges. [project] maps a coordinate to screen pixels; [density] converts the label box from dp.
 */
fun buildMarkerSpecs(
    inView: List<MapPin>,
    selectedId: String?,
    labelMode: MapLabel,
    sell: Boolean,
    best: Int?,
    zoom: Double,
    density: Float,
    project: (Double, Double) -> Pair<Float, Float>
): List<MarkerSpec> {
    val out = ArrayList<MarkerSpec>()
    val selPin = selectedId?.let { id -> inView.firstOrNull { it.id == id } }
    var priced = inView.filter { it.kind == MapPinKind.PRICED && it.id != selectedId }
    if (zoom < MAP_BRANCH_ZOOM) {
        priced = priced.groupBy { it.bankKey }.values.map { g -> g.minByOrNull { it.rawKm ?: Double.MAX_VALUE } ?: g.first() }
            .filter { selPin == null || it.bankKey != selPin.bankKey }
    }
    val extras = inView.filter { it.kind != MapPinKind.PRICED && it.id != selectedId }
    // Rate-less branches only when zoomed in close; ATMs (opt-in) from city zoom.
    extras.filter { if (it.kind == MapPinKind.ATM) zoom >= 12.0 else zoom >= 15.0 }.take(300)
        .forEach { out += pinSpec(it, false, labelMode) }

    val boxW = (if (labelMode == MapLabel.BOTH) 150 else 96) * density
    val boxH = 40 * density
    val ordered = priced.sortedWith(
        compareByDescending<MapPin> { it.good && !it.closed }
            .thenBy { it.closed }
            .thenBy { if (sell) -it.rb!!.perUnit else it.rb!!.perUnit }
    )
    val placed = ArrayList<Pair<Float, Float>>()
    val kept = ArrayList<MapPin>()
    selPin?.let { placed += project(it.lat, it.lng) }
    for (p in ordered) {
        if (kept.size >= MAX_LABELS) break
        val (px, py) = project(p.lat, p.lng)
        if (placed.any { abs(it.first - px) < boxW && abs(it.second - py) < boxH }) continue
        placed += px to py
        kept += p
    }
    // Lowest priority first so the best labels end up on top.
    kept.asReversed().forEach { out += pinSpec(it, false, labelMode) }
    selPin?.let { out += pinSpec(it, true, labelMode) }
    return out
}

private const val MAX_LABELS = 60

/**
 * Topmost marker under [x],[y] (screen px). [project] maps a coordinate to screen px; [sizeOf] gives a marker's image
 * size in px. Hit boxes are grown to at least [minTouch] px so small pins stay easy to tap.
 */
fun hitTestMarkers(
    specs: List<MarkerSpec>,
    x: Float,
    y: Float,
    minTouch: Float,
    project: (Double, Double) -> Pair<Float, Float>?,
    sizeOf: (MarkerSpec) -> Pair<Float, Float>
): MarkerSpec? {
    // specs are back-to-front; test front-most first.
    for (spec in specs.asReversed()) {
        val (px, py) = project(spec.lat, spec.lng) ?: continue
        val (w, h) = sizeOf(spec)
        val cy = if (spec.anchorBottom) py - h / 2f else py
        val hw = max(w, minTouch) / 2f
        val hh = max(h, minTouch) / 2f
        if (abs(x - px) <= hw && abs(y - cy) <= hh) return spec
    }
    return null
}

/**
 * What tapping a cluster does: zoom in while that can still split it, otherwise open its best pin.
 * Branches in one building never split (same coordinates), and past street level a further zoom only looks broken.
 */
fun clusterTapTarget(group: List<MapPin>, zoom: Double): MapPin? {
    val sameSpot = group.maxOf { it.lat } - group.minOf { it.lat } < 1e-4 && group.maxOf { it.lng } - group.minOf { it.lng } < 1e-4
    return if (sameSpot || zoom >= 16.5) group.first() else null
}

/* ---------- marker images (one renderer for both platforms) ---------- */

/** Colours for marker images; taken from the theme at composition time. */
class PinColors(val accent: Color, val onAccent: Color, val ink: Color, val inkSoft: Color, val line: Color, val bg: Color)

/**
 * Draws marker images with Compose: a one-line capsule ("Anor  12 025") with a small tip, a count badge for clusters,
 * dots for ATMs / rate-less branches. Cached by [MarkerSpec.imageKey].
 *
 * [touchPadding]: pad images with transparent pixels to a 48 dp hit area (MapKit hit-tests the image itself).
 * Android hit-tests in code ([hitTestMarkers]) and keeps images tight.
 */
class PinRenderer(
    private val measurer: TextMeasurer,
    private val density: Density,
    private val c: PinColors,
    private val touchPadding: Boolean = true
) {
    private val cache = HashMap<String, ImageBitmap>()

    fun image(spec: MarkerSpec): ImageBitmap {
        cache[spec.imageKey]?.let { return it }
        if (cache.size > 600) cache.clear()
        val img = when (spec.kind) {
            MarkerKind.PILL -> pill(spec)
            MarkerKind.DOT -> dot(atm = false, closed = spec.closed)
            MarkerKind.ATM -> dot(atm = true, closed = false)
        }
        cache[spec.imageKey] = img
        return img
    }

    private fun draw(w: Int, h: Int, block: DrawScope.() -> Unit): ImageBitmap {
        val img = ImageBitmap(w, h)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(img), Size(w.toFloat(), h.toFloat()), block)
        return img
    }

    private fun pad(size: Float): Float = if (touchPadding) max(size, 48 * density.density) else size

    private fun pill(s: MarkerSpec): ImageBitmap {
        val d = density.density
        // Best = accent fill; selected = ink fill; others = white; closed = the same white pin at 40 %.
        val closedA = if (s.style == MapPinStyle.CLOSED && !s.selected) 0.4f else 1f
        val fill = when {
            s.selected -> c.ink
            s.style == MapPinStyle.GOOD -> c.accent
            else -> c.bg
        }.let { it.copy(alpha = it.alpha * (0.6f + 0.4f * closedA)) }
        val strong = when {
            s.selected -> c.bg
            s.style == MapPinStyle.GOOD -> c.onAccent
            else -> c.ink
        }.let { it.copy(alpha = it.alpha * closedA) }
        val softColor = if (s.style == MapPinStyle.GOOD || s.selected) strong.copy(alpha = 0.8f * strong.alpha) else c.inkSoft.copy(alpha = c.inkSoft.alpha * closedA)

        val big = s.selected
        // Two parts on one line: label (bank, regular) + value (rate, semibold). Single-part pins use the strong style.
        val labelText = if (s.sub != null) s.text else null
        val valueText = s.sub ?: s.text
        val valueStyle = TextStyle(
            fontSize = (if (big) 15 else 13).sp, fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum", color = strong
        )
        val labelStyle = TextStyle(fontSize = (if (big) 13 else 11).sp, fontWeight = FontWeight.Medium, color = softColor)
        val label = labelText?.let { measurer.measure(it, labelStyle) }
        val value = measurer.measure(valueText, valueStyle)

        val padX = (if (big) 12 else 10) * d
        val gap = 5 * d
        val pillH = (if (big) 34 else 28) * d
        val pillW = padX * 2 + value.size.width + (label?.let { it.size.width + gap } ?: 0f)
        val tipH = if (s.tip) 5 * d else 0f
        val shadow = 3 * d
        // Badge: hidden-pin count in a small ink circle overlapping the top-right corner.
        val badge = if (s.more > 0) measurer.measure(
            if (s.more > 99) "99+" else "+${s.more}",
            TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c.bg, fontFeatureSettings = "tnum")
        ) else null
        val badgeH = 16 * d
        val badgeW = badge?.let { max(badgeH, it.size.width + 8 * d) } ?: 0f
        val overhang = if (badge != null) badgeW / 2f else 0f
        val contentW = pillW + 2 * overhang + 2 * shadow
        val contentH = pillH + tipH + (if (badge != null) badgeH / 2f else 0f) + 2 * shadow
        val bw = pad(contentW).toInt() + 1
        val bh = pad(contentH).toInt() + 1

        return draw(bw, bh) {
            val left = (bw - pillW) / 2f
            // Bottom of the tip sits on the image's bottom edge (minus shadow room) so the anchor is exact.
            val bottom = (if (s.tip) bh - shadow - tipH else (bh + pillH) / 2f)
            val top = bottom - pillH
            val r = CornerRadius(pillH / 2f)
            // soft shadow: three widening translucent layers
            for (i in 3 downTo 1) {
                val g = i * d
                drawRoundRect(
                    Color.Black.copy(alpha = 0.035f), Offset(left - g / 2, top - g / 2 + 1.5f * d),
                    Size(pillW + g, pillH + g), CornerRadius(pillH / 2f + g / 2)
                )
            }
            if (s.tip) {
                val path = Path().apply {
                    moveTo(bw / 2f - 5 * d, bottom - 1 * d)
                    lineTo(bw / 2f, bottom + tipH)
                    lineTo(bw / 2f + 5 * d, bottom - 1 * d)
                    close()
                }
                drawPath(path, fill)
            }
            drawRoundRect(fill, Offset(left, top), Size(pillW, pillH), r)
            if ((s.style == MapPinStyle.NEUTRAL || s.style == MapPinStyle.CLOSED) && !s.selected) {
                drawRoundRect(c.line, Offset(left, top), Size(pillW, pillH), r, style = Stroke(1 * d))
            }
            var x = left + padX
            if (label != null) {
                drawText(label, topLeft = Offset(x, (top + bottom) / 2f - label.size.height / 2f))
                x += label.size.width + gap
            }
            drawText(value, topLeft = Offset(x, (top + bottom) / 2f - value.size.height / 2f))
            if (badge != null) {
                val bl = left + pillW - badgeW / 2f
                val bt = top - badgeH / 2f
                drawRoundRect(c.bg, Offset(bl - 1.5f * d, bt - 1.5f * d), Size(badgeW + 3 * d, badgeH + 3 * d), CornerRadius(badgeH))
                // Neutral ink badge: green is reserved for "good rate".
                drawRoundRect(if (s.selected) c.accent else c.ink, Offset(bl, bt), Size(badgeW, badgeH), CornerRadius(badgeH / 2f))
                drawText(badge, topLeft = Offset(bl + badgeW / 2f - badge.size.width / 2f, bt + badgeH / 2f - badge.size.height / 2f))
            }
        }
    }

    /** Small neutral marker: filled dot = branch without a rate, ring = ATM. */
    private fun dot(atm: Boolean, closed: Boolean): ImageBitmap {
        val d = density.density
        val size = pad(16 * d).toInt()
        return draw(size, size) {
            val cx = Offset(size / 2f, size / 2f)
            if (atm) {
                drawCircle(c.bg, 6 * d, cx)
                drawCircle(c.inkSoft, 6 * d, cx, style = Stroke(2 * d))
            } else {
                drawCircle(c.bg, 6.5f * d, cx)
                drawCircle(if (closed) c.line else c.inkSoft, 5 * d, cx)
            }
        }
    }

    /** The user's position: accent dot with a light ring. */
    fun locationDot(): ImageBitmap {
        val d = density.density
        val size = (26 * d).toInt()
        return draw(size, size) {
            val cx = Offset(size / 2f, size / 2f)
            drawCircle(c.accent.copy(alpha = 0.2f), size / 2f, cx)
            drawCircle(c.bg, 8 * d, cx)
            drawCircle(c.accent, 6 * d, cx)
        }
    }
}

@Composable
fun rememberPinRenderer(touchPadding: Boolean = true): PinRenderer {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val density = LocalDensity.current
    val colors = PinColors(VsEmerald, VsBg, VsInk, VsInkSoft, VsLine, VsBg)
    return remember(measurer, density, colors.accent, colors.ink, colors.bg, touchPadding) {
        PinRenderer(measurer, density, colors, touchPadding)
    }
}

/* ---------- platform map view ---------- */

/** Camera commands the shared screen sends to the platform map. */
interface MapController {
    fun animateTo(lat: Double, lng: Double, zoom: Double? = null)
    fun fitBounds(north: Double, east: Double, south: Double, west: Double, paddingDp: Int, animate: Boolean)
    /** The map's own idea of the user's position (may be fresher than [lastKnownLocation]). */
    fun myLocation(): Pair<Double, Double>?
}

/**
 * The map itself: MapLibre on Android, MapKit on iOS. Gets the filtered pins; does viewport culling, projection and
 * drawing (via [buildMarkerSpecs] + [PinRenderer]). Cluster taps zoom in; pin taps go to [onPinTap].
 */
@Composable
expect fun BankMapView(
    modifier: Modifier,
    darkMode: Boolean,
    pins: List<MapPin>,
    selectedId: String?,
    labelMode: MapLabel,
    sell: Boolean,
    best: Int?,
    showMyLocation: Boolean,
    onPinTap: (MapPin) -> Unit,
    onController: (MapController) -> Unit,
    onTileFailure: (Boolean) -> Unit
)
