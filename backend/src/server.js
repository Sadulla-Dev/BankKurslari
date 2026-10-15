import * as cheerio from "cheerio";
import express from "express";
import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const DATA_DIR = join(dirname(fileURLToPath(import.meta.url)), "..", "data");

// Loads a pre-scraped static JSON dataset (bank.uz branches/ATMs), or null if absent.
async function loadStatic(file) {
  try {
    return JSON.parse(await readFile(join(DATA_DIR, file), "utf8"));
  } catch {
    return null;
  }
}

const PORT = Number(process.env.PORT || 8080);
// A single bank.uz currency page contains tabs for every currency (#best_USD, #best_EUR, #best_RUB ...).
const BANK_UZ_PAGE = "https://bank.uz/uz/currency/dollar-ssha-uz-aqsh-dollari";
const CBU_ALL_URL = "https://cbu.uz/uz/arkhiv-kursov-valyut/json/";
const cbuByDateUrl = (cur, date) => `https://cbu.uz/uz/arkhiv-kursov-valyut/json/${cur}/${date}/`;
const CACHE_TTL_MS = Number(process.env.CACHE_TTL_MS || 15 * 60 * 1000);
const ATM_CACHE_TTL_MS = Number(process.env.ATM_CACHE_TTL_MS || 24 * 60 * 60 * 1000);
const HISTORY_TTL_MS = Number(process.env.HISTORY_TTL_MS || 12 * 60 * 60 * 1000);
// How far back history goes and how finely it is sampled per age band.
const HISTORY_START = process.env.HISTORY_START || "2009-01-01";
const RECENT_DAILY_DAYS = Number(process.env.RECENT_DAILY_DAYS || 60); // last 60 days: daily
const WEEKLY_UNTIL_DAYS = Number(process.env.WEEKLY_UNTIL_DAYS || 730); // up to 2y: weekly
const BIWEEKLY_UNTIL_DAYS = Number(process.env.BIWEEKLY_UNTIL_DAYS || 1830); // up to 5y: every 2 weeks; older: monthly
// Past CBU rates never change: keep them on disk so a restart only fetches what is missing.
const HISTORY_DISK_FRESH_DAYS = 7; // re-fetch the last week (late corrections), trust older stored values
const OVERPASS_URL = process.env.OVERPASS_URL || "https://overpass-api.de/api/interpreter";
// Yandex Places (organization search) API — set YANDEX_PLACES_KEY to get full branch coverage.
const YANDEX_PLACES_KEY = process.env.YANDEX_PLACES_KEY || "";
const YANDEX_PLACES_URL = "https://search-maps.yandex.ru/v1/";
// Tashkent center (lon,lat) and span (dlon,dlat) for the Yandex search window.
const TASHKENT_LL = "69.2797,41.3111";
const TASHKENT_SPN = "0.60,0.48";
// Tashkent bounding box (south, west, north, east).
const TASHKENT_BBOX = "41.15,69.10,41.40,69.40";

const SUPPORTED = ["USD", "EUR", "RUB"];

const app = express();
// Per-currency caches, keyed by currency code.
const ratesCache = new Map();
const historyCache = new Map();
let cbuAllCache = null; // { cachedAt, map: { USD: {...}, ... } }
let bankPageCache = null; // { cachedAt, html }
let cachedAtms = null;
let cachedBranches = null;

