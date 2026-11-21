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
import kotlin.math.roundToLong
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
        // All three: each currency has its own home-screen widget.
        val currencies = (listOf(selected) + WIDGET_CURRENCIES + alerts.map { it.currency }).distinct()

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
                    if (!previous.isNaN() && previous != snapshot.rate) {
                        // Once a day the CBU publishes a new rate: send the daily summary (rate, best banks, wallet).
                        val wallet = WalletStore.load().takeIf { it.isNotEmpty() }?.let { list ->
                            val quotes = cachedQuotes() + fetchQuotes()
                            walletSummary(list, quotes).takeIf { s -> s.rows.any { it.som != null } }
                        }
                        notifyDailySummary(ctx, snapshot, previous, payload, wallet)
                    }
                }
            }
            evaluateAlerts(ctx, currency, payload.banks)
        }
        maybeWeeklySummary(ctx)
        // Wallet lines in GBP, KZT or gold need their own rates for the wallet widget.
        if (WalletStore.load().any { it.code !in setOf(WalletCode.USD, WalletCode.EUR, WalletCode.RUB, WalletCode.UZS) }) {
            runCatching { fetchQuotes() }
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
                trSaved("Kunlik xulosa", "Сводка за день", "Daily summary"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = trSaved("Markaziy bank yangi kursni eʼlon qilganda: kurs, eng yaxshi banklar va hamyoningiz", "Когда ЦБ объявляет новый курс: курс, лучшие банки и ваш кошелёк", "When the CBU publishes a new rate: rate, best banks and your wallet") }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        fun ensureAlertChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                trSaved("Kurs ogohlantirishlari", "Оповещения о курсе", "Rate alerts"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = trSaved("Belgilangan kursga yetganda xabar", "Уведомление при достижении заданного курса", "Notification when the rate reaches your target")
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
            val cur = Currency.entries.firstOrNull { it.code == a.currency } ?: Currency.USD
            val op = if (a.side == AlertSide.SELL) "≥" else "≤"
            val unit = trSaved("soʻm", "сум", "UZS")
            val title = "${a.currency} $op ${a.threshold.formatUzs()} $unit"
            val v = "${value.formatUzs()} $unit"
            val text = if (a.side == AlertSide.SELL)
                trSaved("${cur.nameUz} sotsangiz: $bank $v toʻlaydi", "Если продаёте ${cur.nameRu.lowercase()}: $bank платит $v", "Selling ${cur.nameEn.lowercase()}: $bank pays $v")
            else
                trSaved("${cur.nameUz} olsangiz: $bank $v dan sotadi", "Если покупаете ${cur.nameRu.lowercase()}: $bank продаёт по $v", "Buying ${cur.nameEn.lowercase()}: $bank sells at $v")
            val open = PendingIntent.getActivity(
                context, a.id.hashCode(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notes_mono)
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

        /**
         * Sunday from 10:00, once: the week in three lines (dollar, wallet, goal). The periodic worker runs every
         * ~30 minutes, so the first run after 10:00 on Sunday sends it; "weekly_sent" keeps it to one per Sunday.
         */
        private suspend fun maybeWeeklySummary(context: Context) {
            val cal = java.util.Calendar.getInstance()
            if (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY || cal.get(java.util.Calendar.HOUR_OF_DAY) < 10) return
            val today = todayIso()
            val store = appStore()
            if (store.getString("weekly_sent") == today) return
            if (!canNotify(context)) return
            val lines = runCatching { weeklySummaryLines(today) }.getOrNull() ?: return
            ensureChannel(context)
            val open = PendingIntent.getActivity(
                context, 2,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_TAB, if (WalletStore.load().isNotEmpty()) HomeTab.WALLET.name else HomeTab.TREND.name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notes_mono)
                .setContentTitle(trSaved("Haftalik xulosa", "Итоги недели", "Your week"))
                .setContentText(lines.first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify("weekly".hashCode(), n)
                store.putString("weekly_sent", today) // only once it is really posted
            } catch (_: SecurityException) {
            }
        }

        /** Enqueue a one-off refresh (used on app start and by the widget). */
        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RateWorker>().build()
            WorkManager.getInstance(context)
                // KEEP: app start, the currency switch and every widget's onUpdate all ask for a refresh; REPLACE
                // cancelled the one already running each time, so with several widgets placed it rarely finished.
                // The worker fetches every currency anyway, so the running refresh covers the new request.
                .enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.KEEP, request)
        }

        /** Schedule a recurring background check (minimum WorkManager period is 15 min). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RateWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * Daily summary, posted when the CBU rate for the selected currency changes (once a day):
         *   USD 12 004 so'm  ▲ +33
         *   Selling: Anorbank 11 995 · Buying: Openbank 12 010
         *   Wallet: 17 955 000 so'm (+49 260 since yesterday)
         */
        private fun notifyDailySummary(context: Context, snap: RateService.Snapshot, previous: Double, payload: RatesPayload, wallet: WalletSummary?) {
            if (!canNotify(context)) return
            ensureChannel(context)
            // The CBU's own change against its previous rate; our last check can be older than a day.
            val delta = payload.cbu.diff.takeIf { it != 0.0 } ?: (snap.rate - previous)
            val arrow = if (delta >= 0) "▲ +" else "▼ −"
            val unit = trSaved("soʻm", "сум", "UZS")
            val title = "${snap.currency} ${RateService.formatRate(snap.rate)} $unit  $arrow${DecimalFormat("0.##").format(abs(delta))}"

            val lines = mutableListOf<String>()
            val sellAt = payload.banks.filter { it.buy != null }.maxByOrNull { it.buy!! }
            val buyAt = payload.banks.filter { it.sell != null }.minByOrNull { it.sell!! }
            if (sellAt != null && buyAt != null) {
                lines += trSaved(
                    "Sotish: ${bankShortName(sellAt.bank)} ${sellAt.buy.formatUzs()} · Olish: ${bankShortName(buyAt.bank)} ${buyAt.sell.formatUzs()}",
                    "Продать: ${bankShortName(sellAt.bank)} ${sellAt.buy.formatUzs()} · Купить: ${bankShortName(buyAt.bank)} ${buyAt.sell.formatUzs()}",
                    "Sell: ${bankShortName(sellAt.bank)} ${sellAt.buy.formatUzs()} · Buy: ${bankShortName(buyAt.bank)} ${buyAt.sell.formatUzs()}"
                )
            }
            if (wallet != null) {
                val d = wallet.dayChange.roundToLong()
                val dStr = (if (d > 0) "+" else "") + d.formatUzsL()
                lines += trSaved(
                    "Hamyon: ${wallet.total.roundToLong().formatUzsL()} $unit ($dStr)",
                    "Кошелёк: ${wallet.total.roundToLong().formatUzsL()} $unit ($dStr)",
                    "Wallet: ${wallet.total.roundToLong().formatUzsL()} $unit ($dStr)"
                )
            }
            val text = lines.joinToString("\n")
            val open = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notes_mono)
                .setContentTitle(title)
                .setContentText(lines.firstOrNull().orEmpty())
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(snap.currency.hashCode(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}
