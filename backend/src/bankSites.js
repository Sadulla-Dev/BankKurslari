// Fallback source for bank cash rates: each bank's own website, read when bank.uz is down or blocks us.
// Where every bank keeps its rates (and why) is documented in ../BANK_RATE_SOURCES.md.
//
// fetchBankSites({ cbu }) -> { banks: [{ bank, sourceUrl, rates: { USD: { buy, sell }, ... } }], errors: [{ bank, error }] }
//
// `cbu` is { USD: 12004.48, ... } (CBU rate per 1 unit). HTML pages are read by a generic extractor that looks for a
// currency code followed by two numbers close to its CBU rate, so it needs the CBU rates; JSON sources ignore them.
// Bank names match bank.uz's, so logos, branches and favourites keep working when the source switches.
import * as cheerio from "cheerio";
import https from "node:https";
import http from "node:http";
import zlib from "node:zlib";

export const SITE_CURRENCIES = ["USD", "EUR", "RUB", "GBP", "KZT", "CHF", "JPY", "CNY"];
const ISO = { 840: "USD", 978: "EUR", 643: "RUB", 826: "GBP", 398: "KZT", 756: "CHF", 392: "JPY", 156: "CNY" };
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

/**
 * Plain node:https instead of fetch() (undici), because some bank servers trip it up:
 *  - Asakabank sends a header with a character undici rejects (insecureHTTPParser accepts it);
 *  - Kapitalbank's Cloudflare challenges undici's request but not a plain one;
 *  - Turon times out over IPv6 (family: 4).
 */
function request(url, { accept, timeout, redirects = 5 }) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https:") ? https : http;
    const req = lib.get(url, {
      headers: { "user-agent": UA, accept, "accept-language": "uz,ru;q=0.9,en;q=0.8", "accept-encoding": "gzip, deflate, br" },
      insecureHTTPParser: true, family: 4, timeout
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location && redirects > 0) {
        res.resume();
        return resolve(request(new URL(res.headers.location, url).toString(), { accept, timeout, redirects: redirects - 1 }));
      }
      const enc = res.headers["content-encoding"];
      const stream = enc === "gzip" ? res.pipe(zlib.createGunzip()) : enc === "deflate" ? res.pipe(zlib.createInflate()) : enc === "br" ? res.pipe(zlib.createBrotliDecompress()) : res;
      const chunks = [];
      stream.on("data", (c) => chunks.push(c));
      stream.on("end", () => {
        const body = Buffer.concat(chunks).toString("utf8");
        if (res.statusCode !== 200) return reject(new Error(`HTTP ${res.statusCode}`));
        resolve(body);
      });
      stream.on("error", reject);
    });
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.on("error", reject);
  });
}

async function get(url, { json = false, timeout = 20000 } = {}) {
  const accept = json ? "application/json, text/plain, */*" : "text/html,application/xhtml+xml,*/*;q=0.8";
  let text;
  try {
    text = await request(url, { accept, timeout });
  } catch (error) {
    if (/^HTTP 4/.test(error.message)) throw error; // a refusal, not a hiccup
    text = await request(url, { accept, timeout }); // one retry: single timeouts and resets happen
  }
  return json ? JSON.parse(text) : text;
}

/** "11950", "11 950.00", 1195000 (tiyin) ... -> number or null. "-", "", 0 -> null. */
function num(v, divide = 1) {
  if (v == null) return null;
  const n = typeof v === "number" ? v : Number(String(v).replace(/[\s  ]/g, "").replace(",", "."));
  return Number.isFinite(n) && n > 0 ? n / divide : null;
}

function put(rates, code, buy, sell) {
  if (!SITE_CURRENCIES.includes(code)) return;
  if (buy == null && sell == null) return;
  rates[code] = { buy, sell };
}

/* ---------- JSON sources ---------- */

