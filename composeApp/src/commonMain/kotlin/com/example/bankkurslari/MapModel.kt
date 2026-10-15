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

/** "Asia Alliance Bank" -> "Asia Alliance", "Oʻzbekiston Milliy banki" -> "Oʻzbekiston Milliy"; single-word names kept. */
fun shortBankName(name: String): String {
    val t = name.trim().replace(bankSuffix, "").trim().ifEmpty { name.trim() }
    return if (t.length > 18) t.take(17).trimEnd() + "…" else t
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
    val imageKey: String get() = "$kind|$text|$sub|$style|$selected|$tip|$closed"
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

/**
 * Markers for the pins currently in view, back-to-front. Priced pins are greedily clustered in screen space so labels
 * never overlap; best-priced pins claim space first and a cluster shows the BEST rate inside + count ("11 905 · 40").
 * [project] maps a coordinate to screen pixels; [density] converts the label box from dp.
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
    val priced = inView.filter { it.kind == MapPinKind.PRICED && it.id != selectedId }
    val extras = inView.filter { it.kind != MapPinKind.PRICED && it.id != selectedId }
    // Rate-less branches only when zoomed in close; ATMs (opt-in chip) from city zoom.
    extras.filter { if (it.kind == MapPinKind.ATM) zoom >= 12.0 else zoom >= 15.0 }.take(300)
        .forEach { out += pinSpec(it, false, labelMode) }

    val boxW = (if (labelMode == MapLabel.RATE) 92 else 150) * density
    val boxH = (if (labelMode == MapLabel.BOTH) 54 else 38) * density
    val ordered = priced.sortedWith(
        compareByDescending<MapPin> { it.good && !it.closed }
            .thenBy { if (sell) -it.rb!!.perUnit else it.rb!!.perUnit }
    )
    class Group(val x: Float, val y: Float, val pins: MutableList<MapPin>)
    val groups = ArrayList<Group>()
    ordered.forEach { p ->
        val (px, py) = project(p.lat, p.lng)
        val g = groups.firstOrNull { abs(it.x - px) < boxW && abs(it.y - py) < boxH }
        if (g != null) g.pins.add(p) else groups.add(Group(px, py, mutableListOf(p)))
    }
    // Lowest priority first so the best labels end up on top.
    groups.asReversed().forEach { g ->
        val group = g.pins
        val lead = group[0]
        if (group.size == 1) out += pinSpec(lead, false, labelMode) else {
            val rates = group.map { it.rb!!.perUnit }
            val groupBest = if (sell) rates.max() else rates.min()
            val style = when {
                group.all { it.closed } -> MapPinStyle.CLOSED
                best != null && abs(groupBest - best) <= MAP_BEST_TOLERANCE -> MapPinStyle.GOOD
                else -> MapPinStyle.NEUTRAL
            }
            // Lead = best-priced pin of the group (ordering above), so its name matches the shown rate.
            val more = "+${group.size - 1}"
            out += when (labelMode) {
                MapLabel.RATE -> MarkerSpec(MarkerKind.PILL, lead.lat, lead.lng, "${mapRateText(groupBest)} · ${group.size}", null, style, cluster = group)
                MapLabel.BANK -> MarkerSpec(MarkerKind.PILL, lead.lat, lead.lng, "${shortBankName(lead.title)} $more", null, style, cluster = group)
                MapLabel.BOTH -> MarkerSpec(MarkerKind.PILL, lead.lat, lead.lng, "${shortBankName(lead.title)} $more", mapRateText(groupBest), style, cluster = group)
            }
        }
    }
    selPin?.let { out += pinSpec(it, true, labelMode) }
    return out
}

/* ---------- marker images (one renderer for both platforms) ---------- */

/** Colours for marker images; taken from the theme at composition time. */
class PinColors(val accent: Color, val onAccent: Color, val ink: Color, val inkSoft: Color, val line: Color, val bg: Color)

/**
 * Draws marker images with Compose (rounded label with optional second line and tip; dots for ATMs / rate-less
 * branches). Images are padded with transparent pixels to a 48 dp hit area and cached by [MarkerSpec.imageKey].
 */
class PinRenderer(private val measurer: TextMeasurer, private val density: Density, private val c: PinColors) {
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

    private fun pill(s: MarkerSpec): ImageBitmap {
        val d = density.density
        val two = s.sub != null
        val mainStyle = TextStyle(
            fontSize = (if (two) (if (s.selected) 13 else 11) else (if (s.selected) 16 else 13)).sp,
            fontWeight = if (two) FontWeight.Normal else FontWeight.Medium,
            fontFeatureSettings = "tnum"
        )
        val subStyle = TextStyle(fontSize = (if (s.selected) 15 else 13).sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
        val (fill, stroke, textColor, softColor) = when (s.style) {
            MapPinStyle.GOOD -> listOf(c.accent, c.accent, c.onAccent, c.onAccent)
            MapPinStyle.NEUTRAL -> listOf(c.bg, c.inkSoft, c.ink, c.inkSoft)
            MapPinStyle.CLOSED -> listOf(c.line, c.inkSoft, c.inkSoft, c.inkSoft)
        }
        val main = measurer.measure(s.text, mainStyle.copy(color = if (two) softColor else textColor))
        val sub = s.sub?.let { measurer.measure(it, subStyle.copy(color = textColor)) }
        val padX = 10 * d
        val gap = 1 * d
        val pillH = if (two) main.size.height + sub!!.size.height + gap + 10 * d else (if (s.selected) 34f else 28f) * d
        val textW = max(main.size.width.toFloat(), sub?.size?.width?.toFloat() ?: 0f)
        val pillW = textW + 2 * padX
        val strokeW = (if (s.selected) 2.5f else 1f) * d
        val tipH = if (s.tip) 6 * d else 0f
        val touch = 48 * d
        val bw = max(pillW + 2 * strokeW, touch).toInt()
        val bh = max(pillH + tipH + 2 * strokeW, touch).toInt()
        return draw(bw, bh) {
            val left = (bw - pillW) / 2f
            val bottom = if (s.tip) bh - tipH - strokeW else (bh + pillH) / 2f
            val top = bottom - pillH
            val edge = if (s.selected) c.ink else stroke
            if (s.tip) {
                val path = Path().apply {
                    moveTo(bw / 2f - 5 * d, bottom - 1 * d)
                    lineTo(bw / 2f, bottom + tipH)
                    lineTo(bw / 2f + 5 * d, bottom - 1 * d)
                    close()
                }
                drawPath(path, edge)
            }
            val r = CornerRadius((if (two) 10 else 12) * d)
            drawRoundRect(fill, Offset(left, top), Size(pillW, pillH), r)
            drawRoundRect(
                edge, Offset(left, top), Size(pillW, pillH), r,
                style = Stroke(
                    width = strokeW,
                    pathEffect = if (s.style == MapPinStyle.CLOSED) PathEffect.dashPathEffect(floatArrayOf(4 * d, 3 * d)) else null
                )
            )
            if (sub != null) {
                val y1 = top + 5 * d
                drawText(main, topLeft = Offset(bw / 2f - main.size.width / 2f, y1))
                drawText(sub, topLeft = Offset(bw / 2f - sub.size.width / 2f, y1 + main.size.height + gap))
            } else {
                drawText(main, topLeft = Offset(bw / 2f - main.size.width / 2f, (top + bottom) / 2f - main.size.height / 2f))
            }
        }
    }

    /** Small neutral marker: filled dot = branch without a rate, ring = ATM. */
    private fun dot(atm: Boolean, closed: Boolean): ImageBitmap {
        val d = density.density
        val size = (48 * d).toInt()
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
fun rememberPinRenderer(): PinRenderer {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val density = LocalDensity.current
    val colors = PinColors(VsEmerald, VsBg, VsInk, VsInkSoft, VsLine, VsBg)
    return remember(measurer, density, colors.accent, colors.ink, colors.bg) { PinRenderer(measurer, density, colors) }
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
 * The map itself: osmdroid on Android, MapKit on iOS. Gets the filtered pins; does viewport culling, projection and
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
