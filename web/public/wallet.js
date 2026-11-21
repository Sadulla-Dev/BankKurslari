// Hamyon (#/hamyon): what the visitor's savings are worth in so'm today, and currency-to-currency exchange through so'm.
// The arithmetic mirrors composeApp/.../Wallet.kt line for line. Holdings stay in this browser (localStorage).
// Loaded after app.js and uses its globals: S, $, esc, group, store, loadRates, loadGold, getJSON, shortName, badgeHTML.
"use strict";

const W_CODES = {
  USD: { sym: "$", dec: 2 }, EUR: { sym: "€", dec: 2 }, RUB: { sym: "₽", dec: 0 }, GBP: { sym: "£", dec: 2 },
  KZT: { sym: "₸", dec: 0 }, UZS: { sym: "S", dec: 0 }, XAU: { sym: "Au", dec: 1 }
};
const W_ALL = ["USD", "EUR", "RUB", "GBP", "KZT", "UZS", "XAU"];
const W_XCHG = ["USD", "EUR", "RUB", "GBP", "KZT", "UZS"];

const WT = {
  uz: {
    goalSet: "Maqsad qo‘ying", goalHint: "Masalan, mashina uchun 20 000 $: qancha yig‘ilganini ko‘rib borasiz", goal: "Maqsad",
    goalName: "Nima uchun? Masalan: Mashina", goalDone: "Maqsadga yetdingiz! 🎉", goalLeft: (v, u) => `Yana ${v} ${u} kerak`, goalDel: "Maqsadni o‘chirish",
    nudgeT: "Jamg‘armangiz bugun qancha turadi?", nudgeP: "Hamyonga qo‘shing: har kuni qiymati, o‘zgarishi va maqsadingiz", close: "Yopish",
    title: "Hamyon", sub: "Jamg‘armangiz bugun qancha turadi",
    emptyT: "Jamg‘armangizni qo‘shing",
    emptyP: "Dollar, yevro, rubl, oltin yoki so‘mda qancha pulingiz borligini yozing. Sayt har kuni u bugun banklarda qancha turishini va kechagidan qancha o‘zgarganini ko‘rsatadi.",
    add: "Qo‘shish", addMore: "Valyuta yoki oltin qo‘shish", heroL: "Banklarda sotsangiz, bugun",
    day: "Kecha bilan farq", inUsd: "Dollarda", atCbu: "MB kursi bo‘yicha",
    cash: "Naqd so‘m", cbuBuys: "MB qaytarib oladi", grams: "g oltin", gramsUnit: "gramm", gold: "Oltin", loading: "Kurs yuklanmoqda…",
    edit: "O‘zgartirish", save: "Saqlash", remove: "O‘chirish", cancel: "Bekor qilish", addT: "Jamg‘arma qo‘shish",
    merge: "Bu valyuta bor: summa unga qo‘shiladi.",
    xT: "Valyutani valyutaga almashtirish", xEntry: "Masalan, dollarni yevroga: eng foydali yo‘l",
    xP: "Masalan, safarga yevro kerak, sizda dollar bor. Banklar to‘g‘ridan-to‘g‘ri almashtirmaydi: avval so‘mga sotasiz, keyin kerakli valyutani olasiz. Eng foydali yo‘lni hisoblaymiz.",
    have: "Menda bor", need: "Menga kerak", swap: "Almashtirish", get: "Qo‘lingizga tegadi", enter: "Summani kiriting",
    step1: (b, a, c, r, s) => `${b}: ${a} ${c} sotasiz, kurs ${r} → ${s} so‘m`,
    step2: (b, c, r) => `${b}: ${c} olasiz, kurs ${r}`,
    one: (b, v, u, d) => `Ikkalasi bitta bankda (${b}): ${v} ${u}, ya’ni ${d} kam`,
    cbu: (v, u, p) => `MB kursi bo‘yicha ${v} ${u} bo‘lardi. Banklar farqi: ${p}%`,
    note: "Qiymat bugun eng ko‘p to‘laydigan bankning naqd kursi bo‘yicha (oltin: Markaziy bank qaytarib sotib olish narxi, 10 g quyma). Ma’lumotlaringiz faqat shu brauzerda saqlanadi.",
    som: "so‘m"
  },
  ru: {
    goalSet: "Поставьте цель", goalHint: "Например, 20 000 $ на машину: видно, сколько уже собрано", goal: "Цель",
    goalName: "На что? Например: Машина", goalDone: "Цель достигнута! 🎉", goalLeft: (v, u) => `Осталось ${v} ${u}`, goalDel: "Удалить цель",
    nudgeT: "Сколько стоят ваши сбережения?", nudgeP: "Добавьте в кошелёк: стоимость, изменение и цель каждый день", close: "Закрыть",
    title: "Кошелёк", sub: "Сколько стоят ваши сбережения сегодня",
    emptyT: "Добавьте свои сбережения",
    emptyP: "Укажите, сколько у вас долларов, евро, рублей, золота или сумов. Сайт каждый день покажет, сколько это стоит в банках сегодня и как изменилось со вчера.",
    add: "Добавить", addMore: "Добавить валюту или золото", heroL: "Если продать в банках сегодня",
    day: "За день", inUsd: "В долларах", atCbu: "по курсу ЦБ",
    cash: "Наличные сумы", cbuBuys: "ЦБ выкупает", grams: "г золота", gramsUnit: "грамм", gold: "Золото", loading: "Курс загружается…",
    edit: "Изменить", save: "Сохранить", remove: "Удалить", cancel: "Отмена", addT: "Добавить сбережения",
    merge: "Эта валюта уже есть: сумма прибавится.",
    xT: "Обмен валюты на валюту", xEntry: "Например, доллары на евро: самый выгодный путь",
    xP: "Например, для поездки нужны евро, а у вас доллары. Банки меняют через сумы: сначала продаёте, потом покупаете. Считаем самый выгодный путь.",
    have: "У меня есть", need: "Мне нужно", swap: "Поменять местами", get: "Вы получите", enter: "Введите сумму",
    step1: (b, a, c, r, s) => `${b}: продаёте ${a} ${c}, курс ${r} → ${s} сум`,
    step2: (b, c, r) => `${b}: покупаете ${c}, курс ${r}`,
    one: (b, v, u, d) => `Всё в одном банке (${b}): ${v} ${u}, на ${d} меньше`,
    cbu: (v, u, p) => `По курсу ЦБ было бы ${v} ${u}. Разница банков: ${p}%`,
    note: "Стоимость по наличному курсу банка, который сегодня платит больше всех (золото: цена обратного выкупа ЦБ, слиток 10 г). Данные хранятся только в этом браузере.",
    som: "сум"
  },
  en: {
    goalSet: "Set a goal", goalHint: "E.g. 20 000 $ for a car: see how much is already there", goal: "Goal",
    goalName: "What for? E.g. Car", goalDone: "Goal reached! 🎉", goalLeft: (v, u) => `${v} ${u} to go`, goalDel: "Remove goal",
    nudgeT: "What are your savings worth today?", nudgeP: "Add them to the wallet: value, change and your goal every day", close: "Close",
    title: "Wallet", sub: "What your savings are worth today",
    emptyT: "Add your savings",
    emptyP: "Enter how much you keep in dollars, euros, rubles, gold or so'm. Every day the site shows what it is worth at banks and how it changed since yesterday.",
    add: "Add", addMore: "Add currency or gold", heroL: "If you sold at banks today",
    day: "Since yesterday", inUsd: "In dollars", atCbu: "at CBU rate",
    cash: "Cash so'm", cbuBuys: "CBU buys back", grams: "g gold", gramsUnit: "grams", gold: "Gold", loading: "Loading rate…",
    edit: "Edit", save: "Save", remove: "Remove", cancel: "Cancel", addT: "Add savings",
    merge: "Already in the wallet: the amount is added to it.",
    xT: "Currency to currency", xEntry: "E.g. dollars to euros: the cheapest route",
    xP: "Say you need euros for a trip and hold dollars. Banks go through so'm: sell first, then buy. We find the cheapest route.",
    have: "I have", need: "I need", swap: "Swap", get: "You get", enter: "Enter an amount",
    step1: (b, a, c, r, s) => `${b}: sell ${a} ${c} at ${r} → ${s} so'm`,
    step2: (b, c, r) => `${b}: buy ${c} at ${r}`,
    one: (b, v, u, d) => `At one bank (${b}): ${v} ${u}, ${d} less`,
    cbu: (v, u, p) => `At the CBU rate: ${v} ${u}. Bank margin: ${p}%`,
    note: "Valued at the cash rate of the bank paying the most today (gold: CBU buyback price, 10 g bar). Your data stays in this browser.",
    som: "so'm"
  }
};
WT.uzc = uzCyrillicTexts(WT.uz);
const WL = () => WT[S.lang] || WT.uz;

