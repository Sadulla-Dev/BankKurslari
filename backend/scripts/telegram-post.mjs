// Daily Telegram channel post: a picture of every bank's dollar buy/sell rate plus a short caption.
//
//   node scripts/telegram-post.mjs --dry-run [--out post.png]   render only, no Telegram
//   TELEGRAM_BOT_TOKEN=… TELEGRAM_CHAT_ID=@channel node scripts/telegram-post.mjs
//
// Reads the public API (same data as qulaykurs.uz), renders an HTML table with the system Chrome (playwright-core,
// no browser download) and sends it with the Bot API's sendPhoto. Bank logos come from web/public/bank-logos.
import { readFile, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright-core";
import { icon } from "./render-brand.mjs";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const LOGOS = join(ROOT, "web", "public", "bank-logos");
const API = (process.env.API_URL || "https://api.qulaykurs.uz").replace(/\/$/, "");
const SITE = "qulaykurs.uz";
const CHANNEL = process.env.TELEGRAM_CHANNEL_HANDLE || ""; // e.g. @qulaykurs, shown on the picture and in the caption
const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const outPath = args.includes("--out") ? args[args.indexOf("--out") + 1] : null;

/* ---------- data ---------- */

const MONTHS = ["yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"];
const group = (n) => Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
const money = (n) => (Number.isInteger(n) ? group(n) : `${group(Math.trunc(n))},${Math.abs(n % 1).toFixed(2).slice(2)}`);
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
const key = (n) => n.toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");

/** Display names as people say them; anything not listed keeps the API name. */
const DISPLAY = [
  [/milliy/i, "Milliy Bank"], [/sanoatqurilish/i, "SQB"], [/^apex/i, "Apex Bank"], [/^kdb/i, "KDB Bank"],
  [/^infin/i, "Infin Bank"], [/^ipak/i, "Ipak Yo‘li Bank"], [/^xalq/i, "Xalq Banki"], [/^asia\s*alliance/i, "Asia Alliance"],
  [/^orient/i, "Orient Finans"], [/^universal/i, "Universal Bank"], [/^turon/i, "Turon Bank"], [/^garant/i, "Garant Bank"],
  [/^poytaxt/i, "Poytaxt Bank"], [/^ipoteka/i, "Ipoteka Bank"], [/^kapital/i, "Kapitalbank"], [/^davr/i, "Davr Bank"]
];
const displayName = (n) => (DISPLAY.find(([re]) => re.test(n)) || [null, n])[1];

/** Same mapping the website uses (web/public/app.js logoSlug). */
function logoSlug(name) {
  const k = key(name);
  const table = [
    ["anor", "anorbank"], ["hayot", "hayot-bank"], ["tenge", "tenge-bank"], ["infin", "infinbank"], ["hamkor", "hamkorbank"],
    ["agro", "agrobank"], ["trast", "trastbank"], ["trust", "trastbank"], ["universal", "universal-bank"], ["poytaxt", "poytaxt-bank"],
    ["ipoteka", "ipoteka-bank"], ["asiaalliance", "asia-alliance"], ["aloqa", "aloqabank"], ["garant", "garant-bank"],
    ["turon", "turonbank"], ["orientfinans", "orient-finans"], ["sanoatqurilish", "sqb"], ["mkbank", "mkbank"],
    ["mikrokredit", "mkbank"], ["xalq", "xalq-bank"], ["ipakyuli", "ipak-yuli"], ["asaka", "asakabank"], ["davr", "davrbank"],
    ["apex", "apexbank"], ["milliy", "nbu"], ["openbank", "openbank"], ["ziraat", "ziraat-bank"], ["octo", "octobank"],
    ["kapital", "kapitalbank"]
  ];
  if (k === "brb") return "brb";
  if (k.startsWith("kdb")) return "kdb-bank";
  return (table.find(([needle]) => k.includes(needle)) || [])[1] || null;
}
async function logoData(name) {
  const slug = logoSlug(name);
  const file = slug && join(LOGOS, `${slug}.png`);
  return file && existsSync(file) ? `data:image/png;base64,${(await readFile(file)).toString("base64")}` : null;
}

async function getJSON(path) {
  const res = await fetch(API + path, { signal: AbortSignal.timeout(30000) });
  if (!res.ok) throw new Error(`${path} HTTP ${res.status}`);
  return res.json();
}

/* ---------- picture ---------- */

function tashkentParts(iso) {
  const p = Object.fromEntries(new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Tashkent", day: "2-digit", month: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit", hour12: false
  }).formatToParts(new Date(iso)).map((x) => [x.type, x.value]));
  return { day: Number(p.day), month: Number(p.month), year: p.year, time: `${p.hour}:${p.minute}`, dd: p.day, mm: String(p.month).padStart(2, "0") };
}

