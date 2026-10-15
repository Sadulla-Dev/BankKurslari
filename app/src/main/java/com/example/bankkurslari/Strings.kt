package com.example.bankkurslari

import androidx.compose.runtime.staticCompositionLocalOf

enum class Lang(val code: String) { UZ("uz"), RU("ru") }

/** All user-facing strings. Two implementations: [UzStrings] and [RuStrings]. */
interface AppStrings {
    // Loading / error
    val loading: String
    val errorTitle: String
    val errorHint: String
    val retry: String
    val noBankRates: String
    val connectFail: String

    // Top bar
    fun subtitle(currencyName: String): String
    val live: String
    val offline: String
    val themeToggle: String

    // Tabs
    val tabHome: String
    val tabCalc: String
    val tabTrend: String
    val tabBanks: String
    val tabMap: String

    // CBU hero
    val cbuTitle: String
    fun oneUnitDate(code: String, date: String): String
    fun updated(time: String): String

    // Summary
    val bestBuyTitle: String
    val bestSellTitle: String
    fun sellHint(currencyName: String): String
    fun buyHint(currencyName: String): String
    val som: String

    // Converter
    val calcTitle: String
    fun sellMode(currencyName: String): String
    fun buyMode(currencyName: String): String
    fun amountForeign(code: String): String
    val amountUzs: String
    val getResult: String
    val savings: String
    fun bestBank(bank: String): String

    // Profit ranking
    val profitTitle: String
    fun profitSell(currencyName: String): String
    fun profitBuy(currencyName: String): String
    val route: String
    fun showMore(n: Int): String
    val showLess: String
    val worstLabel: String

    // Analytics
    fun analyticsTitle(currencyName: String): String
    val analyticsLoading: String
    val analyticsUnavailable: String
    val analyticsNoHistory: String
    val trendUp: String
    val trendDown: String
    val trendFlat: String
    fun forecastNext(pct: String): String
    fun sellAdvice(wait: Boolean, down: Boolean): String
    fun buyAdvice(wait: Boolean): String
    val statCurrent: String
    val statWeekly: String
    val statChange: String
    val statForecast: String
    val statSevenDays: String
    fun lastNDays(n: Int): String
    val zoomHint: String
    fun daysChip(d: Int): String
    fun yearsChip(y: Int): String
    val all: String
    val seasonTitle: String
    val seasonStart: String
    val seasonMid: String
    val seasonEnd: String
    fun bestRiseSentence(period: String): String
    val periodStart: String
    val periodMid: String
    val periodEnd: String

    // Bank list
    val banks: String
    val searchBank: String
    val sortByBuy: String
    val sortBySell: String
    val colBuy: String
    val colSell: String
    fun spread(n: Int): String
    fun spreadShort(n: Int): String
    val source: String
    fun noBankFound(query: String): String

    // Map
    val mapAll: String
    val mapBranches: String
    val mapAtms: String
    fun legendBranch(n: Int): String
    fun legendAtm(n: Int): String
    val branchMarker: String
    val atmMarker: String
    val hoursLabel: String
    val bankHours: String
    val atmHours: String
    val myLocation: String
    val nearMe: String
    val locationNeeded: String

    // Footer
    val footer: String
}

object UzStrings : AppStrings {
    override val loading = "Kurslar yuklanmoqda…"
    override val errorTitle = "Ma'lumot olinmadi"
    override val errorHint = "Backend serverini ishga tushiring:  cd backend && npm start"
    override val retry = "Qayta urinish"
    override val noBankRates = "Bank kurslari hozircha mavjud emas. Keyinroq urinib ko'ring."
    override val connectFail = "Serverga ulanib bo'lmadi."

    override fun subtitle(currencyName: String) = "$currencyName kurslari • O‘zbekiston"
    override val live = "Jonli"
    override val offline = "Oflayn"
    override val themeToggle = "Tun/kun rejimi"

    override val tabHome = "Asosiy"
    override val tabCalc = "Hisob"
    override val tabTrend = "Tahlil"
    override val tabBanks = "Banklar"
    override val tabMap = "Xarita"

    override val cbuTitle = "Markaziy Bank rasmiy kursi"
    override fun oneUnitDate(code: String, date: String) = "1 $code  •  sana: $date"
    override fun updated(time: String) = "Yangilandi: $time"

    override val bestBuyTitle = "Eng yaxshi sotib olish"
    override val bestSellTitle = "Eng yaxshi sotish"
    override fun sellHint(currencyName: String) = "${currencyName}ni qimmat topshirish"
    override fun buyHint(currencyName: String) = "${currencyName}ni arzon olish"
    override val som = "so'm"

    override val calcTitle = "Smart kalkulyator"
    override fun sellMode(currencyName: String) = "$currencyName sotaman"
    override fun buyMode(currencyName: String) = "$currencyName olaman"
    override fun amountForeign(code: String) = "Summa ($code)"
    override val amountUzs = "Summa (UZS)"
    override val getResult = "Qo'lga tegadi"
    override val savings = "Tejamkorlik"
    override fun bestBank(bank: String) = "Eng qulay bank: $bank"

