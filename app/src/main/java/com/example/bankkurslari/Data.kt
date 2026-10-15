package com.example.bankkurslari

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.BankKurslariTheme
import com.example.bankkurslari.ui.theme.VsAmber
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsDanger
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldDeep
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsHeroBot
import com.example.bankkurslari.ui.theme.VsHeroMid
import com.example.bankkurslari.ui.theme.VsHeroTop
import com.example.bankkurslari.ui.theme.VsIndigo
import com.example.bankkurslari.ui.theme.VsIndigoSoft
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

internal fun cachedRates(context: android.content.Context, cur: Currency): RatesPayload? =
    RateService.loadCache(context, "rates_${cur.code}")?.let { runCatching { parseRatesPayload(it) }.getOrNull() }

internal fun parseRatesPayload(body: String): RatesPayload {
    val root = JSONObject(body)
    val cbuJson = root.getJSONObject("cbu")
    val banksJson = root.getJSONArray("banks")
    val banks = buildList {
        for (index in 0 until banksJson.length()) {
            val item = banksJson.getJSONObject(index)
            val buy = if (item.isNull("buy")) null else item.getInt("buy")
            val sell = if (item.isNull("sell")) null else item.getInt("sell")
            if (buy == null && sell == null) continue
            val branchJson = item.optJSONObject("branch")
            val branch = branchJson?.let {
                Branch(
                    address = it.optString("address"),
                    lat = it.optDouble("lat"),
                    lng = it.optDouble("lng")
                )
            }
            add(BankRate(bank = item.getString("bank"), buy = buy, sell = sell, branch = branch))
        }
    }
    return RatesPayload(
        cbu = CbuRate(
            rate = cbuJson.getDouble("rate"),
            diff = cbuJson.optDouble("diff", 0.0),
            date = cbuJson.optString("date")
        ),
        banks = banks,
        updatedAt = root.optString("updatedAt").ifEmpty { null }
    )
}

internal suspend fun fetchAtms(context: android.content.Context): List<Atm> = withContext(Dispatchers.IO) {
    try {
        val body = httpGet(backendAtmsUrl, 20000)
        RateService.saveCache(context, "atms", body)
        parseAtms(body)
    } catch (e: Exception) {
        RateService.loadCache(context, "atms")?.let { runCatching { parseAtms(it) }.getOrNull() } ?: emptyList()
    }
}

internal fun parseAtms(body: String): List<Atm> {
    val arr = JSONObject(body).getJSONArray("atms")
    return buildList {
        for (index in 0 until arr.length()) {
            val item = arr.getJSONObject(index)
            val lat = item.optDouble("lat", Double.NaN)
            val lng = item.optDouble("lng", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            add(
                Atm(
                    bank = item.optString("bank").ifEmpty { "Bankomat" },
                    name = item.optString("name").ifEmpty { "Bankomat" },
                    lat = lat,
                    lng = lng
                )
            )
        }
    }
}

internal suspend fun fetchBranches(context: android.content.Context): List<OsmBranch> = withContext(Dispatchers.IO) {
    val body = try {
        httpGet(backendBranchesUrl, 20000).also { RateService.saveCache(context, "branches", it) }
    } catch (e: Exception) {
        RateService.loadCache(context, "branches") ?: return@withContext emptyList()
    }
    val arr = JSONObject(body).getJSONArray("branches")
    buildList {
        for (index in 0 until arr.length()) {
            val item = arr.getJSONObject(index)
            val lat = item.optDouble("lat", Double.NaN)
            val lng = item.optDouble("lng", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            add(
                OsmBranch(
                    bank = item.optString("bank").ifEmpty { "Bank" },
                    name = item.optString("name").ifEmpty { "Bank" },
                    address = if (item.isNull("address")) null else item.optString("address").ifEmpty { null },
                    hours = if (item.isNull("hours")) null else item.optString("hours").ifEmpty { null },
                    lat = lat,
                    lng = lng
                )
            )
        }
    }
}

internal suspend fun fetchAnalytics(cur: Currency): Analytics = withContext(Dispatchers.IO) {
    val analyticsBody = httpGet(analyticsUrl(cur), 20000)
    val historyBody = runCatching { httpGet(historyUrl(cur), 20000) }.getOrNull()
    parseAnalytics(analyticsBody, historyBody)
}

internal fun httpGet(url: String, readTimeoutMs: Int): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = readTimeoutMs
    }
    return try {
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

internal fun parseAnalytics(body: String, historyBody: String?): Analytics {
    val root = JSONObject(body)
    val enough = root.optBoolean("enoughData", false)
    val seasonalityJson = root.optJSONObject("seasonality")
    val recJson = root.optJSONObject("recommendation")
    val points = buildList {
        val arr = historyBody?.let { JSONObject(it).optJSONArray("points") }
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val rate = p.optDouble("rate", Double.NaN)
                if (!rate.isNaN()) add(HistoryPoint(p.optString("date"), rate))
            }
        }
    }
    return Analytics(
        enoughData = enough,
        current = root.optDouble("current", 0.0),
        weekAgo = root.optDouble("weekAgo", 0.0),
        weekChangePct = root.optDouble("weekChangePct", 0.0),
        trend = root.optString("trend", "flat"),
        forecastRate = root.optDouble("forecastRate", 0.0),
        forecastPct = root.optDouble("forecastPct", 0.0),
        seasonality = Seasonality(
            start = seasonalityJson?.optDouble("start", 0.0) ?: 0.0,
            mid = seasonalityJson?.optDouble("mid", 0.0) ?: 0.0,
            end = seasonalityJson?.optDouble("end", 0.0) ?: 0.0
        ),
        bestRisePeriod = root.optString("bestRisePeriod", "start"),
        sellAdvice = recJson?.optString("sell", "now") ?: "now",
        buyAdvice = recJson?.optString("buy", "now") ?: "now",
        points = points
    )
}


/* ---------- history for the ticker sparklines (cache first: a cold backend request can take ~30 s) ---------- */

internal fun parseHistoryPoints(body: String): List<HistoryPoint> {
    val arr = JSONObject(body).optJSONArray("points") ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val rate = p.optDouble("rate", Double.NaN)
            if (!rate.isNaN()) add(HistoryPoint(p.optString("date"), rate))
        }
    }.sortedBy { it.date }
}

internal fun cachedHistory(context: android.content.Context, cur: Currency): List<HistoryPoint> =
    RateService.loadCache(context, "history_${cur.code}")?.let { runCatching { parseHistoryPoints(it) }.getOrNull() }.orEmpty()

internal suspend fun fetchHistory(context: android.content.Context, cur: Currency): List<HistoryPoint> = withContext(Dispatchers.IO) {
    val body = httpGet(historyUrl(cur), 45000)
    RateService.saveCache(context, "history_${cur.code}", body)
    parseHistoryPoints(body)
}
