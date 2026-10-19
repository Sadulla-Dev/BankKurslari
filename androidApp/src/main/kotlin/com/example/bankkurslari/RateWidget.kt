package com.example.bankkurslari

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home-screen widget (spec 2.3 #7): best rate to sell and best rate to buy, in the user's voice. */
class RateWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        initPlatform(context)
        // Render the cached value immediately, then kick off a fresh network fetch.
        renderAll(context, manager, ids)
        RateWorker.enqueueNow(context)
    }

    companion object {
        /** Re-render every placed widget from the latest cached data. */
        fun updateAll(context: Context) {
            initPlatform(context)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RateWidgetProvider::class.java))
            if (ids.isNotEmpty()) renderAll(context, manager, ids)
        }

        /** Last known best rates: dedicated cache first, then the app's raw rates cache. Never throws. */
        private fun bestOrNull(context: Context, cur: String): RateService.Best? =
            RateService.loadBest(cur)
                ?: runCatching {
                    RateService.loadCache("rates_$cur")?.let { RateService.bestFromBody(cur, it) }
                }.getOrNull()

        private fun renderAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val cur = RateService.selectedCurrency()
            val currency = Currency.entries.firstOrNull { it.code == cur } ?: Currency.USD
            val views = RemoteViews(context.packageName, R.layout.widget_rate)
            val best = runCatching { bestOrNull(context, cur) }.getOrNull()
            val snap = RateService.loadSnapshot(cur)

            val som = trSaved("soʻm", "сум", "UZS")
            views.setTextViewText(R.id.widget_currency, "$cur / $som")
            views.setTextViewText(R.id.widget_sell_label, trSaved("${currency.nameUz} sotsangiz", "Продаёте ${currency.nameRu.lowercase()}", "Selling ${currency.nameEn.lowercase()}"))
            views.setTextViewText(R.id.widget_buy_label, trSaved("${currency.nameUz} olsangiz", "Покупаете ${currency.nameRu.lowercase()}", "Buying ${currency.nameEn.lowercase()}"))

            if (best != null) {
                views.setTextViewText(R.id.widget_sell_value, best.sellRate.formatUzs())
                views.setTextViewText(R.id.widget_sell_bank, best.sellBank ?: "")
                views.setTextViewText(R.id.widget_buy_value, best.buyRate.formatUzs())
                views.setTextViewText(R.id.widget_buy_bank, best.buyBank ?: "")
                val time = best.updatedAt.ifEmpty {
                    if (best.fetchedAt > 0) SimpleDateFormat("HH:mm", Locale.US).format(Date(best.fetchedAt)) else ""
                }
                views.setTextViewText(R.id.widget_time, time)
            } else {
                views.setTextViewText(R.id.widget_sell_value, "—")
                views.setTextViewText(R.id.widget_sell_bank, "")
                views.setTextViewText(R.id.widget_buy_value, "—")
                views.setTextViewText(R.id.widget_buy_bank, "")
                views.setTextViewText(R.id.widget_time, trSaved("Yangilanmoqda…", "Обновление…", "Updating…"))
            }
            views.setTextViewText(
                R.id.widget_cbu,
                if (snap != null) "${trSaved("Markaziy bank", "Центробанк", "Central Bank")} ${RateService.formatRate(snap.rate)} $som${if (snap.date.isNotEmpty()) " · ${snap.date}" else ""}" else ""
            )

            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
