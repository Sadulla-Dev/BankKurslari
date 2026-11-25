package com.example.bankkurslari

/**
 * Asks for a store rating (Google Play In-App Review, App Store StoreKit) once the app has proved useful: it was opened
 * on [MIN_DAYS] different days, and the ask comes right after the core action (an amount typed and left for a few
 * seconds), never on launch. At most [MAX_ASKS] asks, [GAP_DAYS] days apart. The store adds its own quota and may show
 * nothing at all; it never says whether the dialog appeared or what was rated, so nothing here depends on that.
 */
object ReviewPrompt {
    private const val KEY_DAYS = "review_days"
    private const val KEY_LAST_DAY = "review_last_day"
    private const val KEY_ASKED_AT = "review_asked_at"
    private const val KEY_ASKS = "review_asks"
    private const val MIN_DAYS = 3
    private const val MAX_ASKS = 3
    private const val GAP_DAYS = 120L

    /** Tashkent day number (UTC+5), like [Usage]. */
    private fun today(): String = ((nowMillis() + 5 * 3_600_000L) / 86_400_000L).toString()

    /** Counts the distinct days the app was opened. */
    fun onAppOpened() {
        val s = appStore()
        val day = today()
        if (s.getString(KEY_LAST_DAY) == day) return
        s.putString(KEY_LAST_DAY, day)
        s.putInt(KEY_DAYS, s.getInt(KEY_DAYS, 0) + 1)
    }

    /** The user just got an answer (typed an amount): ask now if the app has earned it and the last ask is long ago. */
    fun onUseful() {
        val s = appStore()
        if (s.getInt(KEY_DAYS, 0) < MIN_DAYS) return
        val asks = s.getInt(KEY_ASKS, 0)
        if (asks >= MAX_ASKS) return
        val now = nowMillis()
        val last = s.getString(KEY_ASKED_AT)?.toLongOrNull() ?: 0L
        if (last > 0 && now - last < GAP_DAYS * 86_400_000L) return
        if (isDebugBuild()) { debugLog("review prompt: would ask now (debug builds don't)"); return }
        s.putString(KEY_ASKED_AT, now.toString())
        s.putInt(KEY_ASKS, asks + 1)
        Usage.track("review_ask")
        requestStoreReview()
    }
}
