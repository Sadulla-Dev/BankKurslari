// Anonymous usage counts for qulaykurs.uz, sent to bot.qulaykurs.uz/e (backend/bot/worker.js): "the site was opened
// today" once a day per browser, and clicks on elements marked data-ev (the app and bot buttons). No ids, no cookies;
// the server keeps one counter per day and event, nothing about who sent it.
(() => {
  const send = (e) => { try { navigator.sendBeacon("https://bot.qulaykurs.uz/e", JSON.stringify({ p: "web", e })); } catch {} };
  try {
    const day = new Date(Date.now() + 5 * 36e5).toISOString().slice(0, 10); // Tashkent date
    if (localStorage.getItem("qk-ev-day") !== day) { localStorage.setItem("qk-ev-day", day); send(["open"]); }
  } catch {}
  document.addEventListener("click", (ev) => { const el = ev.target.closest?.("[data-ev]"); if (el) send([el.dataset.ev]); }, true);
  window.qkEvent = (name) => send([name]);
})();
