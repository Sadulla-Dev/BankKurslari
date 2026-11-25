# Qulay Kurs

O‘zbekistonda valyuta (USD, EUR, RUB) ayirboshlash uchun yordamchi mobil ilova.

**Asosiy savol, ilova javob beradigan:** *“Mening pulim bor. Qaysi bank menga eng ko‘p so‘m beradi (yoki valyutani eng arzon sotadi), u qayerda va unga qanday boraman?”*

Ilova bank kurslarini, Markaziy bank (MB) kursini va bank filiallarini bitta joyda ko‘rsatadi, foydalanuvchi summasi bo‘yicha banklarni saralaydi va eng yaxshi bankka marshrut quradi.

- Platformalar: **Android** va **iOS** (Compose Multiplatform, bitta umumiy kod)
- Tillar: **o‘zbekcha (lotin)**, **ruscha**
- Mavzu: yorug‘ / qorong‘i / tizim bo‘yicha
- Valyutalar: **USD, EUR, RUB**

---

## 1. Ekranlar va funksiyalar

Ilova to‘rtta tabdan iborat: **Kurs**, **Xarita**, **Tarix**, **Hamyon**, hamda Sozlamalar oynasi.

### 1.1 Kurs (bosh ekran)

“Qayerda almashtirsam foydali?” savoliga bir qarashda javob.

- **Valyuta kartalari (tepada):** USD, EUR, RUB, har birida MB kursi va kechagiga nisbatan o‘zgarish.
  - Tanlangan karta kattalashadi va 30 kunlik mini-grafik (sparkline) ko‘rsatadi.
  - Kartani bosish yoki chap-o‘ngga surish orqali valyuta almashadi.
  - Yuqorida ma’lumot yangilangan vaqti va “yangi” belgisi (yashil nuqta, yangi kurs kelganda 3 marta pulsatsiya qiladi; eskirsa yoki oflaynda sariq).
- **Valyuta tanlash (summa kartasidagi tugma):** USD, EUR, RUB, keyin “Banklarda yana” — naqd almashtiradigan banklar soni bilan GBP, CHF, JPY, KZT, CNY (bank.uz, 21.11.2026: 25 / 23 / 22 / 7 / 2 bank; boshqa hech bir valyutani banklar almashtirmaydi).
  - Faqat kursi qurilmada bor va kamida bitta bank almashtiradigan valyuta ko‘rsatiladi.
  - Bu valyutalar Kurs va Xaritada ishlaydi; Tarix, vidjetlar va ogohlantirishlar faqat USD/EUR/RUB uchun, tanlov saqlanmaydi.
  - Oxirgi band — “Markaziy bank kurslari”: barcha 74 valyuta (ma’lumot uchun).
  - Bank kurslari kasr bo‘lishi mumkin (KZT: 26,1 / 26,9), shuning uchun kurslar `Double`.
- **Yo‘nalish:** “Men sotaman” / “Men olaman”, foydalanuvchi tilida.
- **Summa kiritish:** katta raqam, minglik ajratish bilan; tezkor tugmalar 100 / 500 / 1 000 / 5 000 (RUB, JPY, KZT uchun 10 000 / 50 000 / …; CNY 500 / 1 000 / …). Oxirgi summa eslab qolinadi.
- **Eng yaxshi natija (hero):** “Qo‘lingizga tegadi” yoki “To‘lashingiz kerak” summasi so‘mda, bank nomi, 1 birlik kursi, masofa va ish vaqti.
  - **Yo‘nalish** tugmasi (marshrut) va **Ulashish** tugmasi.
- **Barcha banklar ro‘yxati:** summa bo‘yicha saralangan.
  - Har qatorda: o‘rin, bank, jami so‘m, kurs, masofa, ish vaqti, eng yaxshidan farq.
  - Saralash: so‘m, masofa, kurs, hozir ochiq.
  - Filtrlar: “Ochiq hozir”, “Tanlanganlar”.
  - Bank nomi bo‘yicha qidiruv.
  - Uzoq bosish: tanlanganlarga qo‘shish/olib tashlash.
  - Qatorni bosish: bank oynasi (pastga qarang).
