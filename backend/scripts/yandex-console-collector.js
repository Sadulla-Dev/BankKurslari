// Paste into the DevTools console on an open https://yandex.uz/maps/ tab (Arc/Chrome).
// Uses the same internal search endpoint the Maps UI calls, tiles Uzbekistan, and downloads
// yandex-branches.json in the format backend/data/yandex-branches.json expects.
(async () => {
  const QUERIES = ["bank", "банк"];
  const BBOX = { west: 55.9, south: 37.1, east: 73.2, north: 45.6 };
  const PAGE = 50, MAX_DEPTH = 9, MAX_SKIP = 500;
  const pause = (ms) => new Promise((r) => setTimeout(r, ms));
  const all = new Map();
  let csrf = "", requests = 0, incomplete = 0;

  async function api(text, b, skip) {
    const p = new URLSearchParams({
      ajax: "1", text, lang: "uz_UZ", origin: "maps-search-form", results: String(PAGE), skip: String(skip),
      ll: `${(b.west + b.east) / 2},${(b.south + b.north) / 2}`, spn: `${b.east - b.west},${b.north - b.south}`,
      z: "12", csrfToken: csrf
    });
    for (let attempt = 0; attempt < 4; attempt++) {
      requests++;
      const res = await fetch(`/maps/api/search?${p}`, { credentials: "include" });
      const json = await res.json().catch(() => ({}));
      if (json.csrfToken && !json.data) { csrf = json.csrfToken; p.set("csrfToken", csrf); continue; }
      if (json.data) return json.data;
      await pause(1500 * (attempt + 1)); // throttled or captcha: back off
    }
    throw new Error("Yandex search failed (captcha?). Open the map, solve it, and re-run.");
  }

  const inBox = (b, lat, lng) => lat >= b.south && lat <= b.north && lng >= b.west && lng <= b.east;
  function add(item) {
    const [lng, lat] = item.coordinates || item.displayCoordinates || [];
    if (!Number.isFinite(lat) || !Number.isFinite(lng) || !inBox(BBOX, lat, lng)) return;
    const cats = (item.categories || []).map((c) => `${c.class || ""} ${c.name || ""}`).join(" ");
    const title = item.title || item.shortTitle || "";
    if (!/банк|bank|financ/i.test(`${title} ${cats}`) || /банкомат|atm/i.test(`${title} ${cats}`)) return;
    if (item.country && !/узбекистан|uzbekistan|o‘zbekiston|ozbekiston/i.test(item.country)) return;
    const id = String(item.id || item.uri || `${lat.toFixed(6)},${lng.toFixed(6)}`);
    all.set(id, {
      id, bank: title || "Bank", name: item.description || title || "Bank",
      address: item.fullAddress || item.address || null,
      hours: item.workingTimeText || item.workingTime?.text || null,
      lat, lng, source: "Yandex Maps UI",
      sourceUrl: item.seoname ? `https://yandex.uz/maps/org/${item.seoname}/${item.id}/` : null
    });
  }

  async function collect(text, b, depth = 0) {
    await pause(250);
    const d = await api(text, b, 0);
    const items = d.items || [];
    items.forEach(add);
    const total = Number(d.totalResultCount ?? d.requestResults ?? items.length);
    if (depth < MAX_DEPTH && (items.length >= PAGE || total > items.length)) {
      const mx = (b.west + b.east) / 2, my = (b.south + b.north) / 2;
      for (const t of [
        { west: b.west, south: b.south, east: mx, north: my }, { west: mx, south: b.south, east: b.east, north: my },
        { west: b.west, south: my, east: mx, north: b.north }, { west: mx, south: my, east: b.east, north: b.north }
      ]) await collect(text, t, depth + 1);
      return;
    }
    for (let skip = PAGE; skip < Math.min(total, MAX_SKIP); skip += PAGE) {
      await pause(250);
      const next = (await api(text, b, skip)).items || [];
      next.forEach(add);
      if (next.length < PAGE) break;
    }
    if (total > MAX_SKIP) incomplete++;
    console.log(`depth ${depth} found ${items.length}/${total} -> unique ${all.size}`);
  }

  for (const q of QUERIES) await collect(q, BBOX);

  const now = new Date();
  const branches = [...all.values()].sort((a, b) => a.bank.localeCompare(b.bank) || a.name.localeCompare(b.name));
  const dataset = {
    provider: "yandex-maps", coverage: "uzbekistan",
    fetchedAt: now.toISOString(), expiresAt: new Date(now.getTime() + 30 * 864e5).toISOString(),
    queryCount: requests, incompleteTiles: incomplete,
    completeness: incomplete ? "partial (paging limit reached in dense tiles)" : "best-effort",
    count: branches.length, branches
  };
  const a = Object.assign(document.createElement("a"), {
    href: URL.createObjectURL(new Blob([JSON.stringify(dataset, null, 2) + "\n"], { type: "application/json" })),
    download: "yandex-branches.json"
  });
  a.click();
  console.log(`Done: ${branches.length} branches, ${requests} requests`);
  window.__yandexBranches = dataset;
})();
