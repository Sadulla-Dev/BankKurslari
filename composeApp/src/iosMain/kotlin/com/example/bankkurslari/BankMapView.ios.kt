package com.example.bankkurslari

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectContainsPoint
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIImage
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow

/** Compose-rendered marker image -> UIImage at screen scale. */
@OptIn(ExperimentalForeignApi::class)
private fun ImageBitmap.toUIImage(scale: Double): UIImage? {
    val bytes = Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: return null
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return UIImage.imageWithData(data, scale)
}

@OptIn(ExperimentalForeignApi::class)
private class PinAnnotation(val spec: MarkerSpec, val image: UIImage?, val order: Int) : NSObject(), MKAnnotationProtocol {
    private val coord = CLLocationCoordinate2DMake(spec.lat, spec.lng)
    override fun coordinate(): CValue<CLLocationCoordinate2D> = coord
}

/** Web-mercator style zoom level of the visible region (osmdroid-compatible thresholds: 12 = city, 15 = streets). */
@OptIn(ExperimentalForeignApi::class)
private fun MKMapView.zoomLevel(): Double {
    val widthPt = bounds.useContents { size.width }
    val lngDelta = region.useContents { span.longitudeDelta }.coerceAtLeast(1e-6)
    return ln(360.0 * (widthPt / 256.0) / lngDelta) / ln(2.0)
}

@OptIn(ExperimentalForeignApi::class)
private class MapKitController(private val map: MKMapView) : MapController {
    override fun animateTo(lat: Double, lng: Double, zoom: Double?) {
        val center = CLLocationCoordinate2DMake(lat, lng)
        if (zoom == null) {
            map.setCenterCoordinate(center, animated = true)
            return
        }
        val widthPt = map.bounds.useContents { size.width }.coerceAtLeast(1.0)
        val lngDelta = 360.0 * (widthPt / 256.0) / 2.0.pow(zoom)
        val latDelta = lngDelta * cos(toRadians(lat))
        map.setRegion(MKCoordinateRegionMake(center, MKCoordinateSpanMake(latDelta, lngDelta)), animated = true)
    }

    override fun fitBounds(north: Double, east: Double, south: Double, west: Double, paddingDp: Int, animate: Boolean) {
        val center = CLLocationCoordinate2DMake((north + south) / 2, (east + west) / 2)
        val span = MKCoordinateSpanMake((north - south) * 1.15, (east - west) * 1.15)
        map.setRegion(MKCoordinateRegionMake(center, span), animated = animate)
    }

    override fun myLocation(): Pair<Double, Double>? =
        map.userLocation.location?.coordinate?.useContents { latitude to longitude }
}

/** Holds everything the MapKit callbacks need; the map's delegate is weak, so this object must stay referenced. */
@OptIn(ExperimentalForeignApi::class)
private class MapState(private val scale: Double) : NSObject(), MKMapViewDelegateProtocol {
    var renderer: PinRenderer? = null
    var pins: List<MapPin> = emptyList()
    var selectedId: String? = null
    var labelMode: MapLabel = MapLabel.RATE
    var sell = true
    var best: Int? = null
    var onPinTap: (MapPin) -> Unit = {}
    var onTileFailure: (Boolean) -> Unit = {}
    var map: MKMapView? = null
    private val images = HashMap<String, UIImage?>()
    private var lastKey: String? = null

    private fun imageFor(spec: MarkerSpec): UIImage? =
        images.getOrPut(spec.imageKey) { renderer?.image(spec)?.toUIImage(scale) }