/* ---------- storage ---------- */

const WS = {
  load() {
    return (store.get("wallet_v1") || "").split(";").map((p) => p.split("=")).filter((p) => p.length === 2 && W_CODES[p[0]] && Number(p[1]) > 0)
      .map(([code, a]) => ({ code, amount: Number(a) }));
  },
  save(list) { store.set("wallet_v1", list.filter((h) => h.amount > 0).map((h) => `${h.code}=${h.amount}`).join(";")); },
  upsert(list, h, replace) {
    const i = list.findIndex((x) => x.code === h.code);
    const next = list.slice();
    if (i < 0) next.push(h); else next[i] = replace ? h : { code: h.code, amount: next[i].amount + h.amount };
    return next.filter((x) => x.amount > 0);
  }
};

const GS = {
  load() {
    const p = (store.get("wallet_goal_v1") || "").split("|");
    if (p.length < 2 || !W_CODES[p[0]] || !(Number(p[1]) > 0)) return null;
    return { code: p[0], amount: Number(p[1]), name: p.slice(2).join("|").trim() };
  },
  save(g) { store.set("wallet_goal_v1", g ? `${g.code}|${g.amount}|${g.name.replace(/\|/g, " ").slice(0, 40)}` : ""); }
};
const GOAL_CODES = ["USD", "EUR", "UZS"];

