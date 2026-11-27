// The first worksheet of an .xlsx file as rows of cell texts, with no dependency: an .xlsx is a zip of XML files, and
// node:zlib inflates zip entries. Enough for the banks' weekend lists (src/weekendBranches.js): strings, numbers and
// merged cells; no formulas are evaluated (their cached value is read), dates stay Excel serial numbers.
import zlib from "node:zlib";

/** { path: Buffer } of a zip archive (stored and deflated entries, read from the central directory). */
function unzip(buf) {
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error("xlsx: not a zip file");
  const count = buf.readUInt16LE(eocd + 10);
  let p = buf.readUInt32LE(eocd + 16);
  const out = {};
  for (let n = 0; n < count; n++) {
    if (buf.readUInt32LE(p) !== 0x02014b50) throw new Error("xlsx: bad central directory");
    const method = buf.readUInt16LE(p + 10), size = buf.readUInt32LE(p + 20);
    const nameLen = buf.readUInt16LE(p + 28), extraLen = buf.readUInt16LE(p + 30), commentLen = buf.readUInt16LE(p + 32);
    const local = buf.readUInt32LE(p + 42);
    const name = buf.toString("utf8", p + 46, p + 46 + nameLen);
    const start = local + 30 + buf.readUInt16LE(local + 26) + buf.readUInt16LE(local + 28);
    const raw = buf.subarray(start, start + size);
    if (method === 0) out[name] = raw;
    else if (method === 8) out[name] = zlib.inflateRawSync(raw, { maxOutputLength: 64 * 1024 * 1024 });
    p += 46 + nameLen + extraLen + commentLen;
  }
  return out;
}

const unescape = (s) => s.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&apos;/g, "'")
  .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(Number(d))).replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCodePoint(parseInt(h, 16)))
  .replace(/&amp;/g, "&");
/** The text of every <t> inside an XML fragment (a shared string can be split into runs). */
const runs = (xml) => [...xml.matchAll(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g)].map((m) => unescape(m[1])).join("");
/** "C12" -> [11, 2] (row, column, from 0). */
function ref(r) {
  const m = /^([A-Z]+)(\d+)$/.exec(r);
  let col = 0;
  for (const ch of m[1]) col = col * 26 + ch.charCodeAt(0) - 64;
  return [Number(m[2]) - 1, col - 1];
}

/** Rows of the first sheet: string[][], empty cells "". A merged range repeats its top-left text in every cell. */
export function readXlsx(buf) {
  const files = unzip(buf);
  const text = (p) => (files[p] ? files[p].toString("utf8") : "");
  const shared = [...text("xl/sharedStrings.xml").matchAll(/<si>([\s\S]*?)<\/si>/g)].map((m) => runs(m[1]));
  // The first sheet in workbook order (sheet1.xml is not always it).
  const rid = /<sheet\b[^>]*r:id="([^"]+)"/.exec(text("xl/workbook.xml"))?.[1];
  const target = rid && new RegExp(`<Relationship\\b[^>]*Id="${rid}"[^>]*Target="([^"]+)"`).exec(text("xl/_rels/workbook.xml.rels"))?.[1]
    || new RegExp(`Target="([^"]+)"[^>]*Id="${rid}"`).exec(text("xl/_rels/workbook.xml.rels"))?.[1];
  const sheetPath = target ? (target.startsWith("/") ? target.slice(1) : `xl/${target}`) : "xl/worksheets/sheet1.xml";
  const sheet = text(sheetPath) || text("xl/worksheets/sheet1.xml");
  if (!sheet) throw new Error("xlsx: no worksheet");
  const rows = [];
  for (const c of sheet.matchAll(/<c\b([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
    const attrs = c[1], body = c[2] || "";
    const r = /\br="([A-Z]+\d+)"/.exec(attrs)?.[1];
    if (!r) continue;
    const type = /\bt="([^"]+)"/.exec(attrs)?.[1];
    const v = /<v>([\s\S]*?)<\/v>/.exec(body)?.[1];
    let value = "";
    if (type === "s") value = shared[Number(v)] ?? "";
    else if (type === "inlineStr") value = runs(body);
    else if (v != null) value = unescape(v);
    const [row, col] = ref(r);
    (rows[row] ||= [])[col] = value.trim();
  }
  for (const m of sheet.matchAll(/<mergeCell\b[^>]*ref="([A-Z]+\d+):([A-Z]+\d+)"/g)) {
    const [r1, c1] = ref(m[1]), [r2, c2] = ref(m[2]);
    const v = rows[r1]?.[c1] ?? "";
    for (let r = r1; r <= r2; r++) for (let c = c1; c <= c2; c++) if (!(r === r1 && c === c1)) (rows[r] ||= [])[c] = v;
  }
  const width = Math.max(0, ...rows.filter(Boolean).map((r) => r.length));
  return Array.from(rows, (r) => Array.from({ length: width }, (_, i) => r?.[i] ?? ""));
}
