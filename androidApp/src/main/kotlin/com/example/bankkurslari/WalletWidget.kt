package com.example.bankkurslari

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import android.content.Intent
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

/** What the wallet widget shows; null total = nothing in the wallet yet. */
private data class WalletWidgetModel(val total: Long?, val dayChange: Long, val usd: Long?, val usdRate: Double?, val usdDiff: Double?)

/**
 * "Hamyon" widget: the wallet's worth in so'm at today's best bank rates and the change since yesterday,
 * plus the CBU dollar rate. Reads the same caches as the app; [RateWorker] refreshes them.
 */
class WalletWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        initPlatform(context)
        val model = withContext(Dispatchers.IO) {
            val quotes = runCatching { cachedQuotes() }.getOrDefault(emptyMap())
            val list = WalletStore.load()
            val usd = quotes[WalletCode.USD]
            if (list.isEmpty()) {
                WalletWidgetModel(null, 0, null, usd?.cbu, usd?.cbuDiff)
            } else {
                val s = walletSummary(list, quotes)
                WalletWidgetModel(s.total.roundToLong(), s.dayChange.roundToLong(), s.totalUsd?.roundToLong(), usd?.cbu, usd?.cbuDiff)
            }
        }
        provideContent { WalletContent(model) }
    }
}

class WalletWidgetProvider : CurrencyWidgetReceiver(WalletWidget())

@Composable
private fun WalletContent(m: WalletWidgetModel) {
    val size = LocalSize.current
    val small = size.height < 120.dp
    val root = GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(22.dp).background(WidgetColors.card)
        .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, HomeTab.WALLET.name)))
        .padding(horizontal = 16.dp, vertical = 12.dp)
    val unit = trSaved("soʻm", "сум", "UZS")
    Column(root, verticalAlignment = Alignment.CenterVertically) {
        Text(trSaved("Hamyon", "Кошелёк", "Wallet"), style = TextStyle(color = WidgetColors.soft, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        if (m.total == null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                trSaved("Jamgʻarmangizni ilovada qoʻshing", "Добавьте сбережения в приложении", "Add your savings in the app"),
                style = TextStyle(color = WidgetColors.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 2
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(m.total.formatUzsL(), style = TextStyle(color = WidgetColors.emerald, fontSize = if (small) 22.sp else 26.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(" $unit", style = TextStyle(color = WidgetColors.soft, fontSize = 13.sp), maxLines = 1)
            }
            val d = m.dayChange
            Text(
                (if (d > 0) "+" else "") + d.formatUzsL() + " " + trSaved("kecha bilan", "за день", "since yesterday"),
                style = TextStyle(color = if (d < 0) WidgetColors.rose else WidgetColors.emerald, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
        if (!small && m.usdRate != null) {
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                val diff = m.usdDiff ?: 0.0
                Text(
                    "USD ${m.usdRate.formatRate()}  " + (if (diff >= 0) "▲ +" else "▼ ") + diff.fixed(2),
                    style = TextStyle(color = WidgetColors.soft, fontSize = 12.sp), maxLines = 1
                )
            }
        }
    }
}
