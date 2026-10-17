// Bank branches from the banks' OWN websites (public branch lists / the JSON their branch pages use).
// One importer per bank; each returns offices only (no ATMs), normalised to
// { bank, name, address, hours, lat, lng, source }. A failing importer is skipped, never fatal.

import * as cheerio from "cheerio";

const UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

// Retries network errors / timeouts twice (not HTTP errors such as 403).
async function get(url, opts = {}) {
  for (let attempt = 0; ; attempt++) {
    try {
      return await getOnce(url, opts);
    } catch (error) {
      if (attempt >= 2 || String(error?.message).startsWith("HTTP ")) throw error;
      await new Promise((r) => setTimeout(r, 2000 * (attempt + 1)));
    }
  }
}

async function getOnce(url, { method = "GET", headers = {}, body, timeout = 25000 } = {}) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeout);
  try {
    const res = await fetch(url, { method, body, signal: ctrl.signal, headers: { "User-Agent": UA, Accept: "application/json, text/html;q=0.9, */*;q=0.8", ...headers } });
    if (!res.ok) throw new Error(`HTTP ${res.status} ${url}`);
    return await res.text();
  } finally {
    clearTimeout(timer);
  }
}
const getJson = async (url, opts) => JSON.parse(await get(url, opts));

const num = (v) => {
  const n = typeof v === "number" ? v : parseFloat(String(v ?? "").trim().replace(",", "."));
  return Number.isFinite(n) ? n : null;
};
const clean = (s) => (s == null ? null : String(s).replace(/<[^>]+>/g, " ").replace(/&nbsp;/g, " ").replace(/\s+/g, " ").trim() || null);
// Uzbekistan bounding box: drops 0/0 and swapped coordinates.
const inUz = (lat, lng) => lat != null && lng != null && lat > 37 && lat < 46 && lng > 55 && lng < 74;

function item(bank, name, address, hoursRaw, lat, lng, source) {
  const la = num(lat);
  const ln = num(lng);
  if (!inUz(la, ln)) return null;
  return { bank, name: clean(name) || bank, address: clean(address), hours: normalizeHours(hoursRaw), lat: la, lng: ln, source };
}

/* ---------- opening hours: free text (uz / ru) -> OSM "Mo-Fr 09:00-18:00; Sa 09:00-13:00" ---------- */

const DAYS = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"];
// Longest names first so "dushanba" wins over "du". Values: index into DAYS.
const DAY_WORDS = [
  ["dushanba", 0], ["dush", 0], ["du", 0], ["seshanba", 1], ["sesh", 1], ["se", 1], ["chorshanba", 2], ["chor", 2], ["ch", 2],
  ["payshanba", 3], ["pay", 3], ["pa", 3], ["juma", 4], ["jum", 4], ["ju", 4], ["shanba", 5], ["shan", 5], ["sh", 5],
  ["yakshanba", 6], ["yak", 6], ["ya", 6],
  ["понедельник", 0], ["пон", 0], ["пн", 0], ["вторник", 1], ["вт", 1], ["среда", 2], ["ср", 2], ["четверг", 3], ["чт", 3],
  ["пятница", 4], ["пятн", 4], ["пят", 4], ["пт", 4], ["суббота", 5], ["суб", 5], ["сб", 5], ["воскресенье", 6], ["вос", 6], ["вс", 6]
];
const dayWordRe = new RegExp(`(?<![\\p{L}])(${DAY_WORDS.map(([w]) => w).join("|")})\\.?(?![\\p{L}])`, "giu");
const TIME_RE = /(\d{1,2})[:.](\d{2})\s*(?:-|–|—|до|dan|gacha|to)?\s*(?:до\s*)?(\d{1,2})[:.](\d{2})/i;
const pad = (h, m) => `${String(h).padStart(2, "0")}:${m}`;

