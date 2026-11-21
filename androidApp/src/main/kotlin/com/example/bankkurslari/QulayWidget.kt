package com.example.bankkurslari

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App palette (ui/theme/Color.kt), day and night, so the widget follows the system theme like the app does. */
internal object WidgetColors {
    val card = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1F1C15))
    val ink = ColorProvider(day = Color(0xFF1B1710), night = Color(0xFFF2EDE0))
    val soft = ColorProvider(day = Color(0xFF6A6252), night = Color(0xFFB5AC98))
    val emerald = ColorProvider(day = Color(0xFF26794A), night = Color(0xFF6CC48A))
    val onEmerald = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF15130E))
    val tile = ColorProvider(day = Color(0xFFFBF6E9), night = Color(0xFF29251C))
    val tealTint = ColorProvider(day = Color(0xFFE3F1E4), night = Color(0xFF23382A))
    val rose = ColorProvider(day = Color(0xFFB4433A), night = Color(0xFFFF8A80))
    val gold = ColorProvider(day = Color(0xFFF2B01E), night = Color(0xFFF2B01E))
}

private val WIDE = DpSize(250.dp, 110.dp)

/**
 * Home-screen widget for one currency (USD, EUR and RUB are separate widgets in the picker), adapting to its size:
 *  - narrow (2×2 and up): both rankings stacked, as many places as the height fits;
 *  - wide: both rankings side by side, as many places as the height fits (logos and gaps once it is tall).
 * Reads the rates the app and [RateWorker] cache; tapping opens the app.
 */
abstract class QulayWidget(private val currency: String, private val compactOnly: Boolean = false) : GlanceAppWidget() {
    // Exact: the real size, so the rankings fill whatever height the user resizes the widget to.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        initPlatform(context)
        val cur = currency
        val model = withContext(Dispatchers.IO) {
            runCatching { RateService.loadCache("rates_$cur")?.let { widgetModel(cur, parseRatesPayload(it)) } }.getOrNull()
        }
        val logos = withContext(Dispatchers.IO) {
            val names = model?.let { (it.sell.take(5) + it.buy.take(5)).mapNotNull { e -> e.logo } }.orEmpty().toSet()
            names.associateWith { loadLogo(context, it) }.filterValues { it != null }.mapValues { it.value!! }
        }
        provideContent { WidgetContent(model, logos, compactOnly) }
    }

    private fun loadLogo(context: Context, stem: String): Bitmap? = runCatching {
        context.assets.open("composeResources/com.example.bankkurslari.generated/drawable/$stem.png").use { s ->
            val src = BitmapFactory.decodeStream(s) ?: return@use null
            Bitmap.createScaledBitmap(src, 64, 64, true)
        }
    }.getOrNull()
}

// One class per currency: Glance finds placed widgets by their GlanceAppWidget class.
class UsdWidget : QulayWidget("USD")
class EurWidget : QulayWidget("EUR")
class RubWidget : QulayWidget("RUB")
class CompactUsdWidget : QulayWidget("USD", compactOnly = true)
class CompactEurWidget : QulayWidget("EUR", compactOnly = true)
class CompactRubWidget : QulayWidget("RUB", compactOnly = true)

@Composable
private fun WidgetContent(model: WidgetModel?, logos: Map<String, Bitmap>, compactOnly: Boolean) {
    val size = LocalSize.current
    val root = GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(22.dp).background(WidgetColors.card)
        .clickable(actionStartActivity<MainActivity>())
    if (model == null || model.sell.isEmpty() || model.buy.isEmpty()) {
        Box(root.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(trSaved("Kurslar yuklanmoqda…", "Загрузка курсов…", "Loading rates…"), style = TextStyle(color = WidgetColors.soft, fontSize = 13.sp))
        }
        return
    }
    if (compactOnly) {
        CompactRates(model, root)
        return
    }
    when {
        size.width < WIDE.width -> {
            // Existing resizable widgets keep their ranked layout at compact sizes.
            val each = (((size.height - 74.dp) / 19.dp).toInt() / 2).coerceIn(1, minOf(6, model.sell.size, model.buy.size))
            Small(model, root, each)
        }
        else -> {
            val large = size.height >= 200.dp
            // header + column titles (+ footer when large) take a fixed band; the rest is rows.
            val fixed = if (large) 100.dp else 62.dp
            val pitch = if (large) 33.dp else 21.dp
            val rows = ((size.height - fixed) / pitch).toInt().coerceIn(1, minOf(8, model.sell.size, model.buy.size))
            Board(model, logos, rows = rows, root = root, large = large)
        }
    }
}

/** Decimal comma, as the app writes it in Uzbek and Russian. */
private fun dec(s: String) = if (RateService.language() == "en") s else s.replace('.', ',')

