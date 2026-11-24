// bot.qulaykurs.uz: the @qulaykursbot Telegram bot, anonymous usage counts, and hourly checks.
//
//   POST /tg       Telegram webhook (header X-Telegram-Bot-Api-Secret-Token must equal WEBHOOK_SECRET)
//   POST /e        usage counts from the app and the site: {"p":"android","e":["open","route"]}, no ids, no IPs kept
//   GET  /health
//   cron           rate alerts the bot users set, and a channel post when rates move far during the day
//
// Kept apart from api.qulaykurs.uz on purpose: that Worker is redeployed every hour by CI and must never break.
// Rates are read from the public API like any other client. Setup: see README.md in this folder.
import * as L from "./lib.js";

const CORS = { "access-control-allow-origin": "*", "access-control-allow-methods": "POST, OPTIONS", "access-control-allow-headers": "content-type" };
const PLATFORMS = ["android", "ios", "web"];

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { headers: CORS });
    if (url.pathname === "/health") return new Response("ok");
    if (url.pathname === "/tg" && request.method === "POST") {
      if (!env.WEBHOOK_SECRET || request.headers.get("x-telegram-bot-api-secret-token") !== env.WEBHOOK_SECRET) return new Response("forbidden", { status: 403 });
      const update = await request.json().catch(() => null);
      // Answer Telegram at once; a slow rates fetch must not make it resend the update.
      if (update) ctx.waitUntil(onUpdate(env, update).catch((e) => console.error("update failed", e?.stack || e)));
      return new Response("ok");
    }
    if (url.pathname === "/e" && request.method === "POST") return countEvents(env, request);
    return new Response("not found", { status: 404 });
  },

  async scheduled(event, env, ctx) {
    const all = await allRates(env);
    if (!Object.keys(all).length) return;
    await checkAlerts(env, all).catch((e) => console.error("alerts failed", e?.stack || e));
    await checkMoves(env, all).catch((e) => console.error("moves failed", e?.stack || e));
  }
};

/* ---------- data ---------- */

async function rates(env, cur) {
  const api = (env.RATES_API || "https://api.qulaykurs.uz").replace(/\/$/, "");
  const res = await fetch(`${api}/api/rates/${cur.toLowerCase()}`, { cf: { cacheTtl: 120, cacheEverything: true } });
  if (!res.ok) throw new Error(`rates ${cur}: HTTP ${res.status}`);
  const data = await res.json();
  if (!(data?.banks?.length > 0)) throw new Error(`rates ${cur}: no banks`);
  return data;
}

async function allRates(env) {
  const out = {};
  await Promise.all(L.CURRENCIES.map(async (c) => { out[c] = await rates(env, c).catch((e) => (console.error(e.message), null)); }));
  for (const c of L.CURRENCIES) if (!out[c]) delete out[c];
  return out;
}