/* ---------- data ---------- */

const W = { quotes: {}, editing: null, adding: false, sheetCode: "USD",
  from: store.get("xchg_from") || "USD", to: store.get("xchg_to") || "EUR", amount: store.get("xchg_amount") || "1000" };
if (!W_XCHG.includes(W.from)) W.from = "USD";
if (!W_XCHG.includes(W.to) || W.to === W.from) W.to = W.from === "EUR" ? "USD" : "EUR";

const wExtra = {};
function wRates(code) {
  if (["USD", "EUR", "RUB"].includes(code)) return loadRates(code);
  return (wExtra[code] ||= getJSON(`/api/rates/${code.toLowerCase()}`).catch((e) => { delete wExtra[code]; throw e; }));
}
function wQuote(code, r) {
  const banks = (r.banks || []).map((b) => ({ bank: b.bank, buy: Number(b.buy) || null, sell: Number(b.sell) || null })).filter((b) => b.buy || b.sell);
  return { code, cbu: Number(r.cbu?.rate) || 0, diff: Number(r.cbu?.diff) || 0, banks };
}
function wGoldQuote(g) {
  const bars = g?.bars || [];
  const bar = bars.find((b) => b.grams === 10) || bars.slice().sort((a, b) => a.grams - b.grams)[0];
  if (!bar) return null;
  const sell = bar.sell / bar.grams, buyback = bar.buyback ? bar.buyback / bar.grams : null;
  return { code: "XAU", cbu: sell, diff: 0, banks: [{ bank: "Markaziy bank", buy: buyback, sell }] };
}
const bestBuy = (q) => q.banks.filter((b) => b.buy > 0).reduce((m, b) => (!m || b.buy > m.buy ? b : m), null);
const bestSell = (q) => q.banks.filter((b) => b.sell > 0).reduce((m, b) => (!m || b.sell < m.sell ? b : m), null);

async function wLoad() {
  const jobs = ["USD", "EUR", "RUB", "GBP", "KZT"].map((c) => wRates(c).then((r) => { W.quotes[c] = wQuote(c, r); }).catch(() => {}));
  jobs.push(loadGold().then((g) => { const q = wGoldQuote(g); if (q) W.quotes.XAU = q; }).catch(() => {}));
  await Promise.all(jobs);
}

