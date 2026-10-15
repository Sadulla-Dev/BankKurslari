package com.example.bankkurslari

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/** Best last-known (lat, lng) if location permission is granted, else null. No active location request. */
internal fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return try {
        lm.getProviders(true)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    } catch (_: SecurityException) {
        null
    }
}
