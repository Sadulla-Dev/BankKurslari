# Bank kurslari: banklarning o‘z saytlari (bank.uz uchun zaxira)

Asosiy manba bank.uz, har soatda o‘qiladi. Bu hujjat bank.uz ishlamay qolsa yoki bizni bloklasa, kurslarni banklarning o‘z saytlaridan olish uchun. Zaxira rejimida saytlar kuniga 4 marta o‘qiladi: Toshkent vaqti bilan 1:00, 8:00, 13:00 va 20:00.

Tekshiruv sanasi: **21.11.2026**, 29 bank (bank.uz ro‘yxatidagi hammasi).

- So‘rovlar Chrome brauzeri user-agent’i bilan yuborildi, backend ham shunday ishlaydi.
- Har bir manbadagi bugungi USD olish/sotish kursi bank.uz’dagi aynan shu bank kursi bilan solishtirildi. “✓” belgisi raqamlar aniq mos kelganini bildiradi.

## Qisqacha

| Holat | Banklar soni | Nimani anglatadi |
|---|---|---|
| **A. JSON API** | 8 | Eng barqaror. Kursni tayyor JSON’da beradi; sayt dizayni o‘zgarsa ham odatda buzilmaydi. |
| **B. HTML** (server sahifaga yozadi) | 16 | Oddiy so‘rov yetadi. Sahifa tuzilishi o‘zgarsa, yig‘uvchi yangilanishi kerak. |
| **C. Faqat brauzerda** | 4 | Kurs JavaScript bilan chiziladi, manbasi hali aniqlanmagan. Keyingi tekshiruv kerak. |

**USD olish/sotish kursi bank.uz bilan mos kelgan banklar:**
- Oddiy server so‘rovi bilan tekshirilganlar: **24 ta**.
- Faqat brauzerda ko‘rilganlar: **yana 4 ta** (Asakabank, Turon, Poytaxt, Hamkorbank).
- **Xalq banki:** sahifasidagi raqam mos keladi, lekin kurs qaysi so‘rovdan kelishi aniq emas (C-guruhda batafsil).

Demak, zaxira sifatida 24 ta bankni (A + B) hozirgi backend usulida, brauzersiz o‘qish mumkin.

## A. JSON API (8)

| Bank | Manzil | Format | Valyutalar | USD |
|---|---|---|---|---|
| **BRB** | `https://brb.uz/api/currency/compare?type=1` | `data[]`: `{code, buy, sell}`, **tiyinda (÷100)**. Javobda `updated_at` bor. | USD, EUR, RUB, GBP, CHF, JPY, TJS; yana “MOBIL USD” (ilovadagi kurs) | ✓ 11 980 / 12 060 |
| **Tenge Bank** | `https://tengebank.uz/api/exchangerates/tables` | `personal[0].currency.USD = {buy, sell}`; `personal[]` kunlar bo‘yicha (oxirgi kun birinchi). | USD, EUR, RUB, **KZT (kasr: 26,1 / 26,9)**, GBP | ✓ 11 965 / 12 030 |
| **Agrobank** | `https://agrobank.uz/api/v1/?action=pages&code=uz/person&lang=uz` | Sahifa JSON’i ichida `type: "currency-rates"` bloki: `{alpha3, buy, sale, rate, updated}` | USD, EUR, RUB, GBP, JPY, … | ✓ 11 960 / 12 050 |
| **SQB** (O‘zsanoatqurilishbank) | `https://sqb.uz/api/site-kurs-api/` | `data.offline[]` (kassadagi naqd): `{code, buy, sell}`, **tiyinda (÷100)**. Yana `online`, `atm`, `juridic` ro‘yxatlari bor. | Naqd: USD, EUR, RUB, GBP, CHF, JPY, CNY. Yuridik: + KZT | ✓ 11 950 / 12 060 |
| **Universal bank** | `https://universalbank.uz/api/currencies/daily` | `items[]`: `{code (ISO raqam: 840 = USD), buyingRate, sellingRate}`, qiymatlar matn ko‘rinishida | USD, EUR, RUB, GBP, JPY | ✓ 11 940 / 12 030 |
| **Openbank** (avval Smartbank) | `https://api.smartbank.uz/retail/cbs-acc-mgmt/currency/rate` | `[{ccy1, buyRate, sellRate, cbuRate}]` | Faqat USD | ✓ 11 930 / 12 010 |
| **Hayot Bank** | `https://api.hayotbank.uz/api/curr-exchange-rate/get-all?size=2000&search=currExchangeRate.active=true` | `data[]`: `{currency.code (ISO raqam), buy, sell, cbu}`, qiymatlar matn ko‘rinishida | USD, EUR, RUB, GBP, CHF, JPY, KZT | ✓ 11 940 / 12 040 |
| **Ziraat Bank** | `https://ziraatbank.uz/tr/GetCurrency` | `[{name, value, oldValue, …}]`. **Nomlar chalg‘itadi:** `value` = olish, `oldValue` = sotish. | USD, EUR, **TRY (210 / 250)**, … | ✓ 11 950 / 12 060 |

Universal, Hayot va boshqa ba’zi banklar valyutani ISO raqamli kod bilan beradi: 840 USD, 978 EUR, 643 RUB, 826 GBP, 756 CHF, 392 JPY, 398 KZT, 156 CNY.