/** Standard competition ranking (1, 1, 3, …): banks with the same rate share a place. */
function ranked(banks, field, better) {
  const list = [...banks].sort((a, b) => better(a[field], b[field]) || a.name.localeCompare(b.name, "uz"));
  const best = list[0][field];
  let place = 0;
  return list.map((b, i) => {
    if (i === 0 || b[field] !== list[i - 1][field]) place = i + 1;
    return { ...b, place, rate: b[field], gap: Math.abs(b[field] - best) };
  });
}

/**
 * Two leaderboards side by side, each answering one question: where do I get the most so'm for my dollars (bank
 * buy rate, highest first) and where do I pay the least for dollars (bank sell rate, lowest first).
 */
function html({ banks, cbu, when }) {
  const sellSide = ranked(banks, "buy", (a, b) => b - a);
  const buySide = ranked(banks, "sell", (a, b) => a - b);
  const medal = (p) => (p === 1 ? "m1" : p === 2 ? "m2" : p === 3 ? "m3" : "");
  const row = (sign) => (b) => `<div class="row ${medal(b.place)}">
      <span class="place">${b.place}</span>
      ${b.logo ? `<img src="${b.logo}" alt="">` : `<span class="ph">${esc(b.name.slice(0, 2))}</span>`}
      <span class="name">${esc(b.name)}</span>
      <span class="rate">${group(b.rate)}</span>
      <span class="gap">${b.gap ? `${sign}${group(b.gap)}` : "eng yaxshi"}</span>
    </div>`;
  const board = (title, hint, icon, list, tone, sign) => `<section class="board ${tone}">
      <div class="bh"><span class="ic">${icon}</span><div><h2>${title}</h2><p>${hint}</p></div></div>
      <div class="cols"><span>#</span><span></span><span>Bank</span><span>Kurs</span><span>Farq</span></div>
      ${list.map(row(sign)).join("")}
    </section>`;
  const diff = cbu.diff || 0;
  return `<!doctype html><html><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Onest:wght@500;600;700;800&display=block" rel="stylesheet">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { width: 1000px; font-family: Onest, system-ui, sans-serif; background: #F6F0E1; color: #1B1710; font-feature-settings: "tnum", "lnum"; }
  .card { padding: 0 0 0; background: #F6F0E1; }
  header { display: flex; align-items: center; justify-content: space-between; padding: 26px 30px 24px; background: linear-gradient(180deg, #FFF6DC, #F3D98E); color: #1B1710; }
  .brand { display: flex; align-items: center; gap: 14px; }
  h1 { font-size: 31px; font-weight: 800; letter-spacing: -.02em; line-height: 1.1; }
  .sub { margin-top: 5px; font-size: 16px; font-weight: 500; opacity: .72; }
  .when { text-align: right; }
  .when b { display: block; font-size: 25px; font-weight: 800; }
  .when span { font-size: 15px; opacity: .85; }
  .cbu { display: flex; align-items: baseline; gap: 12px; padding: 14px 30px; background: #1B1710; font-size: 18px; font-weight: 600; color: #D9D0BC; }
  .cbu b { font-size: 22px; font-weight: 800; color: #fff; }
  .cbu .d { font-weight: 700; color: ${diff < 0 ? "#F08B6A" : "#6CC48A"}; }
  .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; padding: 16px; }
  .board { background: #fff; border-radius: 20px; overflow: hidden; box-shadow: 0 1px 2px rgba(18,23,26,.05), 0 8px 24px -12px rgba(18,23,26,.15); }
  .bh { display: flex; align-items: center; gap: 12px; padding: 16px 18px 14px; }
  .ic { width: 44px; height: 44px; border-radius: 13px; display: grid; place-items: center; flex: none; }
  .sell .ic { background: #E3F1E4; color: #26794A; }
  .buy .ic { background: #E0EAFA; color: #2A62BE; }
  h2 { font-size: 21px; font-weight: 800; letter-spacing: -.01em; }
  .bh p { font-size: 14px; font-weight: 500; color: #6A6252; margin-top: 1px; }
  .cols, .row { display: grid; grid-template-columns: 34px 30px 1fr auto 86px; align-items: center; gap: 10px; padding: 0 16px; }
  .cols { height: 30px; background: #F7F2E6; font-size: 12px; font-weight: 700; color: #948B78; text-transform: uppercase; letter-spacing: .05em; }
  .cols span:nth-child(4), .cols span:nth-child(5) { text-align: right; }
  .row { height: 41px; border-top: 1px solid #F1EBDD; }
  .place { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; font-size: 14px; font-weight: 800; color: #6A6252; background: #F4EEDF; }
  .row img, .ph { width: 28px; height: 28px; border-radius: 7px; object-fit: cover; }
  .ph { display: grid; place-items: center; background: #F4EEDF; color: #6A6252; font-size: 11px; }
  .name { font-size: 17px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .rate { font-size: 19px; font-weight: 800; text-align: right; }
  .gap { font-size: 14px; font-weight: 600; color: #A0692B; text-align: right; }
  .row.m1 { background: #26794A; color: #fff; border-top-color: #26794A; height: 48px; }
  .row.m1 .place { background: #F2B01E; color: #4A3300; }
  .row.m1 .gap { color: #CFEADF; }
  .row.m1 img, .row.m1 .ph { background: #fff; padding: 2px; }
  .row.m1 .name { font-size: 18px; }
  .row.m1 .rate { font-size: 21px; }
  .row.m2 .place { background: #DDE2E6; color: #3E4A52; }
  .row.m3 .place { background: #EBCFB2; color: #6B4220; }
  .row.m2, .row.m3 { background: #FCFAF3; }
  footer { display: flex; justify-content: space-between; align-items: center; padding: 15px 30px; background: #1B1710; color: #fff; font-size: 17px; font-weight: 600; }
  footer span:last-child { font-weight: 500; opacity: .8; font-size: 15px; }
</style></head><body><div class="card">
  <header>
    <div class="brand">
      ${icon().replace("<svg ", '<svg width="60" height="60" style="filter:drop-shadow(0 2px 6px rgba(90,63,0,.25))" ')}
      <div><h1>Dollar kursi: banklar reytingi</h1><p class="sub">Qayerda sotish va qayerda olish foydali — 1 $ uchun, so‘mda</p></div>
    </div>
    <div class="when"><b>${when.dd}.${when.mm}.${when.year}</b></div>
  </header>
  <div class="cbu">Markaziy bank kursi: <b>1 $ = ${money(cbu.rate)}</b><span class="d">(${diff < 0 ? "−" : "+"}${money(Math.abs(diff))})</span></div>
  <div class="grid">
    ${board("Dollar sotsangiz", "Bank sizdan oladi — eng qimmati tepada", `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M6 11l6-6 6 6"/></svg>`, sellSide, "sell", "−")}
    ${board("Dollar olsangiz", "Bank sizga sotadi — eng arzoni tepada", `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M6 13l6 6 6-6"/></svg>`, buySide, "buy", "+")}
  </div>
  <footer><span>${esc(SITE)}${CHANNEL ? ` · ${esc(CHANNEL)}` : ""}</span><span>Farq — eng yaxshi kursdan necha so‘m yomon (1 $ uchun)</span></footer>
</div></body></html>`;
}

/** Renders the card once and returns it as PNG (sharpest) and JPEG (fallback if Telegram can't process the PNG). */
async function render(page) {
  const browser = await chromium.launch({ channel: process.env.CHROME_CHANNEL || "chrome" });
  try {
    const tab = await browser.newPage({ viewport: { width: 1000, height: 1600 }, deviceScaleFactor: 1.28 });
    await tab.setContent(page, { waitUntil: "networkidle" });
    await tab.evaluate(() => document.fonts.ready);
    const card = tab.locator(".card");
    return { png: await card.screenshot({ type: "png" }), jpeg: await card.screenshot({ type: "jpeg", quality: 92 }) };
  } finally {
    await browser.close();
  }
}

/* ---------- caption ---------- */

function caption({ banks, cbu, when }) {
  const maxBuy = Math.max(...banks.map((b) => b.buy));
  const minSell = Math.min(...banks.map((b) => b.sell));
  const names = (list) => list.map((b) => b.name).join(", ");
  const diff = cbu.diff || 0;
  return [
    `💵 <b>Banklarda dollar kursi</b> — ${when.day}-${MONTHS[when.month - 1]}`,
    "",
    `🏦 Markaziy bank: <b>${money(cbu.rate)}</b> so‘m (${diff < 0 ? "−" : "+"}${money(Math.abs(diff))})`,
    `📈 Dollar sotsangiz, eng yaxshisi: <b>${group(maxBuy)}</b> — ${esc(names(banks.filter((b) => b.buy === maxBuy)))}`,
    `📉 Dollar olsangiz, eng arzoni: <b>${group(minSell)}</b> — ${esc(names(banks.filter((b) => b.sell === minSell)))}`,
    "",
    `🧮 Summangizni hisoblang, yaqin filialni xaritada toping: <a href="https://${SITE}">${SITE}</a>`,
    "",
    `#dollar_kursi${CHANNEL ? `  ${esc(CHANNEL)}` : ""}`
  ].join("\n");
}

