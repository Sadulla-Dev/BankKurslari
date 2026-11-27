// Banks open on the coming weekend (or holiday), as the Central Bank announces them.
//
// Every week, usually on Thursday or Friday evening, the Central Bank posts on its Telegram channel
// (t.me/centralbankuzbekistan) a link to a telegra.ph page titled "Dam olish kunlari ayrim banklar tomonidan ... valyuta
// ayirboshlash va pul o'tkazmalari shoxobchalari ...": the banks whose offices exchange currency and pay out money
// transfers on Saturday and/or Sunday, each linked to the bank's own list of those offices (an HTML page, PDF, xlsx or
// picture, different for every bank). The banks change from week to week.
//
// This reads the channel's public web preview (t.me/s/...), finds the newest such post, and reads the page through the
// telegra.ph API. Bank-level only: the offices themselves are on the linked pages.

const CHANNEL = "centralbankuzbekistan";
const UA = "Mozilla/5.0 (compatible; QulayKurs/1.0; +https://qulaykurs.uz)";
// telegra.ph slugs of the weekly list ("Dam-oliş-kunlari-ayrim-banklar-...") and of holiday lists ("Bayram-...").
const LIST_SLUG = /^(dam-oli|bayram)/i;

async function text(url) {
  const res = await fetch(url, { headers: { "User-Agent": UA }, signal: AbortSignal.timeout(20000) });
  if (!res.ok) throw new Error(`${new URL(url).host}: HTTP ${res.status}`);
  return res.text();
}

const decode = (s) => s.replace(/&amp;/g, "&").replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, "<").replace(/&gt;/g, ">");

/** Newest posts first: [{ id, at (ISO), listUrl }] for the posts that link a weekend/holiday list. */
function listPosts(html) {
  const out = [];
  for (const part of html.split(`data-post="${CHANNEL}/`).slice(1)) {
    const id = Number(part.slice(0, part.indexOf('"')));
    const at = /datetime="([^"]+)"/.exec(part)?.[1];
    for (const [, href] of part.matchAll(/href="(https:\/\/telegra\.ph\/[^"]+)"/g)) {
      const slug = decodeURIComponent(decode(href).split("/").pop());
      if (LIST_SLUG.test(slug)) { out.push({ id, at, listUrl: decode(href) }); break; }
    }
  }
  return out.sort((a, b) => b.id - a.id);
}

// The Central Bank writes in the new Latin alphabet ("Özmilliybank", "Biznesni rivojlantiriş banki"); the site and the
// bank lists use the current one.
const NEW_LATIN = { ö: "o‘", Ö: "O‘", ğ: "g‘", Ğ: "G‘", ş: "sh", Ş: "Sh", ç: "ch", Ç: "Ch" };
export const oldLatin = (s) => s.replace(/[öÖğĞşŞçÇ]/g, (c) => NEW_LATIN[c]);

/** The bank links of a telegra.ph page: [{ name, url }], in the page's order, one per bank. */
function bankLinks(content) {
  const out = [];
  const walk = (node) => {
    if (typeof node === "string" || !node) return;
    if (node.tag === "a" && node.attrs?.href) {
      const name = oldLatin((node.children || []).filter((c) => typeof c === "string").join("").replace(/\s+/g, " ").trim());
      if (name && /^https?:\/\//.test(node.attrs.href) && !out.some((b) => b.name === name)) out.push({ name, url: node.attrs.href });
      return;
    }
    for (const c of node.children || []) walk(c);
  };
  for (const n of content || []) walk(n);
  return out;
}

// Tashkent is UTC+5 all year.
const tashkentDate = (ms) => new Date(ms + 5 * 3600 * 1000).toISOString().slice(0, 10);

/**
 * Saturday and Sunday the list is for: the weekend right after it was posted. The weekly list goes up on Thursday or
 * Friday evening (a list posted on Saturday or Sunday is for that same weekend). Posted on another day, or a holiday
 * list ("Bayram ..."), it may be for other dates: null then, and the site shows the list without dates rather than
 * guessing them.
 */
export function weekendAfter(iso, slug) {
  if (/^bayram/i.test(slug)) return null;
  const day = new Date(`${tashkentDate(Date.parse(iso))}T00:00:00Z`);
  const dow = day.getUTCDay(); // 0 Sunday … 6 Saturday
  if (dow >= 1 && dow <= 3) return null;
  const sat = day.getTime() + (dow === 0 ? -1 : 6 - dow) * 86400000;
  return [sat, sat + 86400000].map((ms) => new Date(ms).toISOString().slice(0, 10));
}

/**
 * { title, publishedAt, postUrl, listUrl, days: ["YYYY-MM-DD" Saturday, Sunday] | null, banks: [{ name, url }] }.
 * Throws when no list is found among the channel's recent posts or the list has too few banks, so the build keeps the
 * previous file.
 */
export async function getWeekendBanks() {
  const posts = listPosts(await text(`https://t.me/s/${CHANNEL}`));
  if (!posts.length) throw new Error("no weekend list among the Central Bank's recent Telegram posts");
  const post = posts[0];
  const path = new URL(post.listUrl).pathname.slice(1);
  const res = await fetch(`https://api.telegra.ph/getPage/${path}?return_content=true`, { headers: { "User-Agent": UA }, signal: AbortSignal.timeout(20000) });
  if (!res.ok) throw new Error(`telegra.ph: HTTP ${res.status}`);
  const page = await res.json();
  if (!page.ok) throw new Error(`telegra.ph: ${page.error}`);
  const banks = bankLinks(page.result.content);
  if (banks.length < 3) throw new Error(`only ${banks.length} banks on ${post.listUrl}`);
  return {
    title: oldLatin(page.result.title),
    publishedAt: post.at,
    postUrl: `https://t.me/${CHANNEL}/${post.id}`,
    listUrl: post.listUrl,
    days: weekendAfter(post.at, decodeURIComponent(path)),
    banks,
  };
}
