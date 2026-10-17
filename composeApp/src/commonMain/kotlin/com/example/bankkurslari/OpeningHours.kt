package com.example.bankkurslari

import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime


/** One open interval in minutes since midnight. end may exceed 1440 for overnight spans. */
data class HoursSpan(val start: Int, val end: Int)

/** Parsed OSM opening_hours: days[0]=Monday .. days[6]=Sunday. */
class HoursSchedule(val always: Boolean, val days: Array<List<HoursSpan>>)

private val DAY_TOKENS = listOf("mo", "tu", "we", "th", "fr", "sa", "su")
private val TIME_RE = Regex("""^(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})$""")
private val OFF_RE = Regex("""\b(off|closed)\b""", RegexOption.IGNORE_CASE)

private fun parseDayPart(part: String): Set<Int>? {
    val out = mutableSetOf<Int>()
    for (raw in part.split(',')) {
        val tok = raw.trim().lowercase()
        if (tok.isEmpty()) continue
        if (tok == "ph" || tok == "sh") continue // public / school holidays: ignored
        if ('-' in tok) {
            val (a, b) = tok.split('-', limit = 2).map { it.trim() }
            val ia = DAY_TOKENS.indexOf(a)
            val ib = DAY_TOKENS.indexOf(b)
            if (ia < 0 || ib < 0) return null
            var i = ia
            while (true) {
                out += i
                if (i == ib) break
                i = (i + 1) % 7
            }
        } else {
            val i = DAY_TOKENS.indexOf(tok)
            if (i < 0) return null
            out += i
        }
    }
    return out
}

private fun parseSpans(rest: String): List<HoursSpan>? {
    if (rest.equals("off", true) || rest.equals("closed", true)) return emptyList()
    val spans = mutableListOf<HoursSpan>()
    for (piece in rest.split(',')) {
        val m = TIME_RE.matchEntire(piece.trim()) ?: return null
        val s = m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
        var e = m.groupValues[3].toInt() * 60 + m.groupValues[4].toInt()
        if (s > 1440 || e > 1440 || m.groupValues[2].toInt() > 59 || m.groupValues[4].toInt() > 59) return null
        if (e <= s) e += 1440 // overnight
        spans += HoursSpan(s, e)
    }
    return spans
}

private val hoursMemo = HashMap<String, HoursSchedule?>()
private val hoursLock = PlatformLock() // used from the UI thread and from background pin building

/** Returns null when the text cannot be understood (caller treats hours as unknown). Memoised: thousands of branches
 *  share a handful of distinct schedule strings. */
fun parseHours(raw: String?): HoursSchedule? {
    val key = raw?.trim().orEmpty()
    if (key.isEmpty()) return null
    val hit = hoursLock.withLock { if (hoursMemo.containsKey(key)) Result.success(hoursMemo[key]) else null }
    if (hit != null) return hit.getOrNull()
    val parsed = parseHoursUncached(key)
    hoursLock.withLock { if (hoursMemo.size < 4096) hoursMemo[key] = parsed }
    return parsed
}

private fun parseHoursUncached(raw: String?): HoursSchedule? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    val days = arrayOfNulls<List<HoursSpan>>(7)
    var any = false
    for (ruleRaw in text.split(';')) {
        val rule = ruleRaw.trim()
        if (rule.isEmpty()) continue
        if (rule.equals("24/7", true)) {
            for (d in 0..6) days[d] = listOf(HoursSpan(0, 1440))
            any = true
            continue
        }
        val firstDigit = rule.indexOfFirst { it.isDigit() }
        val offMatch = OFF_RE.find(rule)
        val idx = listOfNotNull(firstDigit.takeIf { it >= 0 }, offMatch?.range?.first).minOrNull() ?: return null
        val dayPart = rule.substring(0, idx).trim()
        val rest = rule.substring(idx).trim()
        val spans = parseSpans(rest) ?: return null
        val dayIdx = if (dayPart.isEmpty()) (0..6).toSet() else parseDayPart(dayPart) ?: return null
        for (d in dayIdx) {
            days[d] = spans
            any = true
        }
    }
    if (!any) return null
    return HoursSchedule(false, Array(7) { days[it] ?: emptyList() })
}

private fun hhmm(minutes: Int): String {
    val m = ((minutes % 1440) + 1440) % 1440
    return "${(m / 60).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}"
}

/**
 * untilText: when open = closing time "18:00" (null when open around the clock);
 * when closed = today's next opening time "09:00" (null if it will not open again today).
 * Unknown / unparseable hours -> known=false, open=true.
 */
private val localZone by lazy { TimeZone.currentSystemDefault() }

/** "Now" in local time, refreshed at most once a second (openState is called for every bank and branch). */
private var nowCache: Pair<Long, LocalDateTime>? = null

private fun localNow(): LocalDateTime {
    val ms = Clock.System.now().toEpochMilliseconds()
    nowCache?.let { (at, v) -> if (ms - at < 1000) return v } // a stale read from another thread is harmless
    return Clock.System.now().toLocalDateTime(localZone).also { nowCache = ms to it }
}

fun openState(hours: String?): OpenState = openState(hours, localNow())

fun openState(hours: String?, now: LocalDateTime): OpenState {
    val sch = parseHours(hours) ?: return OpenState(false, true, null)
    if (sch.always) return OpenState(true, true, null)
    val dow = now.dayOfWeek.isoDayNumber - 1 // Monday = 0
    val minute = now.hour * 60 + now.minute
    for (span in sch.days[dow]) {
        if (minute >= span.start && minute < span.end) {
            val full = span.start == 0 && span.end == 1440
            return OpenState(true, true, if (full) null else if (span.end == 1440) "24:00" else hhmm(span.end))
        }
    }
    for (span in sch.days[(dow + 6) % 7]) { // overnight spill from yesterday
        if (span.end > 1440 && minute < span.end - 1440) return OpenState(true, true, hhmm(span.end))
    }
    val next = sch.days[dow].filter { it.start > minute }.minOfOrNull { it.start }
    return OpenState(true, false, next?.let { hhmm(it) })
}

/** Human schedule like "Du–Ju 09:00–18:00 · Sh 09:00–13:00" (UZ) / "Пн–Пт 09:00–18:00 · Сб 09:00–13:00" (RU). */
fun formatHours(raw: String?, ru: Boolean): String? {
    val sch = parseHours(raw) ?: return null
    if (sch.always) return "24/7"
    val names = when {
        ru -> listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        lang == Lang.EN -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        lang == Lang.UZ_CYRL -> listOf("Ду", "Се", "Чо", "Па", "Жу", "Ша", "Як")
        else -> listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")
    }
    val keys = sch.days.map { spans ->
        spans.joinToString(", ") {
            val e = if (it.end > 1440) it.end - 1440 else it.end
            "${hhmm(it.start)}–${if (e == 1440) "24:00" else hhmm(e)}"
        }
    }
    val parts = mutableListOf<String>()
    var i = 0
    while (i < 7) {
        if (keys[i].isEmpty()) { i++; continue }
        var j = i
        while (j + 1 < 7 && keys[j + 1] == keys[i]) j++
        parts += (if (j == i) names[i] else "${names[i]}–${names[j]}") + " " + keys[i]
        i = j + 1
    }
    return parts.joinToString(" · ").ifEmpty { null }
}