function daysIn(text) {
  const found = [];
  for (const m of text.matchAll(dayWordRe)) {
    const hit = DAY_WORDS.find(([w]) => w === m[1].toLowerCase());
    if (hit) found.push({ idx: hit[1], at: m.index });
  }
  if (!found.length) {
    // Days off first: "dam olish kunlari" also contains "ish kun".
    if (/выходн|dam olish|shanba.{0,3}yakshanba/i.test(text)) return [5, 6];
    if (/будн|(?<!ol)ish kun|рабоч/i.test(text)) return [0, 1, 2, 3, 4];
    if (/ежеднев|har kuni|kunlik|круглосуточ|24\/7/i.test(text)) return [0, 1, 2, 3, 4, 5, 6];
    return null;
  }
  // "Du-Ju" / "Пн–Пт" -> range; otherwise a list of single days.
  if (found.length >= 2 && /[-–—]/.test(text.slice(found[0].at, found[1].at + 1))) {
    const out = [];
    for (let d = found[0].idx; ; d = (d + 1) % 7) {
      out.push(d);
      if (d === found[1].idx || out.length > 7) break;
    }
    return out;
  }
  return [...new Set(found.map((f) => f.idx))];
}

function rangeLabel(days) {
  const sorted = [...days].sort((a, b) => a - b);
  const contiguous = sorted.every((d, i) => i === 0 || d === sorted[i - 1] + 1);
  if (contiguous && sorted.length > 1) return `${DAYS[sorted[0]]}-${DAYS[sorted[sorted.length - 1]]}`;
  return sorted.map((d) => DAYS[d]).join(",");
}

/** Best effort; returns null when days or times cannot be read (the app then shows "hours unknown"). */
export function normalizeHours(raw) {
  if (raw == null) return null;
  // Cyrillic "а" typed inside Latin words ("Jumа") breaks day matching.
  const text = (Array.isArray(raw) ? raw.join("; ") : String(raw)).replace(/(?<=[a-z])а|а(?=[a-z])/gi, "a");
  if (!text.trim()) return null;
  if (!TIME_RE.test(text) && /uzluksiz/i.test(text)) return "24/7";
  if (/круглосуточ|24\s*\/\s*7|kecha-kunduz/i.test(text)) return "24/7";

  // Split into rules at line breaks / semicolons; "lunch" lines are ignored.
  // "Du-Ju 09:00-18:00, Tushlik 13:00-14:00": the lunch clause becomes its own (ignored) part.
  const parts = text.replace(/,\s*(?=tushlik|обед|перерыв)/gi, ";").split(/[\n;|]+/).map((p) => p.trim()).filter((p) => p && !/обед|tushlik|перерыв/i.test(p));
  const rules = [];
  let offDays = null;
  for (const p of parts) {
    const t = p.match(TIME_RE);
    if (!t) {
      // "Dam olish kunlari: shanba, yakshanba" -> remember the days off.
      if (/dam olish|выходн/i.test(p)) offDays = daysIn(p.replace(/dam olish kunlari|выходные дни|выходные/i, ""));
      continue;
    }
    const rest = p.slice(0, t.index) + " " + p.slice(t.index + t[0].length);
    const offWord = /выходн|dam olish/i;
    let days;
    // "c 9:00 до 18:00 (сб и вс выходные)": named days + "days off" word -> those days are closed.
    // "Выходные — с 09:00 до 17:00" (no named day) stays a weekend schedule.
    const named = [...rest.matchAll(dayWordRe)].length > 0;
    if (named && offWord.test(rest)) {
      const off = daysIn(rest.replace(offWord, " "));
      days = off ? [0, 1, 2, 3, 4, 5, 6].filter((d) => !off.includes(d)) : null;
    } else {
      days = daysIn(rest);
    }
    rules.push({ days, time: `${pad(t[1], t[2])}-${pad(t[3], t[4])}` });
  }
  if (!rules.length) return null;
  // A single time with no days: only usable when the days off are stated (=> the rest of the week).
  const out = [];
  for (const r of rules) {
    let days = r.days;
    if (!days && offDays && offDays.length) days = [0, 1, 2, 3, 4, 5, 6].filter((d) => !offDays.includes(d));
    if (!days || !days.length) return null;
    out.push(`${rangeLabel(days)} ${r.time}`);
  }
  return out.join("; ");
}

/* ---------- importers ---------- */