/* ---------- arithmetic (same as Wallet.kt) ---------- */

function wSummary(list) {
  const rows = list.map((h) => {
    if (h.code === "UZS") return { h, som: h.amount, bank: null, rate: 1, day: 0 };
    const q = W.quotes[h.code];
    if (!q) return { h, som: null, bank: null, rate: null, day: 0 };
    const b = bestBuy(q);
    return { h, som: b ? b.buy * h.amount : null, bank: b?.bank || null, rate: b?.buy || null, day: q.diff * h.amount };
  });
  const total = rows.reduce((s, r) => s + (r.som || 0), 0);
  const usd = W.quotes.USD?.cbu > 0 ? total / W.quotes.USD.cbu : null;
  return { rows, total, day: rows.reduce((s, r) => s + r.day, 0), usd };
}

function wExchange(from, to, amount) {
  if (!(amount > 0) || from === to) return null;
  const qf = from === "UZS" ? null : W.quotes[from], qt = to === "UZS" ? null : W.quotes[to];
  if ((from !== "UZS" && !qf) || (to !== "UZS" && !qt)) return null;
  const sell = qf ? bestBuy(qf) : null, buy = qt ? bestSell(qt) : null;
  if ((qf && !sell) || (qt && !buy)) return null;
  const som = qf ? amount * sell.buy : amount;
  const result = qt ? som / buy.sell : som;
  let one = null, oneBank = null;
  if (qf && qt) {
    const sells = new Map(qt.banks.map((b) => [b.bank, b.sell]));
    for (const b of qf.banks) {
      const s = sells.get(b.bank);
      if (!(b.buy > 0) || !(s > 0)) continue;
      const r = amount * b.buy / s;
      if (one == null || r > one) { one = r; oneBank = b.bank; }
    }
  }
  const cf = qf ? qf.cbu : 1, ct = qt ? qt.cbu : 1;
  const cbu = cf > 0 && ct > 0 ? amount * cf / ct : null;
  return { from, to, amount, result, som, sellBank: sell?.bank, sellRate: sell?.buy, buyBank: buy?.bank, buyRate: buy?.sell, one, oneBank, cbu, loss: cbu ? (cbu - result) / cbu : null };
}

/* ---------- formatting ---------- */

function wAmt(v, dec) {
  if (!dec || Math.abs(v - Math.round(v)) < 0.005) return group(v);
  const [i, f] = Math.abs(v).toFixed(dec).split(".");
  return (v < 0 ? "−" : "") + group(Number(i)) + "." + f;
}
// Group spaces inside numbers become no-break spaces, so "11 995 000" never splits across lines.
const nb = (s) => String(s).replace(/(\d) (?=\d)/g, "$1\u00A0");
const wRate = (v) => (v >= 1000 ? group(v) : v.toFixed(2));
const wSigned = (v) => (Math.round(v) > 0 ? "+" : "") + group(v);
const wParse = (s) => { const n = Number(String(s).replace(/\s/g, "").replace(",", ".")); return Number.isFinite(n) ? n : null; };
const wCodeLabel = (c) => (c === "XAU" ? WL().gold : c);
const wUnit = (c) => (c === "UZS" ? WL().som : c === "XAU" ? WL().gramsUnit : c);

let wLoaded = null;

/* ---------- render ---------- */

function wChips(codes, on, attr) {
  return `<div class="w-chips">${codes.map((c) => `<button type="button" data-${attr}="${c}" class="${c === on ? "on" : ""}">${esc(wCodeLabel(c))}</button>`).join("")}</div>`;
}