// Approximate Tashkent head-office / main currency branch coordinates per bank.
// Keyed by normalizeBankName(bank). Used by the app to open Google Maps navigation.
const BANK_BRANCHES = {
  "kapitalbank": { address: "Toshkent, Nukus ko'chasi 88A", lat: 41.2869, lng: 69.2406 },
  "asia alliance bank": { address: "Toshkent, Mahtumquli ko'chasi 2A", lat: 41.3111, lng: 69.2797 },
  "hamkorbank": { address: "Toshkent, Shahrisabz ko'chasi 1", lat: 41.3056, lng: 69.2869 },
  "ipoteka bank": { address: "Toshkent, Shahrisabz ko'chasi 30", lat: 41.3122, lng: 69.2850 },
  "ozbekiston milliy banki": { address: "Toshkent, Amir Temur shoh ko'chasi 101", lat: 41.3306, lng: 69.2847 },
  "agrobank": { address: "Toshkent, Mukimiy ko'chasi 43", lat: 41.2808, lng: 69.2211 },
  "anorbank": { address: "Toshkent, Afrosiyob ko'chasi 4B", lat: 41.3049, lng: 69.2699 },
  "mkbank": { address: "Toshkent, Nukus ko'chasi 3", lat: 41.2946, lng: 69.2417 },
  "hayot bank": { address: "Toshkent, Shota Rustaveli ko'chasi 1", lat: 41.2883, lng: 69.2436 },
  "ozsanoatqurilishbank": { address: "Toshkent, Shahrisabz tor ko'chasi 3", lat: 41.3127, lng: 69.2887 },
  "turon bank": { address: "Toshkent, Osiyo ko'chasi 36", lat: 41.3284, lng: 69.2881 },
  "xalq banki": { address: "Toshkent, Kichik halqa yo'li 3", lat: 41.2733, lng: 69.2056 },
  "aloqabank": { address: "Toshkent, Amir Temur shoh ko'chasi 4", lat: 41.3167, lng: 69.2794 },
  "openbank": { address: "Toshkent, Islom Karimov ko'chasi 1", lat: 41.3039, lng: 69.2688 },
  "trastbank": { address: "Toshkent, Istiqbol ko'chasi 17", lat: 41.3102, lng: 69.2778 },
  "ipak yuli bank": { address: "Toshkent, Abdulla Qodiriy ko'chasi 1", lat: 41.3333, lng: 69.2878 },
  "orient finans bank": { address: "Toshkent, Shahrisabz ko'chasi 25", lat: 41.3119, lng: 69.2842 },
  "octobank": { address: "Toshkent, Muqimiy ko'chasi 178", lat: 41.2783, lng: 69.2039 },
  "universal bank": { address: "Toshkent, Bobur ko'chasi 1", lat: 41.2978, lng: 69.2631 },
  "garant bank": { address: "Toshkent, Afrosiyob ko'chasi 12", lat: 41.3056, lng: 69.2711 },
  "tenge bank": { address: "Toshkent, Sharof Rashidov ko'chasi 24", lat: 41.3194, lng: 69.2611 },
  "infinbank": { address: "Toshkent, Bobur ko'chasi 51", lat: 41.2961, lng: 69.2658 },
  "poytaxt bank": { address: "Toshkent, Olmazor ko'chasi 171", lat: 41.3419, lng: 69.2439 },
  "ziraat bank": { address: "Toshkent, Afrosiyob ko'chasi 1B", lat: 41.3044, lng: 69.2694 },
  "kdb bank uzbekiston": { address: "Toshkent, Amir Temur shoh ko'chasi 107B", lat: 41.3353, lng: 69.2858 },
  "brb": { address: "Toshkent, Chilonzor ko'chasi 2", lat: 41.2856, lng: 69.2039 },
  "asakabank": { address: "Toshkent, Nukus ko'chasi 67", lat: 41.2889, lng: 69.2422 }
};

function normalizeCurrency(raw) {
  const cur = String(raw || "").toUpperCase();
  return SUPPORTED.includes(cur) ? cur : null;
}

app.get("/health", (_request, response) => {
  response.json({ ok: true, currencies: SUPPORTED });
});

// Backwards-compatible alias for the original USD-only endpoint.
app.get("/api/rates/usd", (request, response) => handleRates("USD", response));
app.get("/api/rates/:cur", (request, response) => {
  const cur = normalizeCurrency(request.params.cur);
  if (!cur) return response.status(404).json({ error: "unsupported_currency", supported: SUPPORTED });
  return handleRates(cur, response);
});

