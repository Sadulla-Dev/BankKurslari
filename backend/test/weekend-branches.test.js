// The banks' weekend lists (src/weekendLists.js) and matching them to the map's points (src/weekendBranches.js).
// Texts and files are what the banks published for 21–22 November 2026. Run: npm test
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { datesIn, hoursIn, dayWords, tableEntries, proseEntries, pageContext, pdfGrid, htmlEntries, entriesOf, readBankList } from "../src/weekendLists.js";
import { matchEntries, tokens } from "../src/weekendBranches.js";
import { readXlsx } from "../src/xlsx.js";

const DAYS = ["2026-11-21", "2026-11-22"];
const fixture = (f) => readFileSync(new URL(`./fixtures/weekend/${f}`, import.meta.url));
const ctx = (open = ["Sa", "Su"], hours = "") => ({ days: DAYS, open: new Set(open), hours });
const brief = (entries) => entries.map((e) => [e.name, e.address, e.open]);

/* ---------- dates, hours, days ---------- */

test("dates as the banks write them", () => {
  const d = (s) => [...datesIn(s, DAYS[0])].sort();
  assert.deepEqual(d("21-22-noyabr kunlari"), ["2026-11-21", "2026-11-22"]);
  assert.deepEqual(d("2026-yil 21 va 22-Noyabr kunlari"), ["2026-11-21", "2026-11-22"]);
  assert.deepEqual(d("shanba kunlari - 2026 yil 7, 14, 21 va 28-noyabr sanalarida"), ["2026-11-07", "2026-11-14", "2026-11-21", "2026-11-28"]);
  assert.deepEqual(d("Шанба (21.11.26)"), ["2026-11-21"]);
  assert.deepEqual(d("Ish vaqti 11/21/2026"), ["2026-11-21"]); // Hamkorbank: month first
  assert.deepEqual(d("21/11/2026"), ["2026-11-21"]);
  assert.deepEqual(d("2026 йил 21-22 ноябр кунлари"), ["2026-11-21", "2026-11-22"]);
  assert.deepEqual(d("в субботу, 21 ноября"), ["2026-11-21"]);
  // "since 3 August" is when a schedule started, not a day it is about.
  assert.deepEqual(d("2026-yil 3-avgustdan boshlab"), []);
  assert.deepEqual(d("2026-yil 31-oktyabrdan boshlab va 1, 7, 8, 14, 15, 21, 22, 28, 29-noyabr kunlari").filter((x) => x >= "2026-11-21" && x <= "2026-11-22"), DAYS);
});

test("opening hours as the banks write them", () => {
  assert.equal(hoursIn("9-00dan-16-00gacha"), "09:00–16:00"); // BRB
  assert.equal(hoursIn("09:00 dan 13:00 gacha"), "09:00–13:00");
  assert.equal(hoursIn("10:00 - 16:00"), "10:00–16:00");
  assert.equal(hoursIn("00:00-07:00\n08:00-00:00"), "00:00–07:00, 08:00–24:00"); // Ipoteka, a kassa open around the clock but one hour
  assert.equal(hoursIn("soat 09:00 dan 16:00 gacha (Tushlik vaqti 13:00-14:00)"), "09:00–16:00");
  assert.equal(hoursIn("Ish vaqti: 9.00dan 17.00gacha"), "09:00–17:00");
  assert.equal(hoursIn("kunu-tun, siz uchun qulay istalgan vaqtda"), "00:00–24:00");
  assert.equal(hoursIn("21.11.2026"), null); // a date is not a time
  assert.equal(hoursIn("Ishlaydi"), null);
});

test("Saturday and Sunday, typos included, weekdays not", () => {
  const w = (s) => [...dayWords(s)].sort().join(",");
  assert.equal(w("Shanba, Yakshanba"), "Sa,Su");
  assert.equal(w("Shanba, Yaskshanba"), "Sa,Su"); // Agrobank
  assert.equal(w("Şanba"), "Sa"); // the Central Bank's new Latin
  assert.equal(w("ЯКШАНБА"), "Su");
  assert.equal(w("Payshanba BXM"), ""); // Thursday
  assert.equal(w("Dushanba – Juma"), "");
});