const importers = {
  async "Anorbank"() {
    const j = await getJson("https://anorbank.uz/api/locations.php?lang=uz");
    return (j.data || []).filter((x) => x.type === "office")
      .map((x) => item("Anorbank", x.name, x.address, x.work_time, x.latitude, x.longitude, "anorbank.uz"));
  },

  async "Hayot Bank"() {
    const url = (cat) => `https://api.hayotbank.uz/api/contacts/get-all?page=0&size=2000&search=contact.active=true;contactCategory.id=${cat}`;
    const lists = await Promise.all([6, 11].map((c) => getJson(url(c)).catch(() => ({ data: [] }))));
    return lists.flatMap((j) => j.data || []).map((x) =>
      item("Hayot Bank", x.title?.uz || x.title?.ru, x.address?.uz || x.address?.ru, x.workTime?.ru || x.workTime?.uz,
        x.mapLocation?.latitude, x.mapLocation?.longitude, "hayotbank.uz"));
  },

  async "Tenge Bank"() {
    const out = [];
    for (const kind of ["branch", "universal_cash"]) {
      for (let page = 1; page <= 10; page++) {
        const j = await getJson(`https://tengebank.uz/uz/api/map_new/${kind}?city=&page=${page}`, {
          method: "POST", headers: { "Content-Type": "application/json" }, body: "{}"
        });
        for (const x of j.data || []) out.push(item("Tenge Bank", x.title, x.address, x.works, x.coords?.[0], x.coords?.[1], "tengebank.uz"));
        if (page >= (j.pages || 1)) break;
      }
    }
    return out;
  },

  async "Infinbank"() {
    const j = await getJson("https://www.infinbank.com/test-api/v1-wa/locations/branch/", { headers: { Lang: "uz", "Content-Type": "application/json" } });
    return (j.data?.locations || []).map((x) => {
      // workDays [1..5] (1 = Monday) + openTime/closeTime -> build the rule directly.
      const days = (x.workDays || []).map((d) => d - 1).filter((d) => d >= 0 && d < 7);
      const hours = days.length && x.openTime && x.closeTime ? `${rangeLabel(days)} ${x.openTime}-${x.closeTime}` : null;
      const it = item("Infinbank", x.title, x.address, null, x.latitude, x.longitude, "infinbank.com");
      if (it) it.hours = hours;
      return it;
    });
  },

  async "Hamkorbank"() {
    const j = await getJson("https://hamkorbank.uz/api-next/branches/");
    return (j.data || []).map((x) =>
      item("Hamkorbank", x.title?.uz || x.title?.ru, x.address?.uz || x.address?.ru,
        [x.work_time?.ru || x.work_time?.uz, x.weekends?.ru ? `Выходные ${x.weekends.ru}` : null].filter(Boolean).join("\n"),
        x.coords?.lat, x.coords?.lng, "hamkorbank.uz"));
  },

  async "Agrobank"() {
    const j = await getJson("https://agrobank.uz/api/v1/?action=pos.getList&lang=uz&page=0&pageSize=1000");
    const items = j.data?.items || j.data?.list || j.data || [];
    return (Array.isArray(items) ? items : []).filter((x) => x.type === "office")
      .map((x) => item("Agrobank", x.title, x.address, x.workingTime, x.coords?.[0], x.coords?.[1], "agrobank.uz"));
  },

  async "Trastbank"() {
    // section_id: 2870 offices, 2875 service centres (BXM), 3111 exchange points.
    const qs = ["2870", "2875", "3111"].map((s) => `s[]=${s}`).join("&");
    const j = await getJson(`https://trustbank.uz/uz/branches/ajax_map_search_v2.php?${qs}`, { headers: { "X-Requested-With": "XMLHttpRequest" } });
    return (Array.isArray(j) ? j : []).filter((x) => ["2870", "2875", "3111"].includes(String(x.section_id)))
      .map((x) => item("Trastbank", x.name, x.address, x.schedule, x.lat, x.lng, "trustbank.uz"));
  },

  async "Universal bank"() {
    const j = await getJson("https://universalbank.uz/api/locations?pagination[pageSize]=1000&locale=uz");
    return (j.data || []).map(({ attributes: a = {} }) => {
      const addr = (String(a.content || "").match(/Manzil:\s*([^\r\n]+)/i) || [])[1];
      return item("Universal bank", a.name, addr, null, a.latitude, a.longitude, "universalbank.uz");
    });
  },

  async "Poytaxt bank"() {
    const j = await getJson("https://poytaxtbank.uz/uz/branches/ajax_map_search.php?s[]=3181", { headers: { "X-Requested-With": "XMLHttpRequest" } });
    return (Array.isArray(j) ? j : []).filter((x) => String(x.section_id) === "3181").map((x) => {
      const html = String(x.popupHtml || "");
      const hours = (html.match(/Ish tartibi:<\/b>\s*([^<]+)/i) || [])[1];
      const addr = (html.match(/Manzil:<\/b>\s*([^<]+)/i) || [])[1];
      return item("Poytaxt bank", x.name, addr, hours, x.lat, x.lng, "poytaxtbank.uz");
    });
  },

  async "Ipoteka bank"() {
    const $ = cheerio.load(await get("https://www.ipotekabank.uz/about/branches/"));
    const raw = $("#map").attr("data-branches");
    const list = raw ? JSON.parse(raw) : [];
    return list.filter((x) => x.filial_type === "BRANCH" || x.filial_type === "CBU")
      .map((x) => item("Ipoteka bank", x.name, x.address, x.worktime, x.latitude, x.longtitude, "ipotekabank.uz"));
  },

  async "Asia Alliance Bank"() {
    const $ = cheerio.load(await get("https://aab.uz/uz/contacts/branches2/"));
    // Second .filials_list holds the branches (the first one is ATMs).
    const lists = $("div.filials_list");
    const branches = lists.length > 1 ? $(lists[1]) : $(lists[0]);
    const out = [];
    branches.find("div.filial").each((_, el) => {
      const e = $(el);
      const hours = [e.find(".workdays").text(), e.find(".worktime").text()].join(" ");
      out.push(item("Asia Alliance Bank", e.find(".title").text(), e.find(".address").text(), hours,
        e.attr("data-latitude"), e.attr("data-longtitude"), "aab.uz"));
    });
    return out;
  },

  async "Aloqabank"() {
    const $ = cheerio.load(await get("https://aloqabank.uz/uz/branches/"));
    const links = [...new Set($("a[href*='/uz/branches/branches/']").map((_, a) => $(a).attr("href")).get())].slice(0, 30);
    const out = [];
    for (const href of links) {
      const url = href.startsWith("http") ? href : `https://aloqabank.uz${href}`;
      try {
        const page = await get(url);
        const p = cheerio.load(page);
        const m = page.match(/'LON':'([\d.]+)','LAT':'([\d.]+)'/);
        const text = p("body").text().replace(/\s+/g, " ");
        const addr = (text.match(/Manzil:\s*(.{5,140}?)(?:Telefon|Ish tartibi|Tel\.|$)/i) || [])[1];
        const hours = (text.match(/Ish tartibi:\s*(.{5,120}?)(?:Telefon|Manzil|Tel\.|$)/i) || [])[1];
        out.push(item("Aloqabank", p("h1").first().text(), addr, hours, m?.[2], m?.[1], "aloqabank.uz"));
      } catch {
        // one branch page failing must not drop the rest
      }
    }
    return out;
  }
};

