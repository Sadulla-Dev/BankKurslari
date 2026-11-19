package com.example.bankkurslari

import androidx.compose.runtime.Composable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Desktop actuals used only to render store screenshots of the shared UI. State lives in memory. */
private class MemStore : KeyValueStore {
    val m = ConcurrentHashMap<String, Any>()
    override fun contains(key: String) = m.containsKey(key)
    override fun getString(key: String) = m[key] as? String
    override fun putString(key: String, value: String?) { if (value == null) m.remove(key) else m[key] = value }
    override fun getInt(key: String, default: Int) = m[key] as? Int ?: default
    override fun putInt(key: String, value: Int) { m[key] = value }
    override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { m[key] = value }
    override fun getDouble(key: String) = m[key] as? Double
    override fun putDouble(key: String, value: Double) { m[key] = value }
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String) = m[key] as? Set<String> ?: emptySet()
    override fun putStringSet(key: String, value: Set<String>) { m[key] = value }
}

private val store: KeyValueStore = MemStore()
actual fun appStore(): KeyValueStore = store
fun shotStore(): KeyValueStore = store

actual fun platformServerBase(lan: String): String = lan

private val cacheDir = File(System.getProperty("java.io.tmpdir"), "qk-shots-cache").apply { mkdirs() }
actual fun readCacheFile(name: String): String? = File(cacheDir, name).takeIf { it.exists() }?.readText()
actual fun writeCacheFile(name: String, text: String) { File(cacheDir, name).writeText(text) }

actual class PlatformLock actual constructor() {
    private val l = ReentrantLock()
    actual fun <T> withLock(block: () -> T): T = l.withLock(block)
}

actual fun shareText(text: String) {}
actual fun openUri(uri: String): Boolean = false
actual fun isMapAppInstalled(app: MapApp): Boolean = true
actual fun launchMapApp(app: MapApp, target: RouteTarget): Boolean = false

/** Fixed user position for distances in screenshots: Amir Temur square, Tashkent. */
var shotLocation: Pair<Double, Double> = 41.3135 to 69.2660
actual fun lastKnownLocation(): Pair<Double, Double>? = shotLocation
actual fun hasLocationPermission(): Boolean = true

@Composable
actual fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit = { onResult(true) }

@Composable
actual fun rememberNotificationPermissionRequest(): () -> Unit = {}

actual fun debugLog(msg: String) { if (System.getenv("QK_DEBUG") != null) println("QK $msg") }
