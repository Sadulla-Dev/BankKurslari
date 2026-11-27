// Branch-level weekend data: every bank's own list of offices open this weekend (src/weekendLists.js), each office
// matched to its point on the map (/api/branches).
//
// A match needs the same bank, the same town or district (when both addresses name one), and enough of the street,
// house number or office name in common, with a clear lead over the next-best point. Anything less stays unmatched:
// it is listed with the address the bank gave, and never put on the map at a guessed position.
import { readBankList, latin } from "./weekendLists.js";

export const branchBankKey = (name) => String(name || "").toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");

/* ---------- address tokens ---------- */

// Words every address has; they say nothing about which branch it is.
const STOP = new Set(`kochasi kocha koch kuchasi kucha kuch k shoh sh shahri shahar shahr shaharchasi tumani tuman t
  viloyati viloyat vil respublikasi respublika uy uyi uylar mfy kfy fy massivi massiv mavzesi mavze dahasi daha kvartal
  ul ulitsa g gorod dom prospekt prospekti raion rayon oblast tor berk yoki va bilan qarshisida karshisida hududi hududida
  binosi binosida joylashgan manzil manzili bhm bho mbho ubho bank banki bankning hizmatlari hizmat korsatish markazi
  markazlari ofisi ofis filiali filial kassasi kassa kassalari chakana amaliyotlar amaliyotlari amaliyot vash va ham
  atb aj atib ajb sonli raqamli uz ozbekiston`.split(/\s+/));
// Words that name a town or district: the word before one of these.
// ("Yangi shahar ko‘chasi" is a street, not the town of Yangi.)
const PLACE = /([a-z]{3,})\s+(?:tumani|tuman|t|shahri|shahar|shahr|sh|shaharchasi|viloyati|vil|rayon|raion|oblast)\b(?!\s+(?:kochasi|kuchasi|kocha|kucha|koch)\b)/g;