function renderWallet() {
  const root = $("walletRoot");
  if (!root) return;
  const t = WL(), list = WS.load(), s = wSummary(list);
  const ready = s.rows.every((r) => r.som != null); // before the rates arrive the total would read 0
  let html = `<div class="w-head"><h1>${esc(t.title)}</h1><p class="muted">${esc(t.sub)}</p></div>`;
  if (!list.length) {
    html += `<div class="card w-card"><h2>${esc(t.emptyT)}</h2><p class="muted">${esc(t.emptyP)}</p><button class="btn btn-accent w-add" type="button" data-wadd>${esc(t.add)}</button></div>`;
  } else {
    html += `<div class="card w-card w-hero"><p class="res-label">${esc(t.heroL)}</p>
      <div class="result"><span class="big">${ready ? group(s.total) : "…"}</span><span class="som">${esc(t.som)}</span></div>
      <div class="w-stats">
        <div class="w-stat"><small>${esc(t.day)}</small><b class="${Math.round(s.day) < 0 ? "neg" : ""}">${ready ? wSigned(s.day) : "—"}</b><em>${esc(t.atCbu)}</em></div>
        <div class="w-stat"><small>${esc(t.inUsd)}</small><b>${ready && s.usd != null ? "$" + group(s.usd) : "—"}</b><em>${esc(t.atCbu)}</em></div>
      </div></div>
      ${renderGoal(ready ? s.total : null)}
      <div class="card w-list">${s.rows.map((r) => {
        const h = r.h, c = W_CODES[h.code];
        const unit = h.code === "UZS" ? t.som : h.code === "XAU" ? t.grams : h.code;
        const meta = h.code === "UZS" ? t.cash : r.bank && r.rate ? `${h.code === "XAU" ? t.cbuBuys : shortName(r.bank)} · ${wRate(r.rate)}` : t.loading;
        const d = Math.round(r.day);
        return `<button type="button" class="w-row" data-wedit="${h.code}">
          <span class="w-sym${h.code === "XAU" ? " gold" : ""}">${esc(c.sym)}</span>
          <span class="w-mid"><b>${wAmt(h.amount, c.dec)} ${esc(unit)}</b><small>${esc(meta)}</small></span>
          <span class="w-val"><b>${r.som != null ? group(r.som) : "—"}</b>${h.code !== "UZS" && h.code !== "XAU" && d ? `<small class="${d < 0 ? "neg" : "pos"}">${wSigned(d)}</small>` : ""}</span>
        </button>`;
      }).join("")}
      <button type="button" class="w-row w-more" data-wadd><span class="w-sym plus">+</span><span class="w-mid"><b>${esc(t.addMore)}</b></span></button></div>`;
  }
  html += `<p class="muted small w-note">${esc(t.note)}</p>`;
  root.innerHTML = html;
  renderWalletSheet();
}

function renderGoal(totalSom) {
  const t = WL(), g = GS.load();
  if (!g) return `<button type="button" class="w-goal-empty" data-wgoal><span aria-hidden="true">🎯</span><span><b>${esc(t.goalSet)}</b><small>${esc(t.goalHint)}</small></span></button>`;
  const rate = totalSom == null ? null : g.code === "UZS" ? 1 : W.quotes[g.code]?.cbu;
  const u = g.code === "UZS" ? t.som : g.code;
  if (!rate) return `<button type="button" class="card w-card w-goal" data-wgoal><span class="w-goal-top"><b>${esc(g.name || t.goal)}</b><em>—</em></span></button>`;
  const saved = totalSom / rate, share = Math.max(0, Math.min(1, saved / g.amount)), left = g.amount - saved;
  return `<button type="button" class="card w-card w-goal" data-wgoal>
    <span class="w-goal-top"><b>${esc(g.name || t.goal)}</b><em>${Math.round(share * 100)}%</em></span>
    <span class="w-bar"><i style="width:${(share * 100).toFixed(1)}%"></i></span>
    <small class="muted">${group(saved)} / ${group(g.amount)} ${esc(u)}</small>
    <span class="w-goal-left${left <= 0 ? " done" : ""}">${esc(left <= 0 ? t.goalDone : t.goalLeft(group(left), u))}</span>
  </button>`;
}

