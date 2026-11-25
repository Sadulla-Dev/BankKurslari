// Points @qulaykursbot at the Worker and sets its command menu and descriptions. Run once after the first deploy,
// and again whenever the texts below change:
//   TELEGRAM_BOT_TOKEN=… WEBHOOK_SECRET=… node setup.mjs [https://bot.qulaykurs.uz]
// WEBHOOK_SECRET must be the same value as the Worker secret of that name.
// Texts only (menu, name, descriptions, the channel description), without touching the webhook; needs only the token:
//   TELEGRAM_BOT_TOKEN=… node setup.mjs --texts
const textsOnly = process.argv.includes("--texts");
const token = process.env.TELEGRAM_BOT_TOKEN, secret = process.env.WEBHOOK_SECRET;
const base = (process.argv.slice(2).find((a) => !a.startsWith("--")) || "https://bot.qulaykurs.uz").replace(/\/$/, "");
if (!token || (!secret && !textsOnly)) {
  console.error(textsOnly ? "Set TELEGRAM_BOT_TOKEN." : "Set TELEGRAM_BOT_TOKEN and WEBHOOK_SECRET (or use --texts).");
  process.exit(1);
}

async function call(method, body) {
  const res = await fetch(`https://api.telegram.org/bot${token}/${method}`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });
  const out = await res.json();
  console.log(`${method}${body.language_code ? ` (${body.language_code})` : ""}: ${out.ok ? "ok" : out.description}`);
  if (!out.ok) process.exitCode = 1;
}

if (!textsOnly) await call("setWebhook", { url: `${base}/tg`, secret_token: secret, allowed_updates: ["message", "callback_query", "inline_query"], drop_pending_updates: true });

const commands = {
  uz: [["kurs", "Bugungi eng yaxshi kurslar"], ["ogohlantirish", "Kurs yetganda xabar berish"], ["help", "Qanday ishlatish"]],
  ru: [["kurs", "Лучшие курсы сегодня"], ["alert", "Уведомление о курсе"], ["help", "Как пользоваться"]]
};
const toList = (l) => l.map(([command, description]) => ({ command, description }));
await call("setMyCommands", { commands: toList(commands.uz) });
await call("setMyCommands", { commands: toList(commands.ru), language_code: "ru" });

// Display name: same words as the username @qulaykursbot.
await call("setMyName", { name: "Qulay Kurs Bot" });
// "About" on the bot's profile (≤120) and the text shown before /start (≤512). Both name the channel and the site with
// https://, so Telegram makes them tappable. No sample amounts here: the buttons after /start show the way.
await call("setMyShortDescription", { short_description: "29 bankda eng foydali dollar, yevro va rubl kursi.\nKanal: @qulay_kurs · Sayt: https://qulaykurs.uz" });
await call("setMyShortDescription", { short_description: "Лучший курс доллара, евро и рубля в 29 банках.\nКанал: @qulay_kurs · Сайт: https://qulaykurs.uz", language_code: "ru" });
await call("setMyDescription", {
  description:
    "Qulay Kurs Bot O‘zbekistondagi 29 bankda dollar, yevro va rubl kursini solishtiradi.\n\n" +
    "Bot nima qiladi:\n" +
    "• summangiz uchun eng ko‘p so‘m beradigan yoki eng arzon sotadigan bankni topadi\n" +
    "• kurs siz kutgan darajaga yetganda xabar beradi\n" +
    "• istalgan chatda @qulaykursbot deb yozib, kursni do‘stingizga yuborasiz\n\n" +
    "📢 Kanal @qulay_kurs: har kuni soat 8:00 da bugungi eng yaxshi kurslar\n" +
    "🌐 https://qulaykurs.uz: barcha banklar, filiallar xaritasi, kurs tarixi va Android ilova\n\n" +
    "Reklama yo‘q, ro‘yxatdan o‘tish yo‘q."
});
await call("setMyDescription", {
  language_code: "ru",
  description:
    "Qulay Kurs Bot сравнивает курсы доллара, евро и рубля в 29 банках Узбекистана.\n\n" +
    "Что умеет бот:\n" +
    "• найдёт банк, который даст больше сумов за вашу сумму или продаст дешевле\n" +
    "• сообщит, когда курс дойдёт до нужного вам значения\n" +
    "• в любом чате напишите @qulaykursbot, чтобы отправить курс другу\n\n" +
    "📢 Канал @qulay_kurs: каждый день в 8:00 лучшие курсы на сегодня\n" +
    "🌐 https://qulaykurs.uz: все банки, карта отделений, история курса и Android-приложение\n\n" +
    "Без рекламы и регистрации."
});

// The channel's own description (≤255) points to the bot. Works when the bot is a channel admin allowed to change
// channel info; otherwise this line fails and the text goes in by hand (channel → Edit → Description).
await call("setChatDescription", {
  chat_id: "@qulay_kurs",
  description:
    "Qulay Kurs: banklarda dollar kursi har kuni 8:00 da bitta rasmda, qayerda sotish va olish foydali.\n\n" +
    "🤖 Bot: @qulaykursbot, summangiz uchun eng yaxshi bank va kurs ogohlantirishi\n" +
    "🌐 Sayt: https://qulaykurs.uz, barcha banklar, xarita, Android ilova"
});
