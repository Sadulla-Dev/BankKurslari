# Jonli narxlar (RU, KZ → UZ) va migrant nigohidan Qulay Kurs

Tekshirildi: 2026-11-22, ~20:30 (Toshkent), provayderlarning ochiq kalkulyatorlari, login'siz, qo'lda (brauzerda).
Etalon: CBU kursi 21.11.2026 — 1 USD = 12 004,48 · 1 RUB = 146,56 · 1 KZT = 26,35 so'm.
Banklarning eng yaxshi USD sotib olish kursi (Qulay Kurs API): 11 995 so'm (Anorbank).

## 1. Jonli narxlar

### Rossiya → O'zbekiston

| Provayder | Qanday tushadi | Kurs | Komissiya | 10 000 ₽ → | 50 000 ₽ → | CBU'ga nisbatan yo'qotish |
|---|---|---|---|---|---|---|
| Avosend ([avosend.com](https://avosend.com/)) | Kartaga, so'mda, bir necha daqiqa | 1 ₽ = 140,20 so'm | 29 ₽ | **1 397 934 so'm** | **7 005 934 so'm** | 4,6% / 4,4% |
| Korona ([koronapay.com/transfers/online](https://koronapay.com/transfers/online/)) | Naqd **USD**, bank kassasida | 1 $ = 85,4152 ₽ | 0 ₽ (pullik SMS-xabarnoma o'chirilsa; yoqilsa 99 ₽) | 117,08 $ | 585,38 $ | 4,2–4,3% (kursda) |
| Korona + dollarni eng yaxshi bankda sotish (11 995) | — | — | — | ≈ 1 404 317 so'm | ≈ 7 021 584 so'm | 4,2% |

Izohlar:
- Ikkalasida ham "komissiya" deyarli yo'q, **asosiy xarajat kursda (~4,3%)**. 50 000 ₽ da bu ~310 000 so'm.
- Korona kalkulyatorida O'zbekiston uchun faqat USD tanlanadi (limit 5 000 $ / o'tkazma). Kartaga so'mda tushirish faqat ilovada, narxi ochiq emas.
- Korona operatori iyul 2026 da YeI sanksiyasiga tushgan, bir qator O'zbek banklari to'lovni to'xtatgan — kalkulyator ishlaydi, lekin qaysi bankda olish mumkinligi alohida tekshirilishi kerak.
- Unistream: sayt brauzerda ochilmadi (Rossiya TLS sertifikati), narx olinmadi.
- Sber, T-Bank, VTB, Alfa: kurs faqat ilova ichida — ochiq narx yo'q.

### Qozog'iston → O'zbekiston

| Provayder | Qanday tushadi | Kurs | Komissiya | 50 000 KZT → | 100 000 KZT → | CBU'ga nisbatan yo'qotish |
|---|---|---|---|---|---|---|
| Paysend ([paysend.com](https://paysend.com/en-kz/send-money/from-kazakhstan-to-uzbekistan)) | Humo/Uzcard/Visa/MC kartaga, so'mda, daqiqalar | 1 KZT = 25,7613 so'm | 199 KZT | **1 282 939 so'm** | **2 571 004 so'm** | 2,6% / 2,4% |

- Kaspi, Halyk, BCC: kurs faqat ilovada, ochiq kalkulyator yo'q.
- Korona KZ yo'nalishi sanksiyadan keyin buzilgan — tekshirilmadi.

### Xulosa narxlar bo'yicha
1. Ochiq, tekshirsa bo'ladigan narx hozircha faqat 3 ta provayderda: **Avosend, Korona, Paysend**. Bu MVP uchun kam, lekin haqiqiy.
2. Haqiqiy narx = kurs ustamasi. "0% komissiya" reklamalari foydalanuvchini adashtiradi — bizning asosiy qiymatimiz shu farqni ko'rsatish.
3. "Dollarda olib, eng yaxshi bankda sotish" yo'li bugun kartaga so'mda yuborishdan biroz yaxshi chiqdi (10 000 ₽ da +6 400 so'm). Buni faqat biz hisoblay olamiz, chunki banklarning kursi bizda bor.

## 2. Migrant nigohidan: men Moskvada / Seulda / Londonda ishlayman

Har oy oilaga pul yuboraman. Mening haqiqiy savollarim:

| Savol | Hozir qanday hal qilaman | Qulay Kurs nima bera oladi |
|---|---|---|
| "Qaysi xizmat onamning kartasiga **ko'proq so'm** tushiradi?" | Do'stlardan so'rayman, Telegram guruhlar, odatdagi bankim | Mamlakat + summa → "Oilangiz oladi: X so'm" bo'yicha tartiblangan ro'yxat, har birida tekshirilgan vaqt |
| "0% komissiya deyishadi, lekin pul kam keladi — nega?" | Bilmayman | Har provayderda "Kursda yashirin xarajat: 4,3% = 310 000 so'm" qatori |
| "Dollarda yuborsammi yoki so'mda?" | Taxmin | Ikkala yo'lni yonma-yon: "USD naqd + eng yaxshi bankda sotish" vs "kartaga so'm" — oilaning shahridagi eng yaxshi kurs va xaritadagi yaqin filial bilan (bizda bor) |
| "Korona to'xtadimi? Qaysi bank hali beradi?" | Yangiliklarni qidiraman, vahima | "Holat" paneli: ishlaydi / to'xtatilgan / noaniq, sana va manba bilan. Telegram kanalga ogohlantirish |
| "Bugun yuborsam yaxshimi yoki ertaga?" | Bilmayman | Rubl/tenge → so'm kursi grafigi va "kurs yaxshilanganda xabar ber" (Telegram bot / ilova push) |
| "Yiliga qancha yo'qotyapman?" | O'ylab ko'rmaganman | "Oyiga 50 000 ₽ yuborsangiz, kursdagi yashirin xarajat yiliga ~3,7–3,9 mln so'm" kalkulyatori (bugungi narxlar: oyiga 306–322 ming so'm CBU kursiga nisbatan) |
| "Limit qancha? Bank savol beradimi?" | Muammo chiqqanda bilaman | Qisqa qo'llanma: provayder limitlari, CBU monitoring chegaralari (~17 000 $ / ~34 000 $ 30 kunda) |
| Oila tomoni: "Pul keldi, dollarni qayerda sotay?" | Eng yaqin bankka boraman | Oila uchun havola: "Bugun dollarni eng qimmat oladigan bank — yaqin filial xaritada" |

### Nega aynan bizning sayt kerak
- **Neytral**: biz pul o'tkazmaymiz, hech kimning ilovasi emasmiz. Provayderning o'z sayti o'zini maqtaydi.
- **Ikkala tomonni biladigan yagona joy**: jo'natish narxi (provayder) + qabul qilish tomoni (O'zbek banklari kursi, CBU, filiallar xaritasi). Raqobatchilar (Monito, Wise compare) O'zbekiston va Rossiya yo'nalishini qamramaydi; Sravni/Banki.ru/bank.uz'da ham jonli taqqoslash topilmadi.
- **Ishonch**: har narx yonida "tekshirildi: 22.11, 20:30", manba havolasi; taxminiy narx hech qachon aniq deb ko'rsatilmaydi.
- **Migrant qayerda bo'lsa, o'sha yerda**: Telegram kanal/bot (migrantlar Telegram'da yashaydi), yengil mobil sahifa, o'zbek (lotin + kirill) va rus tili.

### Keyingi qadamlar (taklif)
1. Shu 3 provayder bilan "Oilangiz qancha oladi" MVP sahifasi (RU va KZ), narxlar kuniga 1–2 marta qo'lda yoki provayder ruxsati bilan yangilanadi.
2. "USD naqd + eng yaxshi bank" hisobini mavjud bank kurslari bilan ulash.
3. Provayderlar holati paneli + Telegram ogohlantirish.
4. Avosend, Paysend bilan hamkorlik/narx oqimi (feed) haqida yozish — scraping o'rniga.
5. Bir hafta davomida shu narxlarni kuniga 1 marta qayd etib, kurs ustamasi qanchalik o'zgarishini ko'rish.