/** Comparable text: Latin, no apostrophes, x = h and q = k (spelt both ways), house numbers glued to their letter. */
export function norm(s) {
  return latin(s)
    .replace(/'/g, "")
    .replace(/x/g, "h").replace(/q/g, "k").replace(/ʼ/g, "")
    .replace(/(\d+)\s*-?\s*(?:uy|uyi|dom|d)\b\.?/g, "$1 ")
    .replace(/(\d+)\s*-?\s*([a-z])(?![a-z])/g, "$1$2")
    .replace(/[^a-z0-9\/]+/g, " ")
    .replace(/\s+/g, " ").trim();
}
const STREET_AFTER = /\b(?:koch|kuch|ul|ulitsa)\s+([a-z]{3,})/g; // "ko‘ch. Hamza, 2" (bank.uz)
const STREET_BEFORE = /([a-z]{3,})(?:\s+(?:shahar|shoh|shoh|yoli|tor))?\s+(?:kochasi|kuchasi|kocha|kucha|prospekti|shohkochasi)\b/g; // "Hamza ko‘chasi 2-uy"
/**
 * { words, numbers, places, streets, house, ring } of an address or name. Postcodes (6 digits) are not house numbers.
 * `house`: the number after the street (or the last number), `ring`: which Tashkent ring road.
 */
export function tokens(s) {
  const t = norm(s);
  const words = new Set(), numbers = new Set(), places = new Set(), streets = new Set();
  for (const w of t.split(" ")) {
    if (/^\d{6}$/.test(w)) continue;
    if (/^\d/.test(w)) { numbers.add(w.replace(/\/.*$/, "")); numbers.add(w); continue; }
    if (w.length >= 3 && !STOP.has(w)) words.add(w);
  }
  for (const m of t.matchAll(PLACE)) if (!STOP.has(m[1])) places.add(m[1]);
  for (const re of [STREET_AFTER, STREET_BEFORE]) for (const m of t.matchAll(re)) if (!STOP.has(m[1]) && !places.has(m[1])) streets.add(m[1]);
  const afterStreet = /\b(?:kochasi|kuchasi|kocha|koch|kuch|prospekti|ul)\s+(?:[a-z]+\s+){0,3}?(\d+[a-z]?)(?:\/\d+)?\b/.exec(t);
  const nums = t.split(" ").filter((w) => /^\d+[a-z]?(\/\d+)?$/.test(w) && !/^\d{6}$/.test(w));
  const house = afterStreet?.[1] || (streets.size ? nums.at(-1) : null) || null;
  // Tashkent has three ring roads: TKAD/TXAY (the outer one), Katta halqa yo‘li and Kichik halqa yo‘li.
  const ring = /\bkichik halka\b/.test(t) ? "small" : /\bkatta halka\b/.test(t) ? "big" : /\b(tkad|thay|tkay|halka avtomobil)\b/.test(t) ? "outer" : null;
  return { words, numbers, places, streets, house, ring };
}
const houseNo = (h) => (h ? Number.parseInt(h, 10) : null);

/* ---------- matching ---------- */

// Who knows a branch's position best: a user-confirmed point, the bank's own list, then the rest. ("hamkorbank.uz" is
// the bank's own site, not bank.uz.)
const sourceRank = (b) => (/^manual/i.test(b.source || "") ? 0 : b.source && !/^(bank\.uz|osm|openstreetmap|yandex)\b/i.test(b.source) ? 1 : 2);
const metres = (a, b) => Math.hypot((a.lat - b.lat) * 111320, (a.lng - b.lng) * 111320 * Math.cos((a.lat * Math.PI) / 180));

/**
 * Entries of one bank -> the same entries with `point` ({ lat, lng } of the matched /api/branches point, or null).
 * `points`: that bank's points from /api/branches.
 */
export function matchEntries(entries, points) {
  const P = points.filter((p) => p.lat && p.lng).map((p) => ({ p, addr: tokens(p.address || ""), name: tokens(p.name || "") }));
  // How rare each word is among this bank's points: a word on one point identifies it, "toshkent" does not.
  const df = new Map();
  for (const x of P) for (const w of new Set([...x.addr.words, ...x.name.words])) df.set(w, (df.get(w) || 0) + 1);
  const idf = (w) => Math.log((P.length + 1) / ((df.get(w) || 0) + 0.5));
  return entries.map((e) => {
    const addr = tokens(e.address || "");
    const name = tokens(`${e.name || ""} ${e.parent || ""}`);
    const region = tokens(e.region || "");
    const places = new Set([...addr.places, ...region.places]);
    const scored = [];
    for (const x of P) {
      // Different towns: never the same branch.
      const theirPlaces = new Set([...x.addr.places, ...x.name.places]);
      if (places.size && theirPlaces.size && ![...places].some((w) => theirPlaces.has(w))) continue;
      // A different street, house number or ring road is a different place, whatever the names say.
      if (addr.streets.size && x.addr.streets.size && ![...addr.streets].some((w) => x.addr.streets.has(w))) continue;
      if (addr.house && x.addr.house && houseNo(addr.house) !== houseNo(x.addr.house)) continue;
      if (addr.ring && x.addr.ring && addr.ring !== x.addr.ring) continue;
      let score = 0, street = 0, nameHit = 0;
      for (const w of addr.words) if (x.addr.words.has(w) || x.name.words.has(w)) { score += idf(w); if (!places.has(w)) street++; }
      const house = [...addr.numbers].some((n) => x.addr.numbers.has(n));
      if (house && street) score += 2;
      for (const w of name.words) if (x.name.words.has(w)) { score += idf(w); nameHit++; }
      // Without an address, the office's own name must all be in the point's name ("Qo‘rg‘ontepa BXM").
      // ("City Mall SKMdagi BXO": "SKMdagi" is "in the SKM mall", not part of the name.)
      if (!e.address && (!name.words.size || [...name.words].some((w) => !x.name.words.has(w) && !/dagi$/.test(w)))) continue;
      if (score > 0) scored.push({ x, score, street, house, nameHit });
    }
    scored.sort((a, b) => b.score - a.score);
    // The same street and house number, or the office's own name (with the street when there is an address).
    const evidence = (c) => (c.house && c.street) || (c.nameHit && (c.street || !e.address));
    let best = scored[0];
    let ok = false;
    if (best && evidence(best) && best.score >= 3) {
      // Close runners-up must be the same place: nearby, or the same branch listed twice (bank.uz and the bank's own
      // site, sometimes kilometres apart). Then the better source wins, as in /api/branches.
      const close = scored.slice(1).filter((c) => best.score - c.score < 1.5 && metres(best.x.p, c.x.p) >= 80);
      if (!close.length) ok = true;
      else if (close.every((c) => evidence(c) && c.house === best.house && c.nameHit === best.nameHit)) {
        const ranked = [best, ...close].sort((a, b) => sourceRank(a.x.p) - sourceRank(b.x.p));
        if (sourceRank(ranked[0].x.p) < sourceRank(ranked[1].x.p)) { best = ranked[0]; ok = true; }
      }
    }
    const point = ok ? { lat: best.x.p.lat, lng: best.x.p.lng } : null;
    return { ...e, point };
  });
}

/* ---------- everything ---------- */

/** Run `fn` over `items`, `n` at a time. */
async function pool(items, n, fn) {
  const out = new Array(items.length);
  let i = 0;
  await Promise.all(Array.from({ length: Math.min(n, items.length) }, async () => { while (i < items.length) { const k = i++; out[k] = await fn(items[k]); } }));
  return out;
}

/**
 * weekend: the object from getWeekendBanks(); branches: /api/branches' list.
 * -> { lists: [{ name, url, bank, status, note, count, matched }], entries: [{ bank, name, parent, address, region, open, point }] }
 */
export async function getWeekendBranches(weekend, branches, { get, previous } = {}) {
  const results = await pool(weekend.banks, 4, (b) => readBankList(b, weekend.days, get ? { get } : {}));
  const lists = [], entries = [];
  for (const r of results) {
    // A bank that answered last time but not now keeps last time's list (same Central Bank list only).
    const prev = previous?.listUrl === weekend.listUrl ? previous.lists?.find((l) => l.url === r.url) : null;
    if ((r.status === "error" || r.status === "blocked") && prev?.status === "ok") {
      lists.push({ ...prev, note: `kept from ${previous.branchesCheckedAt}: ${r.note}` });
      entries.push(...previous.entries.filter((e) => e.list === r.url));
      continue;
    }
    const key = branchBankKey(r.bank);
    const points = branches.filter((p) => branchBankKey(p.bank) === key);
    const matched = r.entries.length ? matchEntries(r.entries, points) : [];
    lists.push({ name: r.name, url: r.url, bank: points[0]?.bank || r.bank, status: r.status, ...(r.note ? { note: r.note } : {}), count: matched.length, matched: matched.filter((e) => e.point).length });
    for (const e of matched) entries.push({ bank: points[0]?.bank || r.bank, list: r.url, ...e });
  }
  return { lists, entries };
}
