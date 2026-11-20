// Draws the Qulay Kurs mark (a euro note and a dollar note on warm gold) and renders every brand asset from it.
//   node scripts/render-brand.mjs
// Needs `rsvg-convert` (librsvg) and `cwebp` on PATH, and Chrome for the link-preview pictures. Writes brand/*, web/public icons, the iOS app icon,
// the Play Store icon and the Android launcher bitmaps. The geometry lives only in this file.
import { mkdirSync, writeFileSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");

// Brand colours (kept in step with web/public/style.css and ui/theme/Color.kt).
export const GOLD_TOP = "#FFF6DC", GOLD_BOTTOM = "#F3D98E";
const W = 42, H = 25, R = 4.6; // one banknote, in 64-unit icon space

const DEFS = `
<filter id="sh" x="-30%" y="-40%" width="160%" height="200%"><feGaussianBlur stdDeviation="1.7"/></filter>
<filter id="sh2" x="-30%" y="-40%" width="160%" height="200%"><feGaussianBlur stdDeviation=".5"/></filter>
<linearGradient id="hl" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".75"/><stop offset=".45" stop-color="#fff" stop-opacity="0"/></linearGradient>
<linearGradient id="gd" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#6CC48A"/><stop offset="1" stop-color="#2E8552"/></linearGradient>
<linearGradient id="ge" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#5E9CF0"/><stop offset="1" stop-color="#2A62BE"/></linearGradient>
<radialGradient id="md" cx=".4" cy=".3" r=".8"><stop offset="0" stop-color="#FAFFF8"/><stop offset="1" stop-color="#CFE6CF"/></radialGradient>
<radialGradient id="me" cx=".4" cy=".3" r=".8"><stop offset="0" stop-color="#FAFCFF"/><stop offset="1" stop-color="#CBDCF5"/></radialGradient>
<radialGradient id="gold" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#FFE89A"/><stop offset="1" stop-color="#F2B01E"/></radialGradient>
<radialGradient id="sheen" cx=".2" cy="0" r=".9"><stop offset="0" stop-color="#fff" stop-opacity=".32"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
<linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${GOLD_TOP}"/><stop offset="1" stop-color="${GOLD_BOTTOM}"/></linearGradient>
<clipPath id="cn"><rect width="${W}" height="${H}" rx="${R}"/></clipPath>`;

const DOLLAR = `<path stroke-width="2.1" d="M3.2 -2.6C3.2 -4.4 1.8 -5.2 0 -5.2S-3.2 -4.3 -3.2 -2.7C-3.2 .6 3.2 -.6 3.2 2.7C3.2 4.3 1.8 5.2 0 5.2S-3.2 4.4 -3.2 2.6"/><path stroke-width="1.5" d="M0 -7V7"/>`;
const EURO = `<path stroke-width="2.2" d="M3 -3.9A5 5 0 1 0 3 3.9"/><path stroke-width="1.6" d="M-6.200 -1.4H1.2M-6.200 1.4H.4"/>`;

// A sign pressed into the medallion: a light edge half a unit below the ink.
function glyph(x, y, scale, ink, paths, emboss = true) {
  const g = (dy, colour, opacity) => `<g transform="translate(${x} ${y + dy}) scale(${scale})" fill="none" stroke="${colour}" stroke-linecap="round" opacity="${opacity}">${paths}</g>`;
  return (emboss ? g(0.55, "#fff", 0.9) : "") + g(0, ink, 1);
}

// The note body. `detail` adds the blurred shadow, sheen, rim light and inner frame; small sizes drop them.
function note(gradient, pale, detail) {
  const shadow = detail
    ? `<rect x=".4" y="2.6" width="${W}" height="${H}" rx="${R}" fill="#3A2A00" opacity=".36" filter="url(#sh)"/><rect y=".7" width="${W}" height="${H}" rx="${R}" fill="#3A2A00" opacity=".32" filter="url(#sh2)"/>`
    : `<rect x=".5" y="1.8" width="${W}" height="${H}" rx="${R}" fill="#3A2A00" opacity=".22"/>`;
  const finish = detail
    ? `<rect width="${W}" height="${H}" fill="url(#sheen)" clip-path="url(#cn)"/><rect x=".35" y=".35" width="${W - 0.7}" height="${H - 0.7}" rx="${R - 0.3}" fill="none" stroke="url(#hl)" stroke-width=".7"/><rect x="2.8" y="2.8" width="${W - 5.6}" height="${H - 5.6}" rx="2.2" fill="none" stroke="${pale}" stroke-width=".85" opacity=".7"/>`
    : "";
  return `${shadow}<rect width="${W}" height="${H}" rx="${R}" fill="url(#${gradient})"/>${finish}`;
}

function dollarNote(detail) {
  const cx = W / 2, cy = H / 2;
  let s = note("gd", "#E6F6E6", detail);
  if (!detail) return s + `<ellipse cx="${cx}" cy="${cy}" rx="7.8" ry="8.8" fill="#F1FAEF"/>` + glyph(cx, cy, 1.08, "#1A5435", DOLLAR, false);
  s += `<ellipse cx="${cx}" cy="${cy}" rx="6.7" ry="7.8" fill="url(#md)" stroke="#1C5536" stroke-opacity=".28" stroke-width=".6"/>` + glyph(cx, cy, 0.92, "#1A5435", DOLLAR);
  for (const x of [7.3, W - 7.3]) s += `<circle cx="${x}" cy="${cy}" r="1.9" fill="none" stroke="#E6F6E6" stroke-width="1" opacity=".9"/>`;
  for (const [x1, x2] of [[5.2, 9.4], [W - 9.4, W - 5.2]])
    for (const y of [6.6, H - 6.6]) s += `<path d="M${x1} ${y}H${x2}" stroke="#E6F6E6" stroke-width=".9" stroke-linecap="round" opacity=".7"/>`;
  return s;
}

function euroNote(detail) {
  const cx = 12.8, cy = 10.8;
  let s = note("ge", "#E3EDFC", detail);
  if (!detail) return s + `<circle cx="${cx + 0.4}" cy="${cy + 0.2}" r="7.4" fill="#F1F6FF"/>` + glyph(cx + 1, cy + 0.2, 0.95, "#1B4790", EURO, false);
  s += `<circle cx="${cx}" cy="${cy}" r="6.4" fill="url(#me)" stroke="#1B4790" stroke-opacity=".28" stroke-width=".6"/>` + glyph(cx + 0.5, cy, 0.8, "#1B4790", EURO);
  for (const deg of [-48, -24, 0]) {
    const a = (deg * Math.PI) / 180, x = (cx + 11.8 * Math.cos(a) + 2).toFixed(2), y = cy + 11.8 * Math.sin(a) + 1.5;
    s += `<circle cx="${x}" cy="${(y + 0.35).toFixed(2)}" r="1.35" fill="#12306A" opacity=".35"/><circle cx="${x}" cy="${y.toFixed(2)}" r="1.35" fill="url(#gold)"/>`;
  }
  return s;
}

// The two notes, fanned: euro behind and tilted more, dollar in front and nearly level.
const notes = (detail, scale = 1) =>
  `<g transform="translate(32 32) scale(${scale}) translate(-32 -32)"><g transform="rotate(-19 32 32) translate(6 10)">${euroNote(detail)}</g><g transform="rotate(-5 32 32) translate(16 28)">${dollarNote(detail)}</g></g>`;

/** shape: "rounded" (logo), "square" (stores and iOS mask it themselves), "circle", "none" (notes only, transparent). */
export function icon({ shape = "rounded", detail = true, scale = 1 } = {}) {
  const ground = { rounded: `<rect width="64" height="64" rx="15" fill="url(#bg)"/>`, square: `<rect width="64" height="64" fill="url(#bg)"/>`, circle: `<circle cx="32" cy="32" r="32" fill="url(#bg)"/>`, none: "" }[shape];
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><defs>${DEFS}</defs>${ground}${notes(detail, scale)}</svg>`;
}

const wordmark = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 212 44"><defs>${DEFS}</defs><g transform="translate(0 2) scale(.625)"><rect width="64" height="64" rx="15" fill="url(#bg)"/>${notes(true)}</g><text x="52" y="30" font-family="Onest, system-ui, sans-serif" font-size="23" font-weight="700" letter-spacing="-0.5" fill="#1B1710">Qulay <tspan fill="#2E8552">Kurs</tspan></text></svg>`;

function write(file, text) {
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, text + "\n");
}
function png(svg, size, file) {
  mkdirSync(dirname(file), { recursive: true });
  execFileSync("rsvg-convert", ["-w", String(size), "-h", String(size), "-o", file], { input: svg });
}
function webp(svg, size, file) {
  const tmp = file.replace(/\.webp$/, ".tmp.png");
  png(svg, size, tmp);
  execFileSync("cwebp", ["-quiet", "-lossless", tmp, "-o", file]);
  rmSync(tmp);
}

// Link-preview pictures (og:image), one per site language. These need the Onest web font, so they are drawn with
// the system Chrome through playwright-core (already a dependency of telegram-post.mjs).
const SHARE = {
  uz: ["Qaysi bankda almashtirish foydali?", "Dollar, yevro va rubl kursi — O‘zbekistonning barcha banklarida"],
  ru: ["В каком банке выгоднее обменять?", "Курс доллара, евро и рубля — во всех банках Узбекистана"],
  en: ["Which bank gives the best rate?", "Dollar, euro and ruble rates at every bank in Uzbekistan"],
};
async function shareImages(web) {
  const { chromium } = await import("playwright-core");
  const browser = await chromium.launch({ channel: process.env.CHROME_CHANNEL || "chrome" });
  for (const [lang, [title, sub]] of Object.entries(SHARE)) {
    const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });
    await page.setContent(`<html><head><link href="https://fonts.googleapis.com/css2?family=Onest:wght@500;800&display=block" rel="stylesheet"><style>
      body { margin: 0; width: 1200px; height: 630px; display: flex; align-items: center; gap: 56px; padding: 0 80px; box-sizing: border-box;
        background: linear-gradient(180deg, ${GOLD_TOP}, ${GOLD_BOTTOM}); font-family: Onest, system-ui, sans-serif; color: #1B1710; }
      svg { flex: none; filter: drop-shadow(0 18px 30px rgba(90, 63, 0, .22)); }
      .brand { font-size: 34px; font-weight: 800; letter-spacing: -.02em; } .brand b { color: #26794A; }
      h1 { margin: 14px 0 18px; font-size: 68px; line-height: 1.04; font-weight: 800; letter-spacing: -.035em; }
      p { margin: 0; font-size: 30px; line-height: 1.3; font-weight: 500; color: rgba(27, 23, 16, .72); }
      .url { margin-top: 30px; font-size: 26px; font-weight: 800; color: #26794A; }
    </style></head><body>${icon({ shape: "none", scale: 1.08 }).replace("<svg ", '<svg width="400" height="400" overflow="visible" ')}
      <div><div class="brand">Qulay <b>Kurs</b></div><h1>${title}</h1><p>${sub}</p><div class="url">qulaykurs.uz</div></div></body></html>`, { waitUntil: "networkidle" });
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: web(`og-${lang}.png`) });
    await page.close();
  }
  await browser.close();
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const brand = (f) => join(ROOT, "brand", f), web = (f) => join(ROOT, "web", "public", f);

  write(brand("logo.svg"), icon());
  write(brand("logo-small.svg"), icon({ detail: false }));
  write(brand("logo-wordmark.svg"), wordmark);
  png(icon(), 512, brand("logo-512.png"));
  png(icon({ shape: "square" }), 1024, brand("app-icon-1024.png"));
  // Telegram crops the avatar to a circle: the notes are scaled down to sit inside it.
  png(icon({ shape: "square", scale: 0.84 }), 640, brand("telegram-avatar.png"));

  write(web("logo.svg"), icon());
  write(web("favicon.svg"), icon({ detail: false }));
  png(icon({ shape: "square" }), 180, web("apple-touch-icon.png"));
  png(icon({ shape: "square", scale: 0.84 }), 192, web("icon-192.png"));
  png(icon({ shape: "square", scale: 0.84 }), 512, web("icon-512.png"));

  png(icon({ shape: "square" }), 1024, join(ROOT, "iosApp", "iosApp", "Assets.xcassets", "AppIcon.appiconset", "AppIcon-1024.png"));
  png(icon({ shape: "square" }), 512, join(ROOT, "store", "icon-512.png"));

  // Android. 8+ draws the adaptive icon: gradient background (drawable/ic_launcher_background.xml) under this
  // transparent foreground, whose notes stay inside the 66dp safe circle of the 108dp canvas. Older versions use the bitmaps.
  const res = (d, f) => join(ROOT, "androidApp", "src", "main", "res", `mipmap-${d}`, f);
  const density = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };
  for (const [d, k] of Object.entries(density)) {
    webp(icon({ shape: "none", scale: 0.66 }), 108 * k, res(d, "ic_launcher_foreground.webp"));
    webp(icon({ detail: k < 2 ? false : true }), 48 * k, res(d, "ic_launcher.webp"));
    webp(icon({ shape: "circle", scale: 0.84, detail: k < 2 ? false : true }), 48 * k, res(d, "ic_launcher_round.webp"));
  }
  await shareImages(web);
  console.log("brand assets rendered");
}
