package com.example.bankkurslari

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlin.math.abs
import kotlinx.datetime.minus
import kotlin.math.roundToLong

/*
 * Wallet ("Hamyon"): what the user's savings are worth in so'm today, and currency-to-currency exchange through so'm.
 * Everything here is plain data + arithmetic so the web page (web/public/wallet.js) can mirror it line for line.
 */

/** Codes the wallet understands. UZS is cash in so'm, XAU is CBU gold bars in grams. */
enum class WalletCode(val code: String, val symbol: String, val nameUz: String, val nameRu: String, val nameEn: String, val decimals: Int) {
    USD("USD", "$", "Dollar", "Доллар", "Dollar", 2),
    EUR("EUR", "€", "Yevro", "Евро", "Euro", 2),
    RUB("RUB", "₽", "Rubl", "Рубль", "Ruble", 0),
    GBP("GBP", "£", "Funt", "Фунт", "Pound", 2),
    KZT("KZT", "₸", "Tenge", "Тенге", "Tenge", 0),
    UZS("UZS", "soʻm", "Soʻm", "Сум", "Som", 0),
    XAU("XAU", "Au", "Oltin, gramm", "Золото, грамм", "Gold, grams", 1);

    fun displayName(lang: Lang) = pick(lang, nameUz, nameRu, nameEn)

    /** Bank cash rates exist for these; UZS needs none, gold comes from the CBU bar prices. */
    val hasBankRates: Boolean get() = this != UZS && this != XAU

    companion object {
        fun of(code: String): WalletCode? = entries.firstOrNull { it.code == code }
        /** What you can exchange at a bank counter. */
        val exchangeable: List<WalletCode> get() = listOf(USD, EUR, RUB, GBP, KZT, UZS)
    }
}

data class Holding(val code: WalletCode, val amount: Double)

/** One bank's cash rates for a currency; buy = what the bank pays you, sell = what you pay the bank. */
data class QuoteBank(val bank: String, val buy: Double?, val sell: Double?)

/** Market for one currency, in so'm per 1 unit. */
data class Quote(
    val code: WalletCode,
    val cbu: Double,
    val cbuDiff: Double,
    val banks: List<QuoteBank>,
    val updatedAt: String?
) {
    val bestBuy: QuoteBank? get() = banks.filter { it.buy != null && it.buy > 0 }.maxByOrNull { it.buy!! }
    val bestSell: QuoteBank? get() = banks.filter { it.sell != null && it.sell > 0 }.minByOrNull { it.sell!! }
}

/* ---------- storage ---------- */

object WalletStore {
    private const val KEY = "wallet_v1"

    fun load(): List<Holding> = appStore().getString(KEY).orEmpty().split(';').mapNotNull { part ->
        val (c, a) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        val code = WalletCode.of(c) ?: return@mapNotNull null
        val amount = a.toDoubleOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        Holding(code, amount)
    }

    fun save(list: List<Holding>) {
        appStore().putString(KEY, list.filter { it.amount > 0 }.joinToString(";") { "${it.code.code}=${it.amount}" })
    }

    /** Adds to an existing line of the same currency instead of making a second one. */
    fun upsert(list: List<Holding>, h: Holding, replace: Boolean): List<Holding> {
        val i = list.indexOfFirst { it.code == h.code }
        val next = when {
            i < 0 -> list + h
            replace -> list.toMutableList().also { it[i] = h }
            else -> list.toMutableList().also { it[i] = Holding(h.code, it[i].amount + h.amount) }
        }
        return next.filter { it.amount > 0 }
    }

    fun exchangeFrom(): String = appStore().getString("xchg_from") ?: "USD"
    fun exchangeTo(): String = appStore().getString("xchg_to") ?: "EUR"
    fun exchangeAmount(): String = appStore().getString("xchg_amount") ?: "1000"
    fun saveExchange(from: String, to: String, amount: String) {
        appStore().putString("xchg_from", from)
        appStore().putString("xchg_to", to)
        appStore().putString("xchg_amount", amount)
    }
}

/* ---------- data ---------- */

/** Same cache keys as the rest of the app, so a rate fetched on the Kurs tab is reused here and vice versa. */
private fun cacheKey(code: WalletCode) = if (code in setOf(WalletCode.USD, WalletCode.EUR, WalletCode.RUB)) "rates_${code.code}" else "rates_x_${code.code}"

fun parseQuote(code: WalletCode, body: String): Quote {
    val root = parseJsonObject(body)
    val cbu = root.obj("cbu") ?: error("no cbu")
    val banks = (root.arr("banks") ?: JsonArray(emptyList())).objects().mapNotNull { b ->
        val buy = b.dbl("buy")
        val sell = b.dbl("sell")
        if (buy == null && sell == null) null else QuoteBank(b.str("bank") ?: return@mapNotNull null, buy, sell)
    }
    return Quote(code, cbu.dbl("rate") ?: error("no rate"), cbu.dbl("diff") ?: 0.0, banks, root.str("updatedAt"))
}

