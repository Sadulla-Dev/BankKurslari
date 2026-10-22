// qulaykurs.uz — the web version of the app. Reads the same public API (api.qulaykurs.uz, rebuilt every 20 min).
"use strict";

const API = "https://api.qulaykurs.uz";
const TASHKENT = [41.3111, 69.2797]; // [lat, lng]
const CURS = ["USD", "EUR", "RUB"];
const BRANCH_ZOOM = 14;
const MAX_MAP_CANDS = 600; // hard cap on candidates before the per-marker project+overlap pass, independent of branch count
const MAP_STYLE = "https://tiles.openfreemap.org/styles/positron";
const ROUTES = { "": "rates", "xarita": "map", "tarix": "history" };

/* ---------- i18n ---------- */

function ruPlural(n, one, few, many) {
  const a = Math.abs(n) % 100, b = a % 10;
  if (a > 10 && a < 20) return many;
  if (b === 1) return one;
  if (b >= 2 && b <= 4) return few;
  return many;
}

const T = {
  uz: {
    "tab.rates": "Kurslar", "tab.map": "Xarita", "tab.history": "Tarix",
    "mode.sell": "Sotaman", "mode.buy": "Sotib olaman", "som": "so‘m", "go": "Yo‘l",
    "col.bank": "Bank", "col.buy": "Sotib oladi", "col.sell": "Sotadi", "col.km": "Masofa",
    "promo.title": "Qulay Kurs ilovasi", "promo.sub": "Kurs ogohlantirishlari, bosh ekran vidjeti va xarita — telefoningizda. Tez kunda.",
    "store.soon": "Tez kunda", "hist.cbu": "Markaziy bank kursi",
    "spread.buy": "Bugun eng yuqori xarid", "spread.sell": "Bugun eng past sotuv",
    "spread.note": "Xarid — bank sizdan valyutani oladigan narx. Sotuv — bank sizga sotadigan narx.",
    "foot.src": "Ma’lumotlar: cbu.uz va bank.uz · har 20 daqiqada yangilanadi", "foot.note": "Kurslar ma’lumot uchun. Bitim oldidan bank bilan aniqlashtiring.",
    "sh.buys": "Bank sotib oladi", "sh.sells": "Bank sotadi", "sh.near": "Eng yaqin filiallar", "sh.showMap": "Xaritada", "sh.go": "Yo‘nalish",
    title: { USD: "Banklarda dollar kursi", EUR: "Banklarda yevro kursi", RUB: "Banklarda rubl kursi" },
    tblSub: (n, t, s) => `${n} bank · ${t} holatiga · ${s}`,
    sortBy: { best: "eng foydalisi tepada", buy: "xarid kursi bo‘yicha", sell: "sotuv kursi bo‘yicha", name: "nomi bo‘yicha", km: "yaqinlik bo‘yicha" },
    getSell: "Siz olasiz", getBuy: "Siz to‘laysiz",
    search: "Bank qidirish", near: "Yaqinini ko‘rsatish", nearOn: "Yaqinlari tepada",
    status: (t, n) => `${t} holatiga · ${n} bank`,
    unit: { USD: () => "dollar", EUR: () => "yevro", RUB: () => "rubl" },
    perUnit: (r, u, n) => `1 ${u} uchun ${r} so‘m — ${n} ta bank ichida eng yaxshisi`,
    perUnitBuy: (r, u, n) => `1 ${u} uchun ${r} so‘m — ${n} ta bank ichida eng arzoni`,
    less: (v) => `−${v}`, more: (v) => `+${v}`, best: "eng yaxshi",
    km: (v) => `${v} km`, away: (v) => `${v} km uzoqlikda`,
    forAmt: (a, u) => `so‘m — ${a} ${u} uchun`,
    cmpLess: (v, b) => `${b}dan ${v} so‘m kam`, cmpMore: (v, b) => `${b}dan ${v} so‘m qimmat`,
    further: (k) => `, ${k} km uzoqroq`, isBest: "Hozir eng qulay kurs shu bankda",
    today: "bugun", noChange: "o‘zgarmagan",
    askLoc: "Masofani ko‘rsatish", noBranches: "Filiallar ro‘yxati topilmadi", noMatch: "Bunday bank topilmadi",
    mapBest: (a, u) => `${a} ${u} uchun eng yaxshi`,
    sideHint: { sell: "Bank sizdan sotib oladigan kurs. Yuqoridagisi — eng foydalisi.", buy: "Bank sizga sotadigan kurs. Yuqoridagisi — eng arzoni." },
    locFail: "Joylashuvni aniqlab bo‘lmadi", copied: "Havola nusxalandi",
    share: (b, r, u) => `${b}: 1 ${u} = ${r} so‘m — qulaykurs.uz`,
    src: "Manba", since: (y) => `${y}-yildan`,
    ranges: ["1H", "1O", "3O", "1Y", "Hammasi"], periods: ["7 kun", "30 kun", "3 oy", "1 yil"],
    toToday: "bugungacha", cbu: "Markaziy bank", tkBuy: "xarid", tkSell: "sotuv",
    error: "Kurslarni yuklab bo‘lmadi. Internetni tekshirib, sahifani yangilang."
  },
  ru: {
    "tab.rates": "Курсы", "tab.map": "Карта", "tab.history": "История",
    "mode.sell": "Продаю", "mode.buy": "Покупаю", "som": "сум", "go": "Маршрут",
    "col.bank": "Банк", "col.buy": "Покупает", "col.sell": "Продаёт", "col.km": "Расстояние",
    "promo.title": "Приложение Qulay Kurs", "promo.sub": "Уведомления о курсе, виджет на экране и карта — в вашем телефоне. Скоро.",
    "store.soon": "Скоро", "hist.cbu": "Курс Центрального банка",
    "spread.buy": "Лучшая покупка сегодня", "spread.sell": "Лучшая продажа сегодня",
    "spread.note": "Покупка — цена, по которой банк берёт у вас валюту. Продажа — цена, по которой продаёт вам.",
    "foot.src": "Данные: cbu.uz и bank.uz · обновляется каждые 20 минут", "foot.note": "Курсы носят справочный характер. Уточняйте в банке перед сделкой.",
    "sh.buys": "Банк покупает", "sh.sells": "Банк продаёт", "sh.near": "Ближайшие филиалы", "sh.showMap": "На карте", "sh.go": "Маршрут",
    title: { USD: "Курс доллара в банках", EUR: "Курс евро в банках", RUB: "Курс рубля в банках" },
    tblSub: (n, t, s) => `${n} банков · на ${t} · ${s}`,
    sortBy: { best: "выгодные сверху", buy: "по покупке", sell: "по продаже", name: "по названию", km: "ближайшие сверху" },
    getSell: "Вы получите", getBuy: "Вы заплатите",
    search: "Поиск банка", near: "Показать ближайшие", nearOn: "Ближайшие сверху",
    status: (t, n) => `на ${t} · ${n} банков`,
    unit: { USD: (n) => ruPlural(n, "доллар", "доллара", "долларов"), EUR: () => "евро", RUB: (n) => ruPlural(n, "рубль", "рубля", "рублей") },
    perUnit: (r, u, n) => `${r} сум за 1 ${u} — лучший из ${n} банков`,
    perUnitBuy: (r, u, n) => `${r} сум за 1 ${u} — дешевле всех из ${n} банков`,
    less: (v) => `−${v}`, more: (v) => `+${v}`, best: "лучший",
    km: (v) => `${v} км`, away: (v) => `${v} км от вас`,
    forAmt: (a, u) => `сум за ${a} ${u}`,
    cmpLess: (v, b) => `На ${v} сум меньше, чем в ${b}`, cmpMore: (v, b) => `На ${v} сум дороже, чем в ${b}`,
    further: (k) => `, на ${k} км дальше`, isBest: "Сейчас здесь самый выгодный курс",
    today: "сегодня", noChange: "без изменений",
    askLoc: "Показать расстояние", noBranches: "Список филиалов не найден", noMatch: "Банк не найден",
    mapBest: (a, u) => `Лучший курс за ${a} ${u}`,
    sideHint: { sell: "Курс, по которому банк покупает у вас. Сверху — самый выгодный.", buy: "Курс, по которому банк продаёт вам. Сверху — самый дешёвый." },
    locFail: "Не удалось определить местоположение", copied: "Ссылка скопирована",
    share: (b, r, u) => `${b}: 1 ${u} = ${r} сум — qulaykurs.uz`,
    src: "Источник", since: (y) => `с ${y} г.`,
    ranges: ["1Н", "1М", "3М", "1Г", "Всё"], periods: ["7 дней", "30 дней", "3 месяца", "1 год"],
    toToday: "до сегодня", cbu: "ЦБ", tkBuy: "покупка", tkSell: "продажа",
    error: "Не удалось загрузить курсы. Проверьте интернет и обновите страницу."
  },
  en: {
    "tab.rates": "Rates", "tab.map": "Map", "tab.history": "History",
    "mode.sell": "I sell", "mode.buy": "I buy", "som": "so‘m", "go": "Go",
    "col.bank": "Bank", "col.buy": "Buys at", "col.sell": "Sells at", "col.km": "Distance",
    "promo.title": "The Qulay Kurs app", "promo.sub": "Rate alerts, a home-screen widget and the map — on your phone. Coming soon.",
    "store.soon": "Coming soon", "hist.cbu": "Central Bank rate",
    "spread.buy": "Best buy rate today", "spread.sell": "Best sell rate today",
    "spread.note": "Buy is what a bank pays you for currency. Sell is what it charges you.",
    "foot.src": "Data: cbu.uz and bank.uz · updated every 20 minutes", "foot.note": "Rates are for reference. Confirm with the bank before you exchange.",
    "sh.buys": "Bank buys at", "sh.sells": "Bank sells at", "sh.near": "Nearest branches", "sh.showMap": "Show on map", "sh.go": "Directions",
    title: { USD: "Dollar rates at banks", EUR: "Euro rates at banks", RUB: "Ruble rates at banks" },
    tblSub: (n, t, s) => `${n} banks · as of ${t} · ${s}`,
    sortBy: { best: "best first", buy: "by buy rate", sell: "by sell rate", name: "by name", km: "nearest first" },
    getSell: "You get", getBuy: "You pay",
    search: "Search banks", near: "Show nearest", nearOn: "Nearest first",
    status: (t, n) => `as of ${t} · ${n} banks`,
    unit: { USD: (n) => (n === 1 ? "dollar" : "dollars"), EUR: (n) => (n === 1 ? "euro" : "euros"), RUB: (n) => (n === 1 ? "ruble" : "rubles") },
    perUnit: (r, u, n) => `${r} so‘m per ${u} — the best of ${n} banks`,
    perUnitBuy: (r, u, n) => `${r} so‘m per ${u} — the cheapest of ${n} banks`,
    less: (v) => `−${v}`, more: (v) => `+${v}`, best: "best",
    km: (v) => `${v} km`, away: (v) => `${v} km away`,
    forAmt: (a, u) => `so‘m for ${a} ${u}`,
    cmpLess: (v, b) => `${v} so‘m less than ${b}`, cmpMore: (v, b) => `${v} so‘m more than ${b}`,
    further: (k) => `, ${k} km further`, isBest: "The best rate right now",
    today: "today", noChange: "no change",
    askLoc: "Show distance", noBranches: "No branch list found", noMatch: "No bank matches",
    mapBest: (a, u) => `Best for ${a} ${u}`,
    sideHint: { sell: "What the bank pays you. The top one is the best.", buy: "What the bank charges you. The top one is the cheapest." },
    locFail: "Couldn’t get your location", copied: "Link copied",
    share: (b, r, u) => `${b}: 1 ${u} = ${r} so‘m — qulaykurs.uz`,
    src: "Source", since: (y) => `since ${y}`,
    ranges: ["1W", "1M", "3M", "1Y", "All"], periods: ["7 days", "30 days", "3 months", "1 year"],
    toToday: "to today", cbu: "CBU", tkBuy: "buy", tkSell: "sell",
    error: "Couldn’t load rates. Check your connection and reload."
  }
};