- **MB kursi** ro‘yxat oxirida ma’lumot sifatida.
- **Holatlar:** yuklanish skeleti, oflayn rejim (oxirgi saqlangan kurslar va vaqti), eskirgan ma’lumot ogohlantirishi, joylashuv yo‘qligida masofasiz ishlash.
- **Valyutani valyutaga almashtirish** (summa kartasi ostidagi qator, pastdan oyna bo‘lib ochiladi): masalan USD → EUR. Banklar to‘g‘ridan-to‘g‘ri almashtirmaydi, so‘m orqali: eng foydali yo‘l (bir bankda sotish, boshqasida olish), ikkalasi bitta bankda bo‘lsa natija, MB kursi bo‘yicha qiymat va banklar farqi (%). Kod: `Hamyon.kt` → `ExchangeSheet`; web: `wallet.js` → `wXEntry()`.
- **Pastga tortib yangilash** (pull to refresh).

### 1.2 Bank oynasi (bottom sheet)

Kurs ro‘yxatidan ham, xaritadan ham ochiladi.

- Bank nomi, manzil, masofa, ish vaqti (hozir ochiq/yopiq va jadval).
- Sotib olish va sotish kurslari, ular orasidagi farq (spred).
- Foydalanuvchi summasi uchun natija: “Sizning 100 USD: 1 196 000 so‘m”.
- **Yo‘nalish**, **Ulashish**, **Tanlanganlarga qo‘shish** (yulduzcha).

### 1.3 Marshrut tanlash

“Yo‘nalish” bosilganda ilova to‘g‘ridan-to‘g‘ri bitta xaritani ochmaydi, qaysi ilovada ochishni so‘raydi:

- Yandex Maps, Yandex Navigator, 2GIS, Google Maps, Waze, boshqa ilova (iOS’da Apple Maps).
- O‘rnatilgan ilovalar tepada; o‘rnatilmaganlari brauzerda ochiladi.
- “Har doim shu ilovada ochilsin” tanlovi; Sozlamalarda o‘zgartiriladi.

### 1.4 Xarita

“Yaqin atrofda qaysi filialda yaxshi kurs bor?”

- Bank filiallari xaritada **kurs yozilgan pinlar** bilan.
  - Yashil: eng yaxshi kursga yaqin (±5 so‘m); oq: boshqa; kulrang uzuq chiziq: hozir yopiq.
- **Pin yorlig‘i rejimi:** Kurs / Bank / **Ikkalasi** (xarita ochilganda “Ikkalasi”).
- **Klasterlash:** yaqin pinlar ustma-ust tushmasligi uchun guruhlanadi; guruhda eng yaxshi kurs va soni (“11 905 · 40”). Guruhni bosish yaqinlashtiradi; ajratib bo‘lmaydigan guruhda eng yaxshi bankning oynasi ochiladi.
- **Filtrlar:** Ochiq hozir, Eng yaxshi, Bank tanlash, Bankomatlar, Ro‘yxat ko‘rinishi.
- Joylashuvim tugmasi; joylashuv aniqlanmasa shahar tanlash.
- Xaritada bankni bosish → bank oynasi.
- Android: OpenStreetMap/Esri xaritasi (osmdroid). iOS: Apple MapKit.

### 1.5 Tarix

“Bugungi kurs odatdagidanmi?” Faqat faktlar, prognoz yo‘q.

- **Interaktiv grafik** (MB kursi):
  - Oraliqlar: 7k, 30k, 90k, 1y, 5y, **Max (2009-yildan)**.
  - Bir barmoq: grafik bo‘ylab yurgizib, kunlik qiymat pufakchada (sana, kurs, bugungacha farq).
  - Ikki barmoq: kattalashtirish va surish.
  - Pastda butun tarix mini-grafigi; ko‘rinadigan oynani surish, cho‘zish yoki bosib sakrash.
  - **“1 USD / mening summam”** rejimi: grafik va pufakcha so‘mda (“14.09 da 100 USD = 1 176 500 so‘m, bugun +19 800”).
  - Ko‘rinayotgan davr uchun o‘zgarish sarlavhada.