/* ---------- send ---------- */

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * Telegram now and then rejects a perfectly good picture with "400 IMAGE_PROCESS_FAILED" (its own image pipeline),
 * or answers 429/5xx. Those are retried with backoff, and after an image error the JPEG copy is sent instead of the
 * PNG. Errors that a retry can't fix (bad token, chat not found, caption markup) fail at once.
 * A request that timed out is NOT retried: Telegram may have posted it already, and a double post is worse.
 */
async function sendPhoto(images, text) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  const chat = process.env.TELEGRAM_CHAT_ID;
  if (!token || !chat) throw new Error("TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID must be set (or use --dry-run)");
  // Seconds to wait before each retry: quick ones for a blip, then 5 and 20 minutes for a longer Telegram outage.
  const backoff = [5, 15, 30, 60, 90, 300, 1200];
  let useJpeg = false;
  let last = "";
  for (let attempt = 0; attempt <= backoff.length; attempt++) {
    if (attempt) {
      console.log(`retry ${attempt}/${backoff.length} as ${useJpeg ? "JPEG" : "PNG"} after: ${last}`);
    }
    const form = new FormData();
    form.append("chat_id", chat);
    form.append("caption", text);
    form.append("parse_mode", "HTML");
    form.append("photo", useJpeg
      ? new Blob([images.jpeg], { type: "image/jpeg" })
      : new Blob([images.png], { type: "image/png" }), useJpeg ? "dollar-kursi.jpg" : "dollar-kursi.png");
    let res, body;
    try {
      res = await fetch(`https://api.telegram.org/bot${token}/sendPhoto`, { method: "POST", body: form, signal: AbortSignal.timeout(90000) });
      body = await res.json().catch(() => ({}));
    } catch (e) {
      if (e.name === "TimeoutError" || e.name === "AbortError") {
        throw new Error("Telegram sendPhoto timed out — not retrying, check the channel: it may have been posted");
      }
      last = `network: ${e.cause?.code || e.message}`;
      if (attempt < backoff.length) await sleep(backoff[attempt] * 1000);
      continue;
    }
    if (res.ok && body.ok) return body.result.message_id;
    const desc = body.description || "";
    last = `${res.status} ${desc}`;
    const imageError = res.status === 400 && /IMAGE_PROCESS_FAILED|PHOTO_|wrong file|failed to get http url content/i.test(desc);
    const transient = res.status === 429 || res.status >= 500 || imageError;
    if (!transient) throw new Error(`Telegram sendPhoto failed: ${last}`);
    if (imageError) useJpeg = !useJpeg; // PNG failed → try JPEG, then alternate
    const wait = res.status === 429 && body.parameters?.retry_after ? Math.min(body.parameters.retry_after, 120) : backoff[attempt];
    if (attempt < backoff.length) await sleep(wait * 1000);
  }
  throw new Error(`Telegram sendPhoto failed after ${backoff.length + 1} attempts: ${last}`);
}

/* ---------- main ---------- */

const data = await getJSON("/api/rates/usd");
const raw = (data.banks || []).filter((b) => b.buy > 0 && b.sell > 0);
if (raw.length < 10) throw new Error(`only ${raw.length} banks with both rates — refusing to post a thin table`);
const maxBuy = Math.max(...raw.map((b) => b.buy));
const minSell = Math.min(...raw.map((b) => b.sell));
const banks = await Promise.all(raw.map(async (b) => ({
  name: displayName(b.bank), buy: b.buy, sell: b.sell, logo: await logoData(b.bank),
  bestBuy: b.buy === maxBuy, bestSell: b.sell === minSell
})));
banks.sort((a, b) => a.name.localeCompare(b.name, "uz"));
const when = tashkentParts(data.updatedAt || new Date().toISOString());
const model = { banks, cbu: data.cbu, when };

const images = await render(html(model));
const text = caption(model);
if (outPath || dryRun) {
  const file = outPath || "dollar-kursi.png";
  await writeFile(file, images.png);
  console.log(`wrote ${file} (${images.png.length} bytes; JPEG fallback ${images.jpeg.length} bytes)\n\n${text}`);
}
if (!dryRun) console.log(`posted message ${await sendPhoto(images, text)}`);