test("page defaults: the weekend's days and hours, not the posting date or the weekday schedule", () => {
  const c = pageContext("20.11.2026\nMijozlar uchun 21-noyabr kuni soat 09:00 dan 17:00 gacha xizmat ko'rsatamiz.", DAYS);
  assert.deepEqual([...c.open], ["Sa"]);
  assert.equal(c.hours, "09:00–17:00");
  assert.equal(c.stale, false);
  assert.equal(pageContext("ISH GRAFIGI: Dushanba — juma: 9:00 dan 17:00 gacha\nShanba kunlari ochiq bo'lgan filiallar", DAYS).hours, "");
  assert.equal(pageContext("2026-yil 28 noyabr kuni valyuta ayirboshlash shoxobchasi ishlaydi", DAYS).stale, true); // Saderat, a week early
  assert.equal(pageContext("05.11.2026 10:35\nShanba kuni ham odatdagi tartibda", DAYS).standing, true); // Universal
});

/* ---------- tables ---------- */

test("Agrobank: a table with region rows and an 'Ish kuni' column", () => {
  const grid = [
    ["№", "Filial nomi", "Manzil", "Ish kuni"],
    ["Andijon", "Andijon", "Andijon", "Andijon"],
    ["1", "Ulug‘nor filiali", "Ulug‘nor tumani, Mustaqillik ko‘chasi, 1-uy, 170900", "Shanba"],
    ["2", "Paxtaobod filiali", "Paxtaobod tumani, Paxtaobod shahri, A.Temur ko‘chasi, 8-uy, 171303", "Shanba, Yakshanba"],
    ["Samarqand", "Samarqand", "Samarqand", "Samarqand"],
    ["7.0", "Siyob filiali (Aeraportda)", "Ibn Sino ko‘chasi, 1-uy", "Shanba, Yaskshanba"],
  ];
  const { entries } = tableEntries(grid, ctx());
  assert.deepEqual(entries.map((e) => [e.name, e.region, e.open]), [
    ["Ulug‘nor filiali", "Andijon", { Sa: "" }],
    ["Paxtaobod filiali", "Andijon", { Sa: "", Su: "" }],
    ["Siyob filiali (Aeraportda)", "Samarqand", { Sa: "", Su: "" }],
  ]);
});

test("Xalq banki: no header row, columns told by their contents", () => {
  const grid = [
    ["MA’LUMOT", "MA’LUMOT", "MA’LUMOT", "MA’LUMOT", "MA’LUMOT"],
    ["1", "Andijon viloyati", "Xo'jaobod BXM", "171400, Xo‘jaobod tumani, Xo‘jaobod shahri, Uzun MFY, Uzun ko‘chasi, 5-uy", "Shanba, Yakshanba"],
    ["2", "Andijon viloyati", "Xonobod BXM", "170500, Xonobod shahri, Istiqlol MFY, Farog‘at ko‘chasi, 3-uy", "Shanba"],
    ["3", "Buxoro viloyati", "Payshanba BXM", "Payshanba shahri, A. Navoiy ko‘chasi, 4A-uy", "Yakshanba"],
  ];
  const { entries } = tableEntries(grid, ctx());
  assert.equal(tableEntries(grid.map((r) => r.filter((_, i) => i !== 1)), ctx()).entries[0].region, ""); // the title is no region
  assert.deepEqual(entries.map((e) => [e.name, e.region, e.open]), [
    ["Xo'jaobod BXM", "Andijon viloyati", { Sa: "", Su: "" }],
    ["Xonobod BXM", "Andijon viloyati", { Sa: "" }],
    ["Payshanba BXM", "Buxoro viloyati", { Su: "" }],
  ]);
});

test("Turonbank: names and a date column; Infinbank: a table for another weekend is stale", () => {
  const turon = tableEntries([["№", "BXM nomi", "21.11.2026"], ["1", "Xorazm", "Ishlaydi"], ["2", "Navoiy", "Ishlamaydi"]], ctx(["Sa"], "09:00–17:00"));
  assert.deepEqual(brief(turon.entries), [["Xorazm", "", { Sa: "09:00–17:00" }]]);
  const infin = tableEntries([["№", "Bo'linma nomi", "Valyuta ayirboshlash", "Хalqaro pul o‘tkazmalari"], ["1", "\"Mirobod\" BXM", "28.11.2026", "28.11.2026"]], ctx());
  assert.deepEqual(infin, { entries: [], stale: true });
  const infinNow = tableEntries([["№", "Bo'linma nomi", "Valyuta ayirboshlash", "Хalqaro pul o‘tkazmalari"], ["1", "\"Mirobod\" BXM", "21.11.2026", "21.11.2026"], ["2", "Zangiota filiali", "22.11.2026", ""]], ctx([], "09:00–15:00"));
  assert.deepEqual(brief(infinNow.entries), [["\"Mirobod\" BXM", "", { Sa: "09:00–15:00" }], ["Zangiota filiali", "", { Su: "09:00–15:00" }]]);
});

