// Weekend data that must never claim more than the banks say (see officialBranches.js WEEKEND_FROM_WEEKLY_LIST_ONLY
// and weekend.js). Run: npm test
import { test } from "node:test";
import assert from "node:assert/strict";
import { normalizeHours, weekdaysOnly } from "../src/officialBranches.js";
import { weekendAfter } from "../src/weekend.js";

test("weekdaysOnly drops Saturday and Sunday", () => {
  assert.equal(weekdaysOnly("Mo-Fr 09:00-17:00; Sa-Su 09:00-17:00"), "Mo-Fr 09:00-17:00"); // Ipoteka bank
  assert.equal(weekdaysOnly("Mo-Fr 09:00-17:00; Sa 09:00-16:00"), "Mo-Fr 09:00-17:00"); // BRB
  assert.equal(weekdaysOnly("Mo-Sa 09:00-17:00"), "Mo-Fr 09:00-17:00");
  assert.equal(weekdaysOnly("Mo-Su 10:00-22:00"), "Mo-Fr 10:00-22:00");
  assert.equal(weekdaysOnly("Sa-Su 09:00-13:00"), null);
  assert.equal(weekdaysOnly("24/7"), "Mo-Fr 00:00-24:00");
  assert.equal(weekdaysOnly(null), null);
});

test("24/7 on named days only stays on those days", () => {
  assert.equal(normalizeHours("Dush - Juma 24/7"), "Mo-Fr 00:00-24:00"); // Xalq banki, Olot BXM
  assert.equal(normalizeHours("Без выходных 24/7"), "24/7"); // Hayot Bank exchange points
  assert.equal(normalizeHours("24/7"), "24/7");
});

test("the weekend a Central Bank list is for", () => {
  assert.deepEqual(weekendAfter("2026-11-20T14:03:10+00:00", "Dam-oliş-kunlari"), ["2026-11-21", "2026-11-22"]); // Friday
  assert.deepEqual(weekendAfter("2026-11-19T15:00:00+00:00", "Dam-oliş-kunlari"), ["2026-11-21", "2026-11-22"]); // Thursday
  assert.deepEqual(weekendAfter("2026-11-21T05:00:00+00:00", "Dam-oliş-kunlari"), ["2026-11-21", "2026-11-22"]); // Saturday
  assert.deepEqual(weekendAfter("2026-11-22T05:00:00+00:00", "Dam-oliş-kunlari"), ["2026-11-21", "2026-11-22"]); // Sunday
  assert.deepEqual(weekendAfter("2026-11-20T20:30:00+00:00", "Dam-oliş-kunlari"), ["2026-11-21", "2026-11-22"]); // Saturday 01:30 Tashkent
  assert.equal(weekendAfter("2026-11-17T10:00:00+00:00", "Dam-oliş-kunlari"), null); // Tuesday: not guessed
  assert.equal(weekendAfter("2026-11-20T14:00:00+00:00", "Bayram-kunlari"), null); // holiday list: not guessed
});