async function handleRates(cur, response) {
  try {
    const payload = await getRatesPayload(cur);
    response.json(payload);
  } catch (error) {
    const cached = ratesCache.get(cur);
    if (cached) {
      response.json(cached.payload);
      return;
    }
    response.status(502).json({
      error: "rates_unavailable",
      message: error instanceof Error ? error.message : "Unknown upstream error"
    });
  }
}

app.get("/api/history/:cur", async (request, response) => {
  const cur = normalizeCurrency(request.params.cur);
  if (!cur) return response.status(404).json({ error: "unsupported_currency", supported: SUPPORTED });
  try {
    const points = await getHistory(cur);
    response.json({ currency: cur, updatedAt: new Date().toISOString(), points });
  } catch (error) {
    const cached = historyCache.get(cur);
    if (cached) return response.json({ currency: cur, updatedAt: new Date(cached.cachedAt).toISOString(), points: cached.points });
    response.status(502).json({ error: "history_unavailable", message: error instanceof Error ? error.message : "Unknown error" });
  }
});

app.get("/api/analytics/:cur", async (request, response) => {
  const cur = normalizeCurrency(request.params.cur);
  if (!cur) return response.status(404).json({ error: "unsupported_currency", supported: SUPPORTED });
  try {
    const points = await getHistory(cur);
    response.json(computeAnalytics(cur, points));
  } catch (error) {
    response.status(502).json({ error: "analytics_unavailable", message: error instanceof Error ? error.message : "Unknown error" });
  }
});

app.get("/api/branches", async (_request, response) => {
  try {
    const branches = await getBranches();
    response.json({ updatedAt: new Date().toISOString(), count: branches.length, branches });
  } catch (error) {
    if (cachedBranches) {
      response.json({ updatedAt: new Date(cachedBranches.cachedAt).toISOString(), count: cachedBranches.branches.length, branches: cachedBranches.branches });
      return;
    }
    response.status(502).json({ error: "branches_unavailable", message: error instanceof Error ? error.message : "Unknown error" });
  }
});

app.get("/api/atms", async (_request, response) => {
  try {
    const atms = await getAtms();
    response.json({ updatedAt: new Date().toISOString(), count: atms.length, atms });
  } catch (error) {
    if (cachedAtms) {
      response.json({ updatedAt: new Date(cachedAtms.cachedAt).toISOString(), count: cachedAtms.atms.length, atms: cachedAtms.atms });
      return;
    }
    response.status(502).json({
      error: "atms_unavailable",
      message: error instanceof Error ? error.message : "Unknown upstream error"
    });
  }
});

app.listen(PORT, () => {
  console.log(`ValyutaStat backend listening on http://localhost:${PORT}`);
  // Warm caches at boot so client requests always hit ready payloads.
  refreshAllRates();
  setInterval(refreshAllRates, CACHE_TTL_MS).unref();
  // Warm every currency one after another (cold start fetches years of CBU data; disk cache makes later starts fast).
  (async () => {
    for (const cur of SUPPORTED) {
      await getHistory(cur)
        .then((p) => console.log(`${cur} history warmed: ${p.length} points`))
        .catch((e) => console.error(`${cur} history warm failed:`, e.message));
    }
  })();
  getAtms().then((a) => console.log(`ATM cache warmed: ${a.length}`)).catch((e) => console.error("ATM warm failed:", e.message));
  getBranches().then((b) => console.log(`Branch cache warmed: ${b.length}`)).catch((e) => console.error("Branch warm failed:", e.message));
  setInterval(() => getAtms({ force: true }).catch(() => {}), ATM_CACHE_TTL_MS).unref();
  setInterval(() => getBranches({ force: true }).catch(() => {}), ATM_CACHE_TTL_MS).unref();
});

