package com.example.bankkurslari

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

fun String.formatUpdatedAt(): String {
    val timePart = substringAfter('T', missingDelimiterValue = "")
    return if (timePart.isEmpty()) this else timePart.take(5)
}

/** Whole so'm for big-unit currencies (USD/EUR), two decimals for RUB-scale rates. */
fun fmtSom(v: Double, rate: Double): String =
    if (rate >= 1000) kotlin.math.abs(v).roundToLong().formatUzsL() else kotlin.math.abs(v).fixed(2)
