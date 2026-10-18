// Builds the public API as static JSON files for the Cloudflare Worker (worker/public), then wrangler deploys them.
//
//   node src/build.js rates   every ~20 min: bank rates, CBU list, gold
//   node src/build.js full    twice a day: everything (adds branches, ATMs, CBU history, analytics)
//
// State that cannot be re-downloaded (collected bank/gold history, CBU history cache, official branch cache) lives in
// public/_state and is pulled back from the live site (STATE_URL) before each build. If the live site cannot be read
// the build stops, so a failed download never overwrites collected history with an empty file.
// First deploy: add --seed to start from backend/data instead.
import { mkdir, readFile, rm, writeFile, copyFile, readdir } from "node:fs/promises";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const SITE = process.env.SITE_DIR || join(ROOT, "worker", "public");
const STATE = join(SITE, "_state");
const mode = process.argv[2] === "full" ? "full" : "rates";
const seed = process.argv.includes("--seed");
const STATE_URL = (process.env.STATE_URL || "").replace(/\/$/, "");

async function put(path, data) {
  const file = join(SITE, path);
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, typeof data === "string" ? data : JSON.stringify(data));
}

async function pullPrevious() {
  if (seed) {
    await mkdir(STATE, { recursive: true });
    for (const f of await readdir(join(ROOT, "data"))) {
      if (/^(history-|bank-history-|gold-history|official-branches)/.test(f)) await copyFile(join(ROOT, "data", f), join(STATE, f));
    }
    console.log("Seeded state from backend/data");
    return;
  }
  if (!STATE_URL) throw new Error("STATE_URL is not set (use --seed for the first deploy)");
  const res = await fetch(`${STATE_URL}/_manifest.json`, { cache: "no-store" });
  if (!res.ok) throw new Error(`manifest HTTP ${res.status}`);
  const { files } = await res.json();
  for (const f of files) {
    const r = await fetch(`${STATE_URL}/${f}`, { cache: "no-store" });
    if (!r.ok) throw new Error(`${f} HTTP ${r.status}`);
    await put(f, await r.text());
  }
  console.log(`Pulled ${files.length} files from ${STATE_URL}`);
}

// One failing source keeps its previous file instead of failing the whole build.
async function step(name, fn) {
  try {
    await fn();
    console.log(`ok   ${name}`);
  } catch (e) {
    console.error(`FAIL ${name}: ${e.message}`);
  }
}

async function listFiles(dir) {
  const out = [];
  for (const e of await readdir(dir, { withFileTypes: true })) {
    const p = join(dir, e.name);
    if (e.isDirectory()) out.push(...(await listFiles(p)));
    else out.push(relative(SITE, p).split("\\").join("/"));
  }
  return out;
}

await rm(SITE, { recursive: true, force: true });
await mkdir(STATE, { recursive: true });
await pullPrevious();

process.env.DATA_DIR = STATE;
const api = await import("./server.js");
const now = () => new Date().toISOString();

for (const cur of api.SUPPORTED) {
  await step(`rates ${cur}`, async () => put(`api/rates/${cur}.json`, await api.getRatesPayload(cur, { force: true })));
}
await step("cbu all", async () => put("api/cbu/all.json", await api.getCbuFull()));
await step("gold", async () => put("api/gold.json", await api.getGold({ force: true })));

if (mode === "full") {
  for (const cur of api.SUPPORTED) {
    await step(`history ${cur}`, async () => {
      const points = await api.getHistory(cur, { force: true });
      if (!points.length) throw new Error("no points");
      await put(`api/history/${cur}.json`, { currency: cur, updatedAt: now(), points });
      await put(`api/analytics/${cur}.json`, api.computeAnalytics(cur, points));
    });
  }
  await step("branches", async () => {
    const branches = await api.getBranches({ force: true });
    await put("api/branches.json", { updatedAt: now(), count: branches.length, branches });
  });
  await step("atms", async () => {
    const atms = await api.getAtms({ force: true });
    await put("api/atms.json", { updatedAt: now(), count: atms.length, atms });
  });
}

await put("health.json", { ok: true, currencies: api.SUPPORTED, builtAt: now(), mode });
const files = (await listFiles(SITE)).filter((f) => f !== "_manifest.json").sort();
await put("_manifest.json", { builtAt: now(), files });
console.log(`Built ${files.length} files (${mode})`);
// server.js keeps no timers when imported, but undici keep-alive sockets can hold the loop open.
process.exit(0);
