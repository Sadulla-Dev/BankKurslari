// Pure helpers of the Telegram bot: reading what people type, ranking banks, and the texts.
// No Worker or network APIs here, so `node test.mjs` can check all of it.

export const CURRENCIES = ["USD", "EUR", "RUB"];
export const SITE = "https://qulaykurs.uz";
export const BOT = "qulaykursbot";
export const DEFAULT_AMOUNT = { USD: 100, EUR: 100, RUB: 10000 };
export const MAX_ALERTS = 5;
const TZ_OFFSET_MS = 5 * 60 * 60 * 1000; // Tashkent, UTC+5, no daylight saving

/* ---------- reading the user's text ---------- */

const CUR_WORDS = [
  ["USD", /\$|\busd\b|dollar|dollor|доллар|долар|бакс|(?<!\p{L})долл/u],
  ["EUR", /€|\beur\b|yevro|evro|euro|евро/],
  ["RUB", /₽|\brub\b|rubl|рубл|(?<!\p{L})руб(?!\p{L})/u]
];
// Cyrillic needs lookarounds: \b only knows Latin letters. "сумма" (an amount) is not "сум".
const SOM = /so['‘’ʻ`]?m\b|\bsum\b|(?<!\p{L})(сум|сўм)(?!ма|му|мы|ме)|\buzs\b/u;
const BUY = /sotib\s*ol|\bolaman|\bolmoqchi|\bolsam|\bolish|xarid|\bbuy\b|куп|покуп/; // the user buys currency
const SELL = /\bsot|\bsell\b|прода|сдать|сдам|обмен/; // the user sells currency (checked after BUY: "sotib olaman")
const UP = /oshsa|ko['‘’ʻ`]?tarilsa|yuqori|ortsa|qimmatlasa|>|выше|больше|подним|выраст/;
const DOWN = /tushsa|pasaysa|arzonlasa|kamaysa|<|ниже|меньше|упад|опуст|снизит/;

const normalize = (text) =>
  String(text || "")
    .toLowerCase()
    .replace(/[‘’ʻ`]/g, "'")
    .replace(/ /g, " ");

export function currencyIn(t) {
  for (const [cur, re] of CUR_WORDS) if (re.test(t)) return cur;
  return null;
}

/** The first number in the text: "1 000", "1,000", "1.5 ming", "2 mln", "500$". 0 when there is none. */
export function numberIn(t) {
  const m = t.match(/(\d[\d\s]*(?:[.,]\d+)?)\s*(ming|минг|тыс|k\b|mln|million|миллион|млн)?/);
  if (!m) return 0;
  let raw = m[1].trim();
  // "1 000 000" and "1,000" are thousands; "1,5" and "1.5" are decimals.
  raw = raw.replace(/(\d)\s+(?=\d{3}(\D|$))/g, "$1").replace(/\s+.*/, "");
  if (/^\d{1,3}([.,]\d{3})+$/.test(raw)) raw = raw.replace(/[.,]/g, "");
  let n = Number(raw.replace(",", "."));
  if (!Number.isFinite(n)) return 0;
  const mult = m[2];
  if (mult && /ming|минг|тыс|k/.test(mult)) n *= 1000;
  if (mult && /mln|million|миллион|млн/.test(mult)) n *= 1_000_000;
  return n;
}

/**
 * What a chat message asks for.
 *  { kind: "rate", cur, dir: "sell"|"buy", amount, inSom }   amount of currency (or of so'm when inSom)
 *  { kind: "alert", cur, side: "SELL"|"BUY", threshold }
 *  { kind: "summary" } for "kurs", "курс", a bare greeting
 *  { kind: "alerts" } for the alerts menu
 *  { kind: "number", amount } for a bare number (the worker fills in the last currency and direction)
 *  null when nothing is recognised
 */
export function parse(text) {
  const t = normalize(text);
  const cur = currencyIn(t);
  const n = numberIn(t);
  // The "🔔 Ogohlantirish" keyboard button, or the word typed.
  if (!(n > 0) && /ogohlantir|уведомлен|оповещ|\balerts?\b/.test(t)) return { kind: "alerts" };
  if (cur && n > 0 && (UP.test(t) || DOWN.test(t)) && !/necha|сколько/.test(t)) {
    // "dollar 12 100 dan oshsa": waits for a rate, not an amount.
    return { kind: "alert", cur, side: UP.test(t) ? "SELL" : "BUY", threshold: Math.round(n) };
  }
  if (cur || n > 0) {
    const dir = BUY.test(t) ? "buy" : SELL.test(t) ? "sell" : null;
    // "1 000 000 so'm dollar" asks how many dollars that buys; "100 dollar necha so'm" asks for so'm.
    const somAmount = n > 0 && new RegExp(`\\d[\\d\\s.,]*\\s*(ming|mln|млн|тыс)?\\s*(${SOM.source})`, "u").test(t);
    if (somAmount) return { kind: "rate", cur: cur || "USD", dir: "buy", amount: n, inSom: true };
    if (!cur) return { kind: "number", amount: n }; // "750": the currency and direction the user looked at last
    return { kind: "rate", cur, dir: dir || "sell", dirGiven: !!dir, amount: n > 0 ? n : DEFAULT_AMOUNT[cur], inSom: false };
  }
  if (/kurs|курс|qancha|сколько|narx|^\/?start|salom|привет|assalom/.test(t)) return { kind: "summary" };
  return null;
}

/* ---------- ranking ---------- */

/** Banks best first for the user's direction: selling currency wants the highest bank buy rate, buying the lowest sell. */
export function rank(data, dir) {
  const banks = (data?.banks || []).filter((b) => (dir === "sell" ? b.buy > 0 : b.sell > 0));
  return dir === "sell" ? banks.sort((a, b) => b.buy - a.buy) : banks.sort((a, b) => a.sell - b.sell);
}

/** The rate an alert watches: best bank buy for SELL, best bank sell for BUY. */
export function alertValue(data, side) {
  const best = rank(data, side === "SELL" ? "sell" : "buy")[0];
  if (!best) return null;
  return { value: side === "SELL" ? best.buy : best.sell, bank: best.bank };
}

export const alertMet = (side, value, threshold) => (side === "SELL" ? value >= threshold : value <= threshold);

/* ---------- formatting ---------- */

export const group = (n) => Math.round(Math.abs(n)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
export function money(n) {
  if (Number.isInteger(n)) return group(n);
  const [i, f] = Math.abs(n).toFixed(2).split(".");
  return `${group(Number(i))},${f}`;
}
export const esc = (s) => String(s).replace(/[&<>]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" })[c]);

/** Display names as people say them; anything not listed keeps the API name. Same list as scripts/telegram-post.mjs. */
const DISPLAY = [
  [/milliy/i, "Milliy Bank"], [/sanoatqurilish/i, "SQB"], [/^apex/i, "Apex Bank"], [/^kdb/i, "KDB Bank"],
  [/^infin/i, "Infin Bank"], [/^ipak/i, "Ipak Yo‘li Bank"], [/^xalq/i, "Xalq Banki"], [/^asia\s*alliance/i, "Asia Alliance"],
  [/^orient/i, "Orient Finans"], [/^universal/i, "Universal Bank"], [/^turon/i, "Turon Bank"], [/^garant/i, "Garant Bank"],
  [/^poytaxt/i, "Poytaxt Bank"], [/^ipoteka/i, "Ipoteka Bank"], [/^kapital/i, "Kapitalbank"], [/^davr/i, "Davr Bank"]
];
export const bankName = (n) => (DISPLAY.find(([re]) => re.test(n)) || [null, n])[1];

/** "24.11, 15:05" in Tashkent time. */
export function stamp(iso) {
  const d = new Date(Date.parse(iso) + TZ_OFFSET_MS);
  const p = (x) => String(x).padStart(2, "0");
  return `${p(d.getUTCDate())}.${p(d.getUTCMonth() + 1)}, ${p(d.getUTCHours())}:${p(d.getUTCMinutes())}`;
}
export const tashkentDate = (ms = Date.now()) => new Date(ms + TZ_OFFSET_MS).toISOString().slice(0, 10);
export const tashkentHour = (ms = Date.now()) => new Date(ms + TZ_OFFSET_MS).getUTCHours();

export const lang = (code) => (/^(ru|be|uk|kk)/.test(code || "") ? "ru" : "uz");

const FLAG = { USD: "💵", EUR: "💶", RUB: "🇷🇺" };
const NAME = {
  uz: { USD: "dollar", EUR: "yevro", RUB: "rubl" },
  ru: { USD: "доллар", EUR: "евро", RUB: "рубль" }
};

export const T = {
  uz: {
    sellHead: (a, cur) => `${FLAG[cur]} <b>${a} ${cur}</b> sotsangiz, eng ko‘p so‘m beradigan banklar:`,
    buyHead: (a, cur) => `${FLAG[cur]} <b>${a} ${cur}</b> sotib olsangiz, eng arzon banklar:`,
    somHead: (a, cur) => `${FLAG[cur]} <b>${a} so‘m</b>ga eng ko‘p ${cur} beradigan banklar:`,
    som: "so‘m",
    cbu: "MB kursi",
    updated: (s) => `Yangilandi: ${s}`,
    note: "Kurslar ma’lumot uchun, almashtirishdan oldin bankda tekshiring.",
    toBuy: "🔁 Sotib olsam",
    toSell: "🔁 Sotsam",
    site: "🌐 Barcha banklar va xarita",
    alertBtn: "🔔 Kurs o‘zgarsa xabar ber",
    summaryHead: "<b>Bugun banklarda eng yaxshi kurslar</b>",
    bestBuy: "sotsangiz",
    bestSell: "olsangiz",
    summaryHint: "Summani yozing, masalan: <code>500 dollar</code>, <code>1000 yevro sotib olaman</code> yoki <code>5 mln so‘m dollar</code>.",
    help:
      "Men O‘zbekistondagi 29 ta bank kursini solishtiraman.\n\n" +
      "Yozing:\n• <code>500 dollar</code>: qaysi bank ko‘p so‘m beradi\n• <code>1000 dollar sotib olaman</code>: qayerda arzon\n" +
      "• <code>5 mln so‘m dollar</code>: so‘mga qancha dollar\n• <code>kurs</code>: bugungi eng yaxshi kurslar\n" +
      "• <code>dollar 12 100 dan oshsa</code>: kurs yetganda xabar beraman\n\n" +
      `Istalgan chatda: <code>@${BOT} 100 dollar</code>`,
    alertHelp:
      "🔔 <b>Kurs ogohlantirishi</b>\n\nKurs siz kutgan songa yetganda shu yerga yozaman. “➕ Yangi ogohlantirish” tugmasini bosing yoki yozing, masalan:\n• <code>dollar 12 100 dan oshsa</code>: banklardan biri dollarni 12 100 yoki qimmatroqqa sotib olsa\n" +
      "• <code>dollar 11 900 dan tushsa</code>: banklardan biri dollarni 11 900 yoki arzonroqqa sotsa\n\nKo‘pi bilan 5 ta.",
    alertList: "Sizning ogohlantirishlaringiz:",
    alertNone: "Hozircha ogohlantirish yo‘q.",
    alertSaved: (cur, side, x, now) =>
      `✅ Saqlandi. ${side === "SELL" ? `Biror bank ${NAME.uz[cur]}ni <b>${x}</b> so‘m yoki qimmatroqqa sotib olsa` : `Biror bank ${NAME.uz[cur]}ni <b>${x}</b> so‘m yoki arzonroqqa sotsa`}, shu yerga yozaman.\nHozir: ${now}.`,
    alertAlready: (now) => `Bu shart hozir ham bajarilgan: ${now}. Boshqa son yozib ko‘ring.`,
    alertFull: "Ko‘pi bilan 5 ta ogohlantirish. Avval bittasini o‘chiring: /ogohlantirish",
    alertOdd: "Bu son bugungi kursdan juda uzoq. Tekshirib qayta yozing.",
    alertFired: (cur, side, x, value, bank) =>
      `🔔 <b>${NAME.uz[cur][0].toUpperCase() + NAME.uz[cur].slice(1)} ${side === "SELL" ? `${x} dan oshdi` : `${x} dan tushdi`}</b>\n${bankName(bank)}: ${side === "SELL" ? "sotib olish" : "sotish"} kursi <b>${value}</b> so‘m.`,
    alertRow: (cur, side, x) => `${cur} ${side === "SELL" ? "≥" : "≤"} ${x} (${side === "SELL" ? "sotish uchun" : "olish uchun"})`,
    del: "🗑",
    deleted: "O‘chirildi",
    stopped: "Ma’lumotlaringiz va ogohlantirishlaringiz o‘chirildi. Qaytsangiz, shunchaki summani yozing.",
    unknown: "Tushunmadim. Masalan: <code>500 dollar</code> yoki <code>kurs</code>. Yordam: /help",
    noData: "Kurslarni hozir ololmadim, birozdan keyin qayta urinib ko‘ring.",
    inlineSell: (a, cur, bank, total) => `${a} ${cur} sotish: ${bank}, ${total} so‘m`,
    inlineBuy: (a, cur, bank, total) => `${a} ${cur} olish: ${bank}, ${total} so‘m`,
    openBot: "🤖 Botda boshqa summa",
    cbuWord: "MB",
    loading: "⏳ Kurslar yuklanmoqda…",
    kbUSD: "💵 Dollar", kbEUR: "💶 Yevro", kbRUB: "🇷🇺 Rubl", kbRates: "📊 Bugungi kurslar", kbAlerts: "🔔 Ogohlantirish",
    dirSell: "Sotaman", dirBuy: "Sotib olaman",
    amountHint: "Boshqa summa: raqamni yozing, masalan <code>750</code>.",
    welcome: "Assalomu alaykum! Pastdagi tugmalardan valyutani tanlang: eng ko‘p so‘m beradigan yoki eng arzon sotadigan 5 bankni ko‘rsataman.",
    newAlert: "➕ Yangi ogohlantirish",
    myAlerts: "🔔 Ogohlantirishlarim",
    back: "⬅️ Orqaga",
    pickCur: "🔔 Qaysi valyuta uchun?",
    pickSide: (cur, s, b) => `🔔 <b>${NAME.uz[cur][0].toUpperCase() + NAME.uz[cur].slice(1)}</b>\nHozir eng yaxshi: sotsangiz <b>${s}</b>, olsangiz <b>${b}</b> so‘m.\n\nNimani kutyapsiz?`,
    sideUp: "📈 Qimmatlashini (sotish uchun)",
    sideDown: "📉 Arzonlashini (olish uchun)",
    pickValue: (cur, side, now) => `🔔 ${side === "SELL" ? `Biror bank ${NAME.uz[cur]}ni qanchadan sotib olsa xabar beray?` : `Biror bank ${NAME.uz[cur]}ni qanchadan sotsa xabar beray?`}\nHozir: <b>${now}</b>\n\nBoshqa son uchun yozing, masalan: <code>${NAME.uz[cur]} ${side === "SELL" ? "12 300 dan oshsa" : "11 800 dan tushsa"}</code>`
  },
  ru: {
    sellHead: (a, cur) => `${FLAG[cur]} <b>${a} ${cur}</b>: где дадут больше всего сумов`,
    buyHead: (a, cur) => `${FLAG[cur]} <b>${a} ${cur}</b>: где купить дешевле`,
    somHead: (a, cur) => `${FLAG[cur]} На <b>${a} сум</b> больше всего ${cur} дадут:`,
    som: "сум",
    cbu: "Курс ЦБ",
    updated: (s) => `Обновлено: ${s}`,
    note: "Курсы справочные, перед обменом уточняйте в банке.",
    toBuy: "🔁 Хочу купить",
    toSell: "🔁 Хочу продать",
    site: "🌐 Все банки и карта",
    alertBtn: "🔔 Сообщить, когда курс изменится",
    summaryHead: "<b>Лучшие курсы в банках сегодня</b>",
    bestBuy: "продать",
    bestSell: "купить",
    summaryHint: "Напишите сумму, например: <code>500 долларов</code>, <code>купить 1000 евро</code> или <code>5 млн сум в долларах</code>.",
    help:
      "Я сравниваю курсы 29 банков Узбекистана.\n\n" +
      "Напишите:\n• <code>500 долларов</code>: какой банк даст больше сумов\n• <code>купить 1000 долларов</code>: где дешевле\n" +
      "• <code>5 млн сум доллар</code>: сколько долларов на сумы\n• <code>курс</code>: лучшие курсы сегодня\n" +
      "• <code>доллар выше 12 100</code>: сообщу, когда курс дойдёт\n\n" +
      `В любом чате: <code>@${BOT} 100 долларов</code>`,
    alertHelp:
      "🔔 <b>Уведомление о курсе</b>\n\nНапишу сюда, когда курс дойдёт до нужного числа. Нажмите «➕ Новое уведомление» или напишите, например:\n• <code>доллар выше 12 100</code>: когда какой-то банк будет покупать доллар по 12 100 или дороже\n" +
      "• <code>доллар ниже 11 900</code>: когда какой-то банк будет продавать доллар по 11 900 или дешевле\n\nНе больше 5.",
    alertList: "Ваши уведомления:",
    alertNone: "Уведомлений пока нет.",
    alertSaved: (cur, side, x, now) =>
      `✅ Сохранено. Напишу, когда какой-то банк будет ${side === "SELL" ? `покупать ${cur} по <b>${x}</b> сум или дороже` : `продавать ${cur} по <b>${x}</b> сум или дешевле`}.\nСейчас: ${now}.`,
    alertAlready: (now) => `Это условие уже выполнено: ${now}. Попробуйте другое число.`,
    alertFull: "Не больше 5 уведомлений. Сначала удалите одно: /alert",
    alertOdd: "Это число слишком далеко от сегодняшнего курса. Проверьте и напишите снова.",
    alertFired: (cur, side, x, value, bank) =>
      `🔔 <b>${cur} ${side === "SELL" ? `выше ${x}` : `ниже ${x}`}</b>\n${bankName(bank)}: курс ${side === "SELL" ? "покупки" : "продажи"} <b>${value}</b> сум.`,
    alertRow: (cur, side, x) => `${cur} ${side === "SELL" ? "≥" : "≤"} ${x} (${side === "SELL" ? "для продажи" : "для покупки"})`,
    del: "🗑",
    deleted: "Удалено",
    stopped: "Ваши данные и уведомления удалены. Чтобы вернуться, просто напишите сумму.",
    unknown: "Не понял. Например: <code>500 долларов</code> или <code>курс</code>. Помощь: /help",
    noData: "Не удалось получить курсы, попробуйте чуть позже.",
    inlineSell: (a, cur, bank, total) => `Продать ${a} ${cur}: ${bank}, ${total} сум`,
    inlineBuy: (a, cur, bank, total) => `Купить ${a} ${cur}: ${bank}, ${total} сум`,
    openBot: "🤖 Другая сумма в боте",
    cbuWord: "ЦБ",
    loading: "⏳ Загружаю курсы…",
    kbUSD: "💵 Доллар", kbEUR: "💶 Евро", kbRUB: "🇷🇺 Рубль", kbRates: "📊 Курсы сегодня", kbAlerts: "🔔 Уведомления",
    dirSell: "Продаю", dirBuy: "Покупаю",
    amountHint: "Другая сумма: напишите число, например <code>750</code>.",
    welcome: "Здравствуйте! Выберите валюту кнопками внизу: покажу 5 банков, где дадут больше сумов или продадут дешевле.",
    newAlert: "➕ Новое уведомление",
    myAlerts: "🔔 Мои уведомления",
    back: "⬅️ Назад",
    pickCur: "🔔 Для какой валюты?",
    pickSide: (cur, s, b) => `🔔 <b>${cur}</b>\nСейчас лучший курс: продать <b>${s}</b>, купить <b>${b}</b> сум.\n\nЧего ждёте?`,
    sideUp: "📈 Подорожания (чтобы продать)",
    sideDown: "📉 Подешевления (чтобы купить)",
    pickValue: (cur, side, now) => `🔔 ${side === "SELL" ? `Когда какой-то банк будет покупать ${cur} по какой цене?` : `Когда какой-то банк будет продавать ${cur} по какой цене?`}\nСейчас: <b>${now}</b>\n\nДругое число можно написать, например: <code>${NAME.ru[cur]} ${side === "SELL" ? "выше 12 300" : "ниже 11 800"}</code>`
  }
};

/* ---------- site links ---------- */

// Amounts that have their own page on the site (web/build.mjs AMOUNTS).
const PAGE_AMOUNTS = {
  USD: [1, 5, 10, 20, 50, 100, 200, 300, 500, 1000, 2000, 5000, 10000],
  EUR: [1, 5, 10, 20, 50, 100, 200, 300, 500, 1000, 2000, 5000, 10000],
  RUB: [100, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000]
};
const UZ_UNIT = { USD: "dollar", EUR: "yevro", RUB: "rubl" };
const CUR_PAGE = { uz: { USD: "/", EUR: "/yevro-kursi", RUB: "/rubl-kursi" }, ru: { USD: "/ru/", EUR: "/ru/kurs-evro", RUB: "/ru/kurs-rublya" } };

export function siteUrl(l, cur, amount) {
  if (amount && PAGE_AMOUNTS[cur].includes(amount)) {
    return SITE + (l === "ru" ? `/ru/${amount}-${cur.toLowerCase()}-v-sumah` : `/${amount}-${UZ_UNIT[cur]}-necha-som`);
  }
  return SITE + CUR_PAGE[l][cur];
}

/* ---------- messages ---------- */

const MEDAL = ["🥇", "🥈", "🥉", "4.", "5."];

/** The top-5 text and its buttons for one request. */
export function rateMessage(l, data, q) {
  const t = T[l];
  const list = rank(data, q.dir).slice(0, 5);
  if (!list.length) return { text: t.noData };
  let head, lines;
  if (q.inSom) {
    head = t.somHead(group(q.amount), q.cur);
    lines = list.map((b, i) => `${MEDAL[i]} ${esc(bankName(b.bank))}: <b>${money(q.amount / b.sell)} ${q.cur}</b> <i>(${money(b.sell)})</i>`);
  } else {
    head = q.dir === "sell" ? t.sellHead(money(q.amount), q.cur) : t.buyHead(money(q.amount), q.cur);
    const key = q.dir === "sell" ? "buy" : "sell";
    lines = list.map((b, i) => `${MEDAL[i]} ${esc(bankName(b.bank))}: <b>${group(q.amount * b[key])} ${t.som}</b> <i>(${money(b[key])})</i>`);
    const worst = rank(data, q.dir).at(-1);
    const gap = Math.abs(list[0][key] - worst[key]) * q.amount;
    if (gap >= 1000) lines.push(l === "ru" ? `\nРазница между лучшим и худшим банком: <b>${group(gap)} сум</b>` : `\nEng yaxshi va eng yomon bank farqi: <b>${group(gap)} so‘m</b>`);
  }
  const text = [head, "", ...lines, "", `${t.cbu}: ${money(data.cbu?.rate || 0)} · ${t.updated(stamp(data.updatedAt))}`, `<i>${t.note}</i>`].join("\n");
  return { text, reply_markup: rateButtons(l, q) };
}

// Quick amounts under a result; any other amount is typed as a number.
export const AMOUNT_BUTTONS = { USD: [100, 500, 1000, 5000, 10000], EUR: [100, 500, 1000, 5000, 10000], RUB: [5000, 10000, 50000, 100000] };
const SOM_BUTTONS = [1_000_000, 5_000_000, 10_000_000, 50_000_000];
const short = (n) => (n >= 1_000_000 ? `${n / 1_000_000} mln` : group(n));

export function rateButtons(l, q) {
  const t = T[l];
  const mark = (on, text) => (on ? `✅ ${text}` : text);
  const amounts = q.inSom ? SOM_BUTTONS : AMOUNT_BUTTONS[q.cur];
  return {
    inline_keyboard: [
      amounts.map((a) => ({ text: mark(a === q.amount, short(a)), callback_data: cb({ ...q, amount: a }) })),
      q.inSom ? [] : [
        { text: mark(q.dir === "sell", t.dirSell), callback_data: cb({ ...q, dir: "sell" }) },
        { text: mark(q.dir === "buy", t.dirBuy), callback_data: cb({ ...q, dir: "buy" }) }
      ],
      // Switching currency keeps the direction and starts from that currency's usual amount.
      CURRENCIES.map((cur) => ({ text: mark(cur === q.cur, cur), callback_data: cb(cur === q.cur ? q : { ...q, cur, amount: q.inSom ? q.amount : DEFAULT_AMOUNT[cur] }) })),
      [{ text: t.site, url: siteUrl(l, q.cur, q.inSom ? 0 : q.amount) }],
      [{ text: t.alertBtn, callback_data: "an|" + q.cur }]
    ].filter((row) => row.length)
  };
}

/** The keyboard under the message box: currencies, today's rates, alerts. Its buttons send their text. */
export function mainKeyboard(l) {
  const t = T[l];
  return {
    keyboard: [[{ text: t.kbUSD }, { text: t.kbEUR }, { text: t.kbRUB }], [{ text: t.kbRates }, { text: t.kbAlerts }]],
    resize_keyboard: true,
    is_persistent: true
  };
}

/* ---------- alerts by buttons: currency → what to wait for → value ---------- */

export function alertCurMenu(l) {
  const t = T[l];
  return { text: t.pickCur, reply_markup: { inline_keyboard: [CURRENCIES.map((cur) => ({ text: cur, callback_data: `an|${cur}` })), [{ text: t.myAlerts, callback_data: "ah" }]] } };
}

export function alertSideMenu(l, cur, data) {
  const t = T[l];
  const s = alertValue(data, "SELL"), b = alertValue(data, "BUY");
  return {
    text: t.pickSide(cur, s ? money(s.value) : "—", b ? money(b.value) : "—"),
    reply_markup: { inline_keyboard: [[{ text: t.sideUp, callback_data: `as|${cur}|S` }], [{ text: t.sideDown, callback_data: `as|${cur}|B` }], [{ text: t.back, callback_data: "an" }]] }
  };
}

const STEPS = { USD: [20, 50, 100, 200], EUR: [20, 50, 100, 200], RUB: [1, 2, 3, 5] };
/** Values a bit beyond today's best rate, in the direction the user waits for. */
export function alertChoices(cur, side, now) {
  const round = (x) => (cur === "RUB" ? Math.round(x * 2) / 2 : Math.round(x / 5) * 5);
  return STEPS[cur].map((d) => round(side === "SELL" ? now + d : now - d));
}

export function alertValueMenu(l, cur, side, data) {
  const t = T[l];
  const now = alertValue(data, side);
  if (!now) return { text: t.noData };
  const choices = alertChoices(cur, side, now.value);
  const rows = [];
  for (let i = 0; i < choices.length; i += 2) {
    rows.push(choices.slice(i, i + 2).map((v) => ({ text: `${side === "SELL" ? "≥" : "≤"} ${money(v)}`, callback_data: `ac|${cur}|${side === "SELL" ? "S" : "B"}|${v}` })));
  }
  rows.push([{ text: t.back, callback_data: `an|${cur}` }]);
  return { text: t.pickValue(cur, side, `${esc(bankName(now.bank))}, ${money(now.value)} ${t.som}`), reply_markup: { inline_keyboard: rows } };
}

/** Callback data, at most 64 bytes: "r|USD|s|500|0". */
export const cb = (q) => `r|${q.cur}|${q.dir === "buy" ? "b" : "s"}|${q.amount}|${q.inSom ? 1 : 0}`;
export function uncb(data) {
  const [k, cur, d, a, s] = String(data).split("|");
  const amount = Number(a);
  if (k !== "r" || !CURRENCIES.includes(cur) || !(amount > 0)) return null;
  return { kind: "rate", cur, dir: d === "b" ? "buy" : "sell", amount, inSom: s === "1" };
}

/** "kurs": best buy and sell of the three currencies. */
export function summaryMessage(l, all) {
  const t = T[l];
  const rows = CURRENCIES.filter((c) => all[c]).map((cur) => {
    const d = all[cur], s = rank(d, "sell")[0], b = rank(d, "buy")[0];
    if (!s || !b) return "";
    return `${FLAG[cur]} <b>${cur}</b> · ${t.cbuWord} ${money(d.cbu?.rate || 0)}\n   ${t.bestBuy}: <b>${money(s.buy)}</b> ${esc(bankName(s.bank))}\n   ${t.bestSell}: <b>${money(b.sell)}</b> ${esc(bankName(b.bank))}`;
  }).filter(Boolean);
  const first = all.USD || Object.values(all)[0];
  const text = [t.summaryHead, "", rows.join("\n\n"), "", t.updated(first ? stamp(first.updatedAt) : "—"), "", t.summaryHint].join("\n");
  return {
    text,
    reply_markup: { inline_keyboard: [CURRENCIES.map((cur) => ({ text: cur, callback_data: cb({ cur, dir: "sell", amount: DEFAULT_AMOUNT[cur], inSom: false }) })), [{ text: t.site, url: SITE + (l === "ru" ? "/ru/" : "/") }]] }
  };
}

/** Inline mode: one result per direction for the asked amount ("@qulaykursbot 1000 usd"). */
export function inlineResults(l, data, q) {
  const t = T[l];
  const out = [];
  for (const dir of q.inSom ? ["buy"] : ["sell", "buy"]) {
    const qq = { ...q, dir };
    const best = rank(data, dir)[0];
    if (!best) continue;
    const msg = rateMessage(l, data, qq);
    const total = q.inSom ? `${money(q.amount / best.sell)} ${q.cur}` : group(q.amount * (dir === "sell" ? best.buy : best.sell));
    out.push({
      type: "article",
      id: `${dir}-${q.cur}-${q.amount}-${q.inSom ? 1 : 0}`,
      title: dir === "sell" ? t.inlineSell(money(q.amount), q.cur, bankName(best.bank), total) : t.inlineBuy(q.inSom ? group(q.amount) + " " + t.som : money(q.amount), q.cur, bankName(best.bank), total),
      description: `${t.cbu}: ${money(data.cbu?.rate || 0)} · ${t.updated(stamp(data.updatedAt))}`,
      input_message_content: { message_text: msg.text, parse_mode: "HTML", link_preview_options: { is_disabled: true } },
      // Buttons that work for anyone who sees the forwarded message: the site and the bot.
      reply_markup: { inline_keyboard: [[{ text: t.site, url: siteUrl(l, q.cur, q.inSom ? 0 : q.amount) }], [{ text: t.openBot, url: `https://t.me/${BOT}?start=inline` }]] }
    });
  }
  return out;
}

