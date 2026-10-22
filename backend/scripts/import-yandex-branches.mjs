// Refreshes the separate Yandex Places cache. Requires the Yandex license/API key.
// Run: YANDEX_PLACES_KEY=... npm run import:yandex-branches [-- --force]
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const DATA_DIR = process.env.DATA_DIR || join(dirname(fileURLToPath(import.meta.url)), "..", "data");
const FILE = join(DATA_DIR, "yandex-branches.json");
const KEY = process.env.YANDEX_PLACES_KEY;
const URL = "https://search-maps.yandex.ru/v1/";
const CACHE_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_RESULTS = 50;
const MAX_DEPTH = 8;
const MAX_PAGES = 21; // skip = 0..1000, in 50-result increments
const QUERIES = (process.env.YANDEX_BANK_QUERIES || "bank,банк").split(",").map((x) => x.trim()).filter(Boolean);
// Uzbekistan search envelope (west,south,east,north); country_code filtering removes neighboring areas.
const COUNTRY_BBOX = { west: 55.9, south: 37.1, east: 73.2, north: 45.6 };

if (!KEY) throw new Error("YANDEX_PLACES_KEY is required");

let previous = null;
try { previous = JSON.parse(await readFile(FILE, "utf8")); } catch { /* first import */ }
const force = process.argv.includes("--force");
const age = previous?.fetchedAt ? Date.now() - Date.parse(previous.fetchedAt) : Infinity;
if (!force && previous?.coverage === "uzbekistan" && age >= 0 && age < CACHE_MS) {
  console.log(`Yandex dataset is fresh (${previous.count} branches, expires ${previous.expiresAt}); no refresh needed.`);
  process.exit(0);
}

const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const all = new Map();
let requests = 0;
let incompleteTiles = 0;

function isBank(feature) {
  const meta = feature.properties?.CompanyMetaData || {};
  const categoryText = (meta.Categories || []).map((category) => `${category.class || ""} ${category.name || ""}`).join(" ");
  return /банк|bank|financial|finance/i.test(`${meta.name || ""} ${categoryText}`) && !/банкомат|atm/i.test(`${meta.name || ""}`);
}

function toBranch(feature) {
  const meta = feature.properties?.CompanyMetaData || {};
  const [lng, lat] = feature.geometry?.coordinates || [];
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  if (meta.Address?.country_code && meta.Address.country_code !== "UZ") return null;
  if (!meta.Address?.country_code && (lat < 37.1 || lat > 45.6 || lng < 55.9 || lng > 73.2)) return null;
  if (!isBank(feature)) return null;
  const id = String(meta.id || feature.properties?.uri || `${lat.toFixed(6)},${lng.toFixed(6)}`);
  return {
    id,
    bank: meta.name || "Bank",
    name: meta.name || "Bank",
    address: meta.Address?.formatted || meta.address || null,
    hours: meta.Hours?.text || null,
    lat,
    lng,
    source: "Yandex Places API"
  };
}

async function search(text, bbox, skip = 0) {
  const params = new URLSearchParams({
    apikey: KEY, text, lang: "ru_RU", type: "biz", rspn: "1", results: String(MAX_RESULTS), skip: String(skip),
    bbox: `${bbox.west},${bbox.south}~${bbox.east},${bbox.north}`
  });
  const response = await fetch(`${URL}?${params}`);
  requests++;
  if (!response.ok) throw new Error(`Yandex Places HTTP ${response.status}`);
  const data = await response.json();
  const meta = data.properties?.ResponseMetaData?.SearchResponse || {};
  return { features: data.features || [], found: Number(meta.found) || 0 };
}

async function collect(text, bbox, depth = 0) {
  await pause(120); // stay well below Yandex's documented per-second request limit
  const first = await search(text, bbox, 0);
  const local = first.features.map(toBranch).filter(Boolean);
  for (const branch of local) all.set(branch.id, branch);

  // Dense tiles are split so a relevance-ranked 50-item response does not silently hide locations.
  if (depth < MAX_DEPTH && (first.found > first.features.length || first.features.length === MAX_RESULTS)) {
    const midLng = (bbox.west + bbox.east) / 2;
    const midLat = (bbox.south + bbox.north) / 2;
    for (const tile of [
      { west: bbox.west, south: bbox.south, east: midLng, north: midLat },
      { west: midLng, south: bbox.south, east: bbox.east, north: midLat },
      { west: bbox.west, south: midLat, east: midLng, north: bbox.north },
      { west: midLng, south: midLat, east: bbox.east, north: bbox.north }
    ]) await collect(text, tile, depth + 1);
    return;
  }

  // At the smallest tile, page the documented 50-result window (skip is capped at 1000).
  const pageCount = Math.min(MAX_PAGES, Math.ceil(first.found / MAX_RESULTS));
  if (first.found > MAX_PAGES * MAX_RESULTS) incompleteTiles++;
  for (let page = 1; page < pageCount; page++) {
    await pause(120);
    const next = await search(text, bbox, page * MAX_RESULTS);
    for (const feature of next.features) {
      const branch = toBranch(feature);
      if (branch) all.set(branch.id, branch);
    }
    if (next.features.length < MAX_RESULTS) break;
  }
}

for (const query of QUERIES) {
  console.log(`Searching Uzbekistan for: ${query}`);
  await collect(query, COUNTRY_BBOX);
}

const fetchedAt = new Date();
const branches = [...all.values()].sort((a, b) => a.bank.localeCompare(b.bank) || a.name.localeCompare(b.name));
const dataset = {
  provider: "yandex-maps",
  coverage: "uzbekistan",
  fetchedAt: fetchedAt.toISOString(),
  expiresAt: new Date(fetchedAt.getTime() + CACHE_MS).toISOString(),
  queryCount: requests,
  incompleteTiles,
  completeness: incompleteTiles ? "partial (API paging limit reached in dense tiles)" : "best-effort (Yandex search ranking is not a guaranteed directory)",
  count: branches.length,
  branches
};
await mkdir(DATA_DIR, { recursive: true });
await writeFile(FILE, JSON.stringify(dataset, null, 2) + "\n");
console.log(`Saved ${branches.length} Yandex bank locations (${requests} requests) to ${FILE}`);
