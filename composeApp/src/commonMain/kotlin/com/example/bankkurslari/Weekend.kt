package com.example.bankkurslari

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlin.math.roundToLong

/*
 * Banks open on Saturday and Sunday (/api/weekend, built by backend/src/weekend.js and weekendBranches.js).
 *
 * Every week the Central Bank names the banks that open some offices on the weekend, and each bank publishes its own
 * list of those offices. The API reads both and ties every office it can to a point of /api/branches. Here that becomes
 * one index: which branch points are open on Saturday and Sunday, and with what hours.
 *
 * Only what the banks say: an office counts when this weekend's list of its bank names it (while that weekend is still
 * ahead), or when its own usual hours include the weekend and the bank is one whose usual hours were checked against
 * its weekly lists (the same list as the site, WEEKEND_HOURS_CHECKED in web/public/shared.js). A list that names days
 * but no hours gives "open" without a time; hours are never made up.
 */

/** A branch's weekend: [sat] / [sun] are "09:00–13:00", "" (open, the list gives no hours) or null (closed). */
data class WeekendOpen(val sat: String?, val sun: String?)

/** One bank's list for this weekend: [status] "ok" (offices read), "all" (every office open), or why it was not read. */
data class WeekendList(val bank: String, val status: String, val count: Int)

/** One office on a bank's list, with the branch point it was tied to (null: the list's address matched no point). */
data class WeekendOffice(val bank: String, val open: WeekendOpen, val lat: Double?, val lng: Double?)

class WeekendData(
    /** Saturday and Sunday the Central Bank's list is for (yyyy-MM-dd), null when the list did not say. */
    val days: Pair<String, String>?,
    /** True when [offices] were read from the banks' lists for this same Central Bank list. */
    val officesCurrent: Boolean,
    val lists: List<WeekendList>,
    val offices: List<WeekendOffice>
)

fun parseWeekend(body: String): WeekendData {
    val o = parseJsonObject(body)
    val days = o.arr("days")?.let { a -> (a as JsonArray).mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } }
        ?.takeIf { it.size == 2 }?.let { it[0] to it[1] }
    fun open(x: kotlinx.serialization.json.JsonObject?) = WeekendOpen(x?.str("Sa"), x?.str("Su"))
    return WeekendData(
        days = days,
        officesCurrent = o.str("branchesListUrl") != null && o.str("branchesListUrl") == o.str("listUrl"),
        lists = (o.arr("branchLists") ?: JsonArray(emptyList())).objects().map {
            WeekendList(it.str("bank") ?: it.str("name").orEmpty(), it.str("status").orEmpty(), it.int("count") ?: 0)
        },
        offices = (o.arr("weekendBranches") ?: JsonArray(emptyList())).objects().mapNotNull {
            val p = it.obj("point")
            WeekendOffice(it.str("bank") ?: return@mapNotNull null, open(it.obj("open")), p?.dbl("lat"), p?.dbl("lng"))
        }
    )
}

val backendWeekendUrl: String get() = "$backendBase/api/weekend"

suspend fun cachedWeekend(): WeekendData? = withContext(Dispatchers.Default) {
    RateService.loadCache("weekend")?.let { runCatching { parseWeekend(it) }.getOrNull() }
}

suspend fun fetchWeekend(): WeekendData = withContext(Dispatchers.Default) {
    val body = httpGet(backendWeekendUrl, 15_000)
    val parsed = parseWeekend(body)
    RateService.saveCache("weekend", body)
    parsed
}

/** Today in Tashkent (UTC+5 all year). */
fun tashkentToday(): LocalDate =
    LocalDate.fromEpochDays(((kotlin.time.Clock.System.now().toEpochMilliseconds() + 5 * 3_600_000L) / 86_400_000L).toInt())

/**
 * Banks whose usual weekend hours were checked against their own weekly lists (2026-11-27): Anorbank, Asia Alliance,
 * Hamkorbank, Hayot, Ipak Yo‘li, Tenge, Orient Finans. Ipoteka bank and BRB failed that check (their branch pages claim
 * weekend hours their lists contradict); the API already drops those.
 */
private val WEEKEND_HOURS_CHECKED = listOf("anor", "asiaalliance", "hamkor", "hayot", "ipakyuli", "ipakyoli", "tenge", "orientfinans")
fun weekendHoursChecked(bank: String): Boolean = bankKey(bank).let { k -> WEEKEND_HOURS_CHECKED.any { k.contains(it) } }

/** Map key of a branch point: bank and position to 5 decimals (about a metre), as the API ties offices to points. */
fun weekendPointKey(bank: String, lat: Double, lng: Double): String = weekendKeyOf(bankKey(bank), lat, lng)
fun weekendKeyOf(bankKey: String, lat: Double, lng: Double): String =
    "$bankKey|${(lat * 1e5).roundToLong()}|${(lng * 1e5).roundToLong()}"

/** One bank open this weekend: how many offices, and whether every office is ([all], e.g. Anorbank's list). */
data class WeekendBank(val bank: String, val offices: Int, val all: Boolean)

class WeekendIndex(
    /** Saturday and Sunday this index is for; null when built from usual hours only (no current list). */
    val days: Pair<String, String>?,
    /** branch point (weekendPointKey) -> its weekend hours. */
    val points: Map<String, WeekendOpen>,
    /** bankKey -> the bank. */
    val banks: Map<String, WeekendBank>
) {
    fun at(bank: String, lat: Double, lng: Double): WeekendOpen? = points[weekendPointKey(bank, lat, lng)]
    /** For a map pin, which carries its bank as a bankKey already. */
    fun atKey(bankKey: String, lat: Double, lng: Double): WeekendOpen? = points[weekendKeyOf(bankKey, lat, lng)]
}

