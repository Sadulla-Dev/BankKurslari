# Raqobatchilar tahlili: Qulay Kurs va boshqa kurs saytlari, ilovalari

Sana: 25.11.2026. Har bir saytni o‘zim ochib ko‘rdim, ilovalarni Google Play / App Store sahifasidan o‘qidim. Jadvaldagi “✓” — o‘z ko‘zim bilan ko‘rganim, “~” — qisman, “—” — topmadim.

## Asosiy xulosa

**Funksiyalar bo‘yicha biz ko‘pchilikdan oldindamiz, lekin bizni hech kim topmayapti.**

- Google Play’da Qulay Kurs: **1+ yuklab olish**. Yetakchi ilova “Курсы валют Узбекистана” (Yurchuk Viktor): **100 000+ yuklab olish, 14,6 ming baho**, reklamali va oddiy ilova.
- Veb-qidiruvda (AQSh qidiruv tizimi orqali) `qulaykurs.uz` umuman chiqmadi. Bu Google/Yandex’dagi holatni to‘liq ko‘rsatmaydi, lekin Search Console’da tekshirish shart.
- Faqat bizda bor narsalar: summa bo‘yicha “qo‘lingizga X so‘m” reytingi, kurs yozilgan xarita, marshrut ilovasini tanlash, ish vaqti va “hozir ochiq”, “borishga arziydimi” hisobi, 2009-yildan beri tarix, Telegram bot inline rejimi.

Demak, eng kerakli ish **yangi funksiya emas, tarqatish**. Funksiyalardan esa eng avval allaqachon yozilganlarini chiqarish kerak: Hamyon va pul o‘tkazmalari.

## Raqobatchilar

