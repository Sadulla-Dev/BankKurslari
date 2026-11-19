// Renders the OpenFreeMap Positron basemap (the style the app's map uses) at the screenshot camera.
// Usage: node basemap.js <maplibre-gl dist dir> <out.png> [lat lng zoom]
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  const [dist, out, lat = '41.3060', lng = '69.2790', zoom = '13.6'] = process.argv.slice(2);
  const js = fs.readFileSync(path.join(dist, 'maplibre-gl.js'), 'utf8');
  const css = fs.readFileSync(path.join(dist, 'maplibre-gl.css'), 'utf8');
  const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const page = await browser.newPage({ viewport: { width: 420, height: 900 }, deviceScaleFactor: 3 });
  page.on('pageerror', e => console.log('ERR', e.message));
  await page.setContent(`<html><head><style>${css} html,body,#m{margin:0;width:100%;height:100%}</style></head><body><div id="m"></div></body></html>`);
  await page.addScriptTag({ content: js });
  await page.evaluate(([lat, lng, zoom]) => new Promise(res => {
    const map = new maplibregl.Map({ container: 'm', style: 'https://tiles.openfreemap.org/styles/positron',
      center: [lng, lat], zoom, attributionControl: false, interactive: false, preserveDrawingBuffer: true });
    map.on('idle', () => res());
  }), [+lat, +lng, +zoom]);
  await page.waitForTimeout(500);
  await page.screenshot({ path: out });
  await browser.close();
  console.log('basemap', out);
})();