private fun sellTitle() = trSaved("Sotsangiz", "Продаёте", "Selling")
private fun buyTitle() = trSaved("Olsangiz", "Покупаете", "Buying")
private fun som() = trSaved("soʻm", "сум", "UZS")
private fun time(m: WidgetModel) = m.updatedAt?.formatUpdatedAt().orEmpty()

/** 2×2: the two best bank rates, large and easy to scan. */
@Composable
private fun CompactRates(m: WidgetModel, root: GlanceModifier) {
    val size = LocalSize.current
    val oneRow = size.height < 95.dp || size.height < size.width * 0.75f
    if (oneRow) {
        CompactRatesShort(m, root)
        return
    }
    Column(root.padding(horizontal = 11.dp, vertical = 9.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Currency.entries.firstOrNull { it.code == m.currency }?.symbol ?: "$",
                style = TextStyle(color = WidgetColors.emerald, fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(6.dp))
            Text(m.currency, style = TextStyle(color = WidgetColors.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(GlanceModifier.height(4.dp))
        CompactRateRow(trSaved("Sotish", "Продажа", "Sell"), m.sell.firstOrNull()?.rate,
            WidgetColors.emerald, GlanceModifier.fillMaxWidth().defaultWeight())
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(WidgetColors.tile)) {}
        CompactRateRow(trSaved("Olish", "Покупка", "Buy"), m.buy.firstOrNull()?.rate,
            WidgetColors.ink, GlanceModifier.fillMaxWidth().defaultWeight())
    }
}

/** One-row height keeps the width at two cells and fits both prices in a horizontal strip. */
@Composable
private fun CompactRatesShort(m: WidgetModel, root: GlanceModifier) {
    Row(root.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(m.currency, style = TextStyle(color = WidgetColors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.width(5.dp))
        CompactShortRate(trSaved("Sotish", "Продажа", "Sell"), m.sell.firstOrNull()?.rate, WidgetColors.emerald,
            GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.width(3.dp))
        CompactShortRate(trSaved("Olish", "Покупка", "Buy"), m.buy.firstOrNull()?.rate, WidgetColors.ink,
            GlanceModifier.defaultWeight())
    }
}

@Composable
private fun CompactShortRate(label: String, rate: Int?, color: ColorProvider, modifier: GlanceModifier) {
    val valueSize = if (LocalSize.current.width < 108.dp) 9.sp else 11.sp
    Column(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = WidgetColors.soft, fontSize = 8.sp), maxLines = 1)
        Text(rate?.toLong()?.formatUzsL() ?: "—",
            style = TextStyle(color = color, fontSize = valueSize, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun CompactRateRow(label: String, rate: Int?, color: ColorProvider, modifier: GlanceModifier) {
    Column(modifier,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = WidgetColors.soft, fontSize = 10.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        Text(rate?.toLong()?.formatUzsL() ?: "—",
            style = TextStyle(color = color, fontSize = 25.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun Header(m: WidgetModel, showCbu: Boolean) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.size(20.dp).cornerRadius(6.dp).background(WidgetColors.emerald), contentAlignment = Alignment.Center) {
            Text(Currency.entries.firstOrNull { it.code == m.currency }?.symbol ?: "$", style = TextStyle(color = WidgetColors.onEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(m.currency, style = TextStyle(color = WidgetColors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        if (showCbu) {
            Spacer(GlanceModifier.width(8.dp))
            val diff = m.cbuDiff
            Text(
                "${trSaved("MB", "ЦБ", "CBU")} ${dec(m.cbuRate.formatRate())}",
                style = TextStyle(color = WidgetColors.soft, fontSize = 12.sp), maxLines = 1
            )
            if (kotlin.math.abs(diff) >= 0.005) {
                Text(
                    " ${if (diff > 0) "▲" else "▼"}${dec(kotlin.math.abs(diff).formatRate())}",
                    style = TextStyle(color = if (diff > 0) WidgetColors.emerald else WidgetColors.rose, fontSize = 12.sp), maxLines = 1
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(time(m), style = TextStyle(color = WidgetColors.soft, fontSize = 11.sp), maxLines = 1)
    }
}

/** Narrow: the two rankings stacked, [each] places apiece. */
@Composable
private fun Small(m: WidgetModel, root: GlanceModifier, each: Int) {
    Column(root.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Header(m, showCbu = false)
        Spacer(GlanceModifier.height(6.dp))
        MiniRanking("↑ " + sellTitle(), m.sell.take(each))
        Spacer(GlanceModifier.height(4.dp))
        MiniRanking("↓ " + buyTitle(), m.buy.take(each))
    }
}

// Glance draws at most 10 children per Row/Column (the rest are dropped silently), so every list lives in its own
// Column and each row is a single Box child: no Spacer between rows.
@Composable
private fun MiniRanking(title: String, list: List<WidgetEntry>) {
  Column(GlanceModifier.fillMaxWidth()) {
    Text(title, style = TextStyle(color = WidgetColors.soft, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.padding(bottom = 2.dp))
    list.forEach { e ->
        val lead = e.place == 1
        Box(GlanceModifier.fillMaxWidth().padding(bottom = 1.dp)) { Row(
            GlanceModifier.fillMaxWidth().height(18.dp).cornerRadius(6.dp).background(if (lead) WidgetColors.tealTint else WidgetColors.card).padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(GlanceModifier.size(14.dp).cornerRadius(7.dp).background(if (lead) WidgetColors.gold else WidgetColors.tile), contentAlignment = Alignment.Center) {
                Text("${e.place}", style = TextStyle(color = if (lead) ColorProvider(day = Color(0xFF5A3F00), night = Color(0xFF5A3F00)) else WidgetColors.soft, fontSize = 8.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.width(4.dp))
            Text(e.short, style = TextStyle(color = WidgetColors.ink, fontSize = 11.sp, fontWeight = if (lead) FontWeight.Bold else FontWeight.Normal), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Text(e.rate.toLong().formatUzsL(), style = TextStyle(color = if (lead) WidgetColors.emerald else WidgetColors.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        } }
    }
  }
}

/** 4×2 and 4×4: two leaderboards side by side. */
@Composable
private fun Board(m: WidgetModel, logos: Map<String, Bitmap>, rows: Int, root: GlanceModifier, large: Boolean) {
    Column(root.padding(horizontal = 14.dp, vertical = if (large) 14.dp else 10.dp)) {
        Header(m, showCbu = true)
        Spacer(GlanceModifier.height(if (large) 10.dp else 6.dp))
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Ranking("↑ " + sellTitle(), m.sell.take(rows), logos, large, sign = "−", modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(10.dp))
            Ranking("↓ " + buyTitle(), m.buy.take(rows), logos, large, sign = "+", modifier = GlanceModifier.defaultWeight())
        }
        if (large) {
            Text(
                trSaved("${m.bankCount} ta bank · 1 ${m.currency} uchun, ${som()}", "${m.bankCount} банков · за 1 ${m.currency}, ${som()}", "${m.bankCount} banks · per 1 ${m.currency}, ${som()}"),
                style = TextStyle(color = WidgetColors.soft, fontSize = 11.sp), maxLines = 1
            )
        }
    }
}

@Composable
private fun Ranking(title: String, list: List<WidgetEntry>, logos: Map<String, Bitmap>, large: Boolean, sign: String, modifier: GlanceModifier) {
    val rowH = if (large) 30.dp else 20.dp
    Column(modifier) {
        Text(title, style = TextStyle(color = WidgetColors.soft, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.padding(bottom = if (large) 6.dp else 3.dp))
        list.forEach { e ->
            val lead = e.place == 1
            Box(GlanceModifier.fillMaxWidth().padding(bottom = if (large) 3.dp else 1.dp)) { Row(
                GlanceModifier.fillMaxWidth().height(rowH).cornerRadius(8.dp).background(if (lead) WidgetColors.tealTint else WidgetColors.card).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    GlanceModifier.size(if (large) 18.dp else 15.dp).cornerRadius(9.dp).background(if (lead) WidgetColors.gold else WidgetColors.tile),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${e.place}", style = TextStyle(color = if (lead) ColorProvider(day = Color(0xFF5A3F00), night = Color(0xFF5A3F00)) else WidgetColors.soft, fontSize = if (large) 10.sp else 9.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.width(5.dp))
                val logo = e.logo?.let { logos[it] }
                if (large && logo != null) {
                    Image(ImageProvider(logo), contentDescription = null, contentScale = ContentScale.Fit, modifier = GlanceModifier.size(18.dp).cornerRadius(5.dp))
                    Spacer(GlanceModifier.width(5.dp))
                }
                Text(e.short, style = TextStyle(color = WidgetColors.ink, fontSize = if (large) 12.sp else 11.sp, fontWeight = if (lead) FontWeight.Bold else FontWeight.Normal), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Column(horizontalAlignment = Alignment.End) {
                    Text(e.rate.toLong().formatUzsL(), style = TextStyle(color = if (lead) WidgetColors.emerald else WidgetColors.ink, fontSize = if (large) 13.sp else 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), maxLines = 1)
                    if (large && e.gap > 0) {
                        Text("$sign${e.gap}", style = TextStyle(color = WidgetColors.soft, fontSize = 9.sp, textAlign = TextAlign.End), maxLines = 1)
                    }
                }
            } }
        }
    }
}