const JSON_SOURCES = [
  {
    bank: "BRB", url: "https://brb.uz/api/currency/compare?type=1",
    parse(d) { const r = {}; for (const x of d.data || []) put(r, x.code, num(x.buy, 100), num(x.sell, 100)); return r; } // tiyin
  },
  {
    bank: "Tenge Bank", url: "https://tengebank.uz/api/exchangerates/tables",
    parse(d) { const r = {}; for (const [c, x] of Object.entries(d.personal?.[0]?.currency || {})) put(r, c, num(x.buy), num(x.sell)); return r; }
  },
  {
    bank: "Agrobank", url: "https://agrobank.uz/api/v1/?action=pages&code=uz/person&lang=uz",
    parse(d) {
      // The rates are a "currency-rates" block somewhere inside the page JSON.
      let items = null;
      (function walk(o) {
        if (items || !o || typeof o !== "object") return;
        if (o.type === "currency-rates" && Array.isArray(o.content?.items)) { items = o.content.items; return; }
        for (const v of Object.values(o)) walk(v);
      })(d);
      const r = {}; for (const x of items || []) put(r, x.alpha3, num(x.buy), num(x.sale)); return r;
    }
  },
  {
    bank: "O‘zsanoatqurilishbank", url: "https://sqb.uz/api/site-kurs-api/",
    parse(d) { const r = {}; for (const x of d.data?.offline || []) put(r, x.code, num(x.buy, 100), num(x.sell, 100)); return r; } // cash desk, tiyin
  },
  {
    bank: "Universal bank", url: "https://universalbank.uz/api/currencies/daily",
    parse(d) { const r = {}; for (const x of d.items || []) put(r, ISO[x.code], num(x.buyingRate), num(x.sellingRate)); return r; }
  },
  {
    bank: "Openbank", url: "https://api.smartbank.uz/retail/cbs-acc-mgmt/currency/rate",
    parse(d) { const r = {}; for (const x of Array.isArray(d) ? d : []) if (x.ccy2 === "UZS") put(r, x.ccy1, num(x.buyRate), num(x.sellRate)); return r; }
  },
  {
    bank: "Hayot Bank", url: "https://api.hayotbank.uz/api/curr-exchange-rate/get-all?size=2000&search=currExchangeRate.active=true",
    parse(d) { const r = {}; for (const x of d.data || []) put(r, ISO[x.currency?.code], num(x.buy), num(x.sell)); return r; }
  },
  {
    bank: "Ziraat Bank", url: "https://ziraatbank.uz/tr/GetCurrency",
    // Misleading names: value = buy, oldValue = sell.
    parse(d) { const r = {}; for (const x of Array.isArray(d) ? d : []) put(r, x.name, num(x.value), num(x.oldValue)); return r; }
  },
  {
    bank: "Asakabank", url: "https://back.asakabank.uz/core/v1/currency-list/",
    async load() {
      const rows = [];
      let url = this.url;
      for (let i = 0; url && i < 5; i++) { const d = await get(url, { json: true }); rows.push(...(d.results || [])); url = d.links?.next?.replace(/^http:/, "https:"); }
      return rows;
    },
    parse(rows) { const r = {}; for (const x of rows) if (x.currency_type === 1) put(r, x.short_name, num(x.purchase), num(x.sale)); return r; } // individuals
  },
  {
    bank: "Hamkorbank", url: "https://api-dbo.hamkorbank.uz/webflow/v1/exchanges",
    // destination 2 = cash desk (7 = in the app); begin_sum_i > 0 = rates for large amounts. Tiyin.
    parse(d) { const r = {}; for (const x of d.data || []) if (String(x.destination_code) === "2" && !Number(x.begin_sum_i)) put(r, String(x.currency_char).toUpperCase(), num(x.buying_rate, 100), num(x.selling_rate, 100)); return r; }
  },
  {
    bank: "Xalq Banki", url: "https://xb.uz/api/v1/external/client/default?_f=json&_l=uz&destination=2",
    parse(d) { const r = {}; for (const x of Array.isArray(d) ? d : []) put(r, x.title, num(x.BUYING_RATE), num(x.SELLING_RATE)); return r; }
  }
];

/* ---------- HTML sources (generic extractor) ---------- */

