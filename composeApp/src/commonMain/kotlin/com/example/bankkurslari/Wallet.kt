package com.example.bankkurslari

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlin.math.abs
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