async function refreshAllRates() {
  for (const cur of SUPPORTED) {
    try {
      await getRatesPayload(cur, { force: true });
    } catch (error) {
      console.error(`${cur} refresh failed:`, error instanceof Error ? error.message : error);
    }
  }
  console.log(`Rates refreshed at ${new Date().toISOString()}`);
}

/* ---------- rates ---------- */

async function getRatesPayload(cur, { force = false } = {}) {
  const cached = ratesCache.get(cur);
  if (!force && cached && Date.now() - cached.cachedAt < CACHE_TTL_MS) {
    return cached.payload;
  }

  const [cbuMap, commercialResult] = await Promise.all([
    getCbuCurrent(),
    fetchCommercialRates(cur).catch((error) => {
      console.error(`Commercial ${cur} rates failed:`, error.message);
      return [];
    })
  ]);

  // Drop scraping artifacts: bank.uz occasionally returns a rate with a missing
  // digit (e.g. 1155 instead of 11550). Reject values far from the official rate.
  const cbuRate = cbuMap[cur]?.rate;
  const commercialRates = sanitizeRates(commercialResult, cbuRate);

  const bestBuy = commercialRates
    .filter((rate) => Number.isFinite(rate.buy))
    .sort((left, right) => right.buy - left.buy)[0] ?? null;
  const bestSell = commercialRates
    .filter((rate) => Number.isFinite(rate.sell))
    .sort((left, right) => left.sell - right.sell)[0] ?? null;

  const payload = {
    currency: cur,
    updatedAt: new Date().toISOString(),
    sources: { cbu: CBU_ALL_URL, commercialBanks: BANK_UZ_PAGE },
    cbu: cbuMap[cur] ?? null,
    bestBuy,
    bestSell,
    banks: commercialRates
  };

  ratesCache.set(cur, { cachedAt: Date.now(), payload });
  return payload;
}

function sanitizeRates(rates, cbuRate) {
  if (!Number.isFinite(cbuRate)) return rates;
  const lo = cbuRate * 0.5;
  const hi = cbuRate * 1.5;
  const inBand = (v) => Number.isFinite(v) && v >= lo && v <= hi;

  return rates
    .map((r) => {
      const buy = inBand(r.buy) ? r.buy : null;
      const sell = inBand(r.sell) ? r.sell : null;
      return {
        ...r,
        buy,
        sell,
        spread: buy != null && sell != null ? sell - buy : null
      };
    })
    .filter((r) => r.buy != null || r.sell != null);
}

async function getCbuCurrent() {
  if (cbuAllCache && Date.now() - cbuAllCache.cachedAt < CACHE_TTL_MS) {
    return cbuAllCache.map;
  }
  const data = await fetchJson(CBU_ALL_URL);
  const map = {};
  for (const row of Array.isArray(data) ? data : []) {
    if (!SUPPORTED.includes(row.Ccy)) continue;
    map[row.Ccy] = {
      code: row.Ccy,
      rate: parseFloat(String(row.Rate).replace(",", ".")),
      diff: parseFloat(String(row.Diff ?? "0").replace(",", ".")),
      date: row.Date ?? null
    };
  }
  cbuAllCache = { cachedAt: Date.now(), map };
  return map;
}

async function getBankPage() {
  if (bankPageCache && Date.now() - bankPageCache.cachedAt < CACHE_TTL_MS) {
    return bankPageCache.html;
  }
  const html = await fetchText(BANK_UZ_PAGE);
  bankPageCache = { cachedAt: Date.now(), html };
  return html;
}

