# Bank kurslari: banklarning o‘z saytlari (bank.uz uchun zaxira)

Asosiy manba bank.uz, har soatda o‘qiladi. Bu hujjat bank.uz ishlamay qolsa yoki bizni bloklasa, kurslarni banklarning o‘z saytlaridan olish uchun. Zaxira rejimida saytlar kuniga 4 marta o‘qiladi: Toshkent vaqti bilan 1:00, 8:00, 13:00 va 20:00.

Tekshiruv sanasi: **21–22.11.2026**, 29 bank (bank.uz ro‘yxatidagi hammasi). **29 tasining hammasidan kursni brauzersiz, oddiy so‘rov bilan olish mumkin.**

- So‘rovlar Chrome brauzeri user-agent’i bilan yuborildi, backend ham shunday ishlaydi.
- Har bir manbadagi bugungi USD olish/sotish kursi bank.uz’dagi aynan shu bank kursi bilan solishtirildi. “✓” belgisi raqamlar aniq mos kelganini bildiradi.

## Qisqacha

| Holat | Banklar soni | Nimani anglatadi |
|---|---|---|
| **A. JSON API** | 11 | Eng barqaror. Kursni tayyor JSON’da beradi; sayt dizayni o‘zgarsa ham odatda buzilmaydi. |
| **B. HTML** (server sahifaga yoki HTML bo‘lagiga yozadi) | 18 | Oddiy so‘rov yetadi. Sahifa tuzilishi o‘zgarsa, yig‘uvchi yangilanishi kerak. |

Hamma 29 bankda USD olish/sotish kursi bank.uz bilan aniq mos keldi (oddiy server so‘rovi bilan tekshirildi).

**Manbani topish bo‘yicha saboq:** ko‘p bank kursni boshqa subdomendan yoki keyin yuklanadigan HTML bo‘lagidan oladi: `back.asakabank.uz`, `api-dbo.hamkorbank.uz`, `api.smartbank.uz`, `…/lazyload/main-page.php`. Brauzerda DevTools → Network ro‘yxatini domen bo‘yicha emas, **Fetch/XHR** turi bo‘yicha filtrlab qarash kerak.

## A. JSON API (11)

| Bank | Manzil | Format | Valyutalar | USD |
|---|---|---|---|---|
| **BRB** | `https://brb.uz/api/currency/compare?type=1` | `data[]`: `{code, buy, sell}`, **tiyinda (÷100)**. Javobda `updated_at` bor. | USD, EUR, RUB, GBP, CHF, JPY, TJS; yana “MOBIL USD” (ilovadagi kurs) | ✓ 11 980 / 12 060 |
| **Tenge Bank** | `https://tengebank.uz/api/exchangerates/tables` | `personal[0].currency.USD = {buy, sell}`; `personal[]` kunlar bo‘yicha (oxirgi kun birinchi). | USD, EUR, RUB, **KZT (kasr: 26,1 / 26,9)**, GBP | ✓ 11 965 / 12 030 |
| **Agrobank** | `https://agrobank.uz/api/v1/?action=pages&code=uz/person&lang=uz` | Sahifa JSON’i ichida `type: "currency-rates"` bloki: `{alpha3, buy, sale, rate, updated}` | USD, EUR, RUB, GBP, JPY, … | ✓ 11 960 / 12 050 |
| **SQB** (O‘zsanoatqurilishbank) | `https://sqb.uz/api/site-kurs-api/` | `data.offline[]` (kassadagi naqd): `{code, buy, sell}`, **tiyinda (÷100)**. Yana `online`, `atm`, `juridic` ro‘yxatlari bor. | Naqd: USD, EUR, RUB, GBP, CHF, JPY, CNY. Yuridik: + KZT | ✓ 11 950 / 12 060 |
| **Universal bank** | `https://universalbank.uz/api/currencies/daily` | `items[]`: `{code (ISO raqam: 840 = USD), buyingRate, sellingRate}`, qiymatlar matn ko‘rinishida | USD, EUR, RUB, GBP, JPY | ✓ 11 940 / 12 030 |
| **Openbank** (avval Smartbank) | `https://api.smartbank.uz/retail/cbs-acc-mgmt/currency/rate` | `[{ccy1, buyRate, sellRate, cbuRate}]` | Faqat USD | ✓ 11 930 / 12 010 |
| **Hayot Bank** | `https://api.hayotbank.uz/api/curr-exchange-rate/get-all?size=2000&search=currExchangeRate.active=true` | `data[]`: `{currency.code (ISO raqam), buy, sell, cbu}`, qiymatlar matn ko‘rinishida | USD, EUR, RUB, GBP, CHF, JPY, KZT | ✓ 11 940 / 12 040 |
| **Asakabank** | `https://back.asakabank.uz/core/v1/currency-list/` (+ `?page=2`) | `results[]`: `{short_name, purchase, sale, rate_cb, currency_type}`. Jismoniy shaxslar: `currency_type: 1`; kurs bo‘lmasa `"-"`. Sahifasiga 10 ta qator, jami 14 → 2 sahifa. Topilgan sana: 22.11.2026 | USD, EUR, RUB, … | ✓ 11 950 / 12 030 |
| **Hamkorbank** | `https://api-dbo.hamkorbank.uz/webflow/v1/exchanges` | `data[]`: `{currency_char, buying_rate, selling_rate, destination_code, begin_sum_i, created_at}`, **tiyinda (÷100)**. Kassa kursi: `destination_code: "2"` va `begin_sum_i: 0`. `destination_code: "7"` — ilova kursi, `begin_sum_i > 0` — katta summalar uchun. Manzil sahifadagi `siteConfig.api.exchangeRates`da turadi | USD, EUR, RUB, … (9 qator) | ✓ 11 950 / 12 060 |
| **Xalq banki** | `https://xb.uz/api/v1/external/client/default?_f=json&_l=uz&destination=2` | `[{title, BUYING_RATE, SELLING_RATE, EQUIVAL, day, month}]`, qiymatlar matn ko‘rinishida. Eslatma: `…/exchange-rate/last-thirty-day` boshqa narsa — 30 kunlik tarix, bugungi kassa kursi emas | USD, GBP, RUB, JPY, EUR, KZT, CHF | ✓ 11 960 / 12 060 |
| **Ziraat Bank** | `https://ziraatbank.uz/tr/GetCurrency` | `[{name, value, oldValue, …}]`. **Nomlar chalg‘itadi:** `value` = olish, `oldValue` = sotish. | USD, EUR, **TRY (210 / 250)**, … | ✓ 11 950 / 12 060 |