test("SQB: one table for Saturday, one for Sunday, an office in both is one entry", () => {
  const table = (rows) => `<table><tr><td>№</td><td>Biriktirilgan tarmoq</td><td>Viloyat</td><td>Tuman</td><td>Manzil</td></tr>${rows.map((r) => `<tr>${r.map((c) => `<td>${c}</td>`).join("")}</tr>`).join("")}</table>`;
  const html = `<div class="generalInfo"><p>ISH GRAFIGI: Dushanba — juma: 9:00 dan 17:00 gacha</p>
    <div><div class="t">Shanba kunlari ochiq bo'lgan filiallar ro'yxati</div><div>${table([["1", "Chilonzor BXM", "Toshkent shahri", "Chilonzor tumani", "100097 Toshkent shahar, Chilonzor-S mavzesi, Navbahor MFY, Mehr ko‘chasi, 11a-uy."], ["2", "Rakat BXM", "Toshkent shahri", "Yakkasaroy tumani", "100100 Toshkent shahar, Yakkasaroy tumani, Belariq MFY, Shohjaxon ko‘chasi, 2a-uy."]])}</div></div>
    <div><div class="t">Yakshanba kunlari ochiq bo'lgan filiallar ro'yxati</div><div>${table([["1", "Chilonzor BXM", "Toshkent shahri", "Chilonzor tumani", "100097 Toshkent shahar, Chilonzor-S mavzesi, Navbahor MFY, Mehr ko‘chasi, 11a-uy."]])}</div></div></div>`;
  const { entries } = htmlEntries(html, "https://sqb.uz/uz/contacts/", DAYS, { root: ".generalInfo" });
  // htmlEntries keeps one entry per table row; readBankList merges them (see the next test).
  assert.deepEqual(entries.map((e) => [e.name, e.address.slice(0, 20), e.open]), [
    ["Chilonzor BXM", "100097 Toshkent shah", { Sa: "" }],
    ["Rakat BXM", "100100 Toshkent shah", { Sa: "" }],
    ["Chilonzor BXM", "100097 Toshkent shah", { Su: "" }],
  ]);
});

test("readBankList: the page, then its files; merged per office; a bot check is reported, not worked around", async () => {
  const pages = {
    "https://www.ipotekabank.uz/blog/news/x/": `<div class="blog-detail"><p>Dam olish kunlari ishlaydigan kassalar</p><a href="/upload/grafik-news/21-22-november-2026.xlsx">xlsx</a></div>`,
    "https://www.ipotekabank.uz/upload/grafik-news/21-22-november-2026.xlsx": fixture("ipoteka-2026-11-21.xlsx"),
    "https://hamkorbank.uz/x.pdf": "<html><head><title>Just a moment...</title></head><body>cf-chl</body></html>",
  };
  const get = async (url) => {
    const v = pages[url];
    if (v == null) throw new Error("HTTP 404");
    return { buf: Buffer.isBuffer(v) ? v : Buffer.from(v), type: Buffer.isBuffer(v) ? "application/octet-stream" : "text/html", url };
  };
  const ipoteka = await readBankList({ name: "Ipoteka-bank", url: "https://www.ipotekabank.uz/blog/news/x/" }, DAYS, { get });
  assert.equal(ipoteka.status, "ok");
  assert.equal(ipoteka.bank, "Ipoteka bank");
  assert.equal(ipoteka.entries.length, 32);
  assert.deepEqual(ipoteka.read.length, 2);
  const hamkor = await readBankList({ name: "Hamkorbank", url: "https://hamkorbank.uz/x.pdf" }, DAYS, { get });
  assert.equal(hamkor.status, "blocked");
  const missing = await readBankList({ name: "Infinbank", url: "https://www.infinbank.com/uz/press/news/1/" }, DAYS, { get });
  assert.equal(missing.status, "error");
});

/* ---------- xlsx ---------- */

