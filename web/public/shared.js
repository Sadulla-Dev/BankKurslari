// Shared by the browser app (app.js) and the page generator (../build.mjs): interface texts and bank-name helpers.
// A classic script on purpose: everything here is a global, loaded before app.js. Keep it free of DOM access.
"use strict";

/* ---------- i18n ---------- */

function ruPlural(n, one, few, many) {
  const a = Math.abs(n) % 100, b = a % 10;
  if (a > 10 && a < 20) return many;
  if (b === 1) return one;
  if (b >= 2 && b <= 4) return few;
  return many;
}

// So'm. Below this, the best bank is not worth a trip past a nearer one: about a taxi there and back in Tashkent.
// The verdict texts name it, so the assumption is never hidden.
const WORTH = 30000;

const T = {
  uz: {
    "tab.rates": "Kurslar", "tab.map": "Xarita", "tab.history": "Tarix", "tab.wallet": "Hamyon",
    "mode.sell": "Sotaman", "mode.buy": "Sotib olaman", "som": "so‘m", "go": "Yo‘l",
    "col.bank": "Bank", "col.buy": "Sotsangiz", "col.sell": "Olsangiz", "col.km": "Masofa",
    "promo.title": "Ilovada va Telegram kanalda", "promo.sub": "Android ilova: yaqin filial xaritada, marshrut va kurs ogohlantirishi. Telegram kanalda har kuni soat 8:00 da bugungi eng yaxshi kurslar.", "promo.play": "Android ilova", "promo.channel": "Telegram kanal",
    "app.title": "Qulay Kurs ilovasi", "app.sub": "Xarita, marshrut va kurs ogohlantirishi", "app.install": "O‘rnatish", "app.close": "Yopish",
    "store.soon": "Tez kunda", "hist.cbu": "Markaziy bank kursi",
    "spread.buy": "Sotsangiz — bugungi eng yaxshi kurs", "spread.sell": "Olsangiz — bugungi eng arzon kurs",
    "spread.note": "",
    "foot.updated": "Har soatda yangilanadi", "foot.tg": "Telegram kanalimiz", "store.tgNow": "Har kuni kurs", "theme.dark": "Qorong‘i rejim", "theme.light": "Yorug‘ rejim",
    "all.btn": "Boshqa valyutalar", "all.title": "Barcha valyutalar", "all.search": "Valyuta qidirish", "all.favs": "Sevimlilar", "all.all": "Barchasi", "all.banks": "Banklarda almashtiriladi", "all.cbuOnly": "Faqat Markaziy bank kursi — banklarda almashtirilmaydi", "all.empty": "Hech narsa topilmadi", "all.som": "so‘m", "all.swap": "Yo‘nalishni almashtirish", "all.fav": "Sevimlilarga",
    "ab.sell": "Sotsangiz", "ab.buy": "Olsangiz", "ab.best": "eng yaxshi", "ab.none": "Banklar bu valyuta kursini e’lon qilmagan — faqat Markaziy bank kursi.",
    abTitle: (n) => `Banklarda (${n})`, abHint: (sell) => (sell ? "Bank sizdan oladi — eng qimmati tepada" : "Bank sizga sotadi — eng arzoni tepada"), abBadge: (n) => `${n} ta bankda`,
    allSub: (n) => `Funt, frank, yena, tenge, yuan banklarda · ${n} ta MB kursi`, allNote: (d) => `Markaziy bank kursi · ${d} · banklarda naqd kurs boshqacha bo‘lishi mumkin`,
    lang: "Til", open: "Ochish", rank: (n) => `${n}-o‘rin`,
    vdSmall: () => "Qaysi bankka borsangiz ham farq kichik",
    vdBig: () => "Bankni tanlash foydali: farq katta",
    vdLine: (sell, n, p, a, u, gap, th) => `${a} ${u} ${sell ? "sotsangiz" : "olsangiz"}, eng yaxshi bank bilan eng yomoni orasidagi farq ${gap} so‘m. Taksida borib-kelish ~${th} so‘m turadi.`,
    vdNear: (sell, b, km, d) => `Sizga eng yaqini — ${b}, ${km} km: ${d ? `${d} so‘m ${sell ? "kam" : "qimmat"}` : "eng yaxshi kurs"}.`,
    vdOne: "Bugun faqat bitta bank kurs e’lon qilgan.",
    "sh.buys": "Sotsangiz", "sh.sells": "Olsangiz", "sh.near": "Eng yaqin filiallar", "sh.hours": "Ish vaqti",
    hrs: { days: ["Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya"], off: "Dam olish kuni", always: "Har kuni, 24 soat", open: (e) => `Hozir ochiq · ${e} gacha`, closed: "Hozir yopiq", opens: (d, t) => `Hozir yopiq · ${d} ${t} da ochiladi`, today: "bugun", tomorrow: "ertaga", usual: "Bu bank filiallarining odatiy ish vaqti", unknown: "Ish vaqti nomaʼlum", phone: "Telefon", wk: (s) => `Shu dam olish kunlari, bank ro‘yxatiga ko‘ra: ${s}`, wkOpen: "ochiq", wkClosed: "yopiq" }, "sh.showMap": "Xaritada", "sh.go": "Yo‘nalish",
    title: { USD: "Banklarda dollar kursi", EUR: "Banklarda yevro kursi", RUB: "Banklarda rubl kursi" },
    tblSub: (n, t, s) => `${n} bank · ${t} holatiga · ${s}`,
    sortBy: { best: "eng foydalisi tepada", buy: "xarid kursi bo‘yicha", sell: "sotuv kursi bo‘yicha", name: "nomi bo‘yicha", km: "yaqinlik bo‘yicha" },
    getSell: "Siz olasiz", getBuy: "Siz to‘laysiz",
    search: "Bank qidirish", near: "Yaqinini ko‘rsatish", nearOn: "Yaqinlari tepada",
    status: (t, n) => `${t} holatiga · ${n} bank`,
    unit: { USD: () => "dollar", EUR: () => "yevro", RUB: () => "rubl" },
    perUnit: (r, u, n, k = 1) => `1 ${u} uchun ${r} so‘m — ${n} ta bank ichida eng yaxshisi` + (k > 1 ? `, yana ${k - 1} ta bankda ham shunday` : ""),
    perUnitBuy: (r, u, n, k = 1) => `1 ${u} uchun ${r} so‘m — ${n} ta bank ichida eng arzoni` + (k > 1 ? `, yana ${k - 1} ta bankda ham shunday` : ""),
    less: (v) => `${v} kam`, more: (v) => `${v} qimmat`, best: "eng yaxshi",
    km: (v) => `${v} km`, away: (v) => `${v} km uzoqlikda`,
    forAmt: (a, u) => `${a} ${u} uchun`,
    cmpLess: (v, b) => `${b}dan ${v} so‘m kam`, cmpMore: (v, b) => `${b}dan ${v} so‘m qimmat`,
    further: (k) => `, ${k} km uzoqroq`, isBest: "Hozir eng qulay kurs shu bankda",
    today: "bugun", noChange: "o‘zgarmagan",
    tel: (p) => `Tel: ${p}`, call: (p) => `Qo‘ng‘iroq: ${p}`,
    "fb.title": "Fikr yuborish", "rep.link": "Xato haqida xabar berish",
    rep: { title: "Xato haqida xabar", fbTitle: "Fikr-taklif", fbSub: "Sayt yoki ilova haqida fikringiz yoki taklifingizni yozing.",
      kinds: { no_place: "Bu joyda filial yo‘q", wrong_address: "Manzil noto‘g‘ri", wrong_phone: "Telefon noto‘g‘ri", wrong_hours: "Ish vaqti noto‘g‘ri", wrong_rate: "Kurs noto‘g‘ri", other: "Boshqa" },
      msg: "Xabaringiz…", comment: "Izoh (ixtiyoriy)", send: "Yuborish", sending: "Yuborilmoqda…",
      fail: "Yuborilmadi. Internetni tekshirib, qayta urinib ko‘ring.", thanks: "Rahmat! Xabaringiz yuborildi." },
    askLoc: "Masofani ko‘rsatish", noBranches: "Filiallar ro‘yxati topilmadi", noMatch: "Bunday bank topilmadi",
    mapBest: (a, u) => `${a} ${u} uchun eng yaxshi`, branchesHere: (n) => `Bu yerda ${n} ta filial`,
    allBanks: "Barcha banklar", wkChip: "Dam olish kuni ochiq",
    wkCard: { title: "Dam olish kunlari ochiq banklar", sub: (r) => `Shanba va yakshanba, ${r} · banklarning o‘z ro‘yxatlari`, usual: "Filiallarning odatiy ish vaqti bo‘yicha", count: (n) => `${n} ta filial ochiq`, all: "barcha filiallari", map: "Xaritada ko‘rsatish", more: (n) => `Barcha ${n} bank`, less: "Kamroq ko‘rsatish", page: "Har bir bankning ro‘yxati" },
    sideHint: { sell: "Sotsangiz: kim ko‘proq beradi. Eng yaxshisi tepada.", buy: "Olsangiz: kim arzonroq sotadi. Eng arzoni tepada." },
    locFail: "Joylashuvni aniqlab bo‘lmadi", copied: "Havola nusxalandi",
    share: (b, r, u) => `${b}: 1 ${u} = ${r} so‘m — qulaykurs.uz`,
    src: "Manba", since: (y) => `${y}-yildan`,
    ranges: ["1H", "1O", "3O", "1Y", "5Y", "Hammasi"], periods: ["7 kun", "30 kun", "3 oy", "1 yil", "5 yil"],
    periodsIn: ["7 kunda", "30 kunda", "3 oyda", "1 yilda", "5 yilda"], sinceIn: (y) => `${y}-yildan beri`,
    moved: (up, v, p, when) => (up == null ? `${when} o‘zgarmadi` : `${when} ${v} so‘m ${up ? "qimmatladi" : "arzonladi"} (${p})`),
    stHigh: "Eng yuqori", stLow: "Eng past", stAvg: "O‘rtacha", stChg: "O‘zgarish", avg: "o‘rtacha", prevPt: "oldingi nuqtaga",
    toToday: "bugungacha", cbu: "Markaziy bank", tkBuy: "xarid", tkSell: "sotuv",
    error: "Kurslarni yuklab bo‘lmadi. Internetni tekshirib, sahifani yangilang.",
    "gold.title": "Oltin quymalar narxi", "gold.chart": "Grafik", "gold.bar": "Quyma", "gold.sell": "Sotib olish", "gold.buyback": "Qaytarib topshirish",
    "gold.perGram": "1 gramm", "gold.loss": "Farq", "gold.tab": "Oltin",
    goldLoss: (g, v, p) => `Bugun ${g} g quyma olib, darhol qaytarib topshirsangiz — ${v} so‘m yo‘qotasiz (${p}%).`,
    "gold.note": "Narxlar — Markaziy bank o‘lchovli oltin quymalari uchun. “Sotib olish” — bankdan quyma olish narxi, “Qaytarib topshirish” — quymani bankka qaytarganda olinadigan pul (qadoq buzilmagan bo‘lsa).",
    goldSub: (d, tm) => `Markaziy bank · ${d}, ${tm} holatiga`, grams: (g) => `${g} g`,
    goldLabel: "Oltin, 1 gramm (10 g quyma)", goldSellL: "1 g — sotib olish narxi", goldBuyL: "1 g — qaytarib topshirish", goldSrc: "Markaziy bank · 10 g quyma",
    goldSideNote: "Sotib olish — Markaziy bank quymani sizga sotadigan narx. Qaytarib topshirish — bank shikastlanmagan quymani sizdan qabul qiladigan narx."
  },
  ru: {
    "tab.rates": "Курсы", "tab.map": "Карта", "tab.history": "История", "tab.wallet": "Кошелёк",
    "mode.sell": "Продаю", "mode.buy": "Покупаю", "som": "сум", "go": "Маршрут",
    "col.bank": "Банк", "col.buy": "Продать", "col.sell": "Купить", "col.km": "Расстояние",
    "promo.title": "В приложении и в Telegram-канале", "promo.sub": "Android-приложение: ближайшие отделения на карте, маршрут и уведомления о курсе. В Telegram-канале каждый день в 8:00 — лучшие курсы на сегодня.", "promo.play": "Android-приложение", "promo.channel": "Telegram-канал",
    "app.title": "Приложение Qulay Kurs", "app.sub": "Карта, маршрут и уведомления о курсе", "app.install": "Установить", "app.close": "Закрыть",
    "store.soon": "Скоро", "hist.cbu": "Курс Центрального банка",
    "spread.buy": "Продать — лучший курс сегодня", "spread.sell": "Купить — самый дешёвый сегодня",
    "spread.note": "",
    "foot.updated": "Обновляется каждый час", "foot.tg": "Наш Telegram-канал", "store.tgNow": "Курс каждый день", "theme.dark": "Тёмная тема", "theme.light": "Светлая тема",
    "all.btn": "Другие валюты", "all.title": "Все валюты", "all.search": "Поиск валюты", "all.favs": "Избранные", "all.all": "Все", "all.banks": "Меняют в банках", "all.cbuOnly": "Только курс ЦБ — банки не меняют", "all.empty": "Ничего не найдено", "all.som": "сум", "all.swap": "Поменять направление", "all.fav": "В избранное",
    "ab.sell": "Продать", "ab.buy": "Купить", "ab.best": "лучший", "ab.none": "Банки не публикуют курс этой валюты — только курс ЦБ.",
    abTitle: (n) => `В банках (${n})`, abHint: (sell) => (sell ? "Банк покупает у вас — самый дорогой сверху" : "Банк продаёт вам — самый дешёвый сверху"), abBadge: (n) => `в ${n} банках`,
    allSub: (n) => `Фунт, франк, иена, тенге, юань в банках · ${n} курсов ЦБ`, allNote: (d) => `Курс ЦБ · ${d} · наличный курс в банках может отличаться`,
    lang: "Язык", open: "Показать", rank: (n) => `${n}-е место`,
    vdSmall: () => "В какой банк ни пойдёте, разница небольшая",
    vdBig: () => "Банк стоит выбрать: разница большая",
    vdLine: (sell, n, p, a, u, gap, th) => `${sell ? "Продавая" : "Покупая"} ${a} ${u}, между лучшим и худшим банком разница ${gap} сум. Такси туда и обратно стоит ~${th} сум.`,
    vdNear: (sell, b, km, d) => `Ближайший к вам — ${b}, ${km} км: ${d ? `на ${d} сум ${sell ? "меньше" : "дороже"}` : "лучший курс"}.`,
    vdOne: "Сегодня курс опубликовал только один банк.",
    "sh.buys": "Продать", "sh.sells": "Купить", "sh.near": "Ближайшие филиалы", "sh.hours": "Часы работы",
    hrs: { days: ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"], off: "Выходной", always: "Круглосуточно", open: (e) => `Сейчас открыто · до ${e}`, closed: "Сейчас закрыто", opens: (d, t) => `Сейчас закрыто · откроется ${d} в ${t}`, today: "сегодня", tomorrow: "завтра", usual: "Обычные часы работы филиалов банка", unknown: "Часы работы неизвестны", phone: "Телефон", wk: (s) => `В эти выходные, по списку банка: ${s}`, wkOpen: "открыто", wkClosed: "закрыто" }, "sh.showMap": "На карте", "sh.go": "Маршрут",
    title: { USD: "Курс доллара в банках", EUR: "Курс евро в банках", RUB: "Курс рубля в банках" },
    tblSub: (n, t, s) => `${n} банков · на ${t} · ${s}`,
    sortBy: { best: "выгодные сверху", buy: "по покупке", sell: "по продаже", name: "по названию", km: "ближайшие сверху" },
    getSell: "Вы получите", getBuy: "Вы заплатите",
    search: "Поиск банка", near: "Показать ближайшие", nearOn: "Ближайшие сверху",
    status: (t, n) => `на ${t} · ${n} банков`,
    unit: { USD: (n) => ruPlural(n, "доллар", "доллара", "долларов"), EUR: () => "евро", RUB: (n) => ruPlural(n, "рубль", "рубля", "рублей") },
    perUnit: (r, u, n, k = 1) => `${r} сум за 1 ${u} — лучший из ${n} банков` + (k > 1 ? `, такой же курс ещё в ${k - 1} ${ruPlural(k - 1, "банке", "банках", "банках")}` : ""),
    perUnitBuy: (r, u, n, k = 1) => `${r} сум за 1 ${u} — дешевле всех из ${n} банков` + (k > 1 ? `, такой же курс ещё в ${k - 1} ${ruPlural(k - 1, "банке", "банках", "банках")}` : ""),
    less: (v) => `на ${v} меньше`, more: (v) => `на ${v} дороже`, best: "лучший",
    km: (v) => `${v} км`, away: (v) => `${v} км от вас`,
    forAmt: (a, u) => `за ${a} ${u}`,
    cmpLess: (v, b) => `На ${v} сум меньше, чем в ${b}`, cmpMore: (v, b) => `На ${v} сум дороже, чем в ${b}`,
    further: (k) => `, на ${k} км дальше`, isBest: "Сейчас здесь самый выгодный курс",
    today: "сегодня", noChange: "без изменений",
    tel: (p) => `Тел.: ${p}`, call: (p) => `Позвонить: ${p}`,
    "fb.title": "Обратная связь", "rep.link": "Сообщить об ошибке",
    rep: { title: "Сообщить об ошибке", fbTitle: "Отзыв и предложения", fbSub: "Напишите отзыв или предложение о сайте или приложении.",
      kinds: { no_place: "Здесь нет филиала", wrong_address: "Неверный адрес", wrong_phone: "Неверный телефон", wrong_hours: "Неверные часы работы", wrong_rate: "Неверный курс", other: "Другое" },
      msg: "Ваше сообщение…", comment: "Комментарий (необязательно)", send: "Отправить", sending: "Отправка…",
      fail: "Не отправлено. Проверьте интернет и попробуйте снова.", thanks: "Спасибо! Сообщение отправлено." },
    askLoc: "Показать расстояние", noBranches: "Список филиалов не найден", noMatch: "Банк не найден",
    mapBest: (a, u) => `Лучший курс за ${a} ${u}`, branchesHere: (n) => `Здесь ${n} ${ruPlural(n, "филиал", "филиала", "филиалов")}`,
    allBanks: "Все банки", wkChip: "Открыто в выходные",
    wkCard: { title: "Банки, открытые в выходные", sub: (r) => `Суббота и воскресенье, ${r} · списки самих банков`, usual: "По обычному графику отделений", count: (n) => `открыто отделений: ${n}`, all: "все отделения", map: "Показать на карте", more: (n) => `Все ${n} банков`, less: "Свернуть", page: "Список каждого банка" },
    sideHint: { sell: "Продаёте: кто даст больше. Лучший сверху.", buy: "Покупаете: кто продаст дешевле. Самый дешёвый сверху." },
    locFail: "Не удалось определить местоположение", copied: "Ссылка скопирована",
    share: (b, r, u) => `${b}: 1 ${u} = ${r} сум — qulaykurs.uz`,
    src: "Источник", since: (y) => `с ${y} г.`,
    ranges: ["1Н", "1М", "3М", "1Г", "5Л", "Всё"], periods: ["7 дней", "30 дней", "3 месяца", "1 год", "5 лет"],
    periodsIn: ["За 7 дней", "За 30 дней", "За 3 месяца", "За год", "За 5 лет"], sinceIn: (y) => `С ${y} г.`,
    moved: (up, v, p, when) => (up == null ? `${when} без изменений` : `${when} ${up ? "подорожал" : "подешевел"} на ${v} сум (${p})`),
    stHigh: "Максимум", stLow: "Минимум", stAvg: "Среднее", stChg: "Изменение", avg: "среднее", prevPt: "к предыдущей точке",
    toToday: "до сегодня", cbu: "ЦБ", tkBuy: "покупка", tkSell: "продажа",
    error: "Не удалось загрузить курсы. Проверьте интернет и обновите страницу.",
    "gold.title": "Цены на золотые слитки", "gold.chart": "График", "gold.bar": "Слиток", "gold.sell": "Покупка", "gold.buyback": "Обратный выкуп",
    "gold.perGram": "1 грамм", "gold.loss": "Разница", "gold.tab": "Золото",
    goldLoss: (g, v, p) => `Купите слиток ${g} г сегодня и сразу сдадите обратно — потеряете ${v} сум (${p}%).`,
    "gold.note": "Цены на мерные золотые слитки Центрального банка. «Покупка» — за сколько вы покупаете слиток, «Обратный выкуп» — сколько банк платит за него (в целой упаковке).",
    goldSub: (d, tm) => `Центральный банк · на ${d}, ${tm}`, grams: (g) => `${g} г`,
    goldLabel: "Золото, 1 грамм (слиток 10 г)", goldSellL: "1 г — цена покупки", goldBuyL: "1 г — цена обратного выкупа", goldSrc: "Центральный банк · слиток 10 г",
    goldSideNote: "Продажа — цена, по которой ЦБ продаёт вам слиток. Выкуп — цена, по которой ЦБ принимает неповреждённый слиток."
  },
  en: {
    "tab.rates": "Rates", "tab.map": "Map", "tab.history": "History", "tab.wallet": "Wallet",
    "mode.sell": "I sell", "mode.buy": "I buy", "som": "so‘m", "go": "Go",
    "col.bank": "Bank", "col.buy": "You sell", "col.sell": "You buy", "col.km": "Distance",
    "promo.title": "In the app and on Telegram", "promo.sub": "Android app: nearby branches on a map, directions and rate alerts. Our Telegram channel posts today’s best rates every day at 8:00.", "promo.play": "Android app", "promo.channel": "Telegram channel",
    "app.title": "Qulay Kurs app", "app.sub": "Map, directions and rate alerts", "app.install": "Install", "app.close": "Close",
    "store.soon": "Coming soon", "hist.cbu": "Central Bank rate",
    "spread.buy": "To sell — best rate today", "spread.sell": "To buy — cheapest today",
    "spread.note": "",
    "foot.updated": "Updated every hour", "foot.tg": "Our Telegram channel", "store.tgNow": "Daily rates", "theme.dark": "Dark mode", "theme.light": "Light mode",
    "all.btn": "Other currencies", "all.title": "All currencies", "all.search": "Search currency", "all.favs": "Favourites", "all.all": "All", "all.banks": "Exchanged at banks", "all.cbuOnly": "Central Bank rate only — banks don’t exchange these", "all.empty": "Nothing found", "all.som": "som", "all.swap": "Swap direction", "all.fav": "Favourite",
    "ab.sell": "Sell", "ab.buy": "Buy", "ab.best": "best", "ab.none": "Banks don’t publish a rate for this currency — Central Bank rate only.",
    abTitle: (n) => `At banks (${n})`, abHint: (sell) => (sell ? "The bank buys from you — highest first" : "The bank sells to you — cheapest first"), abBadge: (n) => `at ${n} banks`,
    allSub: (n) => `Pound, franc, yen, tenge, yuan at banks · ${n} CBU rates`, allNote: (d) => `Central Bank rate · ${d} · bank cash rates may differ`,
    lang: "Language", open: "Show", rank: (n) => `#${n}`,
    vdSmall: () => "Any bank will do: the gap is small",
    vdBig: () => "Pick the bank: the gap is big",
    vdLine: (sell, n, p, a, u, gap, th) => `${sell ? "Selling" : "Buying"} ${a} ${u}, the best and the worst bank differ by ${gap} so‘m. A taxi there and back costs about ${th} so‘m.`,
    vdNear: (sell, b, km, d) => `Nearest to you — ${b}, ${km} km: ${d ? `${d} so‘m ${sell ? "less" : "more"}` : "the best rate"}.`,
    vdOne: "Only one bank has published a rate today.",
    "sh.buys": "You sell", "sh.sells": "You buy", "sh.near": "Nearest branches", "sh.hours": "Opening hours",
    hrs: { days: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"], off: "Closed", always: "Open 24/7", open: (e) => `Open now · until ${e}`, closed: "Closed now", opens: (d, t) => `Closed now · opens ${d} at ${t}`, today: "today", tomorrow: "tomorrow", usual: "Usual hours of this bank's branches", unknown: "Opening hours unknown", phone: "Phone", wk: (s) => `This weekend, by the bank's list: ${s}`, wkOpen: "open", wkClosed: "closed" }, "sh.showMap": "Show on map", "sh.go": "Directions",
    title: { USD: "Dollar rates at banks", EUR: "Euro rates at banks", RUB: "Ruble rates at banks" },
    tblSub: (n, t, s) => `${n} banks · as of ${t} · ${s}`,
    sortBy: { best: "best first", buy: "by buy rate", sell: "by sell rate", name: "by name", km: "nearest first" },
    getSell: "You get", getBuy: "You pay",
    search: "Search banks", near: "Show nearest", nearOn: "Nearest first",
    status: (t, n) => `as of ${t} · ${n} banks`,
    unit: { USD: (n) => (n === 1 ? "dollar" : "dollars"), EUR: (n) => (n === 1 ? "euro" : "euros"), RUB: (n) => (n === 1 ? "ruble" : "rubles") },
    perUnit: (r, u, n, k = 1) => `${r} so‘m per ${u} — the best of ${n} banks` + (k > 1 ? `, matched by ${k - 1} more` : ""),
    perUnitBuy: (r, u, n, k = 1) => `${r} so‘m per ${u} — the cheapest of ${n} banks` + (k > 1 ? `, matched by ${k - 1} more` : ""),
    less: (v) => `${v} less`, more: (v) => `${v} more`, best: "best",
    km: (v) => `${v} km`, away: (v) => `${v} km away`,
    forAmt: (a, u) => `for ${a} ${u}`,
    cmpLess: (v, b) => `${v} so‘m less than ${b}`, cmpMore: (v, b) => `${v} so‘m more than ${b}`,
    further: (k) => `, ${k} km further`, isBest: "The best rate right now",
    today: "today", noChange: "no change",
    tel: (p) => `Tel: ${p}`, call: (p) => `Call ${p}`,
    "fb.title": "Send feedback", "rep.link": "Report a problem",
    rep: { title: "Report a problem", fbTitle: "Feedback", fbSub: "Tell us what you think about the site or the app.",
      kinds: { no_place: "No branch here", wrong_address: "Wrong address", wrong_phone: "Wrong phone", wrong_hours: "Wrong opening hours", wrong_rate: "Wrong rate", other: "Other" },
      msg: "Your message…", comment: "Comment (optional)", send: "Send", sending: "Sending…",
      fail: "Not sent. Check your connection and try again.", thanks: "Thank you! Your message was sent." },
    askLoc: "Show distance", noBranches: "No branch list found", noMatch: "No bank matches",
    mapBest: (a, u) => `Best for ${a} ${u}`, branchesHere: (n) => `${n} branches here`,
    allBanks: "All banks", wkChip: "Open on the weekend",
    wkCard: { title: "Banks open this weekend", sub: (r) => `Saturday and Sunday, ${r} · the banks' own lists`, usual: "By the branches' usual hours", count: (n) => `${n} open`, all: "all branches", map: "Show on the map", more: (n) => `All ${n} banks`, less: "Show fewer", page: "Each bank's list" },
    sideHint: { sell: "Selling: who pays the most. Best first.", buy: "Buying: who charges the least. Cheapest first." },
    locFail: "Couldn’t get your location", copied: "Link copied",
    share: (b, r, u) => `${b}: 1 ${u} = ${r} so‘m — qulaykurs.uz`,
    src: "Source", since: (y) => `since ${y}`,
    ranges: ["1W", "1M", "3M", "1Y", "5Y", "All"], periods: ["7 days", "30 days", "3 months", "1 year", "5 years"],
    periodsIn: ["in 7 days", "in 30 days", "in 3 months", "in a year", "in 5 years"], sinceIn: (y) => `since ${y}`,
    moved: (up, v, p, when) => (up == null ? `Unchanged ${when}` : `${up ? "Up" : "Down"} ${v} so‘m (${p}) ${when}`),
    stHigh: "High", stLow: "Low", stAvg: "Average", stChg: "Change", avg: "avg", prevPt: "vs previous point",
    toToday: "to today", cbu: "CBU", tkBuy: "buy", tkSell: "sell",
    error: "Couldn’t load rates. Check your connection and reload.",
    "gold.title": "Gold bar prices", "gold.chart": "Chart", "gold.bar": "Bar", "gold.sell": "You buy at", "gold.buyback": "Buy-back",
    "gold.perGram": "Per gram", "gold.loss": "Spread", "gold.tab": "Gold",
    goldLoss: (g, v, p) => `Buy a ${g} g bar today and hand it straight back, and you lose ${v} so‘m (${p}%).`,
    "gold.note": "Prices for the Central Bank of Uzbekistan’s gold bars. “You buy at” is what you pay for a bar, “Buy-back” is what the bank pays you for it (packaging intact).",
    goldSub: (d, tm) => `Central Bank · as of ${d}, ${tm}`, grams: (g) => `${g} g`,
    goldLabel: "Gold, per gram (10 g bar)", goldSellL: "1 g — buy price", goldBuyL: "1 g — buy-back price", goldSrc: "Central Bank · 10 g bar",
    goldSideNote: "Sell is what the CBU charges you for a bar. Buy-back is what it pays for an undamaged bar."
  }
};

/* ---------- Uzbek Cyrillic ---------- */

// Uzbek Latin -> Uzbek Cyrillic by the rules of the mobile app (composeApp/…/Translit.kt), so the Uzbek texts exist
// once. Left as they are: the site's name, @handles, 3-letter currency codes (USD), tokens with a dot, slash or digit
// inside (cbu.uz, 12:31), and everything between the two KEEP marks (bank names and other data put into a text).
const KEEP = ["\u0001", "\u0002"];
const APOS = "‘’ʻʼ'`";
const UZC_LETTERS = { a: "а", b: "б", c: "с", d: "д", e: "е", f: "ф", g: "г", h: "ҳ", i: "и", j: "ж", k: "к", l: "л", m: "м", n: "н", o: "о", p: "п", q: "қ", r: "р", s: "с", t: "т", u: "у", v: "в", w: "в", x: "х", y: "й", z: "з" };
const UZC_PAIRS = { sh: "ш", ch: "ч", ya: "я", yu: "ю", ye: "е" };
const UZC_WORDS = { rubl: "рубль", kalkulyator: "калькулятор" }; // written with ь, which the Latin spelling does not show

function uzCyrillicWord(w) {
  if (/[.\/@0-9]/.test(w) || /^[A-Z]{3}$/.test(w)) return w;
  const code = /^([A-Z]{3})([a-z‘’ʻʼ'`]+)$/.exec(w); // a currency code with an ending: USDdan
  if (code) return code[1] + uzCyrillicWord(code[2]);
  const cap = (str, upper) => (upper ? str[0].toUpperCase() + str.slice(1) : str);
  if (UZC_WORDS[w.toLowerCase()]) return cap(UZC_WORDS[w.toLowerCase()], w[0] !== w[0].toLowerCase());
  let out = "";
  for (let i = 0; i < w.length; ) {
    const c = w[i], lc = c.toLowerCase(), next = (w[i + 1] || "").toLowerCase(), upper = c !== lc;
    const emit = (str, len) => { out += cap(str, upper); i += len; };
    if (APOS.includes(c)) { out += "ъ"; i++; } // tutuq belgisi: ma’lumot
    else if ((lc === "o" || lc === "g") && next && APOS.includes(next)) emit(lc === "o" ? "ў" : "ғ", 2);
    else if (UZC_PAIRS[lc + next]) emit(UZC_PAIRS[lc + next], 2);
    else if (lc === "y" && next === "o" && !APOS.includes(w[i + 2] || " ")) emit("ё", 2); // yoki, but not yo‘l
    else if (lc === "e" && (i === 0 || "aeiou".includes(w[i - 1].toLowerCase()))) emit("э", 1);
    else if (UZC_LETTERS[lc]) emit(UZC_LETTERS[lc], 1);
    else { out += c; i++; }
  }
  return out;
}
function uzCyrillic(text) {
  return String(text).split(/(\u0001[^\u0002]*\u0002|Qulay Kurs|@\w+)/).map((part, i) => (i % 2
    ? part.replace(/[\u0001\u0002]/g, "")
    : part.replace(/[A-Za-z](?:[A-Za-z0-9‘’ʻʼ'`]|[.@\/](?=[A-Za-z0-9]))*/g, uzCyrillicWord))).join("");
}
// What a text function is called with is data, not text: it goes through unchanged. Currency codes stay bare,
// because the texts look words up by them.
const uzcKeep = (v) => (typeof v === "string" ? (v === "" || /^[A-Z]{3}$/.test(v) ? v : KEEP[0] + v + KEEP[1])
  : v && typeof v === "object" ? new Proxy(v, { get: (target, key) => uzcKeep(target[key]) }) : v);
// A whole table of texts (strings, functions that build strings, lists, nested tables) in Cyrillic.
function uzCyrillicTexts(v) {
  if (typeof v === "string") return uzCyrillic(v);
  if (typeof v === "function") return (...args) => uzCyrillicTexts(v(...args.map(uzcKeep)));
  if (Array.isArray(v)) return v.map(uzCyrillicTexts);
  if (v && typeof v === "object") return Object.fromEntries(Object.entries(v).map(([key, x]) => [key, uzCyrillicTexts(x)]));
  return v;
}
T.uzc = uzCyrillicTexts(T.uz);

/* ---------- addresses ---------- */

/**
 * [street + number, the rest]: "100029, Toshkent shahri, Mirobod tumani, A.Temur ko'chasi, 53 uy"
 * -> ["A.Temur ko'chasi 53 uy", "Mirobod tumani"]. Postcodes and the bare city name are dropped.
 */
function addrParts(a) {
  if (!a) return ["", ""];
  // A leading postcode ("100000 Toshkent shahar") and the bare city name in any spelling ("г. Ташкент") say nothing here.
  const parts = a.split(/,\s*/).map((p) => p.replace(/^\d{6}\s+/, "").trim())
    .filter((p) => p && !/^\d{5,6}$/.test(p) && !/^(г\.\s*)?(toshkent|tashkent|ташкент)(\s+(shahri|shahar|sh\.|city|г\.))?$/i.test(p));
  const si = parts.findIndex((p) => /ko.ch|kuch|street|ул\.|улица|prospekt|проспект|shoh/i.test(p));
  if (si < 0) return [parts.slice(0, 2).join(", "), parts.slice(2).join(", ")];
  let head = parts[si];
  const used = new Set([si]);
  const num = [si + 1, si - 1].find((i) => i >= 0 && i < parts.length && /^\d+[\p{L}/-]?\d*(\s*(uy|dom|д\.))?$/iu.test(parts[i]));
  if (num != null) { head += " " + parts[num]; used.add(num); }
  return [head, parts.filter((_, i) => !used.has(i)).join(", ")];
}
const cleanAddr = (a) => addrParts(a).filter(Boolean).join(", ");

/* ---------- bank names ---------- */

const bankKey = (n) => n.toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");
const SHORT = [
  [/milliy/i, "NBU"], [/sanoatqurilish|промстрой/i, "SQB"], [/xalq/i, "Xalq"],
  [/mikrokredit|mkbank/i, "MKBank"], [/orient\s*finans/i, "Orient Finans"],
  [/asia\s*alliance/i, "Asia Alliance"], [/ipak\s*yo/i, "Ipak Yo‘li"], [/ipoteka/i, "Ipoteka"],
  [/turonbank/i, "Turon"], [/hamkor/i, "Hamkor"], [/aloqa/i, "Aloqa"]
];
function shortName(name) {
  for (const [re, s] of SHORT) if (re.test(name)) return s;
  const s = name.replace(/\s*(aksiyadorlik|tijorat|ATB|AT|ATIB|banki|bank)\b\.?/gi, "").trim() || name;
  return s.length > 14 ? s.slice(0, 13).trim() + "…" : s;
}
function initials(name) {
  const s = shortName(name).replace(/[‘’ʻ'`]/g, "");
  const words = s.split(/\s+/).filter(Boolean);
  return (words.length > 1 ? words[0][0] + words[1][0] : s.slice(0, 2)).toUpperCase();
}
// Banks whose weekend hours on their branch pages were checked against their own weekly lists (2026-11-27, see
// backend/src/officialBranches.js for the two that failed). Only these count as "always open on weekends" on the
// weekend page and in the map's weekend filter; a bank that starts publishing weekend hours is left out until someone
// has compared them the same way. Slugs as logoSlug gives them.
const WEEKEND_HOURS_CHECKED = new Set(["anorbank", "asia-alliance", "hamkorbank", "hayot-bank", "ipak-yuli", "tenge-bank", "orient-finans"]);
function logoSlug(name) {
  const key = bankKey(name);
  if (key.includes("anor")) return "anorbank";
  if (key.includes("hayot")) return "hayot-bank";
  if (key.includes("tenge")) return "tenge-bank";
  if (key.includes("infin")) return "infinbank";
  if (key.includes("hamkor")) return "hamkorbank";
  if (key.includes("agro")) return "agrobank";
  if (key.includes("trast") || key.includes("trust")) return "trastbank";
  if (key.includes("universal")) return "universal-bank";
  if (key.includes("poytaxt")) return "poytaxt-bank";
  if (key.includes("ipoteka")) return "ipoteka-bank";
  if (key.includes("asiaalliance")) return "asia-alliance";
  if (key.includes("aloqa")) return "aloqabank";
  if (key.includes("garant")) return "garant-bank";
  if (key.includes("turon")) return "turonbank";
  if (key.includes("orientfinans") || key.includes("orientfinance")) return "orient-finans";
  if (key.includes("sanoatqurilish") || key === "sqb") return "sqb";
  if (key.includes("mkbank") || key.includes("mikrokredit")) return "mkbank";
  if (key.includes("xalq")) return "xalq-bank";
  if (key.includes("ipakyuli")) return "ipak-yuli";
  if (key === "brb" || key.startsWith("biznesnirivojlantirish")) return "brb"; // the Central Bank's lists spell it out
  if (key.includes("asaka")) return "asakabank";
  if (key.includes("davr")) return "davrbank";
  if (key.includes("apex")) return "apexbank";
  if (key.includes("milliy") || key === "nbu") return "nbu";
  if (key.includes("openbank")) return "openbank";
  if (key.includes("ziraat")) return "ziraat-bank";
  if (key.includes("octo")) return "octobank";
  if (key.startsWith("kdb")) return "kdb-bank";
  if (key.includes("kapital")) return "kapitalbank";
  return null;
}
