package com.example.bankkurslari

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.camera.Viewport
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

// OpenFreeMap vector styles: free, no API key, OSM data (attribution shown by the map).
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

private fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (!granted) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return runCatching {
        lm.getProviders(true)
            .mapNotNull { @Suppress("MissingPermission") lm.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }.getOrNull()
}

private fun bounds(north: Double, east: Double, south: Double, west: Double) = BoundingBox(west, south, east, north)

private class LibreController(
    private val state: MapState,
    private val scope: CoroutineScope,
    private val context: Context
) : MapController {
    override fun animateTo(lat: Double, lng: Double, zoom: Double?) {
        scope.launch {
            runCatching {
                state.animateCamera(CameraUpdate(target = Position(lng, lat), zoom = zoom), CameraAnimation.Ease(600.milliseconds))
            }
        }
    }

    override fun fitBounds(north: Double, east: Double, south: Double, west: Double, paddingDp: Int, animate: Boolean) {
        val pad = DpPadding(paddingDp.dp, paddingDp.dp, paddingDp.dp, paddingDp.dp)
        scope.launch {
            runCatching {
                if (animate) {
                    val cam = state.cameraForBounds(bounds(north, east, south, west), fitPadding = pad)
                    state.animateCamera(cam.toCameraUpdate(), CameraAnimation.Ease(600.milliseconds))
                } else {
                    state.fitCameraToBounds(bounds(north, east, south, west), fitPadding = pad)
                }
            }
        }
    }

    override fun myLocation(): Pair<Double, Double>? = lastKnownLocation(context)
}

@Composable
actual fun BankMapView(
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
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val renderer = rememberPinRenderer()
    val tapPin by rememberUpdatedState(onPinTap)
    val scope = rememberCoroutineScope()

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(if (darkMode) STYLE_DARK else STYLE_LIGHT),
        initialCameraPosition = CameraPosition(target = Position(69.279, 41.311), zoom = 11.0)
    )

    LaunchedEffect(mapState) {
        onTileFailure(false)
        onController(LibreController(mapState, scope, context))
    }

    // Markers are rebuilt only when the camera settles; while it moves they ride along via placedAt.
    var settled by remember { mutableStateOf<Viewport?>(null) }
    LaunchedEffect(mapState) {
        snapshotFlow { if (mapState.isCameraMoving) null else mapState.viewport }
            .filterNotNull()
            .collect { settled = it }
    }
    val specs = remember(settled, pins, selectedId, labelMode, sell, best) {
        val vp = settled ?: return@remember emptyList()
        val sw = vp.visibleBounds.southwest
        val ne = vp.visibleBounds.northeast
        val inView = pins.filter { it.lat in sw.latitude..ne.latitude && it.lng in sw.longitude..ne.longitude }
        buildMarkerSpecs(inView, selectedId, labelMode, sell, best, vp.cameraPosition.zoom, density) { lat, lng ->
            val o = mapState.screenLocationFromPosition(Position(lng, lat))
            if (o == null) -1e6f to -1e6f else o.x.value * density to o.y.value * density
        }
    }

    var myLoc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(showMyLocation) {
        if (!showMyLocation) {
            myLoc = null
            return@LaunchedEffect
        }
        while (true) {
            myLoc = lastKnownLocation(context)
            delay(10_000)
        }
    }

    MaplibreMap(
        modifier = modifier,
        state = mapState,
        interactions = MapInteractions {
            camera {
                rotate { enabled = false }
                tilt { enabled = false }
            }
        },
        overlay = {
            include(MapOverlay.AttributionOnly)
            myLoc?.let { (lat, lng) ->
                Image(renderer.locationDot(), null, Modifier.placedAt(Position(lng, lat)).zIndex(3f))
            }
            specs.forEach { spec ->
                key(spec.imageKey, spec.lat, spec.lng) {
                    Image(
                        bitmap = renderer.image(spec),
                        contentDescription = spec.pin?.let { "${it.title} ${spec.text}" } ?: spec.text,
                        modifier = Modifier
                            .placedAt(Position(spec.lng, spec.lat), if (spec.anchorBottom) Alignment.BottomCenter else Alignment.Center)
                            .zIndex(if (spec.selected) 2f else if (spec.cluster != null) 1f else 0f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                val group = spec.cluster
                                when {
                                    group != null -> {
                                        val open = clusterTapTarget(group, mapState.cameraPosition.zoom)
                                        if (open != null) tapPin(open) else scope.launch {
                                            val pad = DpPadding(64.dp, 64.dp, 64.dp, 64.dp)
                                            runCatching {
                                                val cam = mapState.cameraForBounds(
                                                    bounds(group.maxOf { it.lat }, group.maxOf { it.lng }, group.minOf { it.lat }, group.minOf { it.lng }),
                                                    fitPadding = pad
                                                )
                                                mapState.animateCamera(cam.toCameraUpdate(), CameraAnimation.Ease(500.milliseconds))
                                            }
                                        }
                                    }
                                    spec.pin != null -> tapPin(spec.pin)
                                }
                            }
                    )
                }
            }
        }
    )
}
