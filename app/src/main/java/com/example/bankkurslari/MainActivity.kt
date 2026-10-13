package com.example.bankkurslari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.BankKurslariTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DecimalFormat
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BankKurslariTheme {
                CurrencyTrackerApp()
            }
        }
    }
}

private data class BankRate(
    val bank: String,
    val buy: Int,
    val sell: Int,
    val distanceKm: Double,
    val branch: String
)

private data class CbuRate(
    val rate: Double,
    val diff: Double,
    val date: String
)

private data class RatesPayload(
    val cbu: CbuRate,
    val banks: List<BankRate>
)

private data class TrendPoint(val label: String, val value: Int)

private const val backendUsdUrl = "http://10.0.2.2:8080/api/rates/usd"

private val sampleRates = listOf(
    BankRate("Kapitalbank", 11935, 11972, 1.2, "Yunusobod 4-kvartal"),
    BankRate("SQB", 11928, 11966, 2.8, "Amir Temur shoh ko'chasi"),
    BankRate("Agrobank", 11920, 11980, 3.5, "Chilonzor C mavzesi"),
    BankRate("MKBank", 11931, 11970, 4.1, "Mirzo Ulug'bek filiali"),
    BankRate("Ipoteka Bank", 11912, 11964, 5.0, "Shayxontohur markazi"),
    BankRate("Hamkorbank", 11925, 11975, 6.4, "Mirobod ayirboshlash")
)

private val trendPoints = listOf(
    TrendPoint("1", 11882),
    TrendPoint("5", 11918),
    TrendPoint("10", 11904),
    TrendPoint("15", 11896),
    TrendPoint("20", 11912),
    TrendPoint("25", 11931),
    TrendPoint("30", 11926)
)

private val sampleCbuRate = CbuRate(rate = 11899.09, diff = 1.19, date = "13.10.2026")

