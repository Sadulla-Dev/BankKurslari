// Builds the deployable site into ./dist: a copy of ./public plus pre-rendered pages, so that search engines and
// link previews get real URLs with today's rates in the HTML instead of one empty JavaScript shell.
//
//   node build.mjs            reads https://api.qulaykurs.uz (API_URL overrides), writes ./dist
//
// Pages, in Uzbek (/), Uzbek in Cyrillic (/uzc/, the Uzbek texts transliterated by shared.js), Russian (/ru/) and English (/en/):
//   currency pages   /, /yevro-kursi, /rubl-kursi …   the app itself, with the bank table already filled in
//   bank pages       /bank/<slug>                     plain HTML: one bank's rates for all three currencies
//   gold page        /oltin-narxi …                   plain HTML: Central Bank gold bar prices
//   all currencies   /valyutalar, /valyuta/<code>     plain HTML: every Central Bank rate, one page per currency
//   amount pages     /100-dollar-necha-som …          plain HTML: what an amount is worth at each bank, with a calculator
//                    /1000000-som-necha-dollar …      the same from so‘m; /1000-von-necha-som … at the Central Bank rate
//   history pages    /dollar-kursi-tarixi …           plain HTML: the Central Bank rate by year and for the last 30 days
// plus sitemap.xml, 404.html and .indexnow.json (the list of addresses the deploy workflow sends to IndexNow).
// GOOGLE_SITE_VERIFICATION / YANDEX_VERIFICATION, when set, add the search consoles' ownership tags to every page. No dependencies. Runs hourly from .github/workflows/web-deploy.yml.
import { createHash } from "node:crypto";
import { cp, mkdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const HERE = dirname(fileURLToPath(import.meta.url));
const PUBLIC = join(HERE, "public"), DIST = join(HERE, "dist");
const API = (process.env.API_URL || "https://api.qulaykurs.uz").replace(/\/$/, "");
const ORIGIN = "https://qulaykurs.uz";
const LANGS = ["uz", "uzc", "ru", "en"], CURS = ["USD", "EUR", "RUB"];
// "uzc" is this site's own name for Uzbek in Cyrillic; pages and search engines are told the standard one.
const HTML_LANG = { uz: "uz", uzc: "uz-Cyrl", ru: "ru", en: "en" }, LANG_LABEL = { uz: "UZ", uzc: "ЎЗ", ru: "RU", en: "EN" };
const isUz = (lang) => lang === "uz" || lang === "uzc", isCyrillic = (lang) => lang === "uzc" || lang === "ru";
// Cache-buster for css/js: changes only when one of those files changes, so _headers can let browsers keep them for good.
const VERSION = await (async () => {
  const hash = createHash("sha1");
  for (const f of ["style.css", "pages.css", "shared.js", "app.js", "calc.js", "wallet.js"]) hash.update(await readFile(join(PUBLIC, f)));
  return hash.digest("hex").slice(0, 10);
})();
// The key file public/<key>.txt proves to IndexNow (Yandex, Bing) that address lists come from this site. Not a secret.
const INDEXNOW_KEY = "00ece51a0affd5c9f0edc23507e252e0";
const VERIFY = [["google-site-verification", process.env.GOOGLE_SITE_VERIFICATION], ["yandex-verification", process.env.YANDEX_VERIFICATION]]
  .filter(([, v]) => /^[\w-]{8,100}$/.test(v || "")).map(([name, v]) => `<meta name="${name}" content="${v}">`).join("\n");

/* ---------- shared with the browser app ---------- */

const shared = vm.createContext({});
vm.runInContext((await readFile(join(PUBLIC, "shared.js"), "utf8")) + "\n;globalThis.out = { T, logoSlug, uzCyrillic, uzCyrillicTexts };", shared);
const { T, logoSlug, uzCyrillic, uzCyrillicTexts } = shared.out;

/* ---------- addresses of pages ---------- */

const PREFIX = { uz: "", uzc: "/uzc", ru: "/ru", en: "/en" }; // the Cyrillic pages keep the Latin addresses of the Uzbek ones
const CUR_SLUG = {
  uz: { USD: "", EUR: "yevro-kursi", RUB: "rubl-kursi" },
  ru: { USD: "", EUR: "kurs-evro", RUB: "kurs-rublya" },
  en: { USD: "", EUR: "eur-rate", RUB: "rub-rate" },
};
const GOLD_SLUG = { uz: "oltin-narxi", uzc: "oltin-narxi", ru: "cena-zolota", en: "gold-price" };
CUR_SLUG.uzc = CUR_SLUG.uz;
const curUrl = (lang, cur) => `${PREFIX[lang]}/${CUR_SLUG[lang][cur]}`;
const goldUrl = (lang) => `${PREFIX[lang]}/${GOLD_SLUG[lang]}`;
const bankUrl = (lang, slug) => `${PREFIX[lang]}/bank/${slug}`;
const CBU_DIR = { uz: "valyuta", uzc: "valyuta", ru: "valyuta", en: "currency" };
const ALL_SLUG = { uz: "valyutalar", uzc: "valyutalar", ru: "kursy-valyut", en: "currencies" };
const HIST_SLUG = {
  uz: { USD: "dollar-kursi-tarixi", EUR: "yevro-kursi-tarixi", RUB: "rubl-kursi-tarixi" },
  ru: { USD: "kurs-dollara-istoriya", EUR: "kurs-evro-istoriya", RUB: "kurs-rublya-istoriya" },
  en: { USD: "usd-rate-history", EUR: "eur-rate-history", RUB: "rub-rate-history" },
};
HIST_SLUG.uzc = HIST_SLUG.uz;
// Currency words for the amount pages. uz: one form, also the word in the page address; ru: after 1, after 2–4, after 5
// and more; ruIn: "в долларах" (only where sums are converted back, see REV_AMOUNTS); en: one, many.
const UNIT = {
  USD: { uz: "dollar", ru: ["доллар", "доллара", "долларов"], ruIn: "долларах", en: ["US dollar", "US dollars"] },
  EUR: { uz: "yevro", ru: ["евро", "евро", "евро"], ruIn: "евро", en: ["euro", "euros"] },
  RUB: { uz: "rubl", ru: ["рубль", "рубля", "рублей"], ruIn: "рублях", en: ["Russian ruble", "Russian rubles"] },
  KZT: { uz: "tenge", ru: ["тенге", "тенге", "тенге"], en: ["Kazakh tenge", "Kazakh tenge"] },
  KRW: { uz: "von", ru: ["вона", "воны", "вон"], en: ["South Korean won", "South Korean won"] },
  TRY: { uz: "lira", ru: ["турецкая лира", "турецкие лиры", "турецких лир"], en: ["Turkish lira", "Turkish lira"] },
  CNY: { uz: "yuan", ru: ["юань", "юаня", "юаней"], en: ["Chinese yuan", "Chinese yuan"] },
  AED: { uz: "dirham", ru: ["дирхам ОАЭ", "дирхама ОАЭ", "дирхамов ОАЭ"], en: ["UAE dirham", "UAE dirhams"] },
  GBP: { uz: "funt", ru: ["фунт стерлингов", "фунта стерлингов", "фунтов стерлингов"], en: ["British pound", "British pounds"] },
  JPY: { uz: "iyena", ru: ["иена", "иены", "иен"], en: ["Japanese yen", "Japanese yen"] },
  SAR: { uz: "riyal", ru: ["саудовский риял", "саудовских рияла", "саудовских риялов"], en: ["Saudi riyal", "Saudi riyals"] },
};
// Amounts people actually search for ("100 dollar necha so‘m", "1000 von necha so‘m"); each gets a page per language.
// Dollar, euro and ruble pages compare banks; the others (where Uzbeks work, trade and travel) use the Central Bank rate.
const AMOUNTS = {
  USD: [1, 5, 10, 20, 50, 100, 200, 300, 500, 1000, 2000, 5000, 10000], EUR: [1, 5, 10, 20, 50, 100, 200, 300, 500, 1000, 2000, 5000, 10000],
  RUB: [100, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000], KZT: [100, 500, 1000, 5000, 10000, 50000, 100000],
  KRW: [1000, 5000, 10000, 50000, 100000, 1000000], TRY: [1, 10, 50, 100, 500, 1000, 5000], CNY: [1, 10, 50, 100, 500, 1000, 5000, 10000],
  AED: [1, 10, 50, 100, 500, 1000, 5000], GBP: [1, 10, 50, 100, 500, 1000], JPY: [100, 1000, 10000, 100000], SAR: [1, 10, 100, 500, 1000, 5000],
};
// One amount of each currency is linked from every page, so the rest can be reached from anywhere.
const FEATURED = { USD: 100, EUR: 100, RUB: 1000, KZT: 1000, KRW: 10000, TRY: 100, CNY: 100, AED: 100, GBP: 100, JPY: 10000, SAR: 100 };
// The other direction ("1000000 so‘m necha dollar"), for the three currencies banks sell.
const REV_AMOUNTS = [100000, 500000, 1000000, 5000000, 10000000, 50000000, 100000000];
const allUrl = (lang) => `${PREFIX[lang]}/${ALL_SLUG[lang]}`;
const cbuUrl = (lang, code) => (CURS.includes(code) ? curUrl(lang, code) : `${PREFIX[lang]}/${CBU_DIR[lang]}/${code.toLowerCase()}`);
const histUrl = (lang, cur) => `${PREFIX[lang]}/${HIST_SLUG[lang][cur]}`;
const amountUrl = (lang, cur, n) => `${PREFIX[lang]}/${n}-${isUz(lang) ? `${UNIT[cur].uz}-necha-som` : lang === "ru" ? `${cur.toLowerCase()}-v-sumah` : `${cur.toLowerCase()}-to-uzs`}`;
const revUrl = (lang, cur, n) => `${PREFIX[lang]}/${n}-${isUz(lang) ? `som-necha-${UNIT[cur].uz}` : lang === "ru" ? `uzs-v-${cur.toLowerCase()}` : `uzs-to-${cur.toLowerCase()}`}`;
const fileOf = (url) => (url.endsWith("/") ? `${url}index.html` : `${url}.html`).slice(1);
const slugify = (name) => name.toLowerCase().replace(/[‘’ʻ'`]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const bankSlug = (name) => logoSlug(name) || slugify(name);

/* ---------- formatting ---------- */

const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
const group = (n) => Math.round(Math.abs(n)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
const rate = (n, lang) => (Number.isInteger(n) ? group(n) : `${group(Math.trunc(n))}${lang === "en" ? "." : ","}${n.toFixed(2).split(".")[1]}`);
// Any size of number: whole so‘m for big sums, more decimals the smaller the value (1 so‘m is 0.00008 dollars).
// public/calc.js rounds the same way.
function num(n, lang) {
  const a = Math.abs(n), digits = a >= 100000 ? 0 : a >= 1 ? 2 : a >= 0.01 ? 4 : 6;
  const [whole, frac = ""] = a.toFixed(digits).split("."), tail = frac.replace(/0+$/, "");
  return group(Number(whole)) + (tail ? (lang === "en" ? "." : ",") + tail : "");
}
const pct = (ratio, lang) => `${ratio < 0 ? "−" : "+"}${(Math.abs(ratio) * 100).toFixed(1).replace(".", lang === "en" ? "." : ",")}`;
const dayLabel = (iso, lang) => (lang === "en" ? iso : iso.split("-").reverse().join("."));
const delta = (n, lang) => (Math.abs(n) < 0.005 ? "0" : signed(n, lang));
const signed = (n, lang) => `${n < 0 ? "−" : "+"}${rate(Math.abs(n), lang)}`;
const MONTHS = {
  uz: ["yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"],
  uzc: ["январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"],
  ru: ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"],
  en: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
};
const fmtDate = (y, m, d, lang) => (lang === "en" ? `${MONTHS.en[m - 1]} ${d}, ${y}` : lang === "ru" ? `${d} ${MONTHS.ru[m - 1]} ${y}` : lang === "uzc" ? `${y} йил ${d} ${MONTHS.uzc[m - 1]}` : `${y}-yil ${d}-${MONTHS.uz[m - 1]}`);
// The Central Bank writes its dates as 20.11.2026.
const cbuDate = (s, lang) => { const m = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(s || ""); return m ? fmtDate(+m[3], +m[2], +m[1], lang) : s || ""; };
// Search results cut titles at about 60 characters, so long ones go without the site name (it is shown beside the result anyway).
const fit = (title) => (title.length > 62 ? title.replace(/ \| Qulay Kurs$/, "") : title);
// Tashkent is UTC+5 all year.
function when(iso, lang) {
  const d = new Date(Date.parse(iso) + 5 * 3600e3), day = d.getUTCDate(), y = d.getUTCFullYear();
  const time = `${String(d.getUTCHours()).padStart(2, "0")}:${String(d.getUTCMinutes()).padStart(2, "0")}`;
  return { date: fmtDate(y, d.getUTCMonth() + 1, day, lang), time };
}
function ruPlural(n, one, few, many) {
  const a = Math.abs(n) % 100, b = a % 10;
  return a > 10 && a < 20 ? many : b === 1 ? one : b >= 2 && b <= 4 ? few : many;
}

/* ---------- texts of the generated parts (the app's own texts come from shared.js) ---------- */

const AMOUNT = { USD: 100, EUR: 100, RUB: 10000 };
const X = {
  uz: {
    cur: { USD: "dollar", EUR: "yevro", RUB: "rubl" }, Cur: { USD: "Dollar", EUR: "Yevro", RUB: "Rubl" },
    amount: (c) => `${group(AMOUNT[c])} ${X.uz.cur[c]}`, som: "so‘m", banks: (n) => `${n} ta bank`, diffText: (d) => (Math.abs(d) < 0.005 ? "kechagidan o‘zgarmagan" : `kechagidan ${signed(d, "uz")}`),
    title: (c) => `${X.uz.Cur[c]} kursi bugun: banklarda sotib olish va sotish | Qulay Kurs`,
    desc: (c, s) => `Bugun ${X.uz.cur[c]}ni eng qimmat ${s.bestBuy.bank} oladi — ${s.buy} so‘m, eng arzon ${s.bestSell.bank} sotadi — ${s.sell} so‘m. ${s.count} ta bank kursi, Markaziy bank kursi (${s.cbu}) va kalkulyator.`,
    sub: (n, time) => `${n} ta bank · ${time} holatiga · eng foydalisi tepada`,
    h2: (c, date) => `${X.uz.Cur[c]} kursi bugun, ${date}`,
    lead: (c, s) => `Markaziy bank kursi — 1 ${X.uz.cur[c]} = ${s.cbu} so‘m (${s.diffText}). Banklar ${X.uz.cur[c]}ni ${s.minBuy}–${s.buy} so‘mdan sotib oladi va ${s.sell}–${s.maxSell} so‘mdan sotadi. ${X.uz.amount(c)} almashtirganda eng yaxshi va eng yomon bank orasidagi farq — ${s.gap} so‘m.`,
    q1: (c) => `${X.uz.Cur[c]}ni qaysi bankka sotgan foydali?`, a1: (c, s) => `Bugun eng yuqori sotib olish kursi: ${s.topBuy}. ${X.uz.amount(c)} uchun ${s.bestBuy.bank} ${s.buyTotal} so‘m beradi.`,
    q2: (c) => `${X.uz.Cur[c]}ni qayerdan arzon sotib olish mumkin?`, a2: (c, s) => `Bugun eng past sotish kursi: ${s.topSell}. ${X.uz.amount(c)} sotib olish ${s.bestSell.bank}da ${s.sellTotal} so‘mga tushadi.`,
    q3: "Markaziy bank kursi nega bank kursidan farq qiladi?", a3: "Markaziy bank kursi — rasmiy hisob-kitob kursi: u buxgalteriya, bojxona va soliq uchun ishlatiladi. Naqd valyutani banklar o‘zlari belgilagan sotib olish va sotish kurslari bo‘yicha almashtiradi, shuning uchun bankdagi kurs har doim biroz boshqacha bo‘ladi.",
    other: "Boshqa valyutalar", allBanks: "Banklar bo‘yicha kurslar", gold: "Oltin quymalar narxi", home: "Kurslar", map: "Xarita", history: "Tarix", wallet: "Hamyon",
    bankTitle: (b, s) => `${b}: dollar kursi bugun — ${s} | Qulay Kurs`, bankH1: (b) => `${b}: valyuta kurslari bugun`,
    bankDesc: (b, u) => `${b} bugun dollarni ${u.buy} so‘mdan sotib oladi va ${u.sell} so‘mdan sotadi. Yevro va rubl kurslari, boshqa banklar bilan solishtirish — Qulay Kursda.`,
    bankShort: (u) => `oladi ${u.buy}, sotadi ${u.sell}`, colCur: "Valyuta", colBuy: "Bank sotib oladi", colSell: "Bank sotadi", colCbu: "Markaziy bank",
    updated: (d, t) => `${d}, ${t} holatiga`, rankBuy: (c, r, n) => `${X.uz.Cur[c]} sotib olish kursi bo‘yicha ${n} ta bank ichida ${r}-o‘rinda.`,
    bankSell: (c, b, total, gap, best) => `${X.uz.amount(c)} sotsangiz, ${b} ${total} so‘m beradi` + (gap ? ` — eng yaxshi kursdan (${best}) ${gap} so‘m kam.` : ` — bu bugungi eng yaxshi kurs.`),
    address: "Bosh ofis manzili", compare: "Barcha banklar bilan solishtirish", otherBanks: "Boshqa banklar",
    disclaimer: "Kurslar ma’lumot uchun berilgan va banklar hamda Markaziy bank e’lon qilgan ma’lumotlarga asoslanadi. Aniq kursni bankning o‘zidan tekshiring. Qulay Kurs biror bankka tegishli emas.",
    goldTitle: "Oltin (tilla) quymalar narxi bugun — Markaziy bank | Qulay Kurs", goldH1: "Markaziy bank oltin quymalari narxi",
    goldDesc: (g) => `Bugun 1 gramm oltin (tilla, 10 g quyma) — ${g.perGram} so‘m. 5, 10, 20, 50 va 100 grammlik quymalarning sotilish va qaytarib olinish narxlari.`,
    goldLead: (g) => `Markaziy bank 10 grammlik oltin quymani ${g.sell10} so‘mga sotadi va qadog‘i buzilmagan holda ${g.back10} so‘mga qaytarib oladi. Quymani olib, darhol qaytarib topshirsangiz, narxning ${g.loss}% qismini yo‘qotasiz.`,
    goldCols: ["Quyma", "Sotib olish narxi", "Qaytarib topshirish", "1 gramm", "Farq"], gram: (n) => `${n} g`,
    notFound: "Sahifa topilmadi", notFoundText: "Bunday sahifa yo‘q. Bugungi kurslar bosh sahifada.", toHome: "Bosh sahifaga",
  },
  ru: {
    cur: { USD: "доллара", EUR: "евро", RUB: "рубля" }, Cur: { USD: "доллара", EUR: "евро", RUB: "рубля" }, acc: { USD: "доллар", EUR: "евро", RUB: "рубль" },
    amount: (c) => `${group(AMOUNT[c])} ${{ USD: "долларов", EUR: "евро", RUB: "рублей" }[c]}`, som: "сум", banks: (n) => `${n} ${ruPlural(n, "банк", "банка", "банков")}`, diffText: (d) => (Math.abs(d) < 0.005 ? "без изменений со вчерашнего дня" : `${signed(d, "ru")} ко вчерашнему`),
    title: (c) => `Курс ${X.ru.cur[c]} в банках Узбекистана сегодня: покупка и продажа | Qulay Kurs`,
    desc: (c, s) => `Сегодня дороже всех ${X.ru.acc[c]} покупает ${s.bestBuy.bank} — ${s.buy} сум, дешевле всех продаёт ${s.bestSell.bank} — ${s.sell} сум. Курсы ${X.ru.banks(s.count).replace(/банк$/, "банка")}, курс ЦБ (${s.cbu}) и калькулятор.`,
    sub: (n, time) => `${X.ru.banks(n)} · на ${time} · сначала выгодные`,
    h2: (c, date) => `Курс ${X.ru.cur[c]} на сегодня, ${date}`,
    lead: (c, s) => `Курс Центрального банка — 1 ${X.ru.acc[c]} = ${s.cbu} сум (${s.diffText}). Банки покупают ${X.ru.acc[c]} по ${s.minBuy}–${s.buy} сум и продают по ${s.sell}–${s.maxSell} сум. При обмене ${X.ru.amount(c)} разница между лучшим и худшим банком — ${s.gap} сум.`,
    q1: (c) => `В каком банке выгоднее продать ${X.ru.acc[c]}?`, a1: (c, s) => `Самый высокий курс покупки сегодня: ${s.topBuy}. За ${X.ru.amount(c)} ${s.bestBuy.bank} даст ${s.buyTotal} сум.`,
    q2: (c) => `Где дешевле купить ${X.ru.acc[c]}?`, a2: (c, s) => `Самый низкий курс продажи сегодня: ${s.topSell}. Купить ${X.ru.amount(c)} в ${s.bestSell.bank} обойдётся в ${s.sellTotal} сум.`,
    q3: "Почему курс ЦБ отличается от курса в банке?", a3: "Курс Центрального банка — официальный расчётный курс: он нужен для бухгалтерии, таможни и налогов. Наличную валюту банки меняют по собственным курсам покупки и продажи, поэтому курс в банке всегда немного другой.",
    other: "Другие валюты", allBanks: "Курсы по банкам", gold: "Цены на золотые слитки", home: "Курсы", map: "Карта", history: "История", wallet: "Кошелёк",
    bankTitle: (b, s) => `${b}: курс доллара сегодня — ${s} | Qulay Kurs`, bankH1: (b) => `${b}: курсы валют сегодня`,
    bankDesc: (b, u) => `${b} сегодня покупает доллар по ${u.buy} сум и продаёт по ${u.sell} сум. Курсы евро и рубля, сравнение с другими банками Узбекистана — в Qulay Kurs.`,
    bankShort: (u) => `покупка ${u.buy}, продажа ${u.sell}`, colCur: "Валюта", colBuy: "Банк покупает", colSell: "Банк продаёт", colCbu: "Курс ЦБ",
    updated: (d, t) => `на ${d}, ${t}`, rankBuy: (c, r, n) => `По курсу покупки ${X.ru.cur[c]} — ${r}-е место среди ${n} банков.`,
    bankSell: (c, b, total, gap, best) => `Если продать ${X.ru.amount(c)}, ${b} даст ${total} сум` + (gap ? ` — на ${gap} сум меньше, чем по лучшему курсу (${best}).` : ` — это лучший курс на сегодня.`),
    address: "Адрес головного офиса", compare: "Сравнить со всеми банками", otherBanks: "Другие банки",
    disclaimer: "Курсы приведены для справки и основаны на данных, опубликованных банками и Центральным банком. Точный курс уточняйте в банке. Qulay Kurs не связан ни с одним банком.",
    goldTitle: "Цены на золотые слитки сегодня — Центральный банк Узбекистана | Qulay Kurs", goldH1: "Цены на золотые слитки Центрального банка",
    goldDesc: (g) => `Сегодня 1 грамм золота (слиток 10 г) — ${g.perGram} сум. Цены продажи и обратного выкупа слитков 5, 10, 20, 50 и 100 граммов.`,
    goldLead: (g) => `Центральный банк продаёт слиток 10 граммов за ${g.sell10} сум и выкупает его в неповреждённой упаковке за ${g.back10} сум. Если купить слиток и сразу сдать обратно, потеряете ${g.loss}% цены.`,
    goldCols: ["Слиток", "Цена покупки", "Обратный выкуп", "1 грамм", "Разница"], gram: (n) => `${n} г`,
    notFound: "Страница не найдена", notFoundText: "Такой страницы нет. Курсы на сегодня — на главной.", toHome: "На главную",
  },
  en: {
    cur: { USD: "US dollar", EUR: "euro", RUB: "ruble" }, Cur: { USD: "US dollar", EUR: "Euro", RUB: "Ruble" },
    amount: (c) => `${group(AMOUNT[c])} ${{ USD: "US dollars", EUR: "euros", RUB: "rubles" }[c]}`, som: "so‘m", banks: (n) => `${n} banks`, diffText: (d) => (Math.abs(d) < 0.005 ? "unchanged since yesterday" : `${signed(d, "en")} since yesterday`),
    title: (c) => `${c} to UZS today: ${X.en.cur[c]} rates at banks in Uzbekistan | Qulay Kurs`,
    desc: (c, s) => `Today ${s.bestBuy.bank} pays the most for a ${X.en.cur[c]} — ${s.buy} so‘m — and ${s.bestSell.bank} sells it cheapest at ${s.sell} so‘m. Rates from ${s.count} banks, the Central Bank rate (${s.cbu}) and a converter.`,
    sub: (n, time) => `${n} banks · as of ${time} · best first`,
    h2: (c, date) => `${X.en.Cur[c]} exchange rate in Uzbekistan today, ${date}`,
    lead: (c, s) => `The Central Bank rate is 1 ${c} = ${s.cbu} so‘m (${s.diffText}). Banks buy the ${X.en.cur[c]} at ${s.minBuy}–${s.buy} so‘m and sell it at ${s.sell}–${s.maxSell} so‘m. On ${X.en.amount(c)} the gap between the best and the worst bank is ${s.gap} so‘m.`,
    q1: (c) => `Which bank pays the most for a ${X.en.cur[c]}?`, a1: (c, s) => `The highest buying rates today: ${s.topBuy}. For ${X.en.amount(c)}, ${s.bestBuy.bank} gives you ${s.buyTotal} so‘m.`,
    q2: (c) => `Where is it cheapest to buy ${c}?`, a2: (c, s) => `The lowest selling rates today: ${s.topSell}. Buying ${X.en.amount(c)} at ${s.bestSell.bank} costs ${s.sellTotal} so‘m.`,
    q3: "Why is the Central Bank rate different from the rate at a bank?", a3: "The Central Bank rate is the official reference rate used for accounting, customs and taxes. Banks exchange cash at their own buying and selling rates, so the rate you get at a bank is always slightly different.",
    other: "Other currencies", allBanks: "Rates by bank", gold: "Gold bar prices", home: "Rates", map: "Map", history: "History", wallet: "Wallet",
    bankTitle: (b, s) => `${b}: USD rate today — ${s} | Qulay Kurs`, bankH1: (b) => `${b}: exchange rates today`,
    bankDesc: (b, u) => `${b} buys the US dollar at ${u.buy} so‘m and sells it at ${u.sell} so‘m today. Euro and ruble rates and a comparison with other banks in Uzbekistan on Qulay Kurs.`,
    bankShort: (u) => `buys ${u.buy}, sells ${u.sell}`, colCur: "Currency", colBuy: "Bank buys at", colSell: "Bank sells at", colCbu: "Central Bank",
    updated: (d, t) => `as of ${d}, ${t}`, rankBuy: (c, r, n) => `Ranked ${r} of ${n} banks by ${X.en.cur[c]} buying rate.`,
    bankSell: (c, b, total, gap, best) => `If you sell ${X.en.amount(c)}, ${b} gives you ${total} so‘m` + (gap ? ` — ${gap} so‘m less than the best rate (${best}).` : ` — the best rate today.`),
    address: "Head office address", compare: "Compare with all banks", otherBanks: "Other banks",
    disclaimer: "Rates are for information only and come from data published by the banks and the Central Bank. Confirm the exact rate with the bank. Qulay Kurs is not affiliated with any bank.",
    goldTitle: "Gold bar prices today — Central Bank of Uzbekistan | Qulay Kurs", goldH1: "Central Bank gold bar prices",
    goldDesc: (g) => `Today 1 gram of gold (10 g bar) costs ${g.perGram} so‘m. Purchase and buy-back prices for 5, 10, 20, 50 and 100 gram bars.`,
    goldLead: (g) => `The Central Bank sells a 10 gram gold bar for ${g.sell10} so‘m and buys it back, packaging intact, for ${g.back10} so‘m. If you buy a bar and hand it straight back you lose ${g.loss}% of the price.`,
    goldCols: ["Bar", "You buy at", "Buy-back", "Per gram", "Spread"], gram: (n) => `${n} g`,
    notFound: "Page not found", notFoundText: "There is no such page. Today’s rates are on the home page.", toHome: "Go to the home page",
  },
};

// Texts of the all-currencies, amount and history pages. `u(c, n)` is the currency word in the form the number needs.
const u = (lang, c, n) => (lang === "uz" ? UNIT[c].uz : lang === "uzc" ? uzCyrillic(UNIT[c].uz) : lang === "ru" ? ruPlural(n, ...UNIT[c].ru) : UNIT[c].en[n === 1 ? 0 : 1]);
const Y = {
  uz: {
    allCur: "Barcha valyutalar — Markaziy bank kursi", amounts: "Ko‘p so‘raladigan summalar",
    allTitle: (n) => `Markaziy bank valyuta kurslari bugun — ${n} ta valyuta | Qulay Kurs`, allH1: "Markaziy bank valyuta kurslari",
    allDesc: (n, d, usd) => `O‘zbekiston Markaziy bankining ${d} sanasidagi rasmiy kurslari: ${n} ta valyuta. 1 dollar = ${usd} so‘m. Har bir valyuta uchun so‘mga hisoblash jadvali.`,
    allLead: (n, d) => `Markaziy bank ${d} sanasiga ${n} ta xorijiy valyutaning so‘mga nisbatan rasmiy kursini belgilagan. Bu kurs buxgalteriya, bojxona va soliq hisob-kitoblarida ishlatiladi; banklardagi naqd almashtirish kursi undan farq qiladi.`,
    allCols: ["Kod", "Valyuta", "Miqdor", "Kurs, so‘m", "O‘zgarish"],
    cTitle: (c, name) => `${name} (${c}) kursi bugun — so‘mda, Markaziy bank | Qulay Kurs`, cH1: (c, name) => `${name} kursi bugun`,
    cDesc: (c, name, r) => `Markaziy bank kursi: ${r.nominal} ${c} = ${r.rate} so‘m (${r.date}). ${c}dan so‘mga hisoblash jadvali.`,
    cLead: (c, name, r) => `Markaziy bank kursi bo‘yicha ${r.nominal} ${name} (${c}) = ${r.rate} so‘m. Kurs ${r.date}dan amal qiladi, oldingi kursdan farqi ${r.diff} so‘m. Bu rasmiy hisob-kitob kursi: banklar va ayirboshlash shoxobchalaridagi naqd kurs boshqacha bo‘lishi mumkin.`,
    toUzs: (c) => `${c} → so‘m`, fromUzs: (c) => `So‘m → ${c}`, som: "so‘m",
    amtLink: (n, c) => `${group(n)} ${u("uz", c, n)} necha so‘m`,
    amtTitle: (n, c) => `${group(n)} ${u("uz", c, n)} necha so‘m? Bugungi kurs — banklar va Markaziy bank | Qulay Kurs`, amtH1: (n, c) => `${group(n)} ${u("uz", c, n)} necha so‘m bo‘ladi?`,
    amtDesc: (n, c, a) => `Bugun ${group(n)} ${u("uz", c, n)} Markaziy bank kursi bo‘yicha ${a.cbu} so‘m. Bankka sotsangiz ${a.get} so‘mgacha olasiz (${a.buyBank}), sotib olish ${a.pay} so‘mdan (${a.sellBank}).`,
    amtLead: (n, c, a) => `Bugun ${group(n)} ${u("uz", c, n)} Markaziy bank kursi bo‘yicha ${a.cbu} so‘m bo‘ladi. ${group(n)} ${u("uz", c, n)}ni bankka sotsangiz, eng ko‘pi bilan ${a.get} so‘m olasiz — eng yuqori kurs ${a.buyBank}da. ${group(n)} ${u("uz", c, n)} sotib olish esa eng arzoni ${a.pay} so‘mga tushadi (${a.sellBank}).`,
    otherAmounts: "Boshqa summalar",
    amtTitleCbu: (n, c) => `${group(n)} ${u("uz", c, n)} necha so‘m? Markaziy bank kursi bugun | Qulay Kurs`,
    amtDescCbu: (n, c, a) => `Bugun ${group(n)} ${u("uz", c, n)} (${a.name}) = ${a.cbu} so‘m — Markaziy bank kursi, ${a.date}. 1 ${c} = ${a.rate} so‘m. Boshqa summalar jadvali va kalkulyator.`,
    amtLeadCbu: (n, c, a) => `Bugun ${group(n)} ${u("uz", c, n)} (${a.name}, ${c}) Markaziy bank kursi bo‘yicha ${a.cbu} so‘m bo‘ladi: 1 ${c} = ${a.rate} so‘m (${a.date}). Bu rasmiy kurs: banklar va ayirboshlash shoxobchalaridagi naqd kurs boshqacha bo‘lishi mumkin.`,
    revLink: (n, c) => `${group(n)} so‘m necha ${UNIT[c].uz}`, revH1: (n, c) => `${group(n)} so‘m necha ${UNIT[c].uz} bo‘ladi?`,
    revTitle: (n, c) => `${group(n)} so‘m necha ${UNIT[c].uz}? Bugungi kurs — banklar va Markaziy bank | Qulay Kurs`,
    revDesc: (n, c, a) => `Bugun ${group(n)} so‘m Markaziy bank kursi bo‘yicha ${a.cbu} ${UNIT[c].uz}. Bankda ${group(n)} so‘mga ${a.buyGet} ${UNIT[c].uz}gacha sotib olish mumkin (${a.sellBank}).`,
    revLead: (n, c, a) => `Bugun ${group(n)} so‘m Markaziy bank kursi bo‘yicha ${a.cbu} ${UNIT[c].uz} bo‘ladi. Bankdan ${UNIT[c].uz} sotib olsangiz, ${group(n)} so‘mga eng ko‘pi bilan ${a.buyGet} ${UNIT[c].uz} olasiz: eng arzon sotish kursi ${a.sellBank}da — ${a.sell} so‘m.`,
    revCols: (c) => ["Bank", "Sotish kursi", `Siz olasiz, ${c}`],
    amtCols: ["Bank", "Sotib olish kursi", "Siz olasiz", "Sotish kursi", "Siz to‘laysiz"],
    histLink: (c) => `${X.uz.Cur[c]} kursi tarixi`, histH1: (c) => `${X.uz.Cur[c]} kursi tarixi`,
    histTitle: (c, h) => `${X.uz.Cur[c]} kursi tarixi: ${h.y0}-yildan bugungacha, yillar bo‘yicha | Qulay Kurs`,
    histDesc: (c, h) => `Markaziy bankning ${X.uz.cur[c]} kursi ${h.y0}-yildan bugungacha: yillar bo‘yicha jadval va so‘nggi 30 kunlik kurs. Bugun 1 ${X.uz.cur[c]} = ${h.now} so‘m, bir yil oldin — ${h.yearAgo} so‘m.`,
    histLead: (c, h) => `Bugun Markaziy bank kursi — 1 ${X.uz.cur[c]} = ${h.now} so‘m. Bir yil oldin u ${h.yearAgo} so‘m edi: bir yilda kurs ${h.yearPct}% o‘zgardi. ${h.y0}-yil boshida 1 ${X.uz.cur[c]} ${h.first} so‘m turgan.`,
    yearsH: "Yillar bo‘yicha", yearCols: ["Yil", "Yil boshida", "Yil oxirida", "O‘zgarish"], ytd: "bugungacha",
    daysH: "So‘nggi 30 kun", dayCols: ["Sana", "Kurs, so‘m", "O‘zgarish"], chart: "Interaktiv grafik",
  },
  ru: {
    allCur: "Все валюты — курс ЦБ", amounts: "Популярные суммы",
    allTitle: (n) => `Курсы валют ЦБ Узбекистана на сегодня — ${n} ${ruPlural(n, "валюта", "валюты", "валют")} | Qulay Kurs`, allH1: "Курсы валют Центрального банка Узбекистана",
    allDesc: (n, d, usd) => `Официальные курсы Центрального банка Узбекистана на ${d}: ${n} ${ruPlural(n, "валюта", "валюты", "валют")}. 1 доллар = ${usd} сум. Для каждой валюты — таблица пересчёта в сумы.`,
    allLead: (n, d) => `Центральный банк установил на ${d} официальные курсы иностранных валют к суму — всего ${n}. Этот курс используют в бухгалтерии, на таможне и для налогов; наличный курс обмена в банках отличается.`,
    allCols: ["Код", "Валюта", "Единиц", "Курс, сум", "Изменение"],
    cTitle: (c, name) => `${name} (${c}): курс к суму на сегодня — ЦБ Узбекистана | Qulay Kurs`, cH1: (c, name) => `${name}: курс к суму на сегодня`,
    cDesc: (c, name, r) => `Курс ЦБ Узбекистана: ${r.nominal} ${c} = ${r.rate} сум (${r.date}). Таблица пересчёта ${c} в сумы.`,
    cLead: (c, name, r) => `По курсу Центрального банка ${r.nominal} ${c} (${name}) = ${r.rate} сум. Курс действует с ${r.date}, изменение к предыдущему — ${r.diff} сум. Это официальный расчётный курс: наличный курс в банках и обменных пунктах может отличаться.`,
    toUzs: (c) => `${c} → сум`, fromUzs: (c) => `Сум → ${c}`, som: "сум",
    amtLink: (n, c) => `${group(n)} ${u("ru", c, n)} в сумах`,
    amtTitle: (n, c) => `${group(n)} ${u("ru", c, n)} в сумах сегодня — по курсу банков и ЦБ | Qulay Kurs`, amtH1: (n, c) => `${group(n)} ${u("ru", c, n)} в сумах`,
    amtDesc: (n, c, a) => `Сегодня ${group(n)} ${u("ru", c, n)} — это ${a.cbu} сум по курсу ЦБ. При продаже банку — до ${a.get} сум (${a.buyBank}), покупка — от ${a.pay} сум (${a.sellBank}).`,
    amtLead: (n, c, a) => `Сегодня ${group(n)} ${u("ru", c, n)} по курсу Центрального банка — ${a.cbu} сум. Если продать ${group(n)} ${u("ru", c, n)} банку, вы получите максимум ${a.get} сум: самый высокий курс в ${a.buyBank}. Купить ${group(n)} ${u("ru", c, n)} дешевле всего в ${a.sellBank} — за ${a.pay} сум.`,
    otherAmounts: "Другие суммы",
    amtTitleCbu: (n, c) => `${group(n)} ${u("ru", c, n)} в сумах сегодня — курс ЦБ Узбекистана | Qulay Kurs`,
    amtDescCbu: (n, c, a) => `Сегодня ${group(n)} ${u("ru", c, n)} = ${a.cbu} сум по курсу ЦБ Узбекистана на ${a.date}. 1 ${c} = ${a.rate} сум. Таблица для других сумм и калькулятор.`,
    amtLeadCbu: (n, c, a) => `Сегодня ${group(n)} ${u("ru", c, n)} (${c}) по курсу Центрального банка Узбекистана — ${a.cbu} сум: 1 ${c} = ${a.rate} сум на ${a.date}. Это официальный курс: наличный курс в банках и обменных пунктах может отличаться.`,
    revLink: (n, c) => `${group(n)} сум в ${UNIT[c].ruIn}`, revH1: (n, c) => `${group(n)} сум в ${UNIT[c].ruIn}`,
    revTitle: (n, c) => `${group(n)} сум в ${UNIT[c].ruIn} сегодня — по курсу банков и ЦБ | Qulay Kurs`,
    revDesc: (n, c, a) => `Сегодня ${group(n)} сум — это ${a.cbu} ${c} по курсу ЦБ. В банке на ${group(n)} сум можно купить до ${a.buyGet} ${c} (${a.sellBank}).`,
    revLead: (n, c, a) => `Сегодня ${group(n)} сум по курсу Центрального банка — это ${a.cbu} ${c}. В банке на ${group(n)} сум можно купить максимум ${a.buyGet} ${c}: самый низкий курс продажи в ${a.sellBank} — ${a.sell} сум.`,
    revCols: (c) => ["Банк", "Курс продажи", `Вы получите, ${c}`],
    amtCols: ["Банк", "Курс покупки", "Вы получите", "Курс продажи", "Вы заплатите"],
    histLink: (c) => `История курса ${X.ru.cur[c]}`, histH1: (c) => `История курса ${X.ru.cur[c]} к суму`,
    histTitle: (c, h) => `История курса ${X.ru.cur[c]} к суму: по годам с ${h.y0} года | Qulay Kurs`,
    histDesc: (c, h) => `Курс ${X.ru.cur[c]} ЦБ Узбекистана с ${h.y0} года до сегодня: таблица по годам и курс за последние 30 дней. Сегодня 1 ${X.ru.acc[c]} = ${h.now} сум, год назад — ${h.yearAgo} сум.`,
    histLead: (c, h) => `Сегодня курс Центрального банка — 1 ${X.ru.acc[c]} = ${h.now} сум. Год назад он был ${h.yearAgo} сум: изменение за год — ${h.yearPct}%. В начале ${h.y0} года 1 ${X.ru.acc[c]} стоил ${h.first} сум.`,
    yearsH: "По годам", yearCols: ["Год", "В начале года", "В конце года", "Изменение"], ytd: "до сегодня",
    daysH: "Последние 30 дней", dayCols: ["Дата", "Курс, сум", "Изменение"], chart: "Интерактивный график",
  },
  en: {
    allCur: "All currencies — Central Bank rates", amounts: "Popular amounts",
    allTitle: (n) => `Central Bank of Uzbekistan exchange rates today — ${n} currencies | Qulay Kurs`, allH1: "Central Bank of Uzbekistan exchange rates",
    allDesc: (n, d, usd) => `Official Central Bank of Uzbekistan rates for ${d}: ${n} currencies. 1 US dollar = ${usd} so‘m. A conversion table to so‘m for every currency.`,
    allLead: (n, d) => `For ${d} the Central Bank has set official rates against the so‘m for ${n} foreign currencies. This rate is used for accounting, customs and taxes; the cash exchange rate at banks is different.`,
    allCols: ["Code", "Currency", "Units", "Rate, so‘m", "Change"],
    cTitle: (c, name) => `${c} to UZS today: ${name} rate — Central Bank of Uzbekistan | Qulay Kurs`, cH1: (c, name) => `${name} (${c}) to Uzbek so‘m today`,
    cDesc: (c, name, r) => `Central Bank of Uzbekistan rate: ${r.nominal} ${c} = ${r.rate} so‘m (${r.date}). Conversion table from ${c} to UZS.`,
    cLead: (c, name, r) => `At the Central Bank rate ${r.nominal} ${c} (${name}) = ${r.rate} so‘m. The rate applies from ${r.date}; the change from the previous rate is ${r.diff} so‘m. This is the official reference rate: cash rates at banks and exchange offices may differ.`,
    toUzs: (c) => `${c} → UZS`, fromUzs: (c) => `UZS → ${c}`, som: "so‘m",
    amtLink: (n, c) => `${group(n)} ${c} to UZS`,
    amtTitle: (n, c) => `${group(n)} ${c} to UZS today: ${group(n)} ${u("en", c, n)} in Uzbek so‘m | Qulay Kurs`, amtH1: (n, c) => `${group(n)} ${c} to UZS`,
    amtDesc: (n, c, a) => `Today ${group(n)} ${c} is ${a.cbu} so‘m at the Central Bank rate. Selling to a bank gets you up to ${a.get} so‘m (${a.buyBank}); buying costs from ${a.pay} so‘m (${a.sellBank}).`,
    amtLead: (n, c, a) => `Today ${group(n)} ${u("en", c, n)} is ${a.cbu} so‘m at the Central Bank rate. If you sell ${group(n)} ${c} to a bank, the most you get is ${a.get} so‘m — the highest rate is at ${a.buyBank}. Buying ${group(n)} ${c} costs at least ${a.pay} so‘m (${a.sellBank}).`,
    otherAmounts: "Other amounts",
    amtTitleCbu: (n, c) => `${group(n)} ${c} to UZS today: ${group(n)} ${u("en", c, n)} in Uzbek so‘m | Qulay Kurs`,
    amtDescCbu: (n, c, a) => `Today ${group(n)} ${c} is ${a.cbu} so‘m at the Central Bank of Uzbekistan rate for ${a.date}. 1 ${c} = ${a.rate} so‘m. A table for other amounts and a calculator.`,
    amtLeadCbu: (n, c, a) => `Today ${group(n)} ${u("en", c, n)} (${c}) is ${a.cbu} so‘m at the Central Bank of Uzbekistan rate: 1 ${c} = ${a.rate} so‘m as of ${a.date}. This is the official rate: cash rates at banks and exchange offices may differ.`,
    revLink: (n, c) => `${group(n)} UZS to ${c}`, revH1: (n, c) => `${group(n)} UZS to ${c}`,
    revTitle: (n, c) => `${group(n)} UZS to ${c} today: ${group(n)} Uzbek so‘m in ${UNIT[c].en[1]} | Qulay Kurs`,
    revDesc: (n, c, a) => `Today ${group(n)} so‘m is ${a.cbu} ${c} at the Central Bank rate. At a bank ${group(n)} so‘m buys up to ${a.buyGet} ${c} (${a.sellBank}).`,
    revLead: (n, c, a) => `Today ${group(n)} so‘m is ${a.cbu} ${c} at the Central Bank rate. At a bank, ${group(n)} so‘m buys at most ${a.buyGet} ${c}: the lowest selling rate is at ${a.sellBank}, ${a.sell} so‘m.`,
    revCols: (c) => ["Bank", "Selling rate", `You get, ${c}`],
    amtCols: ["Bank", "Buying rate", "You get", "Selling rate", "You pay"],
    histLink: (c) => `${c} rate history`, histH1: (c) => `${X.en.Cur[c]} to UZS rate history`,
    histTitle: (c, h) => `${c} to UZS history: ${X.en.cur[c]} rate by year since ${h.y0} | Qulay Kurs`,
    histDesc: (c, h) => `The Central Bank of Uzbekistan ${X.en.cur[c]} rate from ${h.y0} to today: a table by year and the last 30 days. Today 1 ${c} = ${h.now} so‘m; a year ago it was ${h.yearAgo} so‘m.`,
    histLead: (c, h) => `Today the Central Bank rate is 1 ${c} = ${h.now} so‘m. A year ago it was ${h.yearAgo} so‘m, a change of ${h.yearPct}% over the year. At the start of ${h.y0}, 1 ${c} cost ${h.first} so‘m.`,
    yearsH: "By year", yearCols: ["Year", "Start of year", "End of year", "Change"], ytd: "to date",
    daysH: "Last 30 days", dayCols: ["Date", "Rate, so‘m", "Change"], chart: "Interactive chart",
  },
};

// Uzbek in Cyrillic: the Uzbek texts, transliterated. Numbers, dates and names put into them are left as they are.
X.uzc = uzCyrillicTexts(X.uz);
Y.uzc = uzCyrillicTexts(Y.uz);

/* ---------- data ---------- */

async function get(path) {
  const res = await fetch(API + path, { signal: AbortSignal.timeout(30000) });
  if (!res.ok) throw new Error(`${path}: HTTP ${res.status}`);
  return res.json();
}
const rates = Object.fromEntries(await Promise.all(CURS.map(async (c) => [c, await get(`/api/rates/${c.toLowerCase()}`)])));
const gold = await get("/api/gold").catch((e) => (console.warn(`gold skipped: ${e.message}`), null));
const skip = (what) => (e) => (console.warn(`${what} skipped: ${e.message}`), null);
const cbuAll = await get("/api/cbu/all").then((d) => (d.currencies?.length > 10 ? { ...d, currencies: d.currencies.filter((c) => /^[A-Z]{3}$/.test(c.code) && c.rate > 0) } : null)).catch(skip("all currencies"));
const history = Object.fromEntries(await Promise.all(CURS.map(async (c) => [c, await get(`/api/history/${c.toLowerCase()}`).then((d) => (d.points?.length > 30 ? d.points : null)).catch(skip(`${c} history`))])));
// Banks publish cash rates for these too: their pages get a bank table when the API has it. Not part of the app's tabs.
for (const c of ["GBP", "KZT"]) {
  const d = await get(`/api/rates/${c.toLowerCase()}`).catch(skip(`${c} bank rates`));
  if (d?.cbu?.rate > 0 && d.banks?.some((b) => b.buy > 0 && b.sell > 0)) rates[c] = d;
}
const cbuOf = (code) => cbuAll?.currencies.find((c) => c.code === code);
// Central Bank rate of one unit, 0 when unknown.
const perUnit = (code) => rates[code]?.cbu.rate || (cbuOf(code) ? cbuOf(code).rate / cbuOf(code).nominal : 0);
for (const c of CURS) if (!(rates[c].banks?.length > 5)) throw new Error(`${c}: too few banks, refusing to build`);

// What the texts need about one currency, already formatted for `lang`.
function summary(cur, lang) {
  const data = rates[cur], banks = data.banks.filter((b) => b.buy > 0);
  const byBuy = [...banks].sort((a, b) => b.buy - a.buy), bySell = banks.filter((b) => b.sell > 0).sort((a, b) => a.sell - b.sell);
  const top = (list, key) => list.slice(0, 3).map((b) => `${b.bank} — ${rate(b[key], lang)}`).join(", ");
  const n = AMOUNT[cur];
  return {
    count: banks.length, byBuy, bySell, bestBuy: byBuy[0], bestSell: bySell[0],
    buy: rate(byBuy[0].buy, lang), minBuy: rate(byBuy.at(-1).buy, lang), sell: rate(bySell[0].sell, lang), maxSell: rate(bySell.at(-1).sell, lang),
    cbu: rate(data.cbu.rate, lang), diffText: X[lang].diffText(data.cbu.diff || 0), gap: group((byBuy[0].buy - byBuy.at(-1).buy) * n),
    buyTotal: group(byBuy[0].buy * n), sellTotal: group(bySell[0].sell * n), topBuy: top(byBuy, "buy"), topSell: top(bySell, "sell"),
  };
}

/* ---------- parts of a page ---------- */

const alternates = (urlOf) => LANGS.map((l) => `<link rel="alternate" hreflang="${HTML_LANG[l]}" href="${ORIGIN}${urlOf(l)}">`).join("\n") + `\n<link rel="alternate" hreflang="x-default" href="${ORIGIN}${urlOf("uz")}">`;
const ld = (obj) => `<script type="application/ld+json">${JSON.stringify(obj).replace(/</g, "\\u003c")}</script>`;
const ORG = { "@type": "Organization", "@id": `${ORIGIN}/#org`, name: "Qulay Kurs", url: `${ORIGIN}/`, logo: `${ORIGIN}/icon-512.png`, sameAs: ["https://t.me/qulay_kurs"] };
const crumbs = (items) => ({ "@type": "BreadcrumbList", itemListElement: items.map(([name, url], i) => ({ "@type": "ListItem", position: i + 1, name, item: ORIGIN + url })) });

function head({ lang, url, urlOf, title, desc, graph }) {
  return `<title>${esc(fit(title))}</title>
<meta name="description" content="${esc(desc)}">
<meta name="robots" content="index, follow, max-image-preview:large">
<meta name="theme-color" content="#FBF6E9">
<link rel="canonical" href="${ORIGIN}${url}">
${alternates(urlOf)}
<meta property="og:site_name" content="Qulay Kurs">
<meta property="og:title" content="${esc(title.replace(/ \| Qulay Kurs$/, ""))}">
<meta property="og:description" content="${esc(desc)}">
<meta property="og:type" content="website">
<meta property="og:url" content="${ORIGIN}${url}">
<meta property="og:image" content="${ORIGIN}/og-${lang === "uzc" ? "uz" : lang}.png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:locale" content="${{ uz: "uz_UZ", uzc: "uz_UZ", ru: "ru_RU", en: "en_US" }[lang]}">
<meta name="twitter:card" content="summary_large_image">${VERIFY ? "\n" + VERIFY : ""}
${ld({ "@context": "https://schema.org", "@graph": [ORG, { "@type": "WebSite", "@id": `${ORIGIN}/#site`, name: "Qulay Kurs", alternateName: ["QulayKurs", "qulaykurs.uz"], url: `${ORIGIN}/`, publisher: { "@id": `${ORIGIN}/#org` }, inLanguage: Object.values(HTML_LANG) }, ...graph] })}`;
}

const badge = (name, extra = "") => {
  const slug = logoSlug(name);
  return `<span class="badge${extra}">${slug ? `<img src="/bank-logos/${slug}.png" alt="" width="40" height="40" loading="lazy">` : esc(name.slice(0, 2).toUpperCase())}</span>`;
};

// Links every page carries at the bottom: the other currencies, gold, and each bank's page.
function linkBlock(lang, bankNames, skipCur) {
  const x = X[lang], y = Y[lang], link = (url, text) => `<a href="${url}">${esc(text)}</a>`;
  const curs = CURS.filter((c) => c !== skipCur).map((c) => link(curUrl(lang, c), T[lang].title[c]));
  if (gold) curs.push(link(goldUrl(lang), x.gold));
  if (cbuAll) curs.push(link(allUrl(lang), y.allCur));
  for (const c of CURS) if (history[c]) curs.push(link(histUrl(lang, c), y.histLink(c)));
  const of = AMOUNTS[skipCur] && perUnit(skipCur) ? skipCur : "USD";
  const amounts = AMOUNTS[of].map((n) => link(amountUrl(lang, of, n), y.amtLink(n, of)));
  for (const [c, n] of Object.entries(FEATURED)) if (c !== of && perUnit(c)) amounts.push(link(amountUrl(lang, c, n), y.amtLink(n, c)));
  const back = CURS.includes(of) ? REV_AMOUNTS.map((n) => link(revUrl(lang, of, n), y.revLink(n, of))) : [];
  return `<h3>${esc(y.amounts)}</h3><p class="seo-links">${amounts.join("")}</p>${back.length ? `<p class="seo-links">${back.join("")}</p>` : ""}
  <h3>${esc(x.other)}</h3><p class="seo-links">${curs.join("")}</p>
  <h3>${esc(x.allBanks)}</h3><p class="seo-links">${bankNames.map((b) => link(bankUrl(lang, bankSlug(b)), b)).join("")}</p>
  <p class="seo-note">${esc(x.disclaimer)}</p>`;
}
const langLinks = (lang, urlOf) => LANGS.map((l) => `<a href="${urlOf(l)}" hreflang="${HTML_LANG[l]}"${l === lang ? ' class="on" aria-current="true"' : ""}>${LANG_LABEL[l]}</a>`).join("");

/* ---------- currency pages: the app, pre-filled ---------- */

const template = await readFile(join(PUBLIC, "index.html"), "utf8");
const FONT_LATIN = '<link rel="preload" href="/fonts/onest-latin.woff2" as="font" type="font/woff2" crossorigin>';
const FONT_CYRILLIC = FONT_LATIN.replace("latin", "cyrillic");
// Ў is in the basic Cyrillic file; қ, ғ and ҳ are in the extended one.
const cyrillicFonts = (lang) => FONT_CYRILLIC + (lang === "uzc" ? "\n" + FONT_LATIN.replace("latin", "cyrillic-ext") : "");
if (!template.includes(FONT_LATIN)) throw new Error("template marker not found: font preload");
function cut(html, from, to, replacement) {
  const a = html.indexOf(from), b = html.indexOf(to, a + from.length);
  if (a < 0 || b < 0) throw new Error(`template marker not found: ${from} … ${to}`);
  return html.slice(0, a) + replacement + html.slice(b + to.length);
}
const pageMap = {
  urls: Object.fromEntries(LANGS.map((l) => [l, Object.fromEntries(CURS.map((c) => [c, curUrl(l, c)]))])),
  titles: Object.fromEntries(LANGS.map((l) => [l, Object.fromEntries(CURS.map((c) => [c, fit(X[l].title(c))]))])),
};

function currencyPage(lang, cur) {
  const x = X[lang], t = T[lang], s = summary(cur, lang), data = rates[cur], url = curUrl(lang, cur), at = when(data.updatedAt, lang);
  const faq = [[x.q1(cur), x.a1(cur, s)], [x.q2(cur), x.a2(cur, s)], [x.q3, x.a3]];
  const graph = [{ "@type": "FAQPage", inLanguage: HTML_LANG[lang], dateModified: data.updatedAt, mainEntity: faq.map(([q, a]) => ({ "@type": "Question", name: q, acceptedAnswer: { "@type": "Answer", text: a } })) }];
  if (cur !== "USD") graph.push(crumbs([["Qulay Kurs", curUrl(lang, "USD")], [t.title[cur], url]]));

  // Same order and markup as renderTable() in app.js for "I sell, 100": the app repaints this on load without a jump.
  const rows = s.byBuy.map((b) => {
    const diff = (s.bestBuy.buy - b.buy) * 100, best = diff < 0.5;
    return `<tr data-bank="${esc(b.bank)}"${best ? ' class="top1"' : ""}>
      <td class="c-bank"><span class="bn">${badge(b.bank, best ? " accent" : "")}<span><b>${esc(b.bank)}</b><small>${esc(b.branch?.address || "")}</small></span></span></td>
      <td class="num c-buy${b.buy === s.bestBuy.buy ? " hi" : ""}">${rate(b.buy, lang)}</td>
      <td class="num c-sell${b.sell === s.bestSell.sell ? " hi" : ""}">${b.sell ? rate(b.sell, lang) : "—"}</td>
      <td class="num c-get get"><b>${group(b.buy * 100)}</b><small${best ? ' class="best"' : ""}>${best ? esc(t.best) : `−${group(diff)}`}</small></td>
    </tr>`;
  }).join("\n");

  let html = template.replace('<html lang="uz">', `<html lang="${HTML_LANG[lang]}">`);
  html = cut(html, "<title>", '<link rel="sitemap" type="application/xml" href="https://qulaykurs.uz/sitemap.xml">', head({ lang, url, urlOf: (l) => curUrl(l, cur), title: x.title(cur), desc: x.desc(cur, s), graph }));
  html = cut(html, '<script type="application/ld+json">', "</script>", "");
  html = html.replace(/(data-i18n="([\w.]+)"[^>]*>)[^<]*</g, (m, open, key) => (typeof t[key] === "string" ? `${open}${esc(t[key])}<` : m));
  // Texts the app writes on load; without them a page read without JavaScript keeps the template's Uzbek words.
  const fill = { amountUnit: t.unit[cur](100), resLabel: t.getSell, colGet: t.getSell, nearLabel: t.near, tLabel: t["hist.cbu"], acTo: t["all.som"], total: group(s.bestBuy.buy * 100), ...(cbuAll ? { allCurSub: t.allSub(cbuAll.currencies.length) } : {}) };
  for (const [id, text] of Object.entries(fill)) {
    const place = new RegExp(`( id="${id}"[^>]*>)[^<]*<`);
    if (!place.test(html)) throw new Error(`template marker not found: #${id}`);
    html = html.replace(place, (m, open) => `${open}${esc(text)}<`);
  }
  html = cut(html, 'id="search" type="search" autocomplete="off" placeholder="', '"', `id="search" type="search" autocomplete="off" placeholder="${esc(t.search)}"`);
  html = cut(html, '<h1 id="tblTitle">', "</h1>", `<h1 id="tblTitle">${esc(t.title[cur])}</h1>`);
  html = cut(html, '<p class="muted" id="tblSub">', "</p>", `<p class="muted" id="tblSub">${esc(x.sub(s.count, at.time))}</p>`);
  html = cut(html, '<tbody id="tbody">', "</tbody>", `<tbody id="tbody">\n${rows}\n</tbody>`);
  html = cut(html, '<section class="wrap seo-copy"', "</section>", `<section class="wrap seo-copy" aria-labelledby="seo-title">
  <h2 id="seo-title">${esc(x.h2(cur, at.date))}</h2>
  <p>${esc(x.lead(cur, s))}</p>
  ${faq.map(([q, a]) => `<h3>${esc(q)}</h3><p>${esc(a)}</p>`).join("\n  ")}
  ${linkBlock(lang, s.byBuy.map((b) => b.bank), cur)}
  <p class="seo-langs">${langLinks(lang, (l) => curUrl(l, cur))}</p>
</section>`);
  if (isCyrillic(lang)) html = html.replace(FONT_LATIN, FONT_LATIN + "\n" + cyrillicFonts(lang));
  html = html.replace(/\?v=\w+/g, `?v=${VERSION}`).replace('<link rel="stylesheet" href="/style.css', '<link rel="stylesheet" href="/pages.css?v=' + VERSION + '">\n<link rel="stylesheet" href="/style.css');
  return html.replace('<script src="/shared.js', `<script>window.QK=${JSON.stringify({ lang, cur, ...pageMap })}</script>\n<script src="/shared.js`);
}

/* ---------- plain pages: banks, gold, 404 ---------- */

function shell({ lang, url, urlOf, title, desc, graph, body, noindex, calc }) {
  const x = X[lang], home = curUrl(lang, "USD");
  return `<!doctype html>
<html lang="${HTML_LANG[lang]}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
${noindex ? `<title>${esc(title)}</title>\n<meta name="robots" content="noindex">\n<meta name="theme-color" content="#FBF6E9">` : head({ lang, url, urlOf, title, desc, graph })}
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="manifest" href="/manifest.webmanifest">
${FONT_LATIN}${isCyrillic(lang) ? "\n" + cyrillicFonts(lang) : ""}
<script>try{const t=localStorage.getItem("theme");document.documentElement.dataset.theme=t||(matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light")}catch{document.documentElement.dataset.theme="light"}</script>
<link rel="stylesheet" href="/style.css?v=${VERSION}">
<link rel="stylesheet" href="/pages.css?v=${VERSION}">
</head>
<body class="page">
<header class="top">
  <div class="wrap top-in">
    <a class="logo" href="${home}" aria-label="Qulay Kurs"><img src="/logo.svg" width="32" height="32" alt=""><span>Qulay <b class="logo-k">Kurs</b></span></a>
    <nav class="tabs-nav" aria-label="Menu"><a href="${home}">${esc(x.home)}</a><a href="${home}#/xarita">${esc(x.map)}</a><a href="${home}#/tarix">${esc(x.history)}</a><a href="${home}#/hamyon">${esc(x.wallet)}</a></nav>
    <div class="top-r">${urlOf ? `<div class="langs">${langLinks(lang, urlOf)}</div>` : ""}</div>
  </div>
</header>
<main class="wrap page-main">
${body}
</main>
<footer class="foot"><div class="wrap foot-in"><span>© ${new Date().getUTCFullYear()} Qulay Kurs</span><a class="foot-tg" href="https://t.me/qulay_kurs" rel="noopener">Telegram @qulay_kurs</a><a href="/privacy">Privacy</a></div></footer>${calc ? `\n<script src="/calc.js?v=${VERSION}" defer></script>` : ""}
</body>
</html>
`;
}

function bankPage(lang, name, slug, allNames) {
  const x = X[lang], t = T[lang], url = bankUrl(lang, slug), home = curUrl(lang, "USD");
  const rowsData = CURS.map((cur) => ({ cur, s: summary(cur, lang), b: rates[cur].banks.find((b) => bankSlug(b.bank) === slug) })).filter((r) => r.b?.buy > 0);
  const usd = rowsData.find((r) => r.cur === "USD") || rowsData[0];
  const u = { buy: rate(usd.b.buy, lang), sell: usd.b.sell ? rate(usd.b.sell, lang) : "—" };
  const at = when(rates[usd.cur].updatedAt, lang);
  const rank = usd.s.byBuy.filter((b) => b.buy > usd.b.buy).length + 1;
  const gap = (usd.s.bestBuy.buy - usd.b.buy) * AMOUNT[usd.cur];
  const address = usd.b.branch?.address;
  const body = `<nav class="crumbs" aria-label="Breadcrumb"><a href="${home}">Qulay Kurs</a><span>›</span><span>${esc(name)}</span></nav>
<div class="card page-card">
  <div class="page-head">${badge(name)}<div><h1>${esc(x.bankH1(name))}</h1><p class="muted">${esc(x.updated(at.date, at.time))}</p></div></div>
  <div class="tbl-scroll"><table class="tbl page-tbl">
    <thead><tr><th>${esc(x.colCur)}</th><th class="num">${esc(x.colBuy)}</th><th class="num">${esc(x.colSell)}</th><th class="num">${esc(x.colCbu)}</th></tr></thead>
    <tbody>${rowsData.map(({ cur, s, b }) => `<tr><td><a href="${curUrl(lang, cur)}"><b>${cur}</b> <span class="muted">${esc(x.Cur[cur])}</span></a></td><td class="num${b.buy === s.bestBuy.buy ? " hi" : ""}">${rate(b.buy, lang)}</td><td class="num${b.sell && b.sell === s.bestSell.sell ? " hi" : ""}">${b.sell ? rate(b.sell, lang) : "—"}</td><td class="num muted">${s.cbu}</td></tr>`).join("")}</tbody>
  </table></div>
  <div class="page-text">
    <p>${esc(x.bankSell(usd.cur, name, group(usd.b.buy * AMOUNT[usd.cur]), gap > 0 ? group(gap) : "", usd.s.bestBuy.bank))} ${esc(x.rankBuy(usd.cur, rank, usd.s.count))}</p>
    ${address ? `<p><span class="muted">${esc(x.address)}:</span> ${esc(address)}</p>` : ""}
    <p><a class="btn btn-accent" href="${home}">${esc(x.compare)}</a></p>
  </div>
</div>
<section class="seo-copy">${linkBlock(lang, allNames.filter((b) => b !== name), null)}</section>`;
  return shell({
    lang, url, urlOf: (l) => bankUrl(l, slug), title: x.bankTitle(name, x.bankShort(u)), desc: x.bankDesc(name, u), body,
    graph: [crumbs([["Qulay Kurs", home], [name, url]]), { "@type": "WebPage", name: x.bankH1(name), url: ORIGIN + url, inLanguage: HTML_LANG[lang], dateModified: rates[usd.cur].updatedAt, about: { "@type": "BankOrCreditUnion", name, ...(address ? { address } : {}) } }],
  });
}

function goldPage(lang, allNames) {
  const x = X[lang], url = goldUrl(lang), home = curUrl(lang, "USD"), ten = gold.bars.find((b) => b.grams === 10) || gold.bars[0];
  const g = { perGram: group(ten.sell / ten.grams), sell10: group(ten.sell), back10: group(ten.buyback), loss: (((ten.sell - ten.buyback) / ten.sell) * 100).toFixed(1).replace(".", lang === "en" ? "." : ",") };
  const body = `<nav class="crumbs" aria-label="Breadcrumb"><a href="${home}">Qulay Kurs</a><span>›</span><span>${esc(x.gold)}</span></nav>
<div class="card page-card">
  <div class="page-head"><div><h1>${esc(x.goldH1)}</h1><p class="muted">cbu.uz · ${esc(cbuDate(gold.date, lang))}, ${esc(gold.time)}</p></div></div>
  <div class="tbl-scroll"><table class="tbl page-tbl">
    <thead><tr>${x.goldCols.map((c, i) => `<th${i ? ' class="num"' : ""}>${esc(c)}</th>`).join("")}</tr></thead>
    <tbody>${gold.bars.map((b) => `<tr><td><b>${esc(x.gram(b.grams))}</b></td><td class="num">${group(b.sell)}</td><td class="num">${group(b.buyback)}</td><td class="num">${group(b.sell / b.grams)}</td><td class="num muted">−${(((b.sell - b.buyback) / b.sell) * 100).toFixed(1)}%</td></tr>`).join("")}</tbody>
  </table></div>
  <div class="page-text"><p>${esc(x.goldLead(g))}</p><p><a class="btn btn-accent" href="${home}#/tarix">${esc(x.history)}</a></p></div>
</div>
<section class="seo-copy">${linkBlock(lang, allNames, null)}</section>`;
  return shell({ lang, url, urlOf: goldUrl, title: x.goldTitle, desc: x.goldDesc(g), body, graph: [crumbs([["Qulay Kurs", home], [x.gold, url]])] });
}

/* ---------- plain pages: every Central Bank currency ---------- */

// `rows` is an array of arrays of ready HTML cells; the first column is text, the rest are numbers.
const table = (cols, rows) => `<div class="tbl-scroll"><table class="tbl page-tbl">
    <thead><tr>${cols.map((c, i) => `<th${i ? ' class="num"' : ""}>${esc(c)}</th>`).join("")}</tr></thead>
    <tbody>${rows.map((r) => `<tr>${r.map((c, i) => `<td${i ? ' class="num"' : ""}>${c}</td>`).join("")}</tr>`).join("")}</tbody>
  </table></div>`;
const crumbNav = (lang, ...items) => `<nav class="crumbs" aria-label="Breadcrumb"><a href="${curUrl(lang, "USD")}">Qulay Kurs</a>${items.map(([name, url]) => `<span>›</span>${url ? `<a href="${url}">${esc(name)}</a>` : `<span>${esc(name)}</span>`}`).join("")}</nav>`;
const cbuName = (c, lang) => c.names?.[lang] || c.names?.en || c.code;

function allPage(lang, bankNames) {
  const y = Y[lang], url = allUrl(lang), list = cbuAll.currencies, date = cbuDate(list[0].date, lang);
  const usd = list.find((c) => c.code === "USD");
  const body = `${crumbNav(lang, [y.allH1])}
<div class="card page-card">
  <div class="page-head"><div><h1>${esc(y.allH1)}</h1><p class="muted">cbu.uz · ${esc(date)}</p></div></div>
  ${table(y.allCols, list.map((c) => [`<a href="${cbuUrl(lang, c.code)}"><b>${c.code}</b></a>`, `<a href="${cbuUrl(lang, c.code)}">${esc(cbuName(c, lang))}</a>`, c.nominal, rate(c.rate, lang), `<span class="muted">${delta(c.diff || 0, lang)}</span>`]))}
  <div class="page-text"><p>${esc(y.allLead(list.length, date))}</p><p><a class="btn btn-accent" href="${curUrl(lang, "USD")}">${esc(X[lang].compare)}</a></p></div>
</div>
<section class="seo-copy">${linkBlock(lang, bankNames, null)}</section>`;
  return shell({ lang, url, urlOf: allUrl, title: y.allTitle(list.length), desc: y.allDesc(list.length, date, usd ? rate(usd.rate, lang) : "—"), body, graph: [crumbs([["Qulay Kurs", curUrl(lang, "USD")], [y.allH1, url]])] });
}

function cbuPage(lang, c, bankNames) {
  const y = Y[lang], x = X[lang], url = cbuUrl(lang, c.code), name = cbuName(c, lang), per = c.rate / c.nominal, s = rates[c.code] ? summary(c.code, lang) : null;
  const r = { nominal: c.nominal, rate: rate(c.rate, lang), date: cbuDate(c.date, lang), diff: delta(c.diff || 0, lang) };
  const body = `${crumbNav(lang, [y.allH1, allUrl(lang)], [c.code])}
<div class="card page-card">
  <div class="page-head"><span class="badge">${c.code}</span><div><h1>${esc(y.cH1(c.code, name))}</h1><p class="muted">cbu.uz · ${esc(r.date)}</p></div></div>
  <div class="page-text"><p>${esc(y.cLead(c.code, name, r))}</p></div>
  ${table([y.toUzs(c.code), y.som], [1, 10, 100, 1000, 10000].map((n) => [`<b>${group(n)}</b> ${c.code}`, num(per * n, lang)]))}
  ${c.rate < 1 ? "" /* too few digits in the published rate to divide by */ : table([y.fromUzs(c.code), c.code], [10000, 100000, 1000000, 10000000].map((n) => [`<b>${group(n)}</b> ${esc(y.som)}`, num(n / per, lang)]))}
  ${s ? `<h2 class="page-h2">${esc(T[lang].abTitle(s.count))}</h2>
  ${table([T[lang]["col.bank"], x.colBuy, x.colSell], s.byBuy.map((b) => [bankLink(lang, b.bank), `<span${b.buy === s.bestBuy.buy ? ' class="hi"' : ""}>${rate(b.buy, lang)}</span>`, b.sell ? `<span${b.sell === s.bestSell.sell ? ' class="hi"' : ""}>${rate(b.sell, lang)}</span>` : "—"]))}` : ""}
</div>
<section class="seo-copy">
  <h3>${esc(y.allCur)}</h3><p class="seo-links">${cbuAll.currencies.filter((o) => o.code !== c.code).map((o) => `<a href="${cbuUrl(lang, o.code)}" title="${esc(cbuName(o, lang))}">${o.code}</a>`).join("")}</p>
  ${linkBlock(lang, bankNames, c.code)}
</section>`;
  return shell({
    lang, url, urlOf: (l) => cbuUrl(l, c.code), title: y.cTitle(c.code, name), desc: y.cDesc(c.code, name, r), body,
    graph: [crumbs([["Qulay Kurs", curUrl(lang, "USD")], [y.allH1, allUrl(lang)], [c.code, url]])],
  });
}

/* ---------- plain pages: one amount, in so‘m and back ---------- */

// The amount on these pages can be retyped: public/calc.js recalculates every number marked data-mul / data-div.
const calcBox = (n, label, lang) => `<input class="calc-in" style="width:${group(n).length + 3}ch" data-calc data-sep="${lang === "en" ? "." : ","}" inputmode="decimal" autocomplete="off" value="${group(n)}" aria-label="${esc(label)}">`;
const bankLink = (lang, name) => (bySlug.has(bankSlug(name)) ? `<a href="${bankUrl(lang, bankSlug(name))}"><b>${esc(name)}</b></a>` : `<b>${esc(name)}</b>`);

function amountPage(lang, cur, n, bankNames) {
  const y = Y[lang], x = X[lang], per = perUnit(cur), main = CURS.includes(cur), c = cbuOf(cur), url = amountUrl(lang, cur, n);
  const s = rates[cur] ? summary(cur, lang) : null, at = s && when(rates[cur].updatedAt, lang), name = c ? cbuName(c, lang) : cur;
  const a = {
    cbu: num(per * n, lang), rate: num(per, lang), name, date: cbuDate(rates[cur]?.cbu.date || c?.date, lang),
    ...(s ? { get: num(s.bestBuy.buy * n, lang), pay: num(s.bestSell.sell * n, lang), buyBank: s.bestBuy.bank, sellBank: s.bestSell.bank } : {}),
  };
  const parent = main ? [T[lang].title[cur], curUrl(lang, cur)] : [cur, cbuUrl(lang, cur)], trail = main || !cbuAll ? [parent] : [[y.allH1, allUrl(lang)], parent];
  const bankRows = s && s.byBuy.map((b) => [
    bankLink(lang, b.bank), `<span class="muted">${rate(b.buy, lang)}</span>`, `<span data-mul="${b.buy}"${b.buy === s.bestBuy.buy ? ' class="hi"' : ""}>${num(b.buy * n, lang)}</span>`,
    `<span class="muted">${b.sell ? rate(b.sell, lang) : "—"}</span>`, b.sell ? `<span data-mul="${b.sell}"${b.sell === s.bestSell.sell ? ' class="hi"' : ""}>${num(b.sell * n, lang)}</span>` : "—",
  ]);
  const others = AMOUNTS[cur].filter((m) => m !== n).map((m) => [`<a href="${amountUrl(lang, cur, m)}"><b>${group(m)}</b> ${esc(u(lang, cur, m))}</a>`, num(per * m, lang)]);
  const body = `${crumbNav(lang, ...trail, [`${group(n)} ${cur}`])}
<div class="card page-card">
  <div class="page-head"><div><h1>${esc(y.amtH1(n, cur))}</h1><p class="muted">${esc(s ? x.updated(at.date, at.time) : `cbu.uz · ${a.date}`)}</p></div></div>
  <div class="page-text"><p class="page-big">${calcBox(n, cur, lang)} <span>${cur}</span> = <b data-mul="${per}">${a.cbu}</b> ${esc(y.som)} <span class="muted">· ${esc(x.colCbu)}</span></p><p>${esc(s ? y.amtLead(n, cur, a) : y.amtLeadCbu(n, cur, a))}</p></div>
  ${s ? table(y.amtCols, bankRows) : ""}
  <h2 class="page-h2">${esc(y.otherAmounts)}</h2>
  ${table([y.toUzs(cur), y.som], others)}
  <div class="page-text"><p><a class="btn btn-accent" href="${parent[1]}">${esc(main ? x.compare : y.cH1(cur, name))}</a></p></div>
</div>
<section class="seo-copy">${linkBlock(lang, bankNames, cur)}</section>`;
  return shell({
    lang, url, urlOf: (l) => amountUrl(l, cur, n), title: s ? y.amtTitle(n, cur) : y.amtTitleCbu(n, cur), desc: s ? y.amtDesc(n, cur, a) : y.amtDescCbu(n, cur, a), body, calc: true,
    graph: [crumbs([["Qulay Kurs", curUrl(lang, "USD")], ...trail, [`${group(n)} ${cur}`, url]])],
  });
}

function reversePage(lang, cur, n, bankNames) {
  const y = Y[lang], x = X[lang], s = summary(cur, lang), per = perUnit(cur), url = revUrl(lang, cur, n), at = when(rates[cur].updatedAt, lang);
  const a = { cbu: num(n / per, lang), buyGet: num(n / s.bestSell.sell, lang), sellBank: s.bestSell.bank, sell: s.sell };
  const parent = [T[lang].title[cur], curUrl(lang, cur)], label = `${group(n)} UZS`;
  const rows = s.bySell.map((b) => [bankLink(lang, b.bank), `<span class="muted">${rate(b.sell, lang)}</span>`, `<span data-div="${b.sell}"${b.sell === s.bestSell.sell ? ' class="hi"' : ""}>${num(n / b.sell, lang)}</span>`]);
  const others = REV_AMOUNTS.filter((m) => m !== n).map((m) => [`<a href="${revUrl(lang, cur, m)}"><b>${group(m)}</b> ${esc(y.som)}</a>`, num(m / per, lang)]);
  const body = `${crumbNav(lang, parent, [label])}
<div class="card page-card">
  <div class="page-head"><div><h1>${esc(y.revH1(n, cur))}</h1><p class="muted">${esc(x.updated(at.date, at.time))}</p></div></div>
  <div class="page-text"><p class="page-big">${calcBox(n, y.som, lang)} <span>${esc(y.som)}</span> = <b data-div="${per}">${a.cbu}</b> ${cur} <span class="muted">· ${esc(x.colCbu)}</span></p><p>${esc(y.revLead(n, cur, a))}</p></div>
  ${table(y.revCols(cur), rows)}
  <h2 class="page-h2">${esc(y.otherAmounts)}</h2>
  ${table([y.fromUzs(cur), cur], others)}
  <div class="page-text"><p><a class="btn btn-accent" href="${parent[1]}">${esc(x.compare)}</a></p></div>
</div>
<section class="seo-copy">${linkBlock(lang, bankNames, cur)}</section>`;
  return shell({
    lang, url, urlOf: (l) => revUrl(l, cur, n), title: y.revTitle(n, cur), desc: y.revDesc(n, cur, a), body, calc: true,
    graph: [crumbs([["Qulay Kurs", curUrl(lang, "USD")], parent, [label, url]])],
  });
}

/* ---------- plain pages: Central Bank rate history ---------- */

function historyPage(lang, cur, bankNames) {
  const y = Y[lang], t = T[lang], url = histUrl(lang, cur), points = history[cur], last = points.at(-1), first = points[0];
  const yearBack = `${Number(last.date.slice(0, 4)) - 1}${last.date.slice(4)}`, ago = points.findLast((p) => p.date <= yearBack) || first;
  const h = { y0: first.date.slice(0, 4), now: rate(last.rate, lang), yearAgo: rate(ago.rate, lang), yearPct: pct(last.rate / ago.rate - 1, lang), first: rate(first.rate, lang) };
  const years = Object.entries(Object.groupBy(points, (p) => p.date.slice(0, 4))).sort(([a], [b]) => b - a);
  const yearRows = years.map(([year, list]) => [`<b>${year}</b>${year === last.date.slice(0, 4) ? ` <span class="muted">(${esc(y.ytd)})</span>` : ""}`, rate(list[0].rate, lang), rate(list.at(-1).rate, lang), `<span class="muted">${pct(list.at(-1).rate / list[0].rate - 1, lang)}%</span>`]);
  const recent = points.slice(-31), dayRows = recent.slice(1).map((p, i) => [dayLabel(p.date, lang), rate(p.rate, lang), `<span class="muted">${delta(p.rate - recent[i].rate, lang)}</span>`]).reverse();
  const body = `${crumbNav(lang, [t.title[cur], curUrl(lang, cur)], [y.histH1(cur)])}
<div class="card page-card">
  <div class="page-head"><div><h1>${esc(y.histH1(cur))}</h1><p class="muted">cbu.uz · ${esc(dayLabel(last.date, lang))}</p></div></div>
  <div class="page-text"><p>${esc(y.histLead(cur, h))}</p><p><a class="btn btn-accent" href="${curUrl(lang, cur)}#/tarix">${esc(y.chart)}</a></p></div>
  <h2 class="page-h2">${esc(y.daysH)}</h2>
  ${table(y.dayCols, dayRows)}
  <h2 class="page-h2">${esc(y.yearsH)}</h2>
  ${table(y.yearCols, yearRows)}
</div>
<section class="seo-copy">${linkBlock(lang, bankNames, cur)}</section>`;
  return shell({
    lang, url, urlOf: (l) => histUrl(l, cur), title: y.histTitle(cur, h), desc: y.histDesc(cur, h), body,
    graph: [crumbs([["Qulay Kurs", curUrl(lang, "USD")], [t.title[cur], curUrl(lang, cur)], [y.histH1(cur), url]])],
  });
}

/* ---------- write everything ---------- */

await rm(DIST, { recursive: true, force: true });
await cp(PUBLIC, DIST, { recursive: true });
const pages = []; // [url, lastmod, alternates(l) => url, html]
async function page(url, html, lastmod, urlOf) {
  const file = join(DIST, fileOf(url));
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, html);
  pages.push([url, lastmod, urlOf, html]);
}

const bankNames = [...new Set(CURS.flatMap((c) => summary(c, "uz").byBuy.map((b) => b.bank)))];
const bySlug = new Map(bankNames.map((n) => [bankSlug(n), n])); // one page per slug, first spelling wins
for (const lang of LANGS) {
  for (const cur of CURS) await page(curUrl(lang, cur), currencyPage(lang, cur), rates[cur].updatedAt, (l) => curUrl(l, cur));
  for (const [slug, name] of bySlug) await page(bankUrl(lang, slug), bankPage(lang, name, slug, [...bySlug.values()]), rates.USD.updatedAt, (l) => bankUrl(l, slug));
  if (gold) await page(goldUrl(lang), goldPage(lang, [...bySlug.values()]), gold.fetchedAt || rates.USD.updatedAt, goldUrl);
  if (cbuAll) {
    await page(allUrl(lang), allPage(lang, [...bySlug.values()]), cbuAll.updatedAt, allUrl);
    for (const c of cbuAll.currencies) if (!CURS.includes(c.code)) await page(cbuUrl(lang, c.code), cbuPage(lang, c, [...bySlug.values()]), cbuAll.updatedAt, (l) => cbuUrl(l, c.code));
  }
  for (const cur of Object.keys(AMOUNTS)) {
    if (perUnit(cur)) for (const n of AMOUNTS[cur]) await page(amountUrl(lang, cur, n), amountPage(lang, cur, n, [...bySlug.values()]), rates[cur]?.updatedAt || cbuAll.updatedAt, (l) => amountUrl(l, cur, n));
  }
  for (const cur of CURS) {
    for (const n of REV_AMOUNTS) await page(revUrl(lang, cur, n), reversePage(lang, cur, n, [...bySlug.values()]), rates[cur].updatedAt, (l) => revUrl(l, cur, n));
    if (history[cur]) await page(histUrl(lang, cur), historyPage(lang, cur, [...bySlug.values()]), rates[cur].updatedAt, (l) => histUrl(l, cur));
  }
}
await writeFile(join(DIST, "404.html"), shell({ lang: "uz", title: "404 | Qulay Kurs", noindex: true, body: `<div class="card page-card"><div class="page-text"><h1>404</h1>${LANGS.map((l) => `<p>${esc(X[l].notFoundText)} <a href="${curUrl(l, "USD")}">${esc(X[l].toHome)}</a></p>`).join("")}</div></div>` }));

const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${pages.map(([url, lastmod, urlOf]) => `  <url><loc>${ORIGIN}${url}</loc><lastmod>${lastmod}</lastmod>${LANGS.map((l) => `<xhtml:link rel="alternate" hreflang="${HTML_LANG[l]}" href="${ORIGIN}${urlOf(l)}"/>`).join("")}</url>`).join("\n")}
  <url><loc>${ORIGIN}/privacy</loc></url>
</urlset>
`;
await writeFile(join(DIST, "sitemap.xml"), sitemap);
/* ---------- self-check: a page with a dead link or a hole in its text must not be published ---------- */
// The build fails here, the workflow stops before the deploy, and the site keeps its previous version.
{
  const known = new Set(pages.map(([url]) => url)), problems = [], titles = new Map(), seen = new Map();
  const isFile = (path) => stat(join(DIST, path)).then((s) => s.isFile(), () => false);
  const exists = (href) => seen.get(href) ?? seen.set(href, known.has(href) ? true : Promise.all([isFile(href.slice(1)), isFile(`${href.slice(1)}.html`)]).then((r) => r.includes(true))).get(href);
  for (const [url, , urlOf, html] of pages) {
    const title = /<title>(.*?)<\/title>/.exec(html)?.[1];
    if (!title) problems.push(`${url}: no title`);
    else if (titles.has(title)) problems.push(`${url}: same title as ${titles.get(title)}`);
    else titles.set(title, url);
    if (!/<meta name="description" content="[^"]{50,}"/.test(html)) problems.push(`${url}: description missing or too short`);
    if ((html.match(/<h1[\s>]/g) || []).length !== 1) problems.push(`${url}: needs exactly one h1`);
    const hole = /\b(undefined|NaN|Infinity)\b|\[object /.exec(html.replace(/<script[\s\S]*?<\/script>/g, "").replace(/<[^>]+>/g, " "));
    if (hole) problems.push(`${url}: "${hole[0]}" in the text`);
    for (const l of LANGS) if (!known.has(urlOf(l))) problems.push(`${url}: no ${l} version`);
    for (const [, href] of html.matchAll(/\shref="(\/[^"#?]*)/g)) if (!(await exists(href))) problems.push(`${url}: dead link ${href}`);
  }
  if (problems.length) throw new Error(`${problems.length} problems in the built pages, not publishing:\n  ${problems.slice(0, 25).join("\n  ")}`);
}
// Read by the deploy workflow, which posts it to IndexNow after a deploy. Kept out of dist: it is not part of the site.
await writeFile(join(HERE, ".indexnow.json"), JSON.stringify({ host: new URL(ORIGIN).host, key: INDEXNOW_KEY, keyLocation: `${ORIGIN}/${INDEXNOW_KEY}.txt`, urlList: pages.map(([url]) => ORIGIN + url) }));
console.log(`Built ${pages.length} pages into dist (${bySlug.size} banks, rates as of ${rates.USD.updatedAt})`);
