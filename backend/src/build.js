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
import { createAlerts, flushAlerts } from "./alerts.js";
import { fetchBankSites, SITE_CURRENCIES, BANK_SITE_COUNT } from "./bankSites.js";
import { getWeekendBanks } from "./weekend.js";
import { getWeekendBranches } from "./weekendBranches.js";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const SITE = process.env.SITE_DIR || join(ROOT, "worker", "public");
const STATE = join(SITE, "_state");
const mode = process.argv[2] === "full" ? "full" : "rates";
const seed = process.argv.includes("--seed");
const STATE_URL = (process.env.STATE_URL || "").replace(/\/$/, "");
const alerts = createAlerts(); // problems for the owner's Telegram (src/alerts.js)

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
    alerts.add(`step:${name}`, `${name}: ${e.message}`);
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

async function readPrevious(path) {
  try { return JSON.parse(await readFile(join(SITE, path), "utf8")); } catch { return null; }
}

const bankUzFailed = new Set(); // currencies bank.uz did not give usable rates for in this run
for (const cur of [...api.SUPPORTED, ...api.EXTRA_RATES]) {
  await step(`rates ${cur}`, async () => {
    const path = `api/rates/${cur}.json`;
    const prev = await readPrevious(path); // pulled from the live site by pullPrevious()
    const before = prev?.banks?.length || 0;
    bankUzFailed.add(cur); // until it succeeds below
    const next = await api.getRatesPayload(cur, { force: true });
    // bank.uz down, blocking us or changing its markup comes back as few or no banks. Writing that would replace the
    // last good rates with an empty list ("no bank rates" in the apps); keep the previous file instead. Its old
    // updatedAt then shows the data as stale, which is the truth.
    if (before && next.banks.length < Math.ceil(before / 2)) {
      throw new Error(`only ${next.banks.length} banks (was ${before}), keeping the previous file`);
    }
    await put(path, next);
    bankUzFailed.delete(cur);
  });
}

/* ---------- fallback: the banks' own websites (src/bankSites.js, BANK_RATE_SOURCES.md) ---------- */

// When bank.uz gives no usable dollar rates, read the 29 banks' sites instead, but only at these Tashkent hours (light
// on the banks). In between, the last fallback files stay. A late or skipped run catches up on the next one.
const SITE_HOURS = [1, 8, 13, 20];
function lastSiteSlot(nowMs = Date.now()) {
  const local = new Date(nowMs + 5 * 3600 * 1000);
  for (let daysBack = 0; daysBack < 2; daysBack++) {
    for (const h of [...SITE_HOURS].reverse()) {
      const slot = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate() - daysBack, h) - 5 * 3600 * 1000;
      if (slot <= nowMs) return slot;
    }
  }
  return 0;
}
async function cbuRates() {
  const map = await api.getCbuCurrent();
  return { map, perUnit: Object.fromEntries(Object.entries(map).map(([c, v]) => [c, v.rate])) };
}
if (bankUzFailed.has("USD")) {
  await step("rates from bank sites", async () => {
    const prevUsd = await readPrevious("api/rates/USD.json");
    if (prevUsd?.source === "bank-sites" && Date.parse(prevUsd.updatedAt) >= lastSiteSlot()) {
      console.log(`keep bank-site rates from ${prevUsd.updatedAt} (read at ${SITE_HOURS.join(":00, ")}:00 Tashkent)`);
      for (const cur of bankUzFailed) if ((await readPrevious(`api/rates/${cur}.json`))?.source === "bank-sites") alerts.remove(`step:rates ${cur}`);
      alerts.add("fallback", `bank.uz ishlamayapti: kurslar banklarning o'z saytlaridan olinmoqda (kuniga 4 marta)`);
      return;
    }
    const { map, perUnit } = await cbuRates();
    const { banks, errors } = await fetchBankSites({ cbu: perUnit });
    for (const cur of SITE_CURRENCIES) {
      if (!bankUzFailed.has(cur)) continue; // bank.uz still fine for this one
      const rows = banks.filter((b) => b.rates[cur]).map((b) => ({ bank: b.bank, buy: b.rates[cur].buy, sell: b.rates[cur].sell, sourceUrl: b.sourceUrl }));
      if (rows.length < (cur === "USD" ? 10 : 1)) continue; // too few to trust: keep the previous file
      const payload = api.buildRatesPayload(cur, map, rows, "bank websites", "bank-sites");
      await put(`api/rates/${cur}.json`, payload);
      // The day's rates go into the history too, or every bank.uz outage leaves a hole in it (23.10–06.11.2026 did:
      // bank.uz and cbu.uz were unreachable from GitHub's runners for two weeks).
      await api.recordBankHistory(cur, payload.banks, { source: "sites" }).catch((e) => console.error(`Bank history (${cur}):`, e.message));
      alerts.remove(`step:rates ${cur}`); // filled from the bank sites: the "fallback" alert below says so once
    }
    alerts.add("fallback", `bank.uz ishlamayapti: kurslar ${banks.length}/${BANK_SITE_COUNT} bank saytidan olindi (kuniga 4 marta)`
      + (errors.length ? `. O'qilmadi: ${errors.map((e) => `${e.bank} (${e.error})`).join(", ")}` : ""));
  });
}

