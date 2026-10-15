package com.example.bankkurslari

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlatform(this)
        enableEdgeToEdge()

        // Background rate monitoring: widget refresh + change notifications.
        RateWorker.ensureChannel(this)
        RateWorker.schedulePeriodic(this)
        RateWorker.enqueueNow(this)
        PlatformHooks.onCurrencyChanged = { RateWorker.enqueueNow(this) }
        PlatformHooks.onAlertsChanged = {
            RateWorker.ensureAlertChannel(this)
            RateWorker.schedulePeriodic(this)
        }
        maybeRequestNotificationPermission()

        setContent {
            App(onDarkTheme = { dark ->
                // System bar icons follow the app theme, not the system one (avoids white-on-white icons).
                val t = Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            })
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf("android.permission.POST_NOTIFICATIONS"), 1001)
            }
        }
    }
}