/** Gold as a quote: CBU buys back a 10 g bar for X (per gram X/10) and sells it for Y. One "bank": the CBU. */
fun goldQuote(g: GoldPrices): Quote? {
    val bar = g.bars.firstOrNull { it.grams == 10.0 } ?: g.bars.minByOrNull { it.grams } ?: return null
    val buyback = bar.buyback?.let { it / bar.grams }
    val sell = bar.sell / bar.grams
    return Quote(WalletCode.XAU, sell, 0.0, listOf(QuoteBank("Markaziy bank", buyback, sell)), null)
}

suspend fun cachedQuotes(): Map<WalletCode, Quote> = withContext(Dispatchers.Default) {
    val out = mutableMapOf<WalletCode, Quote>()
    WalletCode.entries.filter { it.hasBankRates }.forEach { c ->
        RateService.loadCache(cacheKey(c))?.let { body -> runCatching { parseQuote(c, body) }.getOrNull()?.let { out[c] = it } }
    }
    cachedGold()?.let(::goldQuote)?.let { out[WalletCode.XAU] = it }
    out
}

/** Fetches every currency in parallel; a failed one is simply missing (the caller keeps its cached copy). */
suspend fun fetchQuotes(): Map<WalletCode, Quote> = withContext(Dispatchers.Default) {
    coroutineScope {
        val jobs = WalletCode.entries.filter { it.hasBankRates }.map { c ->
            async {
                runCatching {
                    val body = httpGet("$backendBase/api/rates/${c.code.lowercase()}", 15_000)
                    val q = parseQuote(c, body)
                    RateService.saveCache(cacheKey(c), body)
                    c to q
                }.getOrNull()
            }
        }
        val gold = async { runCatching { fetchGold() }.getOrNull()?.let(::goldQuote)?.let { WalletCode.XAU to it } }
        (jobs.awaitAll() + gold.await()).filterNotNull().toMap()
    }
}

/* ---------- arithmetic ---------- */

data class HoldingValue(
    val holding: Holding,
    /** So'm you get today selling at the best bank (CBU buyback for gold); null if no bank buys it. */
    val som: Double?,
    val bank: String?,
    /** Rate used, so'm per unit. */
    val rate: Double?,
    /** Change since yesterday at the CBU rate, in so'm. */
    val dayChange: Double
)

data class WalletSummary(val rows: List<HoldingValue>, val total: Double, val dayChange: Double, val totalUsd: Double?)

fun walletSummary(list: List<Holding>, quotes: Map<WalletCode, Quote>): WalletSummary {
    val rows = list.map { h ->
        if (h.code == WalletCode.UZS) return@map HoldingValue(h, h.amount, null, 1.0, 0.0)
        val q = quotes[h.code] ?: return@map HoldingValue(h, null, null, null, 0.0)
        val best = q.bestBuy
        HoldingValue(h, best?.buy?.let { it * h.amount }, best?.bank, best?.buy, q.cbuDiff * h.amount)
    }
    val total = rows.sumOf { it.som ?: 0.0 }
    val usd = quotes[WalletCode.USD]?.cbu?.takeIf { it > 0 }?.let { total / it }
    return WalletSummary(rows, total, rows.sumOf { it.dayChange }, usd)
}

/** Result of exchanging [amount] of [from] into [to] through so'm. */
data class Exchange(
    val from: WalletCode,
    val to: WalletCode,
    val amount: Double,
    /** Best route: sell at one bank, buy at another (may be the same bank). */
    val result: Double,
    val sellBank: String?,
    val sellRate: Double?,
    val buyBank: String?,
    val buyRate: Double?,
    val som: Double,
    /** Best when both steps happen at one bank (one trip). */
    val oneBank: String?,
    val oneBankResult: Double?,
    /** At the CBU cross rate: the fair value, for showing what the banks keep. */
    val cbuResult: Double?
) {
    /** Share of the CBU value lost to bank spreads, 0.012 = 1.2 %. */
    val loss: Double? get() = cbuResult?.takeIf { it > 0 }?.let { (it - result) / it }
}

