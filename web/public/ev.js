// Anonymous usage counts for qulaykurs.uz, sent to bot.qulaykurs.uz/e (backend/bot/worker.js): "the site was opened
// today" once a day per browser, and clicks on elements marked data-ev (the app and bot buttons). No ids, no cookies;
// the server keeps one counter per day and event, nothing about who sent it.
//
// The same events (and page views) also go to PostHog when PH_KEY is set, in anonymous mode: a random id kept in
// localStorage (no cookies), no person profiles, no autocapture, no session recording, no surveys, no GeoIP.
// PH_KEY is the project API key (phc_...), public by design; empty = PostHog is not loaded at all.
(() => {
  const PH_KEY = "";
  const PH_HOST = "https://us.i.posthog.com"; // or https://eu.i.posthog.com, same region as the project

  let ph = null;
  if (PH_KEY) {
    try {
      // The official posthog-js loader, reduced to the calls used here: a queue that array.js replays once loaded.
      ph = window.posthog = [];
      ph.__SV = 1;
      ph._i = [[PH_KEY, {
        api_host: PH_HOST,
        persistence: "localStorage",
        person_profiles: "never",
        autocapture: false,
        capture_pageview: true,
        capture_pageleave: false,
        disable_session_recording: true,
        disable_surveys: true,
        advanced_disable_feature_flags: true,
        ip: false,
      }]];
      for (const m of ["capture", "register"]) ph[m] = (...a) => ph.push([m, ...a]);
      ph.register({ $geoip_disable: true });
      const s = document.createElement("script");
      s.async = true;
      s.crossOrigin = "anonymous";
      s.src = PH_HOST.replace(".i.posthog.com", "-assets.i.posthog.com") + "/static/array.js";
      document.head.appendChild(s);
    } catch { ph = null; }
  }

  const beacon = (e) => { try { navigator.sendBeacon("https://bot.qulaykurs.uz/e", JSON.stringify({ p: "web", e })); } catch {} };
  const event = (name) => { beacon([name]); try { window.posthog?.capture?.(name); } catch {} };
  try {
    const day = new Date(Date.now() + 5 * 36e5).toISOString().slice(0, 10); // Tashkent date
    if (localStorage.getItem("qk-ev-day") !== day) { localStorage.setItem("qk-ev-day", day); beacon(["open"]); }
  } catch {}
  document.addEventListener("click", (ev) => { const el = ev.target.closest?.("[data-ev]"); if (el) event(el.dataset.ev); }, true);
  window.qkEvent = event;
})();
