// Points @qulaykursbot at the Worker and sets its command menu and descriptions. Run once after the first deploy,
// and again whenever the texts below change:
//   TELEGRAM_BOT_TOKEN=… WEBHOOK_SECRET=… node setup.mjs [https://bot.qulaykurs.uz]
// WEBHOOK_SECRET must be the same value as the Worker secret of that name.
const token = process.env.TELEGRAM_BOT_TOKEN, secret = process.env.WEBHOOK_SECRET;
const base = (process.argv[2] || "https://bot.qulaykurs.uz").replace(/\/$/, "");
if (!token || !secret) {
  console.error("Set TELEGRAM_BOT_TOKEN and WEBHOOK_SECRET.");
  process.exit(1);
}

async function call(method, body) {
  const res = await fetch(`https://api.telegram.org/bot${token}/${method}`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });
  const out = await res.json();
  console.log(`${method}${body.language_code ? ` (${body.language_code})` : ""}: ${out.ok ? "ok" : out.description}`);
  if (!out.ok) process.exitCode = 1;
}

await call("setWebhook", { url: `${base}/tg`, secret_token: secret, allowed_updates: ["message", "callback_query", "inline_query"], drop_pending_updates: true });

const commands = {
  uz: [["kurs", "Bugungi eng yaxshi kurslar"], ["ogohlantirish", "Kurs yetganda xabar berish"], ["help", "Qanday ishlatish"]],
  ru: [["kurs", "Лучшие курсы сегодня"], ["alert", "Уведомление о курсе"], ["help", "Как пользоваться"]]
};
const toList = (l) => l.map(([command, description]) => ({ command, description }));
await call("setMyCommands", { commands: toList(commands.uz) });
await call("setMyCommands", { commands: toList(commands.ru), language_code: "ru" });

// Display name: same words as the username @qulaykursbot.
await call("setMyName", { name: "Qulay Kurs Bot" });
await call("setMyShortDescription", { short_description: "29 bank kursi: summangizni yozing, eng ko‘p so‘m beradigan bankni ko‘rsataman. qulaykurs.uz" });
await call("setMyShortDescription", { short_description: "Курсы 29 банков: напишите сумму, покажу, где выгоднее обменять. qulaykurs.uz", language_code: "ru" });
await call("setMyDescription", {
  description:
    "Qulay Kurs: O‘zbekistondagi banklarda dollar, yevro va rubl kursini solishtiradi.\n\n" +
    "• 500 dollar deb yozing: qaysi bank ko‘p so‘m beradi\n• dollar 12 100 dan oshsa: kurs yetganda xabar beraman\n" +
    "• istalgan chatda @qulaykursbot 100 dollar\n\nReklama yo‘q, ro‘yxatdan o‘tish yo‘q."
});
await call("setMyDescription", {
  language_code: "ru",
  description:
    "Qulay Kurs: сравнивает курсы доллара, евро и рубля в банках Узбекистана.\n\n" +
    "• напишите 500 долларов: какой банк даст больше сумов\n• доллар выше 12 100: сообщу, когда курс дойдёт\n" +
    "• в любом чате @qulaykursbot 100 долларов\n\nБез рекламы и регистрации."
});