private fun span(s: HoursSchedule, day: Int): String? {
    if (s.always) return "00:00–24:00"
    val spans = s.days[day]
    if (spans.isEmpty()) return null
    fun hm(m: Int) = "${(m / 60 % 24).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}".let { if (m == 1440) "24:00" else it }
    return spans.joinToString(", ") { "${hm(it.start)}–${hm(it.end)}" }
}

/**
 * The weekend index, or null when there is nothing to show: from Thursday, once this weekend's list is out, until the
 * end of Sunday; on Saturday and Sunday also without a list (usual hours of the checked banks only).
 */
fun buildWeekendIndex(data: WeekendData?, branches: List<OsmBranch>, today: LocalDate = tashkentToday()): WeekendIndex? {
    val todayIso = today.toString()
    val listDays = data?.days?.takeIf { it.second >= todayIso }
    val weekendNow = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
    if (listDays == null && !weekendNow) return null
    val useList = listDays != null && data.officesCurrent
    val points = HashMap<String, WeekendOpen>()
    val listCount = HashMap<String, Int>()     // bankKey -> offices on its list
    val listBank = HashMap<String, String>()
    val allOpen = HashSet<String>()
    val listed = HashSet<String>()             // banks whose own list was read: their list alone counts
    if (useList) {
        for (l in data.lists) {
            val k = bankKey(l.bank)
            if (l.status == "ok" && l.count > 0) { listed += k; listCount[k] = l.count; listBank[k] = l.bank }
            if (l.status == "all") { allOpen += k; listBank[k] = l.bank }
        }
        for (o in data.offices) {
            if (o.lat == null || o.lng == null) continue
            if (o.open.sat == null && o.open.sun == null) continue
            points[weekendPointKey(o.bank, o.lat, o.lng)] = o.open
        }
    }
    // Usual hours that include the weekend, for the checked banks whose list this weekend was not read (or there is none).
    val usual = HashMap<String, Int>()
    for (br in branches) {
        val k = bankKey(br.bank)
        if (k in listed || !weekendHoursChecked(br.bank)) continue
        val s = parseHours(br.hours) ?: continue
        val open = WeekendOpen(span(s, 5), span(s, 6))
        if (open.sat == null && open.sun == null) continue
        val key = weekendPointKey(br.bank, br.lat, br.lng)
        if (key !in points) { points[key] = open; usual[k] = (usual[k] ?: 0) + 1; listBank.getOrPut(k) { br.bank } }
    }
    val banks = HashMap<String, WeekendBank>()
    for ((k, name) in listBank) {
        val n = maxOf(listCount[k] ?: 0, usual[k] ?: 0)
        if (n > 0 || k in allOpen) banks[k] = WeekendBank(name, n, k in allOpen)
    }
    if (banks.isEmpty()) return null
    return WeekendIndex(if (useList) listDays else null, points, banks)
}

/** The bank of [index] a rate-table name belongs to ("Xalq Banki" / "Xalq banki", "Ipoteka bank" / "Ipoteka-bank"). */
fun WeekendIndex.bankFor(rateBank: String): WeekendBank? {
    val k = bankKey(rateBank)
    banks[k]?.let { return it }
    return banks.entries.firstOrNull { (bk, _) -> keysMatch(bk, k) }?.value
}

/** "28–29-noyabr" / "28–29 ноября" / "November 28–29" (months differ: "31-oktabr – 1-noyabr"). */
fun weekendRangeText(days: Pair<String, String>, lang: Lang): String {
    val (a, b) = days.first.split("-").map { it.toInt() } to days.second.split("-").map { it.toInt() }
    val uz = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr")
    val ru = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    val en = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val same = a[1] == b[1]
    return when (lang) {
        Lang.RU -> if (same) "${a[2]}–${b[2]} ${ru[a[1] - 1]}" else "${a[2]} ${ru[a[1] - 1]} – ${b[2]} ${ru[b[1] - 1]}"
        Lang.EN -> if (same) "${en[a[1] - 1]} ${a[2]}–${b[2]}" else "${en[a[1] - 1]} ${a[2]} – ${en[b[1] - 1]} ${b[2]}"
        else -> pick(lang, if (same) "${a[2]}–${b[2]}-${uz[a[1] - 1]}" else "${a[2]}-${uz[a[1] - 1]} – ${b[2]}-${uz[b[1] - 1]}", "")
    }
}

/** "Shanba 09:00–13:00 · Yakshanba yopiq"; "ochiq" where the list gives no time. */
fun weekendOpenText(o: WeekendOpen, lang: Lang): String {
    fun day(v: String?) = when {
        v == null -> pick(lang, "yopiq", "выходной", "closed")
        v.isEmpty() -> pick(lang, "ochiq", "открыто", "open")
        else -> v
    }
    return pick(lang, "Shanba", "Суббота", "Saturday") + " " + day(o.sat) + " · " + pick(lang, "Yakshanba", "Воскресенье", "Sunday") + " " + day(o.sun)
}

/** Map tab request from the Kurs card: open the map with only the branches open this weekend. */
object WeekendRequests {
    private val state = androidx.compose.runtime.mutableStateOf(false)
    var showOnMap: Boolean
        get() = state.value
        set(v) { state.value = v }
}
