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

function html({ banks, cbu, when }) {
  const half = Math.ceil(banks.length / 2);
  const cols = [banks.slice(0, half), banks.slice(half)];
  const row = (b) => b
    ? `<tr>
        <td class="bank">${b.logo ? `<img src="${b.logo}" alt="">` : `<span class="ph">${esc(b.name.slice(0, 2))}</span>`}<span>${esc(b.name)}</span></td>
        <td class="n${b.bestBuy ? " best" : ""}">${group(b.buy)}</td>
        <td class="n${b.bestSell ? " best" : ""}">${group(b.sell)}</td>
      </tr>`
    : `<tr class="empty"><td colspan="3"></td></tr>`;
  const table = (list) => `<table>
      <thead><tr><th class="bank">Banklar</th><th class="n">Sotib oladi</th><th class="n">Sotadi</th></tr></thead>
      <tbody>${Array.from({ length: half }, (_, i) => row(list[i])).join("")}</tbody>
    </table>`;
  const diff = cbu.diff || 0;
  return `<!doctype html><html><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Onest:wght@500;600;700;800&display=block" rel="stylesheet">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { width: 1000px; font-family: Onest, system-ui, sans-serif; background: #F4F5F2; color: #12171A; font-feature-settings: "tnum", "lnum"; }
  .card { margin: 0; background: #fff; }
  header { display: flex; align-items: center; justify-content: space-between; padding: 26px 30px 22px; background: linear-gradient(135deg, #2F7A68, #1B4F43); color: #fff; }
  .brand { display: flex; align-items: center; gap: 12px; }
  .brand svg { flex: none; }
  h1 { font-size: 30px; font-weight: 800; letter-spacing: -.02em; line-height: 1.1; }
  .sub { margin-top: 4px; font-size: 16px; font-weight: 500; opacity: .85; }
  .when { text-align: right; }
  .when b { display: block; font-size: 26px; font-weight: 800; letter-spacing: -.01em; }
  .when span { font-size: 15px; opacity: .85; }
  .cbu { display: flex; align-items: baseline; gap: 12px; padding: 14px 30px; background: #E1EEE8; font-size: 18px; font-weight: 600; color: #1E5A4B; }
  .cbu b { font-size: 22px; font-weight: 800; color: #12171A; }
  .cbu .d { font-weight: 700; color: ${diff < 0 ? "#B4532F" : "#2F7A68"}; }
  .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0; }
  .grid > table + table { border-left: 2px solid #E6E9E5; }
  table { width: 100%; border-collapse: collapse; }
  th { font-size: 14px; font-weight: 700; color: #5B6463; text-transform: uppercase; letter-spacing: .04em; padding: 12px 16px; background: #F0F2EF; text-align: left; }
  th.n, td.n { text-align: right; }
  td { padding: 0 16px; height: 50px; border-bottom: 1px solid #ECEEEB; font-size: 21px; font-weight: 600; }
  tr:nth-child(even) td { background: #FAFBF9; }
  td.bank { display: flex; align-items: center; gap: 12px; height: 50px; font-size: 19px; font-weight: 700; white-space: nowrap; }
  td.bank img, .ph { width: 32px; height: 32px; border-radius: 8px; object-fit: cover; flex: none; }
  .ph { display: grid; place-items: center; background: #F0F2EF; color: #5B6463; font-size: 13px; }
  td.best { background: #2F7A68 !important; color: #fff; font-weight: 800; }
  tr.empty td { border-bottom: 0; background: #fff !important; }
  footer { display: flex; justify-content: space-between; align-items: center; padding: 16px 30px; background: #12171A; color: #fff; font-size: 17px; font-weight: 600; }
  footer .legend { display: flex; align-items: center; gap: 10px; font-weight: 500; opacity: .9; }
  footer .legend i { width: 18px; height: 18px; border-radius: 5px; background: #2F7A68; display: inline-block; }
</style></head><body><div class="card">
  <header>
    <div class="brand">
      <svg viewBox="0 0 32 32" width="52" height="52"><rect width="32" height="32" rx="9" fill="#fff"/><path d="M8.5 20l5.2-5.2 3.6 3.6L23.5 12" stroke="#2F7A68" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/><circle cx="23.5" cy="12" r="2.2" fill="#2F7A68"/></svg>
      <div><h1>Banklarda dollar kursi</h1><p class="sub">1 AQSH dollari uchun, so‘mda</p></div>
    </div>
    <div class="when"><b>${when.dd}.${when.mm}.${when.year} · ${when.time}</b><span>Toshkent vaqti</span></div>
  </header>
  <div class="cbu">Markaziy bank kursi: <b>1 $ = ${money(cbu.rate)}</b><span class="d">(${diff < 0 ? "−" : "+"}${money(Math.abs(diff))})</span></div>
  <div class="grid">${table(cols[0])}${table(cols[1])}</div>
  <footer><span>${esc(SITE)}${CHANNEL ? ` · ${esc(CHANNEL)}` : ""}</span><span class="legend"><i></i>eng yaxshi kurs</span></footer>
</div></body></html>`;
}

async function render(page) {
  const browser = await chromium.launch({ channel: process.env.CHROME_CHANNEL || "chrome" });
  try {
    const tab = await browser.newPage({ viewport: { width: 1000, height: 800 }, deviceScaleFactor: 1.28 });
    await tab.setContent(page, { waitUntil: "networkidle" });
    await tab.evaluate(() => document.fonts.ready);
    return await tab.locator(".card").screenshot({ type: "png" });
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
    `💵 <b>Banklarda dollar kursi</b> — ${when.day}-${MONTHS[when.month - 1]}, ${when.time}`,
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

async function sendPhoto(png, text) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  const chat = process.env.TELEGRAM_CHAT_ID;
  if (!token || !chat) throw new Error("TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID must be set (or use --dry-run)");
  const form = new FormData();
  form.append("chat_id", chat);
  form.append("caption", text);
  form.append("parse_mode", "HTML");
  form.append("photo", new Blob([png], { type: "image/png" }), "dollar-kursi.png");
  const res = await fetch(`https://api.telegram.org/bot${token}/sendPhoto`, { method: "POST", body: form, signal: AbortSignal.timeout(60000) });
  const body = await res.json().catch(() => ({}));
  if (!res.ok || !body.ok) throw new Error(`Telegram sendPhoto failed: ${res.status} ${body.description || ""}`);
  return body.result.message_id;
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

const png = await render(html(model));
const text = caption(model);
if (outPath || dryRun) {
  const file = outPath || "dollar-kursi.png";
  await writeFile(file, png);
  console.log(`wrote ${file} (${png.length} bytes)\n\n${text}`);
}
if (!dryRun) console.log(`posted message ${await sendPhoto(png, text)}`);
