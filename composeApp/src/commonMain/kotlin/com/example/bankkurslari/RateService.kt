package com.example.bankkurslari

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Rate fetch + preference store shared by the app, the widget and the background worker. */
object RateService {
    private const val KEY_CURRENCY = "currency"
    private fun lastRateKey(cur: String) = "last_rate_$cur"
    private val prefs: KeyValueStore get() = appStore()

    val currencies = listOf("USD", "EUR", "RUB")

    fun symbol(cur: String): String = when (cur) {
        "EUR" -> "€"; "RUB" -> "₽"; else -> "$"
    }

    data class Snapshot(val currency: String, val rate: Double, val diff: Double, val date: String)

    /** Best bank rates in the user's voice. [sellRate]: what you get per unit when you SELL FX (highest bank buy rate).
     *  [buyRate]: what you pay per unit when you BUY FX (lowest bank sell rate). */
    data class Best(
        val currency: String,
        val sellRate: Int?, val sellBank: String?,
        val buyRate: Int?, val buyBank: String?,
        val updatedAt: String,   // "HH:mm" or ""
        val fetchedAt: Long
    )

    /** Raw JSON of /api/rates/<cur>. */
    suspend fun fetchRatesBody(currency: String): String = httpGet("$SERVER_BASE/api/rates/${currency.lowercase()}", 15_000)

    fun snapshotFromBody(currency: String, body: String): Snapshot {
        val cbu = parseJsonObject(body).obj("cbu") ?: error("no cbu")
        return Snapshot(
            currency = currency,
            rate = cbu.dbl("rate") ?: error("no rate"),
            diff = cbu.dbl("diff") ?: 0.0,
            date = cbu.str("date").orEmpty()
        )
    }

    fun bestFromBody(currency: String, body: String): Best {
        val payload = parseRatesPayload(body)
        val sell = payload.banks.filter { it.buy != null }.maxByOrNull { it.buy!! }
        val buy = payload.banks.filter { it.sell != null }.minByOrNull { it.sell!! }
        return Best(
            currency = currency,
            sellRate = sell?.buy, sellBank = sell?.bank,
            buyRate = buy?.sell, buyBank = buy?.bank,
            updatedAt = payload.updatedAt?.formatUpdatedAt() ?: "",
            fetchedAt = nowMillis()
        )
    }

    suspend fun fetchRate(currency: String): Snapshot = snapshotFromBody(currency, fetchRatesBody(currency))

    suspend fun fetchBest(currency: String): Best = bestFromBody(currency, fetchRatesBody(currency))

    fun saveBest(best: Best) {
        val json = JsonObject(
            mapOf(
                "sellRate" to (best.sellRate?.let { JsonPrimitive(it) } ?: JsonNull),
                "sellBank" to (best.sellBank?.let { JsonPrimitive(it) } ?: JsonNull),
                "buyRate" to (best.buyRate?.let { JsonPrimitive(it) } ?: JsonNull),
                "buyBank" to (best.buyBank?.let { JsonPrimitive(it) } ?: JsonNull),
                "updatedAt" to JsonPrimitive(best.updatedAt),
                "fetchedAt" to JsonPrimitive(best.fetchedAt)
            )
        )
        prefs.putString("best_${best.currency}", json.toString())
    }

    fun loadBest(currency: String): Best? {
        val raw = prefs.getString("best_$currency") ?: return null
        return runCatching {
            val o = parseJsonObject(raw)
            Best(
                currency = currency,
                sellRate = o.int("sellRate"), sellBank = o.str("sellBank"),
                buyRate = o.int("buyRate"), buyBank = o.str("buyBank"),
                updatedAt = o.str("updatedAt").orEmpty(),
                fetchedAt = o.long("fetchedAt") ?: 0L
            )
        }.getOrNull()
    }

    fun selectedCurrency(): String = prefs.getString(KEY_CURRENCY) ?: "USD"

    fun setSelectedCurrency(currency: String) = prefs.putString(KEY_CURRENCY, currency)

    /** Theme choice: 0 = system, 1 = light, 2 = dark. Falls back to the legacy "dark_mode" key
     *  (-1 system, 0 light, 1 dark) until the new key is written. */
    fun themeMode(): Int {
        if (prefs.contains("theme_mode")) return prefs.getInt("theme_mode", 0).coerceIn(0, 2)
        return when (prefs.getInt("dark_mode", -1)) { 1 -> 2; 0 -> 1; else -> 0 }
    }

    fun setThemeMode(mode: Int) {
        val legacy = when (mode) { 2 -> 1; 1 -> 0; else -> -1 }
        prefs.putInt("theme_mode", mode.coerceIn(0, 2))
        prefs.putInt("dark_mode", legacy)
    }

    /** City name in Uzbek (key), empty when not chosen. */
    fun city(): String = prefs.getString("city").orEmpty()

    fun setCity(city: String) = prefs.putString("city", city)

    /** Raw JSON array of alerts; use [Alerts] to read/write typed. */
    fun alertsJson(): String? = prefs.getString("alerts_json")

    fun setAlertsJson(json: String) = prefs.putString("alerts_json", json)

    /** Saved choice, else the device language (no language step on first launch). */
    fun language(): String = prefs.getString("lang") ?: deviceLanguageTag().lowercase().let { t ->
        when {
            t.startsWith("ru") -> "ru"
            t.startsWith("en") -> "en"
            t.startsWith("uz") && "cyrl" in t -> "uz-cyrl"
            else -> "uz"
        }
    }

    fun setLanguage(code: String) = prefs.putString("lang", code)

    // --- Favorite banks ---
    fun favorites(): Set<String> = prefs.getStringSet("fav_banks")

    fun toggleFavorite(bank: String): Set<String> {
        val cur = favorites().toMutableSet()
        if (!cur.add(bank)) cur.remove(bank)
        prefs.putStringSet("fav_banks", cur)
        return cur
    }

    // --- Generic JSON cache (offline support): files, see [writeCacheFile] ---
    fun saveCache(key: String, json: String) = writeCacheFile("$key.json", json)

    fun loadCache(key: String): String? = readCacheFile("$key.json")

    /** NaN when never stored. */
    fun lastRate(currency: String): Double = prefs.getDouble(lastRateKey(currency)) ?: Double.NaN

    fun saveSnapshot(snap: Snapshot) {
        prefs.putDouble(lastRateKey(snap.currency), snap.rate)
        prefs.putDouble("diff_${snap.currency}", snap.diff)
        prefs.putString("date_${snap.currency}", snap.date)
    }

    fun loadSnapshot(currency: String): Snapshot? {
        val rate = prefs.getDouble(lastRateKey(currency)) ?: return null
        return Snapshot(
            currency = currency,
            rate = rate,
            diff = prefs.getDouble("diff_$currency") ?: 0.0,
            date = prefs.getString("date_$currency").orEmpty()
        )
    }

    fun formatRate(rate: Double): String = rate.formatRate()
}
