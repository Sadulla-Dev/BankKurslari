package com.example.bankkurslari

/**
 * Home-screen widget data, shared by the Android widget (the iOS widget computes the same from the API in Swift).
 * Two leaderboards like the Telegram post: where selling pays most (bank buy rate, highest first) and where buying
 * costs least (bank sell rate, lowest first). Equal rates share a place (1, 1, 3, …).
 */
data class WidgetEntry(
    val bank: String,
    val short: String,
    val rate: Int,
    val place: Int,
    /** So'm worse than the leader for 1 unit (0 for the leaders). */
    val gap: Int,
    /** Drawable file stem of the bank logo (e.g. "bank_nbu"), or null. */
    val logo: String?
)

data class WidgetModel(
    val currency: String,
    val cbuRate: Double,
    val cbuDiff: Double,
    val updatedAt: String?,
    val sell: List<WidgetEntry>,
    val buy: List<WidgetEntry>,
    val bankCount: Int
)

fun widgetModel(currency: String, payload: RatesPayload): WidgetModel {
    fun rank(rated: List<Pair<BankRate, Int>>, higherIsBetter: Boolean): List<WidgetEntry> {
        val sorted = rated.sortedWith(compareBy<Pair<BankRate, Int>> { if (higherIsBetter) -it.second else it.second }.thenBy { bankShortName(it.first.bank) })
        val best = sorted.firstOrNull()?.second ?: return emptyList()
        var place = 0
        return sorted.mapIndexed { i, (b, r) ->
            if (i == 0 || r != sorted[i - 1].second) place = i + 1
            WidgetEntry(b.bank, bankShortName(b.bank), r, place, kotlin.math.abs(r - best), bankLogoName(b.bank))
        }
    }
    val sell = rank(payload.banks.mapNotNull { b -> b.buy?.let { b to it } }, higherIsBetter = true)
    val buy = rank(payload.banks.mapNotNull { b -> b.sell?.let { b to it } }, higherIsBetter = false)
    return WidgetModel(currency, payload.cbu.rate, payload.cbu.diff, payload.updatedAt, sell, buy, payload.banks.size)
}

/** Names as people say them on a small screen: "O‘zbekiston Milliy banki" → "Milliy bank", "Asia Alliance Bank" → "Asia Alliance". */
fun bankShortName(name: String): String {
    val key = name.lowercase().filter(Char::isLetterOrDigit)
    return when {
        "milliy" in key -> "Milliy bank"
        "sanoatqurilish" in key -> "SQB"
        "mikrokredit" in key || "mkbank" in key -> "MKBank"
        "asiaalliance" in key -> "Asia Alliance"
        "orientfinans" in key -> "Orient Finans"
        "ipakyuli" in key -> "Ipak Yo‘li"
        key.startsWith("kdb") -> "KDB Bank"
        key.startsWith("apex") -> "Apex Bank"
        else -> name.replace(Regex("\\s+(Bank|bank|banki|Banki)$"), "").trim().ifEmpty { name }
    }
}

/** Same mapping as the app's logo badges (DesignSystem.bankLogoResource), as a drawable file stem. */
fun bankLogoName(name: String): String? {
    val key = name.lowercase().filter(Char::isLetterOrDigit)
    return when {
        "anor" in key -> "bank_anorbank"
        "hayot" in key -> "bank_hayot_bank"
        "tenge" in key -> "bank_tenge_bank"
        "infin" in key -> "bank_infinbank"
        "hamkor" in key -> "bank_hamkorbank"
        "agro" in key -> "bank_agrobank"
        "trast" in key || "trust" in key -> "bank_trastbank"
        "universal" in key -> "bank_universal_bank"
        "poytaxt" in key -> "bank_poytaxt_bank"
        "ipoteka" in key -> "bank_ipoteka_bank"
        "asiaalliance" in key -> "bank_asia_alliance"
        "aloqa" in key -> "bank_aloqabank"
        "garant" in key -> "bank_garant_bank"
        "turon" in key -> "bank_turonbank"
        "orientfinans" in key || "orientfinance" in key -> "bank_orient_finans"
        "sanoatqurilish" in key || key == "sqb" -> "bank_sqb"
        "mkbank" in key || "mikrokredit" in key -> "bank_mkbank"
        "xalq" in key -> "bank_xalq_bank"
        "ipakyuli" in key -> "bank_ipak_yuli"
        key == "brb" -> "bank_brb"
        "asaka" in key -> "bank_asakabank"
        "davr" in key -> "bank_davrbank"
        "apex" in key -> "bank_apexbank"
        "milliy" in key || key == "nbu" -> "bank_nbu"
        "openbank" in key -> "bank_openbank"
        "ziraat" in key -> "bank_ziraat_bank"
        "octo" in key -> "bank_octobank"
        key.startsWith("kdb") -> "bank_kdb_bank"
        "kapital" in key -> "bank_kapitalbank"
        else -> null
    }
}
