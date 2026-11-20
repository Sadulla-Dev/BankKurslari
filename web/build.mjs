// Builds the deployable site into ./dist: a copy of ./public plus pre-rendered pages, so that search engines and
// link previews get real URLs with today's rates in the HTML instead of one empty JavaScript shell.
//
//   node build.mjs            reads https://api.qulaykurs.uz (API_URL overrides), writes ./dist
//
// Pages, in Uzbek (/), Russian (/ru/) and English (/en/):
//   currency pages   /, /yevro-kursi, /rubl-kursi …   the app itself, with the bank table already filled in
//   bank pages       /bank/<slug>                     plain HTML: one bank's rates for all three currencies
//   gold page        /oltin-narxi …                   plain HTML: Central Bank gold bar prices
// plus sitemap.xml and 404.html. No dependencies. Runs hourly from .github/workflows/web-deploy.yml.
import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const HERE = dirname(fileURLToPath(import.meta.url));
const PUBLIC = join(HERE, "public"), DIST = join(HERE, "dist");
const API = (process.env.API_URL || "https://api.qulaykurs.uz").replace(/\/$/, "");
const ORIGIN = "https://qulaykurs.uz";
const LANGS = ["uz", "ru", "en"], CURS = ["USD", "EUR", "RUB"];
const VERSION = new Date().toISOString().slice(0, 13).replace(/\D/g, ""); // cache-buster for css/js, changes hourly

/* ---------- shared with the browser app ---------- */

const shared = vm.createContext({});
vm.runInContext((await readFile(join(PUBLIC, "shared.js"), "utf8")) + "\n;globalThis.out = { T, logoSlug };", shared);
const { T, logoSlug } = shared.out;

/* ---------- addresses of pages ---------- */

