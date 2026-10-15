package com.example.bankkurslari

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

/* ---------- storage ---------- */

private class DefaultsStore(private val d: NSUserDefaults) : KeyValueStore {
    override fun contains(key: String) = d.objectForKey(key) != null
    override fun getString(key: String): String? = d.stringForKey(key)
    override fun putString(key: String, value: String?) {
        if (value == null) d.removeObjectForKey(key) else d.setObject(value, key)
    }
    override fun getInt(key: String, default: Int) = if (contains(key)) d.integerForKey(key).toInt() else default
    override fun putInt(key: String, value: Int) = d.setInteger(value.toLong(), key)
    override fun getBoolean(key: String, default: Boolean) = if (contains(key)) d.boolForKey(key) else default
    override fun putBoolean(key: String, value: Boolean) = d.setBool(value, key)
    override fun getDouble(key: String): Double? = if (contains(key)) d.doubleForKey(key) else null
    override fun putDouble(key: String, value: Double) = d.setDouble(value, key)
    override fun getStringSet(key: String): Set<String> =
        (d.arrayForKey(key) ?: emptyList<Any?>()).filterIsInstance<String>().toSet()
    override fun putStringSet(key: String, value: Set<String>) = d.setObject(value.toList(), key)
}

private val store: KeyValueStore by lazy { DefaultsStore(NSUserDefaults.standardUserDefaults) }

actual fun appStore(): KeyValueStore = store

private val cacheDirPath: String by lazy {
    val base = (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String) ?: NSTemporaryDirectory()
    val dir = "$base/cache-json"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    // One-time cleanup: older builds kept large JSON under "cache_*" keys in NSUserDefaults.
    val d = NSUserDefaults.standardUserDefaults
    d.dictionaryRepresentation().keys.filterIsInstance<String>().filter { it.startsWith("cache_") }.forEach { d.removeObjectForKey(it) }
    dir
}

actual fun readCacheFile(name: String): String? =
    NSString.stringWithContentsOfFile("$cacheDirPath/$name", encoding = NSUTF8StringEncoding, error = null)

actual fun writeCacheFile(name: String, text: String) {
    // atomically = true: written to a temp file and moved into place.
    NSString.create(string = text).writeToFile("$cacheDirPath/$name", atomically = true, encoding = NSUTF8StringEncoding, error = null)
}

actual class PlatformLock actual constructor() {
    private val lock = NSLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}

/* ---------- UI helpers ---------- */

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
        ?: return null
    var vc = window.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

actual fun shareText(text: String) {
    val vc = topViewController() ?: return
    val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    // iPad: anchor the popover to the presenting view.
    sheet.popoverPresentationController?.sourceView = vc.view
    vc.presentViewController(sheet, animated = true, completion = null)
}

actual fun openUri(uri: String): Boolean {
    val url = NSURL.URLWithString(uri) ?: return false
    if (!UIApplication.sharedApplication.canOpenURL(url)) return false
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    return true
}

/** Needs the scheme in Info.plist LSApplicationQueriesSchemes, otherwise iOS always answers "no". */
actual fun isMapAppInstalled(app: MapApp): Boolean {
    val scheme = app.iosScheme ?: return false
    val url = NSURL.URLWithString("$scheme://") ?: return false
    return UIApplication.sharedApplication.canOpenURL(url)
}

actual fun launchMapApp(app: MapApp, target: RouteTarget): Boolean {
    // OTHER on iOS = Apple Maps, which is always there.
    val uri = if (app == MapApp.OTHER) "maps://?daddr=${target.lat},${target.lng}&dirflg=d"
    else (app.iosApp ?: app.app)(target)
    return openUri(uri)
}

/* ---------- location ---------- */

private object Location {
    val manager = CLLocationManager()
    private var pending: ((Boolean) -> Unit)? = null

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            val status = manager.authorizationStatus
            if (status == kCLAuthorizationStatusNotDetermined) return
            if (granted()) manager.startUpdatingLocation()
            pending?.invoke(granted())
            pending = null
        }
    }

    init {
        manager.delegate = delegate
        if (granted()) manager.startUpdatingLocation()
    }

    fun granted(): Boolean {
        val s = manager.authorizationStatus
        return s == kCLAuthorizationStatusAuthorizedWhenInUse || s == kCLAuthorizationStatusAuthorizedAlways
    }

    fun request(onResult: (Boolean) -> Unit) {
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            onResult(granted())
            return
        }
        pending = onResult
        manager.requestWhenInUseAuthorization()
    }
}

actual fun hasLocationPermission(): Boolean = Location.granted()

@OptIn(ExperimentalForeignApi::class)
actual fun lastKnownLocation(): Pair<Double, Double>? {
    if (!Location.granted()) return null
    return Location.manager.location?.coordinate?.useContents { latitude to longitude }
}

@Composable
actual fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit =
    remember(onResult) { { Location.request(onResult) } }

@Composable
actual fun rememberNotificationPermissionRequest(): () -> Unit = remember {
    {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }
}
