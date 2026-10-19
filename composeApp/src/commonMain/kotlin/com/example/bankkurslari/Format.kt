package com.example.bankkurslari

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** "1234567" -> "1 234 567" (plain space, as on bank boards). */
fun groupDigits(digits: String, sep: Char = ' '): String {
    val sb = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(sep)
        sb.append(c)
    }
    return sb.toString()
}

/** Fixed decimals with '.' separator and half-up rounding: 11913.345.fixed(2) = "11913.35". */
fun Double.fixed(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val r = (abs(this) * factor).roundToLong()
    val f = factor.toLong()
    val int = (r / f).toString()
    val sign = if (this < 0 && r != 0L) "-" else ""
    if (decimals == 0) return sign + int
    return sign + int + "." + (r % f).toString().padStart(decimals, '0')
}

fun Int?.formatUzs(): String = this?.toLong()?.formatUzsL() ?: "—"

fun Long.formatUzsL(): String = (if (this < 0) "-" else "") + groupDigits(abs(this).toString())

/** "#,##0.00" with space grouping: 11 913.35 */
fun Double.formatRate(): String {
    val s = fixed(2)
    val neg = s.startsWith("-")
    val body = s.removePrefix("-")
    val int = body.substringBefore('.')
    return (if (neg) "-" else "") + groupDigits(int) + "." + body.substringAfter('.')
}

/** "#,##0.##" with sign: +14.26, -3, +1 200 */
fun Double.formatSigned(): String {
    val s = abs(this).fixed(2).trimEnd('0').trimEnd('.')
    val int = s.substringBefore('.')
    val frac = s.substringAfter('.', "")
    val body = groupDigits(int) + if (frac.isNotEmpty()) ".$frac" else ""
    return if (this >= 0) "+$body" else "-$body"
}

/** "HH:mm" in the device time zone. The server sends UTC ("…T06:11:00Z"); slicing the string leaked UTC to the UI. */
fun String.formatUpdatedAt(): String {
    val ms = kParseMillis(this) ?: return substringAfter('T', this).take(5)
    val t = kotlin.time.Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

/** "just now" / "2 min ago" / "3 h ago" / "yesterday" relative to now, in the UI language. null when unparseable. */
fun updatedAgo(updatedAt: String?): String? {
    val ms = kParseMillis(updatedAt) ?: return null
    val min = ((nowMillis() - ms) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> pick(lang, "Hozirgina yangilandi", "Обновлено только что", "Updated just now")
        min < 60 -> pick(lang, "$min daqiqa oldin yangilandi", "Обновлено $min мин назад", "Updated $min min ago")
        min < 24 * 60 -> (min / 60).let { pick(lang, "$it soat oldin yangilandi", "Обновлено $it ч назад", "Updated $it h ago") }
        else -> (min / (24 * 60)).let { pick(lang, "$it kun oldin yangilandi", "Обновлено $it дн. назад", "Updated $it d ago") }
    }
}

/** Whole so'm for big-unit currencies (USD/EUR), two decimals for RUB-scale rates. */
fun fmtSom(v: Double, rate: Double): String =
    if (rate >= 1000) kotlin.math.abs(v).roundToLong().formatUzsL() else kotlin.math.abs(v).fixed(2)