// Once a day while bank.uz works, read the bank sites anyway and compare: a site that changed is found before the day
// it is needed. Its problems stay listed (and alerted) until the next check.
await step("bank sites check", async () => {
  const checkPath = join(STATE, "sites-check.json");
  let check = {};
  try { check = JSON.parse(await readFile(checkPath, "utf8")); } catch { /* first run */ }
  if (!bankUzFailed.has("USD") && !(Date.now() - (check.at || 0) < 24 * 3600 * 1000)) {
    const usd = await readPrevious("api/rates/USD.json");
    const ref = new Map((usd?.banks || []).map((b) => [b.bank, b]));
    const { perUnit } = await cbuRates();
    const { banks, errors } = await fetchBankSites({ cbu: perUnit });
    const problems = {};
    for (const e of errors) problems[e.bank] = `sayti o'qilmadi (${e.error})`;
    for (const b of banks) {
      const r = ref.get(b.bank), mine = b.rates.USD;
      if (!r) continue; // bank.uz has no rate for it to compare with
      const off = (x, y) => x != null && y != null && Math.abs(x - y) > 0.5;
      if (off(mine.buy, r.buy) || off(mine.sell, r.sell)) problems[b.bank] = `saytdagi USD ${mine.buy}/${mine.sell}, bank.uz'da ${r.buy}/${r.sell}`;
    }
    check = { at: Date.now(), ok: banks.length - Object.keys(problems).filter((k) => banks.some((b) => b.bank === k)).length, problems };
    await writeFile(checkPath, JSON.stringify(check));
    console.log(`bank sites check: ${banks.length}/${BANK_SITE_COUNT} read, ${Object.keys(problems).length} problem(s)`);
  }
  // Told once when it starts and once when it is fixed: a bank that blocks our servers stays blocked for a long time.
  for (const [bank, text] of Object.entries(check.problems || {})) alerts.add(`site:${bank}`, `Zaxira manba, ${bank}: ${text}`, { remind: false });
});
// Which banks stopped publishing a USD rate. A bank counts as known for 7 days after it was last seen, so one that
// stays missing keeps its alert (with a reminder) instead of looking "fixed" on the next run.
await step("bank watch", async () => {
  const usd = await readPrevious("api/rates/USD.json"); // the file this run wrote, or the kept one
  const seenPath = join(STATE, "banks-seen.json");
  let seen = {};
  try { seen = JSON.parse(await readFile(seenPath, "utf8")); } catch { /* first run */ }
  const nowMs = Date.now();
  const present = new Set((usd?.banks || []).map((b) => b.bank));
  for (const name of present) seen[name] = nowMs;
  for (const [name, at] of Object.entries(seen)) {
    if (present.has(name)) continue;
    if (nowMs - at > 7 * 24 * 3600 * 1000) { delete seen[name]; continue; }
    const last = new Date(at + 5 * 3600 * 1000).toISOString().slice(0, 16).replace("T", " ");
    alerts.add(`bank:${name}`, `${name}: USD kursi kelmayapti (oxirgi marta ${last}, Toshkent vaqti)`);
  }
  await writeFile(seenPath, JSON.stringify(seen));
  // The rates file is kept when bank.uz fails, so a growing age means bank.uz has not given usable data for hours.
  const ageH = usd?.updatedAt ? (nowMs - Date.parse(usd.updatedAt)) / 3600000 : Infinity;
  // Bank-site rates are refreshed every 5-7 hours by design, so they are "late" only after 8.
  if (ageH > (usd?.source === "bank-sites" ? 8 : 3)) alerts.add("stale:USD", `Kurslar ${Number.isFinite(ageH) ? Math.round(ageH) + " soat" : "uzoq vaqt"}dan beri yangilanmadi (bank.uz javob bermayapti yoki bloklagan bo'lishi mumkin)`);
});
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