fun exchange(from: WalletCode, to: WalletCode, amount: Double, quotes: Map<WalletCode, Quote>): Exchange? {
    if (amount <= 0 || from == to) return null
    val qf = if (from == WalletCode.UZS) null else quotes[from] ?: return null
    val qt = if (to == WalletCode.UZS) null else quotes[to] ?: return null
    val sell = qf?.bestBuy
    val buy = qt?.bestSell
    if (qf != null && sell == null) return null
    if (qt != null && buy == null) return null
    val som = if (qf == null) amount else amount * sell!!.buy!!
    val result = if (qt == null) som else som / buy!!.sell!!

    // One bank doing both legs (only meaningful when neither side is so'm).
    var oneBank: String? = null
    var oneResult: Double? = null
    if (qf != null && qt != null) {
        val sells = qt.banks.associateBy { it.bank }
        qf.banks.forEach { b ->
            val bb = b.buy ?: return@forEach
            val s = sells[b.bank]?.sell ?: return@forEach
            if (bb <= 0 || s <= 0) return@forEach
            val r = amount * bb / s
            if (oneResult == null || r > oneResult!!) { oneResult = r; oneBank = b.bank }
        }
    }
    val cbuFrom = if (qf == null) 1.0 else qf.cbu
    val cbuTo = if (qt == null) 1.0 else qt.cbu
    val cbu = if (cbuFrom > 0 && cbuTo > 0) amount * cbuFrom / cbuTo else null
    return Exchange(from, to, amount, result, sell?.bank, sell?.buy, buy?.bank, buy?.sell, som, oneBank, oneResult, cbu)
}

/* ---------- formatting ---------- */

/** 1234.5 -> "1 234.50" with the code's decimals; whole numbers drop the fraction. */
fun fmtAmount(v: Double, decimals: Int): String {
    if (decimals == 0 || abs(v - v.roundToLong()) < 0.005) return (v.roundToLong()).formatUzsL()
    val s = abs(v).fixed(decimals)
    val (i, f) = s.split('.').let { it[0] to it.getOrElse(1) { "" } }
    return (if (v < 0) "-" else "") + groupDigits(i) + if (f.isNotEmpty()) ".$f" else ""
}

/** Rate per unit: whole so'm when big (11 995), two decimals when small (26.10 for KZT). */
fun fmtUnitRate(v: Double): String = if (v >= 1000) v.roundToLong().formatUzsL() else v.fixed(2)

/** Parses "1 234,5" or "1234.5". */
fun parseAmount(text: String): Double? = text.replace(" ", "").replace(',', '.').toDoubleOrNull()

/* ---------- goal ---------- */

/** A savings target, e.g. 20 000 USD "Mashina". Stored as "USD|20000|Mashina". */
data class WalletGoal(val code: WalletCode, val amount: Double, val name: String)

object GoalStore {
    private const val KEY = "wallet_goal_v1"
    fun load(): WalletGoal? {
        val p = appStore().getString(KEY)?.split('|')?.takeIf { it.size >= 2 } ?: return null
        val code = WalletCode.of(p[0]) ?: return null
        val amount = p[1].toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        return WalletGoal(code, amount, p.drop(2).joinToString("|").trim())
    }
    fun save(g: WalletGoal?) = appStore().putString(KEY, g?.let { "${it.code.code}|${it.amount}|${it.name.replace('|', ' ').take(40)}" })
}

/** Codes a goal can be set in: what people save towards. */
val GOAL_CODES = listOf(WalletCode.USD, WalletCode.EUR, WalletCode.UZS)

data class GoalProgress(val saved: Double, val target: Double, val share: Double)

/** Wallet total (so'm at bank rates) expressed in the goal's currency at the CBU rate. */
fun goalProgress(goal: WalletGoal, totalSom: Double, quotes: Map<WalletCode, Quote>): GoalProgress? {
    val rate = if (goal.code == WalletCode.UZS) 1.0 else quotes[goal.code]?.cbu?.takeIf { it > 0 } ?: return null
    val saved = totalSom / rate
    return GoalProgress(saved, goal.amount, (saved / goal.amount).coerceIn(0.0, 1.0))
}

/* ---------- trend ---------- */

/**
 * The wallet's value in so'm at CBU rates over the last [days] days: one point per date any held currency has a
 * rate for, each currency at its latest rate on or before that date. Currencies without a history (GBP, KZT) count
 * at today's rate, so they add to the level but not to the change. The last point is today at today's CBU rates.
 */
fun walletTrend(
    list: List<Holding>,
    histories: Map<WalletCode, List<HistoryPoint>>,
    quotes: Map<WalletCode, Quote>,
    since: String,
    today: String
): List<HistoryPoint> {
    if (list.isEmpty()) return emptyList()
    val series = list.associate { h -> h.code to histories[h.code].orEmpty().sortedBy { it.date } }
    val dates = series.values.flatMap { s -> s.map { it.date } }.filter { it >= since && it < today }.toSortedSet()
    if (dates.isEmpty()) return emptyList()
    fun rateOn(code: WalletCode, date: String): Double? {
        if (code == WalletCode.UZS) return 1.0
        val s = series[code].orEmpty()
        if (s.isEmpty()) return quotes[code]?.cbu
        return (s.lastOrNull { it.date <= date } ?: s.first()).rate
    }
    val points = dates.mapNotNull { d ->
        var sum = 0.0
        for (h in list) sum += (rateOn(h.code, d) ?: return@mapNotNull null) * h.amount
        HistoryPoint(d, sum)
    }.toMutableList()
    val now = list.sumOf { h -> (if (h.code == WalletCode.UZS) 1.0 else quotes[h.code]?.cbu ?: return emptyList()) * h.amount }
    points += HistoryPoint(today, now)
    return points
}