/* ---------- sharp moves for the channel ---------- */

// How far the best bank rate must move from the morning to be worth an extra channel post.
export const MOVE_PCT = { USD: 0.4, EUR: 0.5, RUB: 1.5 };
const UZ_GEN = { USD: "Dollar", EUR: "Yevro", RUB: "Rubl" };

/**
 * A channel post when the best buy or sell rate moved far from the morning baseline, else null.
 * base: { buy, sell } from the first check after 08:00; now: { buy, sell, buyBank, sellBank }.
 */
export function moveMessage(cur, base, now, cbuRate) {
  const pct = (a, b) => ((a - b) / b) * 100;
  const dBuy = pct(now.buy, base.buy), dSell = pct(now.sell, base.sell);
  const d = Math.abs(dBuy) >= Math.abs(dSell) ? dBuy : dSell;
  if (Math.abs(d) < MOVE_PCT[cur]) return null;
  const up = d > 0;
  const sign = (x) => (x > 0 ? "+" : x < 0 ? "−" : "") + money(Math.abs(x));
  return [
    `${up ? "📈" : "📉"} <b>${UZ_GEN[cur]} banklarda bugun ${up ? "qimmatladi" : "arzonladi"}</b>`,
    "",
    `Sotsangiz (eng yaxshi): <b>${money(now.buy)}</b> so‘m, ertalab ${money(base.buy)} (${sign(now.buy - base.buy)})`,
    `   ${esc(bankName(now.buyBank))}`,
    `Olsangiz (eng arzon): <b>${money(now.sell)}</b> so‘m, ertalab ${money(base.sell)} (${sign(now.sell - base.sell)})`,
    `   ${esc(bankName(now.sellBank))}`,
    "",
    `MB kursi: ${money(cbuRate)}`,
    `👉 <a href="${SITE}${cur === "USD" ? "/" : cur === "EUR" ? "/yevro-kursi" : "/rubl-kursi"}">qulaykurs.uz</a> · <a href="https://t.me/${BOT}">@${BOT}</a>`
  ].join("\n");
}