Universal, Hayot va boshqa ba’zi banklar valyutani ISO raqamli kod bilan beradi: 840 USD, 978 EUR, 643 RUB, 826 GBP, 756 CHF, 392 JPY, 398 KZT, 156 CNY.

**Ziraat Bank turk lirasini (TRY) naqd sotib oladi va sotadi.** bank.uz buni ko‘rsatmaydi, shuning uchun “TRY’ni hech bir bank almashtirmaydi” degan avvalgi xulosamiz noto‘g‘ri edi. TRY’ni qo‘shish uchun shu manba kerak bo‘ladi.

## B. HTML (18)

| Bank | Sahifa | Izoh | Valyutalar (sahifada uchraydi) | USD |
|---|---|---|---|---|
| **Anorbank** | `https://anorbank.uz/about/exchange-rates/` | | USD, EUR, RUB, GBP, CHF, JPY, KZT | ✓ |
| **Asia Alliance Bank** | `https://aab.uz/uz/` (bosh sahifa) | `/uz/individuals/currency/` 404 qaytaradi | 8 tasi ham | ✓ |
| **Milliy bank (NBU)** | `https://nbu.uz/jismoniy-shaxslar-valyutalar-kursi` | | 8 tasi ham | ✓ |
| **Kapitalbank** | `https://kapitalbank.uz/uz/` | Cloudflare orqasida, Nuxt | 8 tasi ham | ✓ |
| **Infinbank** | `https://www.infinbank.com/uz/` | Cloudflare, Bitrix kurs komponenti | USD, EUR, RUB, GBP, CHF, JPY | ✓ |
| **Apexbank** | `https://apexbank.uz/about/exchange-rates/` | | USD, EUR, RUB | ✓ |
| **Trastbank** | `https://trustbank.uz/uz/` | Domen `trustbank.uz` | USD, EUR, RUB, GBP, CHF, JPY, CNY | ✓ |
| **KDB Bank** | `https://kdb.uz/uz` | | USD, EUR, JPY | ✓ |
| **Orient Finans Bank** | `https://ofb.uz/kursy-valyut` | | USD, EUR, RUB, GBP, CHF, JPY | ✓ |
| **Ipak Yo‘li** | `https://ipakyulibank.uz/physical/valyuta-ayirboshlash` | Cloudflare; eski `/physical/obmen-valyut` shu manzilga yo‘naltiradi | USD, EUR, RUB, GBP, CHF, JPY | ✓ |
| **Aloqabank** | `https://aloqabank.uz/uz/` | `www.aloqabank.uz` bo‘sh sahifa qaytaradi | USD, EUR, RUB, GBP, CHF, JPY | ✓ |
| **Octobank** (avval Ravnaq) | `https://octobank.uz/` | Cloudflare. Kurs vidjeti har sahifada bor; `/jismoniy-shaxslarga/kurs-v-otdelenii` 404 holat kodi bilan qaytadi | USD, EUR, RUB, GBP, CHF, JPY | ✓ 11 920 |
| **Davr bank** | `https://abbdavr-bank.uz/uz` | **Domen o‘zgargan** (`davrbank.uz` shu yerga yo‘naltiradi); Next.js, Cloudflare | USD, EUR, GBP, CHF, JPY | ✓ |
| **MKBank** | `https://mkbank.uz/uz/` | `www.mkbank.uz` bo‘sh sahifa qaytaradi | USD, EUR, RUB, GBP, CHF, JPY | ✓ |
| **Ipoteka bank** | `https://www.ipotekabank.uz/` (bosh sahifa) | “Yangilandi: vaqt, sana” yozuvi bor. `/about/rates/` — tariflar sahifasi, kurs emas | USD, … | ✓ 11 915 / 12 035 |
| **Turon bank** | `https://turonbank.uz/uz/lazyload/main-page.php` | Bosh sahifaning keyin yuklanadigan HTML bo‘lagi (Bitrix, `pixelcraft/mk.main.rates`). Oddiy so‘rovga to‘liq javob beradi, maxsus sarlavha kerak emas | USD, EUR, GBP, JPY, CHF, RUB, KZT | ✓ 11 960 / 12 060 |
| **Poytaxt bank** | `https://poytaxtbank.uz/uz/lazyload/main-page.php` | Turon bilan bir xil modul, bitta yig‘uvchi ikkalasiga ishlaydi. 3 ta jadval bor: kassa, ilova, bankomat. **Birinchisini (kassa) olish kerak**; jadval ostida “… dan ma’lumotlar” sanasi bor | USD, EUR, GBP, JPY, CHF, RUB | ✓ 11 915 / 12 030 |
| **Garant bank** (avval Savdogar) | `https://garantbank.uz/uz` | Ba’zan sekin: bir marta 20 soniyada javob bermadi, keyin 0,8 soniyada javob berdi. Timeout’ni katta qo‘yish kerak | USD, EUR, … | ✓ 11 955 / 12 055 |

