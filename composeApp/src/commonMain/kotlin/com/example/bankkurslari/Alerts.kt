package com.example.bankkurslari

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

/**
 * Rate alert (spec 6.3). Direction is in the user's voice:
 *  - SELL: "I sell FX". Relevant rate = bank BUY rate, fires when it is >= threshold.
 *  - BUY:  "I buy FX".  Relevant rate = bank SELL rate, fires when it is <= threshold.
 * [bank] == null means "market best" (max bank buy for SELL, min bank sell for BUY).
 */
enum class AlertSide { SELL, BUY }

data class RateAlert(
    val id: String,
    val currency: String,          // "USD" | "EUR" | "RUB"
    val side: AlertSide,
    val threshold: Int,            // so'm per 1 unit
    val bank: String?,             // null = market best
    val fired: Boolean = false,    // true while the condition holds and the user was already notified
    val lastFiredAt: Long = 0L     // epoch millis, 0 = never
)

object Alerts {
    const val MAX = 5

    private val lock = PlatformLock()

    fun load(): List<RateAlert> = parse(RateService.alertsJson())

    /** Atomic read-modify-write so the worker and the UI never overwrite each other. */
    fun update(transform: (List<RateAlert>) -> List<RateAlert>): List<RateAlert> =
        lock.withLock {
            val next = transform(load())
            RateService.setAlertsJson(serialize(next))
            next
        }

    fun add(alert: RateAlert): Boolean {
        var added = false
        update { list ->
            if (list.size >= MAX) list else { added = true; list + alert }
        }
        return added
    }

    fun remove(id: String) {
        update { list -> list.filterNot { it.id == id } }
    }

    fun newId(): String = nowMillis().toString(36) + (0..999).random()

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
            JsonLenient.parseToJsonElement(json).jsonArray.objects().map { o ->
                RateAlert(
                    id = o.str("id") ?: error("id"),
                    currency = o.str("currency") ?: error("currency"),
                    side = if (o.str("side") == "BUY") AlertSide.BUY else AlertSide.SELL,
                    threshold = o.int("threshold") ?: error("threshold"),
                    bank = o.str("bank")?.ifEmpty { null },
                    fired = o.bool("fired") ?: false,
                    lastFiredAt = o.long("lastFiredAt") ?: 0L
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serialize(list: List<RateAlert>): String =
        JsonArray(list.map { a ->
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(a.id),
                    "currency" to JsonPrimitive(a.currency),
                    "side" to JsonPrimitive(a.side.name),
                    "threshold" to JsonPrimitive(a.threshold),
                    "bank" to (a.bank?.let { JsonPrimitive(it) } ?: JsonNull),
                    "fired" to JsonPrimitive(a.fired),
                    "lastFiredAt" to JsonPrimitive(a.lastFiredAt)
                )
            )
        }).toString()
}
