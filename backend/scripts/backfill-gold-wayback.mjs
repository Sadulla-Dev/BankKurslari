// One-off backfill of CBU gold bar prices from Internet Archive snapshots of the CBU price page.
// The CBU has no archive for bar prices, so before our own daily collection (data/gold-history.json, from Oct 2026)
// the only record of real CBU prices is the Wayback Machine. Every snapshot of the page in any language is parsed
// and written to data/gold-history-archive.json as { "yyyy-mm-dd": { "5": { sell, buyback }, ... } }.
//
//   node scripts/backfill-gold-wayback.mjs
import { readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import * as cheerio from "cheerio";

const OUT = join(dirname(fileURLToPath(import.meta.url)), "..", "data", "gold-history-archive.json");
const LANGS = ["uz", "oz", "ru", "en"];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const somNumber = (text) => Number(String(text).replace(/[^0-9]/g, "")) || null;

async function get(url, tries = 4) {
  for (let i = 0; i < tries; i++) {
    try {
      const res = await fetch(url, { headers: { "user-agent": "qulaykurs-backfill/1.0" }, signal: AbortSignal.timeout(45000) });
      if (res.ok) return await res.text();
      if (res.status === 404) return null;
    } catch { /* retry */ }
    await sleep(3000 * (i + 1));
  }
  return null;
}

/** grams | sale [| buy-back [| buy-back (damaged)]]: before the buy-back program (2023) the table had only two columns. */
function parse(html, snapDay) {
  const $ = cheerio.load(html);
  const bars = {};
  $("table tr").each((_, tr) => {
    const cells = $(tr).find("td").map((__, td) => $(td).text().trim()).get();
    if (cells.length < 2) return;
    const grams = Number((cells[0].match(/[\d.]+/) || [])[0]);
    const sell = somNumber(cells[1]);
    const buyback = cells.length > 2 ? somNumber(cells[2]) : null;
    if (![5, 10, 20, 50, 100].includes(grams) || !sell) return;
    // Sanity: a gram of gold has cost between 100 000 and 10 000 000 so'm over the archive's life.
    if (sell / grams < 1e5 || sell / grams > 1e7) return;
    bars[String(grams)] = { sell, buyback: buyback && buyback < sell ? buyback : null };
  });
  if (!bars["10"]) return null;
  // Prefer the price date printed on the page; fall back to the snapshot day when it is missing or implausible.
  const text = $("main, .content, body").first().text().replace(/\s+/g, " ");
  let day = snapDay;
  for (const m of text.matchAll(/(\d{2})\.(\d{2})\.(20\d{2})/g)) {
    const d = `${m[3]}-${m[2]}-${m[1]}`;
    const lag = (Date.parse(snapDay) - Date.parse(d)) / 864e5;
    if (lag >= 0 && lag <= 45) { day = d; break; }
  }
  return { day, bars };
}

let out = {};
try { out = JSON.parse(await readFile(OUT, "utf8")); } catch { /* new file */ }
const save = () => writeFile(OUT, JSON.stringify(Object.fromEntries(Object.entries(out).sort(([a], [b]) => (a < b ? -1 : 1)))) + "\n");

// Every snapshot day across all language versions of the page; one fetch per distinct day.
const jobs = new Map();
for (const lang of LANGS) {
  const page = `cbu.uz/${lang}/banknotes-coins/gold-bars/prices/`;
  const cdx = await get(`http://web.archive.org/cdx/search/cdx?url=${page}&output=json&fl=timestamp&filter=statuscode:200&collapse=timestamp:8`);
  const stamps = cdx ? JSON.parse(cdx).slice(1).map((r) => r[0]) : [];
  console.log(`${lang}: ${stamps.length} snapshots`);
  for (const ts of stamps) {
    const snapDay = `${ts.slice(0, 4)}-${ts.slice(4, 6)}-${ts.slice(6, 8)}`;
    if (!jobs.has(snapDay)) jobs.set(snapDay, { ts, page, snapDay });
  }
}
const queue = [...jobs.values()];
console.log(`${queue.length} distinct days to fetch`);
let done = 0;
// A few requests at a time: the archive answers slowly (~10 s a page) but tolerates light parallelism.
async function worker() {
  while (queue.length) {
    const { ts, page, snapDay } = queue.shift();
    const html = await get(`http://web.archive.org/web/${ts}id_/https://${page}`);
    const row = html && parse(html, snapDay);
    done++;
    if (!row) { console.log(`[${done}/${jobs.size}] ${ts} -> no table`); continue; }
    out[row.day] = row.bars;
    await save();
    console.log(`[${done}/${jobs.size}] ${ts} -> ${row.day} 10g ${row.bars["10"].sell}`);
  }
}
await Promise.all([worker(), worker(), worker()]);
await save();
console.log(`wrote ${Object.keys(out).length} days to ${OUT}`);
