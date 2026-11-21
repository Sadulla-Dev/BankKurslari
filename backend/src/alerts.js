// Problems in the hourly API build, sent to the owner only, as a private Telegram message from the channel's bot.
//
//   TELEGRAM_BOT_TOKEN       the bot that posts to the channel (already a repo secret)
//   TELEGRAM_OWNER_CHAT_ID   the owner's personal chat id. The owner opens the bot and presses Start once, so the
//                            bot may write to them. The bot never answers anyone: it only ever sends to this id.
//
// Without these the alerts are only printed to the build log.
//
// One message per run at most. It lists problems that are new and problems that went away. A problem that lasts is
// repeated every REMIND_MS, so an hourly build does not send the same thing 24 times a day. State lives in
// _state/alerts.json, which the build carries over from run to run like the other state files.
import { readFile, writeFile } from "node:fs/promises";

const REMIND_MS = 12 * 60 * 60 * 1000;
const MAX_LEN = 3800; // Telegram allows 4096 characters per message

export function createAlerts() {
  const issues = new Map(); // key -> text
  return {
    /** Records a problem. Same key on every run = same problem (used to tell new, ongoing and resolved apart). */
    add(key, text) {
      issues.set(key, text);
      console.error(`ALERT ${key}: ${text}`);
    },
    issues
  };
}

async function readState(path) {
  try {
    return JSON.parse(await readFile(path, "utf8"));
  } catch {
    return {};
  }
}

export async function sendToOwner(text) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  const chat = process.env.TELEGRAM_OWNER_CHAT_ID;
  if (!token || !chat) {
    console.log("(alerts) TELEGRAM_BOT_TOKEN / TELEGRAM_OWNER_CHAT_ID not set, message not sent:\n" + text);
    return false;
  }
  const res = await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ chat_id: chat, text: text.slice(0, MAX_LEN), disable_web_page_preview: true }),
    signal: AbortSignal.timeout(20000)
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok || !body.ok) throw new Error(`Telegram sendMessage failed: ${res.status} ${body.description || ""}`);
  return true;
}

/**
 * Compares this run's problems with the previous run's, sends one message if something changed (or a reminder is
 * due), and saves the new state. Never throws: alerting must not break the build.
 */
export async function flushAlerts(alerts, statePath, { now = Date.now(), runUrl = "" } = {}) {
  try {
    const prev = await readState(statePath);
    const next = {};
    const opened = [];
    const ongoing = [];
    const closed = [];
    for (const [key, text] of alerts.issues) {
      const was = prev[key];
      if (!was) {
        opened.push(text);
        next[key] = { text, since: now, sentAt: now };
      } else if (now - was.sentAt >= REMIND_MS) {
        ongoing.push(`${text} (${Math.round((now - was.since) / 3600000)} soatdan beri)`);
        next[key] = { text, since: was.since, sentAt: now };
      } else {
        next[key] = { ...was, text };
      }
    }
    for (const [key, was] of Object.entries(prev)) {
      if (!alerts.issues.has(key)) closed.push(was.text);
    }
    if (opened.length || ongoing.length || closed.length) {
      const lines = ["Qulay Kurs: API build"];
      if (opened.length) lines.push("", "⚠️ Yangi muammo:", ...opened.map((t) => "• " + t));
      if (ongoing.length) lines.push("", "⏳ Hali hal bo'lmagan:", ...ongoing.map((t) => "• " + t));
      if (closed.length) lines.push("", "✅ Tiklandi:", ...closed.map((t) => "• " + t));
      if (runUrl) lines.push("", runUrl);
      await sendToOwner(lines.join("\n"));
    }
    await writeFile(statePath, JSON.stringify(next));
  } catch (error) {
    console.error("(alerts) failed:", error.message);
  }
}
