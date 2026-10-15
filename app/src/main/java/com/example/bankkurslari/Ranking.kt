package com.example.bankkurslari

import kotlin.math.roundToLong

// Spec section 8 (ranking logic). Kurs list and Xarita MUST use rankBanks() so order is identical.

/** open/closed state derived from OSM-style opening_hours text. known=false -> hours unknown (treated open but flagged).
 *  untilText: raw "HH:mm" closing time when open; next opening time today when closed. */
internal data class OpenState(val known: Boolean, val open: Boolean, val untilText: String?)

/** One ranked bank row / map pin. total = so'm the user receives (sell) or pays (buy) for `amount`. */
internal data class RankedBank(
    val rate: BankRate,
    val perUnit: Int,            // buy rate when user sells, sell rate when user buys
    val total: Long,             // perUnit * amount, rounded
    val distanceKm: Double?,     // null when user location unknown or nearest branch > 100 km away
    val address: String?,
    val hours: String?,
    val open: OpenState,
    val lat: Double?,
    val lng: Double?
)

/** Normalised key so "Asakabank" == "ASAKABANK" == "Asaka Bank" etc. */
internal fun bankKey(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

private fun keysMatch(ka: String, kb: String): Boolean {
    if (ka.isEmpty() || kb.isEmpty()) return false
    if (ka == kb) return true
    val (s, l) = if (ka.length <= kb.length) ka to kb else kb to ka
    return s.length >= 5 && l.contains(s)
}

/** Find the BankRate that belongs to an OSM/ATM bank name (fuzzy, via bankKey). */
internal fun findRate(banks: List<BankRate>, osmBankName: String): BankRate? {
    val k = bankKey(osmBankName)
    return banks.firstOrNull { bankKey(it.bank) == k } ?: banks.firstOrNull { keysMatch(bankKey(it.bank), k) }
}

private const val MAX_KM = 100.0

private fun kmBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

private class Cand(val lat: Double, val lng: Double, val address: String?, val hours: String?)

internal fun rankBanks(
    banks: List<BankRate>,
    direction: ConvertMode,
    amount: Double,
    user: Pair<Double, Double>?,
    osmBranches: List<OsmBranch>,
    nearMode: Boolean = false,
    kPerKm: Int = 1000
): List<RankedBank> {
    val sell = direction == ConvertMode.SELL_USD
    val orderAmount = if (amount > 0) amount else 1.0
    val osmKeyed = osmBranches.map { bankKey(it.bank) to it }

    class Row(val item: RankedBank, val sortValue: Double)

    val rows = banks.mapNotNull { r ->
        val per = (if (sell) r.buy else r.sell) ?: return@mapNotNull null
        if (per <= 0) return@mapNotNull null
        val bk = bankKey(r.bank)
        val cands = ArrayList<Cand>()
        for ((k, ob) in osmKeyed) if (keysMatch(bk, k)) cands += Cand(ob.lat, ob.lng, ob.address, ob.hours)
        r.branch?.let { cands += Cand(it.lat, it.lng, it.address, null) }

        val chosen: Cand?
        var km: Double? = null
        if (user != null && cands.isNotEmpty()) {
            var best: Cand? = null
            var bestKm = Double.MAX_VALUE
            for (c in cands) {
                val d = kmBetween(user.first, user.second, c.lat, c.lng)
                if (d < bestKm) { bestKm = d; best = c }
            }
            chosen = best
            km = if (bestKm > MAX_KM) null else bestKm
        } else {
            chosen = cands.firstOrNull { it.hours != null } ?: cands.firstOrNull()
        }
        val hours = chosen?.hours
        val total = (per * amount).roundToLong()
        val item = RankedBank(
            rate = r, perUnit = per, total = total, distanceKm = km,
            address = chosen?.address ?: r.branch?.address,
            hours = hours, open = openState(hours),
            lat = chosen?.lat, lng = chosen?.lng
        )
        val base = per * orderAmount
        val pen = if (nearMode && km != null) kPerKm * km else 0.0
        // lower sortValue = better
        Row(item, if (sell) -(base - pen) else (base + pen))
    }

    return rows.sortedWith(
        compareBy<Row> { it.sortValue }
            .thenBy { it.item.distanceKm ?: Double.MAX_VALUE }
    ).map { it.item }
}
