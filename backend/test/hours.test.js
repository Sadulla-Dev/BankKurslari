// Opening hours as the banks write them on their own sites (src/officialBranches.js) -> the OSM-style text the apps
// read. Every case is a string a bank actually published. Run: npm test
import { test } from "node:test";
import assert from "node:assert/strict";
import { normalizeHours } from "../src/officialBranches.js";

const cases = [
  // Ipoteka bank: "Cб" with a Latin C used to read as Sunday only.
  [["Пн-ПТ: 9:00-17:00", "Cб-Вс: 9:00-17:00"], "Mo-Fr 09:00-17:00; Sa-Su 09:00-17:00"],
  // Hamkorbank: the Saturday rule after a comma, sometimes with a Latin C.
  ["Пон-Пят 8:00-20:00, Суб: 9:00-13:00\nВыходные Воскресенье", "Mo-Fr 08:00-20:00; Sa 09:00-13:00"],
  ["Пон-Пят 9:00-17:00, Cуб: 9:00-13:00 \nВыходные Воскресенье", "Mo-Fr 09:00-17:00; Sa 09:00-13:00"],
  ["Пон-Пят 9:00-17:00 \nВыходные Суббота, Воскресенье", "Mo-Fr 09:00-17:00"],
  // Trastbank: days off on their own line, a typo, lunch and days off inside one line, days and time on two lines.
  ["9:00 dan 18:00 gacha\r\nDam olish kunlari: shanba, yakshanba.", "Mo-Fr 09:00-18:00"],
  ["9:00 dan 18:00 gacha \r\nDam olish kunlari: shanba, yaskhanba", "Mo-Fr 09:00-18:00"],
  ["9:00 dan 18:00 gacha Tushlik: 13:00 dan 14:00 gacha Dam olish kunlari: shanba, yakshanba", "Mo-Fr 09:00-18:00"],
  ["Dushanba-juma kunlari \r\nsoat 09:00 dan 18:00 gacha.", "Mo-Fr 09:00-18:00"],
  ["9:00 dan 18:00 gacha / Dam olish kunlari: dushanba, shanba, yakshanba.".replace(" / ", "\n"), "Tu-Fr 09:00-18:00"],
  ["9.00-18.00", null], // no days at all: unknown, the apps show the bank's usual hours
  // Garant bank: a dash for the colon.
  ["Dushanba – Juma 09-00-17:00", "Mo-Fr 09:00-17:00"],
  ["Dushanba – Juma 09-00 -17:00", "Mo-Fr 09:00-17:00"],
  // Anorbank: working days and the weekend as words.
  [["Ish kunlari — 09:00 dan 18:00 gacha", "Dam olish kunlari — 09:00 dan 17:00 gacha"], "Mo-Fr 09:00-18:00; Sa-Su 09:00-17:00"],
  // Hayot Bank: the days off inside brackets.
  ["c 9:00 до 18:00 (сб и вс выходные)", "Mo-Fr 09:00-18:00"],
  ["Без выходных 24/7", "24/7"],
  // BRB, Asia Alliance, Turon (Cyrillic "а" in "Jumа"), Xalq banki, MKBank (lunch after a comma).
  ["Dushanba - Juma: 9:00 - 17:00\nShanba:  9:00 - 16:00", "Mo-Fr 09:00-17:00; Sa 09:00-16:00"],
  ["Ish kunlari: пн-сб Ish vaqti: 09:00 - 17:00", "Mo-Sa 09:00-17:00"],
  ["Dushanba-Jumа: 9:00-18:00", "Mo-Fr 09:00-18:00"],
  ["Dush - Juma 09:00 dan 18:00 gacha, tushlik 13:00 dan 14:00 gacha", "Mo-Fr 09:00-18:00"],
  ["Dushanba-Juma 09:00-18:00, Tushlik 13:00-14:00", "Mo-Fr 09:00-18:00"],
  ["Dushanba-Yakshanba 10:00-22:00", "Mo-Su 10:00-22:00"],
  // A list of days stays one rule.
  ["Пн, Вт, Ср 9:00-18:00", "Mo-We 09:00-18:00"],
];

for (const [raw, want] of cases) {
  test(JSON.stringify(raw), () => assert.equal(normalizeHours(raw), want));
}
