package com.example.bankkurslari

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Periodically fetches the selected currency's CB rate, refreshes the widget and
 *  posts a notification when the rate has changed since the last check. */
class RateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        initPlatform(ctx)
        val selected = RateService.selectedCurrency()
        val alerts = Alerts.load()
        val currencies = (listOf(selected) + alerts.map { it.currency }).distinct()

        var selectedOk = false
        var anyOk = false
        for (currency in currencies) {
            val body = runCatching { RateService.fetchRatesBody(currency) }.getOrNull() ?: continue
            anyOk = true
            RateService.saveCache("rates_$currency", body) // offline cache shared with the UI
            val payload = runCatching { parseRatesPayload(body) }.getOrNull() ?: continue
            runCatching { RateService.saveBest(RateService.bestFromBody(currency, body)) }

            if (currency == selected) {
                val snapshot = runCatching { RateService.snapshotFromBody(currency, body) }.getOrNull()
                if (snapshot != null) {
                    selectedOk = true
                    val previous = RateService.lastRate(currency) // NaN on first run
                    RateService.saveSnapshot(snapshot)
                    if (!previous.isNaN() && previous != snapshot.rate) notifyRateChange(ctx, snapshot, previous)
                }
            }
            evaluateAlerts(ctx, currency, payload.banks)
        }
        RateWidgetProvider.updateAll(ctx)
        if (!anyOk || !selectedOk) Result.retry() else Result.success()
    }

    companion object {
        const val CHANNEL_ID = "rate_updates"
        const val ALERT_CHANNEL_ID = "rate_alerts"
        private const val PERIODIC_NAME = "rate_periodic"
        private const val ONE_TIME_NAME = "rate_now"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return // no channels before Android 8
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kurs yangilanishlari",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Valyuta kursi o'zgarganda bildirishnoma" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        fun ensureAlertChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val uz = RateService.language() != "ru"
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                if (uz) "Kurs ogohlantirishlari" else "Оповещения о курсе",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (uz) "Belgilangan kursga yetganda xabar" else "Уведомление при достижении заданного курса"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun canNotify(context: Context): Boolean =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED

        /** Fires each alert once when its condition becomes true; re-arms when it becomes false (no spam). */
        private fun evaluateAlerts(context: Context, currency: String, banks: List<BankRate>) {
            if (banks.isEmpty()) return
            val toNotify = mutableListOf<Triple<RateAlert, String, Int>>()
            Alerts.update() { list ->
                list.map { a ->
                    if (a.currency != currency) return@map a
                    val best = Alerts.bestFor(a, banks) ?: return@map a
                    val met = Alerts.isMet(a.side, best.second, a.threshold)
                    when {
                        met && !a.fired -> {
                            toNotify += Triple(a, best.first, best.second)
                            a.copy(fired = true, lastFiredAt = nowMillis())
                        }
                        !met && a.fired -> a.copy(fired = false)
                        else -> a
                    }
                }
            }
            toNotify.forEach { (a, bank, value) -> notifyAlert(context, a, bank, value) }
        }

        private fun notifyAlert(context: Context, a: RateAlert, bank: String, value: Int) {
            if (!canNotify(context)) return
            ensureAlertChannel(context)
            val uz = RateService.language() != "ru"
            val cur = Currency.entries.firstOrNull { it.code == a.currency } ?: Currency.USD
            val name = if (uz) cur.nameUz else cur.nameRu.lowercase()
            val op = if (a.side == AlertSide.SELL) "≥" else "≤"
            val unit = if (uz) "soʻm" else "сум"
            val title = "${a.currency} $op ${a.threshold.formatUzs()} $unit"
            val text = if (uz) {
                if (a.side == AlertSide.SELL) "$name sotsangiz: $bank ${value.formatUzs()} $unit toʻlaydi"
                else "$name olsangiz: $bank ${value.formatUzs()} $unit dan sotadi"
            } else {
                if (a.side == AlertSide.SELL) "Если продаёте $name: $bank платит ${value.formatUzs()} $unit"
                else "Если покупаете $name: $bank продаёт по ${value.formatUzs()} $unit"
            }
            val open = PendingIntent.getActivity(
                context, a.id.hashCode(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            // canNotify() was checked above; the permission can still be revoked in between.
            try {
                NotificationManagerCompat.from(context).notify(("alert_" + a.id).hashCode(), n)
            } catch (_: SecurityException) {
            }
        }

        /** Enqueue a one-off refresh (used on app start and by the widget). */
        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RateWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Schedule a recurring background check (minimum WorkManager period is 15 min). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RateWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun notifyRateChange(context: Context, snap: RateService.Snapshot, previous: Double) {
            ensureChannel(context)
            val delta = snap.rate - previous
            val up = delta >= 0
            val arrow = if (up) "▲" else "▼"
            val deltaStr = (if (up) "+" else "-") + DecimalFormat("0.##").format(abs(delta))
            val symbol = RateService.symbol(snap.currency)

            val title = "${snap.currency} kursi yangilandi"
            val text = "$symbol 1 ${snap.currency} = ${RateService.formatRate(snap.rate)} UZS  $arrow $deltaStr"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            // On Android 13+ posting requires the runtime POST_NOTIFICATIONS permission.
            if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
            ) {
                NotificationManagerCompat.from(context).notify(snap.currency.hashCode(), notification)
            }
        }
    }
}