- **Bugun bozorda:** eng yaxshi sotib oluvchi va eng arzon sotuvchi bank.
- **Markaziy bank oltin quymalari:** 5–100 g quymalarning sotish va qaytarib sotib olish narxlari, 1 gramm narxi, “sotib olib darhol qaytarsangiz yo‘qotish” foizi; qatorni bosganda qadog‘i buzilgan holatdagi narx. Manba: cbu.uz, oflaynda ham ko‘rinadi.
- **Ogohlantirishlar:** “USD 12 000 dan oshsa xabar ber” — bozordagi eng yaxshi yoki aniq bank bo‘yicha, ≥ / ≤ shart, ko‘pi bilan 5 ta.
- Ma’lumot manbalari (bank.uz, cbu.uz) va yangilangan vaqt.

### 1.6 Hamyon

“Jamg‘armam bugun qancha turadi?” — har kuni ochishga sabab.

- Foydalanuvchi dollar, yevro, rubl, funt, tenge, so‘m va oltin (gramm) miqdorini kiritadi; ma’lumot faqat telefonda (web’da brauzerda) saqlanadi.
- **Jami qiymat** so‘mda: bugun eng ko‘p to‘laydigan bankning naqd kursi bo‘yicha (oltin: MB qaytarib sotib olish narxi, 10 g quyma).
- **Kecha bilan farq** (MB kursi o‘zgarishi bo‘yicha) va **dollardagi qiymat**.
- Har qatorda: miqdor, bank va kurs, so‘mdagi qiymat, kunlik o‘zgarish. Bosish — o‘zgartirish yoki o‘chirish.
- Hamyon bo‘sh bo‘lsa, Kurs ekranida unga taklif kartasi (yopish mumkin).
- **Maqsad:** masalan “Mashina — 20 000 $”: progress bar, yig‘ilgan summa va yana qancha kerakligi (USD, EUR yoki so‘mda).
- Kod: `Wallet.kt` (hisob-kitob, saqlash), `Hamyon.kt` (ekran); web: `web/public/wallet.js` (`#/hamyon`), hisob-kitob bir xil.
- Web oflayn ishlaydi (`web/public/sw.js`): sahifa va oxirgi kurslar keshda, internet bo‘lmasa shular ko‘rsatiladi.

### 1.7 Sozlamalar

- Til: O‘zbekcha / Русский
- Mavzu: Tizim / Yorug‘ / Qorong‘i
- Standart valyuta
- Shahar
- Marshrut ilovasi: har safar so‘rash yoki doim bitta ilova
- Ogohlantirishlar ro‘yxatiga o‘tish
- Ma’lumot manbalari va yangilanish tartibi
- Fikr bildirish (e-mail)

### 1.8 Android’ga xos
- **Baho so‘rash** (Play In-App Review; iOS’da StoreKit): ilova kamida 3 xil kunda ochilgan bo‘lsa, summa kiritilgandan 4 soniya keyin. Ko‘pi bilan 3 marta, 120 kun oralig‘ida; dialogni ko‘rsatish-ko‘rsatmaslikni Play o‘zi hal qiladi. Kod: `ReviewPrompt.kt`.