    /** Rebuild annotations for the visible region. [force] after data changes; otherwise only when the view moved. */
    fun refresh(force: Boolean) {
        val m = map ?: return
        if (renderer == null) return
        val (cLat, cLng, dLat, dLng) = m.region.useContents {
            listOf(center.latitude, center.longitude, span.latitudeDelta, span.longitudeDelta)
        }
        val key = "$cLat|$cLng|$dLat|$dLng"
        if (!force && key == lastKey) return
        lastKey = key
        if (images.size > 600) images.clear()
        val inView = pins.filter {
            it.lat in (cLat - dLat / 2)..(cLat + dLat / 2) && it.lng in (cLng - dLng / 2)..(cLng + dLng / 2)
        }
        // Projection is in points (≈ dp), so the label box needs no density factor.
        val specs = buildMarkerSpecs(inView, selectedId, labelMode, sell, best, m.zoomLevel(), 1f) { lat, lng ->
            m.convertCoordinate(CLLocationCoordinate2DMake(lat, lng), toPointToView = m).useContents { x.toFloat() to y.toFloat() }
        }
        val old = m.annotations.filterIsInstance<PinAnnotation>()
        m.removeAnnotations(old)
        // MapKit has no z-order by insertion; zPriority on the views below keeps the best labels on top.
        m.addAnnotations(specs.mapIndexed { i, spec -> PinAnnotation(spec, imageFor(spec), i) })
    }

    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        val a = viewForAnnotation as? PinAnnotation ?: return null // user location: default blue dot
        val v = mapView.dequeueReusableAnnotationViewWithIdentifier("pin")
            ?: MKAnnotationView(annotation = a, reuseIdentifier = "pin")
        v.annotation = a
        v.image = a.image
        v.canShowCallout = false
        v.setEnabled(false) // selection is handled by onMapTap
        val h = a.image?.size?.useContents { height } ?: 0.0
        v.centerOffset = if (a.spec.anchorBottom) CGPointMake(0.0, -h / 2) else CGPointMake(0.0, 0.0)
        // Later specs (better pins, the selected one) get a higher priority so they draw on top.
        v.zPriority = a.order.toFloat()
        return v
    }

    /** Pin tap: open the pin; cluster tap: zoom in, or open its best pin when it cannot split further. */
    private fun handleTap(mapView: MKMapView, a: PinAnnotation) {
        val group = a.spec.cluster
        when {
            group != null -> {
                val open = clusterTapTarget(group, mapView.zoomLevel())
                if (open != null) onPinTap(open) else {
                    val n = group.maxOf { it.lat }; val s = group.minOf { it.lat }
                    val e = group.maxOf { it.lng }; val w = group.minOf { it.lng }
                    MapKitController(mapView).fitBounds(n, e, s, w, 64, true)
                }
            }
            a.spec.pin != null -> onPinTap(a.spec.pin)
        }
    }

    /**
     * Our own hit test. MapKit's annotation selection did not fire for the label pins on device, so taps are
     * resolved here: the topmost pin whose image (incl. its 48 dp touch padding) contains the point wins.
     */
    @ObjCAction
    fun onMapTap(sender: UITapGestureRecognizer) {
        val mapView = map ?: return
        val pt = sender.locationInView(mapView)
        val hit = mapView.annotations.filterIsInstance<PinAnnotation>()
            .filter { a ->
                val v = mapView.viewForAnnotation(a) ?: return@filter false
                CGRectContainsPoint(v.convertRect(v.bounds, toView = mapView), pt)
            }
            .maxByOrNull { it.order }
        if (hit != null) handleTap(mapView, hit)
    }

    override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) = refresh(force = false)

    override fun mapViewDidFailLoadingMap(mapView: MKMapView, withError: NSError) = onTileFailure(true)

    override fun mapViewDidFinishLoadingMap(mapView: MKMapView) = onTileFailure(false)
}

@OptIn(ExperimentalForeignApi::class)
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
    val renderer = rememberPinRenderer()
    val scale = LocalDensity.current.density.toDouble()
    val state = remember { MapState(scale) }
    val tap by rememberUpdatedState(onPinTap)
    val tile by rememberUpdatedState(onTileFailure)
    state.onPinTap = { tap(it) }
    state.onTileFailure = { tile(it) }

    DisposableEffect(Unit) { onDispose { state.map?.delegate = null; state.map = null } }

    UIKitView(
        factory = {
            MKMapView().apply {
                delegate = state
                // Single tap on a pin, without waiting for MapKit's double-tap-to-zoom.
                addGestureRecognizer(
                    UITapGestureRecognizer(target = state, action = NSSelectorFromString("onMapTap:")).apply {
                        cancelsTouchesInView = false
                    }
                )
                setRegion(
                    MKCoordinateRegionMake(CLLocationCoordinate2DMake(41.311, 69.279), MKCoordinateSpanMake(0.35, 0.35)),
                    animated = false
                )
                state.map = this
                onController(MapKitController(this))
            }
        },
        modifier = modifier,
        update = { map ->
            map.overrideUserInterfaceStyle =
                if (darkMode) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
            map.showsUserLocation = showMyLocation
            val changed = state.renderer !== renderer || state.pins !== pins || state.selectedId != selectedId ||
                state.labelMode != labelMode || state.sell != sell || state.best != best
            state.renderer = renderer
            state.pins = pins
            state.selectedId = selectedId
            state.labelMode = labelMode
            state.sell = sell
            state.best = best
            if (changed) state.refresh(force = true)
        },
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
    )
}