async function tg(env, method, body) {
  const api = (env.TG_API || "https://api.telegram.org").replace(/\/$/, "");
  const res = await fetch(`${api}/bot${env.TELEGRAM_BOT_TOKEN}/${method}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  });
  const out = await res.json().catch(() => ({ ok: false, description: `HTTP ${res.status}` }));
  if (!out.ok) console.error(`${method}: ${out.error_code || res.status} ${out.description}`);
  return out;
}

const send = (env, chat_id, msg) =>
  tg(env, "sendMessage", { chat_id, text: msg.text, parse_mode: "HTML", link_preview_options: { is_disabled: true }, ...(msg.reply_markup ? { reply_markup: msg.reply_markup } : {}) });

/* ---------- updates ---------- */

async function onUpdate(env, u) {
  if (u.message) return onMessage(env, u.message);
  if (u.callback_query) return onCallback(env, u.callback_query);
  if (u.inline_query) return onInline(env, u.inline_query);
}

async function remember(env, from, source) {
  const now = new Date().toISOString();
  await env.DB.prepare(
    "INSERT INTO users (chat_id, lang, first_seen, last_seen, source) VALUES (?1, ?2, ?3, ?3, ?4) ON CONFLICT(chat_id) DO UPDATE SET last_seen = ?3, lang = ?2"
  ).bind(from.id, L.lang(from.language_code), now, source || null).run();
}

async function onMessage(env, m) {
  const text = m.text || "";
  // In groups the bot only answers its own commands; private chats get everything.
  if (m.chat.type !== "private" && !/^\/\w+@qulaykursbot\b/i.test(text)) return;
  const l = L.lang(m.from?.language_code);
  const t = L.T[l];
  const chat = m.chat.id;
  const cmd = (text.match(/^\/(\w+)/) || [])[1]?.toLowerCase();
  if (m.chat.type === "private" && m.from) await remember(env, m.from, cmd === "start" ? text.split(/\s+/)[1] || "start" : null);

  if (cmd === "start" || cmd === "help" || cmd === "yordam") {
    if (cmd === "start") await countDirect(env, "bot_start", "telegram");
    const all = await allRates(env);
    if (Object.keys(all).length) await send(env, chat, L.summaryMessage(l, all));
    return send(env, chat, { text: t.help });
  }
  if (cmd === "kurs" || cmd === "rates") {
    const all = await allRates(env);
    return send(env, chat, Object.keys(all).length ? L.summaryMessage(l, all) : { text: t.noData });
  }
  if (cmd === "ogohlantirish" || cmd === "alert" || cmd === "alerts") return send(env, chat, await alertsMessage(env, chat, l));
  if (cmd === "stats" && env.OWNER_CHAT_ID && String(chat) === String(env.OWNER_CHAT_ID)) return send(env, chat, { text: await stats(env) });

  const q = L.parse(text);
  if (!q) return send(env, chat, { text: t.unknown });
  if (q.kind === "summary") {
    const all = await allRates(env);
    return send(env, chat, Object.keys(all).length ? L.summaryMessage(l, all) : { text: t.noData });
  }
  const data = await rates(env, q.cur).catch(() => null);
  if (!data) return send(env, chat, { text: t.noData });
  if (q.kind === "alert") return send(env, chat, { text: await addAlert(env, chat, l, q, data) });
  await countDirect(env, "bot_rate", "telegram");
  return send(env, chat, L.rateMessage(l, data, q));
}

async function onCallback(env, c) {
  const l = L.lang(c.from?.language_code);
  const t = L.T[l];
  const chat = c.message?.chat?.id;
  const data = String(c.data || "");
  let toast;
  if (data.startsWith("r|")) {
    const q = L.uncb(data);
    const d = q && (await rates(env, q.cur).catch(() => null));
    if (d && c.message) {
      const msg = L.rateMessage(l, d, q);
      await tg(env, "editMessageText", { chat_id: chat, message_id: c.message.message_id, text: msg.text, parse_mode: "HTML", link_preview_options: { is_disabled: true }, reply_markup: msg.reply_markup });
    } else if (!d) toast = t.noData;
  } else if (data === "ah" && chat) {
    await send(env, chat, await alertsMessage(env, chat, l));
  } else if (data.startsWith("ad|") && chat) {
    await env.DB.prepare("DELETE FROM alerts WHERE id = ?1 AND chat_id = ?2").bind(Number(data.slice(3)) || 0, chat).run();
    const msg = await alertsMessage(env, chat, l);
    await tg(env, "editMessageText", { chat_id: chat, message_id: c.message.message_id, text: msg.text, parse_mode: "HTML", reply_markup: msg.reply_markup || { inline_keyboard: [] } });
    toast = t.deleted;
  }
  return tg(env, "answerCallbackQuery", { callback_query_id: c.id, ...(toast ? { text: toast } : {}) });
}

async function onInline(env, iq) {
  const l = L.lang(iq.from?.language_code);
  let q = L.parse(iq.query || "");
  if (!q || q.kind !== "rate") {
    const cur = L.currencyIn((iq.query || "").toLowerCase()) || "USD";
    q = { kind: "rate", cur, dir: "sell", amount: L.DEFAULT_AMOUNT[cur], inSom: false };
  }
  const data = await rates(env, q.cur).catch(() => null);
  await countDirect(env, "bot_inline", "telegram");
  return tg(env, "answerInlineQuery", { inline_query_id: iq.id, results: data ? L.inlineResults(l, data, q) : [], cache_time: 300, is_personal: false });
}

/* ---------- alerts ---------- */

async function alertsMessage(env, chat, l) {
  const t = L.T[l];
  const { results } = await env.DB.prepare("SELECT id, cur, side, threshold FROM alerts WHERE chat_id = ?1 ORDER BY id").bind(chat).all();
  if (!results.length) return { text: `${t.alertHelp}\n\n${t.alertNone}` };
  return {
    text: `${t.alertHelp}\n\n${t.alertList}`,
    reply_markup: { inline_keyboard: results.map((a) => [{ text: `${t.del} ${t.alertRow(a.cur, a.side, L.money(a.threshold))}`, callback_data: `ad|${a.id}` }]) }
  };
}

async function addAlert(env, chat, l, q, data) {
  const t = L.T[l];
  const now = L.alertValue(data, q.side);
  if (!now) return t.noData;
  const nowText = `${L.bankName(now.bank)}, ${L.money(now.value)} ${t.som}`;
  // A typo like "dollar 1200 dan oshsa" (a zero short) would fire at once or never.
  const cbu = data.cbu?.rate || now.value;
  if (q.threshold < cbu * 0.7 || q.threshold > cbu * 1.3) return t.alertOdd;
  if (L.alertMet(q.side, now.value, q.threshold)) return t.alertAlready(nowText);
  const { count } = await env.DB.prepare("SELECT COUNT(*) AS count FROM alerts WHERE chat_id = ?1").bind(chat).first();
  if (count >= L.MAX_ALERTS) return t.alertFull;
  await env.DB.prepare("INSERT INTO alerts (chat_id, lang, cur, side, threshold, created) VALUES (?1, ?2, ?3, ?4, ?5, ?6)")
    .bind(chat, l, q.cur, q.side, q.threshold, new Date().toISOString()).run();
  await countDirect(env, "bot_alert", "telegram");
  return t.alertSaved(q.cur, q.side, L.money(q.threshold), nowText);
}

// Same rule as the app (Alerts.kt): tell once when the condition starts to hold, then again only after it stopped holding.
async function checkAlerts(env, all) {
  const { results } = await env.DB.prepare("SELECT * FROM alerts").all();
  for (const a of results) {
    const data = all[a.cur];
    const now = data && L.alertValue(data, a.side);
    if (!now) continue;
    const met = L.alertMet(a.side, now.value, a.threshold);
    if (met && !a.fired) {
      const t = L.T[a.lang] || L.T.uz;
      const res = await send(env, a.chat_id, { text: t.alertFired(a.cur, a.side, L.money(a.threshold), L.money(now.value), now.bank), reply_markup: L.rateButtons(a.lang, { cur: a.cur, dir: a.side === "SELL" ? "sell" : "buy", amount: L.DEFAULT_AMOUNT[a.cur], inSom: false }) });
      // 403: the user blocked the bot. Nobody to tell any more.
      if (res.error_code === 403) await env.DB.prepare("DELETE FROM alerts WHERE chat_id = ?1").bind(a.chat_id).run();
      else await env.DB.prepare("UPDATE alerts SET fired = 1, last_fired = ?2 WHERE id = ?1").bind(a.id, new Date().toISOString()).run();
    } else if (!met && a.fired) {
      await env.DB.prepare("UPDATE alerts SET fired = 0 WHERE id = ?1").bind(a.id).run();
    }
  }
}

/* ---------- channel post on sharp moves ---------- */

// The morning baseline is the first check after 08:00 Tashkent; at most one extra post per currency a day, 09:00–20:00.
async function checkMoves(env, all) {
  if (!env.MOVES_CHANNEL) return;
  const hour = L.tashkentHour();
  if (hour < 8 || hour > 20) return;
  const day = L.tashkentDate();
  for (const cur of L.CURRENCIES) {
    const data = all[cur];
    const s = data && L.rank(data, "sell")[0], b = data && L.rank(data, "buy")[0];
    if (!s || !b) continue;
    const now = { buy: s.buy, sell: b.sell, buyBank: s.bank, sellBank: b.bank };
    const key = `move:${day}:${cur}`;
    const row = await env.DB.prepare("SELECT v FROM state WHERE k = ?1").bind(key).first();
    if (!row) {
      await env.DB.prepare("INSERT INTO state (k, v) VALUES (?1, ?2)").bind(key, JSON.stringify({ buy: now.buy, sell: now.sell, posted: false })).run();
      continue;
    }
    const base = JSON.parse(row.v);
    if (base.posted || hour < 9) continue;
    const text = L.moveMessage(cur, base, now, data.cbu?.rate || 0);
    if (!text) continue;
    const res = await tg(env, "sendMessage", { chat_id: env.MOVES_CHANNEL, text, parse_mode: "HTML", link_preview_options: { is_disabled: true } });
    if (res.ok) await env.DB.prepare("UPDATE state SET v = ?2 WHERE k = ?1").bind(key, JSON.stringify({ ...base, posted: true })).run();
  }
  // Old baselines are not needed after a week.
  await env.DB.prepare("DELETE FROM state WHERE k LIKE 'move:%' AND k < ?1").bind(`move:${L.tashkentDate(Date.now() - 7 * 864e5)}`).run();
}

/* ---------- usage counts ---------- */

// One row per day, event and platform with a counter: nothing about who sent it.
async function countEvents(env, request) {
  const body = await request.text().catch(() => "");
  let p, names;
  try {
    const j = JSON.parse(body);
    p = j.p;
    names = Array.isArray(j.e) ? j.e : [j.e];
  } catch {
    return new Response("bad json", { status: 400, headers: CORS });
  }
  names = [...new Set(names)].filter((n) => /^[a-z][a-z0-9_]{1,31}$/.test(n || "")).slice(0, 10);
  if (!PLATFORMS.includes(p) || !names.length) return new Response("bad event", { status: 400, headers: CORS });
  const day = L.tashkentDate();
  const stmt = env.DB.prepare("INSERT INTO events (day, name, platform, n) VALUES (?1, ?2, ?3, 1) ON CONFLICT(day, name, platform) DO UPDATE SET n = n + 1");
  await env.DB.batch(names.map((n) => stmt.bind(day, n, p)));
  return new Response(null, { status: 204, headers: CORS });
}

const countDirect = (env, name, platform) =>
  env.DB.prepare("INSERT INTO events (day, name, platform, n) VALUES (?1, ?2, ?3, 1) ON CONFLICT(day, name, platform) DO UPDATE SET n = n + 1")
    .bind(L.tashkentDate(), name, platform).run().catch((e) => console.error("count failed", e));

async function stats(env) {
  const week = L.tashkentDate(Date.now() - 6 * 864e5);
  const weekAgoIso = new Date(Date.now() - 7 * 864e5).toISOString();
  const [users, active, alerts, ev] = await env.DB.batch([
    env.DB.prepare("SELECT COUNT(*) AS n FROM users"),
    env.DB.prepare("SELECT COUNT(*) AS n FROM users WHERE last_seen >= ?1").bind(weekAgoIso),
    env.DB.prepare("SELECT COUNT(*) AS n FROM alerts"),
    env.DB.prepare("SELECT platform, name, SUM(n) AS n FROM events WHERE day >= ?1 GROUP BY platform, name ORDER BY platform, n DESC").bind(week)
  ]);
  const lines = ev.results.map((r) => `${r.platform} · ${r.name}: ${r.n}`);
  return [
    "<b>Qulay Kurs, 7 kun</b>",
    `Bot foydalanuvchilari: ${users.results[0].n} (7 kunda faol: ${active.results[0].n})`,
    `Ogohlantirishlar: ${alerts.results[0].n}`,
    "",
    lines.length ? lines.join("\n") : "Hodisalar yo‘q"
  ].join("\n");
}
