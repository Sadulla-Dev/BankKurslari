// Amount pages (built by ../build.mjs): typing another amount recalculates every number marked data-mul or data-div.
"use strict";
(() => {
  const input = document.querySelector("[data-calc]");
  if (!input) return;
  const sep = input.dataset.sep || ",";
  // Same rounding as num() in build.mjs.
  const format = (v) => {
    const a = Math.abs(v), digits = a >= 100000 ? 0 : a >= 1 ? 2 : a >= 0.01 ? 4 : 6;
    const [whole, frac = ""] = a.toFixed(digits).split("."), tail = frac.replace(/0+$/, "");
    return whole.replace(/\B(?=(\d{3})+(?!\d))/g, " ") + (tail ? sep + tail : "");
  };
  input.addEventListener("input", () => {
    const n = parseFloat(input.value.replace(/\s/g, "").replace(",", "."));
    if (!(n > 0) || n > 1e12) return;
    for (const el of document.querySelectorAll("[data-mul]")) el.textContent = format(n * el.dataset.mul);
    for (const el of document.querySelectorAll("[data-div]")) el.textContent = format(n / el.dataset.div);
  });
  input.addEventListener("focus", () => input.select());
})();
