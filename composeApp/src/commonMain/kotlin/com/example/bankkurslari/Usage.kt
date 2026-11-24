package com.example.bankkurslari

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Anonymous usage counts: which features people actually use, as daily totals (backend/bot/worker.js,
 * POST https://bot.qulaykurs.uz/e). The request carries the platform and event names only: no id, no device data,
 * no location. The server adds 1 to a counter per (day, event, platform) and keeps nothing else.
 *
 * Each event is sent at most once per app session (a hundred taps on one button are one count), and "open" once a
 * day per install, so the numbers read as "sessions that used X" and "people who opened the app today".
 * Fire and forget: a failed send is dropped.
 */
object Usage {
    private const val URL = "https://bot.qulaykurs.uz/e"
    private const val OPEN_DAY_KEY = "usage_open_day"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = PlatformLock()
    private val sent = mutableSetOf<String>()

    /** Tashkent day number (UTC+5, no daylight saving). */
    private fun today(): String = ((Clock.System.now().toEpochMilliseconds() + 5 * 3_600_000L) / 86_400_000L).toString()

    fun appOpened() {
        val store = appStore()
        val day = today()
        if (store.getString(OPEN_DAY_KEY) == day) return
        store.putString(OPEN_DAY_KEY, day)
        send("open")
    }

    fun track(event: String) {
        val first = lock.withLock { sent.add(event) }
        if (first) send(event)
    }

    private fun send(event: String) {
        if (isDebugBuild()) return
        scope.launch {
            runCatching { httpPostJson(URL, """{"p":"$usagePlatform","e":["$event"]}""", 5_000) }
        }
    }
}
