// The banks' own lists of offices open on the coming weekend.
//
// Each bank on the Central Bank's weekly list (src/weekend.js) links its own announcement, and every bank publishes it
// differently: an HTML table (Agrobank, Xalq banki, Turonbank, Infinbank, SQB), an .xlsx file (Ipoteka bank, BRB), a
// PDF with a text layer (Hamkorbank, Mikrokreditbank, Asakabank through its news API, NBU), a few lines of text (Garant,
// Tenge, Asia Alliance, Orient Finans, Apex, Universal, Poytaxt, Hayot through its API), or a picture (Trastbank).
// This reads all but pictures with one generic reader per format; the per-bank part is only where on the page the
// announcement sits (BANKS below). Pictures are reported, not read.
//
// readBankList({ name, url }, days) -> { name, url, bank, status, entries, read, note }
//   status   "ok" (entries for this weekend), "stale" (the page is for another weekend), "none" (read, nothing found:
//            the format changed), "image" (a picture), "blocked" (a bot check), "error" (not reachable), "all" (the bank
//            says all its offices open), "regular" (a standing schedule: the branches' own hours already say it)
//   entries  [{ name, parent, address, region, open: { Sa?: "09:00–17:00" | "", Su?: ... } }]; "" = hours not given
//   read     the documents actually read (the page, then its PDF/xlsx files)
//
// Nothing here guesses: a row without a weekend day is dropped, a page with only other weekends' dates is "stale".
import * as cheerio from "cheerio";
import https from "node:https";
import http from "node:http";
import zlib from "node:zlib";
import { readXlsx } from "./xlsx.js";
import { normalizeHours } from "./officialBranches.js";

const UA = "Mozilla/5.0 (compatible; QulayKurs/1.0; +https://qulaykurs.uz)";
const MAX_BYTES = 20 * 1024 * 1024;

/**
 * Where each bank's announcement sits, by the host of the link on the Central Bank's list. `bank` is the bank's name in
 * /api/branches (for matching the points). `root`: the CSS selector of the announcement (default: article, main, body).
 * `api`: the JSON the bank's own page is rendered from (single-page sites). Unknown hosts still get the generic reader.
 */
export const BANKS = {
  "agrobank.uz": { bank: "Agrobank", root: ".ab-page__text" },
  "aab.uz": { bank: "Asia Alliance Bank", root: ".post-content" },
  "apexbank.uz": { bank: "Apex Bank", root: ".article-body" },
  "brb.uz": { bank: "BRB", root: "main" },
  "ipotekabank.uz": { bank: "Ipoteka bank", root: ".blog-detail" },
  "nbu.uz": { bank: "O‘zbekiston Milliy banki" },
  "ofb.uz": { bank: "Orient Finans Bank", root: "article" },
  "tengebank.uz": { bank: "Tenge Bank", root: ".content-inner-editer, .page--news" },
  "trustbank.uz": { bank: "Trastbank", root: ".news-detail-new, .detail-text" },
  "hamkorbank.uz": { bank: "Hamkorbank" },
  // A standing note on the contacts page: every office works on weekends. Their hours are on each branch already.
  "anorbank.uz": { bank: "Anorbank", root: ".anorbank-alert-desc", all: /barcha\s+hududiy\s+savdo\s+ofislarimiz\s+ham\s+dam\s+olish\s+kunlarisiz/i },
  "garantbank.uz": { bank: "Garant bank", root: ".section-col.content" },
  "hayotbank.uz": { bank: "Hayot Bank", api: (u) => (/\/view\/(\d+)/.exec(u.pathname) ? `https://api.hayotbank.uz/api/press-centers/get/${/\/view\/(\d+)/.exec(u.pathname)[1]}` : null) },
  "sqb.uz": { bank: "O‘zsanoatqurilishbank", root: ".generalInfo__accordion, .generalInfo" },
  "asakabank.uz": { bank: "Asakabank", api: (u) => { const slug = u.pathname.split("/").filter(Boolean).pop(); return slug ? `https://back.asakabank.uz/news/v1/detail/${slug}/` : null; } },
  "xb.uz": { bank: "Xalq banki", root: "main" },
  // Its link is a standing article (Saturday hours of some branches since spring); those hours are on the branches.
  "ipakyulibank.uz": { bank: "Ipak Yuli Bank", regular: true },
  "mkbank.uz": { bank: "MKBank", root: "article" },
  "poytaxtbank.uz": { bank: "Poytaxt bank", root: ".post-content" },
  "turonbank.uz": { bank: "Turon bank", root: ".post-content" },
  "universalbank.uz": { bank: "Universal bank", root: ".base__content" },
  "saderatbank.uz": { bank: "Saderat Bank", root: "body" },
  "infinbank.com": { bank: "Infinbank", root: "article" },
};
export const bankConf = (url) => {
  const host = new URL(url).hostname.replace(/^www\./, "");
  return BANKS[host] || BANKS[Object.keys(BANKS).find((h) => host.endsWith("." + h))] || {};
};

/* ---------- download ---------- */

/** GET a URL -> { buf, type, url } (plain node:https: some bank servers send headers undici rejects; see bankSites.js). */
export function download(url, { timeout = 25000, redirects = 5 } = {}) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https:") ? https : http;
    const req = lib.get(url, {
      headers: { "user-agent": UA, accept: "text/html,application/json,application/pdf,*/*;q=0.8", "accept-language": "uz,ru;q=0.9,en;q=0.8", "accept-encoding": "gzip, deflate, br" },
      insecureHTTPParser: true, family: 4, timeout,
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location && redirects > 0) {
        res.resume();
        return resolve(download(new URL(res.headers.location, url).toString(), { timeout, redirects: redirects - 1 }));
      }
      if (res.statusCode !== 200) { res.resume(); return reject(new Error(`HTTP ${res.statusCode}`)); }
      const enc = res.headers["content-encoding"];
      const stream = enc === "gzip" ? res.pipe(zlib.createGunzip()) : enc === "deflate" ? res.pipe(zlib.createInflate()) : enc === "br" ? res.pipe(zlib.createBrotliDecompress()) : res;
      const chunks = [];
      let size = 0;
      stream.on("data", (c) => { size += c.length; if (size > MAX_BYTES) req.destroy(new Error("too large")); else chunks.push(c); });
      stream.on("end", () => resolve({ buf: Buffer.concat(chunks), type: String(res.headers["content-type"] || ""), url }));
      stream.on("error", reject);
    });
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.on("error", reject);
  });
}

