package com.example.bankkurslari

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Rate alert (spec 6.3). Direction is in the user's voice:
 *  - SELL: "I sell FX". Relevant rate = bank BUY rate, fires when it is >= threshold.
 *  - BUY:  "I buy FX".  Relevant rate = bank SELL rate, fires when it is <= threshold.
 * [bank] == null means "market best" (max bank buy for SELL, min bank sell for BUY).
 */
internal enum class AlertSide { SELL, BUY }

internal data class RateAlert(
    val id: String,
    val currency: String,          // "USD" | "EUR" | "RUB"
    val side: AlertSide,
    val threshold: Int,            // so'm per 1 unit
    val bank: String?,             // null = market best
    val fired: Boolean = false,    // true while the condition holds and the user was already notified
    val lastFiredAt: Long = 0L     // epoch millis, 0 = never
)

internal object Alerts {
    const val MAX = 5

    private val lock = Any()

    fun load(context: Context): List<RateAlert> = parse(RateService.alertsJson(context))

    /** Atomic read-modify-write so the worker and the UI never overwrite each other. */
    fun update(context: Context, transform: (List<RateAlert>) -> List<RateAlert>): List<RateAlert> =
        synchronized(lock) {
            val next = transform(load(context))
            RateService.setAlertsJson(context, serialize(next))
            next
        }

    fun add(context: Context, alert: RateAlert): Boolean {
        var added = false
        update(context) { list ->
            if (list.size >= MAX) list else { added = true; list + alert }
        }
        return added
    }

    fun remove(context: Context, id: String) {
        update(context) { list -> list.filterNot { it.id == id } }
    }

    fun newId(): String = System.currentTimeMillis().toString(36) + (0..999).random()

    /** The rate this alert watches, or null when there is no matching bank rate. */
    fun currentValue(alert: RateAlert, banks: List<BankRate>): Int? = bestFor(alert, banks)?.second

    /** (bank, rate) that currently decides the alert. */
    fun bestFor(alert: RateAlert, banks: List<BankRate>): Pair<String, Int>? {
        val rows = banks.filter { alert.bank == null || it.bank == alert.bank }
        return when (alert.side) {
            AlertSide.SELL -> rows.mapNotNull { r -> r.buy?.let { r.bank to it } }.maxByOrNull { it.second }
            AlertSide.BUY -> rows.mapNotNull { r -> r.sell?.let { r.bank to it } }.minByOrNull { it.second }
        }
    }

    fun isMet(side: AlertSide, value: Int, threshold: Int): Boolean =
        if (side == AlertSide.SELL) value >= threshold else value <= threshold

    private fun parse(json: String?): List<RateAlert> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        RateAlert(
                            id = o.getString("id"),
                            currency = o.getString("currency"),
                            side = if (o.optString("side") == "BUY") AlertSide.BUY else AlertSide.SELL,
                            threshold = o.getInt("threshold"),
                            bank = if (o.isNull("bank")) null else o.optString("bank").ifEmpty { null },
                            fired = o.optBoolean("fired", false),
                            lastFiredAt = o.optLong("lastFiredAt", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serialize(list: List<RateAlert>): String {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("currency", a.currency)
                    .put("side", a.side.name)
                    .put("threshold", a.threshold)
                    .put("bank", a.bank ?: JSONObject.NULL)
                    .put("fired", a.fired)
                    .put("lastFiredAt", a.lastFiredAt)
            )
        }
        return arr.toString()
    }
}