const HTML_SOURCES = [
  { bank: "Anorbank", url: "https://anorbank.uz/about/exchange-rates/" },
  { bank: "Asia Alliance Bank", url: "https://aab.uz/uz/" },
  { bank: "O‘zbekiston Milliy banki", url: "https://nbu.uz/jismoniy-shaxslar-valyutalar-kursi" },
  { bank: "Kapitalbank", url: "https://kapitalbank.uz/uz/" },
  {
    bank: "Infinbank", url: "https://www.infinbank.com/uz/",
    // The calculator tabs carry the rates: <div class="calc__tabs__item" data-rates="…,UZS:11960/12060=…">USD</div>
    parse(html) {
      const $ = cheerio.load(html);
      const r = {};
      $("[data-rates]").each((_i, el) => {
        const code = $(el).text().trim();
        const m = /(?:^|,)UZS:([\d.]+)\/([\d.]+)/.exec($(el).attr("data-rates") || "");
        if (m && !r[code]) put(r, code, num(m[1]), num(m[2]));
      });
      return r;
    }
  },
  { bank: "APEXBANK", url: "https://apexbank.uz/about/exchange-rates/" },
  { bank: "Trastbank", url: "https://trustbank.uz/uz/" },
  { bank: "KDB Bank Uzbekiston", url: "https://kdb.uz/uz" },
  { bank: "Orient Finans Bank", url: "https://ofb.uz/kursy-valyut" },
  {
    bank: "Ipak Yuli Bank", url: "https://ipakyulibank.uz/physical/valyuta-ayirboshlash",
    // The page shows the in-app rates ("Ilovada"); the cash-desk tab ("Kassada") is only in the Nuxt payload.
    parse(html) {
      const $ = cheerio.load(html);
      const raw = $("#__NUXT_DATA__").text();
      if (!raw) return {};
      const data = JSON.parse(raw);
      // Nuxt "devalue" format: a flat array where objects and arrays hold indexes into it.
      const memo = new Map();
      const val = (i) => {
        if (typeof i !== "number") return i;
        if (memo.has(i)) return memo.get(i);
        const v = data[i];
        let out = v;
        if (Array.isArray(v)) { out = []; memo.set(i, out); for (const x of v) out.push(val(x)); }
        else if (v && typeof v === "object") { out = {}; memo.set(i, out); for (const [k, x] of Object.entries(v)) out[k] = val(x); }
        else memo.set(i, out);
        return out;
      };
      const r = {};
      data.forEach((v, i) => {
        if (v && typeof v === "object" && !Array.isArray(v) && "name" in v && "rates" in v && data[v.name] === "Kassada") {
          for (const x of val(i).rates || []) put(r, x?.code_name, num(x?.rate?.buy, 100), num(x?.rate?.sell, 100)); // tiyin
        }
      });
      return r;
    }
  },
  { bank: "Aloqabank", url: "https://aloqabank.uz/uz/" },
  { bank: "Octobank", url: "https://octobank.uz/" },
  // Several rate columns; only USD reliably reads as the cash one (others came out ~50-100 so'm off), so USD only.
  { bank: "Davr bank", url: "https://abbdavr-bank.uz/uz", only: ["USD"] },
  { bank: "MKBank", url: "https://mkbank.uz/uz/" },
  { bank: "Ipoteka bank", url: "https://www.ipotekabank.uz/" },
  { bank: "Garant bank", url: "https://garantbank.uz/uz" },
  // Rates widget is a lazily loaded HTML fragment; its first table is the cash desk (then app, ATM).
  { bank: "Turon bank", url: "https://turonbank.uz/uz/lazyload/main-page.php" },
  { bank: "Poytaxt bank", url: "https://poytaxtbank.uz/uz/lazyload/main-page.php" }
];

const NUM_RE = /\d{1,3}(?:[\s  ]\d{3})+(?:[.,]\d+)?|\d+(?:[.,]\d+)?/g;

function parseNum(token) {
  let t = token.replace(/[\s  ]/g, "");
  // "11,950" is a thousands separator; "26,10" / "13 155,71" are decimals.
  if (/^\d{1,3},\d{3}$/.test(t)) t = t.replace(",", "");
  return Number(t.replace(",", "."));
}

