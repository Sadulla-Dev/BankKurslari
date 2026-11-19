// Renders the brand PNGs (Telegram avatar, iOS app icon, Android legacy launcher icons) from the logo geometry.
//   node scripts/render-brand.mjs   -> brand/*.png, androidApp/src/main/res/mipmap-*/ic_launcher*.png (as webp via cwebp)
import { mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";
import { chromium } from "playwright-core";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const mark = (stroke = "#fff") => `<path d="M7.5 21.5l5.5-5.5 4 4 6.5-7" stroke="${stroke}" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/><circle cx="23.8" cy="12.6" r="3" fill="#F2C14E"/>`;
// Full-bleed square (iOS and Telegram round/crop it themselves); the mark is scaled into the centre so a circle crop keeps it.
const square = (scale) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" fill="#2F7A68"/><g transform="translate(16 16) scale(${scale}) translate(-16 -17)">${mark()}</g></svg>`;
const rounded = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="9" fill="#2F7A68"/>${mark()}</svg>`;
const round = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><circle cx="16" cy="16" r="16" fill="#2F7A68"/><g transform="translate(16 16) scale(.82) translate(-16 -17)">${mark()}</g></svg>`;

const browser = await chromium.launch({ channel: process.env.CHROME_CHANNEL || "chrome" });
async function png(svg, size, file) {
  await mkdir(dirname(file), { recursive: true });
  const page = await browser.newPage({ viewport: { width: Math.max(size, 200), height: Math.max(size, 200) } });
  await page.setContent(`<html><body style="margin:0;background:transparent">${svg.replace("<svg ", `<svg style="display:block" width="${size}" height="${size}" `)}</body></html>`);
  await page.locator("svg").screenshot({ path: file, omitBackground: true });
  await page.close();
}
await png(square(1.12), 640, join(ROOT, "brand", "telegram-avatar.png"));
await png(square(1.12), 1024, join(ROOT, "brand", "app-icon-1024.png"));
await png(square(1.12), 1024, join(ROOT, "iosApp", "iosApp", "Assets.xcassets", "AppIcon.appiconset", "AppIcon-1024.png"));
await png(rounded, 512, join(ROOT, "brand", "logo-512.png"));
// Android < 8 launcher icons (8+ uses the adaptive vector icon).
const dens = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [d, px] of Object.entries(dens)) {
  for (const [name, svg] of [["ic_launcher", rounded], ["ic_launcher_round", round]]) {
    const dir = join(ROOT, "androidApp", "src", "main", "res", `mipmap-${d}`);
    const tmp = join(dir, `${name}.png`);
    await png(svg, px, tmp);
    execFileSync("cwebp", ["-quiet", "-lossless", tmp, "-o", join(dir, `${name}.webp`)]);
    execFileSync("rm", [tmp]);
  }
}
await browser.close();
console.log("brand assets rendered");