“Valyutalar” ustunidagi kodlar sahifa matnida uchraydi; ularning hammasi kurs jadvalida ekani alohida tasdiqlanmagan.

## Himoya va xavflar

- **Cloudflare orqasida:** Kapitalbank, Infinbank, SQB, Openbank, Ipak Yo‘li, Octobank, Davr. Hozir Chrome user-agent bilan o‘tadi. Cloudflare qo‘shimcha tekshiruv yoqsa, yopilib qolishi mumkin.
- **Xalq banki** ko‘p so‘rovga 429 qaytaradi. Kuniga 4 marta, bitta so‘rov bilan muammo bo‘lmasligi kerak.
- **Hamkorbank** saytida F5 bot himoyasi bor (`/TSPD/` skripti). API (`api-dbo.hamkorbank.uz`) hozir oddiy so‘rovga javob beradi, lekin himoya kuchaytirilsa yopilishi mumkin.
- **Ziraat** sahifasida reCAPTCHA bor (formalar uchun), lekin `/tr/GetCurrency` ochiq.
- **Kurs birligi har xil.** BRB, SQB va Hamkorbank tiyinda beradi (100 ga bo‘lish kerak). Tenge KZT’ni kasr bilan beradi. Bir nechta bank ISO raqamli koddan foydalanadi.
- **Eskirgan sana.** Agrobank (`updated` maydoni), Hamkorbank va Ipoteka sahifalarida sana bor. Zaxira yig‘uvchi bu sanani o‘qib, bir necha kundan eski kursni olmasligi kerak.

## Zaxira yig‘uvchi (qurilgan, `src/bankSites.js`)

- **Qachon ishlaydi:** bank.uz USD kursini bermasa — ishlamasa, bloklasa yoki banklar soni avvalgisining yarmidan kamayib ketsa. Shunda build (`src/build.js`) 29 ta bank saytini o‘qiydi, faqat Toshkent vaqti bilan 1:00, 8:00, 13:00, 20:00 da. Oradagi soatlarda oxirgi fayl qoladi. bank.uz tiklanishi bilan kurslar yana bank.uz’dan olinadi.
- **Kunlik tekshiruv:** bank.uz ishlab turganda ham sutkada bir marta 29 ta sayt o‘qiladi va USD kursi bank.uz bilan solishtiriladi. O‘qilmagan yoki farq qilgan bank haqida egasiga Telegram’da xabar boradi (`src/alerts.js`), shuning uchun buzilgan manba zaxira kerak bo‘lishidan oldin topiladi.
- **Tezligi:** 29 bank, 6 tadan parallel, taxminan 5 soniya.
- **Natija (22.11.2026):** 28/29 bank o‘qildi, USD hammasida bank.uz bilan aniq mos keldi; jami 126 ta valyuta qiymati mos, farq yo‘q. RUB/JPY kabi ikkinchi darajali valyutalar ba’zi HTML sahifalarda topilmaydi.
- **Kapitalbank:** Cloudflare 403 qaytaradi (sinovlar ko‘p bo‘lgan kompyuterdan; GitHub serverida tekshirish kerak).
- **Maxsus holatlar kodda:** Infinbank (kalkulyatordagi `data-rates`), Ipak Yo‘li (Nuxt ma’lumotidagi “Kassada” jadvali), Davr (faqat USD), Turon/Poytaxt (lazyload bo‘lagi), so‘rovlar `node:https` orqali (Asakabank sarlavhasi, Turon IPv6, bitta qayta urinish).
- **Sinash:** `node --import <bank.uz’ni o‘chiruvchi skript> src/build.js rates` — bank.uz so‘rovlarini xato qildirib, zaxira rejimini tekshirish mumkin.