- **Uy ekrani vidjeti:** tanlangan valyuta bo‘yicha eng yaxshi sotish/sotib olish kursi va banklar, MB kursi.
- **Hamyon vidjeti:** jamg‘arma qiymati va kechagidan farqi; bosilsa Hamyon ochiladi.
- **Ilova yorliqlari** (ikonkani bosib turish): Hamyon, Xarita, Tarix — to‘g‘ridan-to‘g‘ri shu tabga.
- **Haftalik xulosa:** yakshanba soat 10:00 dan keyin bitta bildirishnoma — dollar haftada qancha o‘zgardi, hamyon haftalik farqi, maqsadga necha foiz. Bosilsa Hamyon ochiladi.
- Hamyon vidjeti 2 qator balandlikda maqsad progressini ham ko‘rsatadi.
- **Fon yangilanishi** (WorkManager, ~30 daqiqada): vidjetlarni yangilaydi, ogohlantirish shartlarini tekshiradi.
- **Kunlik xulosa:** MB yangi kursni e’lon qilganda bitta bildirishnoma — kurs va o‘zgarish, eng yaxshi sotish/olish banklari, hamyon qiymati va kunlik farqi.

---

## 2. Dizayn tamoyillari

- Har ekranda bitta asosiy qaror; raqamlar asosiy.
- Yo‘nalish har doim foydalanuvchi tilida (“Men sotaman”), bankniki emas.
- Har bir raqamda birlik va vaqt bor; prognoz va “hozir sotmang” kabi maslahat yo‘q.
- Bitta urg‘u rangi (yashil = eng yaxshi / asosiy amal), tekis yuzalar, ingichka chiziqlar.
- Tabular raqamlar, 4 ta matn o‘lchami (32 / 20 / 16 / 13).
- Harakat: 150–250 ms, siljiydigan segment tanlagichlar, tab almashinuvida fade-through, ro‘yxat qayta tartiblanishi animatsiyasi.

Batafsil spetsifikatsiya: `redesign.md` (loyiha tashqarisida).

---

## 3. Texnik tuzilma

```
Bankkurslari/
├── composeApp/          umumiy kod (Kotlin Multiplatform + Compose Multiplatform)
│   ├── commonMain/      barcha ekranlar, logika, tarmoq, saralash, grafiklar
│   ├── androidMain/     saqlash, ulashish, marshrut, joylashuv, osmdroid xarita
│   └── iosMain/         saqlash, ulashish, marshrut, CoreLocation, MapKit xarita
├── androidApp/          Android ilova: MainActivity, vidjet, fon ishchisi, bildirishnomalar
├── iosApp/              iOS ilova: SwiftUI qobiq, Xcode loyihasi
└── backend/             Node.js server (Express)
```

### Asosiy texnologiyalar

| Soha | Texnologiya |
|---|---|
| UI | Compose Multiplatform 1.11, Material 3 |
| Til | Kotlin 2.4 |
| Tarmoq | Ktor (Android: android engine, iOS: Darwin) |
| JSON | kotlinx.serialization |
| Sana/vaqt | kotlinx-datetime |
| Saqlash | Sozlamalar: SharedPreferences / NSUserDefaults; katta JSON kesh: fayllar |
| Xarita | Android: osmdroid + Esri tiles; iOS: MapKit |
| Fon ishlari | Android WorkManager |

### Umumiy koddagi asosiy fayllar

| Fayl | Vazifa |
|---|---|
| `App.kt`, `AppShell.kt` | Ilova ildizi, tablar, ma’lumot yuklash |
| `Kurs.kt`, `TickerBar.kt`, `BankSheet.kt` | Kurs ekrani, valyuta kartalari, bank oynasi |
| `MapScreen.kt`, `MapModel.kt` | Xarita UI, pinlar, klasterlash, pin rasmlari |
| `Tarix.kt`, `HistoryChart.kt` | Tarix ekrani, interaktiv grafik |
| `Ranking.kt`, `OpeningHours.kt` | Banklarni saralash, ish vaqtini tahlil qilish |
| `Data.kt`, `RateService.kt`, `Alerts.kt` | Tarmoq, kesh, sozlamalar, ogohlantirishlar |
| `RoutePicker.kt`, `SettingsSheet.kt` | Marshrut tanlash, sozlamalar |
| `Platform.kt` | Platformaga xos funksiyalar uchun `expect` e’lonlar |

### Backend