const store = {
  get(k) { try { return localStorage.getItem(k); } catch { return null; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch { /* private mode */ } }
};

function detectLang() {
  const q = new URLSearchParams(location.search).get("lang");
  if (q && T[q]) return q;
  const saved = store.get("lang");
  if (saved && T[saved]) return saved;
  for (const l of (navigator.languages || [navigator.language || "uz"]).map((x) => x.toLowerCase())) {
    if (l.startsWith("uz")) return "uz";
    if (l.startsWith("ru")) return "ru";
    if (l.startsWith("en")) return "en";
  }
  return "uz";
}

/* ---------- state ---------- */

const S = {
  lang: detectLang(),
  view: "rates",
  mode: store.get("mode") === "buy" ? "buy" : "sell",
  cur: CURS.includes(store.get("cur")) ? store.get("cur") : "USD",
  amount: Number(store.get("amount")) || 100,
  sort: "best", query: "",
  rates: {}, history: {}, bankHist: {},
  branches: null, user: null,
  range: 1, sheetBank: null,
  map: null, markers: [], meMarker: null, hl: null
};
const L = () => T[S.lang];
const $ = (id) => document.getElementById(id);
const setText = (id, v) => { const el = $(id); if (el) el.textContent = v; };

/* ---------- formatting ---------- */

function group(n) {
  const s = Math.round(Math.abs(n)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  return (n < 0 ? "−" : "") + s;
}
const dec = (s) => (S.lang === "en" ? s : s.replace(".", ","));
const rateFmt = (v) => (v >= 1000 || Number.isInteger(v) ? group(v) : dec(v.toFixed(2)));
const kmFmt = (km) => dec(km < 10 ? km.toFixed(1) : String(Math.round(km)));
const tashkentTime = (iso) => new Intl.DateTimeFormat("en-GB", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Tashkent", hour12: false }).format(new Date(iso));
const MONTHS = {
  uz: ["yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"],
  ru: ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"],
  en: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
};
function fullDate(ymd) {
  const [y, m, d] = ymd.split("-").map(Number);
  const mo = MONTHS[S.lang][m - 1];
  return S.lang === "en" ? `${mo} ${d}, ${y}` : S.lang === "ru" ? `${d} ${mo} ${y}` : `${d}-${mo}, ${y}`;
}
const unit = (cur, n) => L().unit[cur](n);
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

/* ---------- bank names & addresses ---------- */

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
const bankKey = (n) => n.toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");
function keysMatch(a, b) {
  if (!a || !b) return false;
  if (a === b) return true;
  const [s, l] = a.length <= b.length ? [a, b] : [b, a];
  return s.length >= 5 && l.includes(s);
}
/**
 * [street + number, the rest]: "100029, Toshkent shahri, Mirobod tumani, A.Temur ko'chasi, 53 uy"
 * -> ["A.Temur ko'chasi 53 uy", "Mirobod tumani"]. Postcodes and the bare city name are dropped.
 */
function addrParts(a) {
  if (!a) return ["", ""];
  const parts = a.split(/,\s*/).filter((p) => p && !/^\d{5,6}$/.test(p) && !/^(toshkent|tashkent|ташкент)(\s+(shahri|sh\.|city|г\.))?$/i.test(p));
  const si = parts.findIndex((p) => /ko.ch|kuch|street|ул\.|улица|prospekt|проспект|shoh/i.test(p));
  if (si < 0) return [parts.slice(0, 2).join(", "), parts.slice(2).join(", ")];
  let head = parts[si];
  const used = new Set([si]);
  const num = [si + 1, si - 1].find((i) => i >= 0 && i < parts.length && /^\d+[\p{L}/-]?\d*(\s*(uy|dom|д\.))?$/iu.test(parts[i]));
  if (num != null) { head += " " + parts[num]; used.add(num); }
  return [head, parts.filter((_, i) => !used.has(i)).join(", ")];
}
const cleanAddr = (a) => addrParts(a).filter(Boolean).join(", ");

/* ---------- geo ---------- */

function distKm(a, b) {
  const R = 6371, rad = Math.PI / 180;
  const dLat = (b[0] - a[0]) * rad, dLng = (b[1] - a[1]) * rad;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(a[0] * rad) * Math.cos(b[0] * rad) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}
const directions = (lat, lng) => `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
function locate() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error("no geolocation"));
    navigator.geolocation.getCurrentPosition(
      (p) => { S.user = [p.coords.latitude, p.coords.longitude]; store.set("loc", "1"); resolve(S.user); },
      reject, { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 });
  });
}

/* ---------- data ---------- */

async function getJSON(path) {
  const res = await fetch(API + path);
  if (!res.ok) throw new Error(path + " " + res.status);
  return res.json();
}
const inflight = {};
const once = (key, fn) => (inflight[key] ||= fn().catch((e) => { delete inflight[key]; throw e; }));
const loadRates = (cur) => S.rates[cur] ? Promise.resolve(S.rates[cur]) : once("r" + cur, async () => (S.rates[cur] = await getJSON(`/api/rates/${cur.toLowerCase()}`)));
const loadHistory = (cur) => S.history[cur] ? Promise.resolve(S.history[cur]) : once("h" + cur, async () => (S.history[cur] = (await getJSON(`/api/history/${cur.toLowerCase()}`)).points));
const loadBankHist = (cur) => S.bankHist[cur] ? Promise.resolve(S.bankHist[cur]) : once("b" + cur, async () => (S.bankHist[cur] = (await getJSON(`/api/bank-history/${cur.toLowerCase()}?days=2`)).days));
const loadBranches = () => S.branches ? Promise.resolve(S.branches) : once("br", async () => (S.branches = (await getJSON("/api/branches")).branches));

const branchCache = new Map();
function branchesOf(bank) {
  if (!S.branches) return [];
  if (branchCache.has(bank)) return branchCache.get(bank);
  const k = bankKey(bank);
  const seen = new Set();
  const list = S.branches.filter((b) => keysMatch(bankKey(b.bank), k)).filter((b) => {
    const id = (b.address || "").toLowerCase().replace(/\s+/g, " ") || `${b.lat.toFixed(4)},${b.lng.toFixed(4)}`;
    if (seen.has(id)) return false;
    seen.add(id);
    return true;
  });
  branchCache.set(bank, list);
  return list;
}
/** Nearest branch to [from]; without a location, the one nearest central Tashkent (km stays null then). */
function nearestBranch(bank, from) {
  const ref = from || TASHKENT;
  const list = branchesOf(bank.bank);
  if (!list.length) return bank.branch?.lat ? { ...bank.branch, km: from ? distKm(from, [bank.branch.lat, bank.branch.lng]) : null } : null;
  let best = null, bd = Infinity;
  for (const b of list) { const d = distKm(ref, [b.lat, b.lng]); if (d < bd) { bd = d; best = b; } }
  return { ...best, km: from ? bd : null };
}
/** Selling -> highest buy rate first; buying -> lowest sell rate first. */
function ranked(data, mode = S.mode) {
  const key = mode === "sell" ? "buy" : "sell";
  return (data?.banks || []).filter((b) => b[key] > 0)
    .sort((a, b) => (mode === "sell" ? b[key] - a[key] : a[key] - b[key]))
    .map((b) => ({ ...b, rate: b[key] }));
}

/* ---------- number animation ---------- */

const numAnim = new WeakMap();
const reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;
function animateNumber(el, to, fmt = group) {
  if (!el) return;
  const from = numAnim.get(el) ?? to;
  numAnim.set(el, to);
  if (from === to || reduced) { el.textContent = fmt(to); return; }
  const start = performance.now(), dur = 420;
  const step = (now) => {
    if (numAnim.get(el) !== to) return;
    const p = Math.min(1, (now - start) / dur), e = 1 - Math.pow(1 - p, 3);
    el.textContent = fmt(p < 1 ? from + (to - from) * e : to);
    if (p < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}

/* ---------- router ---------- */

function route() {
  const key = location.hash.replace(/^#\/?/, "").split("?")[0];
  S.view = ROUTES[key] || "rates";
  for (const v of ["rates", "map", "history"]) $(`v-${v}`).hidden = v !== S.view;
  document.body.className = "v-" + S.view;
  document.querySelectorAll(".tabs-nav a").forEach((a) => a.classList.toggle("on", a.dataset.view === S.view));
  if (S.view === "map") ensureMap().then(() => { S.map?.resize(); renderMap(); });
  if (S.view === "history") renderHistory();
  scrollTo(0, 0);
}

/* ---------- shared controls ---------- */

function syncControls() {
  document.querySelectorAll(".curSeg button").forEach((b) => b.classList.toggle("on", b.dataset.cur === S.cur));
  document.querySelectorAll("#modeSeg button, #mapModeSeg button").forEach((b) => b.classList.toggle("on", b.dataset.mode === S.mode));
}
async function setCur(cur) {
  if (cur === S.cur && S.rates[cur]) return;
  S.cur = cur;
  store.set("cur", cur);
  renderQuick();
  renderAll();
  try { await loadRates(cur); } catch { showError(); }
  renderAll();
}
function setMode(mode) {
  S.mode = mode;
  store.set("mode", mode);
  renderAll();
}
function renderAll() {
  syncControls();
  renderStatus();
  renderConv();
  renderCbu();
  renderTable();
  if (S.view === "map") renderMap();
  if (S.view === "history") renderHistory();
}

function renderStatus() {
  const d = S.rates[S.cur];
  if (d) setText("status", L().status(tashkentTime(d.updatedAt), d.banks.length));
}

/* ---------- converter ---------- */

function renderConv() {
  const t = L();
  setText("amountUnit", unit(S.cur, S.amount));
  setText("resLabel", S.mode === "sell" ? t.getSell : t.getBuy);
  const list = ranked(S.rates[S.cur]);
  if (!list.length) return;
  const best = list[0];
  animateNumber($("total"), best.rate * S.amount);
  setText("totalCap", (S.mode === "sell" ? t.perUnit : t.perUnitBuy)(rateFmt(best.rate), unit(S.cur, 1), list.length));
  setText("bestBadge", initials(best.bank));
  setText("bestName", best.bank);
  const nb = nearestBranch(best, S.user);
  setText("bestSub", nb?.km != null ? t.away(kmFmt(nb.km)) + (nb.address ? " · " + cleanAddr(nb.address) : "") : cleanAddr(nb?.address || best.branch?.address));
  $("bestBank").onclick = () => openSheet(best.bank);
}
function renderQuick() {
  const sets = { USD: [100, 500, 1000, 5000], EUR: [100, 500, 1000, 5000], RUB: [1000, 10000, 50000, 100000] };
  $("quick").innerHTML = sets[S.cur].map((v) => `<button type="button" data-v="${v}">${group(v)}</button>`).join("");
}
function fitAmount() { const el = $("amount"); el.style.width = Math.max(2, el.value.length + 0.6) + "ch"; }
function setAmount(v, fromInput) {
  S.amount = Math.max(0, Math.min(1e9, Math.round(v) || 0));
  store.set("amount", String(S.amount));
  if (!fromInput) $("amount").value = group(S.amount);
  fitAmount();
  renderConv();
  renderTable();
}

/* ---------- CBU strip ---------- */

function renderCbu() {
  const t = L();
  const sym = { USD: "$", EUR: "€", RUB: "₽" };
  $("cbuStrip").innerHTML = CURS.map((c) => {
    const d = S.rates[c];
    if (!d?.cbu) return `<div class="cbu-i skel-box"></div>`;
    const diff = d.cbu.diff || 0;
    const cls = diff > 0 ? "up-t" : diff < 0 ? "down-t" : "";
    return `<button type="button" class="cbu-c${c === S.cur ? " on" : ""}" data-cur="${c}">
      <span class="sym">${sym[c]}</span>
      <span class="l1"><b>${rateFmt(d.cbu.rate)}</b><em class="${cls}">${diff > 0 ? "▲" : diff < 0 ? "▼" : ""} ${rateFmt(Math.abs(diff))}</em></span>
      <span class="l2">${c} · ${t.cbu}</span>
    </button>`;
  }).join("");
}

/* ---------- bank table ---------- */

function renderTable() {
  const t = L();
  const d = S.rates[S.cur];
  setText("tblTitle", t.title[S.cur]);
  $("search").placeholder = t.search;
  setText("colGet", S.mode === "sell" ? t.getSell : t.getBuy);
  $("colKm").hidden = !S.user;
  setText("nearLabel", S.sort === "km" ? t.nearOn : t.near);
  $("nearBtn").classList.toggle("on", S.sort === "km");
  document.querySelectorAll(".tbl th button").forEach((b) => b.classList.toggle("on", b.dataset.sort === S.sort));
  if (!d) { $("tbody").innerHTML = '<tr class="skel-row"><td colspan="5"></td></tr>'.repeat(6); return; }
  setText("tblSub", t.tblSub(d.banks.length, tashkentTime(d.updatedAt), t.sortBy[S.sort]));
  setText("tblFoot", t["foot.src"] + ". " + t["foot.note"]);

  const list = ranked(d);
  if (!list.length) return;
  const best = list[0];
  const maxBuy = Math.max(...list.map((b) => b.buy || 0));
  const minSell = Math.min(...list.filter((b) => b.sell > 0).map((b) => b.sell));
  let rows = list.map((b) => ({ ...b, nb: nearestBranch(b, S.user) }));
  const q = S.query.trim().toLowerCase();
  if (q) rows = rows.filter((b) => b.bank.toLowerCase().includes(q) || shortName(b.bank).toLowerCase().includes(q));
  const by = {
    best: () => 0,
    buy: (a, b) => b.buy - a.buy,
    sell: (a, b) => (a.sell || 1e12) - (b.sell || 1e12),
    name: (a, b) => a.bank.localeCompare(b.bank),
    km: (a, b) => (a.nb?.km ?? 1e9) - (b.nb?.km ?? 1e9)
  }[S.sort];
  rows.sort(by);
  if (!rows.length) { $("tbody").innerHTML = `<tr><td colspan="5" class="empty">${esc(t.noMatch)}</td></tr>`; return; }
  $("tbody").innerHTML = rows.map((b) => {
    const diff = Math.abs(b.rate - best.rate) * S.amount;
    const isBest = diff < 0.5;
    const sub = b.nb?.km != null ? t.away(kmFmt(b.nb.km)) : cleanAddr(b.nb?.address || b.branch?.address);
    return `<tr data-bank="${esc(b.bank)}" class="${isBest ? "top1" : ""}">
      <td class="c-bank"><span class="bn"><span class="badge${isBest ? " accent" : ""}">${esc(initials(b.bank))}</span><span><b>${esc(b.bank)}</b><small>${esc(sub)}</small></span></span></td>
      <td class="num c-buy ${b.buy === maxBuy ? "hi" : ""}">${b.buy ? rateFmt(b.buy) : "—"}</td>
      <td class="num c-sell ${b.sell === minSell ? "hi" : ""}">${b.sell ? rateFmt(b.sell) : "—"}</td>
      <td class="num c-get get"><b>${group(b.rate * S.amount)}</b><small class="${isBest ? "best" : ""}">${isBest ? t.best : (S.mode === "sell" ? t.less : t.more)(group(diff))}</small></td>
      ${S.user ? `<td class="num c-km">${b.nb?.km != null ? t.km(kmFmt(b.nb.km)) : "—"}</td>` : ""}
    </tr>`;
  }).join("");
}

/* ---------- bank sheet ---------- */

async function openSheet(bankName) {
  const list = ranked(S.rates[S.cur]);
  const bank = list.find((b) => b.bank === bankName);
  if (!bank) return;
  S.sheetBank = bankName;
  renderSheet(bank, list);
  $("scrim").hidden = false;
  $("sheet").hidden = false;
  requestAnimationFrame(() => { $("scrim").classList.add("on"); $("sheet").classList.add("on"); });
  document.body.style.overflow = "hidden";
  $("shClose").focus({ preventScroll: true });
  const c = S.cur;
  await Promise.allSettled([loadBranches(), loadBankHist(c)]);
  if (S.sheetBank === bankName && S.cur === c) renderSheet(bank, list);
}
function closeSheet() {
  if ($("sheet").hidden) return;
  S.sheetBank = null;
  $("scrim").classList.remove("on");
  $("sheet").classList.remove("on");
  document.body.style.overflow = "";
  setTimeout(() => { if (!S.sheetBank) { $("scrim").hidden = true; $("sheet").hidden = true; } }, 300);
}
function renderSheet(bank, list) {
  const t = L();
  const best = list[0];
  setText("shBadge", initials(bank.bank));
  $("shBadge").className = "badge" + (bank.rate === best.rate ? " accent" : "");
  setText("shName", bank.bank);
  const nb = nearestBranch(bank, S.user);
  setText("shSub", nb?.km != null ? t.away(kmFmt(nb.km)) : cleanAddr(nb?.address));
  setText("shTotal", group(bank.rate * S.amount));
  setText("shFor", t.forAmt(group(S.amount), unit(S.cur, S.amount)));
  const cmp = $("shCmp");
  if (bank.rate === best.rate) { cmp.className = "sh-cmp good"; cmp.textContent = t.isBest; }
  else {
    const diff = Math.abs(bank.rate - best.rate) * S.amount;
    const bestNb = nearestBranch(best, S.user);
    const further = nb?.km != null && bestNb?.km != null && nb.km - bestNb.km > 0.3 ? t.further(kmFmt(nb.km - bestNb.km)) : "";
    cmp.className = "sh-cmp warn";
    cmp.textContent = (S.mode === "sell" ? t.cmpLess : t.cmpMore)(group(diff), shortName(best.bank)) + further;
  }
  setText("shBuy", bank.buy ? rateFmt(bank.buy) : "—");
  setText("shSell", bank.sell ? rateFmt(bank.sell) : "—");
  const [dBuy, dSell] = todayDelta(bank.bank);
  setDelta($("shBuyD"), dBuy);
  setDelta($("shSellD"), dSell);

  const box = $("shBranches");
  if (!S.branches) box.innerHTML = `<div class="skel"></div>`;
  else {
    const ref = S.user || TASHKENT;
    let brs = branchesOf(bank.bank).map((b) => ({ ...b, d: distKm(ref, [b.lat, b.lng]), km: S.user ? distKm(S.user, [b.lat, b.lng]) : null }));
    brs.sort((a, b) => a.d - b.d);
    if (!brs.length && bank.branch?.lat) brs = [{ ...bank.branch, km: S.user ? distKm(S.user, [bank.branch.lat, bank.branch.lng]) : null }];
    box.innerHTML = brs.length
      ? brs.slice(0, 3).map((b) => {
          const [head, tail] = addrParts(b.address || bank.bank);
          return `<a class="br" href="${directions(b.lat, b.lng)}" target="_blank" rel="noopener"><span><b>${esc(head)}</b>${tail ? `<small>${esc(tail)}</small>` : ""}</span><span>${b.km != null ? t.km(kmFmt(b.km)) : ""}</span></a>`;
        }).join("") + (S.user ? "" : `<button class="ask" type="button" id="askLoc">${t.askLoc}</button>`)
      : `<p class="muted small">${t.noBranches}</p>`;
    const ask = $("askLoc");
    if (ask) ask.onclick = async () => {
      ask.disabled = true;
      try { await locate(); onLocated(); renderSheet(bank, list); } catch { toast(t.locFail); ask.disabled = false; }
    };
  }
  const target = nb || (bank.branch?.lat ? bank.branch : null);
  $("shGo").hidden = !target;
  if (target) $("shGo").href = directions(target.lat, target.lng);
  $("shShowMap").onclick = (e) => {
    e.preventDefault();
    closeSheet();
    S.focus = target ? { bank: bank.bank, lat: target.lat, lng: target.lng } : null;
    if (location.hash !== "#/xarita") location.hash = "#/xarita"; else focusOnMap();
  };
  $("shShare").onclick = async () => {
    const text = t.share(bank.bank, rateFmt(bank.rate), unit(S.cur, 1));
    try {
      if (navigator.share) await navigator.share({ title: "Qulay Kurs", text, url: "https://qulaykurs.uz" });
      else { await navigator.clipboard.writeText(text + " https://qulaykurs.uz"); toast(t.copied); }
    } catch { /* dismissed */ }
  };
  $("shSrc").innerHTML = bank.sourceUrl ? `${t.src}: <a href="${esc(bank.sourceUrl)}" target="_blank" rel="noopener">bank.uz</a>` : "";
}
function todayDelta(bankName) {
  const days = S.bankHist[S.cur];
  if (!days) return [null, null];
  const keys = Object.keys(days).sort();
  if (keys.length < 2) return [null, null];
  const k = bankKey(bankName);
  const pick = (day) => Object.entries(days[day]).find(([n]) => bankKey(n) === k)?.[1]?.last;
  const now = pick(keys[keys.length - 1]), prev = pick(keys[keys.length - 2]);
  return now && prev ? [now[0] - prev[0], now[1] - prev[1]] : [null, null];
}
function setDelta(el, d) {
  const t = L();
  if (d == null) { el.textContent = ""; el.className = ""; return; }
  if (Math.abs(d) < 0.005) { el.textContent = t.noChange; el.className = "flat"; return; }
  el.textContent = `${d > 0 ? "+" : "−"}${rateFmt(Math.abs(d))} ${t.today}`;
  el.className = d > 0 ? "" : "down";
}

/* ---------- map (MapLibre + OpenFreeMap) ---------- */

let mapReady = null;
function ensureMap() {
  mapReady ||= new Promise((resolve) => {
    const start = () => {
      const c = S.user || TASHKENT;
      const map = new maplibregl.Map({
        container: "mapgl", style: MAP_STYLE, center: [c[1], c[0]], zoom: S.user ? 14 : 12.6,
        attributionControl: { compact: true }, dragRotate: false, pitchWithRotate: false
      });
      map.touchZoomRotate.disableRotation();
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-right");
      S.map = map;
      // Coalesced: a mouse-wheel zoom fires one moveend per tick, and with a large branch dataset each
      // render rebuilds up to 150 DOM markers — running that for every tick in a fast scroll backs up
      // the main thread into a visible freeze. Only the last moveend in a burst actually renders.
      let moveendTimer = null;
      map.on("moveend", () => {
        clearTimeout(moveendTimer);
        moveendTimer = setTimeout(renderMap, 80);
      });
      map.on("load", async () => {
        if (S.user) showUser();
        await Promise.allSettled([loadBranches(), loadRates(S.cur)]);
        renderMap();
        focusOnMap();
        resolve();
      });
    };
    if (window.maplibregl) start(); else addEventListener("load", () => (window.maplibregl ? start() : resolve()));
  });
  return mapReady;
}
function focusOnMap() {
  if (!S.map || !S.focus) return;
  S.hl = S.focus.bank;
  S.map.flyTo({ center: [S.focus.lng, S.focus.lat], zoom: 15.5 });
  S.focus = null;
}
function showUser() {
  if (!S.map || !S.user) return;
  const ll = [S.user[1], S.user[0]];
  if (S.meMarker) S.meMarker.setLngLat(ll);
  else {
    const el = document.createElement("div");
    el.className = "me";
    S.meMarker = new maplibregl.Marker({ element: el }).setLngLat(ll).addTo(S.map);
  }
}
/** Pins (one per bank when zoomed out, every branch when zoomed in, best first, no overlaps) and the side list. */
function renderMap() {
  const map = S.map;
  const t = L();
  setText("sideHint", t.sideHint[S.mode]);
  if (!map || !S.rates[S.cur]) return;
  const list = ranked(S.rates[S.cur]);
  if (!list.length) return;
  const bestRate = list[0].rate;
  const c = map.getCenter();
  const center = [c.lat, c.lng];
  const ref = S.user || center;
  const bounds = map.getBounds();
  const zoomedIn = map.getZoom() >= BRANCH_ZOOM;
  const cands = [];
  bankLoop:
  for (const bank of list) {
    let brs = branchesOf(bank.bank);
    if (!brs.length && bank.branch?.lat) brs = [bank.branch];
    if (zoomedIn) {
      for (const b of brs) {
        if (bounds.contains([b.lng, b.lat])) cands.push({ bank, b });
        if (cands.length >= MAX_MAP_CANDS) break bankLoop;
      }
    }
    else {
      let near = null, nd = Infinity;
      for (const b of brs) { const dk = distKm(center, [b.lat, b.lng]); if (dk < nd) { nd = dk; near = b; } }
      if (near && bounds.contains([near.lng, near.lat])) cands.push({ bank, b: near });
    }
  }
  S.markers.forEach((m) => m.remove());
  S.markers = [];
  const placed = [];
  // The highlighted bank goes first so it is never culled.
  cands.sort((a, b) => (b.bank.bank === S.hl) - (a.bank.bank === S.hl));
  for (const { bank, b } of cands) {
    if (S.markers.length >= 150) break;
    const p = map.project([b.lng, b.lat]);
    const label = shortName(bank.bank);
    const w = 24 + (label.length + rateFmt(bank.rate).length) * 7.4, h = 34;
    const box = { x0: p.x - w / 2, x1: p.x + w / 2, y0: p.y - h - 10, y1: p.y };
    if (placed.some((q) => !(box.x1 < q.x0 || box.x0 > q.x1 || box.y1 < q.y0 || box.y0 > q.y1))) continue;
    placed.push(box);
    // MapLibre positions the marker element with its own transform, so the styled pill lives inside a bare wrapper.
    const el = document.createElement("div");
    el.className = "pin-wrap";
    const cls = "pill" + (bank.rate === bestRate ? " best" : "") + (bank.bank === S.hl ? " hl" : "");
    el.innerHTML = `<button type="button" class="${cls}" title="${esc(bank.bank)}"><span>${esc(label)}</span><b>${rateFmt(bank.rate)}</b></button>`;
    el.firstChild.addEventListener("click", (e) => { e.stopPropagation(); openSheet(bank.bank); });
    const m = new maplibregl.Marker({ element: el, anchor: "bottom" }).setLngLat([b.lng, b.lat]).addTo(map);
    if (bank.rate === bestRate || bank.bank === S.hl) m.getElement().style.zIndex = 3;
    S.markers.push(m);
  }

  // Side list: every bank with its branch nearest to you (or to the map centre).
  const best = list[0];
  $("sideList").innerHTML = list.map((bank) => {
    const nb = nearestBranch(bank, ref);
    const diff = Math.abs(bank.rate - best.rate);
    const isBest = diff < 0.005;
    const sub = [S.user && nb ? t.km(kmFmt(distKm(S.user, [nb.lat, nb.lng]))) : "", nb ? addrParts(nb.address)[0] : ""].filter(Boolean).join(" · ");
    return `<button type="button" class="si${bank.bank === S.hl ? " on" : ""}" data-bank="${esc(bank.bank)}" data-lat="${nb?.lat ?? ""}" data-lng="${nb?.lng ?? ""}">
      <span class="badge${isBest ? " accent" : ""}">${esc(initials(bank.bank))}</span>
      <span><b>${esc(bank.bank)}</b><small>${esc(sub)}</small></span>
      <span class="r"><b>${rateFmt(bank.rate)}</b><small class="${isBest ? "best" : "warn"}">${isBest ? t.best : (S.mode === "sell" ? t.less : t.more)(rateFmt(diff))}</small></span>
    </button>`;
  }).join("");

  const inView = cands.map((x) => x.bank);
  const top = inView.length ? inView.reduce((a, b) => ((S.mode === "sell" ? b.rate > a.rate : b.rate < a.rate) ? b : a)) : null;
  const card = $("mapCard");
  if (!top) { card.hidden = true; return; }
  const nb = nearestBranch(top, ref);
  card.hidden = false;
  setText("mcBadge", initials(top.bank));
  setText("mcName", top.bank);
  setText("mcSub", t.mapBest(group(S.amount), unit(S.cur, S.amount)) + (S.user && nb ? ", " + t.km(kmFmt(distKm(S.user, [nb.lat, nb.lng]))) : ""));
  $("mcGo").href = nb ? directions(nb.lat, nb.lng) : "#";
}

/* ---------- history ---------- */

const RANGE_DAYS = [7, 30, 90, 365, null];
async function renderHistory() {
  const cur = S.cur;
  $("ranges").innerHTML = L().ranges.map((r, i) => `<button type="button" data-i="${i}" class="${i === S.range ? "on" : ""}">${r}</button>`).join("");
  renderSpread();
  let pts;
  try { pts = await loadHistory(cur); } catch { return; }
  if (cur !== S.cur || !pts?.length || S.view !== "history") return;
  const days = RANGE_DAYS[S.range];
  const lastDay = new Date(pts[pts.length - 1].date).getTime();
  let slice = days ? pts.filter((p) => new Date(p.date).getTime() >= lastDay - days * 864e5) : pts;
  if (slice.length < 2) slice = pts.slice(-2);
  drawChart(slice);
}
function trendCaption(slice, idx) {
  const t = L();
  const first = slice[0], end = slice[slice.length - 1];
  const sign = (d) => (d < 0 ? "−" : "+") + rateFmt(Math.abs(d));
  if (idx != null) {
    const p = slice[idx];
    return fullDate(p.date) + (idx < slice.length - 1 ? ` · ${t.toToday} ${sign(end.rate - p.rate)}` : "");
  }
  const d = end.rate - first.rate;
  const pct = first.rate ? Math.abs((d / first.rate) * 100).toFixed(2) : "0.00";
  return `${sign(d)} · ${dec(pct)}% · ${S.range < 4 ? t.periods[S.range] : t.since(first.date.slice(0, 4))}`;
}
function drawChart(slice) {
  const el = $("chart");
  const W = el.clientWidth || 700, H = el.clientHeight || 360;
  const top = 28, bottom = H - 28, padR = 22;
  const lo = Math.min(...slice.map((p) => p.rate)), hi = Math.max(...slice.map((p) => p.rate));
  const span = hi - lo || 1;
  const x = (i) => (i * W) / (slice.length - 1);
  const y = (v) => top + (1 - (v - lo) / span) * (bottom - top);
  const d = slice.map((p, i) => `${i ? "L" : "M"}${x(i).toFixed(1)},${y(p.rate).toFixed(1)}`).join("");
  const lx = x(slice.length - 1), ly = y(slice[slice.length - 1].rate);
  el.innerHTML = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="${esc(L()["hist.cbu"])}">
    <defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2F7A68" stop-opacity=".18"/><stop offset="1" stop-color="#2F7A68" stop-opacity="0"/></linearGradient></defs>
    <line x1="0" x2="${W}" y1="${top}" y2="${top}" stroke="rgba(18,23,26,.07)" stroke-dasharray="3 5"/>
    <line x1="0" x2="${W}" y1="${bottom}" y2="${bottom}" stroke="rgba(18,23,26,.07)" stroke-dasharray="3 5"/>
    <path d="${d}L${W},${H}L0,${H}Z" fill="url(#g)"/>
    <path class="ln" d="${d}" fill="none" stroke="#2F7A68" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round"/>
    <text class="ylab" x="${W - padR}" y="${top - 10}" text-anchor="end">${rateFmt(hi)}</text>
    <text class="ylab" x="${W - padR}" y="${bottom + 20}" text-anchor="end">${rateFmt(lo)}</text>
    <line id="scrubL" x1="0" x2="0" y1="0" y2="${H}" stroke="rgba(18,23,26,.25)" stroke-width="1" opacity="0"/>
    <circle id="scrubC" r="6" fill="#12171A" stroke="#fff" stroke-width="3" opacity="0"/>
    <circle cx="${lx}" cy="${ly}" r="5" fill="#2F7A68" stroke="#fff" stroke-width="3" id="endDot"/>
  </svg>`;
  const ln = el.querySelector(".ln");
  if (!reduced && ln.getTotalLength) {
    const len = ln.getTotalLength();
    ln.style.strokeDasharray = len; ln.style.strokeDashoffset = len;
    ln.getBoundingClientRect();
    ln.style.transition = "stroke-dashoffset .8s cubic-bezier(.2,.8,.2,1)";
    ln.style.strokeDashoffset = 0;
  }
  const end = slice[slice.length - 1];
  const tRate = $("tRate");
  animateNumber(tRate, end.rate, rateFmt);
  setText("tCap", trendCaption(slice, null));
  const sl = el.querySelector("#scrubL"), sc = el.querySelector("#scrubC"), ed = el.querySelector("#endDot");
  const at = (cx) => { const r = el.getBoundingClientRect(); return Math.max(0, Math.min(slice.length - 1, Math.round(((cx - r.left) / r.width) * (slice.length - 1)))); };
  const show = (i) => {
    const px = x(i), py = y(slice[i].rate);
    sl.setAttribute("x1", px); sl.setAttribute("x2", px); sl.setAttribute("opacity", 1);
    sc.setAttribute("cx", px); sc.setAttribute("cy", py); sc.setAttribute("opacity", 1);
    ed.setAttribute("opacity", 0);
    numAnim.set(tRate, slice[i].rate);
    tRate.textContent = rateFmt(slice[i].rate);
    setText("tCap", trendCaption(slice, i));
  };
  const hide = () => {
    sl.setAttribute("opacity", 0); sc.setAttribute("opacity", 0); ed.setAttribute("opacity", 1);
    numAnim.set(tRate, end.rate);
    tRate.textContent = rateFmt(end.rate);
    setText("tCap", trendCaption(slice, null));
  };
  el.onpointermove = (e) => show(at(e.clientX));
  el.onpointerdown = (e) => show(at(e.clientX));
  el.onpointerleave = hide;
  el.onpointerup = (e) => { if (e.pointerType !== "mouse") hide(); };
  el.onpointercancel = hide;
}
function renderSpread() {
  const d = S.rates[S.cur];
  if (!d) return;
  const buy = d.banks.filter((b) => b.buy > 0).sort((a, b) => b.buy - a.buy)[0];
  const sell = d.banks.filter((b) => b.sell > 0).sort((a, b) => a.sell - b.sell)[0];
  setText("spBuy", buy ? rateFmt(buy.buy) : "—");
  setText("spBuyBank", buy?.bank || "");
  setText("spSell", sell ? rateFmt(sell.sell) : "—");
  setText("spSellBank", sell?.bank || "");
}

/* ---------- misc ---------- */

let toastTimer;
function toast(msg) {
  const el = $("toast");
  el.textContent = msg; el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.hidden = true), 2200);
}
function showError() { $("tbody").innerHTML = `<tr><td colspan="5" class="empty">${esc(L().error)}</td></tr>`; }
function onLocated() {
  S.sort = "km";
  renderConv();
  renderTable();
  showUser();
  if (S.view === "map") renderMap();
}