    override val profitTitle = "Foyda reytingi"
    override fun profitSell(currencyName: String) =
        "Qaysi bankka ${currencyName.lowercase()} sotsangiz qancha foyda (o'rtacha bankka nisbatan)."
    override fun profitBuy(currencyName: String) =
        "Qaysi bankdan ${currencyName.lowercase()} olsangiz qancha foyda (o'rtacha bankka nisbatan)."
    override val route = "Marshrut"
    override fun showMore(n: Int) = "Yana $n ta bank"
    override val showLess = "Kamroq"
    override val worstLabel = "Eng past"

    override fun analyticsTitle(currencyName: String) = "$currencyName kursi tahlili"
    override val analyticsLoading = "Tahlil hisoblanmoqda…"
    override val analyticsUnavailable = "Tahlil hozircha mavjud emas. Keyinroq urinib ko'ring."
    override val analyticsNoHistory = "Tahlil uchun yetarli tarix yo'q."
    override val trendUp = "Narx oshmoqda"
    override val trendDown = "Narx tushmoqda"
    override val trendFlat = "Narx barqaror"
    override fun forecastNext(pct: String) = "Keyingi hafta: $pct"
    override fun sellAdvice(wait: Boolean, down: Boolean) =
        if (wait) "Sotish: hozir shoshilmang — keyingi haftada oshishi kutilmoqda"
        else "Sotish: hozir soting" + if (down) " — keyingi haftada tushishi kutilmoqda" else ""
    override fun buyAdvice(wait: Boolean) =
        if (wait) "Olish: biroz kuting — tushishi kutilmoqda" else "Olish: hozir olish uchun qulay payt"
    override val statCurrent = "Hozirgi kurs"
    override val statWeekly = "Haftalik"
    override val statChange = "o'zgarish"
    override val statForecast = "Prognoz"
    override val statSevenDays = "7 kun"
    override fun lastNDays(n: Int) = "So'nggi $n kun (MB kursi)"
    override val zoomHint = "ikki barmoq bilan zoom"
    override fun daysChip(d: Int) = "$d kun"
    override fun yearsChip(y: Int) = "$y yil"
    override val all = "Hammasi"
    override val seasonTitle = "Oy ichida qachon ko'proq oshadi"
    override val seasonStart = "Oy boshi"
    override val seasonMid = "O'rta"
    override val seasonEnd = "Oy oxiri"
    override fun bestRiseSentence(period: String) = "Tarixan kurs eng ko'p $period ko'tariladi."
    override val periodStart = "oy boshida"
    override val periodMid = "oyning o'rtasida"
    override val periodEnd = "oy oxirida"

    override val banks = "Banklar"
    override val searchBank = "Bank qidirish…"
    override val sortByBuy = "Sotib olish bo'yicha"
    override val sortBySell = "Sotish bo'yicha"
    override val colBuy = "Sotib"
    override val colSell = "Sotish"
    override fun spread(n: Int) = "Spred $n so'm"
    override fun spreadShort(n: Int) = "• spred $n"
    override val source = "bank.uz"
    override fun noBankFound(query: String) = "“$query” bo'yicha bank topilmadi."

    override val mapAll = "Hammasi"
    override val mapBranches = "Filiallar"
    override val mapAtms = "Bankomatlar"
    override fun legendBranch(n: Int) = "Filial $n"
    override fun legendAtm(n: Int) = "Bankomat $n"
    override val branchMarker = "Bank filiali"
    override val atmMarker = "Bankomat"
    override val hoursLabel = "Ish vaqti"
    override val bankHours = "Dush–Jum 9:00–18:00, Shan 9:00–13:00"
    override val atmHours = "24/7 — kunu tun"
    override val myLocation = "Mening joylashuvim"
    override val nearMe = "Eng yaqin"
    override val locationNeeded = "Joylashuvni ko'rsatish uchun ruxsat kerak"

    override val footer =
        "Manba: bank.uz (tijorat banklari) va cbu.uz (MB kursi). Marshrut tanlangan xarita ilovasida ochiladi."
}

object RuStrings : AppStrings {
    override val loading = "Загрузка курсов…"
    override val errorTitle = "Не удалось получить данные"
    override val errorHint = "Запустите backend-сервер:  cd backend && npm start"
    override val retry = "Повторить"
    override val noBankRates = "Курсы банков пока недоступны. Попробуйте позже."
    override val connectFail = "Не удалось подключиться к серверу."

    override fun subtitle(currencyName: String) = "Курс $currencyName • Узбекистан"
    override val live = "Онлайн"
    override val offline = "Офлайн"
    override val themeToggle = "Тёмный/светлый режим"

    override val tabHome = "Главная"
    override val tabCalc = "Расчёт"
    override val tabTrend = "Анализ"
    override val tabBanks = "Банки"
    override val tabMap = "Карта"