test("Ipoteka bank xlsx: Cyrillic headers, branch and office columns, Saturday and Sunday hours", async () => {
  const r = await entriesOf({ buf: fixture("ipoteka-2026-11-21.xlsx"), type: "", url: "x.xlsx" }, DAYS);
  assert.equal(r.entries.length, 32);
  assert.deepEqual(r.entries[0], { name: "Барака БХМ", parent: "Миробод филиали", address: "Тошкент шаҳар, Бектемир тумани, Нурафшон МФЙ, Қуйлиқ бозор ҳудуди", region: "", open: { Sa: "09:00–18:00", Su: "09:00–18:00" } });
  assert.deepEqual(r.entries[3].open, { Sa: "00:00–07:00, 08:00–24:00" });
  assert.deepEqual(r.entries.find((e) => e.name === "28-касса").open, { Su: "09:00–16:00" });
});

test("BRB xlsx: four Saturdays as columns, only this one counts", () => {
  const grid = readXlsx(fixture("brb-2026-11.xlsx"));
  const { entries } = tableEntries(grid, ctx(["Sa"], "09:00–16:00"));
  assert.equal(entries.length, 19); // the offices with hours under 21.11.2026
  assert.deepEqual(entries[0], { name: "Beruniy BXM", parent: "", address: "", region: "", open: { Sa: "09:00–16:00" } });
  assert.ok(!entries.some((e) => e.name === "Xo'jaobod BXM")); // open on 28 November only
  assert.deepEqual(tableEntries(grid, { ...ctx(), days: ["2026-11-14", "2026-11-15"] }).entries.length, 18);
});

/* ---------- PDF ---------- */

test("Hamkorbank PDF: left-aligned names under centred headers stay in their column", async () => {
  const r = await entriesOf({ buf: fixture("hamkorbank-2026-11-21.pdf"), type: "application/pdf", url: "x.pdf" }, DAYS);
  assert.equal(r.entries.length, 48);
  assert.deepEqual(r.entries[1], { name: "Asaka UBXO", parent: "", address: "Asaka shahar, Karvonsaroy ko'chasi 64-uy", region: "", open: { Sa: "09:00–13:00" } });
  assert.ok(r.entries.every((e) => e.name && e.address && e.open.Sa === "09:00–13:00" && !e.open.Su));
});

test("PDF rows: a number starts a row, merged cells are shared, two offices of one branch are split (Asakabank)", () => {
  const it = (str, x, y, w = 30) => ({ str, x, y, w });
  const pages = [{ width: 842, items: [
    it("№", 35, 490, 8), it("BXM nomi", 116, 489, 40), it("VASH joylashgan manzil", 323, 489, 100), it("SHANBA", 663, 482), it("YAKSHANBA", 730, 474, 45),
    it("21.11.2026", 644, 457, 45), it("22.11.2026", 711, 457, 45),
    it("Toshkent shahar, Sergeli tumani, TXAY ko’chasi", 323, 437, 200), it("10:00 - 16:00", 640, 437, 45), it("10:00 - 15:00", 707, 437, 45),
    it("1", 36, 429, 5), it("Yunusobod", 115, 429, 40),
    it("10:00 - 16:00", 640, 421, 45), it("-", 726, 421, 4), it("Toshkent shahri, Yunusobod tumani, Axmad donish ko'chasi, 34-uy.", 323, 420, 230),
    it("2", 35, 404, 5), it("Nurafshon", 115, 404, 40), it("Toshkent viloyati, Olmaliq tumani, Xonobod ko'chasi 2-uy", 323, 404, 220), it("10:00 - 16:00", 640, 404, 45),
    it("Jami", 315, 262, 20), it("10", 657, 261, 10),
  ] }];
  const grid = pdfGrid(pages);
  const { entries } = tableEntries(grid, ctx());
  assert.deepEqual(brief(entries), [
    ["Yunusobod", "Toshkent shahar, Sergeli tumani, TXAY ko’chasi", { Sa: "10:00–16:00", Su: "10:00–15:00" }],
    ["Yunusobod", "Toshkent shahri, Yunusobod tumani, Axmad donish ko'chasi, 34-uy.", { Sa: "10:00–16:00" }],
    ["Nurafshon", "Toshkent viloyati, Olmaliq tumani, Xonobod ko'chasi 2-uy", { Sa: "10:00–16:00" }],
  ]);
});

/* ---------- text announcements ---------- */

