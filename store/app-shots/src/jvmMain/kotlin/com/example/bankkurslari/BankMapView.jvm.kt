package com.example.bankkurslari

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Image
import java.io.File
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Screenshot stand-in for the MapLibre view: a pre-rendered basemap (same OpenFreeMap Positron style the app uses,
 * rendered by store/app-shots/basemap.js at [ShotCamera]) with the app's own markers on top, built by the shared
 * [buildMarkerSpecs] and drawn by the shared [PinRenderer], exactly as BankMapView.android does.
 */
object ShotCamera {
    var lat = 41.3110
    var lng = 69.2700
    var zoom = 13.6
    var basemap: String? = System.getenv("QK_BASEMAP")
}

private fun mercX(lng: Double) = (lng + 180.0) / 360.0
private fun mercY(lat: Double): Double {
    val r = lat * PI / 180.0
    return (1.0 - ln(tan(r) + 1.0 / kotlin.math.cos(r)) / PI) / 2.0
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
    active: Boolean,
    onPinTap: (MapPin) -> Unit,
    onController: (MapController) -> Unit,
    onTileFailure: (Boolean) -> Unit
) {
    val density = LocalDensity.current.density
    val renderer = rememberPinRenderer(touchPadding = false)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bg: ImageBitmap? = remember {
        ShotCamera.basemap?.let { p -> runCatching { Image.makeFromEncoded(File(p).readBytes()).toComposeImageBitmap() }.getOrNull() }
    }
    LaunchedEffect(Unit) {
        onTileFailure(false)
        onController(object : MapController {
            override fun animateTo(lat: Double, lng: Double, zoom: Double?) {}
            override fun fitBounds(north: Double, east: Double, south: Double, west: Double, paddingDp: Int, animate: Boolean) {}
            override fun myLocation() = lastKnownLocation()
        })
    }
    val world = 512.0 * 2.0.pow(ShotCamera.zoom) * density
    fun project(lat: Double, lng: Double): Pair<Float, Float> {
        val x = (mercX(lng) - mercX(ShotCamera.lng)) * world + size.width / 2.0
        val y = (mercY(lat) - mercY(ShotCamera.lat)) * world + size.height / 2.0
        return x.toFloat() to y.toFloat()
    }
    val specs = remember(pins, selectedId, labelMode, sell, best, size) {
        // Same as Android: only pins within the viewport grown by half a viewport each way.
        val inView = pins.filter {
            val (x, y) = project(it.lat, it.lng)
            x in -size.width * 0.5f..size.width * 1.5f && y in -size.height * 0.5f..size.height * 1.5f
        }
        if (size.width == 0) emptyList()
        else buildMarkerSpecs(inView, selectedId, labelMode, sell, best, ShotCamera.zoom, density, size.width.toFloat(), size.height.toFloat(), ::project)
    }
    Box(modifier.onSizeChanged { size = it }) {
        Canvas(Modifier.matchParentSize()) {
            bg?.let { drawImage(it, Offset((this.size.width - it.width) / 2f, (this.size.height - it.height) / 2f)) }
            if (showMyLocation) lastKnownLocation()?.let { (la, ln) ->
                val dot = renderer.locationDot(); val (x, y) = project(la, ln)
                drawImage(dot, Offset(x - dot.width / 2f, y - dot.height / 2f))
            }
            specs.sortedBy { if (it.selected) 2 else if (it.cluster != null) 1 else 0 }.forEach { s ->
                val img = renderer.image(s); val (x, y) = project(s.lat, s.lng)
                val top = if (s.anchorBottom) y - img.height else y - img.height / 2f
                drawImage(img, Offset(x - img.width / 2f, top))
            }
        }
    }
}
