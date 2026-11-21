package com.example.bankkurslari

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll

/**
 * One home-screen widget per currency, so each shows in the picker on its own. [RateWidgetProvider] keeps its old
 * name (the dollar widget) so widgets people already placed keep working.
 */
abstract class CurrencyWidgetReceiver(widget: GlanceAppWidget) : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = widget

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Show the cached rates now, then fetch fresh ones in the background.
        RateWorker.enqueueNow(context)
    }
}

class RateWidgetProvider : CurrencyWidgetReceiver(UsdWidget()) {
    companion object {
        /** Re-render every placed widget, all currencies, from the latest cached data. */
        suspend fun updateAll(context: Context) {
            initPlatform(context)
            UsdWidget().updateAll(context)
            EurWidget().updateAll(context)
            RubWidget().updateAll(context)
            CompactUsdWidget().updateAll(context)
            CompactEurWidget().updateAll(context)
            CompactRubWidget().updateAll(context)
            WalletWidget().updateAll(context)
        }
    }
}

class EurWidgetProvider : CurrencyWidgetReceiver(EurWidget())
class RubWidgetProvider : CurrencyWidgetReceiver(RubWidget())
class CompactUsdWidgetProvider : CurrencyWidgetReceiver(CompactUsdWidget())
class CompactEurWidgetProvider : CurrencyWidgetReceiver(CompactEurWidget())
class CompactRubWidgetProvider : CurrencyWidgetReceiver(CompactRubWidget())

val WIDGET_CURRENCIES = listOf("USD", "EUR", "RUB")