test("Tenge bank: 'Name (address)' with brackets inside, one office on Saturday only", () => {
  const lines = [
    "Universal kassalar:",
    "«BYD AUTO CENTER» (Sergeli tumani, TKAD (Toshkent halqa avtomobil yo‘li), 109-uy)",
    "BXM \"WAY II\" (Toshkent sh., Mahtumquli ko'chasi, 64/2) faqat 2026-yil 21-noyabr kuni faoliyat yuritadi.",
    "Tashrifingizni rejalashtirayotganda ushbu ma'lumotni hisobga olishingizni so'raymiz.",
  ];
  assert.deepEqual(brief(proseEntries(lines, ctx())), [
    ["«BYD AUTO CENTER»", "Sergeli tumani, TKAD (Toshkent halqa avtomobil yo‘li), 109-uy", { Sa: "", Su: "" }],
    ["BXM \"WAY II\"", "Toshkent sh., Mahtumquli ko'chasi, 64/2", { Sa: "" }],
  ]);
});

test("Orient Finans, Hayot, Universal, Apex: the other ways banks write it", () => {
  assert.deepEqual(brief(proseEntries(["«Sergeli» BXO — soat 10:00 dan 16:00 gacha, 21-noyabr kuni;", "City Mall SKMdagi BXO — soat 10:00 dan 22:00 gacha 21 va 22-noyabr kunlari;"], ctx())), [
    ["«Sergeli» BXO", "", { Sa: "10:00–16:00" }],
    ["City Mall SKMdagi BXO", "", { Sa: "10:00–22:00", Su: "10:00–22:00" }],
  ]);
  assert.deepEqual(brief(proseEntries(["“O’zbekiston” mehmonxonasi manzili: Toshkent shahri, Mahtumquli ko‘chasi, 45"], ctx(["Sa", "Su"], "00:00–24:00"))), [
    ["“O’zbekiston” mehmonxonasi", "Toshkent shahri, Mahtumquli ko‘chasi, 45", { Sa: "00:00–24:00", Su: "00:00–24:00" }],
  ]);
  assert.deepEqual(brief(proseEntries(["📍 Toshkent shahri, Mirobod tumani, Afrosiyob ko‘chasi, 4-uy hududi.", "📆 Ish vaqti: odatdagi tartibda."], ctx(["Sa"]))), [
    ["", "Toshkent shahri, Mirobod tumani, Afrosiyob ko‘chasi, 4-uy hududi", { Sa: "" }],
  ]);
  assert.deepEqual(brief(proseEntries([
    "Офис продаж \"MEGA\"", "г. Ташкент, ул. Янгишахар, 2B", "График работы: Пн–Вс с 10:00 до 22:00 (без перерыва)",
    "Офис продаж \"YUNUSABAD GALLERY\"", "г. Ташкент, Юнусабадский район, 4-квартал, дом 84", "График работы: Пн–Пт с 10:00 до 22:00", "Сб–Вс с 10:00 до 23:00 (без перерыва)",
  ], ctx())), [
    ["Офис продаж \"MEGA\"", "г. Ташкент, ул. Янгишахар, 2B", { Sa: "10:00–22:00", Su: "10:00–22:00" }],
    ["Офис продаж \"YUNUSABAD GALLERY\"", "г. Ташкент, Юнусабадский район, 4-квартал, дом 84", { Sa: "10:00–23:00", Su: "10:00–23:00" }],
  ]);
  // Poytaxt: one office named in a sentence.
  assert.deepEqual(brief(proseEntries(["Hurmatli mijozlar, 21-noyabr kuni Mirobod bank xizmatlari markazi siz uchun faoliyat yuritadi.", "Sizni 09:00 dan 17:00 gacha kutamiz."], ctx(["Sa"], "09:00–17:00"))), [
    ["Mirobod bank xizmatlari markazi", "", { Sa: "09:00–17:00" }],
  ]);
});

/* ---------- matching to the map's points ---------- */

const pt = (name, address, lat, lng, source = "bank.uz") => ({ bank: "Hamkorbank", name, address, lat, lng, source });

test("address tokens: Cyrillic and Latin spellings meet, house numbers and streets are read", () => {
  const a = tokens("Тошкент шаҳар, Юнусобод тумани, Зиёлилар кўчаси, 1Б-уй");
  const b = tokens("Toshkent shahri, Yunusobod tumani, Ziyolilar ko‘chasi, 1b");
  assert.deepEqual([...a.streets], ["ziyolilar"]);
  assert.equal(a.house, "1b");
  assert.deepEqual([...b.streets], ["ziyolilar"]);
  assert.equal(b.house, "1b");
  assert.ok(a.places.has("yunusobod") && b.places.has("yunusobod"));
  assert.deepEqual([...tokens("Shahrixon tumani, ko‘ch. Hamza, 2").streets], ["hamza"]); // bank.uz's word order
  assert.equal(tokens("Sergeli tumani, TKAD, 68-uy").ring, "outer");
  assert.equal(tokens("Yakkasaroy tumani, Kichik Halqa Yo'li, 22-uy").ring, "small");
});

