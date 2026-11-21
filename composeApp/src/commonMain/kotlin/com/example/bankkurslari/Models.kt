package com.example.bankkurslari

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class Branch(val address: String, val lat: Double, val lng: Double)

/** buy/sell in so'm per 1 unit; Double because some currencies trade in fractions (KZT: 26.1 / 26.9). */
data class BankRate(
    val bank: String,
    val buy: Double?,
    val sell: Double?,
    val branch: Branch?
) {
    val spread: Double? = if (buy != null && sell != null) sell - buy else null
}

data class CbuRate(val rate: Double, val diff: Double, val date: String)

data class RatesPayload(val cbu: CbuRate, val banks: List<BankRate>, val updatedAt: String?)

data class Atm(val bank: String, val name: String, val lat: Double, val lng: Double)

data class OsmBranch(val bank: String, val name: String, val address: String?, val hours: String?, val lat: Double, val lng: Double)

enum class MapFilter { ALL, BRANCHES, ATMS }

data class HistoryPoint(val date: String, val rate: Double)

data class Analytics(val points: List<HistoryPoint>)

/**
 * Hamyon (savings) is built but not released yet: false hides its tab, the Kurs nudge, the widget and the shortcut
 * (androidApp res/values/bools.xml `wallet_enabled` must match). Currency-to-currency exchange stays (it lives in Kurs).
 */
const val WALLET_ENABLED = false

enum class HomeTab(val icon: ImageVector) {
    KURS(Icons.Outlined.SwapVert),
    MAP(Icons.Outlined.Map),
    TREND(Icons.AutoMirrored.Outlined.ShowChart),
    WALLET(Icons.Outlined.AccountBalanceWallet);

    @Composable
    fun label(): String = when (this) {
        KURS -> tr("Kurs", "Курс", "Rate")
        MAP -> tr("Xarita", "Карта", "Map")
        TREND -> tr("Tarix", "История", "History")
        WALLET -> tr("Hamyon", "Кошелёк", "Wallet")
    }

    companion object {
        /** Tabs shown in the bottom bar and reachable from widgets, shortcuts and notifications. */
        val visible: List<HomeTab> get() = entries.filter { it != WALLET || WALLET_ENABLED }
    }
}

enum class SortMode { BUY, SELL }

enum class ConvertMode { SELL_USD, BUY_USD }

/**
 * [main]: the three currencies with CBU history, analytics, alerts and widgets (Tarix, Xarita tabs, settings).
 * The others are traded in cash by fewer banks: Kurs shows their bank rates only, and the choice is not saved.
 * bank.uz, 21.11.2026: GBP 25 banks, CHF 23, JPY 22, KZT 7, CNY 2; no bank trades any other currency.
 */
enum class Currency(
    val code: String, val symbol: String, val nameUz: String, val nameRu: String, val nameEn: String, val main: Boolean = true
) {
    USD("USD", "$", "Dollar", "Доллар", "Dollar"),
    EUR("EUR", "€", "Yevro", "Евро", "Euro"),
    RUB("RUB", "₽", "Rubl", "Рубль", "Ruble"),
    GBP("GBP", "£", "Funt sterling", "Фунт стерлингов", "Pound sterling", main = false),
    CHF("CHF", "Fr", "Shveytsariya franki", "Швейцарский франк", "Swiss franc", main = false),
    JPY("JPY", "¥", "Yapon iyenasi", "Японская иена", "Japanese yen", main = false),
    KZT("KZT", "₸", "Qozogʻiston tengesi", "Казахстанский тенге", "Kazakh tenge", main = false),
    CNY("CNY", "元", "Xitoy yuani", "Китайский юань", "Chinese yuan", main = false);

    fun displayName(lang: Lang) = pick(lang, nameUz, nameRu, nameEn)

    companion object {
        /** Tabs, tickers, history, settings: currencies with full data. */
        val MAIN: List<Currency> = entries.filter { it.main }
        /** Bank cash rates only, offered under "Banklarda yana" in Kurs. */
        val EXTRA: List<Currency> = entries.filter { !it.main }
    }
}

/** Public API: Cloudflare Worker serving JSON that GitHub Actions rebuilds (backend/src/build.js). */
const val SERVER_PUBLIC = "https://api.qulaykurs.uz"

/** true: use `npm run dev` on the dev machine instead (emulator/simulator via host alias, phones via SERVER_LAN). */
const val USE_LOCAL_SERVER = false

/** Dev machine's LAN address, for real phones on the same Wi-Fi. Changes with the network. */
const val SERVER_LAN = "http://192.168.1.56:8080"

val SERVER_BASE: String by lazy { if (USE_LOCAL_SERVER) platformServerBase(SERVER_LAN) else SERVER_PUBLIC }
val backendBase: String get() = SERVER_BASE
fun ratesUrl(cur: Currency) = "$backendBase/api/rates/${cur.code.lowercase()}"
fun analyticsUrl(cur: Currency) = "$backendBase/api/analytics/${cur.code.lowercase()}"
fun historyUrl(cur: Currency) = "$backendBase/api/history/${cur.code.lowercase()}"
val backendAtmsUrl: String get() = "$backendBase/api/atms"
val backendBranchesUrl: String get() = "$backendBase/api/branches"
val backendGoldUrl: String get() = "$backendBase/api/gold"

/** One CBU measured gold bar: sale price and buy-back prices (packaging intact / damaged), so'm. */
data class GoldBar(val grams: Double, val sell: Long, val buyback: Long?, val buybackDamaged: Long?)

data class GoldPrices(val date: String?, val time: String?, val bars: List<GoldBar>)

val backendCbuAllUrl: String get() = "$backendBase/api/cbu/all"

/** One CBU currency: [rate] so'm per [nominal] units (e.g. 100 JPY). */
data class CbuCurrency(
    val code: String,
    val nominal: Int,
    val rate: Double,
    val diff: Double,
    val date: String?,
    val nameUz: String,
    val nameUzc: String,
    val nameRu: String,
    val nameEn: String
) {
    val perUnit: Double get() = rate / nominal
}