**Ziraat Bank turk lirasini (TRY) naqd sotib oladi va sotadi.** bank.uz buni ko‘rsatmaydi, shuning uchun “TRY’ni hech bir bank almashtirmaydi” degan avvalgi xulosamiz noto‘g‘ri edi. TRY’ni qo‘shish uchun shu manba kerak bo‘ladi.

## B. HTML, server sahifaga yozadi (16)

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
| **Garant bank** (avval Savdogar) | `https://garantbank.uz/uz` | Ba’zan sekin: bir marta 20 soniyada javob bermadi, keyin 0,8 soniyada javob berdi. Timeout’ni katta qo‘yish kerak | USD, EUR, … | ✓ 11 955 / 12 055 |

“Valyutalar” ustunidagi kodlar sahifa matnida uchraydi; ularning hammasi kurs jadvalida ekani alohida tasdiqlanmagan.

## C. Faqat brauzerda ko‘rinadi, manba aniqlanmagan (4)

| Bank | Sahifa | Nima ma’lum | Keyingi qadam |
|---|---|---|---|
| **Asakabank** | `https://asakabank.uz/uz/physical-persons/home` | Brauzerda “Valyuta kursi … aktual” bloki bor, kurs mos keladi (11 950 / 12 030). Oddiy HTML’da yo‘q. `back.asakabank.uz` hosti bor | DevTools → Network: blok qaysi so‘rov bilan to‘lishini topish |
| **Turon bank**, **Poytaxt bank** | `https://turonbank.uz/uz/`, `https://poytaxtbank.uz/uz/` | Ikkalasi bir xil Bitrix komponentini ishlatadi (`pixelcraft/mk.main.rates`). Brauzerda kurs mos keladi (Turon 11 960 / 12 060, Poytaxt 11 915 / 12 030) | Komponentning AJAX manzilini topish; bitta yig‘uvchi ikkalasiga ishlaydi |
| **Hamkorbank** | `https://hamkorbank.uz/` (Next.js) | Brauzerda mos keladi (11 950 / 12 060). HTML’da ham, RSC javobida ham yo‘q. Sahifada “Последнее обновление: 04.11.2026” yozuvi bor | Manbani DevTools orqali topish |
| **Xalq banki** | `https://xb.uz/` | Sahifada “1 USD = 11 960 UZS” bor (mos). API: `/api/v1/external/client/exchange-rate/last-thirty-day?_f=json&_l=uz` → `data["DD-MM-YYYY"] = {buying, selling}` — bugun 11 900 / 12 030. Bu bank.uz bilan **mos emas**, ehtimol boshqa turdagi kurs. Tez so‘rovlarga **429** (cheklov) qaytaradi | Sahifadagi kurs qaysi so‘rovdan kelishini aniqlash; so‘rovlarni sekin yuborish |

Xalq banki bu jadvalda beshinchi qator, lekin yuqoridagi “Qisqacha” jadvalidagi C-guruh sanog‘iga kirmaydi: sahifadagi raqam mos keladi, faqat manbasi ikkilanadi.

## Himoya va xavflar

- **Cloudflare orqasida:** Kapitalbank, Infinbank, SQB, Openbank, Ipak Yo‘li, Octobank, Davr. Hozir Chrome user-agent bilan o‘tadi. Cloudflare qo‘shimcha tekshiruv yoqsa, yopilib qolishi mumkin.
- **Xalq banki** ko‘p so‘rovga 429 qaytaradi. Kuniga 4 marta, bitta so‘rov bilan muammo bo‘lmasligi kerak.
- **Ziraat** sahifasida reCAPTCHA bor (formalar uchun), lekin `/tr/GetCurrency` ochiq.
- **Kurs birligi har xil.** BRB va SQB tiyinda beradi (100 ga bo‘lish kerak). Tenge KZT’ni kasr bilan beradi. Bir nechta bank ISO raqamli koddan foydalanadi.
- **Eskirgan sana.** Agrobank (`updated` maydoni), Hamkorbank va Ipoteka sahifalarida sana bor. Zaxira yig‘uvchi bu sanani o‘qib, bir necha kundan eski kursni olmasligi kerak.

## Zaxira yig‘uvchini qurish tartibi (taklif)

1. **A-guruhdan boshlash** (8 ta JSON). Har bank uchun kichik funksiya bo‘ladi, javob umumiy `{bank, buy, sell}` ko‘rinishiga keltiriladi.
2. **Keyin B-guruh** (16 ta HTML) cheerio bilan. Har bank uchun selektor va namuna sahifa saqlanadi, sahifa o‘zgarsa test darhol buzilib, ogohlantiradi.
3. **C-guruh** uchun avval manbani DevTools orqali topish.
4. **Ishga tushish qoidasi:** bank.uz natijasi yaroqsiz bo‘lganda (banklar soni avvalgisining yarmidan kam, `build.js`dagi himoya) yoki soatlik `step` xato berganda, zaxira kuniga 4 marta o‘qiladi.
5. **Har bir kursga manba yoziladi** (`source: "bank.uz"` yoki bankning domeni), va 2 kundan eski kurs olinmaydi.
