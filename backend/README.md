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

## Separate Yandex branch dataset

Yandex results are stored separately in `data/yandex-branches.json`; the existing `branches.json` dataset and `/api/branches` response are not merged or overwritten. The separate endpoint is `GET /api/branches/yandex`.

Set `YANDEX_PLACES_KEY` and run `npm run import:yandex-branches` to fetch the Uzbekistan dataset. The importer keeps its own 30-day cache and accepts `-- --force` to refresh early. Set `YANDEX_BANK_QUERIES` to a comma-separated list if the Yandex-approved query terms for your license require different wording. Full scheduled builds refresh this cache when it expires; add the key as the `YANDEX_PLACES_KEY` GitHub Actions secret for that to work in production.

The initial Yandex file contains only the branch captured in Arc and is marked `coverage: sample`; the API endpoint returns it only until its `expiresAt`. A countrywide import requires the Yandex API key in the environment. Search is tiled across Uzbekistan and records completeness diagnostics because the Places API returns ranked search matches, not a guaranteed complete directory.
