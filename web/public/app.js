// qulaykurs.uz — the web version of the app. Reads the same public API (api.qulaykurs.uz, rebuilt hourly).
// Texts (T) and bank-name helpers live in shared.js, which build.mjs also reads to pre-render the pages.
"use strict";

const API = "https://api.qulaykurs.uz";
const TASHKENT = [41.3111, 69.2797]; // [lat, lng]
const CURS = ["USD", "EUR", "RUB"];
const MAX_MAP_CANDS = 5000; // hard cap on candidates before the project+cluster pass (linear, via a screen-space grid)
const MAP_STYLES = { light: "https://tiles.openfreemap.org/styles/positron", dark: "https://tiles.openfreemap.org/styles/dark" };
const ROUTES = { "": "rates", "xarita": "map", "tarix": "history" };
let themeTransitionId = 0;

const store = {
  get(k) { try { return localStorage.getItem(k); } catch { return null; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch { /* private mode */ } }
};

// On a pre-rendered page (built by ../build.mjs) the URL decides language and currency: window.QK is
// { lang, cur, urls: { uz: { USD: "/", … }, … }, titles: { … } }. Opened as plain index.html there is no QK.
const QK = window.QK || null;

function detectLang() {
  if (QK) return QK.lang;
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
  dark: store.get("theme") ? store.get("theme") === "dark" : window.matchMedia?.("(prefers-color-scheme: dark)").matches === true,
  view: "rates",
  mode: store.get("mode") === "buy" ? "buy" : "sell",
  cur: QK ? QK.cur : CURS.includes(store.get("cur")) ? store.get("cur") : "USD",
  amount: Number(store.get("amount")) || 100,
  sort: "best", query: "",
  rates: {}, history: {}, bankHist: {},
  branches: null, user: null,
  range: 1, sheetBank: null, histGold: false, gold: null, goldHist: null,
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

function badgeHTML(name, extra = "") {
  const slug = logoSlug(name);
  const fallback = esc(initials(name));
  return slug
    ? `<span class="badge${extra ? ` ${extra}` : ""}"><img src="/bank-logos/${slug}.png" alt="${esc(name)} logosi" onerror="this.hidden=true;this.nextElementSibling.hidden=false"><span class="badge-fallback" hidden>${fallback}</span></span>`
    : `<span class="badge${extra ? ` ${extra}` : ""}">${fallback}</span>`;
}
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
const loadGold = () => S.gold ? Promise.resolve(S.gold) : once("gold", async () => (S.gold = await getJSON("/api/gold")));
/** CBU price per gram, taken from the 10 g bar (the most common size): archived snapshots since 2020, daily since Oct 2026. */
const loadGoldHist = () => S.goldHist ? Promise.resolve(S.goldHist) : once("goldh", async () => (S.goldHist = ((await getJSON("/api/gold/history")).points || [])
  .filter((p) => p.bars?.["10"]?.sell > 0)
  .map((p) => ({ date: p.date, rate: p.bars["10"].sell / 10, buyback: p.bars["10"].buyback / 10 }))));
function normalizeBranchCoordinate(value, min, max) {
  const n = Number(value);
  if (Number.isFinite(n) && n >= min && n <= max) return n;

  // bank.uz has a few branch coordinates with the decimal point stripped
  // (for example 405351069 instead of 40.5351069). Restore the decimal after
  // the two degree digits, then accept it only if it lands in Uzbekistan.
  const digits = String(value ?? "").trim().replace(/^[+]/, "");
  if (!/^\d{3,}$/.test(digits)) return null;
  const repaired = Number(`${digits.slice(0, 2)}.${digits.slice(2)}`);
  return Number.isFinite(repaired) && repaired >= min && repaired <= max ? repaired : null;
}
const loadBranches = () => S.branches ? Promise.resolve(S.branches) : once("br", async () => {
  const branches = (await getJSON("/api/branches")).branches || [];
  S.branches = branches.map((branch) => ({
    ...branch,
    lat: normalizeBranchCoordinate(branch.lat, 37, 46),
    lng: normalizeBranchCoordinate(branch.lng, 55, 74)
  })).filter((branch) => branch.lat != null && branch.lng != null);
  return S.branches;
});

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
  if (S.view === "map") {
    refreshMapData();
    ensureMap().then(() => { syncMapTheme(); renderMapWhenReady(); }).catch((error) => console.error("Map initialization failed:", error));
  }
  if (S.view === "history") renderHistory();
  scrollTo(0, 0);
}

/* ---------- shared controls ---------- */

function syncControls() {
  document.querySelectorAll(".curSeg button").forEach((b) => b.classList.toggle("on", b.closest("#histSeg") && S.histGold ? b.hasAttribute("data-gold") : b.dataset.cur === S.cur));
  document.querySelectorAll("#modeSeg button, #mapModeSeg button").forEach((b) => b.classList.toggle("on", b.dataset.mode === S.mode));
}
async function setCur(cur) {
  if (cur === S.cur && S.rates[cur]) return;
  S.cur = cur;
  store.set("cur", cur);
  syncTitle();
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
  renderGold();
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
  $("bestBadge").outerHTML = badgeHTML(best.bank).replace("class=\"badge", "id=\"bestBadge\" class=\"badge");
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

/* ---------- all CBU currencies (sheet) ---------- */

const AC_DEFAULT_FAVS = ["CNY", "KZT", "TRY", "AED", "KRW", "GBP"];
const AC = {
  list: null, query: "", toSom: true, amount: 100,
  sel: store.get("acSel") || "CNY",
  favs: (() => { try { const v = JSON.parse(store.get("acFavs")); return Array.isArray(v) ? v : AC_DEFAULT_FAVS; } catch { return AC_DEFAULT_FAVS; } })()
};
// Currencies that bank.uz publishes bank cash rates for besides the main tabs (backend EXTRA_RATES).
const AC_BANK_CURS = ["GBP", "KZT"];
const AB = { sell: true };
const loadAcBanks = (cur) => once("r" + cur, async () => (S.rates[cur] = await getJSON(`/api/rates/${cur.toLowerCase()}`)));
const loadCbuAll = () => AC.list ? Promise.resolve(AC.list) : once("cbuall", async () => (AC.list = (await getJSON("/api/cbu/all")).currencies || []));
const acName = (c) => c.names?.[S.lang] || c.names?.en || c.code;
const acRate = (c) => c.rate / (c.nominal || 1);
/** "1 803" for big rates, "8,21" / "0,0821" for small ones (KRW, JPY per unit). */
const acRateFmt = (v) => (v >= 1000 ? group(v) : dec(v >= 1 ? v.toFixed(2) : v.toFixed(4)));
const acEsc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));

