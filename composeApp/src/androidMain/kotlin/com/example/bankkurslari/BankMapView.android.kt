package com.example.bankkurslari

import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
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

private class OsmController(private val map: MapView, private val loc: () -> MyLocationNewOverlay?) : MapController {
    override fun animateTo(lat: Double, lng: Double, zoom: Double?) {
        if (zoom == null) map.controller.animateTo(GeoPoint(lat, lng))
        else map.controller.animateTo(GeoPoint(lat, lng), zoom, 600L)
    }

    override fun fitBounds(north: Double, east: Double, south: Double, west: Double, paddingDp: Int, animate: Boolean) {
        val pad = (paddingDp * map.context.resources.displayMetrics.density).toInt()
        map.zoomToBoundingBox(BoundingBox(north, east, south, west), animate, pad)
    }

    override fun myLocation(): Pair<Double, Double>? = loc()?.myLocation?.let { it.latitude to it.longitude }
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
    val renderer = rememberPinRenderer()
    val surfaceArgb = VsSurface.toArgb()
    val lineArgb = VsLine.toArgb()
    val tapPin by rememberUpdatedState(onPinTap)
    val tileCb by rememberUpdatedState(onTileFailure)

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var tileFailed by remember { mutableStateOf(false) }
    // Only render markers inside the current viewport (dataset is thousands of points).
    var bbox by remember { mutableStateOf<BoundingBox?>(null) }

    // my-location overlay
    LaunchedEffect(mapView, showMyLocation) {
        val mv = mapView
        if (mv != null && showMyLocation && locOverlay == null) {
            val dot = renderer.locationDot().asAndroidBitmap()
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

    // Tile failures: retry by re-invalidating (failed tiles are not cached, so the next draw requests them again).
    LaunchedEffect(tileFailed, mapView) {
        tileCb(tileFailed)
        while (tileFailed) {
            delay(6000)
            mapView?.invalidate()
        }
    }

    AndroidView(
        modifier = modifier,
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
                onController(OsmController(this) { locOverlay })
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
            val inView = if (b == null) emptyList() else pins.filter { b.contains(it.lat, it.lng) }
            val dens = map.context.resources.displayMetrics.density
            val proj = map.projection
            val zoom = map.zoomLevelDouble
            val specs = buildMarkerSpecs(inView, selectedId, labelMode, sell, best, zoom, dens) { lat, lng ->
                val pt = proj.toPixels(GeoPoint(lat, lng), null)
                pt.x.toFloat() to pt.y.toFloat()
            }
            specs.forEach { spec ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(spec.lat, spec.lng)
                    setAnchor(Marker.ANCHOR_CENTER, if (spec.anchorBottom) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER)
                    icon = BitmapDrawable(map.resources, renderer.image(spec).asAndroidBitmap())
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        val group = spec.cluster
                        when {
                            group != null -> {
                                val n = group.maxOf { it.lat }; val s = group.minOf { it.lat }
                                val e = group.maxOf { it.lng }; val w = group.minOf { it.lng }
                                if (n - s < 1e-4 && e - w < 1e-4) map.controller.animateTo(GeoPoint(spec.lat, spec.lng), zoom + 2.0, 500L)
                                else map.zoomToBoundingBox(BoundingBox(n, e, s, w), true, (64 * dens).toInt())
                            }
                            spec.pin != null -> tapPin(spec.pin)
                        }
                        true
                    }
                })
            }
            locOverlay?.let { map.overlays.add(it) }
            map.invalidate()
        },
        onRelease = { it.onDetach() }
    )
}