/** Histories for the trend: CBU rates for USD/EUR/RUB (cache first), gold per gram from the CBU bar prices. */
suspend fun cachedWalletHistories(): Map<WalletCode, List<HistoryPoint>> = withContext(Dispatchers.Default) {
    val out = mutableMapOf<WalletCode, List<HistoryPoint>>()
    listOf(Currency.USD to WalletCode.USD, Currency.EUR to WalletCode.EUR, Currency.RUB to WalletCode.RUB).forEach { (c, w) ->
        runCatching { cachedHistory(c) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { out[w] = it }
    }
    RateService.loadCache("gold_history_wallet")?.let { body ->
        runCatching { parseWalletGold(body) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { out[WalletCode.XAU] = it }
    }
    out
}

suspend fun fetchWalletHistories(): Map<WalletCode, List<HistoryPoint>> = coroutineScope {
    val cur = listOf(Currency.USD to WalletCode.USD, Currency.EUR to WalletCode.EUR, Currency.RUB to WalletCode.RUB).map { (c, w) ->
        async { runCatching { fetchHistory(c) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { w to it } }
    }
    val gold = async {
        runCatching { fetchGoldHistory() }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { pts ->
            RateService.saveCache("gold_history_wallet", pts.joinToString(";") { "${it.first}=${it.second}" })
            WalletCode.XAU to pts.map { HistoryPoint(it.first, it.second) }
        }
    }
    (cur.awaitAll() + gold.await()).filterNotNull().toMap()
}

private fun parseWalletGold(body: String): List<HistoryPoint> = body.split(';').mapNotNull { p ->
    val (d, v) = p.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
    HistoryPoint(d, v.toDoubleOrNull() ?: return@mapNotNull null)
}

/* ---------- weekly summary (Android notification) ---------- */

/** Lines of the Sunday summary; null when there is nothing worth saying (no history yet). */
suspend fun weeklySummaryLines(today: String): List<String>? {
    val unit = trSaved("soʻm", "сум", "UZS")
    val since = kotlinx.datetime.LocalDate.parse(today).minus(7, kotlinx.datetime.DateTimeUnit.DAY).toString()
    val quotes = cachedQuotes()
    val histories = cachedWalletHistories()
    val lines = mutableListOf<String>()
    val usdHist = histories[WalletCode.USD].orEmpty().sortedBy { it.date }
    val usdNow = quotes[WalletCode.USD]?.cbu
    val usdThen = usdHist.lastOrNull { it.date <= since }?.rate
    if (usdNow != null && usdThen != null) {
        val d = usdNow - usdThen
        val ds = (if (d >= 0) "+" else "-") + kotlin.math.abs(d).fixed(0)
        lines += trSaved("Dollar: ${usdNow.formatRate()} $unit (hafta: $ds)", "Доллар: ${usdNow.formatRate()} $unit (за неделю: $ds)", "Dollar: ${usdNow.formatRate()} $unit (week: $ds)")
    }
    val list = WalletStore.load()
    if (list.isNotEmpty()) {
        val trend = walletTrend(list, histories, quotes, since, today)
        val total = walletSummary(list, quotes).total
        if (trend.size >= 2) {
            val d = (trend.last().rate - trend.first().rate).roundToLong()
            val ds = (if (d > 0) "+" else "") + d.formatUzsL()
            lines += trSaved(
                "Hamyon: ${total.roundToLong().formatUzsL()} $unit (hafta: $ds)",
                "Кошелёк: ${total.roundToLong().formatUzsL()} $unit (за неделю: $ds)",
                "Wallet: ${total.roundToLong().formatUzsL()} $unit (week: $ds)"
            )
        }
        GoalStore.load()?.let { g ->
            goalProgress(g, total, quotes)?.let { p ->
                val u = if (g.code == WalletCode.UZS) unit else g.code.code
                val name = g.name.ifBlank { trSaved("Maqsad", "Цель", "Goal") }
                val left = (p.target - p.saved).coerceAtLeast(0.0)
                lines += trSaved(
                    "$name: ${(p.share * 100).roundToLong()}% (yana ${fmtAmount(left, 0)} $u)",
                    "$name: ${(p.share * 100).roundToLong()}% (осталось ${fmtAmount(left, 0)} $u)",
                    "$name: ${(p.share * 100).roundToLong()}% (${fmtAmount(left, 0)} $u to go)"
                )
            }
        }
    }
    return lines.takeIf { it.isNotEmpty() }
}
