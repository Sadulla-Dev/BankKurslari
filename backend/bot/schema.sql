-- D1 database of the bot Worker. Apply with:
--   npx wrangler d1 execute qulaykurs-bot --remote --file schema.sql
-- Safe to run again: every statement is IF NOT EXISTS.

-- People who wrote to the bot in a private chat. Only to answer them and count them; the bot never writes first,
-- except the rate alerts each user set up themselves.
CREATE TABLE IF NOT EXISTS users (
  chat_id    INTEGER PRIMARY KEY,
  lang       TEXT,
  first_seen TEXT,
  last_seen  TEXT,
  source     TEXT   -- the /start payload of the first visit ("inline", "site", "app"), if any
);

CREATE TABLE IF NOT EXISTS alerts (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id    INTEGER NOT NULL,
  lang       TEXT,
  cur        TEXT NOT NULL,      -- USD | EUR | RUB
  side       TEXT NOT NULL,      -- SELL: best bank buy >= threshold; BUY: best bank sell <= threshold (as in Alerts.kt)
  threshold  REAL NOT NULL,
  fired      INTEGER NOT NULL DEFAULT 0,
  last_fired TEXT,
  created    TEXT
);
CREATE INDEX IF NOT EXISTS alerts_chat ON alerts (chat_id);

-- Anonymous usage counts: one row per day (Tashkent), event and platform.
CREATE TABLE IF NOT EXISTS events (
  day      TEXT NOT NULL,
  name     TEXT NOT NULL,
  platform TEXT NOT NULL,
  n        INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (day, name, platform)
);

-- Small key/value state (the morning baselines of the channel move posts).
CREATE TABLE IF NOT EXISTS state (
  k TEXT PRIMARY KEY,
  v TEXT
);