function applyLang() {
  const t = L();
  document.documentElement.lang = S.lang;
  document.querySelectorAll("[data-i18n]").forEach((el) => { const v = t[el.dataset.i18n]; if (typeof v === "string") el.textContent = v; });
  document.querySelectorAll(".langs button").forEach((b) => b.classList.toggle("on", b.dataset.lang === S.lang));
  document.title = `Qulay Kurs — ${t.title[S.cur].toLowerCase()}`;
  renderAll();
  if (S.sheetBank) { const list = ranked(S.rates[S.cur]); const b = list.find((x) => x.bank === S.sheetBank); if (b) renderSheet(b, list); }
}

function bind() {
  addEventListener("hashchange", route);
  document.querySelectorAll(".langs button").forEach((b) => b.addEventListener("click", () => { S.lang = b.dataset.lang; store.set("lang", S.lang); applyLang(); }));
  document.addEventListener("click", (e) => {
    const cur = e.target.closest(".curSeg button, .cbu-c");
    if (cur) return setCur(cur.dataset.cur);
    const mode = e.target.closest("#modeSeg button, #mapModeSeg button");
    if (mode) return setMode(mode.dataset.mode);
  });
  const amt = $("amount");
  amt.addEventListener("input", () => {
    const digits = amt.value.replace(/\D/g, "").slice(0, 9);
    const before = amt.value.slice(0, amt.selectionStart).replace(/\D/g, "").length;
    amt.value = digits ? group(Number(digits)) : "";
    let i = 0, seen = 0;
    while (i < amt.value.length && seen < before) { if (/\d/.test(amt.value[i])) seen++; i++; }
    amt.setSelectionRange(i, i);
    setAmount(Number(digits), true);
  });
  amt.addEventListener("focus", () => amt.select());
  $("quick").addEventListener("click", (e) => { const v = e.target.closest("button")?.dataset.v; if (v) setAmount(Number(v)); });
  $("search").addEventListener("input", (e) => { S.query = e.target.value; renderTable(); });
  document.querySelector(".tbl thead").addEventListener("click", (e) => {
    const s = e.target.closest("button")?.dataset.sort;
    if (s) { S.sort = s; renderTable(); }
  });
  $("tbody").addEventListener("click", (e) => { const r = e.target.closest("tr[data-bank]"); if (r) openSheet(r.dataset.bank); });
  $("nearBtn").addEventListener("click", async () => {
    if (S.user) { S.sort = S.sort === "km" ? "best" : "km"; renderTable(); return; }
    const btn = $("nearBtn");
    btn.classList.add("busy");
    try { await locate(); await loadBranches(); onLocated(); } catch { toast(L().locFail); }
    btn.classList.remove("busy");
  });
  $("sideList").addEventListener("click", (e) => {
    const it = e.target.closest(".si");
    if (!it) return;
    S.hl = it.dataset.bank;
    if (it.dataset.lat && S.map) S.map.flyTo({ center: [+it.dataset.lng, +it.dataset.lat], zoom: Math.max(S.map.getZoom(), 15) });
    else renderMap();
  });
  $("scrim").addEventListener("click", closeSheet);
  $("shClose").addEventListener("click", closeSheet);
  document.addEventListener("keydown", (e) => { if (e.key === "Escape") closeSheet(); });
  $("locateBtn").addEventListener("click", async () => {
    const btn = $("locateBtn");
    btn.classList.add("busy");
    try {
      await locate();
      await ensureMap();
      onLocated();
      S.map?.flyTo({ center: [S.user[1], S.user[0]], zoom: 14 });
    } catch { toast(L().locFail); }
    btn.classList.remove("busy");
  });
  $("ranges").addEventListener("click", (e) => { const i = e.target.closest("button")?.dataset.i; if (i != null) { S.range = Number(i); renderHistory(); } });
  let rz;
  addEventListener("resize", () => { clearTimeout(rz); rz = setTimeout(() => { if (S.view === "history") renderHistory(); }, 150); });
}

async function init() {
  setText("year", new Date().getFullYear());
  $("amount").value = group(S.amount);
  fitAmount();
  renderQuick();
  bind();
  applyLang();
  route();
  if (store.get("loc") === "1" && navigator.permissions) {
    navigator.permissions.query({ name: "geolocation" }).then((p) => {
      if (p.state === "granted") locate().then(() => loadBranches()).then(() => { renderConv(); renderTable(); showUser(); }).catch(() => {});
    }).catch(() => {});
  }
  try { await loadRates(S.cur); } catch { showError(); }
  renderAll();
  for (const c of CURS) loadRates(c).then(renderCbu).catch(() => {});
  loadBranches().then(() => { renderConv(); renderTable(); if (S.view === "map") renderMap(); }).catch(() => {});
  setInterval(async () => {
    S.rates = {}; S.bankHist = {};
    for (const k of Object.keys(inflight)) if (k[0] === "r" || k[0] === "b") delete inflight[k];
    try { await Promise.all(CURS.map(loadRates)); renderAll(); } catch { /* keep old */ }
  }, 10 * 60 * 1000);
}

init();
