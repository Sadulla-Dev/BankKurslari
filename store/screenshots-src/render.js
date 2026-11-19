// Renders every [data-name] block of index.html to store/screenshots/<name>.png
// Slides are 540x960 CSS px at deviceScaleFactor 2 -> 1080x1920; feature graphic -> 2048x1000 (downscaled to 1024x500 afterwards).
// Usage (from repo root): node store/screenshots-src/render.js
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
  const src = path.join(__dirname, 'index.html');
  const out = path.join(__dirname, '..', 'screenshots');
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1200, height: 1000 }, deviceScaleFactor: 2 });
  page.on('pageerror', e => console.log('ERR', e.message));
  page.on('requestfailed', r => console.log('FAIL', r.url()));
  await page.goto('file://' + src);
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(500);
  for (const el of await page.$$('[data-name]')) {
    const name = await el.getAttribute('data-name');
    await el.screenshot({ path: path.join(out, `${name}.png`) });
    console.log('saved', name);
  }
  await browser.close();
})();