Node.js + Express, port 8080. Ma’lumot manbalari: **bank.uz** (tijorat banklari kurslari), **cbu.uz** (MB kursi va arxivi), OpenStreetMap / Yandex (filiallar, bankomatlar).
USD, EUR, RUB, GBP, KZT kurslari bank.uz’ning umumiy taqqoslash sahifasidan; CHF, JPY, CNY uchun bunday sahifa yo‘q, ular har bir bankning o‘z sahifasidan (`/uz/currency/bank/<bank>`, ~30 sahifa, 4 tadan parallel) yig‘iladi (`server.js`: `PER_BANK_RATES`). Hammasi har soatda yangilanadi. Zaxira: bank.uz USD bermasa, kurslar 29 ta bankning o‘z saytidan olinadi (`backend/src/bankSites.js`, kuniga 4 marta: 1, 8, 13, 20 da); bank.uz ishlaganda ham sutkada bir marta solishtiriladi. Manbalar: `backend/BANK_RATE_SOURCES.md`.
Muammolar (bank kurs bermay qoldi, bank.uz ishlamayapti, kurslar eskirdi, workflow yiqildi) faqat egasiga Telegram’da shaxsiy xabar bo‘lib keladi: `backend/src/alerts.js`; kerakli secret’lar: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_OWNER_CHAT_ID`.
Himoya: bank.uz ishlamasa, bloklasa yoki sahifasini o‘zgartirsa, banklar soni avvalgisining yarmidan kam chiqadi — bunday javob yozilmaydi, oxirgi yaxshi kurslar qoladi (eski vaqti bilan, ilova uni “eskirgan” deb ko‘rsatadi).

| Endpoint | Tavsif |
|---|---|
| `GET /health` | Server holati |
| `GET /api/rates/:cur` | MB kursi + barcha banklar kurslari (`usd`, `eur`, `rub`), 15 daqiqalik kesh |
| `GET /api/history/:cur` | MB kursi tarixi 2009-yildan (kunlik → haftalik → oylik), diskda keshlanadi |
| `GET /api/branches` | Bank filiallari (manzil, ish vaqti, koordinata) |
| `GET /api/atms` | Bankomatlar |
| `GET /api/gold` | MB oltin quymalari narxlari (cbu.uz), 1 soatlik kesh |
| `GET /api/analytics/:cur` | Eski prognoz endpointi — ilova endi ishlatmaydi |

### Telegram bot va hisoblagichlar (`backend/bot`)

Alohida Worker `qulaykurs-bot` (`bot.qulaykurs.uz`), API Worker'dan ajratilgan: u har soat CI orqali qayta deploy bo‘ladi va buzilmasligi kerak. Qo‘lda deploy qilinadi; sozlash: `backend/bot/README.md`.

- **@qulaykursbot**: “500 dollar”, “1000 yevro sotib olaman”, “5 mln so‘m dollar”, “kurs” → eng yaxshi 5 bank, valyuta/yo‘nalish tugmalari, saytga havola. Inline rejim: istalgan chatda `@qulaykursbot 1000 usd`.
- **Ogohlantirishlar** (“dollar 12 100 dan oshsa”), soatda bir tekshiriladi, `Alerts.kt` qoidasi bilan. iOS va web foydalanuvchilari uchun yagona yo‘l.
- **Kanalga qo‘shimcha post**: eng yaxshi kurs ertalabkidan keskin o‘zgarsa (USD 0,4%), kuniga ko‘pi bilan bitta.
- **Anonim hisoblagichlar** `POST /e`: ilova (`Usage.kt`) va sayt (`web/public/ev.js`) kunlik jami sonni yuboradi, identifikatorsiz. Egasi botga `/stats` yozadi.
- Sayt: shahar sahifalari (`/shahar/<shahar>`), har sahifada Google Play va bot tugmalari.
- Sayt → ilova: har sahifa sarlavhasida “Android ilova” tugmasi; Android telefonda tepada yopiladigan banner (30 kun eslab qoladi). iPhone’da ikkalasi yashiriladi (iOS ilova yo‘q). Kod: `web/public/app-link.js`, havolalarda `utm_medium=header|banner`.
- Bot va kanal tavsiflari (`setup.mjs`): bot “about” va /start oldidagi matnda kanal va sayt, kanal tavsifida bot. Faqat matnlarni yangilash: `TELEGRAM_BOT_TOKEN=… node setup.mjs --texts` (webhook’ga tegmaydi).
- Ilova: bank oynasidagi “Ulashish” rasm (`ShareCard.kt`) va havolali matn yuboradi.

---

## 4. Ishga tushirish

### Backend
```bash
cd backend
npm install
node src/server.js        # http://localhost:8080
```
Server manzili `composeApp/.../Models.kt` da: Android emulator avtomatik `10.0.2.2`, iOS simulyator `localhost`, haqiqiy telefonlar `SERVER_LAN` (kompyuterning lokal tarmoq IP manzili — tarmoq o‘zgarsa yangilash kerak).

### Android
```bash
./gradlew :androidApp:installDebug
```
Debug build juda sekin (ishga tushish 1,5–7 s, ANR bo‘lishi mumkin) — tezlikni faqat release kodida baholang. Release’da R8 yoqilgan (`isMinifyEnabled` + `isShrinkResources`); WorkManager reflection bilan yaratadigan klasslar uchun keep qoidalari `androidApp/proguard-rules.pro` da — ularsiz ilova ishga tushishda yiqiladi yoki fon yangilanishi umuman ishlamaydi.

Tezlikni o‘lchash: `benchmark` build turi — release kodi, debug kalit bilan imzolangan, `uz.qulaykurs.app.bench` nomi bilan asosiy ilova yonida o‘rnatiladi:
```bash
./gradlew :androidApp:installBenchmark
adb shell am start -W -n uz.qulaykurs.app.bench/com.example.bankkurslari.MainActivity
```

### iOS
`iosApp/iosApp.xcodeproj` ni Xcode’da oching, Signing’da jamoani tanlang va Run. Tez ishlashi uchun **Release** konfiguratsiyasida yig‘ing (Debug’da Kotlin kodi optimallashtirilmagan).

---

## 5. Hozirgi holat va cheklovlar

- **Server lokal kompyuterda ishlaydi** — ilova faqat shu kompyuter bilan bir Wi‑Fi tarmog‘ida ishlaydi. Ommaga chiqarishdan oldin server internetga joylanishi kerak.
- Android Play Store package nomi (`applicationId`): `uz.qulaykurs.app`. Ichki `namespace` va Kotlin package nomlari `com.example.bankkurslari` bo‘lib qoladi.
- iOS’da vidjet va fon ogohlantirishlari yo‘q (ogohlantirishlar faqat Android’da fonda ishlaydi).
- Bank kurslari bank.uz saytidan olinadi; tijorat foydalanish uchun manba bilan kelishuv kerak.
- Tillar: faqat o‘zbek (lotin) va rus; kirill va ingliz hali yo‘q.

---

## 6. Rivojlantirish g‘oyalari

**Chiqarishdan oldin:** serverni internetga joylash, Play Store’ga tayyorlash (paket nomi, maxfiylik siyosati), crash va analitika.

**Foydalanuvchilarni jalb qilish:** Telegram kanal va bot (kunlik eng yaxshi kurslar, ogohlantirishlar), server tomonidagi push-ogohlantirishlar, chiroyli ulashish kartalari.

**Daromad:** pul o‘tkazmalarini solishtirish (Rossiya → O‘zbekiston) va omonat/kartalarni solishtirish (hamkorlik, har bir mijoz uchun to‘lov), “Reklama” belgili homiy bank, AdMob banner (butun ekran reklamasiz), oltin narxlari, Premium (reklamasiz, cheksiz ogohlantirishlar, qo‘shimcha valyutalar).
