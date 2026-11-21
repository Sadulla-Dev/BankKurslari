// Builds the public API as static JSON files for the Cloudflare Worker (worker/public), then wrangler deploys them.
//
//   node src/build.js rates   hourly: bank rates, CBU list, gold
//   node src/build.js full    twice a day: everything (adds branches, ATMs, CBU history, analytics)
//
// State that cannot be re-downloaded (collected bank/gold history, CBU history cache, official branch cache) lives in
// public/_state and is pulled back from the live site (STATE_URL) before each build. If the live site cannot be read
// the build stops, so a failed download never overwrites collected history with an empty file.
// First deploy: add --seed to start from backend/data instead.
import { mkdir, readFile, rm, writeFile, copyFile, readdir } from "node:fs/promises";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";
import { execFile } from "node:child_process";
import { promisify } from "node:util";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const SITE = process.env.SITE_DIR || join(ROOT, "worker", "public");
const STATE = join(SITE, "_state");
const mode = process.argv[2] === "full" ? "full" : "rates";
const seed = process.argv.includes("--seed");
const STATE_URL = (process.env.STATE_URL || "").replace(/\/$/, "");
const execFileAsync = promisify(execFile);

async function put(path, data) {
  const file = join(SITE, path);
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, typeof data === "string" ? data : JSON.stringify(data));
}

async function pullPrevious() {
  if (seed) {
    await mkdir(STATE, { recursive: true });
    for (const f of await readdir(join(ROOT, "data"))) {
      if (/^(history-|bank-history-|gold-history|official-branches|yandex-branches)/.test(f)) await copyFile(join(ROOT, "data", f), join(STATE, f));
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

// Expired Yandex responses must leave both the public dataset and persisted state.
try {
  const yandexStateFile = join(STATE, "yandex-branches.json");
  const yandexState = JSON.parse(await readFile(yandexStateFile, "utf8"));
  if (!yandexState.expiresAt || Date.parse(yandexState.expiresAt) <= Date.now()) {
    await rm(yandexStateFile, { force: true });
  }
} catch {
  // No Yandex snapshot has been imported yet.
}

// CHF, JPY, CNY come from ~30 separate bank pages (see PER_BANK_RATES in server.js): read them only at these Tashkent
// hours, not every hour, to stay light on bank.uz. A late or skipped scheduled run catches up on the next one.
const PER_BANK_HOURS = [1, 8, 13, 20];
const TASHKENT_OFFSET_MS = 5 * 60 * 60 * 1000;
function lastPerBankSlot(now = Date.now()) {
  const local = new Date(now + TASHKENT_OFFSET_MS);
  for (let daysBack = 0; daysBack < 2; daysBack++) {
    for (const h of [...PER_BANK_HOURS].reverse()) {
      const slot = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate() - daysBack, h) - TASHKENT_OFFSET_MS;
      if (slot <= now) return slot;
    }
  }
  return 0;
}
async function readPrevious(path) {
  try { return JSON.parse(await readFile(join(SITE, path), "utf8")); } catch { return null; }
}

for (const cur of [...api.SUPPORTED, ...api.EXTRA_RATES]) {
  await step(`rates ${cur}`, async () => {
    const path = `api/rates/${cur}.json`;
    const prev = await readPrevious(path); // pulled from the live site by pullPrevious()
    const before = prev?.banks?.length || 0;
    if (api.PER_BANK_RATES.includes(cur) && before && Date.parse(prev.updatedAt) >= lastPerBankSlot()) {
      console.log(`keep ${cur}: bank pages are read at ${PER_BANK_HOURS.join(":00, ")}:00 Tashkent`);
      return;
    }
    const next = await api.getRatesPayload(cur, { force: true });
    // bank.uz down, blocking us or changing its markup comes back as few or no banks. Writing that would replace the
    // last good rates with an empty list ("no bank rates" in the apps); keep the previous file instead. Its old
    // updatedAt then shows the data as stale, which is the truth.
    if (before && next.banks.length < Math.ceil(before / 2)) {
      throw new Error(`only ${next.banks.length} banks (was ${before}), keeping the previous file`);
    }
    await put(path, next);
  });
}
await step("cbu all", async () => put("api/cbu/all.json", await api.getCbuFull()));
await step("gold", async () => put("api/gold.json", await api.getGold({ force: true })));

if (mode === "full") {
  if (process.env.YANDEX_PLACES_KEY) {
    await step("refresh Yandex branch cache", async () => {
      const script = join(ROOT, "scripts", "import-yandex-branches.mjs");
      const { stdout, stderr } = await execFileAsync(process.execPath, [script], {
        env: { ...process.env, DATA_DIR: STATE },
        maxBuffer: 4 * 1024 * 1024
      });
      if (stdout) process.stdout.write(stdout);
      if (stderr) process.stderr.write(stderr);
    });
  } else {
    console.log("skip refresh Yandex branch cache (YANDEX_PLACES_KEY is not set)");
  }
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
  await step("Yandex branches", async () => {
    try {
      await put("api/branches/yandex.json", await api.getYandexBranchDataset());
    } catch (error) {
      await rm(join(STATE, "yandex-branches.json"), { force: true });
      throw error;
    }
  });
  await step("atms", async () => {
    const atms = await api.getAtms({ force: true });
    await put("api/atms.json", { updatedAt: now(), count: atms.length, atms });
  });
}

await put("health.json", { ok: true, currencies: api.SUPPORTED, builtAt: now(), mode });
const files = (await listFiles(SITE)).filter((f) => f !== "_manifest.json").sort();
await put("_manifest.json", { builtAt: now(), files });

// Copies at the exact paths the apps request (lowercase, no .json), so Cloudflare serves them as static assets:
// asset hits are free and unlimited, while every Worker run counts toward the free plan's 100k requests a day.
// Not in the manifest: they are rebuilt from the .json files every time. _headers gives them the JSON type and CORS.
const aliases = [["api/atms.json", "api/atms"], ["api/cbu/all.json", "api/cbu/all"], ["api/gold.json", "api/gold"]];
for (const cur of api.SUPPORTED) {
  for (const kind of ["rates", "history", "analytics"]) aliases.push([`api/${kind}/${cur}.json`, `api/${kind}/${cur.toLowerCase()}`]);
}
for (const cur of api.EXTRA_RATES) aliases.push([`api/rates/${cur}.json`, `api/rates/${cur.toLowerCase()}`]);
for (const [from, to] of aliases) {
  if (files.includes(from)) await copyFile(join(SITE, from), join(SITE, to));
}
await put(
  "_headers",
  ["/api/*", "  Content-Type: application/json; charset=utf-8", "  Access-Control-Allow-Origin: *", "  Cache-Control: public, max-age=60", ""].join("\n")
);
console.log(`Built ${files.length} files (${mode})`);
// server.js keeps no timers when imported, but undici keep-alive sockets can hold the loop open.
process.exit(0);