| Nomi | Turi | Qisqacha | Kuchli tomoni |
|---|---|---|---|
| [bank.uz](https://bank.uz/uz/currency) | Sayt + ilova | Bozor yetakchisi, “moliyaviy gipermarket” (omonat, kredit, karta) | Bizning ham asosiy manbamiz. Pul o‘tkazmalari, kripto, oltin, Telegram kanal |
| [dollar-kurs.uz](https://dollar-kurs.uz/) | Sayt | 33 bank, 17 valyuta, kuniga 3 marta yangilanadi | Eng to‘liq sayt: bank sahifalari, taqqoslash, o‘tkazmalar, arxiv, brauzer ogohlantirishi, vidjet, ochiq JSON, maqolalar. Ammo xatolar bor: RUB “eng yaxshi olish 502 so‘m” deb chiqyapti |
| [themoney.uz](https://themoney.uz/uz/) | Sayt + Android ilova (10 000+) | 30+ “hamkor bank”, 9 davlat | **Naqd / Bankomat / Bank ilovasi / Yuridik** yorliqlari, har bankda xarita havolasi, 60 oylik tarix, vidjetlar, reklama yo‘q |
| [depozit.uz](https://depozit.uz/uz/exchange-rate-bank/dollar) | Sayt | Moliya mahsulotlari portali | Shoxobcha / ilova / bankomat kurslari, sana bo‘yicha arxiv, saytlar uchun informer |
| [pul24.uz](https://pul24.uz/exchange-rates/) | Media + kurslar | Yangiliklar sayti | USD kassa / bankomat / ilova, oltin, Telegram kanal |
| [valyutakursi.uz](https://valyutakursi.uz/) | Sayt | MB va bank kurslari, bank sahifalari | Oddiy, oltin va kripto |
| [Uzrates](https://play.google.com/store/apps/details?id=uz.uzrates.uzratesapp) | Android (10 000+, 1,25 ming baho) | MB, banklar, arxiv, oltin | **“Bozor kursi”** (dollaruz.com va Telegram kanallardan), TRY |
| [Курсы валют Узбекистана — Yurchuk](https://play.google.com/store/apps/details?id=ab.exchangerateuz) | Android (100 000+, 14,6 ming baho) | 28 bank, MB, sana bo‘yicha kurs | Neft, metallar, kripto, birja grafiklari. Reklama bor |
| [Kurs — Valyuta va Banklar](https://apps.apple.com/us/app/id6756253888) | iOS (yangi) | Banklar, MB, oltin | 5 rejimli kalkulyator (shu jumladan “Travel”, kross-kurs), **foizli va “har qanday o‘zgarish” ogohlantirishlari**, vidjetlar, bosh ekranni moslash, Apple/Google orqali kirish, prognoz |

Bundan tashqari Play’da 5–6 ta faqat MB kursini ko‘rsatadigan oddiy ilova bor, ular xavf emas. Media (kun.uz, spot.uz, kursiv.media, anhor.uz) har kuni “qayerda foydali” maqolasini chiqaradi: bu qidiruv trafigi uchun raqobat.

## Funksiyalar jadvali

| Funksiya | Biz | bank.uz | dollar-kurs | themoney | depozit | Uzrates | Yurchuk | Kurs (iOS) |
|---|---|---|---|---|---|---|---|---|
| Summa bo‘yicha “qo‘lingizga X so‘m”, banklar reytingi | ✓ | — | ~ | ~ | — | — | — | ✓ |
| Kurs yozilgan xarita, filial, marshrut | ✓ | ~ | — | ~ | — | — | — | — |
| Ish vaqti, “hozir ochiq” | ✓ | — | — | — | — | — | — | — |
| MB tarixi, grafik | ✓ 2009-yildan | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Har bir bank kursi tarixi | ~ yig‘iladi, faqat kechagi farq | — | ✓ | ✓ | — | — | — | — |
| Karta / ilova / bankomat kursi | — | — | — | ✓ | ✓ | — | — | — |
| Pul o‘tkazmalarini taqqoslash | ~ yozilgan, chiqarilmagan | ✓ | ✓ | — | — | — | — | — |
| Valyutalar soni (bank kurslari) | 8 | 5 | 17 | ? | 7 | ? (TRY bor) | ? | ? |
| MB oltin quymalari | ✓ | ✓ | ✓ | — | — | ✓ | ✓ jahon narxi | ✓ |
| Kurs ogohlantirishi | ✓ chegara (Android, bot) | — | ✓ brauzer | ~ | — | — | — | ✓ foiz, har o‘zgarish |
| Telefon vidjeti | ✓ Android | — | — | ✓ | — | — | — | ✓ |
| Telegram bot / kanal | ✓ bot + kanal | ✓ kanal | — | ~ | — | — | — | — |
| Jamg‘arma (hamyon) | ~ yozilgan, berkitilgan | — | — | — | — | — | — | — |
| Saytga vidjet, ochiq API | ~ API bor, hujjat yo‘q | — | ✓ | — | ✓ | — | — | — |
| Maqolalar, savol-javob | ~ SEO sahifalari | ✓ | ✓ | — | ✓ | — | — | — |
| Kripto | — | ✓ | ✓ | — | — | — | ✓ | — |
| Bozor (“qora”) kursi | — | — | — | — | — | ✓ | — | — |
| Prognoz | — ataylab | — | — | — | — | — | — | ✓ |
| Reklama yo‘q | ✓ | — | ✓ | ✓ | — | ? | — | ✓ |

## Nima qilish kerak (muhimlik tartibida)

### 1. Tarqatish (eng muhim, 1–2 hafta)

1. **Play Store nomi va kalit so‘zlari.** Odamlar ruscha “курс валют узбекистан” deb qidiradi va yetakchi ikki ilovaning nomi aynan shu. Bizniki “Qulay Kurs – курс валют”. Taklif: ru nomi `Курс валют Узбекистана – Qulay Kurs`, uz nomi `Dollar kursi – Qulay Kurs`. Skrinshotlarda bizning noyob narsalarimiz: xarita pinlari va “qo‘lingizga 1 198 000 so‘m”.
2. **Ilova ichida baho so‘rash** (Play In-App Review API): masalan 3-marta summa kiritgandan keyin. Yuklab olishlar soni va baholar Play’dagi o‘rinni belgilaydi. Yurchuk’da 14,6 ming baho bor.
3. **Search Console va Yandex Webmaster.** `build.mjs` tasdiqlash tegini qo‘llab-quvvatlaydi (`GOOGLE_SITE_VERIFICATION`, `YANDEX_VERIFICATION`). Indekslanish, qaysi sahifalar chiqayotgani va qaysi so‘zlar bilan topilayotganini tekshirish kerak. SEO sahifalarimiz (bank, summa, tarix, 4 til, 147+ fayl) dollar-kurs.uz’nikidan kam emas, lekin ular qidiruvda ko‘rinishi kerak.
4. **Saytdan ilovaga yo‘l va Telegram.** Kanal va bot bor. Har kungi postda va bot javobida ilova havolasini qo‘yish.

### 2. Yozilgan, lekin chiqarilmagan (1 hafta)

5. **Hamyonni chiqarish.** Tayyor, 4 ta kalitni yoqish kifoya. Hech bir raqobatchida yo‘q. Har kuni qaytishga sabab bo‘ladi.
6. **Pul o‘tkazmalarini taqqoslashni chiqarish.** Sahifa `backup/pul-otkazmalari` branch’ida tayyor. bank.uz va dollar-kurs.uz’da bor, migrantlar har oy qaytadi. Tadqiqot `reports/` da bor.

### 3. Kamchiliklarni yopish (2–4 hafta)

7. **Ko‘proq valyuta: TRY, AED, KRW.** dollar-kurs.uz 17 ta valyuta ko‘rsatadi, bizda 8 ta. Ziraat Bank TRY’ni naqd almashtiradi, NBU AED kotirovka qiladi (3 232 / 3 297). Bank sahifalaridan kurs yig‘ish mexanizmi bizda allaqachon bor (`PER_BANK_RATES`). Sayohat va Koreyadagi migrantlar uchun kerak.
8. **Aqlliroq ogohlantirishlar:** “sevimli bankim kursini o‘zgartirsa” va “kurs X% ga o‘zgarsa”. Kurs (iOS) ilovasida bor. Bu odamni qaytaradigan bildirishnoma.
9. **Bank sahifasida kurs tarixi grafigi.** `bank-history-*.json` allaqachon yig‘ilyapti. dollar-kurs.uz va themoney’da bor.
10. **Saytlar uchun vidjet + ochiq API hujjati.** dollar-kurs.uz va depozit.uz shunday qilib boshqa saytlardan havola oladi (SEO). API bizda tayyor (statik JSON), faqat iframe vidjeti va bitta hujjat sahifasi kerak.

### 4. Qilmaslik kerak

- **Bozor (“qora”) kursi** (Uzrates): rasmiy bo‘lmagan ayirboshlashni targ‘ib qilishdek ko‘rinadi, aniqligi past, ilova do‘konlari va hamkor banklar bilan muammo bo‘lishi mumkin.
- **Prognoz** (Kurs iOS): “prognoz yo‘q, faqat faktlar” tamoyilimizga zid. Noto‘g‘ri prognoz ishonchni buzadi.
- **Kripto, neft:** mavzudan tashqari, bizning foydalanuvchiga (naqd almashtiradigan odam) kerak emas.
- **Karta / ilova kurslari:** themoney, depozit, pul24’da bor, lekin bugungi farq 0–20 so‘m. Biz bekor qildik va bu to‘g‘ri edi. Farq kattalashsa, qayta yozish taxminan 1 kunlik ish.

## Bizning ustunliklarimiz (marketingda aytish kerak)

- “Qo‘lingizga qancha tegadi” — kurs emas, aniq summa.
- Eng yaqin ochiq filial xaritada, bir bosishda Yandex / 2GIS’da marshrut.
- “Borishga arziydimi”: eng yaxshi bank bilan eng yaqin bank farqi, taksi narxi bilan solishtirib.
- Ma’lumot sifati: noto‘g‘ri kurslar avtomatik tashlanadi (dollar-kurs.uz’da bugun “RUB 502 so‘m” chiqyapti).
- Reklama yo‘q, ro‘yxatdan o‘tish yo‘q, o‘zbekcha birinchi.

## Manbalar

- https://dollar-kurs.uz/ (bosh sahifa, /transfers, /compare, /alerts, /widget, /banks/nbu, /archive)
- https://bank.uz/uz/currency
- https://themoney.uz/uz/
- https://depozit.uz/uz/exchange-rate-bank/dollar
- https://pul24.uz/exchange-rates/
- https://valyutakursi.uz/
- https://play.google.com/store/apps/details?id=uz.uzrates.uzratesapp
- https://play.google.com/store/apps/details?id=ab.exchangerateuz
- https://play.google.com/store/apps/details?id=com.urazaev.themoneyuz
- https://play.google.com/store/apps/details?id=uz.qulaykurs.app
- https://apps.apple.com/us/app/id6756253888
