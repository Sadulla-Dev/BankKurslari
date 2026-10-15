// One-time scraper: pulls every bank branch + ATM (with coordinates) from bank.uz
// and writes static JSON into backend/data/. Run with:  node scripts/scrape-bankuz.mjs
import { writeFile, mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
const SITEMAPS = [
  "https://bank.uz/sitemap_branches_atms-1.xml",
  "https://bank.uz/sitemap_branches_atms-2.xml"
];
const BATCH = 10;
const DATA_DIR = join(dirname(fileURLToPath(import.meta.url)), "..", "data");

async function getText(url, attempt = 1) {
  try {
    const res = await fetch(url, { headers: { "user-agent": UA, accept: "text/html,application/xhtml+xml" } });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.text();
  } catch (error) {
    if (attempt < 3) {
      await sleep(1000 * attempt);
      return getText(url, attempt + 1);
    }
    throw error;
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function jsonLdBlocks(html) {
  const out = [];
  for (const m of html.matchAll(/<script[^>]*application\/ld\+json[^>]*>([\s\S]*?)<\/script>/g)) {
    try { out.push(JSON.parse(m[1].trim())); } catch { /* ignore malformed */ }
  }
  return out;
}

function uzPart(text) {
  if (!text) return null;
  const s = String(text);
  const i = s.indexOf(":uz:");
  return (i >= 0 ? s.slice(i + 4) : s).trim() || null;
}

function parsePage(url, html) {
  const blocks = jsonLdBlocks(html);
  const biz = blocks.find((b) => b["@type"] === "LocalBusiness");
  const crumbs = blocks.find((b) => b["@type"] === "BreadcrumbList");
  if (!biz?.geo) return null;

  const lat = parseFloat(biz.geo.latitude);
  const lng = parseFloat(biz.geo.longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;

  const items = crumbs?.itemListElement ?? [];
  const bank = uzPart(items.find((i) => i.position === 2)?.name) || "Bank";
  const name = uzPart(items.find((i) => i.position === 3)?.name) || bank;
  const address = uzPart(biz.address?.addressLocality);

  const slug = url.split("/").pop();
  const isAtm = /_atm\d+|bankomat|kartomat/i.test(slug) || /bankomat|bankomatlar/i.test(name);

  return { bank, name, address, lat, lng, isAtm };
}

async function collectUrls() {
  const set = new Set();
  for (const sm of SITEMAPS) {
    const xml = await getText(sm);
    for (const m of xml.matchAll(/https:\/\/bank\.uz\/uz\/branches_atms\/([a-z0-9_-]+)/g)) {
      const slug = m[1];
      // Keep only individual pages (they carry a numeric id); skip category listing slugs.
      if (/\d/.test(slug)) set.add(`https://bank.uz/uz/branches_atms/${slug}`);
    }
  }
  return [...set];
}

async function main() {
  const limit = Number(process.argv[2] || 0); // optional: scrape only first N (for testing)
  let urls = await collectUrls();
  if (limit > 0) urls = urls.slice(0, limit);
  console.log(`Collected ${urls.length} individual pages. Scraping…`);

  const branches = [];
  const atms = [];
  let done = 0;
  for (let i = 0; i < urls.length; i += BATCH) {
    const slice = urls.slice(i, i + BATCH);
    const results = await Promise.all(
      slice.map((u) => getText(u).then((h) => parsePage(u, h)).catch(() => null))
    );
    for (const r of results) {
      if (!r) continue;
      (r.isAtm ? atms : branches).push({ bank: r.bank, name: r.name, address: r.address, lat: r.lat, lng: r.lng });
    }
    done += slice.length;
    if (done % 200 < BATCH) console.log(`  ${done}/${urls.length}  (branches ${branches.length}, atms ${atms.length})`);
  }

  await mkdir(DATA_DIR, { recursive: true });
  await writeFile(join(DATA_DIR, "branches.json"), JSON.stringify(branches));
  await writeFile(join(DATA_DIR, "atms.json"), JSON.stringify(atms));
  console.log(`Done. branches=${branches.length} atms=${atms.length} -> backend/data/`);
}

main().catch((e) => { console.error(e); process.exit(1); });