const PREFIX = { uz: "", ru: "/ru", en: "/en" };
const CUR_SLUG = {
  uz: { USD: "", EUR: "yevro-kursi", RUB: "rubl-kursi" },
  ru: { USD: "", EUR: "kurs-evro", RUB: "kurs-rublya" },
  en: { USD: "", EUR: "eur-rate", RUB: "rub-rate" },
};
const GOLD_SLUG = { uz: "oltin-narxi", ru: "cena-zolota", en: "gold-price" };
const curUrl = (lang, cur) => `${PREFIX[lang]}/${CUR_SLUG[lang][cur]}`;
const goldUrl = (lang) => `${PREFIX[lang]}/${GOLD_SLUG[lang]}`;
const bankUrl = (lang, slug) => `${PREFIX[lang]}/bank/${slug}`;
const fileOf = (url) => (url.endsWith("/") ? `${url}index.html` : `${url}.html`).slice(1);
const slugify = (name) => name.toLowerCase().replace(/[‘’ʻ'`]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const bankSlug = (name) => logoSlug(name) || slugify(name);

/* ---------- formatting ---------- */

const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
const group = (n) => Math.round(Math.abs(n)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
const rate = (n, lang) => (Number.isInteger(n) ? group(n) : `${group(Math.trunc(n))}${lang === "en" ? "." : ","}${n.toFixed(2).split(".")[1]}`);
const signed = (n, lang) => `${n < 0 ? "−" : "+"}${rate(Math.abs(n), lang)}`;
const MONTHS = {
  uz: ["yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"],
  ru: ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"],
  en: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
};
// Tashkent is UTC+5 all year.
function when(iso, lang) {
  const d = new Date(Date.parse(iso) + 5 * 3600e3), day = d.getUTCDate(), mo = MONTHS[lang][d.getUTCMonth()], y = d.getUTCFullYear();
  const time = `${String(d.getUTCHours()).padStart(2, "0")}:${String(d.getUTCMinutes()).padStart(2, "0")}`;
  return { date: lang === "en" ? `${mo} ${day}, ${y}` : lang === "ru" ? `${day} ${mo} ${y}` : `${y}-yil ${day}-${mo}`, time };
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
    amount: (c) => `${group(AMOUNT[c])} ${X.uz.cur[c]}`, som: "so‘m", banks: (n) => `${n} ta bank`,
    title: (c) => `${X.uz.Cur[c]} kursi bugun: banklarda sotib olish va sotish | Qulay Kurs`,
    desc: (c, s) => `Bugun ${X.uz.cur[c]}ni eng qimmat ${s.bestBuy.bank} oladi — ${s.buy} so‘m, eng arzon ${s.bestSell.bank} sotadi — ${s.sell} so‘m. ${s.count} ta bank kursi, Markaziy bank kursi (${s.cbu}) va kalkulyator.`,
    sub: (n, time) => `${n} ta bank · ${time} holatiga · eng foydalisi tepada`,
    h2: (c, date) => `${X.uz.Cur[c]} kursi bugun, ${date}`,
    lead: (c, s) => `Markaziy bank kursi — 1 ${X.uz.cur[c]} = ${s.cbu} so‘m (kechagidan ${s.diff}). Banklar ${X.uz.cur[c]}ni ${s.minBuy}–${s.buy} so‘mdan sotib oladi va ${s.sell}–${s.maxSell} so‘mdan sotadi. ${X.uz.amount(c)} almashtirganda eng yaxshi va eng yomon bank orasidagi farq — ${s.gap} so‘m.`,
    q1: (c) => `${X.uz.Cur[c]}ni qaysi bankka sotgan foydali?`, a1: (c, s) => `Bugun eng yuqori sotib olish kursi: ${s.topBuy}. ${X.uz.amount(c)} uchun ${s.bestBuy.bank} ${s.buyTotal} so‘m beradi.`,
    q2: (c) => `${X.uz.Cur[c]}ni qayerdan arzon sotib olish mumkin?`, a2: (c, s) => `Bugun eng past sotish kursi: ${s.topSell}. ${X.uz.amount(c)} sotib olish ${s.bestSell.bank}da ${s.sellTotal} so‘mga tushadi.`,
    q3: "Markaziy bank kursi nega bank kursidan farq qiladi?", a3: "Markaziy bank kursi — rasmiy hisob-kitob kursi: u buxgalteriya, bojxona va soliq uchun ishlatiladi. Naqd valyutani banklar o‘zlari belgilagan sotib olish va sotish kurslari bo‘yicha almashtiradi, shuning uchun bankdagi kurs har doim biroz boshqacha bo‘ladi.",
    other: "Boshqa valyutalar", allBanks: "Banklar bo‘yicha kurslar", gold: "Oltin quymalar narxi", home: "Kurslar", map: "Xarita", history: "Tarix",
    bankTitle: (b, s) => `${b}: dollar kursi bugun — ${s} | Qulay Kurs`, bankH1: (b) => `${b}: valyuta kurslari bugun`,
    bankDesc: (b, u) => `${b} bugun dollarni ${u.buy} so‘mdan sotib oladi va ${u.sell} so‘mdan sotadi. Yevro va rubl kurslari, boshqa banklar bilan solishtirish — Qulay Kursda.`,
    bankShort: (u) => `oladi ${u.buy}, sotadi ${u.sell}`, colCur: "Valyuta", colBuy: "Bank sotib oladi", colSell: "Bank sotadi", colCbu: "Markaziy bank",
    updated: (d, t) => `${d}, ${t} holatiga`, rankBuy: (c, r, n) => `${X.uz.Cur[c]} sotib olish kursi bo‘yicha ${n} ta bank ichida ${r}-o‘rinda.`,
    bankSell: (c, b, total, gap, best) => `${X.uz.amount(c)} sotsangiz, ${b} ${total} so‘m beradi` + (gap ? ` — eng yaxshi kursdan (${best}) ${gap} so‘m kam.` : ` — bu bugungi eng yaxshi kurs.`),
    address: "Bosh ofis manzili", compare: "Barcha banklar bilan solishtirish", otherBanks: "Boshqa banklar",
    disclaimer: "Kurslar ma’lumot uchun berilgan va banklar hamda Markaziy bank e’lon qilgan ma’lumotlarga asoslanadi. Aniq kursni bankning o‘zidan tekshiring. Qulay Kurs biror bankka tegishli emas.",
    goldTitle: "Oltin quymalar narxi bugun — Markaziy bank | Qulay Kurs", goldH1: "Markaziy bank oltin quymalari narxi",
    goldDesc: (g) => `Bugun 1 gramm oltin (10 g quyma) — ${g.perGram} so‘m. 5, 10, 20, 50 va 100 grammlik quymalarning sotilish va qaytarib olinish narxlari.`,
    goldLead: (g) => `Markaziy bank 10 grammlik oltin quymani ${g.sell10} so‘mga sotadi va qadog‘i buzilmagan holda ${g.back10} so‘mga qaytarib oladi. Quymani olib, darhol qaytarib topshirsangiz, narxning ${g.loss}% qismini yo‘qotasiz.`,
    goldCols: ["Quyma", "Sotib olish narxi", "Qaytarib topshirish", "1 gramm", "Farq"], gram: (n) => `${n} g`,
    notFound: "Sahifa topilmadi", notFoundText: "Bunday sahifa yo‘q. Bugungi kurslar bosh sahifada.", toHome: "Bosh sahifaga",
  },
  ru: {
    cur: { USD: "доллара", EUR: "евро", RUB: "рубля" }, Cur: { USD: "доллара", EUR: "евро", RUB: "рубля" }, acc: { USD: "доллар", EUR: "евро", RUB: "рубль" },
    amount: (c) => `${group(AMOUNT[c])} ${{ USD: "долларов", EUR: "евро", RUB: "рублей" }[c]}`, som: "сум", banks: (n) => `${n} ${ruPlural(n, "банк", "банка", "банков")}`,
    title: (c) => `Курс ${X.ru.cur[c]} в банках Узбекистана сегодня: покупка и продажа | Qulay Kurs`,
    desc: (c, s) => `Сегодня дороже всех ${X.ru.acc[c]} покупает ${s.bestBuy.bank} — ${s.buy} сум, дешевле всех продаёт ${s.bestSell.bank} — ${s.sell} сум. Курсы ${X.ru.banks(s.count).replace(/банк$/, "банка")}, курс ЦБ (${s.cbu}) и калькулятор.`,
    sub: (n, time) => `${X.ru.banks(n)} · на ${time} · сначала выгодные`,
    h2: (c, date) => `Курс ${X.ru.cur[c]} на сегодня, ${date}`,
    lead: (c, s) => `Курс Центрального банка — 1 ${X.ru.acc[c]} = ${s.cbu} сум (${s.diff} ко вчерашнему). Банки покупают ${X.ru.acc[c]} по ${s.minBuy}–${s.buy} сум и продают по ${s.sell}–${s.maxSell} сум. При обмене ${X.ru.amount(c)} разница между лучшим и худшим банком — ${s.gap} сум.`,
    q1: (c) => `В каком банке выгоднее продать ${X.ru.acc[c]}?`, a1: (c, s) => `Самый высокий курс покупки сегодня: ${s.topBuy}. За ${X.ru.amount(c)} ${s.bestBuy.bank} даст ${s.buyTotal} сум.`,
    q2: (c) => `Где дешевле купить ${X.ru.acc[c]}?`, a2: (c, s) => `Самый низкий курс продажи сегодня: ${s.topSell}. Купить ${X.ru.amount(c)} в ${s.bestSell.bank} обойдётся в ${s.sellTotal} сум.`,
    q3: "Почему курс ЦБ отличается от курса в банке?", a3: "Курс Центрального банка — официальный расчётный курс: он нужен для бухгалтерии, таможни и налогов. Наличную валюту банки меняют по собственным курсам покупки и продажи, поэтому курс в банке всегда немного другой.",
    other: "Другие валюты", allBanks: "Курсы по банкам", gold: "Цены на золотые слитки", home: "Курсы", map: "Карта", history: "История",
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
    amount: (c) => `${group(AMOUNT[c])} ${{ USD: "US dollars", EUR: "euros", RUB: "rubles" }[c]}`, som: "so‘m", banks: (n) => `${n} banks`,
    title: (c) => `${c} to UZS today: ${X.en.cur[c]} rates at banks in Uzbekistan | Qulay Kurs`,
    desc: (c, s) => `Today ${s.bestBuy.bank} pays the most for a ${X.en.cur[c]} — ${s.buy} so‘m — and ${s.bestSell.bank} sells it cheapest at ${s.sell} so‘m. Rates from ${s.count} banks, the Central Bank rate (${s.cbu}) and a converter.`,
    sub: (n, time) => `${n} banks · as of ${time} · best first`,
    h2: (c, date) => `${X.en.Cur[c]} exchange rate in Uzbekistan today, ${date}`,
    lead: (c, s) => `The Central Bank rate is 1 ${c} = ${s.cbu} so‘m (${s.diff} since yesterday). Banks buy the ${X.en.cur[c]} at ${s.minBuy}–${s.buy} so‘m and sell it at ${s.sell}–${s.maxSell} so‘m. On ${X.en.amount(c)} the gap between the best and the worst bank is ${s.gap} so‘m.`,
    q1: (c) => `Which bank pays the most for a ${X.en.cur[c]}?`, a1: (c, s) => `The highest buying rates today: ${s.topBuy}. For ${X.en.amount(c)}, ${s.bestBuy.bank} gives you ${s.buyTotal} so‘m.`,
    q2: (c) => `Where is it cheapest to buy ${c}?`, a2: (c, s) => `The lowest selling rates today: ${s.topSell}. Buying ${X.en.amount(c)} at ${s.bestSell.bank} costs ${s.sellTotal} so‘m.`,
    q3: "Why is the Central Bank rate different from the rate at a bank?", a3: "The Central Bank rate is the official reference rate used for accounting, customs and taxes. Banks exchange cash at their own buying and selling rates, so the rate you get at a bank is always slightly different.",
    other: "Other currencies", allBanks: "Rates by bank", gold: "Gold bar prices", home: "Rates", map: "Map", history: "History",
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

/* ---------- data ---------- */

async function get(path) {
  const res = await fetch(API + path, { signal: AbortSignal.timeout(30000) });
  if (!res.ok) throw new Error(`${path}: HTTP ${res.status}`);
  return res.json();
}
const rates = Object.fromEntries(await Promise.all(CURS.map(async (c) => [c, await get(`/api/rates/${c.toLowerCase()}`)])));
const gold = await get("/api/gold").catch((e) => (console.warn(`gold skipped: ${e.message}`), null));
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
    cbu: rate(data.cbu.rate, lang), diff: signed(data.cbu.diff || 0, lang), gap: group((byBuy[0].buy - byBuy.at(-1).buy) * n),
    buyTotal: group(byBuy[0].buy * n), sellTotal: group(bySell[0].sell * n), topBuy: top(byBuy, "buy"), topSell: top(bySell, "sell"),
  };
}

/* ---------- parts of a page ---------- */

const alternates = (urlOf) => LANGS.map((l) => `<link rel="alternate" hreflang="${l}" href="${ORIGIN}${urlOf(l)}">`).join("\n") + `\n<link rel="alternate" hreflang="x-default" href="${ORIGIN}${urlOf("uz")}">`;
const ld = (obj) => `<script type="application/ld+json">${JSON.stringify(obj).replace(/</g, "\\u003c")}</script>`;
const ORG = { "@type": "Organization", "@id": `${ORIGIN}/#org`, name: "Qulay Kurs", url: `${ORIGIN}/`, logo: `${ORIGIN}/icon-512.png`, sameAs: ["https://t.me/qulay_kurs"] };
const crumbs = (items) => ({ "@type": "BreadcrumbList", itemListElement: items.map(([name, url], i) => ({ "@type": "ListItem", position: i + 1, name, item: ORIGIN + url })) });

function head({ lang, url, urlOf, title, desc, graph }) {
  return `<title>${esc(title)}</title>
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
<meta property="og:image" content="${ORIGIN}/og-${lang}.png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:locale" content="${{ uz: "uz_UZ", ru: "ru_RU", en: "en_US" }[lang]}">
<meta name="twitter:card" content="summary_large_image">
${ld({ "@context": "https://schema.org", "@graph": [ORG, { "@type": "WebSite", "@id": `${ORIGIN}/#site`, name: "Qulay Kurs", url: `${ORIGIN}/`, publisher: { "@id": `${ORIGIN}/#org` }, inLanguage: LANGS }, ...graph] })}`;
}

const badge = (name, extra = "") => {
  const slug = logoSlug(name);
  return `<span class="badge${extra}">${slug ? `<img src="/bank-logos/${slug}.png" alt="" width="40" height="40" loading="lazy">` : esc(name.slice(0, 2).toUpperCase())}</span>`;
};

// Links every page carries at the bottom: the other currencies, gold, and each bank's page.
function linkBlock(lang, bankNames, skipCur) {
  const x = X[lang];
  const curs = CURS.filter((c) => c !== skipCur).map((c) => `<a href="${curUrl(lang, c)}">${esc(T[lang].title[c])}</a>`);
  if (gold) curs.push(`<a href="${goldUrl(lang)}">${esc(x.gold)}</a>`);
  return `<h3>${esc(x.other)}</h3><p class="seo-links">${curs.join("")}</p>
  <h3>${esc(x.allBanks)}</h3><p class="seo-links">${bankNames.map((b) => `<a href="${bankUrl(lang, bankSlug(b))}">${esc(b)}</a>`).join("")}</p>
  <p class="seo-note">${esc(x.disclaimer)}</p>`;
}
const langLinks = (lang, urlOf) => LANGS.map((l) => `<a href="${urlOf(l)}" hreflang="${l}"${l === lang ? ' class="on" aria-current="true"' : ""}>${l.toUpperCase()}</a>`).join("");

/* ---------- currency pages: the app, pre-filled ---------- */

const template = await readFile(join(PUBLIC, "index.html"), "utf8");
function cut(html, from, to, replacement) {
  const a = html.indexOf(from), b = html.indexOf(to, a + from.length);
  if (a < 0 || b < 0) throw new Error(`template marker not found: ${from} … ${to}`);
  return html.slice(0, a) + replacement + html.slice(b + to.length);
}
const pageMap = {
  urls: Object.fromEntries(LANGS.map((l) => [l, Object.fromEntries(CURS.map((c) => [c, curUrl(l, c)]))])),
  titles: Object.fromEntries(LANGS.map((l) => [l, Object.fromEntries(CURS.map((c) => [c, X[l].title(c)]))])),
};

function currencyPage(lang, cur) {
  const x = X[lang], t = T[lang], s = summary(cur, lang), data = rates[cur], url = curUrl(lang, cur), at = when(data.updatedAt, lang);
  const faq = [[x.q1(cur), x.a1(cur, s)], [x.q2(cur), x.a2(cur, s)], [x.q3, x.a3]];
  const graph = [{ "@type": "FAQPage", inLanguage: lang, dateModified: data.updatedAt, mainEntity: faq.map(([q, a]) => ({ "@type": "Question", name: q, acceptedAnswer: { "@type": "Answer", text: a } })) }];
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

  let html = template.replace('<html lang="uz">', `<html lang="${lang}">`);
  html = cut(html, "<title>", '<link rel="sitemap" type="application/xml" href="https://qulaykurs.uz/sitemap.xml">', head({ lang, url, urlOf: (l) => curUrl(l, cur), title: x.title(cur), desc: x.desc(cur, s), graph }));
  html = cut(html, '<script type="application/ld+json">', "</script>", "");
  html = html.replace(/(data-i18n="([\w.]+)"[^>]*>)[^<]*</g, (m, open, key) => (typeof t[key] === "string" ? `${open}${esc(t[key])}<` : m));
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
  html = html.replace(/\?v=\d+/g, `?v=${VERSION}`).replace('<link rel="stylesheet" href="/style.css', '<link rel="stylesheet" href="/pages.css?v=' + VERSION + '">\n<link rel="stylesheet" href="/style.css');
  return html.replace('<script src="/shared.js', `<script>window.QK=${JSON.stringify({ lang, cur, ...pageMap })}</script>\n<script src="/shared.js`);
}

/* ---------- plain pages: banks, gold, 404 ---------- */

function shell({ lang, url, urlOf, title, desc, graph, body, noindex }) {
  const x = X[lang], home = curUrl(lang, "USD");
  return `<!doctype html>
<html lang="${lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
${noindex ? `<title>${esc(title)}</title>\n<meta name="robots" content="noindex">\n<meta name="theme-color" content="#FBF6E9">` : head({ lang, url, urlOf, title, desc, graph })}
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="manifest" href="/manifest.webmanifest">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Onest:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<script>try{const t=localStorage.getItem("theme");document.documentElement.dataset.theme=t||(matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light")}catch{document.documentElement.dataset.theme="light"}</script>
<link rel="stylesheet" href="/style.css?v=${VERSION}">
<link rel="stylesheet" href="/pages.css?v=${VERSION}">
</head>
<body class="page">
<header class="top">
  <div class="wrap top-in">
    <a class="logo" href="${home}" aria-label="Qulay Kurs"><img src="/logo.svg" width="32" height="32" alt=""><span>Qulay <b class="logo-k">Kurs</b></span></a>
    <nav class="tabs-nav" aria-label="Menu"><a href="${home}">${esc(x.home)}</a><a href="${home}#/xarita">${esc(x.map)}</a><a href="${home}#/tarix">${esc(x.history)}</a></nav>
    <div class="top-r">${urlOf ? `<div class="langs">${langLinks(lang, urlOf)}</div>` : ""}</div>
  </div>
</header>
<main class="wrap page-main">
${body}
</main>
<footer class="foot"><div class="wrap foot-in"><span>© ${new Date().getUTCFullYear()} Qulay Kurs</span><a class="foot-tg" href="https://t.me/qulay_kurs" rel="noopener">Telegram @qulay_kurs</a><a href="/privacy">Privacy</a></div></footer>
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
<section class="seo-copy">
  <h3>${esc(x.otherBanks)}</h3><p class="seo-links">${allNames.filter((b) => b !== name).map((b) => `<a href="${bankUrl(lang, bankSlug(b))}">${esc(b)}</a>`).join("")}</p>
  <h3>${esc(x.other)}</h3><p class="seo-links">${CURS.map((c) => `<a href="${curUrl(lang, c)}">${esc(t.title[c])}</a>`).join("")}${gold ? `<a href="${goldUrl(lang)}">${esc(x.gold)}</a>` : ""}</p>
  <p class="seo-note">${esc(x.disclaimer)}</p>
</section>`;
  return shell({
    lang, url, urlOf: (l) => bankUrl(l, slug), title: x.bankTitle(name, x.bankShort(u)), desc: x.bankDesc(name, u), body,
    graph: [crumbs([["Qulay Kurs", home], [name, url]]), { "@type": "WebPage", name: x.bankH1(name), url: ORIGIN + url, inLanguage: lang, dateModified: rates[usd.cur].updatedAt, about: { "@type": "BankOrCreditUnion", name, ...(address ? { address } : {}) } }],
  });
}

function goldPage(lang, allNames) {
  const x = X[lang], url = goldUrl(lang), home = curUrl(lang, "USD"), ten = gold.bars.find((b) => b.grams === 10) || gold.bars[0];
  const g = { perGram: group(ten.sell / ten.grams), sell10: group(ten.sell), back10: group(ten.buyback), loss: (((ten.sell - ten.buyback) / ten.sell) * 100).toFixed(1).replace(".", lang === "en" ? "." : ",") };
  const body = `<nav class="crumbs" aria-label="Breadcrumb"><a href="${home}">Qulay Kurs</a><span>›</span><span>${esc(x.gold)}</span></nav>
<div class="card page-card">
  <div class="page-head"><div><h1>${esc(x.goldH1)}</h1><p class="muted">cbu.uz · ${esc(gold.date)}, ${esc(gold.time)}</p></div></div>
  <div class="tbl-scroll"><table class="tbl page-tbl">
    <thead><tr>${x.goldCols.map((c, i) => `<th${i ? ' class="num"' : ""}>${esc(c)}</th>`).join("")}</tr></thead>
    <tbody>${gold.bars.map((b) => `<tr><td><b>${esc(x.gram(b.grams))}</b></td><td class="num">${group(b.sell)}</td><td class="num">${group(b.buyback)}</td><td class="num">${group(b.sell / b.grams)}</td><td class="num muted">−${(((b.sell - b.buyback) / b.sell) * 100).toFixed(1)}%</td></tr>`).join("")}</tbody>
  </table></div>
  <div class="page-text"><p>${esc(x.goldLead(g))}</p><p><a class="btn btn-accent" href="${home}#/tarix">${esc(x.history)}</a></p></div>
</div>
<section class="seo-copy">${linkBlock(lang, allNames, null)}</section>`;
  return shell({ lang, url, urlOf: goldUrl, title: x.goldTitle, desc: x.goldDesc(g), body, graph: [crumbs([["Qulay Kurs", home], [x.gold, url]])] });
}

/* ---------- write everything ---------- */

await rm(DIST, { recursive: true, force: true });
await cp(PUBLIC, DIST, { recursive: true });
const pages = []; // [url, lastmod, alternates(l) => url]
async function page(url, html, lastmod, urlOf) {
  const file = join(DIST, fileOf(url));
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, html);
  pages.push([url, lastmod, urlOf]);
}

const bankNames = [...new Set(CURS.flatMap((c) => summary(c, "uz").byBuy.map((b) => b.bank)))];
const bySlug = new Map(bankNames.map((n) => [bankSlug(n), n])); // one page per slug, first spelling wins
for (const lang of LANGS) {
  for (const cur of CURS) await page(curUrl(lang, cur), currencyPage(lang, cur), rates[cur].updatedAt, (l) => curUrl(l, cur));
  for (const [slug, name] of bySlug) await page(bankUrl(lang, slug), bankPage(lang, name, slug, [...bySlug.values()]), rates.USD.updatedAt, (l) => bankUrl(l, slug));
  if (gold) await page(goldUrl(lang), goldPage(lang, [...bySlug.values()]), gold.fetchedAt || rates.USD.updatedAt, goldUrl);
}
await writeFile(join(DIST, "404.html"), shell({ lang: "uz", title: "404 | Qulay Kurs", noindex: true, body: `<div class="card page-card"><div class="page-text"><h1>404</h1>${LANGS.map((l) => `<p>${esc(X[l].notFoundText)} <a href="${curUrl(l, "USD")}">${esc(X[l].toHome)}</a></p>`).join("")}</div></div>` }));

const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${pages.map(([url, lastmod, urlOf]) => `  <url><loc>${ORIGIN}${url}</loc><lastmod>${lastmod}</lastmod>${LANGS.map((l) => `<xhtml:link rel="alternate" hreflang="${l}" href="${ORIGIN}${urlOf(l)}"/>`).join("")}</url>`).join("\n")}
  <url><loc>${ORIGIN}/privacy</loc></url>
</urlset>
`;
await writeFile(join(DIST, "sitemap.xml"), sitemap);
console.log(`Built ${pages.length} pages into dist (${bySlug.size} banks, rates as of ${rates.USD.updatedAt})`);
