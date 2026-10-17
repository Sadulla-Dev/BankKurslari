package com.example.bankkurslari

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Holds the application context for the shared code. Call [initPlatform] before anything else (activity, worker, widget). */
object AndroidPlatform {
    @Volatile
    private var appContext: Context? = null
    val context: Context get() = appContext ?: error("initPlatform(context) was not called")
    fun init(c: Context) { if (appContext == null) appContext = c.applicationContext }
}

fun initPlatform(context: Context) = AndroidPlatform.init(context)

/* ---------- storage ---------- */

private class PrefsStore(private val p: SharedPreferences) : KeyValueStore {
    override fun contains(key: String) = p.contains(key)
    override fun getString(key: String): String? = runCatching { p.getString(key, null) }.getOrNull()
    override fun putString(key: String, value: String?) {
        p.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
    override fun getInt(key: String, default: Int) = runCatching { p.getInt(key, default) }.getOrDefault(default)
    override fun putInt(key: String, value: Int) = p.edit().putInt(key, value).apply()
    override fun getBoolean(key: String, default: Boolean) = runCatching { p.getBoolean(key, default) }.getOrDefault(default)
    override fun putBoolean(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    /** Stored as a string (exact); older app versions stored a Float, still readable. */
    override fun getDouble(key: String): Double? {
        if (!p.contains(key)) return null
        return runCatching { p.getString(key, null)?.toDoubleOrNull() }.getOrNull()
            ?: runCatching { p.getFloat(key, Float.NaN).toDouble().takeIf { !it.isNaN() } }.getOrNull()
    }
    override fun putDouble(key: String, value: Double) = p.edit().putString(key, value.toString()).apply()
    override fun getStringSet(key: String): Set<String> =
        runCatching { p.getStringSet(key, emptySet())?.toSet() }.getOrNull() ?: emptySet()
    override fun putStringSet(key: String, value: Set<String>) = p.edit().putStringSet(key, value).apply()
}

private val store: KeyValueStore by lazy {
    val p = AndroidPlatform.context.getSharedPreferences("valyutastat", Context.MODE_PRIVATE)
    // One-time cleanup: older versions kept megabytes of JSON under "cache_*" keys here.
    val stale = p.all.keys.filter { it.startsWith("cache_") }
    if (stale.isNotEmpty()) p.edit().apply { stale.forEach { remove(it) } }.apply()
    PrefsStore(p)
}

actual fun appStore(): KeyValueStore = store

private val isEmulator: Boolean by lazy {
    android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.FINGERPRINT.contains("emulator") ||
        android.os.Build.MODEL.contains("sdk_gphone") || android.os.Build.HARDWARE.contains("ranchu")
}

/** 10.0.2.2 is the emulator's fixed alias for the host machine, so it survives network/IP changes. */
actual fun platformServerBase(lan: String): String = if (isEmulator) "http://10.0.2.2:8080" else lan

actual fun debugLog(msg: String) { android.util.Log.d("QK", msg) }

private val cacheDir: File by lazy {
    File(AndroidPlatform.context.filesDir, "cache-json").apply { mkdirs() }
}

actual fun readCacheFile(name: String): String? =
    runCatching { File(cacheDir, name).takeIf { it.exists() }?.readText() }.getOrNull()

actual fun writeCacheFile(name: String, text: String) {
    // Write to a temp file and rename, so a reader never sees half a file.
    runCatching {
        val tmp = File(cacheDir, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(File(cacheDir, name))
    }
}

actual class PlatformLock actual constructor() {
    private val lock = ReentrantLock()
    actual fun <T> withLock(block: () -> T): T = lock.withLock(block)
}

/* ---------- intents ---------- */

private fun start(intent: Intent): Boolean =
    runCatching { AndroidPlatform.context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

actual fun shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    start(Intent.createChooser(send, null))
}

actual fun openUri(uri: String): Boolean {
    val action = if (uri.startsWith("mailto:")) Intent.ACTION_SENDTO else Intent.ACTION_VIEW
    return start(Intent(action, Uri.parse(uri)))
}

actual fun isMapAppInstalled(app: MapApp): Boolean {
    val pkg = app.androidPkg ?: return false
    return runCatching { AndroidPlatform.context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
}

actual fun launchMapApp(app: MapApp, target: RouteTarget): Boolean {
    if (app == MapApp.OTHER) {
        // geo: intent without a package -> Android shows the map-app chooser.
        val geo = "geo:${target.lat},${target.lng}?q=${target.lat},${target.lng}(${Uri.encode(target.name)})"
        return start(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(geo)), target.name))
    }
    return start(Intent(Intent.ACTION_VIEW, Uri.parse(app.app(target))).setPackage(app.androidPkg))
}

/* ---------- location ---------- */

private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION

actual fun hasLocationPermission(): Boolean {
    val c = AndroidPlatform.context
    return ContextCompat.checkSelfPermission(c, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

actual fun lastKnownLocation(): Pair<Double, Double>? {
    if (!hasLocationPermission()) return null
    val lm = AndroidPlatform.context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return try {
        lm.getProviders(true)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    } catch (_: SecurityException) {
        null
    }
}

@Composable
actual fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it) }
    return { launcher.launch(FINE_LOCATION) }
}

@Composable
actual fun rememberNotificationPermissionRequest(): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(AndroidPlatform.context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