const unescape = (h) => String(h || "").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&amp;/g, "&");
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

Object.assign(importers, {
  async "Garant bank"() {
    const j = await getJson("https://garantbank.uz/uz/office-list");
    return (j.allPins || []).filter((x) => ["office", "cku", "kiosk"].includes(x.type))
      .map((x) => item("Garant bank", x.name, x.address, `${x.workDays || ""} ${x.workHours || ""}`, x.coords?.lat, x.coords?.lng, "garantbank.uz"));
  },

  async "Turon bank"() {
    // section_id 3181 service centres, 3840 service offices (3184 = ATMs).
    const j = await getJson("https://turonbank.uz/uz/branches/ajax_map_search.php?q=a", { headers: { "X-Requested-With": "XMLHttpRequest" } });
    return (Array.isArray(j) ? j : []).filter((x) => ["3181", "3840"].includes(String(x.section_id))).map((x) => {
      const html = unescape(x.popupHtml);
      const hours = (html.match(/Ish tartibi:<\/b>\s*([^<]+)/i) || [])[1];
      const addr = (html.match(/Manzil:<\/b>\s*([^<]+)/i) || [])[1];
      return item("Turon bank", x.name, addr, hours, x.lat, x.lng, "turonbank.uz");
    });
  },

  async "Orient Finans Bank"() {
    const fields = "id,title,coords,location_type,city,street,house,work_day_first,work_day_last,work_day_start,work_day_end";
    const j = await getJson(`https://ofb.uz/api/collections/locations/entries?filter%5Bsite%5D=uz&fields=${fields}&limit=2000`);
    return (j.data || []).filter((x) => ["BXO", "BXM", "Mini-banklar"].includes(x.location_type?.title) && !/^bankomat/i.test(x.title || ""))
      .map((x) => {
        const [lat, lng] = String(x.coords || "").split(",");
        const hours = x.work_day_start && x.work_day_end
          ? `${x.work_day_first || ""}-${x.work_day_last || ""} ${x.work_day_start}-${x.work_day_end}`
          : `${x.work_day_first || ""}-${x.work_day_last || ""}`;
        const addr = [x.city, x.street, x.house].filter(Boolean).join(", ");
        return item("Orient Finans Bank", x.title, addr, hours, lat, lng, "ofb.uz");
      });
  },

  async "O‘zsanoatqurilishbank"() {
    // The list sqb.uz/uz/filials/ itself loads, from the government open-data portal.
    const j = await getJson("https://data.egov.uz/apiPartner/Partner/WebService?token=636cd1fb276499f932feca4d&name=5-003-0003&offset=0&limit=500&lang=uz");
    return (j.result?.data || []).map((x) =>
      item("O‘zsanoatqurilishbank", x.BXMBXOnomi, [x.TumanNomi, x.Joylashuvi].filter(Boolean).join(", "), x.Ishvaqti, x.latitude, x.longitude, "sqb.uz / data.egov.uz"));
  },

  async "MKBank"() {
    // No list endpoint: office pages from the sitemap, one request each, slowly.
    const xml = await get("https://mkbank.uz/uz/sitemap-iblock-331.xml");
    const urls = [...new Set([...xml.matchAll(/<loc>([^<]*\/uz\/branches\/branches\/[^<]+)<\/loc>/g)].map((m) => m[1]))].slice(0, 200);
    const out = [];
    for (const url of urls) {
      try {
        const page = await get(url);
        const m = page.match(/'LON':'([\d.]+)','LAT':'([\d.]+)'/);
        const name = cheerio.load(page)("h1").first().text();
        const addr = (page.match(/Manzil:(?:&nbsp;)?<\/b>\s*(?:<p>)?\s*([^<]+)/i) || [])[1];
        const hours = (page.match(/Ish tartibi:(?:&nbsp;)?<\/b>\s*([^<]+)/i) || [])[1];
        out.push(item("MKBank", name, addr, hours, m?.[2], m?.[1], "mkbank.uz"));
      } catch {
        // skip one failing page
      }
      await sleep(400);
    }
    return out;
  }
});

/** Runs every importer; returns { branches, report } where report lists per-bank counts or errors. */
export async function fetchOfficialBranches() {
  const report = {};
  const all = [];
  for (const [bank, run] of Object.entries(importers)) {
    try {
      const got = (await run()).filter(Boolean);
      // De-duplicate identical coordinates within a bank.
      const seen = new Set();
      const uniq = got.filter((b) => {
        const k = `${b.lat.toFixed(5)},${b.lng.toFixed(5)}`;
        if (seen.has(k)) return false;
        seen.add(k);
        return true;
      });
      all.push(...uniq);
      report[bank] = { count: uniq.length, withHours: uniq.filter((b) => b.hours).length };
    } catch (error) {
      report[bank] = { error: error instanceof Error ? error.message : String(error) };
    }
  }
  return { branches: all, report };
}