/* ---------- weekend: the banks open this weekend (src/weekend.js), then their offices (src/weekendBranches.js) ---------- */

// Every run, not only the full ones: the Central Bank posts the list on Thursday or Friday evening, and the site should
// have it within the hour. On failure the previous file stays (it carries its own dates). The offices read last time
// for the same list are carried over until they are read again.
const BRANCH_FIELDS = ["branchesListUrl", "branchesCheckedAt", "branchLists", "weekendBranches"];
await step("weekend banks", async () => {
  const prev = await readPrevious("api/weekend.json");
  const list = await getWeekendBanks();
  const kept = prev?.branchesListUrl === list.listUrl ? Object.fromEntries(BRANCH_FIELDS.filter((k) => k in prev).map((k) => [k, prev[k]])) : {};
  await put("api/weekend.json", { updatedAt: now(), ...list, ...kept });
});
// Each bank's own list, read again every 3 hours until the weekend is over (a new list is read at once): the banks put
// their lists up from Thursday to Saturday morning and sometimes correct them.
await step("weekend branches", async () => {
  const w = await readPrevious("api/weekend.json");
  if (!w?.banks?.length || !w.days) return;
  const today = new Date(Date.now() + 5 * 3600e3).toISOString().slice(0, 10);
  const age = Date.now() - Date.parse(w.branchesCheckedAt || 0);
  if (w.days[1] >= today && !(w.branchesListUrl === w.listUrl && age < 3 * 3600e3)) {
    const branches = (await readPrevious("api/branches.json"))?.branches || [];
    if (branches.length < 500) throw new Error(`only ${branches.length} branches to match the offices with`);
    const previous = w.branchesListUrl === w.listUrl ? { listUrl: w.listUrl, lists: w.branchLists, entries: w.weekendBranches, branchesCheckedAt: w.branchesCheckedAt } : null;
    const { lists, entries } = await getWeekendBranches(w, branches, { previous });
    Object.assign(w, { branchesListUrl: w.listUrl, branchesCheckedAt: now(), branchLists: lists, weekendBranches: entries });
    await put("api/weekend.json", w);
    console.log(`weekend branches: ${entries.length} offices of ${lists.filter((l) => l.count).length} banks, ${entries.filter((e) => e.point).length} on the map`);
  }
  // A list that could not be read is told once (and once when it is fixed), for as long as it is the latest list.
  if (w.branchesListUrl === w.listUrl) {
    for (const l of w.branchLists || []) {
      if (["none", "error", "blocked"].includes(l.status)) alerts.add(`weekend:${l.name}`, `Dam olish kunlari ro'yxati o'qilmadi: ${l.name} (${l.note || l.status}) ${l.url}`, { remind: false });
    }
  }
});

const runUrl = process.env.GITHUB_RUN_ID ? `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}` : "";
await flushAlerts(alerts, join(STATE, "alerts.json"), { runUrl }); // before the manifest, so the state is carried over
await put("health.json", { ok: true, currencies: api.SUPPORTED, builtAt: now(), mode });
const files = (await listFiles(SITE)).filter((f) => f !== "_manifest.json").sort();
await put("_manifest.json", { builtAt: now(), files });

// Copies at the exact paths the apps request (lowercase, no .json), so Cloudflare serves them as static assets:
// asset hits are free and unlimited, while every Worker run counts toward the free plan's 100k requests a day.
// Not in the manifest: they are rebuilt from the .json files every time. _headers gives them the JSON type and CORS.
const aliases = [["api/atms.json", "api/atms"], ["api/cbu/all.json", "api/cbu/all"], ["api/gold.json", "api/gold"], ["api/weekend.json", "api/weekend"]];
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