function renderGoalSheet(el) {
  const t = WL(), g = GS.load();
  W.goalCode ||= g?.code || "USD";
  el.innerHTML = `<div class="sheet-grip"></div><h3>${esc(t.goal)}</h3>
    <input class="w-name" id="wGoalName" maxlength="40" autocomplete="off" placeholder="${esc(t.goalName)}" value="${esc(g?.name || "")}">
    ${wChips(GOAL_CODES, W.goalCode, "wgcode")}
    <label class="amount-row w-amt"><input id="wAmount" inputmode="decimal" autocomplete="off" value="${g ? g.amount : ""}" placeholder="0"><span class="unit">${esc(wUnit(W.goalCode))}</span></label>
    <button class="btn btn-accent w-save" type="button" data-wgsave>${esc(t.save)}</button>
    ${g ? `<button class="w-del" type="button" data-wgdel>${esc(t.goalDel)}</button>` : `<button class="w-del w-cancel" type="button" data-wclose>${esc(t.cancel)}</button>`}`;
}

function renderExchange() {
  const t = WL(), amount = wParse(W.amount) || 0, x = wExchange(W.from, W.to, amount);
  let out = `<div class="w-x"><h2>${esc(t.xT)}</h2><p class="muted">${esc(t.xP)}</p>
    <p class="w-lab">${esc(t.have)}</p>${wChips(W_XCHG, W.from, "xfrom")}
    <label class="amount-row w-amt"><input id="wxAmount" inputmode="decimal" autocomplete="off" value="${esc(W.amount)}" aria-label="${esc(t.have)}"><span class="unit">${esc(wUnit(W.from))}</span></label>
    <div class="w-lab w-swaprow"><span>${esc(t.need)}</span><button type="button" class="icon-btn" data-wswap aria-label="${esc(t.swap)}" title="${esc(t.swap)}"><svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true"><path d="M6 3v12M3 6l3-3 3 3M12 15V3M9 12l3 3 3-3" stroke="currentColor" stroke-width="1.7" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg></button></div>
    ${wChips(W_XCHG, W.to, "xto")}`;
  if (!x) {
    out += `<p class="muted w-wait">${esc(amount > 0 ? t.loading : t.enter)}</p>`;
  } else {
    const u = wUnit(x.to), dec = x.to === "UZS" ? 0 : 2;
    out += `<div class="w-res"><small>${esc(t.get)}</small><div class="result"><span class="big">${wAmt(x.result, dec)}</span><span class="som">${esc(u)}</span></div><ol>`;
    if (x.sellBank) out += `<li>${nb(esc(t.step1(shortName(x.sellBank), wAmt(x.amount, W_CODES[x.from].dec), x.from, wRate(x.sellRate), group(x.som))))}</li>`;
    if (x.buyBank) out += `<li>${nb(esc(t.step2(shortName(x.buyBank), x.to, wRate(x.buyRate))))}</li>`;
    out += `</ol>`;
    if (x.oneBank && x.one != null && x.sellBank !== x.buyBank) out += `<p>${esc(t.one(shortName(x.oneBank), wAmt(x.one, 2), u, wAmt(x.result - x.one, 2)))}</p>`;
    if (x.loss != null) out += `<p class="muted">${esc(t.cbu(wAmt(x.cbu, 2), u, (x.loss * 100).toFixed(1)))}</p>`;
    out += `</div>`;
  }
  return out + `</div>`;
}

