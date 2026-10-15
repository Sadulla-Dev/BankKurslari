# ValyutaStat backend

Small local scraper API for the Android app.

## Run

```bash
cd backend
npm install
npm start
```

The server listens on `http://localhost:8080`.

## Endpoints

- `GET /health`
- `GET /api/rates/usd`

`/api/rates/usd` combines:

- CBU official USD rate from `https://cbu.uz/uz/arkhiv-kursov-valyut/json/USD/`
- Commercial bank buy/sell rates parsed from `https://bank.uz/uz/currency/dollar-ssha-uz-aqsh-dollari`

The response is cached in memory for 15 minutes by default. Override with `CACHE_TTL_MS`.
