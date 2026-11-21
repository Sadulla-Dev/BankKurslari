// Offline copy of the site and the rates: when the network is there everything comes fresh from it and a copy is
// kept; when it is not (metro, plane, abroad without roaming) the last copy is shown. Rates carry their own
// "updated" time, so a stale copy is visible as such.
"use strict";
const CACHE = "qk-v1";
const API = "https://api.qulaykurs.uz";
const TIMEOUT_MS = 4000;

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(["/", "/logo.svg", "/favicon.svg"])).catch(() => {}));
  self.skipWaiting();
});
self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});

// Network first (with a timeout), cache as the fallback; successful answers refresh the cache.
async function networkFirst(req) {
  const cache = await caches.open(CACHE);
  try {
    const res = await Promise.race([fetch(req), new Promise((_, no) => setTimeout(() => no(new Error("timeout")), TIMEOUT_MS))]);
    if (res.ok) cache.put(req, res.clone()).catch(() => {});
    return res;
  } catch (err) {
    const hit = await cache.match(req, { ignoreVary: true });
    if (hit) return hit;
    if (req.mode === "navigate") { const home = await cache.match("/"); if (home) return home; }
    throw err;
  }
}

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  const sameOrigin = url.origin === self.location.origin;
  // Map tiles and other third-party files: leave to the browser.
  if (!sameOrigin && url.origin !== API) return;
  // Branch and ATM lists are large and rarely needed offline.
  if (url.origin === API && /\/api\/(branches|atms)/.test(url.pathname)) return;
  e.respondWith(networkFirst(req));
});