test("a match needs the same street and house, or the office's name; else no point", () => {
  const points = [
    pt("Yunusobod universal BXO", "Toshkent shahri, Yunusobod tumani, Yangi shahar ko'chasi, 59a-uy", 41.36, 69.29, "hamkorbank.uz"),
    pt("Universal kassa №1", "Toshkent shahri, Yunusobod tumani, Amir Temur shoh ko‘chasi, 77A", 41.35, 69.28),
    pt("Universal kassa №5", "Toshkent sh., Yakkasaroy tumani, Kichik Halqa Yo'li, 22-uy", 41.29, 69.25),
    pt("Chust universal BXO", "Namangan viloyati, Chust tumani, Serob MFY, Istiqlol ko'chasi, 21-uy", 41.0, 71.23),
    pt("Qo'qon universal BXO", "Farg'ona viloyati, Qo'qon shahri, Istiqlol ko'chasi, 21-uy", 40.53, 70.94),
  ];
  const m = (e) => matchEntries([{ open: { Sa: "" }, parent: "", region: "", ...e }], points)[0].point;
  assert.deepEqual(m({ name: "Yunusobod UBXO", address: "Toshkent shahar Yangi shahar ko'chasi, 59A uy" }), { lat: 41.36, lng: 69.29 });
  assert.equal(m({ name: "«ASTANA MOTORS»", address: "Yunusobod tumani, Amir Temur shoh ko‘chasi, 23-uy" }), null); // same street, another house
  assert.equal(m({ name: "«ASTANA MOTORS»", address: "Yakkasaroy tumani, TKAD (Toshkent halqa avtomobil yo‘li), 22-uy" }), null); // another ring road
  assert.deepEqual(m({ name: "Chust UBXO", address: "Chust shahar Istiqlol ko'cha 21-uy" }), { lat: 41.0, lng: 71.23 }); // not Qo‘qon's Istiqlol 21
  assert.equal(m({ name: "", address: "Andijon shahar, Istiqlol ko'chasi 21-uy" }), null); // another town
});

test("the same branch listed twice far apart: the bank's own point wins; a bare name must be in the point's name", () => {
  const points = [
    pt("“Hamkorbank”ning Farg‘ona viloyat filiali", "150100, Farg‘ona viloyati, Farg‘ona sh. Murabbiylar, 41", 40.3864, 71.7763),
    pt("Farg'ona mintaqaviy BXO", "Farg'ona viloyati, Farg'ona shahri, Murabbiylar ko'chasi, 41-uy", 40.3917, 71.7823, "hamkorbank.uz"),
    pt("Zangiota bank xizmatlari markazi", "Toshkent viloyati, Zangiota tumani, Mustaqillik ko'chasi", 41.2, 69.1),
    pt("Xorazm bank xizmatlari markazi", "Xorazm viloyati, Urganch shahri, Xonqa ko‘chasi, 104a-uy", 41.55, 60.63),
    pt("Turonbank Xorazm filiali", "Xorazm viloyati, Urganch shahri, Al-Xorazmiy ko‘chasi, 1", 41.56, 60.62),
    // The rest of the bank's points: how rare a word is depends on them.
    ...Array.from({ length: 40 }, (_, i) => pt(`BXM ${i}`, `Toshkent shahri, Chilonzor tumani, Bunyodkor ko‘chasi, ${i + 100}-uy`, 41.3 + i / 1000, 69.2)),
  ];
  const m = (e) => matchEntries([{ open: { Sa: "" }, parent: "", region: "", address: "", ...e }], points)[0].point;
  assert.deepEqual(m({ name: "Fargʻona MBXO", address: "Fargʻona shahar, Murabbiylar koʻchasi 41-uy" }), { lat: 40.3917, lng: 71.7823 });
  assert.deepEqual(m({ name: "Zangiota" }), { lat: 41.2, lng: 69.1 });
  assert.equal(m({ name: "Xorazm" }), null); // two points carry the name
  assert.equal(m({ name: "Toshkent shahar" }), null);
});