/**
 * Page text -> { CODE: { buy, sell } }. For each currency code, the first place where it is followed (before the next
 * currency code) by two numbers within ±50% of its CBU rate. The CBU rate itself (often printed next to them) and
 * dates/times are skipped. buy = smaller, sell = larger (a bank always sells dearer than it buys).
 */
export function extractRates(html, cbu) {
  const $ = cheerio.load(html);
  $("script, style, noscript, svg").remove();
  // Tags become spaces, so "<span>USD</span><span>11915</span>" does not read as "USD11915".
  const fromText = extractFromText(cheerio.load(($("body").html() || $.root().html() || "").replace(/<[^>]+>/g, " ")).root().text(), cbu);
  if (fromText.USD) return fromText;
  // Some pages carry the rates only in an embedded data script (Nuxt/Next payload): read the raw HTML too.
  return extractFromText(html.replace(/<[^>]+>/g, " ").replace(/\\u00a0|&nbsp;/g, " ").replace(/["',:{}[\]]/g, " "), cbu);
}

function extractFromText(raw, cbu) {
  let text = raw;
  text = text
    .replace(/\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b/g, " ") // dates
    .replace(/\b\d{1,2}:\d{2}(?::\d{2})?\b/g, " ") // times
    .replace(/[\s  ]+/g, (m) => (/[  ]/.test(m) ? " " : " "));
  const codes = SITE_CURRENCIES.filter((c) => cbu[c]);
  const anyCode = new RegExp(`\\b(${SITE_CURRENCIES.join("|")})\\b`, "g");
  const out = {};
  for (const code of codes) {
    const rate = cbu[code];
    const re = new RegExp(`\\b${code}\\b`, "g");
    let m;
    while (!out[code] && (m = re.exec(text))) {
      const start = m.index + code.length;
      anyCode.lastIndex = start;
      const next = anyCode.exec(text);
      const window = text.slice(start, Math.min(next ? next.index : text.length, start + 400));
      const vals = [];
      for (const tok of window.match(NUM_RE) || []) {
        const v = parseNum(tok);
        if (!(v >= rate * 0.5 && v <= rate * 1.5)) continue;
        if (Math.abs(v - rate) < 0.006) continue; // the CBU rate printed beside the bank's
        // Cash rates above 100 so'm are whole numbers (or .00); "11 971,64" is a CBU rate (today's or yesterday's).
        if (rate > 100 && /[.,]\d*[1-9]/.test(tok) && !/^\d{1,3},\d{3}$/.test(tok.replace(/[\s\u00a0\u202f]/g, ""))) continue;
        vals.push(v);
        if (vals.length === 2) break;
      }
      // Two equal numbers are one value read twice (e.g. a single rate column), not a buy/sell pair.
      if (vals.length === 2 && vals[0] !== vals[1]) out[code] = { buy: Math.min(...vals), sell: Math.max(...vals) };
    }
  }
  return out;
}

/* ---------- all banks ---------- */

async function pool(items, size, fn) {
  const out = new Array(items.length);
  let i = 0;
  await Promise.all(Array.from({ length: size }, async () => { while (i < items.length) { const k = i++; out[k] = await fn(items[k]); } }));
  return out;
}

export async function fetchBankSites({ cbu }) {
  const jobs = [
    ...JSON_SOURCES.map((s) => async () => s.parse(s.load ? await s.load() : await get(s.url, { json: true }))),
    ...HTML_SOURCES.map((s) => async () => {
      const html = await get(s.url);
      const r = s.parse ? s.parse(html) : extractRates(html, cbu);
      if (s.only) for (const c of Object.keys(r)) if (!s.only.includes(c)) delete r[c];
      return r;
    })
  ];
  const sources = [...JSON_SOURCES, ...HTML_SOURCES];
  const banks = [];
  const errors = [];
  await pool(jobs.map((job, k) => [job, sources[k]]), 6, async ([job, s]) => {
    try {
      const rates = await job();
      if (!rates.USD) throw new Error("no USD rate found");
      banks.push({ bank: s.bank, sourceUrl: s.url, rates });
    } catch (error) {
      errors.push({ bank: s.bank, error: error.message });
    }
  });
  return { banks, errors };
}

export const BANK_SITE_COUNT = JSON_SOURCES.length + HTML_SOURCES.length;