function renderWalletSheet() {
  let el = $("walletSheet");
  const open = W.adding || W.editing || W.goalOpen || W.xOpen;
  if (!open) {
    if (el) { el.classList.remove("on"); $("walletScrim").classList.remove("on"); setTimeout(() => { if (!W.adding && !W.editing && !W.goalOpen && !W.xOpen) { el.hidden = true; $("walletScrim").hidden = true; } }, 250); }
    return;
  }
  if (!el) {
    document.body.insertAdjacentHTML("beforeend", `<div class="scrim" id="walletScrim" hidden></div><div class="sheet w-sheet" id="walletSheet" role="dialog" aria-modal="true" hidden></div>`);
    el = $("walletSheet");
  }
  if (W.xOpen) {
    el.innerHTML = `<div class="sheet-grip"></div>${renderExchange()}`;
    el.hidden = false; $("walletScrim").hidden = false;
    requestAnimationFrame(() => { el.classList.add("on"); $("walletScrim").classList.add("on"); });
    return;
  }
  if (W.goalOpen) {
    renderGoalSheet(el);
    el.hidden = false; $("walletScrim").hidden = false;
    requestAnimationFrame(() => { el.classList.add("on"); $("walletScrim").classList.add("on"); });
    return;
  }
  const t = WL(), list = WS.load();
  const code = W.editing || W.sheetCode;
  const cur = list.find((h) => h.code === code);
  const val = W.editing && cur ? String(cur.amount) : "";
  el.innerHTML = `<div class="sheet-grip"></div><h3>${esc(W.editing ? t.edit : t.addT)}</h3>
    ${W.editing ? `<p class="w-edit-code"><span class="w-sym${code === "XAU" ? " gold" : ""}">${esc(W_CODES[code].sym)}</span><b>${esc(wCodeLabel(code))}</b></p>` : wChips(W_ALL, code, "wcode")}
    ${!W.editing && cur ? `<p class="muted small">${esc(t.merge)}</p>` : ""}
    <label class="amount-row w-amt"><input id="wAmount" inputmode="decimal" autocomplete="off" value="${esc(val)}" placeholder="0"><span class="unit">${esc(wUnit(code))}</span></label>
    <button class="btn btn-accent w-save" type="button" data-wsave>${esc(t.save)}</button>
    ${W.editing ? `<button class="w-del" type="button" data-wdel>${esc(t.remove)}</button>` : `<button class="w-del w-cancel" type="button" data-wclose>${esc(t.cancel)}</button>`}`;
  el.hidden = false;
  $("walletScrim").hidden = false;
  requestAnimationFrame(() => { el.classList.add("on"); $("walletScrim").classList.add("on"); });
  setTimeout(() => $("wAmount")?.focus(), 50);
}

function closeWalletSheet() { W.adding = false; W.editing = null; W.goalOpen = false; W.xOpen = false; renderWalletSheet(); }

document.addEventListener("click", (e) => {
  const b = e.target.closest("[data-wadd],[data-wedit],[data-xfrom],[data-xto],[data-wswap],[data-wcode],[data-wsave],[data-wdel],[data-wclose],[data-wgoal],[data-wgcode],[data-wgsave],[data-wgdel],#walletScrim");
  if (!b) return;
  if (b.hasAttribute("data-wgoal")) { W.goalOpen = true; W.goalCode = null; return renderWalletSheet(); }
  if (b.dataset.wgcode) { const n = $("wGoalName")?.value, v = $("wAmount")?.value; W.goalCode = b.dataset.wgcode; renderWalletSheet(); $("wGoalName").value = n || ""; $("wAmount").value = v || ""; return; }
  if (b.hasAttribute("data-wgsave")) {
    const amount = wParse($("wAmount")?.value);
    if (!(amount > 0)) { $("wAmount")?.focus(); return; }
    GS.save({ code: W.goalCode || "USD", amount, name: ($("wGoalName")?.value || "").trim() });
    W.goalOpen = false; return renderWallet();
  }
  if (b.hasAttribute("data-wgdel")) { GS.save(null); W.goalOpen = false; return renderWallet(); }
  if (b.id === "walletScrim" || b.hasAttribute("data-wclose")) return closeWalletSheet();
  if (b.hasAttribute("data-wadd")) { W.adding = true; W.editing = null; const have = new Set(WS.load().map((h) => h.code)); W.sheetCode = W_ALL.find((c) => !have.has(c)) || "USD"; return renderWalletSheet(); }
  if (b.dataset.wedit) { W.editing = b.dataset.wedit; return renderWalletSheet(); }
  if (b.dataset.wcode) { W.sheetCode = b.dataset.wcode; const v = $("wAmount")?.value; renderWalletSheet(); if (v) $("wAmount").value = v; return; }
  if (b.hasAttribute("data-wsave")) {
    const amount = wParse($("wAmount")?.value);
    if (!(amount > 0)) { $("wAmount")?.focus(); return; }
    const code = W.editing || W.sheetCode;
    WS.save(WS.upsert(WS.load(), { code, amount }, !!W.editing));
    wNudge();
    W.adding = false; W.editing = null;
    return renderWallet();
  }
  if (b.hasAttribute("data-wdel")) { WS.save(WS.load().filter((h) => h.code !== W.editing)); W.editing = null; return renderWallet(); }
  if (b.dataset.xfrom) { if (b.dataset.xfrom === W.to) W.to = W.from; W.from = b.dataset.xfrom; }
  if (b.dataset.xto) { if (b.dataset.xto === W.from) W.from = W.to; W.to = b.dataset.xto; }
  if (b.hasAttribute("data-wswap")) [W.from, W.to] = [W.to, W.from];
  wSaveX();
  if (W.xOpen) renderWalletSheet(); else renderWallet();
});
document.addEventListener("input", (e) => {
  if (e.target.id !== "wxAmount") return;
  W.amount = e.target.value.replace(/[^\d.,\s]/g, "").slice(0, 16);
  wSaveX();
  // Re-render only the exchange card so the input keeps focus and caret.
  const card = document.querySelector(".w-x");
  const pos = e.target.selectionStart;
  card.outerHTML = renderExchange();
  const inp = $("wxAmount");
  inp.focus();
  try { inp.setSelectionRange(pos, pos); } catch { /* type=text only */ }
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && (W.adding || W.editing || W.goalOpen || W.xOpen)) closeWalletSheet();
  if (e.key === "Enter" && (e.target.id === "wAmount" || e.target.id === "wGoalName")) document.querySelector("[data-wsave],[data-wgsave]")?.click();
});
function wSaveX() { store.set("xchg_from", W.from); store.set("xchg_to", W.to); store.set("xchg_amount", W.amount); }