@Composable
fun CurrencyTrackerApp() {
    var rates by remember { mutableStateOf(sampleRates) }
    var cbuRate by remember { mutableStateOf(sampleCbuRate) }
    var amountText by remember { mutableStateOf("100") }
    var selectedFilter by remember { mutableStateOf("Eng yaxshi") }
    var dataSource by remember { mutableStateOf("Demo ma'lumot") }
    val bestBuy = rates.maxBy { it.buy }
    val bestSell = rates.minBy { it.sell }
    val amount = amountText.toIntOrNull() ?: 0
    val bestValue = amount * bestBuy.buy
    val worstValue = amount * rates.minOf { it.buy }

    LaunchedEffect(Unit) {
        runCatching { fetchUsdRates() }
            .onSuccess { payload ->
                cbuRate = payload.cbu
                rates = payload.banks
                dataSource = "Live backend"
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF6F8F6)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeaderSection(
                    cbuRate = cbuRate,
                    bestBuy = bestBuy,
                    bestSell = bestSell,
                    dataSource = dataSource,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
            item {
                CalculatorSection(
                    amountText = amountText,
                    onAmountChange = { input -> amountText = input.filter(Char::isDigit).take(7) },
                    bestValue = bestValue,
                    savings = bestValue - worstValue,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                RatesHeader(
                    bestBuy = bestBuy,
                    bestSell = bestSell,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            items(rates, key = { it.bank }) { rate ->
                RateRow(
                    rate = rate,
                    isBestBuy = rate == bestBuy,
                    isBestSell = rate == bestSell,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                MapSection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    bestRate = bestBuy,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                AnalyticsSection(modifier = Modifier.padding(horizontal = 20.dp))
            }
            item {
                WidgetAndAlertSection(modifier = Modifier.padding(horizontal = 20.dp))
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    cbuRate: CbuRate,
    bestBuy: BankRate,
    bestSell: BankRate,
    dataSource: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF103C3A), Color(0xFF277C65), Color(0xFFD9A441))
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ValyutaStat",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CurrencyTracker Uz",
                    color = Color(0xFFE6F2ED),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = dataSource,
                color = Color(0xFF12302F),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE5F7C8))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Markaziy Bank kursi",
                color = Color(0xFFD9EFE8),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "1 USD = ${cbuRate.rate.formatRate()} UZS  (${cbuRate.diff.formatSigned()})",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricPill(
                icon = Icons.Outlined.Savings,
                label = "Top sotib olish",
                value = "${bestBuy.buy.formatUzs()} • ${bestBuy.bank}",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                icon = Icons.Outlined.SwapVert,
                label = "Top sotish",
                value = "${bestSell.sell.formatUzs()} • ${bestSell.bank}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricPill(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFEAF7F2), modifier = Modifier.size(18.dp))
        Text(text = label, color = Color(0xFFE6F2ED), style = MaterialTheme.typography.labelMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CalculatorSection(
    amountText: String,
    onAmountChange: (String) -> Unit,
    bestValue: Int,
    savings: Int,
    modifier: Modifier = Modifier
) {
    SectionTitle(icon = Icons.Outlined.Calculate, title = "Smart kalkulyator", modifier = modifier)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E6DF), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("USD summa") },
            suffix = { Text("USD") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultTile(
                label = "Eng yaxshi bankda",
                value = "${bestValue.formatUzs()} UZS",
                color = Color(0xFF147A55),
                modifier = Modifier.weight(1f)
            )
            ResultTile(
                label = "Tejamkorlik farqi",
                value = "${savings.formatUzs()} UZS",
                color = Color(0xFF2459A7),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResultTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = Color(0xFF5D6863), style = MaterialTheme.typography.labelMedium)
        Text(text = value, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatesHeader(bestBuy: BankRate, bestSell: BankRate, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(icon = Icons.Outlined.SwapVert, title = "Banklar kesimida kurslar")
        Text(
            text = "Yashil - dollarni qimmatroq topshirish, ko'k - dollarni arzonroq olish.",
            color = Color(0xFF66706B),
            style = MaterialTheme.typography.bodyMedium
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Best buy: ${bestBuy.bank}") })
            AssistChip(onClick = {}, label = { Text("Best sell: ${bestSell.bank}") })
        }
    }
}

@Composable
private fun RateRow(rate: BankRate, isBestBuy: Boolean, isBestSell: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2EF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rate.bank.take(2).uppercase(),
                    color = Color(0xFF17443E),
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = rate.bank, fontWeight = FontWeight.Bold, color = Color(0xFF17211D))
                Text(
                    text = "${rate.branch} • ${rate.distanceKm} km",
                    color = Color(0xFF6C746F),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            RateValue("Sotib", rate.buy, isBestBuy, Color(0xFF147A55))
            RateValue("Sotish", rate.sell, isBestSell, Color(0xFF2459A7))
        }
    }
}

@Composable
private fun RateValue(label: String, value: Int, highlighted: Boolean, color: Color) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = label, color = Color(0xFF77817B), style = MaterialTheme.typography.labelSmall)
        Text(
            text = value.formatUzs(),
            color = if (highlighted) color else Color(0xFF202B27),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (highlighted) color.copy(alpha = 0.12f) else Color.Transparent)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MapSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    bestRate: BankRate,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(icon = Icons.Outlined.LocationOn, title = "Xarita va yaqin filiallar")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Eng yaxshi", "USD ATM", "24/7").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFDDE9E5))
                .border(1.dp, Color(0xFFC5D4CE), RoundedCornerShape(8.dp))
        ) {
            MapPreview()
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Outlined.Route, contentDescription = null, tint = Color(0xFF147A55), modifier = Modifier.size(18.dp))
                    Text(text = "Eng yaqin qulay nuqta", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${bestRate.bank}, ${bestRate.distanceKm} km • ${bestRate.buy.formatUzs()}",
                    color = Color(0xFF5B6660),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MapPreview() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val road = Path().apply {
            moveTo(size.width * 0.06f, size.height * 0.7f)
            cubicTo(size.width * 0.28f, size.height * 0.42f, size.width * 0.52f, size.height * 0.86f, size.width * 0.94f, size.height * 0.35f)
        }
        drawPath(road, color = Color.White, style = Stroke(width = 15f, cap = StrokeCap.Round))
        drawPath(road, color = Color(0xFF9DBAB1), style = Stroke(width = 4f, cap = StrokeCap.Round))
        val markers = listOf(
            Offset(size.width * 0.20f, size.height * 0.60f),
            Offset(size.width * 0.42f, size.height * 0.48f),
            Offset(size.width * 0.66f, size.height * 0.64f),
            Offset(size.width * 0.78f, size.height * 0.35f)
        )
        markers.forEachIndexed { index, offset ->
            drawCircle(
                color = if (index == 1) Color(0xFF147A55) else Color(0xFF2459A7),
                radius = if (index == 1) 15f else 11f,
                center = offset
            )
            drawCircle(color = Color.White, radius = 4f, center = offset)
        }
    }
}

@Composable
private fun AnalyticsSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(icon = Icons.AutoMirrored.Outlined.ShowChart, title = "Statistika va trendlar")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TrendChart(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF17211D))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Oylik signal", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = "1-5 sanalarda o'sish kuchliroq, oyning ikkinchi yarmida spread toraygan.",
                    color = Color(0xFFDCE8E2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = "CBU vs bank o'rtacha: +28 UZS", color = Color(0xFFFFD27A), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TrendChart(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(170.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E6DF), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "30 kunlik USD", color = Color(0xFF3B4641), fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val min = trendPoints.minOf { it.value }
            val max = trendPoints.maxOf { it.value }
            val stepX = size.width / trendPoints.lastIndex.coerceAtLeast(1)
            val points = trendPoints.mapIndexed { index, point ->
                val normalized = (point.value - min).toFloat() / (max - min).coerceAtLeast(1)
                Offset(index * stepX, size.height - normalized * size.height * 0.78f - 12f)
            }
            val path = Path().apply {
                points.forEachIndexed { index, offset ->
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }
            drawPath(path, color = Color(0xFF147A55), style = Stroke(width = 5f, cap = StrokeCap.Round))
            points.forEach { drawCircle(Color(0xFFD9A441), radius = 6f, center = it) }
        }
    }
}

