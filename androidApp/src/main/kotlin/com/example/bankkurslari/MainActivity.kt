package com.example.bankkurslari

import android.content.Intent
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
        openRequestedTab(intent)
        enableEdgeToEdge()

        // Background rate monitoring: widget refresh + change notifications.
        RateWorker.ensureChannel(this)
        RateWorker.schedulePeriodic(this)
        // The app fetches the selected currency itself; the worker (all currencies + widgets) can wait a little.
        RateWorker.enqueueNow(this, delaySeconds = 8)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequestedTab(intent)
    }

    /** App shortcuts, the wallet widget and the daily summary pass the tab to open as extra "tab" (HomeTab name). */
    private fun openRequestedTab(intent: Intent?) {
        val name = intent?.getStringExtra(EXTRA_TAB) ?: return
        HomeTab.visible.firstOrNull { it.name == name }?.let { NavRequests.tab = it }
    }

    companion object {
        const val EXTRA_TAB = "tab"
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