    override val cbuTitle = "Официальный курс ЦБ"
    override fun oneUnitDate(code: String, date: String) = "1 $code  •  дата: $date"
    override fun updated(time: String) = "Обновлено: $time"

    override val bestBuyTitle = "Лучшая покупка"
    override val bestSellTitle = "Лучшая продажа"
    override fun sellHint(currencyName: String) = "сдать $currencyName дороже"
    override fun buyHint(currencyName: String) = "купить $currencyName дешевле"
    override val som = "сум"

    override val calcTitle = "Умный калькулятор"
    override fun sellMode(currencyName: String) = "Продаю $currencyName"
    override fun buyMode(currencyName: String) = "Покупаю $currencyName"
    override fun amountForeign(code: String) = "Сумма ($code)"
    override val amountUzs = "Сумма (UZS)"
    override val getResult = "Получите"
    override val savings = "Экономия"
    override fun bestBank(bank: String) = "Лучший банк: $bank"

    override val profitTitle = "Рейтинг выгоды"
    override fun profitSell(currencyName: String) =
        "В каком банке выгоднее продать $currencyName (относительно среднего банка)."
    override fun profitBuy(currencyName: String) =
        "В каком банке выгоднее купить $currencyName (относительно среднего банка)."
    override val route = "Маршрут"
    override fun showMore(n: Int) = "Ещё $n банков"
    override val showLess = "Свернуть"
    override val worstLabel = "Худший"

    override fun analyticsTitle(currencyName: String) = "Аналитика курса $currencyName"
    override val analyticsLoading = "Идёт расчёт аналитики…"
    override val analyticsUnavailable = "Аналитика пока недоступна. Попробуйте позже."
    override val analyticsNoHistory = "Недостаточно истории для аналитики."
    override val trendUp = "Курс растёт"
    override val trendDown = "Курс падает"
    override val trendFlat = "Курс стабилен"
    override fun forecastNext(pct: String) = "Следующая неделя: $pct"
    override fun sellAdvice(wait: Boolean, down: Boolean) =
        if (wait) "Продажа: не спешите — на следующей неделе ожидается рост"
        else "Продажа: продавайте сейчас" + if (down) " — на следующей неделе ожидается снижение" else ""
    override fun buyAdvice(wait: Boolean) =
        if (wait) "Покупка: немного подождите — ожидается снижение" else "Покупка: сейчас удобный момент"
    override val statCurrent = "Текущий курс"
    override val statWeekly = "За неделю"
    override val statChange = "изменение"
    override val statForecast = "Прогноз"
    override val statSevenDays = "7 дней"
    override fun lastNDays(n: Int) = "Последние $n дней (курс ЦБ)"
    override val zoomHint = "зум двумя пальцами"
    override fun daysChip(d: Int) = "$d дн"
    override fun yearsChip(y: Int) = "$y г"
    override val all = "Все"
    override val seasonTitle = "Когда в месяце растёт сильнее"
    override val seasonStart = "Начало"
    override val seasonMid = "Середина"
    override val seasonEnd = "Конец"
    override fun bestRiseSentence(period: String) = "Исторически курс сильнее всего растёт $period."
    override val periodStart = "в начале месяца"
    override val periodMid = "в середине месяца"
    override val periodEnd = "в конце месяца"

    override val banks = "Банки"
    override val searchBank = "Поиск банка…"
    override val sortByBuy = "По покупке"
    override val sortBySell = "По продаже"
    override val colBuy = "Покупка"
    override val colSell = "Продажа"
    override fun spread(n: Int) = "Спред $n сум"
    override fun spreadShort(n: Int) = "• спред $n"
    override val source = "bank.uz"
    override fun noBankFound(query: String) = "По запросу «$query» банк не найден."

    override val mapAll = "Все"
    override val mapBranches = "Филиалы"
    override val mapAtms = "Банкоматы"
    override fun legendBranch(n: Int) = "Филиал $n"
    override fun legendAtm(n: Int) = "Банкомат $n"
    override val branchMarker = "Отделение банка"
    override val atmMarker = "Банкомат"
    override val hoursLabel = "Время работы"
    override val bankHours = "Пн–Пт 9:00–18:00, Сб 9:00–13:00"
    override val atmHours = "24/7 — круглосуточно"
    override val myLocation = "Моё местоположение"
    override val nearMe = "Ближайший"
    override val locationNeeded = "Для показа местоположения нужно разрешение"

    override val footer =
        "Источник: bank.uz (комм. банки) и cbu.uz (курс ЦБ). Маршрут открывается в выбранном картографическом приложении."
}

fun stringsFor(lang: Lang): AppStrings = if (lang == Lang.RU) RuStrings else UzStrings

val LocalStrings = staticCompositionLocalOf<AppStrings> { UzStrings }

/** Inline UZ/RU pick for strings owned by a single screen file (keeps the big [AppStrings] interface untouched). */
@androidx.compose.runtime.Composable
fun tr(uz: String, ru: String): String = if (LocalStrings.current === RuStrings) ru else uz
