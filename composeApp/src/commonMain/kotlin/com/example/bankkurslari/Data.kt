package com.example.bankkurslari

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/* ---------- JSON helpers (org.json-like, null-safe) ---------- */

val JsonLenient = Json { ignoreUnknownKeys = true; isLenient = true }

fun parseJsonObject(body: String): JsonObject = JsonLenient.parseToJsonElement(body).jsonObject

private fun JsonObject.prim(k: String): JsonPrimitive? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }

fun JsonObject.isNullOrMissing(k: String) = prim(k) == null && this[k] !is JsonObject && this[k] !is JsonArray
fun JsonObject.str(k: String): String? = prim(k)?.contentOrNull
fun JsonObject.dbl(k: String): Double? = prim(k)?.doubleOrNull
fun JsonObject.int(k: String): Int? = prim(k)?.intOrNull ?: prim(k)?.doubleOrNull?.toInt()
fun JsonObject.long(k: String): Long? = prim(k)?.longOrNull
fun JsonObject.bool(k: String): Boolean? = prim(k)?.booleanOrNull
fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject
fun JsonObject.arr(k: String): JsonArray? = this[k] as? JsonArray

fun JsonArray.objects(): List<JsonObject> = mapNotNull { it as? JsonObject }

/* ---------- network ---------- */

private val http by lazy { HttpClient { install(HttpTimeout) { connectTimeoutMillis = 5_000 } } }

/** GET [url] and return the body; throws on non-2xx or timeout. */
suspend fun httpGet(url: String, readTimeoutMs: Long): String {
    val res = http.get(url) {
        timeout {
            requestTimeoutMillis = readTimeoutMs
            socketTimeoutMillis = readTimeoutMs
        }
    }
    if (!res.status.isSuccess()) throw IllegalStateException("HTTP ${res.status.value}")
    return res.bodyAsText()
}

/* ---------- rates ---------- */

/* Parsing and cache I/O run on Dispatchers.Default: the branch/ATM payloads are ~0.5 MB each, and on iOS doing that
   on the main thread blocks taps for seconds. */

suspend fun cachedRates(cur: Currency): RatesPayload? = withContext(Dispatchers.Default) {
    RateService.loadCache("rates_${cur.code}")?.let { runCatching { parseRatesPayload(it) }.getOrNull() }
}

fun parseRatesPayload(body: String): RatesPayload {
    val root = parseJsonObject(body)
    val cbuJson = root.obj("cbu") ?: error("no cbu")
    val banks = (root.arr("banks") ?: JsonArray(emptyList())).objects().mapNotNull { item ->
        val buy = item.int("buy")
        val sell = item.int("sell")
        if (buy == null && sell == null) return@mapNotNull null
        val branch = item.obj("branch")?.let {
            Branch(address = it.str("address").orEmpty(), lat = it.dbl("lat") ?: Double.NaN, lng = it.dbl("lng") ?: Double.NaN)
        }
        BankRate(bank = item.str("bank") ?: return@mapNotNull null, buy = buy, sell = sell, branch = branch)
    }
    return RatesPayload(
        cbu = CbuRate(
            rate = cbuJson.dbl("rate") ?: error("no rate"),
            diff = cbuJson.dbl("diff") ?: 0.0,
            date = cbuJson.str("date").orEmpty()
        ),
        banks = banks,
        updatedAt = root.str("updatedAt")?.ifEmpty { null }
    )
}

suspend fun fetchRates(cur: Currency): RatesPayload = withContext(Dispatchers.Default) {
    val body = httpGet(ratesUrl(cur), 15_000)
    RateService.saveCache("rates_${cur.code}", body) // keep last good copy for offline
    parseRatesPayload(body)
}

/* ---------- ATMs / branches ---------- */

suspend fun fetchAtms(): List<Atm> = withContext(Dispatchers.Default) {
    try {
        val body = httpGet(backendAtmsUrl, 20_000)
        RateService.saveCache("atms", body)
        parseAtms(body)
    } catch (e: Exception) {
        RateService.loadCache("atms")?.let { runCatching { parseAtms(it) }.getOrNull() } ?: emptyList()
    }
}

fun parseAtms(body: String): List<Atm> =
    (parseJsonObject(body).arr("atms") ?: JsonArray(emptyList())).objects().mapNotNull { item ->
        val lat = item.dbl("lat") ?: return@mapNotNull null
        val lng = item.dbl("lng") ?: return@mapNotNull null
        Atm(
            bank = item.str("bank")?.ifEmpty { null } ?: "Bankomat",
            name = item.str("name")?.ifEmpty { null } ?: "Bankomat",
            lat = lat, lng = lng
        )
    }

suspend fun fetchBranches(): List<OsmBranch> = withContext(Dispatchers.Default) {
    val body = try {
        httpGet(backendBranchesUrl, 20_000).also { RateService.saveCache("branches", it) }
    } catch (e: Exception) {
        RateService.loadCache("branches") ?: return@withContext emptyList()
    }
    (parseJsonObject(body).arr("branches") ?: JsonArray(emptyList())).objects().mapNotNull { item ->
        val lat = item.dbl("lat") ?: return@mapNotNull null
        val lng = item.dbl("lng") ?: return@mapNotNull null
        OsmBranch(
            bank = item.str("bank")?.ifEmpty { null } ?: "Bank",
            name = item.str("name")?.ifEmpty { null } ?: "Bank",
            address = item.str("address")?.ifEmpty { null },
            hours = item.str("hours")?.ifEmpty { null },
            lat = lat, lng = lng
        )
    }
}

/* ---------- history ---------- */

/** History for the Tarix chart (the server's forecast/advice fields are no longer used). */
suspend fun fetchAnalytics(cur: Currency): Analytics = Analytics(points = fetchHistory(cur))

fun parseHistoryPoints(body: String): List<HistoryPoint> =
    (parseJsonObject(body).arr("points") ?: JsonArray(emptyList())).objects().mapNotNull { p ->
        val rate = p.dbl("rate") ?: return@mapNotNull null
        HistoryPoint(p.str("date").orEmpty(), rate)
    }.sortedBy { it.date }

/** Cache first: a cold backend request can take ~30 s. */
suspend fun cachedHistory(cur: Currency): List<HistoryPoint> = withContext(Dispatchers.Default) {
    RateService.loadCache("history_${cur.code}")?.let { runCatching { parseHistoryPoints(it) }.getOrNull() }.orEmpty()
}

suspend fun fetchHistory(cur: Currency): List<HistoryPoint> = withContext(Dispatchers.Default) {
    val body = httpGet(historyUrl(cur), 45_000)
    RateService.saveCache("history_${cur.code}", body)
    parseHistoryPoints(body)
}