/** Entry under the converter to the currency-to-currency calculator (opens as a sheet, rates load on demand). */
function wXEntry() {
  document.getElementById("wXEntry")?.remove();
  const conv = document.querySelector(".conv");
  if (!conv) return;
  const t = WL();
  conv.insertAdjacentHTML("beforeend", `<button type="button" class="w-xentry" id="wXEntry" data-wxopen><svg width="22" height="22" viewBox="0 0 20 20" aria-hidden="true"><path d="M3 7h13M12.5 3.5L16 7l-3.5 3.5M17 13H4M7.5 9.5L4 13l3.5 3.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg><span><b>${esc(t.xT)}</b><small>${esc(t.xEntry)}</small></span><svg class="chev" width="16" height="16" viewBox="0 0 16 16" aria-hidden="true"><path d="M6 3.5L10.5 8 6 12.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></button>`);
  wNudge(); // keep the wallet nudge after the entry
}
document.addEventListener("click", (e) => {
  if (!e.target.closest("[data-wxopen]")) return;
  W.xOpen = true;
  renderWalletSheet();
  (wLoaded ||= wLoad()).then(() => { if (W.xOpen) renderWalletSheet(); });
});

/** Quiet card under the converter pointing to #/hamyon, until the wallet has something or the visitor closes it. */
function wNudge() {
  document.getElementById("wNudge")?.remove();
  if (!WALLET_ON || WS.load().length || store.get("wallet_nudge_off")) return;
  const conv = document.querySelector(".conv");
  if (!conv) return;
  const t = WL();
  conv.insertAdjacentHTML("beforeend", `<div class="w-nudge" id="wNudge"><a href="#/hamyon"><svg width="22" height="22" viewBox="0 0 20 20" aria-hidden="true"><rect x="2.5" y="5.5" width="15" height="11" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M4.5 5.5l8.5-2.5.8 2.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/><rect x="12" y="9" width="5.5" height="4" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.7"/></svg><span><b>${esc(t.nudgeT)}</b><small>${esc(t.nudgeP)}</small></span></a><button type="button" class="icon-btn" data-wnudge-off aria-label="${esc(t.close)}"><svg width="14" height="14" viewBox="0 0 18 18"><path d="M4 4l10 10M14 4L4 14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg></button></div>`);
}
document.addEventListener("click", (e) => {
  if (!e.target.closest("[data-wnudge-off]")) return;
  store.set("wallet_nudge_off", "1");
  wNudge();
});

function showWallet() {
  renderWallet();
  (wLoaded ||= wLoad()).then(renderWallet);
}
// app.js routed before this file loaded: catch up.
if (S.view === "wallet") showWallet();
wXEntry();
