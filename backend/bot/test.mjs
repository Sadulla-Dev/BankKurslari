// node test.mjs: checks what the bot reads from messages and what it answers, without Telegram or a Worker.
import assert from "node:assert/strict";
import * as L from "./lib.js";

const p = L.parse;
const eq = (text, want) => {
  const got = p(text);
  try { assert.deepEqual(got && Object.fromEntries(Object.keys(want || {}).map((k) => [k, got[k]])), want); }
  catch { console.error(`✗ ${JSON.stringify(text)} → ${JSON.stringify(got)}, want ${JSON.stringify(want)}`); process.exitCode = 1; return; }
  console.log(`✓ ${text}`);
};

eq("500 dollar", { kind: "rate", cur: "USD", dir: "sell", amount: 500, inSom: false });
eq("500$", { kind: "rate", cur: "USD", amount: 500 });
eq("1 000 dollar sotib olaman", { kind: "rate", cur: "USD", dir: "buy", amount: 1000 });
eq("1,000 usd", { kind: "rate", cur: "USD", amount: 1000 });
eq("1.5 ming dollar sotaman", { kind: "rate", cur: "USD", dir: "sell", amount: 1500 });
eq("100 yevro", { kind: "rate", cur: "EUR", amount: 100 });
eq("50000 rubl", { kind: "rate", cur: "RUB", amount: 50000 });
eq("купить 300 долларов", { kind: "rate", cur: "USD", dir: "buy", amount: 300 });
eq("продать 200 евро", { kind: "rate", cur: "EUR", dir: "sell", amount: 200 });
eq("100 dollar necha so'm", { kind: "rate", cur: "USD", dir: "sell", amount: 100, inSom: false });
eq("5 mln so‘m dollar", { kind: "rate", cur: "USD", dir: "buy", amount: 5_000_000, inSom: true });
eq("1 000 000 сум в долларах", { kind: "rate", cur: "USD", amount: 1_000_000, inSom: true });
eq("500 руб", { kind: "rate", cur: "RUB", amount: 500 });
eq("сумма 300 долларов", { kind: "rate", cur: "USD", amount: 300, inSom: false });
eq("dollar", { kind: "rate", cur: "USD", amount: 100 });
eq("dollar 12 100 dan oshsa", { kind: "alert", cur: "USD", side: "SELL", threshold: 12100 });
eq("dollar 11900 dan tushsa", { kind: "alert", cur: "USD", side: "BUY", threshold: 11900 });
eq("доллар выше 12100", { kind: "alert", cur: "USD", side: "SELL", threshold: 12100 });
eq("kurs", { kind: "summary" });
eq("Assalomu alaykum", { kind: "summary" });
eq("12345", { kind: "number", amount: 12345 });
eq("🔔 Ogohlantirish", { kind: "alerts" });
eq("🔔 Уведомления", { kind: "alerts" });
eq("💵 Dollar", { kind: "rate", cur: "USD", dir: "sell", dirGiven: false, amount: 100 });
eq("📊 Bugungi kurslar", { kind: "summary" });
eq("📊 Курсы сегодня", { kind: "summary" });
eq("rahmat", null);

const data = {
  updatedAt: "2026-11-24T10:05:00Z",
  cbu: { rate: 11971.64 },
  banks: [
    { bank: "Anorbank", buy: 11960, sell: 12010 },
    { bank: "Orient Finans Bank", buy: 11940, sell: 12000 },
    { bank: "Xalq banki", buy: 11900, sell: 12050 },
    { bank: "Kapitalbank", buy: 0, sell: 12020 }
  ]
};
assert.equal(L.rank(data, "sell")[0].bank, "Anorbank");
assert.equal(L.rank(data, "buy")[0].bank, "Orient Finans Bank");
assert.equal(L.rank(data, "sell").length, 3);
const m = L.rateMessage("uz", data, { cur: "USD", dir: "sell", amount: 500, inSom: false });
assert.match(m.text, /5 980 000 so‘m/);
assert.match(m.text, /Yangilandi: 24\.11, 15:05/);
assert.match(m.text, /farqi: <b>30 000 so‘m/);
const r = L.rateMessage("ru", data, { cur: "USD", dir: "buy", amount: 6_000_000, inSom: true });
assert.match(r.text, /500 USD/);
for (const row of m.reply_markup.inline_keyboard) for (const b of row) if (b.callback_data) assert.ok(Buffer.byteLength(b.callback_data) <= 64);
assert.deepEqual(L.uncb(L.cb({ cur: "EUR", dir: "buy", amount: 1500, inSom: false })), { kind: "rate", cur: "EUR", dir: "buy", amount: 1500, inSom: false });
assert.equal(L.uncb("r|XXX|s|1|0"), null);
assert.equal(L.siteUrl("uz", "USD", 500), "https://qulaykurs.uz/500-dollar-necha-som");
assert.equal(L.siteUrl("ru", "EUR", 777), "https://qulaykurs.uz/ru/kurs-evro");
assert.equal(L.inlineResults("uz", data, { cur: "USD", dir: "sell", amount: 100, inSom: false }).length, 2);
assert.equal(L.alertValue(data, "SELL").value, 11960);
assert.equal(L.alertValue(data, "BUY").value, 12000);
assert.ok(L.alertMet("SELL", 12100, 12100) && !L.alertMet("BUY", 12001, 12000));
assert.equal(L.moveMessage("USD", { buy: 11960, sell: 12010 }, { buy: 11990, sell: 12030, buyBank: "A", sellBank: "B" }, 11971), null);
assert.match(L.moveMessage("USD", { buy: 11960, sell: 12010 }, { buy: 12030, sell: 12080, buyBank: "Anorbank", sellBank: "B" }, 11971), /qimmatladi[\s\S]*\+70/);
assert.equal(L.money(26.1), "26,10");
// Buttons: every callback fits Telegram's 64 bytes, the marked amount and direction match the request.
const kb = L.rateButtons("uz", { cur: "RUB", dir: "buy", amount: 50000, inSom: false }).inline_keyboard;
assert.ok(kb[0].some((b) => b.text === "✅ 50 000"));
assert.ok(kb[1][1].text.startsWith("✅") && !kb[1][0].text.startsWith("✅"));
for (const menu of [L.alertCurMenu("uz"), L.alertSideMenu("ru", "USD", data), L.alertValueMenu("uz", "USD", "SELL", data), L.rateMessage("uz", data, { cur: "USD", dir: "sell", amount: 10000, inSom: false })])
  for (const row of menu.reply_markup.inline_keyboard) for (const b of row) if (b.callback_data) assert.ok(Buffer.byteLength(b.callback_data) <= 64, b.callback_data);
assert.deepEqual(L.alertChoices("USD", "BUY", 12000), [11980, 11950, 11900, 11800]);
assert.equal(L.mainKeyboard("ru").keyboard[1][1].text, "🔔 Уведомления");
console.log(process.exitCode ? "\nFAILED" : "\nall good");