function renderAllBtn() {
  const t = L();
  setText("allCurSub", AC.list ? t.allSub(AC.list.length) : t["hist.cbu"]);
  setText("allCurIc", AC.list ? `+${Math.max(0, AC.list.length - CURS.length)}` : "+");
}
function renderAllConv() {
  const t = L();
  const c = AC.list?.find((x) => x.code === AC.sel);
  if (!c) { setText("acRes", "—"); setText("acRate", ""); return; }
  const r = acRate(c);
  setText("acFrom", AC.toSom ? c.code : t["all.som"]);
  setText("acTo", AC.toSom ? t["all.som"] : c.code);
  const out = AC.toSom ? AC.amount * r : AC.amount / r;
  setText("acRes", out >= 100 || out === 0 ? group(out) : acRateFmt(out));
  setText("acRate", `1 ${c.code} = ${acRateFmt(r)} ${t["all.som"]} · ${acName(c)}`);
}
function renderAllList() {
  const t = L();
  if (!AC.list) { $("acList").innerHTML = `<div class="cbu-i skel-box"></div><div class="cbu-i skel-box"></div><div class="cbu-i skel-box"></div>`; return; }
  const q = AC.query.trim().toLowerCase();
  const match = (c) => !q || c.code.toLowerCase().includes(q) || Object.values(c.names || {}).some((n) => String(n).toLowerCase().includes(q));
  const items = AC.list.filter(match);
  const favs = AC.favs.map((code) => items.find((c) => c.code === code)).filter(Boolean);
  const rest = items.filter((c) => !AC.favs.includes(c.code));
  const row = (c) => {
    const d = c.diff / (c.nominal || 1);
    const cls = d > 0 ? "up-t" : d < 0 ? "down-t" : "";
    const fav = AC.favs.includes(c.code);
    return `<div class="ac-i${c.code === AC.sel ? " on" : ""}" role="button" tabindex="0" data-code="${acEsc(c.code)}">
      <span class="ac-sym">${acEsc(c.code)}</span>
      <span class="ac-n"><b>${acEsc(acName(c))}</b><small>${c.nominal > 1 ? `${c.nominal} ${acEsc(c.code)} = ${acRateFmt(c.rate)}` : `1 ${acEsc(c.code)}`}${AC_BANK_CURS.includes(c.code) && S.rates[c.code]?.banks?.length ? ` · <span class="ac-bk">${t.abBadge(S.rates[c.code].banks.length)}</span>` : ""}</small></span>
      <span class="ac-r"><b>${acRateFmt(acRate(c))}</b><em class="${cls}">${d ? `${d > 0 ? "▲" : "▼"} ${acRateFmt(Math.abs(d))}` : ""}</em></span>
      <button type="button" class="ac-star${fav ? " on" : ""}" data-star="${acEsc(c.code)}" aria-label="${t["all.fav"]}" aria-pressed="${fav}"><svg width="18" height="18" viewBox="0 0 20 20" aria-hidden="true"><path d="M10 2.6l2.3 4.7 5.1.7-3.7 3.6.9 5.1L10 14.3l-4.6 2.4.9-5.1L2.6 8l5.1-.7z" fill="${fav ? "currentColor" : "none"}" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg></button>
    </div>`;
  };
  let html = "";
  if (favs.length) html += `<h4 class="ac-h">${t["all.favs"]}</h4>` + favs.map(row).join("");
  if (rest.length) html += `<h4 class="ac-h">${t["all.all"]}</h4>` + rest.map(row).join("");
  $("acList").innerHTML = html || `<p class="muted ac-empty">${t["all.empty"]}</p>`;
}
/** Bank cash rates for the selected currency, only for banks that publish one. */
function renderAcBanks() {
  const t = L();
  const box = $("acBanks");
  const cur = AC.sel;
  if (!AC.list) { box.hidden = true; return; }
  box.hidden = false;
  if (!AC_BANK_CURS.includes(cur)) { box.innerHTML = `<p class="ab-none">${t["ab.none"]}</p>`; return; }
  const d = S.rates[cur];
  if (!d) {
    box.innerHTML = `<div class="cbu-i skel-box"></div><div class="cbu-i skel-box"></div>`;
    loadAcBanks(cur).then(() => { if (S.allOpen && AC.sel === cur) { renderAcBanks(); renderAllList(); } }).catch(() => { box.hidden = true; });
    return;
  }
  const field = AB.sell ? "buy" : "sell";
  const banks = d.banks.filter((b) => Number.isFinite(b[field])).sort((a, b) => (AB.sell ? b[field] - a[field] : a[field] - b[field]));
  if (!banks.length) { box.innerHTML = `<p class="ab-none">${t["ab.none"]}</p>`; return; }
  const best = banks[0][field];
  const fmt = (v) => { const r = Math.round(v * 100) / 100; return Number.isInteger(r) ? group(r) : dec(r.toFixed(2)); };
  const rows = banks.map((b) => {
    const gap = Math.abs(b[field] - best);
    const top = gap < 1e-9;
    return `<div class="ab-i${top ? " top" : ""}">${badgeHTML(b.bank, "sm")}<b class="ab-n">${esc(shortName(b.bank))}</b><b class="ab-r">${acRateFmt(b[field])}</b><em>${top ? t["ab.best"] : `${AB.sell ? "−" : "+"}${fmt(gap)}`}</em></div>`;
  }).join("");
  box.innerHTML = `<div class="ab-head"><h4 class="ac-h">${t.abTitle(d.banks.length)}</h4>
    <div class="ab-seg" role="tablist"><button type="button" role="tab" data-ab="sell" aria-selected="${AB.sell}" class="${AB.sell ? "on" : ""}">${t["ab.sell"]}</button><button type="button" role="tab" data-ab="buy" aria-selected="${!AB.sell}" class="${AB.sell ? "" : "on"}">${t["ab.buy"]}</button></div></div>
    <small class="ab-hint">${t.abHint(AB.sell)}</small>${rows}`;
}
function renderAllSheet() {
  const t = L();
  const date = AC.list?.[0]?.date;
  setText("allNote", t.allNote(date || "…"));
  $("acSearch").placeholder = t["all.search"];
  $("acSwap").setAttribute("aria-label", t["all.swap"]);
  renderAllConv();
  renderAcBanks();
  renderAllList();
}
async function openAll() {
  S.allOpen = true;
  renderAllSheet();
  $("scrim").hidden = false;
  $("allSheet").hidden = false;
  requestAnimationFrame(() => { $("scrim").classList.add("on"); $("allSheet").classList.add("on"); });
  document.body.style.overflow = "hidden";
  $("allClose").focus({ preventScroll: true });
  // Static files, so the "N ta bankda" marks in the list cost nothing to fetch up front.
  for (const cur of AC_BANK_CURS) loadAcBanks(cur).then(() => { if (S.allOpen) renderAllList(); }).catch(() => {});
  try { await loadCbuAll(); } catch { toast(L().error); }
  renderAllBtn();
  if (S.allOpen) renderAllSheet();
}
function closeAll() {
  if ($("allSheet").hidden) return;
  S.allOpen = false;
  $("allSheet").classList.remove("on");
  if (!S.sheetBank) { $("scrim").classList.remove("on"); document.body.style.overflow = ""; }
  setTimeout(() => { if (!S.allOpen) { $("allSheet").hidden = true; if (!S.sheetBank) $("scrim").hidden = true; } }, 300);
}
function bindAll() {
  $("allCurBtn").addEventListener("click", openAll);
  $("allClose").addEventListener("click", closeAll);
  $("acSearch").addEventListener("input", (e) => { AC.query = e.target.value; renderAllList(); });
  $("acAmount").addEventListener("input", (e) => {
    const el = e.target;
    const digits = el.value.replace(/\D/g, "").slice(0, 13);
    AC.amount = Number(digits) || 0;
    el.value = digits ? group(AC.amount) : "";
    renderAllConv();
  });
  $("acAmount").addEventListener("focus", (e) => e.target.select());
  $("acSwap").addEventListener("click", () => { AC.toSom = !AC.toSom; renderAllConv(); });
  $("acBanks").addEventListener("click", (e) => {
    const b = e.target.closest("[data-ab]");
    if (!b) return;
    AB.sell = b.dataset.ab === "sell";
    renderAcBanks();
  });
  $("acList").addEventListener("click", (e) => {
    const star = e.target.closest("[data-star]");
    if (star) {
      const code = star.dataset.star;
      AC.favs = AC.favs.includes(code) ? AC.favs.filter((c) => c !== code) : [...AC.favs, code];
      store.set("acFavs", JSON.stringify(AC.favs));
      renderAllList();
      renderAllBtn();
      return;
    }
    const it = e.target.closest(".ac-i");
    if (!it) return;
    AC.sel = it.dataset.code;
    store.set("acSel", AC.sel);
    renderAllConv();
    renderAcBanks();
    renderAllList();
    $("allSheet").scrollTo({ top: 0, behavior: "smooth" });
  });
  $("acList").addEventListener("keydown", (e) => { if ((e.key === "Enter" || e.key === " ") && e.target.classList.contains("ac-i")) { e.preventDefault(); e.target.click(); } });
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
      <td class="c-bank"><span class="bn">${badgeHTML(b.bank, isBest ? "accent" : "")}<span><b>${esc(b.bank)}</b><small>${esc(sub)}</small></span></span></td>
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
  $("shBadge").outerHTML = badgeHTML(bank.bank, bank.rate === best.rate ? "accent" : "").replace("class=\"badge", "id=\"shBadge\" class=\"badge");
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
let mapRenderFrame = 0;
let mapDarkMode = null;
function scheduleMapRender() {
  if (mapRenderFrame) return;
  mapRenderFrame = requestAnimationFrame(() => {
    mapRenderFrame = 0;
    // Marker and sidebar rendering does not depend on the style finishing every
    // tile/source update. In Dia, isStyleLoaded() can remain false after the
    // map is visible, leaving the map blank until the next moveend (zoom/pan).
    if (S.view !== "map" || !S.map) return;
    S.map.resize();
    renderMap();
  });
}
function renderMapWhenReady() {
  if (S.view !== "map" || !S.map) return;
  S.map.resize();
  renderMap();
}
function refreshMapData() {
  const currency = S.cur;
  loadRates(currency).then(() => {
    if (S.cur === currency) renderMapWhenReady();
  }).catch(() => {});
  loadBranches().then(renderMapWhenReady).catch(() => {});
}
function addMousePanFallback(map) {
  const canvas = map.getCanvas();
  let drag = null;
  canvas.addEventListener("mousedown", (e) => {
    if (e.button !== 0) return;
    const c = map.getCenter();
    drag = { x: e.clientX, y: e.clientY, startX: e.clientX, startY: e.clientY, lng: c.lng, lat: c.lat, fallback: false };
  });
  window.addEventListener("mousemove", (e) => {
    if (!drag) return;
    const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
    const c = map.getCenter();
    const nativePanMoved = Math.abs(c.lng - drag.lng) > 1e-8 || Math.abs(c.lat - drag.lat) > 1e-8;
    if (!drag.fallback && nativePanMoved) { drag = null; return; }
    if (!drag.fallback && Math.hypot(e.clientX - drag.startX, e.clientY - drag.startY) < 4) return;
    if (!drag.fallback) { drag.fallback = true; map.dragPan.disable(); }
    map.panBy([-dx, -dy], { duration: 0 });
    drag.x = e.clientX; drag.y = e.clientY;
  });
  window.addEventListener("mouseup", () => {
    if (!drag) return;
    const usedFallback = drag.fallback;
    drag = null;
    if (usedFallback) map.dragPan.enable();
  });

  // Some mobile browsers take over one-finger gestures even when MapLibre's
  // touch handler is enabled. Keep pinch zoom native, and use panBy only when
  // MapLibre did not move the map for a single-finger drag.
  let touchDrag = null;
  canvas.addEventListener("pointerdown", (e) => {
    if (e.pointerType !== "touch") return;
    if (!e.isPrimary) {
      if (touchDrag?.fallback) map.dragPan.enable();
      touchDrag = null;
      return;
    }
    const c = map.getCenter();
    touchDrag = { x: e.clientX, y: e.clientY, startX: e.clientX, startY: e.clientY, lng: c.lng, lat: c.lat, fallback: false };
  });
  window.addEventListener("pointermove", (e) => {
    if (!touchDrag || e.pointerType !== "touch" || !e.isPrimary) return;
    const dx = e.clientX - touchDrag.x, dy = e.clientY - touchDrag.y;
    const c = map.getCenter();
    const nativePanMoved = Math.abs(c.lng - touchDrag.lng) > 1e-8 || Math.abs(c.lat - touchDrag.lat) > 1e-8;
    if (!touchDrag.fallback && nativePanMoved) { touchDrag = null; return; }
    if (!touchDrag.fallback && Math.hypot(e.clientX - touchDrag.startX, e.clientY - touchDrag.startY) < 4) return;
    if (!touchDrag.fallback) { touchDrag.fallback = true; map.dragPan.disable(); }
    map.panBy([-dx, -dy], { duration: 0 });
    touchDrag.x = e.clientX; touchDrag.y = e.clientY;
  });
  window.addEventListener("pointerup", (e) => {
    if (e.pointerType !== "touch" || !touchDrag) return;
    const usedFallback = touchDrag.fallback;
    touchDrag = null;
    if (usedFallback) map.dragPan.enable();
  });
}
function ensureMap() {
  mapReady ||= new Promise((resolve) => {
    const start = () => {
      const c = S.user || TASHKENT;
      const map = new maplibregl.Map({
        container: "mapgl", style: MAP_STYLES[S.dark ? "dark" : "light"], center: [c[1], c[0]], zoom: S.user ? 14 : 12.6,
        attributionControl: { compact: true }, dragRotate: false, pitchWithRotate: false
      });
      map.dragPan.enable();
      map.touchZoomRotate.enable();
      map.touchZoomRotate.disableRotation();
      addMousePanFallback(map);
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-right");
      S.map = map;
      mapDarkMode = S.dark;
      // Coalesced: a mouse-wheel zoom fires one moveend per tick, and with a large branch dataset each
      // render rebuilds up to 150 DOM markers — running that for every tick in a fast scroll backs up
      // the main thread into a visible freeze. Only the last moveend in a burst actually renders.
      let moveendTimer = null;
      map.on("moveend", () => {
        clearTimeout(moveendTimer);
        moveendTimer = setTimeout(renderMap, 80);
      });
      map.on("resize", scheduleMapRender);
      // Render once after MapLibre settles its first visible viewport too. The
      // initial load can happen while the map panel is being revealed from a
      // previously hidden route, before its final bounds are available.
      map.once("idle", renderMapWhenReady);
      map.on("load", () => {
        if (S.user) showUser();
        focusOnMap();
        resolve();
        renderMapWhenReady();
        refreshMapData();
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
/** Pill footprint in screen px (marker anchored at its bottom centre). */
const pillW = (label, rate, count) => 24 + (label.length + rate.length) * 7.4 + (count > 1 ? 30 : 0);
const PILL_H = 34;
/**
 * Every branch in view, grouped so no two pills overlap: a branch whose pill would collide with an
 * already-placed one joins that group instead of being dropped. Candidates go best rate first, so each
 * group's pill shows the best rate among its members, plus how many branches it holds.
 */
function clusterBranches(map, cands) {
  const CELL = 200;
  const grid = new Map();
  const clusters = [];
  for (const c of cands) {
    const p = map.project([c.b.lng, c.b.lat]);
    const gx = Math.floor(p.x / CELL), gy = Math.floor(p.y / CELL);
    let hit = null;
    for (let dx = -1; dx <= 1 && !hit; dx++) for (let dy = -1; dy <= 1 && !hit; dy++) {
      for (const q of grid.get(`${gx + dx},${gy + dy}`) || []) {
        // Reserve room for the count badge on every pill, so a group never grows into its neighbour.
        const w = (q.w + pillW(shortName(c.bank.bank), rateFmt(c.bank.rate), 2)) / 2;
        if (Math.abs(p.x - q.x) < w + 4 && Math.abs(p.y - q.y) < PILL_H + 8) { hit = q; break; }
      }
    }
    if (hit) { hit.members.push(c); continue; }
    const cl = { x: p.x, y: p.y, w: pillW(shortName(c.bank.bank), rateFmt(c.bank.rate), 2), members: [c] };
    clusters.push(cl);
    const k = `${gx},${gy}`;
    if (!grid.has(k)) grid.set(k, []);
    grid.get(k).push(cl);
  }
  return clusters;
}
let clusterPopup = null;
function closeClusterPopup() { clusterPopup?.remove(); clusterPopup = null; }
/** Members too close to split by zooming: list them in a popup, one row per branch. */
function openClusterPopup(cl, bestRate) {
  closeClusterPopup();
  const t = L();
  const rows = cl.members.map(({ bank, b }, i) => {
    const isBest = Math.abs(bank.rate - bestRate) < 0.005;
    const addr = addrParts(b.address)[0];
    return `<button type="button" class="cp-row" data-i="${i}">${badgeHTML(bank.bank, isBest ? "accent" : "")}
      <span><b>${esc(bank.bank)}</b>${addr ? `<small>${esc(addr)}</small>` : ""}</span>
      <span class="r"><b>${rateFmt(bank.rate)}</b>${isBest ? `<small class="best">${t.best}</small>` : ""}</span></button>`;
  }).join("");
  const el = document.createElement("div");
  el.className = "cp";
  el.innerHTML = `<p class="cp-h">${esc(t.branchesHere(cl.members.length))}</p><div class="cp-list">${rows}</div>`;
  el.addEventListener("click", (e) => {
    const r = e.target.closest(".cp-row");
    if (!r) return;
    const { bank } = cl.members[Number(r.dataset.i)];
    closeClusterPopup();
    openSheet(bank.bank);
  });
  const lead = cl.members[0].b;
  clusterPopup = new maplibregl.Popup({ offset: 44, maxWidth: "340px", className: "cp-pop", focusAfterOpen: false })
    .setLngLat([lead.lng, lead.lat]).setDOMContent(el).addTo(S.map);
  clusterPopup.on("close", () => { clusterPopup = null; });
}
function onClusterTap(cl, bestRate) {
  const map = S.map;
  if (cl.members.length === 1) return openSheet(cl.members[0].bank.bank);
  let w = Infinity, s = Infinity, e = -Infinity, n = -Infinity;
  for (const { b } of cl.members) { w = Math.min(w, b.lng); e = Math.max(e, b.lng); s = Math.min(s, b.lat); n = Math.max(n, b.lat); }
  // Zooming can only help if the members would actually separate before the map's zoom limit.
  const cam = map.cameraForBounds([[w, s], [e, n]], { padding: 90 });
  const maxZ = Math.min(map.getMaxZoom(), 18);
  if (!cam || map.getZoom() >= maxZ - 0.3 || (cam.zoom ?? maxZ) >= maxZ) return openClusterPopup(cl, bestRate);
  closeClusterPopup();
  map.flyTo({ center: cam.center, zoom: Math.min(cam.zoom, maxZ), duration: 600 });
}
/** Pins (every branch in view, grouped where they would overlap, best rate on top) and the side list. */
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
  const cands = [];
  bankLoop:
  for (const bank of list) {
    let brs = branchesOf(bank.bank);
    if (!brs.length && bank.branch?.lat) brs = [bank.branch];
    for (const b of brs) {
      if (bounds.contains([b.lng, b.lat])) cands.push({ bank, b });
      if (cands.length >= MAX_MAP_CANDS) break bankLoop;
    }
  }
  S.markers.forEach((m) => m.remove());
  S.markers = [];
  // `list` is already best-rate first; the highlighted bank is lifted ahead so its pill leads its group.
  cands.sort((a, b) => (b.bank.bank === S.hl) - (a.bank.bank === S.hl));
  const clusters = clusterBranches(map, cands);
  for (const cl of clusters) {
    if (S.markers.length >= 150) break;
    const { bank, b } = cl.members[0];
    const n = cl.members.length;
    const label = shortName(bank.bank);
    // MapLibre positions the marker element with its own transform, so the styled pill lives inside a bare wrapper.
    const el = document.createElement("div");
    el.className = "pin-wrap";
    const hl = cl.members.some((m) => m.bank.bank === S.hl);
    const cls = "pill" + (bank.rate === bestRate ? " best" : "") + (hl ? " hl" : "") + (n > 1 ? " multi" : "");
    const title = n > 1 ? `${bank.bank} · ${t.branchesHere(n)}` : bank.bank;
    el.innerHTML = `<button type="button" class="${cls}" title="${esc(title)}"><span>${esc(label)}</span><b>${rateFmt(bank.rate)}</b>${n > 1 ? `<i class="cnt">${n > 99 ? "99+" : n}</i>` : ""}</button>`;
    let markerPointerOpenedAt = 0;
    el.firstChild.addEventListener("pointerup", (e) => {
      if (e.button !== 0) return;
      markerPointerOpenedAt = performance.now();
      e.stopPropagation();
      onClusterTap(cl, bestRate);
    });
    el.firstChild.addEventListener("click", (e) => {
      e.stopPropagation();
      if (performance.now() - markerPointerOpenedAt < 500) return;
      onClusterTap(cl, bestRate);
    });
    const m = new maplibregl.Marker({ element: el, anchor: "bottom" }).setLngLat([b.lng, b.lat]).addTo(map);
    if (bank.rate === bestRate || hl) m.getElement().style.zIndex = 3;
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
      ${badgeHTML(bank.bank, isBest ? "accent" : "")}
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
  $("mcBadge").outerHTML = badgeHTML(top.bank).replace("class=\"badge", "id=\"mcBadge\" class=\"badge");
  setText("mcName", top.bank);
  setText("mcSub", t.mapBest(group(S.amount), unit(S.cur, S.amount)) + (S.user && nb ? ", " + t.km(kmFmt(distKm(S.user, [nb.lat, nb.lng]))) : ""));
  $("mcGo").href = nb ? directions(nb.lat, nb.lng) : "#";
}

/* ---------- history ---------- */

const RANGE_DAYS = [7, 30, 90, 365, 1825, null];
const DAY = 864e5;
const dayMs = (ymd) => Date.parse(ymd + "T00:00:00Z");
async function renderHistory() {
  const cur = S.cur, gold = S.histGold;
  const t = L();
  syncControls();
  const shown = S.range;
  $("ranges").innerHTML = t.ranges.map((r, i) => `<button type="button" data-i="${i}" class="${i === shown ? "on" : ""}">${r}</button>`).join("");
  setText("tLabel", gold ? t.goldLabel : t["hist.cbu"]);
  renderSpread();
  let pts;
  try { pts = await (gold ? loadGoldHist() : loadHistory(cur)); } catch { return; }
  if (cur !== S.cur || gold !== S.histGold || !pts?.length || S.view !== "history") return;
  const days = RANGE_DAYS[shown];
  const lastDay = dayMs(pts[pts.length - 1].date);
  let slice = days ? pts.filter((p) => dayMs(p.date) >= lastDay - days * DAY) : pts;
  if (slice.length < 2) slice = pts.slice(-2);
  drawChart(slice);
}
const histRange = () => S.range;
const signed = (d) => (Math.abs(d) < 0.005 ? "±0" : (d < 0 ? "−" : "+") + rateFmt(Math.abs(d)));
const pctOf = (d, base) => dec((base ? Math.abs((d / base) * 100) : 0).toFixed(2)) + "%";
function trendCaption(slice, idx) {
  const t = L();
  const first = slice[0], end = slice[slice.length - 1];
  if (idx != null) {
    const p = slice[idx];
    return fullDate(p.date) + (idx < slice.length - 1 ? ` · ${t.toToday} ${signed(end.rate - p.rate)}` : "");
  }
  const d = end.rate - first.rate;
  const ri = histRange();
  return `${signed(d)} · ${pctOf(d, first.rate)} · ${ri < t.periods.length ? t.periods[ri] : t.since(first.date.slice(0, 4))}`;
}
function shortDate(ms, spanDays) {
  const dt = new Date(ms);
  const y = dt.getUTCFullYear(), m = dt.getUTCMonth(), d = dt.getUTCDate();
  const mo = MONTHS[S.lang][m];
  const moS = S.lang === "en" ? mo : mo.slice(0, 3);
  if (spanDays > 2.5 * 365) return String(y);
  if (spanDays > 120) return m === 0 ? String(y) : moS;
  return S.lang === "en" ? `${moS} ${d}` : `${d} ${moS}`;
}
/** ~[count] round y-axis steps covering [lo, hi]. */
function niceTicks(lo, hi, count) {
  const raw = (hi - lo) / count || 1;
  const mag = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map((k) => k * mag).find((s) => s >= raw) || 10 * mag;
  const out = [];
  for (let v = Math.ceil(lo / step) * step; v <= hi + 1e-9; v += step) out.push(+v.toFixed(6));
  return out;
}
/** Date ticks at calendar boundaries (days, weeks, months, quarters, years) that fit [maxTicks]. */
function timeTicks(t0, t1, maxTicks) {
  const span = (t1 - t0) / DAY;
  const steps = [[1, "d"], [2, "d"], [7, "d"], [14, "d"], [1, "m"], [2, "m"], [3, "m"], [6, "m"], [1, "y"], [2, "y"], [3, "y"], [5, "y"]];
  const per = { d: 1, m: 30.4, y: 365.25 };
  const [n, u] = steps.find(([k, un]) => span / (k * per[un]) <= maxTicks) || [10, "y"];
  const out = [];
  const s = new Date(t0);
  let c;
  if (u === "d") c = Date.UTC(s.getUTCFullYear(), s.getUTCMonth(), s.getUTCDate() + 1);
  else if (u === "m") c = Date.UTC(s.getUTCFullYear(), Math.ceil((s.getUTCMonth() + 1) / n) * n, 1);
  else c = Date.UTC(Math.ceil((s.getUTCFullYear() + 1) / n) * n, 0, 1);
  while (c <= t1 && out.length < 40) {
    out.push(c);
    const d = new Date(c);
    c = u === "d" ? c + n * DAY : u === "m" ? Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + n, 1) : Date.UTC(d.getUTCFullYear() + n, 0, 1);
  }
  return out;
}
function drawChart(slice) {
  const t = L();
  const el = $("chart");
  const W = el.clientWidth || 700, H = el.clientHeight || 360;
  const narrow = W < 480;
  const padL = narrow ? 16 : 22, padR = narrow ? 16 : 22, top = 34, bottom = H - 30;
  const rates = slice.map((p) => p.rate);
  const times = slice.map((p) => dayMs(p.date));
  const hiI = rates.indexOf(Math.max(...rates)), loI = rates.indexOf(Math.min(...rates));
  const hi = rates[hiI], lo = rates[loI];
  const pad = (hi - lo) * 0.08 || hi * 0.002 || 1;
  const ticks = niceTicks(lo - pad, hi + pad, narrow ? 4 : 6);
  const yLo = Math.min(lo - pad, ticks[0]), yHi = Math.max(hi + pad, ticks[ticks.length - 1]);
  const t0 = times[0], t1 = times[times.length - 1];
  const x = (ms) => padL + ((ms - t0) / (t1 - t0 || 1)) * (W - padL - padR);
  const y = (v) => top + (1 - (v - yLo) / (yHi - yLo)) * (bottom - top);
  const xs = times.map(x), ys = rates.map(y);
  const d = xs.map((px, i) => `${i ? "L" : "M"}${px.toFixed(1)},${ys[i].toFixed(1)}`).join("");
  const first = slice[0], end = slice[slice.length - 1];
  const change = end.rate - first.rate;
  const trend = Math.abs(change) < 0.005 ? "flat" : change > 0 ? "up" : "down";
  const avg = rates.reduce((a, b) => a + b, 0) / rates.length;
  const spanDays = (t1 - t0) / DAY;
  const xt = timeTicks(t0, t1, Math.max(2, Math.floor((W - padL - padR) / (narrow ? 70 : 96))));
  // Gridlines span the plot; their values sit on top of them at the left edge, so the line gets the full width.
  const yVals = ticks.filter((v) => y(v) > top + 6 && y(v) < bottom - 14);
  const yGrid = yVals.map((v) => `<line class="grid" x1="${padL}" x2="${W - padR}" y1="${y(v).toFixed(1)}" y2="${y(v).toFixed(1)}"/>`).join("");
  const yLabs = yVals.map((v) => `<text class="ylab" x="${padL}" y="${(y(v) - 6).toFixed(1)}">${rateFmt(v)}</text>`).join("");
  const xLabs = xt.filter((ms) => x(ms) > padL + 18 && x(ms) < W - padR - 18).map((ms) =>
    `<text class="xlab" x="${x(ms).toFixed(1)}" y="${bottom + 21}" text-anchor="middle">${esc(shortDate(ms, spanDays))}</text>`).join("");
  // High/low as small tags just off the line, nudged inward near the edges so they stay on the canvas.
  const extLabel = (i, above) => {
    const px = xs[i], py = ys[i];
    const txt = rateFmt(rates[i]);
    const w = txt.length * 7.4 + 16, h = 22;
    const bx = Math.max(padL, Math.min(W - padR - w, px - w / 2));
    const by = above ? Math.max(2, py - h - 9) : Math.min(bottom - h - 2, py + 9);
    return `<circle class="ext-dot" cx="${px.toFixed(1)}" cy="${py.toFixed(1)}" r="3.5"/>
      <rect class="ext-bg" x="${bx.toFixed(1)}" y="${by.toFixed(1)}" width="${w.toFixed(1)}" height="${h}" rx="7"/>
      <text class="ext" x="${(bx + w / 2).toFixed(1)}" y="${(by + 15).toFixed(1)}" text-anchor="middle">${txt}</text>`;
  };
  el.className = "chart t-" + trend + (S.histGold ? " t-gold" : "");
  el.innerHTML = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="${esc(t["hist.cbu"])}: ${esc(trendCaption(slice, null))}">
    <defs>
      <linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" class="fill-a"/><stop offset="1" class="fill-b"/></linearGradient>
      <clipPath id="chartClip"><rect x="${padL - 2}" y="0" width="${W - padL - padR + 4}" height="${H}"/></clipPath>
    </defs>
    ${yGrid}
    <line class="axis" x1="${padL}" x2="${W - padR}" y1="${bottom}" y2="${bottom}"/>
    ${xLabs}
    <g clip-path="url(#chartClip)">
      <path class="area" d="${d}L${xs[xs.length - 1].toFixed(1)},${bottom}L${xs[0].toFixed(1)},${bottom}Z" fill="url(#chartFill)"/>
      <path class="ln" d="${d}"/>
    </g>
    ${yLabs}
    ${hiI !== loI ? extLabel(hiI, true) + extLabel(loI, false) : ""}
    <line id="scrubL" class="scrub" x1="0" x2="0" y1="${top - 10}" y2="${bottom}" opacity="0"/>
    <circle id="scrubC" class="scrub-dot" r="6" opacity="0"/>
    <circle class="end-halo" cx="${xs[xs.length - 1]}" cy="${ys[ys.length - 1]}" r="10" id="endHalo"/>
    <circle class="end-dot" cx="${xs[xs.length - 1]}" cy="${ys[ys.length - 1]}" r="5" id="endDot"/>
  </svg><div class="tip" id="chartTip" hidden></div>`;
  const ln = el.querySelector(".ln");
  if (!reduced && ln.getTotalLength) {
    const len = ln.getTotalLength();
    ln.style.strokeDasharray = len; ln.style.strokeDashoffset = len;
    ln.getBoundingClientRect();
    ln.style.transition = "stroke-dashoffset .8s cubic-bezier(.2,.8,.2,1)";
    ln.style.strokeDashoffset = 0;
  }
  const tRate = $("tRate");
  animateNumber(tRate, end.rate, rateFmt);
  setText("tCap", trendCaption(slice, null));
  $("tCap").className = "caption " + trend;
  renderHistStats(slice, hiI, loI, avg);

  const sl = el.querySelector("#scrubL"), sc = el.querySelector("#scrubC"), tip = $("chartTip");
  const endEls = [el.querySelector("#endDot"), el.querySelector("#endHalo")];
  let cur = null;
  /** Nearest point by time (points are spaced unevenly: daily recently, weekly/monthly further back). */
  const at = (cx) => {
    const r = el.getBoundingClientRect();
    const px = ((cx - r.left) / r.width) * W;
    let lo = 0, hi = xs.length - 1;
    while (hi - lo > 1) { const m = (lo + hi) >> 1; if (xs[m] < px) lo = m; else hi = m; }
    return Math.abs(xs[lo] - px) <= Math.abs(xs[hi] - px) ? lo : hi;
  };
  const show = (i) => {
    cur = i;
    const px = xs[i], py = ys[i];
    sl.setAttribute("x1", px); sl.setAttribute("x2", px); sl.setAttribute("opacity", 1);
    sc.setAttribute("cx", px); sc.setAttribute("cy", py); sc.setAttribute("opacity", 1);
    endEls.forEach((e) => e.setAttribute("opacity", i === slice.length - 1 ? 1 : 0.35));
    numAnim.set(tRate, slice[i].rate);
    tRate.textContent = rateFmt(slice[i].rate);
    setText("tCap", trendCaption(slice, i));
    const prev = i > 0 ? slice[i].rate - slice[i - 1].rate : null;
    tip.innerHTML = `<small>${esc(fullDate(slice[i].date))}</small><b>${rateFmt(slice[i].rate)} <span>${esc(t.som)}</span></b>` +
      (prev != null ? `<em class="${prev > 0.004 ? "up" : prev < -0.004 ? "down" : ""}">${signed(prev)} ${esc(t.prevPt)}</em>` : "");
    tip.hidden = false;
    const r = el.getBoundingClientRect(), sx = r.width / W;
    const tw = tip.offsetWidth, th = tip.offsetHeight;
    let left = px * sx + 14;
    if (left + tw > r.width - 4) left = px * sx - tw - 14;
    tip.style.left = Math.max(4, left) + "px";
    tip.style.top = Math.max(4, Math.min(py * (r.height / H) - th / 2, r.height - th - 34)) + "px";
  };
  const hide = () => {
    cur = null;
    sl.setAttribute("opacity", 0); sc.setAttribute("opacity", 0);
    endEls.forEach((e) => e.setAttribute("opacity", 1));
    tip.hidden = true;
    numAnim.set(tRate, end.rate);
    tRate.textContent = rateFmt(end.rate);
    setText("tCap", trendCaption(slice, null));
  };
  el.tabIndex = 0;
  el.onpointermove = (e) => show(at(e.clientX));
  el.onpointerdown = (e) => show(at(e.clientX));
  el.onpointerleave = hide;
  el.onpointerup = (e) => { if (e.pointerType !== "mouse") hide(); };
  el.onpointercancel = hide;
  el.onblur = hide;
  el.onkeydown = (e) => {
    const last = slice.length - 1;
    const step = e.shiftKey ? Math.max(1, Math.round(slice.length / 10)) : 1;
    const next = { ArrowLeft: (cur ?? last + 1) - step, ArrowRight: (cur ?? last) + step, Home: 0, End: last }[e.key];
    if (e.key === "Escape") return hide();
    if (next == null) return;
    e.preventDefault();
    show(Math.max(0, Math.min(last, next)));
  };
}
function renderHistStats(slice, hiI, loI, avg) {
  const t = L();
  const first = slice[0], end = slice[slice.length - 1];
  const change = end.rate - first.rate;
  const cell = (label, value, sub, cls = "") => `<div class="ts"><small>${esc(label)}</small><b class="${cls}">${value}</b><span>${esc(sub)}</span></div>`;
  $("tStats").innerHTML =
    cell(t.stHigh, rateFmt(slice[hiI].rate), fullDate(slice[hiI].date)) +
    cell(t.stLow, rateFmt(slice[loI].rate), fullDate(slice[loI].date)) +
    cell(t.stAvg, rateFmt(avg), histRange() < t.periods.length ? t.periods[histRange()] : t.since(first.date.slice(0, 4))) +
    cell(t.stChg, signed(change), pctOf(change, first.rate), Math.abs(change) < 0.005 ? "" : change > 0 ? "up-t" : "down-t");
}
function renderSpread() {
  const t = L();
  setText("spBuyL", S.histGold ? t.goldSellL : t["spread.buy"]);
  setText("spSellL", S.histGold ? t.goldBuyL : t["spread.sell"]);
  setText("spNote", S.histGold ? t.goldSideNote : t["spread.note"]);
  if (S.histGold) {
    const g10 = S.gold?.bars?.find((b) => b.grams === 10);
    setText("spBuy", g10 ? group(g10.sell / 10) : "—");
    setText("spSell", g10 ? group(g10.buyback / 10) : "—");
    setText("spBuyBank", t.goldSrc);
    setText("spSellBank", t.goldSrc);
    if (!S.gold) loadGold().then(() => { if (S.histGold) renderSpread(); }).catch(() => {});
    return;
  }
  const d = S.rates[S.cur];
  if (!d) return;
  const buy = d.banks.filter((b) => b.buy > 0).sort((a, b) => b.buy - a.buy)[0];
  const sell = d.banks.filter((b) => b.sell > 0).sort((a, b) => a.sell - b.sell)[0];
  setText("spBuy", buy ? rateFmt(buy.buy) : "—");
  setText("spBuyBank", buy?.bank || "");
  setText("spSell", sell ? rateFmt(sell.sell) : "—");
  setText("spSellBank", sell?.bank || "");
}

/* ---------- gold ---------- */

/**
 * A minted gold bar standing upright: rounded body with a polished bevel, an inner panel stamped with the weight and
 * fineness. Heavier bars are drawn taller, like a row of real bars side by side. Gradient ids are shared (identical).
 */
const BAR_SIZES = [5, 10, 20, 50, 100];
function ingot(grams) {
  const i = Math.max(0, BAR_SIZES.indexOf(grams));
  const h = 30 + i * 4.5, w = h * 0.62;
  return `<svg class="ingot" viewBox="0 0 31 50" width="${w.toFixed(1)}" height="${h.toFixed(1)}" style="--h:${h.toFixed(1)}px" aria-hidden="true">
  <defs>
    <linearGradient id="barBody" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#FFF0B3"/><stop offset=".22" stop-color="#F2C95A"/><stop offset=".5" stop-color="#C98E22"/><stop offset=".72" stop-color="#F5D27A"/><stop offset="1" stop-color="#B07A16"/>
    </linearGradient>
    <linearGradient id="barPanel" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#F9DE8C"/><stop offset=".45" stop-color="#E6B649"/><stop offset="1" stop-color="#D19B2E"/>
    </linearGradient>
    <linearGradient id="barShine" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".55"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
  </defs>
  <rect x=".6" y=".6" width="29.8" height="48.8" rx="5" fill="url(#barBody)" stroke="#8C5F12" stroke-width=".8"/>
  <rect x="3.4" y="3.4" width="24.2" height="43.2" rx="3" fill="url(#barPanel)" stroke="#FFF2C2" stroke-width=".9"/>
  <rect x="3.4" y="3.4" width="24.2" height="43.2" rx="3" fill="none" stroke="#9C6B14" stroke-width=".5" opacity=".6" transform="translate(.5 .5)"/>
  <path d="M15.5 8.5l3.2 2v3.5h-6.4v-3.5z" fill="none" stroke="#9A6612" stroke-width=".8" opacity=".7"/>
  <text x="15.5" y="24.5" text-anchor="middle" font-family="Onest, system-ui, sans-serif" font-size="${grams >= 100 ? 7.4 : 8.4}" font-weight="800" fill="#7C520C" opacity=".85">${grams}g</text>
  <text x="15.5" y="31.5" text-anchor="middle" font-family="Onest, system-ui, sans-serif" font-size="4.6" font-weight="600" fill="#7C520C" opacity=".7">999,9</text>
  <path d="M7 36.5h17M7 39.5h17" stroke="#9A6612" stroke-width=".5" opacity=".45"/>
  <path d="M5 4.5L13 4.5L5 18z" fill="url(#barShine)" opacity=".7"/>
</svg>`;
}
function renderGold() {
  const g = S.gold;
  const card = $("goldCard");
  if (!g?.bars?.length) { card.hidden = true; return; }
  const t = L();
  card.hidden = false;
  const [dd, mm, yy] = (g.date || "").split(".");
  setText("goldSub", t.goldSub(dd && mm && yy ? fullDate(`${yy}-${mm}-${dd}`) : g.date || "", g.time || ""));
  $("goldBody").innerHTML = g.bars.map((b) => {
    const loss = b.sell ? ((b.sell - b.buyback) / b.sell) * 100 : 0;
    return `<tr>
      <td><span class="gold-bar"><span class="ingot-slot">${ingot(b.grams)}</span><b>${esc(t.grams(b.grams))}</b></span></td>
      <td class="num"><b>${group(b.sell)}</b></td>
      <td class="num">${b.buyback ? group(b.buyback) : "—"}</td>
      <td class="num muted">${group(b.sell / b.grams)}</td>
      <td class="num"><span class="gold-loss">−${dec(loss.toFixed(1))}%</span></td>
    </tr>`;
  }).join("");
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
  updateThemeControl();
  syncTitle();
  renderAll();
  renderAllBtn();
  if (S.allOpen) renderAllSheet();
  if (S.sheetBank) { const list = ranked(S.rates[S.cur]); const b = list.find((x) => x.bank === S.sheetBank); if (b) renderSheet(b, list); }
}

// The address and tab title follow the currency, so a shared or reloaded link shows what the user was looking at.
function syncTitle() {
  if (!QK) { document.title = `Qulay Kurs — ${L().title[S.cur].toLowerCase()}`; return; }
  document.title = QK.titles[S.lang][S.cur];
  const url = QK.urls[S.lang][S.cur];
  if (location.pathname !== url) history.replaceState(null, "", url + location.hash);
}

function updateThemeControl() {
  const label = S.dark ? L()["theme.light"] : L()["theme.dark"];
  $("themeToggle").setAttribute("aria-label", label);
  $("themeToggle").setAttribute("title", label);
}

function applyTheme() {
  document.documentElement.dataset.theme = S.dark ? "dark" : "light";
  document.querySelector('meta[name="theme-color"]').content = S.dark ? "#15130E" : "#FBF6E9";
  updateThemeControl();
}

function syncMapTheme() {
  if (!S.map || S.view !== "map" || mapDarkMode === S.dark) return;
  mapDarkMode = S.dark;
  S.map.setStyle(MAP_STYLES[S.dark ? "dark" : "light"]);
  S.map.once("style.load", scheduleMapRender);
}

function bind() {
  addEventListener("hashchange", route);
  window.matchMedia?.("(prefers-color-scheme: dark)").addEventListener?.("change", (event) => {
    if (!store.get("theme")) {
      S.dark = event.matches;
      applyTheme();
      syncMapTheme();
    }
  });
  $("themeToggle").addEventListener("click", (event) => {
    const button = $("themeToggle").getBoundingClientRect();
    const x = event.clientX || button.left + button.width / 2;
    const y = event.clientY || button.top + button.height / 2;
    S.dark = !S.dark;
    store.set("theme", S.dark ? "dark" : "light");
    document.documentElement.style.setProperty("--theme-x", `${x}px`);
    document.documentElement.style.setProperty("--theme-y", `${y}px`);
    if (document.startViewTransition && !matchMedia("(prefers-reduced-motion: reduce)").matches) {
      const id = ++themeTransitionId;
      document.documentElement.classList.remove("theme-to-dark", "theme-to-light");
      document.documentElement.classList.add(S.dark ? "theme-to-dark" : "theme-to-light");
      const transition = document.startViewTransition(applyTheme);
      const clearOrigin = () => {
        if (id !== themeTransitionId) return;
        document.documentElement.classList.remove("theme-to-dark", "theme-to-light");
        document.documentElement.style.removeProperty("--theme-x");
        document.documentElement.style.removeProperty("--theme-y");
        syncMapTheme();
      };
      transition.finished.then(clearOrigin, clearOrigin);
    } else {
      applyTheme();
      syncMapTheme();
    }
  });
  document.querySelectorAll(".langs button").forEach((b) => b.addEventListener("click", () => {
    store.set("lang", b.dataset.lang);
    if (QK) { if (b.dataset.lang !== S.lang) location.href = QK.urls[b.dataset.lang][S.cur] + location.hash; return; }
    S.lang = b.dataset.lang;
    applyLang();
  }));
  document.addEventListener("click", (e) => {
    if (e.target.closest("[data-gold]")) { S.histGold = true; return renderHistory(); }
    const cur = e.target.closest(".curSeg button, .cbu-c");
    if (cur) {
      const wasGold = S.histGold;
      S.histGold = false;
      if (wasGold && cur.dataset.cur === S.cur) renderHistory();
      return setCur(cur.dataset.cur);
    }
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
    openSheet(it.dataset.bank);
  });
  $("goldChart").addEventListener("click", () => { S.histGold = true; });
  $("scrim").addEventListener("click", () => { closeSheet(); closeAll(); });
  $("shClose").addEventListener("click", closeSheet);
  document.addEventListener("keydown", (e) => { if (e.key === "Escape") { closeSheet(); closeAll(); } });
  bindAll();
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
  applyTheme();
  route();
  if (store.get("loc") === "1" && navigator.permissions) {
    navigator.permissions.query({ name: "geolocation" }).then((p) => {
      if (p.state === "granted") locate().then(() => loadBranches()).then(() => { renderConv(); renderTable(); showUser(); }).catch(() => {});
    }).catch(() => {});
  }
  try { await loadRates(S.cur); } catch { showError(); }
  renderAll();
  for (const c of CURS) loadRates(c).then(renderCbu).catch(() => {});
  loadGold().then(renderGold).catch(() => {});
  loadCbuAll().then(renderAllBtn).catch(() => {});
  loadBranches().then(() => { renderConv(); renderTable(); if (S.view === "map") renderMapWhenReady(); }).catch(() => {});
  setInterval(async () => {
    S.rates = {}; S.bankHist = {}; S.gold = null; AC.list = null; delete inflight.cbuall;
    for (const k of Object.keys(inflight)) if (k[0] === "r" || k[0] === "b" || k === "gold") delete inflight[k];
    loadGold().then(renderGold).catch(() => {});
    try { await Promise.all(CURS.map(loadRates)); renderAll(); } catch { /* keep old */ }
  }, 60 * 60 * 1000);
}

init();
