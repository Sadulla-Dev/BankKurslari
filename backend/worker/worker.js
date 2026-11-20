// api.qulaykurs.uz: serves the JSON files that src/build.js produces (GitHub Actions rebuilds and redeploys them).
// Same routes and response shapes as the Express server, so the app works against either.
const CURRENCIES = ["USD", "EUR", "RUB"];
const CORS = { "access-control-allow-origin": "*" };

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-cache", ...CORS }
  });
}

async function asset(env, request, path) {
  const res = await env.ASSETS.fetch(new URL(path, request.url));
  if (!res.ok) return null;
  return res;
}

async function serve(env, request, path, missing, status = 502) {
  const res = await asset(env, request, path);
  if (!res) return json({ error: missing }, status);
  return new Response(res.body, {
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "public, max-age=60", ...CORS }
  });
}

const localDate = (ms) => new Date(ms + 5 * 60 * 60 * 1000).toISOString().slice(0, 10);

// GitHub drops or delays many `schedule` runs, so the Worker's own cron starts the daily Telegram post instead.
// Needs the GH_DISPATCH_TOKEN secret: a fine-grained token for this repo with "Actions: Read and write".
const DISPATCH = {
  "55 2 * * *": "telegram-post.yml" // 07:55 Tashkent: the post lands around 8:00
};

async function dispatchWorkflow(env, workflow) {
  if (!env.GH_DISPATCH_TOKEN) throw new Error("GH_DISPATCH_TOKEN secret is not set");
  const res = await fetch(`https://api.github.com/repos/Sadulla-Dev/BankKurslari/actions/workflows/${workflow}/dispatches`, {
    method: "POST",
    headers: {
      authorization: `Bearer ${env.GH_DISPATCH_TOKEN}`,
      accept: "application/vnd.github+json",
      "x-github-api-version": "2022-11-28",
      "user-agent": "qulaykurs-api-cron",
      "content-type": "application/json"
    },
    body: JSON.stringify({ ref: "main" })
  });
  if (!res.ok) throw new Error(`dispatch ${workflow} failed: ${res.status} ${await res.text()}`);
}

export default {
  async scheduled(event, env) {
    const workflow = DISPATCH[event.cron];
    if (workflow) await dispatchWorkflow(env, workflow);
  },

  // Requests that reach the Worker (no static file matched) are cached at the edge for 5 minutes, so repeated
  // bank-history / gold-history / branches calls skip the JSON parsing. Data is rebuilt hourly at most.
  async fetch(request, env, ctx) {
    if (request.method !== "GET") return handle(request, env);
    const cache = caches.default;
    const hit = await cache.match(request);
    if (hit) return hit;
    const res = await handle(request, env);
    if (res.status !== 200) return res;
    const out = new Response(res.body, res);
    out.headers.set("cache-control", "public, max-age=300");
    ctx.waitUntil(cache.put(request, out.clone()));
    return out;
  }
};

async function handle(request, env) {
  if (request.method === "OPTIONS") return new Response(null, { headers: { ...CORS, "access-control-allow-methods": "GET" } });
  if (request.method !== "GET" && request.method !== "HEAD") return json({ error: "method_not_allowed" }, 405);

  const url = new URL(request.url);
  const parts = url.pathname.replace(/\/+$/, "").split("/").filter(Boolean);
  // Raw build files (the builder pulls them back before each rebuild).
  if (parts[0] === "api" && url.pathname.endsWith(".json")) return serve(env, request, url.pathname, "not_found", 404);
  if (parts[0] === "health") return serve(env, request, "/health.json", "not_built");
  if (parts[0] !== "api") return json({ error: "not_found" }, 404);

  const [, kind, arg] = parts;
  const cur = String(arg || "").toUpperCase();
  switch (kind) {
    case "rates":
    case "history":
    case "analytics":
      if (!CURRENCIES.includes(cur)) return json({ error: "unsupported_currency", supported: CURRENCIES }, 404);
      return serve(env, request, `/api/${kind}/${cur}.json`, `${kind}_unavailable`);
    case "branches":
      if (arg === "yandex") {
        const res = await asset(env, request, "/api/branches/yandex.json");
        if (!res) return json({ error: "yandex_branches_unavailable" }, 503);
        const dataset = await res.json();
        if (!dataset.expiresAt || Date.parse(dataset.expiresAt) <= Date.now()) return json({ error: "yandex_branches_expired" }, 503);
        return json(dataset);
      }
      if (arg) return json({ error: "not_found" }, 404);
      return serve(env, request, "/api/branches.json", "branches_unavailable");
    case "atms":
      return serve(env, request, `/api/${kind}.json`, `${kind}_unavailable`);
    case "cbu":
      return arg === "all" ? serve(env, request, "/api/cbu/all.json", "cbu_unavailable") : json({ error: "not_found" }, 404);
    case "gold": {
      if (!arg) return serve(env, request, "/api/gold.json", "gold_unavailable");
      if (arg !== "history") return json({ error: "not_found" }, 404);
      const res = await asset(env, request, "/_state/gold-history.json");
      const hist = res ? await res.json() : {};
      const points = Object.entries(hist).sort(([a], [b]) => (a < b ? -1 : 1)).map(([date, bars]) => ({ date, bars }));
      return json({ points });
    }
    case "bank-history": {
      if (!CURRENCIES.includes(cur)) return json({ error: "unsupported_currency" }, 400);
      const days = Math.min(365, Math.max(1, Number(url.searchParams.get("days")) || 30));
      const res = await asset(env, request, `/_state/bank-history-${cur}.json`);
      const data = res ? await res.json() : {};
      const from = localDate(Date.now() - days * 24 * 60 * 60 * 1000);
      const out = Object.fromEntries(Object.entries(data).filter(([d]) => d >= from).sort(([a], [b]) => (a < b ? -1 : 1)));
      return json({ currency: cur, days: out });
    }
    default:
      return json({ error: "not_found" }, 404);
  }
}
