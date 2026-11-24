# @qulaykursbot Worker (`bot.qulaykurs.uz`)

What it does:

- **Buttons first:** `/start` puts a keyboard under the message box (💵 Dollar, 💶 Yevro, 🇷🇺 Rubl, 📊 Bugungi kurslar,
  🔔 Ogohlantirish). A result has buttons for the amount (100 … 10 000), Sotaman / Sotib olaman, the currency, the site,
  and a new alert. Alerts are made with buttons too: currency → rises / falls → one of four values near today's best.
- **Typing works as well:** `500 dollar`, `1000 yevro sotib olaman`, `5 mln so‘m dollar`, `kurs`, or a bare `750`
  (the currency and direction the user looked at last).
- **Loading:** "typing…" at once; if the rates take longer than 350 ms, a "⏳ Kurslar yuklanmoqda…" message that the
  answer then replaces, and on a button press the pressed button turns into ⏳. Rates are kept for a minute in memory.
- **Inline mode:** `@qulaykursbot 1000 usd` in any chat puts that answer into the chat (people share it, the bot spreads).
- **Rate alerts:** `dollar 12 100 dan oshsa` / `dollar 11 900 dan tushsa`, at most 5 a person, checked every hour.
  Same rule as the app (`Alerts.kt`): one message when the condition starts to hold, again only after it stopped holding.
  This is how iOS and web users get alerts.
- **Channel posts on sharp moves:** when the best bank rate moved ≥ 0.4% (USD), 0.5% (EUR), 1.5% (RUB) from the
  morning (08:00) value, one extra post to `MOVES_CHANNEL`, at most once a currency a day, 09:00–20:00.
- **Usage counts** (`POST /e`) from the app and the site: per day, event and platform, a counter. No ids, no IPs.
  The owner sends `/stats` to the bot to see the last 7 days.

The bot only answers people who wrote to it. It never writes first, except the alerts a person set up.

## Files

| File | |
|---|---|
| `lib.js` | Parsing, ranking, texts (uz, ru). No network: `node test.mjs` checks it. |
| `worker.js` | Webhook, `/e`, cron, D1. |
| `schema.sql` | D1 tables. |
| `setup.mjs` | Webhook, command menu, descriptions. |

## First-time setup

Run in `backend/bot`:

1. Database: `npx wrangler d1 create qulaykurs-bot`, put the printed `database_id` into `wrangler.toml`, then
   `npx wrangler d1 execute qulaykurs-bot --remote --file schema.sql`.
2. Secrets: `npx wrangler secret put TELEGRAM_BOT_TOKEN`, `npx wrangler secret put WEBHOOK_SECRET` (any long random
   string, e.g. `openssl rand -hex 24`), `npx wrangler secret put OWNER_CHAT_ID` (same value as the repo secret
   `TELEGRAM_OWNER_CHAT_ID`).
3. Deploy: `npx wrangler deploy` (also attaches `bot.qulaykurs.uz`).
4. Webhook and menu: `TELEGRAM_BOT_TOKEN=… WEBHOOK_SECRET=… node setup.mjs`.
5. In @BotFather: `/setinline` → @qulaykursbot → placeholder `100 dollar` (turns inline mode on).

When the bot token changes (BotFather `/revoke`), repeat step 2 for `TELEGRAM_BOT_TOKEN` and step 4.

## Local test

```bash
printf 'TG_API=http://127.0.0.1:9911\nWEBHOOK_SECRET=test\nTELEGRAM_BOT_TOKEN=x\nOWNER_CHAT_ID=1\n' > .dev.vars
npx wrangler d1 execute qulaykurs-bot --local --file schema.sql
npx wrangler dev --port 8799 --test-scheduled
```

`TG_API` points the Worker at a fake Telegram server (anything that logs the POST bodies and answers `{"ok":true}`).
Post updates to `http://localhost:8799/tg` with the header `X-Telegram-Bot-Api-Secret-Token: test`, and run the cron with
`curl "localhost:8799/__scheduled?cron=20+*+*+*+*"`.
