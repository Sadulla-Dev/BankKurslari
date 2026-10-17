package com.example.bankkurslari

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class Branch(val address: String, val lat: Double, val lng: Double)

data class BankRate(
    val bank: String,
    val buy: Int?,
    val sell: Int?,
    val branch: Branch?
) {
    val spread: Int? = if (buy != null && sell != null) sell - buy else null
}

data class CbuRate(val rate: Double, val diff: Double, val date: String)

data class RatesPayload(val cbu: CbuRate, val banks: List<BankRate>, val updatedAt: String?)

data class Atm(val bank: String, val name: String, val lat: Double, val lng: Double)

data class OsmBranch(val bank: String, val name: String, val address: String?, val hours: String?, val lat: Double, val lng: Double)

enum class MapFilter { ALL, BRANCHES, ATMS }

data class HistoryPoint(val date: String, val rate: Double)

data class Analytics(val points: List<HistoryPoint>)

enum class HomeTab(val icon: ImageVector) {
    KURS(Icons.Outlined.SwapVert),
    MAP(Icons.Outlined.Map),
    TREND(Icons.AutoMirrored.Outlined.ShowChart);

    @Composable
    fun label(): String = when (this) {
        KURS -> tr("Kurs", "Курс")
        MAP -> tr("Xarita", "Карта")
        TREND -> tr("Tarix", "История")
    }
}

enum class SortMode { BUY, SELL }

enum class ConvertMode { SELL_USD, BUY_USD }

enum class Currency(val code: String, val symbol: String, val nameUz: String, val nameRu: String) {
    USD("USD", "$", "Dollar", "Доллар"),
    EUR("EUR", "€", "Yevro", "Евро"),
    RUB("RUB", "₽", "Rubl", "Рубль");

    fun displayName(lang: Lang) = if (lang == Lang.RU) nameRu else nameUz
}

/** Dev machine's LAN address, for real phones on the same Wi-Fi. Changes with the network; a public server replaces this. */
const val SERVER_LAN = "http://192.168.1.56:8080"

/** Backend base URL for this device (emulators/simulators reach the dev machine through a fixed host alias). */
val SERVER_BASE: String by lazy { platformServerBase(SERVER_LAN) }
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
