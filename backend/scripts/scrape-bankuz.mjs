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

  // bank.uz puts the branch (or the bank's call-centre) number in LocalBusiness.telephone, e.g. "1326" or "+998 71 200 00 00".
  const phone = cleanPhone(biz.telephone);

  return { bank, name, address, phone, lat, lng, isAtm, source: "bank.uz", sourceUrl: url };
}

// Several numbers come comma-separated ("1106,+998(71)210-20-02"): kept, as "1106, +998(71)210-20-02".
function cleanPhone(raw) {
  const s = String(raw ?? "").split(",").map((p) => p.replace(/\s+/g, " ").trim()).filter(Boolean).join(", ");
  return /\d{3,}/.test(s.replace(/\D/g, "")) ? s : null;
}

function dedupeExactBranches(items) {
  const seen = new Set();
  return items.filter((branch) => {
    // Remove records with identical details after case/whitespace normalization. Nearby branches remain separate.
    const key = [branch.bank, branch.name, branch.address, branch.lat, branch.lng]
      .map((value) => String(value ?? "").trim().toLocaleLowerCase())
      .join("\u0000");
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
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
      (r.isAtm ? atms : branches).push({
        bank: r.bank, name: r.name, address: r.address, ...(r.phone && !r.isAtm ? { phone: r.phone } : {}), lat: r.lat, lng: r.lng,
        source: r.source
      });
    }
    done += slice.length;
    if (done % 200 < BATCH) console.log(`  ${done}/${urls.length}  (branches ${branches.length}, atms ${atms.length})`);
  }

  await mkdir(DATA_DIR, { recursive: true });
  const uniqueBranches = dedupeExactBranches(branches);
  const uniqueAtms = dedupeExactBranches(atms);
  await writeFile(join(DATA_DIR, "branches.json"), JSON.stringify(uniqueBranches));
  await writeFile(join(DATA_DIR, "atms.json"), JSON.stringify(uniqueAtms));
  console.log(`Done. branches=${uniqueBranches.length} (${branches.length - uniqueBranches.length} exact duplicates removed), atms=${uniqueAtms.length} -> backend/data/`);
}

main().catch((e) => { console.error(e); process.exit(1); });
