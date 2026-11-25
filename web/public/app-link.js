// The way from the site to the Android app (Google Play). Every page has a Play button in the header and a hidden
// banner above it (index.html, build.mjs shell). There is no iOS app, so on an iPhone or iPad both stay out of sight;
// on Android the banner shows until it is closed (then not again for 30 days) or the site runs as an installed web app.
(() => {
  const ua = navigator.userAgent || "";
  const ios = /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1);
  if (ios) {
    for (const el of document.querySelectorAll(".get-app")) el.hidden = true;
    return;
  }
  const banner = document.getElementById("appBanner");
  if (!banner || !/Android/i.test(ua)) return;
  if (matchMedia("(display-mode: standalone)").matches) return;
  const KEY = "qk-app-banner-off";
  try { if (Date.now() - Number(localStorage.getItem(KEY) || 0) < 30 * 864e5) return; } catch { /* private mode */ }
  banner.hidden = false;
  banner.querySelector(".ab-close")?.addEventListener("click", () => {
    banner.hidden = true;
    try { localStorage.setItem(KEY, String(Date.now())); } catch { /* private mode */ }
  });
})();