async function fetchCommercialRates(cur) {
  const html = await getBankPage();
  const $ = cheerio.load(html);
  const tab = $(`#best_${cur}`);

  if (!tab.length) {
    throw new Error(`Bank.uz ${cur} rates tab was not found`);
  }

  const byBank = new Map();
  parseRateColumn($, tab.find(".bc-inner-block-left").first(), "buy", byBank);
  parseRateColumn($, tab.find(".bc-inner-blocks-right").first(), "sell", byBank);

  const rates = [...byBank.values()]
    .filter((rate) => Number.isFinite(rate.buy) || Number.isFinite(rate.sell))
    .map((rate) => ({
      bank: rate.bank,
      buy: Number.isFinite(rate.buy) ? rate.buy : null,
      sell: Number.isFinite(rate.sell) ? rate.sell : null,
      spread: Number.isFinite(rate.buy) && Number.isFinite(rate.sell) ? rate.sell - rate.buy : null,
      sourceUrl: rate.sourceUrl,
      branch: BANK_BRANCHES[normalizeBankName(rate.bank)] ?? null
    }))
    .sort((left, right) => {
      const leftBuy = left.buy ?? Number.NEGATIVE_INFINITY;
      const rightBuy = right.buy ?? Number.NEGATIVE_INFINITY;
      return rightBuy - leftBuy || (left.sell ?? 999999) - (right.sell ?? 999999);
    });

  if (!rates.length) {
    throw new Error(`Bank.uz ${cur} rates could not be parsed`);
  }

  return rates;
}

function parseRateColumn($, column, field, byBank) {
  column.find(".bc-inner-block-left-texts").each((_index, row) => {
    const name = cleanText($(row).find("a .medium-text").first().text());
    const amount = parseAmount($(row).find(".green-date").first().text());
    const href = $(row).find("a").first().attr("href");

    if (!name || !Number.isFinite(amount)) {
      return;
    }

    const key = normalizeBankName(name);
    const current = byBank.get(key) ?? {
      bank: name,
      buy: null,
      sell: null,
      sourceUrl: href ? new URL(href, BANK_UZ_PAGE).toString() : BANK_UZ_PAGE
    };

    current[field] = amount;
    byBank.set(key, current);
  });
}

/* ---------- history ---------- */