@Composable
private fun WidgetAndAlertSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(icon = Icons.Outlined.Widgets, title = "Vidjetlar va bildirishnomalar")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureBlock(
                icon = Icons.Outlined.Widgets,
                title = "Glance 2x4",
                body = "Top 5 bank kursi va xaritaga tezkor o'tish.",
                modifier = Modifier.weight(1f)
            )
            FeatureBlock(
                icon = Icons.Outlined.Notifications,
                title = "Kurs alert",
                body = "USD belgilangan chegaradan o'tsa push yuboriladi.",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureBlock(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E6DF), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2459A7), modifier = Modifier.size(22.dp))
        Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF18221E))
        Text(text = body, color = Color(0xFF65716A), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF147A55), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color(0xFF17211D),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun Int.formatUzs(): String = DecimalFormat("#,###").format(this).replace(",", " ")

private fun Double.formatRate(): String = DecimalFormat("#,##0.00").format(this).replace(",", " ")

private fun Double.formatSigned(): String {
    val formatted = DecimalFormat("#,##0.##").format(kotlin.math.abs(this)).replace(",", " ")
    return if (this >= 0) "+$formatted" else "-$formatted"
}

private suspend fun fetchUsdRates(): RatesPayload = withContext(Dispatchers.IO) {
    val connection = (URL(backendUsdUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = 7000
    }

    try {
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseRatesPayload(body)
    } finally {
        connection.disconnect()
    }
}

private fun parseRatesPayload(body: String): RatesPayload {
    val root = JSONObject(body)
    val cbuJson = root.getJSONObject("cbu")
    val banksJson = root.getJSONArray("banks")
    val banks = buildList {
        for (index in 0 until banksJson.length()) {
            val item = banksJson.getJSONObject(index)
            if (item.isNull("buy") || item.isNull("sell")) continue
            add(
                BankRate(
                    bank = item.getString("bank"),
                    buy = item.getInt("buy"),
                    sell = item.getInt("sell"),
                    distanceKm = (index + 1) / 10.0,
                    branch = "Bank.uz live kurs"
                )
            )
        }
    }

    return RatesPayload(
        cbu = CbuRate(
            rate = cbuJson.getDouble("rate"),
            diff = cbuJson.optDouble("diff", 0.0),
            date = cbuJson.optString("date")
        ),
        banks = banks.ifEmpty { sampleRates }
    )
}

@Preview(showBackground = true)
@Composable
fun CurrencyTrackerPreview() {
    BankKurslariTheme(dynamicColor = false) {
        CurrencyTrackerApp()
    }
}
