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
    val useGoogle = remember { hasGoogleMapsKey(context) }
    if (useGoogle) {
        GoogleBankMap(modifier, darkMode, pins, selectedId, labelMode, sell, best, showMyLocation, onPinTap, onController, onTileFailure)
    } else {
        OsmBankMap(modifier, darkMode, pins, selectedId, labelMode, sell, best, showMyLocation, onPinTap, onController, onTileFailure)
    }
}

@Composable
private fun OsmBankMap(
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
                                val open = clusterTapTarget(group, zoom)
                                if (open != null) tapPin(open) else {
                                    val n = group.maxOf { it.lat }; val s = group.minOf { it.lat }
                                    val e = group.maxOf { it.lng }; val w = group.minOf { it.lng }
                                    map.zoomToBoundingBox(BoundingBox(n, e, s, w), true, (64 * dens).toInt())
                                }
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

/* ---------------- Google Maps (used when a Maps API key is in the manifest) ---------------- */

private fun hasGoogleMapsKey(context: android.content.Context): Boolean = runCatching {
    val info = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
    !info.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank() &&
        com.google.android.gms.common.GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
}.getOrDefault(false)

// Google "night" style, trimmed: dark land/water, muted labels, no POI clutter.
private const val DARK_STYLE = """[
{"elementType":"geometry","stylers":[{"color":"#1f2421"}]},
{"elementType":"labels.text.fill","stylers":[{"color":"#9aa59f"}]},
{"elementType":"labels.text.stroke","stylers":[{"color":"#1f2421"}]},
{"featureType":"road","elementType":"geometry","stylers":[{"color":"#343b37"}]},
{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#454e48"}]},
{"featureType":"water","elementType":"geometry","stylers":[{"color":"#121816"}]},
{"featureType":"poi","stylers":[{"visibility":"off"}]},
{"featureType":"transit","stylers":[{"visibility":"off"}]}
]"""

// Light: hide shops/restaurants etc. so bank pins stay readable.
private const val LIGHT_STYLE = """[
{"featureType":"poi.business","stylers":[{"visibility":"off"}]},
{"featureType":"transit","elementType":"labels.icon","stylers":[{"visibility":"off"}]}
]"""

private class GoogleController(private val map: com.google.android.gms.maps.GoogleMap, private val density: Float) : MapController {
    override fun animateTo(lat: Double, lng: Double, zoom: Double?) {
        val target = com.google.android.gms.maps.model.LatLng(lat, lng)
        val update = if (zoom == null) com.google.android.gms.maps.CameraUpdateFactory.newLatLng(target)
        else com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(target, zoom.toFloat())
        map.animateCamera(update, 600, null)
    }

    override fun fitBounds(north: Double, east: Double, south: Double, west: Double, paddingDp: Int, animate: Boolean) {
        val bounds = com.google.android.gms.maps.model.LatLngBounds(
            com.google.android.gms.maps.model.LatLng(south, west), com.google.android.gms.maps.model.LatLng(north, east)
        )
        val update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, (paddingDp * density).toInt())
        runCatching { if (animate) map.animateCamera(update) else map.moveCamera(update) }
    }

    @Suppress("DEPRECATION")
    override fun myLocation(): Pair<Double, Double>? =
        runCatching { map.myLocation }.getOrNull()?.let { it.latitude to it.longitude }
}

@android.annotation.SuppressLint("MissingPermission") // callers check the permission first
private fun enableMyLocation(g: com.google.android.gms.maps.GoogleMap, on: Boolean) {
    g.isMyLocationEnabled = on
}

@Composable
private fun GoogleBankMap(
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
    val tapPin by rememberUpdatedState(onPinTap)
    var gmap by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    // Bumped on every camera stop so markers are rebuilt for the new viewport/zoom.
    var cameraTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { onTileFailure(false) }

    LaunchedEffect(gmap, darkMode) {
        gmap?.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions(if (darkMode) DARK_STYLE else LIGHT_STYLE))
    }

    LaunchedEffect(gmap, showMyLocation) {
        val g = gmap ?: return@LaunchedEffect
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) enableMyLocation(g, showMyLocation)
    }

    // Markers: only the current viewport, rebuilt when data or camera changes.
    val markers = remember { mutableListOf<com.google.android.gms.maps.model.Marker>() }
    LaunchedEffect(gmap, pins, selectedId, labelMode, sell, best, cameraTick) {
        val g = gmap ?: return@LaunchedEffect
        val proj = g.projection
        val bounds = proj.visibleRegion.latLngBounds
        val inView = pins.filter { bounds.contains(com.google.android.gms.maps.model.LatLng(it.lat, it.lng)) }
        val dens = context.resources.displayMetrics.density
        val zoom = g.cameraPosition.zoom.toDouble()
        val specs = buildMarkerSpecs(inView, selectedId, labelMode, sell, best, zoom, dens) { lat, lng ->
            val pt = proj.toScreenLocation(com.google.android.gms.maps.model.LatLng(lat, lng))
            pt.x.toFloat() to pt.y.toFloat()
        }
        markers.forEach { it.remove() }
        markers.clear()
        specs.forEach { spec ->
            val m = g.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(com.google.android.gms.maps.model.LatLng(spec.lat, spec.lng))
                    .anchor(0.5f, if (spec.anchorBottom) 1f else 0.5f)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(renderer.image(spec).asAndroidBitmap()))
                    .zIndex(if (spec.selected) 2f else if (spec.cluster != null) 1f else 0f)
            )
            if (m != null) {
                m.tag = spec
                markers.add(m)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val options = com.google.android.gms.maps.GoogleMapOptions()
                .camera(com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(41.311, 69.279), 11f))
                .mapToolbarEnabled(false)
                .compassEnabled(false)
                .zoomControlsEnabled(false)
            com.google.android.gms.maps.MapView(ctx, options).apply {
                onCreate(null)
                onStart()
                onResume()
                getMapAsync { g ->
                    g.uiSettings.isMyLocationButtonEnabled = false
                    g.uiSettings.isMapToolbarEnabled = false
                    g.uiSettings.isRotateGesturesEnabled = false
                    g.uiSettings.isTiltGesturesEnabled = false
                    g.isIndoorEnabled = false
                    g.setMinZoomPreference(4f)
                    g.setOnCameraIdleListener { cameraTick++ }
                    g.setOnMarkerClickListener { marker ->
                        val spec = marker.tag as? MarkerSpec ?: return@setOnMarkerClickListener true
                        val group = spec.cluster
                        when {
                            group != null -> {
                                val open = clusterTapTarget(group, g.cameraPosition.zoom.toDouble())
                                if (open != null) tapPin(open) else {
                                    val b = com.google.android.gms.maps.model.LatLngBounds.builder()
                                    group.forEach { b.include(com.google.android.gms.maps.model.LatLng(it.lat, it.lng)) }
                                    val pad = (64 * ctx.resources.displayMetrics.density).toInt()
                                    runCatching { g.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(b.build(), pad)) }
                                }
                            }
                            spec.pin != null -> tapPin(spec.pin)
                        }
                        true
                    }
                    gmap = g
                    onController(GoogleController(g, ctx.resources.displayMetrics.density))
                }
            }
        },
        onRelease = { mv ->
            mv.onPause()
            mv.onStop()
            mv.onDestroy()
        }
    )
}