async function getHistory(cur, { force = false } = {}) {
  const cached = historyCache.get(cur);
  if (!force && cached && Date.now() - cached.cachedAt < HISTORY_TTL_MS) {
    return cached.points;
  }

  const dates = historySampleDates();
  const stored = await loadHistoryDisk(cur);
  const freshFrom = new Date(Date.now() - HISTORY_DISK_FRESH_DAYS * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const missing = dates.filter((d) => d >= freshFrom || !Number.isFinite(stored[d]));
  // Fetch in small concurrent batches to stay polite to cbu.uz.
  const BATCH = 8;
  for (let i = 0; i < missing.length; i += BATCH) {
    const slice = missing.slice(i, i + BATCH);
    const results = await Promise.all(slice.map((date) => fetchCbuRateOn(cur, date).catch(() => null)));
    results.forEach((rate, idx) => {
      if (Number.isFinite(rate)) stored[slice[idx]] = rate;
    });
  }
  await saveHistoryDisk(cur, stored).catch((e) => console.error(`History save failed (${cur}):`, e.message));

  const points = dates.filter((d) => Number.isFinite(stored[d])).map((date) => ({ date, rate: stored[date] }));
  points.sort((a, b) => (a.date < b.date ? -1 : 1));
  historyCache.set(cur, { cachedAt: Date.now(), points });
  return points;
}

const historyFile = (cur) => join(DATA_DIR, `history-${cur}.json`);

async function loadHistoryDisk(cur) {
  try {
    return JSON.parse(await readFile(historyFile(cur), "utf8"));
  } catch {
    return {};
  }
}

async function saveHistoryDisk(cur, map) {
  await writeFile(historyFile(cur), JSON.stringify(map));
}

async function fetchCbuRateOn(cur, date) {
  const data = await fetchJson(cbuByDateUrl(cur, date));
  const row = Array.isArray(data) ? data[0] : data;
  if (!row?.Rate) return Number.NaN;
  return parseFloat(String(row.Rate).replace(",", "."));
}

// Builds a de-duplicated, oldest-first list of sample dates covering HISTORY_DAYS with
// adaptive density: daily for the recent band, then weekly, then monthly for old data.
function historySampleDates() {
  const DAY = 24 * 60 * 60 * 1000;
  const today = new Date();
  const set = new Set();
  const historyDays = Math.floor((today.getTime() - Date.parse(HISTORY_START)) / DAY);
  const add = (daysAgo) => {
    if (daysAgo < 0 || daysAgo > historyDays) return;
    set.add(new Date(today.getTime() - daysAgo * DAY).toISOString().slice(0, 10));
  };

  for (let d = 0; d < RECENT_DAILY_DAYS; d++) add(d);
  for (let d = RECENT_DAILY_DAYS; d <= WEEKLY_UNTIL_DAYS; d += 7) add(d);
  for (let d = WEEKLY_UNTIL_DAYS; d <= BIWEEKLY_UNTIL_DAYS; d += 14) add(d);
  for (let d = BIWEEKLY_UNTIL_DAYS; d <= historyDays; d += 30) add(d);
  add(historyDays);

  return [...set].sort(); // ISO strings sort chronologically
}

/* ---------- analytics ---------- */

function computeAnalytics(cur, points) {
  const updatedAt = new Date().toISOString();
  if (points.length < 2) {
    return { currency: cur, updatedAt, enoughData: false };
  }

  const n = points.length;
  const current = points[n - 1].rate;
  // Rate ~7 days ago (fallback to the earliest available).
  const weekAgoPoint = points[Math.max(0, n - 8)];
  const weekAgo = weekAgoPoint.rate;
  const weekChangePct = round2(((current - weekAgo) / weekAgo) * 100);

  // Linear regression slope (rate per day) over the last up-to-14 points.
  const window = points.slice(Math.max(0, n - 14));
  const slope = linearSlope(window.map((p, i) => [i, p.rate]));
  const forecastRate = round2(current + slope * 7);
  const forecastPct = round2(((forecastRate - current) / current) * 100);

  const THRESHOLD = 0.15; // percent over a week considered significant
  let trend = "flat";
  if (forecastPct > THRESHOLD) trend = "up";
  else if (forecastPct < -THRESHOLD) trend = "down";

  // Day-of-month seasonality: average daily change within each third of the month.
  const buckets = { start: [], mid: [], end: [] };
  for (let i = 1; i < n; i++) {
    const day = Number(points[i].date.slice(8, 10));
    const change = points[i].rate - points[i - 1].rate;
    const key = day <= 10 ? "start" : day <= 20 ? "mid" : "end";
    buckets[key].push(change);
  }
  const seasonality = {
    start: round2(avg(buckets.start)),
    mid: round2(avg(buckets.mid)),
    end: round2(avg(buckets.end))
  };
  // Period where the rate historically rises the most (best time to have sold).
  const bestRisePeriod = ["start", "mid", "end"].reduce((best, k) =>
    seasonality[k] > seasonality[best] ? k : best, "start");

  // Advice: if the rate is expected to rise, sellers should wait and buyers act now.
  let sell = "now";
  let buy = "now";
  if (trend === "up") { sell = "wait"; buy = "now"; }
  else if (trend === "down") { sell = "now"; buy = "wait"; }

  return {
    currency: cur,
    updatedAt,
    enoughData: true,
    points: points.length,
    current,
    weekAgo,
    weekChangePct,
    trend,              // 'up' | 'down' | 'flat'
    forecastRate,
    forecastPct,
    seasonality,        // avg daily change (UZS) per month-third
    bestRisePeriod,     // 'start' | 'mid' | 'end'
    recommendation: { sell, buy } // 'now' | 'wait'
  };
}

function linearSlope(pairs) {
  const n = pairs.length;
  if (n < 2) return 0;
  let sx = 0, sy = 0, sxy = 0, sxx = 0;
  for (const [x, y] of pairs) { sx += x; sy += y; sxy += x * y; sxx += x * x; }
  const denom = n * sxx - sx * sx;
  if (denom === 0) return 0;
  return (n * sxy - sx * sy) / denom;
}

function avg(arr) {
  return arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0;
}

function round2(x) {
  return Math.round(x * 100) / 100;
}

/* ---------- ATMs ---------- */

async function getBranches({ force = false } = {}) {
  if (!force && cachedBranches && Date.now() - cachedBranches.cachedAt < ATM_CACHE_TTL_MS) {
    return cachedBranches.branches;
  }

  // Prefer the pre-scraped bank.uz dataset (full coverage, offline, free).
  const staticBranches = await loadStatic("branches.json");
  if (staticBranches && staticBranches.length) {
    cachedBranches = { cachedAt: Date.now(), branches: staticBranches };
    return staticBranches;
  }

  // Otherwise fall back to Yandex (if keyed) or OSM.
  let branches;
  if (YANDEX_PLACES_KEY) {
    branches = await fetchYandexBranches().catch((error) => {
      console.error("Yandex branches failed, falling back to OSM:", error.message);
      return null;
    });
  }
  if (!branches || !branches.length) {
    branches = await fetchOsmBranches();
  }

  cachedBranches = { cachedAt: Date.now(), branches };
  return branches;
}

async function fetchYandexBranches() {
  const params = new URLSearchParams({
    apikey: YANDEX_PLACES_KEY,
    text: "банк",
    lang: "ru_RU",
    ll: TASHKENT_LL,
    spn: TASHKENT_SPN,
    type: "biz",
    results: "500"
  });
  const response = await fetch(`${YANDEX_PLACES_URL}?${params.toString()}`, {
    headers: { accept: "application/json" }
  });
  if (!response.ok) {
    throw new Error(`Yandex Places HTTP ${response.status}`);
  }
  const data = await response.json();
  return (data.features ?? [])
    .map((f) => {
      const meta = f.properties?.CompanyMetaData ?? {};
      const [lng, lat] = f.geometry?.coordinates ?? [];
      return {
        id: meta.id ?? null,
        bank: cleanText(meta.name || "Bank"),
        name: cleanText(meta.name || "Bank"),
        address: meta.address ? cleanText(meta.address) : null,
        hours: meta.Hours?.text ? cleanText(meta.Hours.text) : null,
        lat,
        lng
      };
    })
    .filter((b) => Number.isFinite(b.lat) && Number.isFinite(b.lng));
}

// Wider greater-Tashkent bbox for branch coverage.
const BRANCH_BBOX = "41.13,69.08,41.45,69.48";

async function fetchOsmBranches() {
  // Nodes + way centers, banks + financial offices — maximises free OSM coverage.
  const query = `[out:json][timeout:40];
(
  node["amenity"="bank"](${BRANCH_BBOX});
  way["amenity"="bank"](${BRANCH_BBOX});
  node["office"="financial"](${BRANCH_BBOX});
  way["office"="financial"](${BRANCH_BBOX});
);
out center;`;
  const data = await fetchOverpass(query);
  const seen = new Set();
  return (data.elements ?? [])
    .map((el) => {
      const lat = el.lat ?? el.center?.lat;
      const lng = el.lon ?? el.center?.lon;
      return { el, lat, lng };
    })
    .filter(({ lat, lng }) => Number.isFinite(lat) && Number.isFinite(lng))
    .map(({ el, lat, lng }) => {
      const tags = el.tags ?? {};
      const addrParts = [tags["addr:street"], tags["addr:housenumber"]].filter(Boolean);
      return {
        id: el.id,
        bank: cleanText(tags.operator || tags.name || tags.brand || "Bank"),
        name: cleanText(tags.name || tags.operator || "Bank"),
        address: addrParts.length ? cleanText(addrParts.join(" ")) : null,
        hours: tags.opening_hours ? cleanText(tags.opening_hours) : null,
        lat,
        lng
      };
    })
    .filter((b) => {
      if (!b.name || b.name === "Bank") return false;
      // Drop near-duplicate points at the same spot.
      const key = `${b.lat.toFixed(5)},${b.lng.toFixed(5)}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
}

async function getAtms({ force = false } = {}) {
  if (!force && cachedAtms && Date.now() - cachedAtms.cachedAt < ATM_CACHE_TTL_MS) {
    return cachedAtms.atms;
  }

  // Prefer the pre-scraped bank.uz ATM dataset (full coverage, offline, free).
  const staticAtms = await loadStatic("atms.json");
  if (staticAtms && staticAtms.length) {
    cachedAtms = { cachedAt: Date.now(), atms: staticAtms };
    return staticAtms;
  }

  // Overpass: ATM nodes plus banks that host an ATM, inside the Tashkent bbox.
  const query = `[out:json][timeout:25];
(
  node["amenity"="atm"](${TASHKENT_BBOX});
  node["amenity"="bank"]["atm"="yes"](${TASHKENT_BBOX});
);
out body;`;

  const data = await fetchOverpass(query);
  const atms = (data.elements ?? [])
    .filter((el) => Number.isFinite(el.lat) && Number.isFinite(el.lon))
    .map((el) => {
      const tags = el.tags ?? {};
      const bank = cleanText(tags.operator || tags.name || tags.brand || "Bankomat");
      return {
        id: el.id,
        bank,
        name: cleanText(tags.name || tags.operator || "Bankomat"),
        lat: el.lat,
        lng: el.lon
      };
    });

  cachedAtms = { cachedAt: Date.now(), atms };
  return atms;
}

async function fetchOverpass(query, attempt = 1) {
  const MAX_ATTEMPTS = 4;
  const response = await fetch(OVERPASS_URL, {
    method: "POST",
    headers: {
      "content-type": "application/x-www-form-urlencoded",
      // Overpass rejects browser user-agents with HTTP 406; use a descriptive app UA.
      "user-agent": "ValyutaStat/1.0 (currency-tracker)",
      "accept": "application/json"
    },
    body: new URLSearchParams({ data: query }).toString()
  });

  // 429 (rate limit) and 504 (gateway timeout) are transient — back off and retry.
  if ((response.status === 429 || response.status === 504) && attempt < MAX_ATTEMPTS) {
    const waitMs = 2000 * attempt;
    console.warn(`Overpass HTTP ${response.status}, retry ${attempt}/${MAX_ATTEMPTS - 1} in ${waitMs}ms`);
    await new Promise((resolve) => setTimeout(resolve, waitMs));
    return fetchOverpass(query, attempt + 1);
  }
  if (!response.ok) {
    throw new Error(`Overpass returned HTTP ${response.status}`);
  }
  return response.json();
}

/* ---------- http helpers ---------- */

async function fetchJson(url) {
  const response = await fetch(url, { headers: requestHeaders() });
  if (!response.ok) {
    throw new Error(`${url} returned HTTP ${response.status}`);
  }
  return response.json();
}

async function fetchText(url) {
  const response = await fetch(url, { headers: requestHeaders() });
  if (!response.ok) {
    throw new Error(`${url} returned HTTP ${response.status}`);
  }
  return response.text();
}

function requestHeaders() {
  // bank.uz returns HTTP 403 for non-browser user agents, so present as a real browser.
  return {
    "accept": "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
    "accept-language": "uz,ru;q=0.9,en;q=0.8",
    "user-agent":
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
  };
}

function cleanText(value) {
  return value.replace(/\s+/g, " ").trim();
}

function parseAmount(value) {
  const normalized = cleanText(value).replace(/[^\d.,]/g, "").replace(/\s/g, "");
  return normalized ? Number(normalized.replace(",", ".")) : Number.NaN;
}

function normalizeBankName(value) {
  return cleanText(value).toLowerCase().replace(/['‘’`]/g, "").replace(/\s+/g, " ");
}
