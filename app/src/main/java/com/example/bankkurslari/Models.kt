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

internal data class Branch(val address: String, val lat: Double, val lng: Double)

internal data class BankRate(
    val bank: String,
    val buy: Int?,
    val sell: Int?,
    val branch: Branch?
) {
    val spread: Int? = if (buy != null && sell != null) sell - buy else null
}

internal data class CbuRate(val rate: Double, val diff: Double, val date: String)

internal data class RatesPayload(val cbu: CbuRate, val banks: List<BankRate>, val updatedAt: String?)

internal data class Atm(val bank: String, val name: String, val lat: Double, val lng: Double)

internal data class OsmBranch(val bank: String, val name: String, val address: String?, val hours: String?, val lat: Double, val lng: Double)

internal enum class MapFilter { ALL, BRANCHES, ATMS }

internal data class HistoryPoint(val date: String, val rate: Double)

internal data class Seasonality(val start: Double, val mid: Double, val end: Double)

internal data class Analytics(
    val enoughData: Boolean,
    val current: Double,
    val weekAgo: Double,
    val weekChangePct: Double,
    val trend: String,        // "up" | "down" | "flat"
    val forecastRate: Double,
    val forecastPct: Double,
    val seasonality: Seasonality,
    val bestRisePeriod: String, // "start" | "mid" | "end"
    val sellAdvice: String,     // "now" | "wait"
    val buyAdvice: String,      // "now" | "wait"
    val points: List<HistoryPoint>
)

internal enum class HomeTab(val icon: ImageVector) {
    KURS(Icons.Outlined.SwapVert),
    MAP(Icons.Outlined.Map),
    TREND(Icons.Outlined.ShowChart);

    @Composable
    fun label(): String = when (this) {
        KURS -> tr("Kurs", "Курс")
        MAP -> tr("Xarita", "Карта")
        TREND -> tr("Tarix", "История")
    }
}

internal enum class SortMode { BUY, SELL }

internal enum class ConvertMode { SELL_USD, BUY_USD }

internal enum class Currency(val code: String, val symbol: String, val nameUz: String, val nameRu: String) {
    USD("USD", "$", "Dollar", "Доллар"),
    EUR("EUR", "€", "Yevro", "Евро"),
    RUB("RUB", "₽", "Rubl", "Рубль");

    fun displayName(lang: Lang) = if (lang == Lang.RU) nameRu else nameUz
}

internal val backendBase = BuildConfig.SERVER_BASE
internal fun ratesUrl(cur: Currency) = "$backendBase/api/rates/${cur.code.lowercase()}"
internal fun analyticsUrl(cur: Currency) = "$backendBase/api/analytics/${cur.code.lowercase()}"
internal fun historyUrl(cur: Currency) = "$backendBase/api/history/${cur.code.lowercase()}"
internal val backendAtmsUrl = "$backendBase/api/atms"
internal val backendBranchesUrl = "$backendBase/api/branches"
