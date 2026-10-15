package com.example.bankkurslari

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Lightweight rate fetch + preference store shared by the widget and the background worker. */
object RateService {
    private val BASE = BuildConfig.SERVER_BASE
    private const val PREFS = "valyutastat"
    private const val KEY_CURRENCY = "currency"
    private fun lastRateKey(cur: String) = "last_rate_$cur"

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

    /** Blocking: raw JSON of /api/rates/<cur>. */
    fun fetchRatesBody(currency: String): String {
        val url = "$BASE/api/rates/${currency.lowercase()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 15000
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun snapshotFromBody(currency: String, body: String): Snapshot {
        val cbu = JSONObject(body).getJSONObject("cbu")
        return Snapshot(
            currency = currency,
            rate = cbu.getDouble("rate"),
            diff = cbu.optDouble("diff", 0.0),
            date = cbu.optString("date")
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
            fetchedAt = System.currentTimeMillis()
        )
    }

    /** Blocking network call (CBU reference rate) — must run off the main thread. */
    fun fetchRate(currency: String): Snapshot = snapshotFromBody(currency, fetchRatesBody(currency))

    /** Blocking: best bank buy/sell for [currency] from the /api/rates bank list. */
    fun fetchBest(currency: String): Best = bestFromBody(currency, fetchRatesBody(currency))

    fun saveBest(context: Context, best: Best) {
        val json = JSONObject()
            .put("sellRate", best.sellRate ?: JSONObject.NULL).put("sellBank", best.sellBank ?: JSONObject.NULL)
            .put("buyRate", best.buyRate ?: JSONObject.NULL).put("buyBank", best.buyBank ?: JSONObject.NULL)
            .put("updatedAt", best.updatedAt).put("fetchedAt", best.fetchedAt)
        prefs(context).edit().putString("best_${best.currency}", json.toString()).apply()
    }

    fun loadBest(context: Context, currency: String): Best? {
        val raw = prefs(context).getString("best_$currency", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            Best(
                currency = currency,
                sellRate = if (o.isNull("sellRate")) null else o.getInt("sellRate"),
                sellBank = if (o.isNull("sellBank")) null else o.getString("sellBank"),
                buyRate = if (o.isNull("buyRate")) null else o.getInt("buyRate"),
                buyBank = if (o.isNull("buyBank")) null else o.getString("buyBank"),
                updatedAt = o.optString("updatedAt"),
                fetchedAt = o.optLong("fetchedAt", 0L)
            )
        }.getOrNull()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun selectedCurrency(context: Context): String =
        prefs(context).getString(KEY_CURRENCY, "USD") ?: "USD"

    /** Returns the saved dark-mode choice, or null when the user hasn't chosen (follow system). */
    fun darkMode(context: Context): Boolean? {
        val v = prefs(context).getInt("dark_mode", -1)
        return if (v == -1) null else v == 1
    }

    fun setDarkMode(context: Context, dark: Boolean) {
        prefs(context).edit().putInt("dark_mode", if (dark) 1 else 0).putInt("theme_mode", if (dark) 2 else 1).apply()
    }

    /** Theme choice: 0 = system, 1 = light, 2 = dark. Falls back to the legacy "dark_mode" key
     *  (-1 system, 0 light, 1 dark) until the new key is written. */
    fun themeMode(context: Context): Int {
        val p = prefs(context)
        if (p.contains("theme_mode")) return p.getInt("theme_mode", 0).coerceIn(0, 2)
        return when (p.getInt("dark_mode", -1)) { 1 -> 2; 0 -> 1; else -> 0 }
    }

    fun setThemeMode(context: Context, mode: Int) {
        val legacy = when (mode) { 2 -> 1; 1 -> 0; else -> -1 }
        prefs(context).edit().putInt("theme_mode", mode.coerceIn(0, 2)).putInt("dark_mode", legacy).apply()
    }

    /** City name in Uzbek (key), empty when not chosen. */
    fun city(context: Context): String = prefs(context).getString("city", "") ?: ""

    fun setCity(context: Context, city: String) {
        prefs(context).edit().putString("city", city).apply()
    }

    /** Raw JSON array of alerts; use [Alerts] to read/write typed. */
    fun alertsJson(context: Context): String? = prefs(context).getString("alerts_json", null)

    fun setAlertsJson(context: Context, json: String) {
        prefs(context).edit().putString("alerts_json", json).apply()
    }

    fun language(context: Context): String =
        prefs(context).getString("lang", "uz") ?: "uz"

    fun setLanguage(context: Context, code: String) {
        prefs(context).edit().putString("lang", code).apply()
    }

    fun setSelectedCurrency(context: Context, currency: String) {
        prefs(context).edit().putString(KEY_CURRENCY, currency).apply()
    }

    // --- Favorite banks ---
    fun favorites(context: Context): Set<String> =
        prefs(context).getStringSet("fav_banks", emptySet())?.toSet() ?: emptySet()

    fun toggleFavorite(context: Context, bank: String): Set<String> {
        val cur = favorites(context).toMutableSet()
        if (!cur.add(bank)) cur.remove(bank)
        prefs(context).edit().putStringSet("fav_banks", cur).apply()
        return cur
    }

    // --- Generic JSON cache (offline support) ---
    fun saveCache(context: Context, key: String, json: String) {
        prefs(context).edit().putString("cache_$key", json).apply()
    }

    fun loadCache(context: Context, key: String): String? =
        prefs(context).getString("cache_$key", null)

    fun lastRate(context: Context, currency: String): Double =
        prefs(context).getFloat(lastRateKey(currency), Float.NaN).toDouble()

    fun saveSnapshot(context: Context, snap: Snapshot) {
        prefs(context).edit()
            .putFloat(lastRateKey(snap.currency), snap.rate.toFloat())
            .putFloat("diff_${snap.currency}", snap.diff.toFloat())
            .putString("date_${snap.currency}", snap.date)
            .apply()
    }

    fun loadSnapshot(context: Context, currency: String): Snapshot? {
        val p = prefs(context)
        val rate = p.getFloat(lastRateKey(currency), Float.NaN).toDouble()
        if (rate.isNaN()) return null
        return Snapshot(
            currency = currency,
            rate = rate,
            diff = p.getFloat("diff_$currency", 0f).toDouble(),
            date = p.getString("date_$currency", "") ?: ""
        )
    }

    fun formatRate(rate: Double): String {
        val s = java.text.DecimalFormat("#,##0.00").format(rate)
        return s.replace(",", " ")
    }
}
