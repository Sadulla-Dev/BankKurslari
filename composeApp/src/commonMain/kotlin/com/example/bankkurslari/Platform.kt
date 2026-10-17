package com.example.bankkurslari

import androidx.compose.runtime.Composable
import kotlin.time.Clock

/** Small persistent key-value store: SharedPreferences("valyutastat") on Android, NSUserDefaults on iOS. */
interface KeyValueStore {
    fun contains(key: String): Boolean
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    /** null when the key is absent. */
    fun getDouble(key: String): Double?
    fun putDouble(key: String, value: Double)
    fun getStringSet(key: String): Set<String>
    fun putStringSet(key: String, value: Set<String>)
}

expect fun appStore(): KeyValueStore

/** Server URL for this device: emulator / simulator use the host alias, real devices [lan]. */
expect fun platformServerBase(lan: String): String

/**
 * Large JSON payloads (rates, branches, ATMs, history) live in plain files, not in [KeyValueStore]:
 * SharedPreferences / NSUserDefaults rewrite and reload the whole store, which blocked the UI for seconds.
 */
expect fun readCacheFile(name: String): String?

expect fun writeCacheFile(name: String, text: String)

/** Mutual exclusion for read-modify-write of stored data (UI vs background worker). */
expect class PlatformLock() {
    fun <T> withLock(block: () -> T): T
}

/** System share sheet with plain text. */
expect fun shareText(text: String)

/** Opens a URL / mailto: link. Returns false if nothing could handle it. */
expect fun openUri(uri: String): Boolean

/** Whether [app] is installed (Android: package lookup, iOS: URL scheme check). */
expect fun isMapAppInstalled(app: MapApp): Boolean

/** Opens [target] in [app] itself (assumes [isMapAppInstalled]); [MapApp.OTHER] = system chooser / Apple Maps. */
expect fun launchMapApp(app: MapApp, target: RouteTarget): Boolean

/** Best last-known (lat, lng) when location permission is granted; no active request. */
expect fun lastKnownLocation(): Pair<Double, Double>?

expect fun hasLocationPermission(): Boolean

/** Returns a function that asks for location permission; [onResult] gets the outcome. */
@Composable
expect fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit

/** Returns a function that asks for notification permission (no-op where not needed). */
@Composable
expect fun rememberNotificationPermissionRequest(): () -> Unit

/** Hooks the host app wires to its background machinery (Android: WorkManager). No-ops by default. */
object PlatformHooks {
    var onCurrencyChanged: () -> Unit = {}
    var onAlertsChanged: () -> Unit = {}
}

/** Diagnostics to the device console (Logcat / Xcode console), tag "QK". */
expect fun debugLog(msg: String)

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