/** What a downloaded document is, by its bytes first (servers mislabel files). */
export function kindOf({ buf, type, url }) {
  if (buf.subarray(0, 5).toString("latin1") === "%PDF-") return "pdf";
  if (buf[0] === 0x50 && buf[1] === 0x4b && /\.xlsx?(\?|$)/i.test(url) || /spreadsheetml|ms-excel/.test(type)) return "xlsx";
  if (/^image\//.test(type) || /\.(png|jpe?g|webp|gif)(\?|$)/i.test(url)) return "image";
  if (/json/.test(type)) return "json";
  return "html";
}

/* ---------- text helpers ---------- */

const CYR = { а: "a", б: "b", в: "v", г: "g", д: "d", е: "e", ё: "yo", ж: "j", з: "z", и: "i", й: "y", к: "k", л: "l", м: "m", н: "n", о: "o", п: "p", р: "r", с: "s", т: "t", у: "u", ф: "f", х: "x", ц: "ts", ч: "ch", ш: "sh", щ: "sh", ъ: "'", ы: "i", ь: "", э: "e", ю: "yu", я: "ya", ў: "o'", қ: "q", ғ: "g'", ҳ: "h" };
/** Lower-case Latin for keyword tests: Cyrillic transliterated, the Central Bank's new Latin (ş, ç, ö, ğ) mapped back. */
export function latin(s) {
  return String(s ?? "").toLowerCase().replace(/[а-яёўқғҳ]/g, (c) => CYR[c] ?? c)
    .replace(/ş/g, "sh").replace(/ç/g, "ch").replace(/ö/g, "o'").replace(/ğ/g, "g'").replace(/ı/g, "i")
    .replace(/[ʻʼ’‘`´]/g, "'");
}
export const clean = (s) => String(s ?? "").replace(/ |​/g, " ").replace(/[ \t\r]+/g, " ").replace(/ *\n[\n ]*/g, "\n").trim();
const oneLine = (s) => clean(s).replace(/\n/g, " ");
const pad2 = (n) => String(n).padStart(2, "0");

const MONTH_RE = "(yanvar|fevral|mart|aprel|may|iyun|iyul|avgust|sentyabr|oktyabr|noyabr|dekabr|january|february|march|april|june|july|august|september|october|november|december)([a-z']*)";
const MONTH_NO = (w) => {
  const i = ["yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentyabr", "oktyabr", "noyabr", "dekabr"].indexOf(w);
  return i >= 0 ? i + 1 : ["january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"].indexOf(w) + 1;
};

/**
 * Event dates in a text, as "YYYY-MM-DD": "21.11.2026", "21/11/26", "11/21/2026", "21-22-noyabr", "7, 14, 21 va 28-noyabr",
 * "21 ноября". `ref` ("YYYY-MM-DD") supplies a missing year. "3-avgustdan boshlab" (since ...) is not an event date.
 */
export function datesIn(text, ref) {
  const t = latin(text);
  const out = new Set();
  const refY = Number(ref.slice(0, 4)), refM = Number(ref.slice(5, 7));
  const add = (y, m, d) => { if (m >= 1 && m <= 12 && d >= 1 && d <= 31) out.add(`${y}-${pad2(m)}-${pad2(d)}`); };
  const yearOf = (m) => (refM === 12 && m === 1 ? refY + 1 : refM === 1 && m === 12 ? refY - 1 : refY);
  for (const [, a, b, y] of t.matchAll(/(?<![\d.\/])(\d{1,2})[./](\d{1,2})[./](\d{4}|\d{2})(?![\d])/g)) {
    const Y = y.length === 2 ? 2000 + Number(y) : Number(y);
    if (Number(b) <= 12) add(Y, Number(b), Number(a)); else if (Number(a) <= 12) add(Y, Number(a), Number(b));
  }
  for (const [, y, m, d] of t.matchAll(/\b(20\d\d)-(\d\d)-(\d\d)\b/g)) add(Number(y), Number(m), Number(d));
  const listRe = new RegExp(`((?:\\d{1,2}\\s*(?:,|va|i|and|&|-|–|—)\\s*)*\\d{1,2})\\s*(?:-|–)?\\s*${MONTH_RE}`, "g");
  for (const m of t.matchAll(listRe)) {
    if (/^dan\b|^dan$/.test(m[3]) || /^\s*boshlab/.test(t.slice(m.index + m[0].length))) continue;
    const month = MONTH_NO(m[2]);
    for (const d of m[1].match(/\d{1,2}/g)) add(yearOf(month), month, Number(d));
  }
  return out;
}

/** Opening hours in a text -> "09:00–17:00" (several ranges joined by ", "), "00:00–24:00" for 24/7, null if none. */
export function hoursIn(text) {
  let t = String(text ?? "")
    .replace(/(?<![\d.\/])\d{1,2}[./]\d{1,2}[./]\d{2,4}(?!\d)/g, " ")
    .replace(/\b(?:tushlik|обед|перерыв|tanaffus)[^;)\n]*/gi, " ");
  if (/24\s*\/\s*7|kunu-?tun|kecha-?kunduz|круглосуточ|24 soat/i.test(t)) return "00:00–24:00";
  // "9-00dan-16-00gacha" (BRB), "09-00 -17:00"
  t = t.replace(/\b(\d{1,2})-(\d{2})(?=\s*(?:dan|дан|gacha|гача|до|-|–|—|$))/gi, "$1:$2");
  const times = [...t.matchAll(/(?<![\d:])(\d{1,2})[:.](\d{2})(?![\d])/g)].map((m) => [Number(m[1]), Number(m[2])]).filter(([h, m]) => h <= 24 && m < 60);
  const ranges = [];
  for (let i = 0; i + 1 < times.length; i += 2) {
    const [a, b] = [times[i], times[i + 1]];
    let end = b[0] * 60 + b[1];
    const start = a[0] * 60 + a[1];
    if (end === 0) end = 24 * 60;
    if (end <= start) continue;
    ranges.push(`${pad2(a[0])}:${pad2(a[1])}–${pad2(Math.floor(end / 60))}:${pad2(end % 60)}`);
  }
  return ranges.length ? [...new Set(ranges)].join(", ") : null;
}

/** "Sa", "Su" named in a text (shanba, yakshanba, суббота, воскресенье, Шанба...). */
export function dayWords(text) {
  const t = latin(text);
  const out = new Set();
  // "Payshanba", "chorshanba", "seshanba", "dushanba" are weekdays: only a word that starts with "shanba" is Saturday.
  // Sunday as typed: "yakshanba", "yaskhanba" (Trastbank), "Yaskshanba" (Agrobank).
  const SUN = /(?<![a-z'])ya[a-z-]{1,4}anba/g;
  if (SUN.test(t) || /voskres|sunday/.test(t)) out.add("Su");
  if (/(?<![a-z'])sh?anba|subbot|saturday/.test(t.replace(SUN, ""))) out.add("Sa");
  return out;
}

const OPEN_WORDS = /ishlaydi|ishlayd|işlaydi|ishlaidi|rabota|работает|ochiq|\bha\b|\bda\b|✓|✔|\+/;
const CLOSED_WORDS = /ishlamay|işlamay|ishlamaidi|ne rabot|не работает|yopiq|zakryt|закрыт|dam olish/;
/** One weekend-day cell of a table -> { open, hours }: "09:00-18:00", "Ishlaydi", "-", "Ishlamaydi", "". */
function dayCell(v) {
  const t = latin(v).trim();
  if (CLOSED_WORDS.test(t)) return { open: false };
  const hours = hoursIn(v);
  if (hours) return { open: true, hours };
  if (OPEN_WORDS.test(t)) return { open: true, hours: "" };
  return { open: false }; // empty, "-", or a note: not open as far as the list says
}

const ADDRESS_RE = /ko'?ch|kocha|kucha|ko'ch|\bk\.|shox|shoh|shahri|shahar|shaxar|\bsh\.|tumani|tuman\b|\bt\.|mfy|qfy|massiv|mavze|dahasi|daxa|kvartal|\buy\b|-uy|\buyi\b|txay|tkad|yo'li|prospekt|ulitsa|\bul\.|\bg\.|\bdom\b|bozor|savdo|hudud|avtosalon|mehmonxona|vokzal|aeroport|massivi/;
const OFFICE_RE = /\bbxm\b|\bbxo\b|mbxo|ubxo|bank xizmatlari|filial|\bofis|kassa|markaz|boshqarma|shoxobcha|shaxobcha|bo'lim|bo'linma|\bvash\b|офис|филиал|касса|отделен|punkt|avtosalon|mehmonxona|vokzal/;
const isAddress = (s) => ADDRESS_RE.test(latin(s)) && /\d|ko'ch|kocha|kucha|tumani|shahri|ул\.|г\./i.test(latin(s) + s);
const isOffice = (s) => OFFICE_RE.test(latin(s)) || /[«“"].+[»”"]/.test(s);
const isNum = (s) => /^\s*\d{1,3}(?:[.,]\d{1,2})?\.?\s*$/.test(String(s ?? ""));

/* ---------- tables (HTML, xlsx and PDF all become a grid of cell texts) ---------- */

/** A cheerio <table> -> string[][] with colspan/rowspan expanded (merged cells repeat their text). */
export function htmlGrid($, table) {
  const grid = [], spans = [];
  $(table).find("tr").each((_, tr) => {
    if ($(tr).closest("table")[0] !== table) return; // rows of a nested table
    const row = [];
    let c = 0;
    const fill = () => { while (spans[c]?.left > 0) { row[c] = spans[c].text; spans[c].left--; c++; } };
    $(tr).children("td,th").each((__, cell) => {
      fill();
      const $c = $(cell).clone();
      $c.find("br").replaceWith("\n");
      $c.find("p,div,li").each((___, e) => { $(e).append("\n"); });
      const text = clean($c.text());
      const cs = Math.min(Number($(cell).attr("colspan")) || 1, 50), rs = Math.min(Number($(cell).attr("rowspan")) || 1, 500);
      for (let k = 0; k < cs; k++) { row[c] = text; if (rs > 1) spans[c] = { left: rs - 1, text }; else delete spans[c]; c++; }
    });
    fill();
    grid.push(row);
  });
  const width = Math.max(0, ...grid.map((r) => r.length));
  return grid.map((r) => Array.from({ length: width }, (_, i) => r[i] ?? ""));
}

/** What a header text says a column holds. */
function columnRole(h, days) {
  const t = latin(h);
  const dates = datesIn(h, days[0]);
  if (dates.has(days[0])) return { role: "day", day: "Sa" };
  if (dates.has(days[1])) return { role: "day", day: "Su" };
  if (dates.size) return { role: "otherDate" };
  if (/^\s*(№|#|n|t\/r)\s*$/.test(t) || /^№/.test(t.trim())) return { role: "num" };
  if (/manzil|adres|address/.test(t)) return { role: "address", strong: true };
  if (/^\s*(tuman|rayon|район|district)\s*$/.test(t)) return { role: "district" };
  if (/bank nomi|^bank$/.test(t.trim())) return { role: "bank" };
  // "Viloyat filiallar nomlari" (NBU) names offices, though it says "viloyat".
  if (/\bnom(i|lari)\b|nazvan|naimen/.test(t)) return { role: "name", strong: true };
  if (/viloyat|hudud|region|oblast|respublika|joylashgan/.test(t)) return { role: "region" };
  if (/raqam|nomer|номер|\bid\b|\bkod/.test(t)) return { role: "skip" };
  const dw = dayWords(h);
  if (dw.size === 1) return { role: "day", day: [...dw][0] };
  if (/vaqt|vremya|время|grafik|soat|chasy|часы|tartib/.test(t)) return { role: "hours" };
  if (/(?<![a-z])ish kun|^kunlari$|^kuni$|dni rab/.test(t)) return { role: "daysList" };
  if (/bank nomi|bank$|^bank\b/.test(t.trim())) return { role: "bank" };
  if (/nomi|filial|bxm|bxo|kassa|bo'lim|bo'linma|infratuzilma|ofis|markaz|tarmoq|naimen|nazvan|punkt|vash|vash$/.test(t)) return { role: "name", strong: /nomi|bxm|kassa|bo'linma|infratuzilma|nazvan|tarmoq/.test(t) };
  if (/valyuta|pul o'tkaz|ayirbosh|obmen|perevod|xpu|eskrou|xizmat/.test(t)) return { role: "service" };
  return { role: "unknown" };
}

/** What a column's data cells look like, for tables without a header row (Xalq banki). */
function contentRole(cells, days) {
  const vals = cells.filter((v) => v.trim());
  if (!vals.length) return { role: "unknown" };
  const share = (fn) => vals.filter(fn).length / vals.length;
  if (share(isNum) > 0.8) return { role: "num" };
  if (share((v) => hoursIn(v) && !dayWords(v).size) > 0.6) return { role: "hours" };
  if (share((v) => dayWords(v).size || CLOSED_WORDS.test(latin(v)) || OPEN_WORDS.test(latin(v)) || hoursIn(v)) > 0.7) {
    const dates = new Set(vals.flatMap((v) => [...datesIn(v, days[0])]));
    return dates.size ? { role: "service" } : { role: "daysList" };
  }
  if (share((v) => /viloyat|respublika|shahri$|shahar$/.test(latin(v)) && v.length < 40) > 0.7) return { role: "region" };
  if (share(isAddress) > 0.6) return { role: "address" };
  if (share(isOffice) > 0.5) return { role: "name", strong: true };
  return { role: "unknown" };
}

const avgLen = (rows, c) => rows.reduce((n, r) => n + (r[c] || "").length, 0) / Math.max(1, rows.length);
const sameText = (row) => new Set(row.map((v) => v.trim()).filter(Boolean)).size === 1;
const STOP_ROW = /^(jami|итого|всего|izoh|примечание)\b/;

/**
 * A grid -> entries. ctx: { days: [sat, sun], open: Set of default days, hours: default hours, heading }.
 * Returns { entries, stale } (stale: the table only has other weekends' date columns).
 */
export function tableEntries(grid, ctx) {
  const { days } = ctx;
  const rows = grid.filter((r) => r.some((v) => v.trim()));
  // The first data row: a number first (№), or a row that already has an address or an opening time in it.
  const isData = (r) => {
    const cells = r.map((v) => v.trim()).filter(Boolean);
    if (cells.length < 2) return false;
    if (isNum(cells[0])) return true;
    return !sameText(r) && cells.some((v) => isAddress(v) && v.length > 12) && cells.some((v) => hoursIn(v) || dayWords(v).size || OPEN_WORDS.test(latin(v)) || CLOSED_WORDS.test(latin(v)));
  };
  const first = rows.findIndex(isData);
  if (first < 0) return { entries: [], stale: false };
  const head = rows.slice(0, first);
  let region = "";
  const headerRows = [];
  for (const r of head) {
    // A row with one text over every column: a region ("Andijon") just before the data, or a title ("MA’LUMOT",
    // a sentence), which names nothing.
    if (sameText(r)) {
      const v = r.find((x) => x.trim()).trim();
      region = v.length <= 40 && v.split(/\s+/).length <= 3 && v !== v.toUpperCase() ? v : "";
      continue;
    }
    headerRows.push(r);
    region = "";
  }
  const width = rows[0].length;
  const data = rows.slice(first);
  const roles = [];
  for (let c = 0; c < width; c++) {
    // A long title merged over several columns ("Dam olish kunlari ishlaydigan kassalar...") names no column.
    const h = [...new Set(headerRows.map((r) => r[c]?.trim()).filter(Boolean))]
      .filter((v) => !(v.length > 25 && headerRows.some((r) => r[c]?.trim() === v && r.filter((x) => x.trim() === v).length >= 3)))
      .join(" ");
    let role = h ? columnRole(h, days) : { role: "unknown" };
    if (role.role === "unknown") role = contentRole(data.map((r) => r[c] || ""), days);
    roles.push(role);
  }
  // Two "name" columns: the stronger one (Kassa/BXM nomi) is the office, the other its branch (Filial).
  const nameCols = roles.map((r, i) => (r.role === "name" ? i : -1)).filter((i) => i >= 0);
  let nameCol = nameCols.find((i) => roles[i].strong) ?? nameCols[0];
  if (nameCol == null) nameCol = roles.findIndex((r) => r.role === "unknown");
  const parentCol = nameCols.find((i) => i !== nameCol);
  const col = (role) => roles.map((r, i) => (r.role === role ? i : -1)).filter((i) => i >= 0);
  const dayCols = roles.map((r, i) => (r.role === "day" ? [i, r.day] : null)).filter(Boolean);
  const hasOther = roles.some((r) => r.role === "otherDate");
  // Several address-like columns (SQB: "Tuman" and "Manzil"): the one its header names, else the longest texts.
  const addrCols = col("address").sort((a, b) => (roles[b].strong ? 1 : 0) - (roles[a].strong ? 1 : 0) || avgLen(data, b) - avgLen(data, a));
  const [addrCol] = addrCols, [regionCol] = col("region"), hoursCols = col("hours"), listCols = col("daysList"), serviceCols = col("service");
  const dateCells = (r) => new Set(r.flatMap((v, i) => (roles[i].role === "service" || roles[i].role === "unknown" ? [...datesIn(v, days[0])] : [])));
  const anyDateInCells = data.some((r) => dateCells(r).size);
  const weekendInCells = data.some((r) => [...dateCells(r)].some((d) => days.includes(d)));
  if (!dayCols.length && ((hasOther && !weekendInCells && !listCols.length) || (anyDateInCells && !weekendInCells))) return { entries: [], stale: true };

  const entries = [];
  for (const r of data) {
    const text = r.join(" ").trim();
    if (STOP_ROW.test(latin(text))) break;
    if (sameText(r)) { region = r.find((v) => v.trim()); continue; }
    const name = (r[nameCol] || "").replace(/\s*\*+\s*$/, "").trim();
    const address = addrCol != null ? r[addrCol].trim() : "";
    if (!name && !address) continue;
    if (headerRows.some((h) => h.join("|") === r.join("|"))) continue; // a header repeated on a new page
    const rowHours = hoursCols.map((i) => hoursIn(r[i])).find(Boolean) || null;
    const open = {};
    let told = false;
    for (const [i, day] of dayCols) {
      told = true;
      const cell = dayCell(r[i]);
      if (cell.open) open[day] = cell.hours || rowHours || ctx.hours || "";
    }
    for (const i of listCols) {
      const dw = dayWords(r[i]);
      if (dw.size || /dam olish|выходн/.test(latin(r[i]))) told = true;
      for (const d of dw.size ? dw : /dam olish|выходн/.test(latin(r[i])) ? ["Sa", "Su"] : []) open[d] = rowHours || hoursIn(r[i]) || ctx.hours || "";
    }
    const cellDates = dateCells(r);
    if (cellDates.size) {
      told = true;
      if (cellDates.has(days[0])) open.Sa ??= rowHours || ctx.hours || "";
      if (cellDates.has(days[1])) open.Su ??= rowHours || ctx.hours || "";
    }
    if (!told) for (const d of ctx.open) open[d] = rowHours || ctx.hours || "";
    if (!Object.keys(open).length) continue;
    const services = serviceCols.filter((i) => [...datesIn(r[i], days[0])].some((d) => days.includes(d)) || OPEN_WORDS.test(latin(r[i]))).map((i) => headerRows.map((h) => h[i]).filter(Boolean).pop()).filter(Boolean);
    entries.push({
      name, parent: parentCol != null ? r[parentCol].trim() : "", address,
      region: (regionCol != null ? r[regionCol].trim() : "") || region || "",
      open, ...(services.length ? { services } : {}),
    });
  }
  return { entries, stale: false };
}

/* ---------- PDF: text items -> grid ---------- */

/** PDF bytes -> pages of text items { str, x, y, w } (y grows upwards, as in PDF). */
export async function pdfPages(buf) {
  const { getDocumentProxy } = await import("unpdf");
  const doc = await getDocumentProxy(new Uint8Array(buf));
  const pages = [];
  for (let p = 1; p <= Math.min(doc.numPages, 40); p++) {
    const page = await doc.getPage(p);
    const { width } = page.getViewport({ scale: 1 });
    const tc = await page.getTextContent();
    pages.push({ width, items: tc.items.filter((i) => i.str && i.str.trim()).map((i) => ({ str: i.str.trim(), x: i.transform[4], y: i.transform[5], w: i.width })) });
  }
  return pages;
}

const HEADER_WORDS = /^(№|#)$|filial|manzil|nomi|shanba|yakshanba|ish vaqti|vaqti|hudud|viloyat|bxm|bxo|vash|adres|bank/;
/** Lines of a page: items within 4 units of height are one line; top to bottom. */
function pdfLines(items) {
  const sorted = [...items].sort((a, b) => b.y - a.y);
  const lines = [];
  for (const it of sorted) {
    const line = lines.at(-1);
    if (line && Math.abs(line.y - it.y) <= 4) { line.items.push(it); line.ys.push(it.y); } else lines.push({ y: it.y, ys: [it.y], items: [it] });
  }
  for (const l of lines) { l.items.sort((a, b) => a.x - b.x); l.text = l.items.map((i) => i.str).join(" "); }
  return lines;
}

/**
 * Pages of a PDF table -> one grid: a header row, then one row per entry. Columns come from the header words' positions;
 * a row starts at a number in the № column, and the lines between two numbers belong to the nearer one (merged cells are
 * printed in the middle of their rows). A row whose lines carry two sets of opening days is split in two (Asakabank: one
 * branch, two exchange offices).
 */
export function pdfGrid(pages) {
  let columns = null;
  const grid = [];
  for (const page of pages) {
    const lines = pdfLines(page.items.filter((i) => i.w < page.width * 0.45 || /^№$/.test(i.str)));
    const hIdx = lines.findIndex((l) => l.items.some((i) => /^№$/.test(i.str)) || l.items.filter((i) => HEADER_WORDS.test(latin(i.str))).length >= 2);
    let start = 0;
    if (hIdx >= 0) {
      const hy = lines[hIdx].y;
      const numCols = (l) => l.items.length >= 4 && l.items.every((i, k) => i.str === String(k + 1));
      // The header ends at the first line that holds data: a row number, an opening time, an address, an office id.
      const dataLike = (l) => isNum(l.items[0].str) || l.items.some((i) => hoursIn(i.str) || OPEN_WORDS.test(latin(i.str)) || CLOSED_WORDS.test(latin(i.str)) || (isAddress(i.str) && i.str.length > 15) || /^\d{5,}$/.test(i.str));
      let dataIdx = lines.findIndex((l, k) => k > hIdx && !numCols(l) && dataLike(l));
      if (dataIdx < 0) dataIdx = lines.length;
      const headerItems = lines.slice(0, dataIdx).filter((l) => l.y <= hy + 12 && !numCols(l)).flatMap((l) => l.items);
      // Columns: header items whose x-ranges overlap are one column (a two-line header, a date under "Shanba").
      const cols = [];
      for (const it of [...headerItems].sort((a, b) => a.x - b.x)) {
        const c = cols.find((k) => it.x < k.x2 - 2 && it.x + it.w > k.x1 + 2);
        if (c) { c.x1 = Math.min(c.x1, it.x); c.x2 = Math.max(c.x2, it.x + it.w); c.items.push(it); } else cols.push({ x1: it.x, x2: it.x + it.w, items: [it] });
      }
      cols.sort((a, b) => a.x1 - b.x1);
      columns = cols.map((c) => ({ x1: c.x1, x2: c.x2, mid: (c.x1 + c.x2) / 2, title: c.items.sort((a, b) => b.y - a.y || a.x - b.x).map((i) => i.str).join(" ") }));
      if (!grid.length) grid.push(columns.map((c) => c.title));
      start = dataIdx;
    }
    if (!columns) continue;
    const body = [];
    for (const l of lines.slice(start)) {
      if (STOP_ROW.test(latin(l.text))) break;
      body.push(l);
    }
    const nearestCol = (it) => {
      const mid = it.x + it.w / 2;
      let best = 0, bd = Infinity;
      columns.forEach((c, i) => { const d = mid < c.x1 ? c.x1 - mid : mid > c.x2 ? mid - c.x2 : 0; if (d < bd) { bd = d; best = i; } });
      return best;
    };
    // Lanes: the x-ranges the data covers, split where a vertical strip is blank on every line. Cells are left-aligned
    // under centred headers of other widths (Hamkorbank: "Asaka UBXO" is nearer the № header than the "Filial" one),
    // so an item belongs to the column whose header is centred over its lane.
    const lanes = [];
    for (const [a, b] of body.flatMap((l) => l.items).map((i) => [i.x, i.x + i.w]).sort((p, q) => p[0] - q[0])) {
      const last = lanes.at(-1);
      if (last && a <= last[1] + 2.5) last[1] = Math.max(last[1], b); else lanes.push([a, b]);
    }
    const laneCol = lanes.map(([a, b]) => {
      const over = columns.map((c, i) => (c.mid >= a && c.mid <= b ? i : -1)).filter((i) => i >= 0);
      return over.length === 1 ? over[0] : null; // none or several: decided per item
    });
    const colOf = (it) => {
      const k = lanes.findIndex(([a, b]) => it.x >= a - 0.01 && it.x <= b);
      return k >= 0 && laneCol[k] != null ? laneCol[k] : nearestCol(it);
    };
    // Rows: anchored at a number in the first column.
    const anchors = body.filter((l) => colOf(l.items[0]) === 0 && isNum(l.items[0].str));
    if (!anchors.length) continue;
    const groups = anchors.map((a) => ({ anchor: a, lines: [] }));
    for (const l of body) {
      let g = groups[0], bd = Infinity;
      for (const x of groups) { const d = Math.abs(x.anchor.y - l.y); if (d < bd) { bd = d; g = x; } }
      // Lines far below the last row (a footnote) belong to nothing.
      if (bd > 40) continue;
      g.lines.push(l);
    }
    const dayColIdx = columns.map((c, i) => (columnRole(c.title, ["0000-00-00", "0000-00-00"]).role === "day" || /\d{1,2}[./]\d{1,2}[./]\d{2,4}|vaqt|shanba|yakshanba/.test(latin(c.title)) ? i : -1)).filter((i) => i > 0);
    const addrIdx = columns.findIndex((c) => columnRole(c.title, ["0000-00-00", "0000-00-00"]).role === "address");
    for (const g of groups) {
      const timed = g.lines.filter((l) => l.items.some((i) => dayColIdx.includes(colOf(i)) && (hoursIn(i.str) || OPEN_WORDS.test(latin(i.str)))));
      const subs = timed.length >= 2 ? timed.map((t) => ({ main: t, lines: [] })) : [{ main: null, lines: [] }];
      for (const l of g.lines) {
        if (subs.length === 1) { subs[0].lines.push(l); continue; }
        if (timed.includes(l)) { subs.find((s) => s.main === l).lines.push(l); continue; }
        // A line with an address belongs to the nearest sub-row; the rest (№, the branch name) is shared.
        if (addrIdx >= 0 && l.items.some((i) => colOf(i) === addrIdx)) {
          let s = subs[0], bd = Infinity;
          for (const x of subs) { const d = Math.abs(x.main.y - l.y); if (d < bd) { bd = d; s = x; } }
          s.lines.push(l);
        } else for (const s of subs) s.lines.push(l);
      }
      for (const s of subs) {
        const cells = columns.map(() => []);
        for (const l of s.lines.sort((a, b) => b.y - a.y)) for (const it of l.items) cells[colOf(it)].push(it.str);
        grid.push(cells.map((c) => c.join(" ").replace(/\s+/g, " ").trim()));
      }
    }
  }
  return grid;
}

/* ---------- text announcements ---------- */

/** The (…) at the end of a line, with nested brackets: "«BYD» (Sergeli tumani, TKAD (halqa yo‘li), 109-uy)". */
function trailingParens(s) {
  const end = s.search(/\)\s*[;.,]?\s*$/);
  const scan = (last) => {
    let depth = 0;
    for (let i = last; i >= 0; i--) {
      if (s[i] === ")") depth++;
      else if (s[i] === "(") { depth--; if (depth === 0) return i; }
    }
    return -1;
  };
  if (end >= 0) { const open = scan(end); if (open > 0) return { before: s.slice(0, open), inside: s.slice(open + 1, end), after: "" }; }
  // "BXM "WAY II" (address) faqat 21-noyabr kuni faoliyat yuritadi." — the address in the middle.
  const m = /^(.*?)\(([^()]*(?:\([^()]*\)[^()]*)*)\)(.*)$/.exec(s);
  if (m && isAddress(m[2])) return { before: m[1], inside: m[2], after: m[3] };
  return null;
}

const stripBullet = (s) => s.replace(/^[\s•·▪●◦‣\-–—*>✅🔹🔸➖📍📌]+/u, "").replace(/^\d{1,2}[.)]\s+/, "").replace(/^[\s️]+/u, "").trim();

/**
 * Lines of an announcement -> entries. Recognised: "Name (address)", "Name manzili: address", "📍 address", an office
 * line followed by its address and "График работы" lines (Apex), "Name — soat 10:00 dan 16:00 gacha, 21-noyabr kuni"
 * (Orient Finans), and a lone office named in a sentence (Poytaxt). Defaults (ctx.open, ctx.hours) come from the rest
 * of the text.
 */
export function proseEntries(lines, ctx) {
  const { days } = ctx;
  const out = [];
  const lineDays = (s) => {
    const ds = datesIn(s, days[0]);
    const set = new Set();
    if (ds.has(days[0])) set.add("Sa");
    if (ds.has(days[1])) set.add("Su");
    // A bare "shanba" counts only on a page that is not about another weekend.
    if (!set.size && !ds.size && !ctx.stale) for (const d of dayWords(s)) set.add(d);
    return { set, other: ds.size > 0 && !set.size };
  };
  const push = (name, address, rest, raw) => {
    const { set, other } = lineDays(rest);
    if (other) return; // "faqat 28-noyabr": another weekend
    const hours = hoursIn(rest);
    const open = {};
    for (const d of set.size ? set : ctx.open) open[d] = hours || ctx.hours || "";
    if (!Object.keys(open).length) return;
    out.push({ name: oneLine(name).replace(/[\s,;:–—-]+$/, ""), parent: "", address: oneLine(address).replace(/[\s;.,]+$/, ""), region: "", open, raw });
  };
  const L = lines.map((l) => ({ raw: l, s: stripBullet(l), pin: /^\s*📍/u.test(l) }));
  for (let i = 0; i < L.length; i++) {
    const { s, pin, raw } = L[i];
    if (!s || s.length > 320 || /:\s*$/.test(s)) continue;
    if (pin) { if (isAddress(s)) push("", s, "", raw); continue; }
    const hay = /^(.{2,120}?)\s+manzili\s*:\s*(.+)$/i.exec(s);
    if (hay && isAddress(hay[2])) { push(hay[1], hay[2], "", raw); continue; }
    const par = trailingParens(s);
    if (par && isAddress(par.inside) && par.before.trim().length >= 2 && par.before.length < 150) { push(par.before, par.inside, par.after, raw); continue; }
    const dash = /^(.{2,80}?)\s+[—–-]\s+(.*)$/.exec(s);
    if (dash && isOffice(dash[1]) && (hoursIn(dash[2]) || datesIn(dash[2], days[0]).size)) { push(dash[1], "", dash[2], raw); continue; }
    // A desk named by where it is, with its own hours: "Samarqand shahri aeroportidagi chakana amaliyotlar kassasi
    // valyuta ayirboshlash xizmatini 24/7 rejimida ko‘rsatadi" (Orient Finans).
    const desk = /^(.{3,90}?dagi\b.{0,60}?\b(?:kassasi|shoxobchasi|shaxobchasi))\b/i.exec(s);
    if (desk && hoursIn(s)) { push(desk[1], "", s.slice(desk[0].length), raw); continue; }
    // An office line, its address on the next line, then its hours (Apex: «Офис продаж "MEGA"» / «г. Ташкент, ...»).
    const next = L[i + 1]?.s || "";
    if (isOffice(s) && s.length < 80 && !isAddress(s) && isAddress(next) && !isOffice(next)) {
      let j = i + 2;
      const hourLines = [];
      while (j < L.length && L[j].s && !isOffice(L[j].s) && (hoursIn(L[j].s) || /график|grafik|ish vaqti/i.test(L[j].s)) && hourLines.length < 4) hourLines.push(L[j++].s);
      const norm = hourLines.length ? normalizeHours(hourLines.join("\n").replace(/^(график работы|grafik|ish vaqti)\s*:?/gim, "")) : null;
      const spans = norm ? weekendSpans(norm) : null;
      if (spans && !spans[0] && !spans[1]) { i = j - 1; continue; } // its hours leave out the weekend
      const open = {};
      if (spans) { if (spans[0]) open.Sa = spans[0]; if (spans[1]) open.Su = spans[1]; } else for (const d of ctx.open) open[d] = ctx.hours || "";
      if (Object.keys(open).length) out.push({ name: oneLine(s), parent: "", address: oneLine(next), region: "", open, raw });
      i = j - 1;
      continue;
    }
  }
  if (out.length) return out;
  // One office named inside a sentence: "21-noyabr kuni Mirobod bank xizmatlari markazi ... faoliyat yuritadi".
  for (const { s, raw } of L) {
    const m = /((?:«[^»]{2,40}»|"[^"]{2,40}"|“[^”]{2,40}”|[A-ZА-ЯЎҚҒҲ][\p{L}'‘’ʻ-]{2,30})\s+(?:bank xizmatlari (?:markazi|ofisi)|BXM|BXO|MBXO|filiali))(?![\p{L}])/u.exec(s)
      || /\b(valyuta ayirboshlash shoxobchasi)\b/i.exec(s)
      || (hoursIn(s) && /^(.{3,90}?\b(?:kassasi|shoxobchasi|shaxobchasi))\b/i.exec(s));
    if (m && (hoursIn(s) || datesIn(s, days[0]).size || ctx.hours)) push(m[1], "", s, raw);
  }
  // Read from a sentence, not a list: dropped when the page's files have the real list (Asakabank).
  for (const e of out) e.weak = true;
  return out;
}

/** Saturday and Sunday of an OSM-style hours text ("Mo-Fr 10:00-22:00; Sa-Su 10:00-23:00") -> [sat, sun], null = closed. */
export function weekendSpans(hours) {
  if (!hours) return null;
  if (/^\s*24\/7\s*$/.test(hours)) return ["00:00–24:00", "00:00–24:00"];
  const DOW = { Mo: 0, Tu: 1, We: 2, Th: 3, Fr: 4, Sa: 5, Su: 6 }, days = Array(7).fill(null);
  for (const part of hours.split(";")) {
    const m = part.trim().match(/^([A-Z][a-z](?:-[A-Z][a-z])?(?:,[A-Z][a-z](?:-[A-Z][a-z])?)*)\s+(\d{1,2}:\d{2})-(\d{1,2}:\d{2})$/);
    if (!m) return null;
    for (const r of m[1].split(",")) {
      const [a, b = a] = r.split("-").map((d) => DOW[d]);
      if (a == null || b == null) return null;
      for (let i = a; ; i = (i + 1) % 7) { days[i] = `${m[2].padStart(5, "0")}–${m[3]}`; if (i === b) break; }
    }
  }
  return [days[5], days[6]];
}

/* ---------- one document ---------- */

const WEEKDAYS = /dushanba|seshanba|chorshanba|payshanba|\bjuma\b|\bdu\s*[-–—]\s*ju|ish kunlari|ponedel|\bpn\s*[-–—]|budn|mo-fr|monday|weekday|\bpon\b/;
/** The page's own defaults: which weekend days it is about, its hours, and whether it is about another weekend. */
export function pageContext(text, days) {
  // When the page was posted or updated is not when it is open: a date with a clock time ("21.10.2026 15:09"), and
  // lines that are only a date or a "updated:" label with one ("Sönggi yangilaniş sanasi: 21.10.2026", SQB).
  const body = text.split("\n")
    .map((l) => l.replace(/(?<![\d.\/])\d{1,2}[./]\d{1,2}[./]\d{2,4},?\s+\d{1,2}:\d{2}(?::\d{2})?/g, " "))
    .filter((l) => !/yangilan|updated|обновл|e'lon qilingan|published/.test(latin(l)))
    .filter((l) => latin(l).replace(/(?<![\d])\d{1,2}[./]\d{1,2}[./]\d{2,4}|\d{1,2}:\d{2}|\d{4}|noy|nov|ноя/g, "").replace(/[^a-z]/g, "").length >= 3)
    .join("\n");
  const dates = datesIn(body, days[0]);
  const open = new Set();
  if (dates.has(days[0])) open.add("Sa");
  if (dates.has(days[1])) open.add("Su");
  const stale = dates.size > 0 && !open.size;
  if (!dates.size) {
    for (const d of dayWords(body)) open.add(d);
    if (!open.size && /dam olish kun|выходн|weekend/.test(latin(body))) { open.add("Sa"); open.add("Su"); }
  }
  // Default hours only from lines about the weekend, not the usual "Dushanba–juma 9:00–17:00" of a contacts page.
  const hours = hoursIn(body.split("\n").filter((l) => !WEEKDAYS.test(latin(l))).join("\n")) || "";
  return { days, open, stale, standing: !dates.size, hours };
}

/** HTML -> { tables: [{ grid, heading }], lines, files, images, text } inside the bank's announcement. */
export function readHtml(html, baseUrl, rootSel) {
  const $ = cheerio.load(html);
  $("script,style,noscript,svg,nav,header,footer,form,button,iframe").remove();
  // Every element the selector matches (Agrobank splits its article into several blocks), outermost only.
  let root = null;
  for (const sel of [rootSel, "article", "main", "body"].filter(Boolean)) {
    const all = $(sel).filter((_, e) => $(e).text().trim().length > 0 || $(e).find("a,img,table").length > 0);
    const el = all.filter((_, e) => !all.toArray().some((o) => o !== e && $(o).find(e).length));
    if (el.length) { root = el; break; }
  }
  if (!root) return { tables: [], lines: [], files: [], images: [], text: "" };
  const abs = (h) => { try { return new URL(h, baseUrl).toString(); } catch { return null; } };
  const files = [...new Set(root.find("a[href]").map((_, a) => abs($(a).attr("href"))).get().filter((h) => h && /\.(pdf|xlsx?)(\?|#|$)/i.test(h)))];
  const images = [...new Set(root.find("img").map((_, i) => abs($(i).attr("data-src") || $(i).attr("src"))).get()
    .filter((s) => s && !/^data:|\.svg|logo|icon|banner|avatar|mob-?app|app_|qr/i.test(s)))];
  const tables = root.find("table").toArray().map((t) => {
    // The day a table is for may be in its heading: "Shanba kunlari ochiq bo'lgan filiallar" (SQB).
    let heading = "";
    let el = $(t);
    for (let k = 0; k < 4 && !heading; k++) {
      const prev = el.prevAll().slice(0, 3).text() + " " + el.parent().children().first().not(t).text();
      if (dayWords(prev).size || /dam olish|выходн/.test(latin(prev))) heading = clean(prev).slice(0, 300);
      el = el.parent();
      if (!el.length) break;
    }
    return { grid: htmlGrid($, t), heading };
  });
  const copy = root.clone();
  copy.find("table").remove();
  copy.find("br").replaceWith("\n");
  copy.find("p,li,div,h1,h2,h3,h4,h5,h6,tr,section,article").each((_, e) => { $(e).append("\n"); });
  const text = clean(copy.text());
  return { tables, lines: text.split("\n"), files, images, text };
}

/** Entries of one document (already downloaded). */
/**
 * tableEntries under the document's own dates: a document that names only other days (Agrobank's PDF next to its
 * 21–22 November table was the 7–8 November list) gives nothing, unless the table itself has this weekend's dates.
 */
function datedEntries(grid, ctx) {
  const r = tableEntries(grid, ctx);
  if (!ctx.stale) return r;
  const thisWeekend = grid.some((row) => row.some((v) => [...datesIn(v, ctx.days[0])].some((d) => ctx.days.includes(d))));
  return thisWeekend ? r : { entries: [], stale: true };
}

export async function entriesOf(doc, days, conf = {}) {
  const kind = kindOf(doc);
  if (kind === "pdf") {
    const pages = await pdfPages(doc.buf);
    const text = pages.map((p) => pdfLines(p.items).map((l) => l.text).join("\n")).join("\n");
    if (!text.trim()) return { kind, entries: [], image: true };
    const lines = pages.flatMap((p) => pdfLines(p.items));
    const ctx = pageContext(lines.filter((l) => l.items.length <= 2 && l.text.length > 25).map((l) => l.text).join("\n"), days);
    const { entries, stale } = datedEntries(pdfGrid(pages), ctx);
    return { kind, entries, stale: stale || (!entries.length && ctx.stale), text };
  }
  if (kind === "xlsx") {
    // Cells as typed in Excel: Ipoteka's addresses come with non-breaking spaces ("Тошкент\u00a0вилояти,\u00a0...").
    const grid = readXlsx(doc.buf).map((r) => r.map((v) => clean(v)));
    const text = grid.map((r) => [...new Set(r)].join(" ")).join("\n");
    // Defaults from the title lines (one text per row), not from the table's own cells.
    const ctx = pageContext(grid.filter((r) => new Set(r.filter((v) => v.trim())).size === 1).map((r) => r.find((v) => v.trim())).join("\n"), days);
    const { entries, stale } = datedEntries(grid, ctx);
    return { kind, entries, stale: stale || (!entries.length && ctx.stale), text };
  }
  if (kind === "image") return { kind, entries: [], image: true };
  return htmlEntries(doc.buf.toString("utf8"), doc.url, days, conf);
}

export function htmlEntries(html, url, days, conf = {}) {
  const page = readHtml(html, url, conf.root);
  const ctx = pageContext(page.text + "\n" + page.tables.map((t) => t.heading).join("\n"), days);
  const entries = [];
  let stale = false;
  for (const t of page.tables) {
    const tctx = t.heading ? { ...ctx, ...(() => { const c = pageContext(t.heading, days); return c.open.size ? { open: c.open } : {}; })() } : ctx;
    const r = datedEntries(t.grid, tctx);
    entries.push(...r.entries);
    stale ||= r.stale;
  }
  let pctx = ctx;
  if (!entries.length) {
    // The page's defaults come from its text around the list, not from a line of the list ("faqat 21-noyabr kuni").
    const first = proseEntries(page.lines, ctx);
    const raws = new Set(first.map((e) => e.raw));
    pctx = pageContext(page.lines.filter((l) => !raws.has(l)).join("\n"), days);
    entries.push(...(first.length ? proseEntries(page.lines, pctx) : first).map(({ raw, ...e }) => e));
  }
  return { kind: "html", entries, stale: stale || (!entries.length && pctx.stale), ctx: pctx, files: page.files, images: page.images, text: page.text, all: conf.all?.test(page.text) || false };
}

/* ---------- one bank ---------- */

/** The bank's list for this weekend. Never throws: problems become a status. */
export async function readBankList({ name, url }, days, { get = download } = {}) {
  const conf = bankConf(url);
  const result = { name, url, bank: conf.bank || name, status: "none", entries: [], read: [] };
  if (conf.regular) return { ...result, status: "regular" };
  try {
    let first;
    const apiUrl = conf.api?.(new URL(url));
    if (apiUrl) {
      const json = JSON.parse((await get(apiUrl)).buf.toString("utf8"));
      result.read.push(apiUrl);
      const d = json.data || json;
      const pick = (v) => (v && typeof v === "object" ? v.uz || v.ru || v.en || "" : v || "");
      const html = `<article><h1>${pick(d.title)}</h1>${pick(d.description)}${pick(d.content)}${pick(d.body)}</article>`;
      const files = [d.files, ...(d.extra_files || []).map((x) => x.files)].flat().filter(Boolean).map((f) => f.file?.url || f.url || f.file).filter((u) => typeof u === "string");
      first = { ...htmlEntries(html, url, days, { root: "article" }), files: files.filter((u) => /\.(pdf|xlsx?)(\?|$)/i.test(u)), images: files.filter((u) => /\.(png|jpe?g|webp)(\?|$)/i.test(u)) };
    } else {
      const doc = await get(url);
      result.read.push(url);
      first = await entriesOf(doc, days, conf);
      // A bot check (Hamkorbank sometimes serves one instead of its PDF): reported, never worked around.
      if (!first.entries.length && kindOf(doc) === "html" && doc.buf.length < 60000 && /cf-chl|challenge-platform|just a moment|verify you are human|ddos-guard|captcha/i.test(doc.buf.toString("utf8"))) {
        return { ...result, status: "blocked", note: "a bot check instead of the list" };
      }
    }
    if (first.all) return { ...result, status: "all", note: "all offices open on weekends" };
    const entries = [...first.entries];
    let stale = first.stale, image = first.image;
    // The announcement's files (the list itself is often a PDF or xlsx next to a sentence).
    for (const f of (first.files || []).slice(0, 4)) {
      try {
        const doc = await get(f);
        result.read.push(f);
        const r = await entriesOf(doc, days, conf);
        entries.push(...r.entries);
        stale ||= r.stale;
        image ||= r.image;
      } catch (e) { result.note = `${new URL(f).pathname.split("/").pop()}: ${e.message}`; }
    }
    const strong = entries.filter((e) => !e.weak);
    if (entries.length) return { ...result, status: "ok", entries: dedupe(strong.length ? strong : entries).map(({ weak, ...e }) => e) };
    if (stale) return { ...result, status: "stale", note: "the page is about another weekend" };
    if (image || (first.images || []).length) return { ...result, status: "image", note: "the list is a picture", images: (first.images || []).slice(0, 3) };
    return result;
  } catch (e) {
    return { ...result, status: "error", note: e.message };
  }
}

/** One entry per office: SQB lists an office open on both days in its Saturday table and again in its Sunday one. */
function dedupe(entries) {
  const byKey = new Map();
  for (const e of entries) {
    const k = latin(`${e.name}|${e.address}`).replace(/[^a-z0-9|]/g, "");
    const was = byKey.get(k);
    if (was) { for (const [d, h] of Object.entries(e.open)) if (!was.open[d]) was.open[d] = h; } else byKey.set(k, { ...e, open: { ...e.open } });
  }
  return [...byKey.values()];
}
