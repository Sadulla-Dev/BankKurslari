# Bank filiallari geoma’lumotlari: manbalar va reja

## Tekshirilgan holat

- `data/branches.json` bank.uz filial sahifalaridan yig‘ilgan keng asosiy ro‘yxat. Har bir nuqtada `source: "bank.uz"` saqlanadi.
- `data/official-branches.json` banklarning o‘z filial xaritalari va ochiq JSON manbalaridan yig‘ilgan nuqtalar. Yig‘uvchi 23 ta bankning rasmiy sahifasini tekshiradi; manba domeni har bir yozuvda bor.
- API qurilganda rasmiy nuqtalar bank.uz yozuvlaridan ustun turadi. Avvalgi 250 m yaqinlik filtri bitta binoga yaqin turgan alohida filiallarni ham yo‘qotishi mumkin edi; endi faqat 75 m ichidagi ehtimoliy takror yozuvlar almashtiriladi.
- Asosiy bazada aynan bir xil bank, filial nomi, manzil va koordinatali 620 takror yozuv bor edi. Ular olib tashlandi; turli nom/manzildagi yoki yaqin joylashgan filiallar saqlandi.
- Aloqabank va MKBank rasmiy importlaridagi sahifa soni chegaralari olib tashlandi. Importer xatosi avvalgi ma’lumotni o‘chirib yubormaydi; hech bir rasmiy manba muvaffaqiyatli ishlamasa, keshning yangilangan vaqti ham o‘zgarmaydi.

## Ish vaqti (hours) bo‘yicha holat — 2026-11 tekshiruvi

Ish vaqti faqat bankning o‘z sahifasidan olinadi; bank e’lon qilmagan vaqt o‘ylab topilmaydi (`hours: null` → ilovada "Ish vaqti nomaʼlum").

Qo‘shilgan yoki tuzatilgan manbalar:

- **Tenge Bank** — ro‘yxat API-sidagi `works` maydoni doim bo‘sh. Vaqt har bir ofis sahifasida (`tengebank.uz/map/inner/{id}`, o‘zbekcha, til prefiksisiz) "Ish jadvali" blokida: uchta qator — ish kunlari / shanba / yakshanba (kun nomlari sahifada bo‘sh, tartib bo‘yicha o‘qiladi; "Вых" = dam olish).
- **Ipak Yuli Bank** — xarita feedida vaqt yo‘q. Filiallar (17 ta): `ipakyulibank.uz/offices` jadvalidagi vaqt (Du–Ju). Shanba: bankning "yaxshilangan ish jadvali" yangiligida nomi keltirilgan filiallar va BXM/EM lar uchun (`IPAK_SATURDAY`, kodda). Boshqa BXM/EM lar uchun vaqt e’lon qilinmagan.
- **Davr bank** — `abbdavr-bank.uz/api/product/api/atm-branch?type=BRANCH` (sayt xaritasi shu feedni o‘qiydi): `start_time`/`end_time`, `work_on_weekend`, `day_and_night`.
- **Octobank** — `octobank.uz/kontaktlar`: bitta ofis (bosh ofis), xarita nuqtasi va Du..Ya jadvali.
- **Mybank** — `my-bank.uz/uz/contact` (Next.js): sahifa ichidagi `dataList` (koordinata, manzil, "Ish vaqti").

Ish vaqti olinmaydigan banklar (sabab):

- **Uzum Bank** — saytda filial/ofis ro‘yxati yo‘q (raqamli bank); bank.uz dagi nuqtalarda vaqt yo‘q.
- **O‘zbekiston Milliy banki** — `nbu.uz/bank-haqida/bank-tarmogi` ro‘yxati Cloudflare Turnstile tekshiruvidan keyin yuklanadi; serverdan oddiy fetch bilan olinmaydi (bot himoyasini chetlab o‘tish qilinmaydi).
- **Kapitalbank** — `kapitalbank.uz/uz/branches` sahifasida har kunlik jadval bor (Nuxt payload: `schedule.days[].workHours`), ammo Node fetch Cloudflare 403 oladi. Ochiq API topilsa importer qo‘shish mumkin.
- **Asakabank** — saytda manzillar bloki "ishlab chiqilmoqda"; ochiq ro‘yxat yo‘q.
- **AVO bank** — saytda faqat kartomatlar (qurilmalar) xaritasi; ofislar yo‘q.
- **Universal bank** — `universalbank.uz/api/locations` da va saytda vaqt yo‘q (faqat nom, manzil, telefon).
- **Ziraat Bank, KDB Bank** — ofis sahifalarida vaqt bor (Ziraat: `ziraatbank.uz/uz/andijon-ofisi` kabi; KDB: `kdb.uz/uz/about-us/branches`), lekin koordinata yo‘q. Importer koordinatasiz yozuvni qabul qilmaydi; bank.uz nuqtalariga manzil bo‘yicha ulash alohida ish.
- **Turkiston bank, Hi-Tech Bank** — rasmiy sayt domeni topilmadi / ochilmadi (`turkistonbank.uz`, `hitechbank.uz` DNS’da yo‘q; `htb.uz` da filial ro‘yxati topilmadi).

## Yandex API kalitisiz variant

1. **Bankning o‘z sayti/API-si — asosiy aniq manba.** Koordinata, nom va manzilni bank o‘zi bergani uchun eng ishonchli manba. Hozirgi importerlarda muvaffaqiyatsiz yoki qamrab olinmagan banklar uchun yangi importerlar shu tartibda qo‘shiladi.
2. **bank.uz — keng zaxira ro‘yxati.** Qamrovi katta, ammo koordinata va yozuvlar rasmiy bank tasdig‘i bilan teng emas. Shuning uchun rasmiy koordinata yaqin bo‘lsa o‘sha ishlatiladi, bank.uz esa rasmiy ro‘yxatda yo‘q mini-bank/xizmat nuqtalarini saqlab qoladi.
3. **OpenStreetMap — bepul ochiq qo‘shimcha va audit manbasi.** O‘zbekiston uchun Geofabrik mamlakat extracti mavjud; sahifa extractlar odatda har kuni yangilanishini va joriy PBF hajmi taxminan 119 MB ekanini ko‘rsatadi. Bu barcha bank filiallarini kafolatlamaydi: ma’lumot ko‘ngillilar tomonidan to‘ldiriladi, qamrov hududga qarab farq qiladi. Keng qamrovli yangilash uchun Overpass’ga muntazam mamlakat miqyosidagi so‘rov yuborish o‘rniga extractni olib, server tomonida filtrlash kerak.

OSM ma’lumoti ODbL 1.0 litsenziyasida: manbani ko‘rsatish va hosila bazaga tegishli share-alike talablarini bajarish kerak. Shu sabab OSM koordinatalarini hozir bank.uz bazasiga ko‘r-ko‘rona qo‘shish tavsiya qilinmaydi; avval alohida OSM dataset/end-point va ilovada manba atributsiyasini saqlash kerak. Yandex xaritasidagi pinlarni ruxsatsiz ko‘chirib olish yoki yashirin ichki endpointlarini ishlatish barqaror va ruxsatli manba emas.

## Dam olish kunlari ochiq filiallar (`src/weekend.js` → `src/weekendLists.js` → `src/weekendBranches.js`)

- Markaziy bank har hafta (payshanba/juma kechqurun) Telegram kanalida banklar ro‘yxatini e’lon qiladi; har bir bank o‘z ro‘yxatiga havola beradi. `api/weekend.json` → `banks`.
- Har bir bankning ro‘yxati kod bilan o‘qiladi, sun’iy intellektsiz: HTML jadval (Agrobank, Xalq banki, Turonbank, Infinbank, SQB), xlsx (Ipoteka, BRB), matnli PDF (Hamkorbank, Mikrokreditbank, Asakabank — `back.asakabank.uz/news/v1/detail/<slug>/` orqali, NBU), matn (Garant, Tenge, Asia Alliance, Orient Finans, Apex, Universal, Poytaxt, Hayot — `api.hayotbank.uz`, Saderat). Bank qayerda e’lon qilishi `weekendLists.js` → `BANKS`da.
- Rasm ko‘rinishidagi ro‘yxat (Trastbank, 2026-11) o‘qilmaydi: sahifada “ro‘yxat rasmda” deb, havola bilan ko‘rsatiladi. Bot tekshiruvi (Hamkorbank ba’zan) chetlab o‘tilmaydi.
- Sanalar tekshiriladi: bank sahifasi boshqa hafta uchun bo‘lsa (Infinbank, Saderat bir sahifani har hafta yangilaydi) — `stale`, filiallari olinmaydi.
- Filial xaritadagi nuqtaga faqat ishonchli moslikda bog‘lanadi: bir bank, bir shahar/tuman, bir ko‘cha va uy raqami yoki filial nomi, keyingi nomzoddan aniq ustunlik. Boshqa uy raqami, boshqa ko‘cha yoki boshqa halqa yo‘li (TXAY / Katta / Kichik halqa) — moslik emas. Mos kelmaganlar xaritaga qo‘yilmaydi, faqat sahifada bank yozgan manzil bilan ro‘yxatda turadi.
- `api/weekend.json`: `branchLists` (har bank: `status` ok/stale/none/image/blocked/error/all/regular, `count`, `matched`), `weekendBranches` (`name`, `parent`, `address`, `region`, `open: { Sa?, Su? }` — `""` = ish vaqti yozilmagan, `point: { lat, lng }` yoki `null`). Ro‘yxatlar dam olish o‘tguncha har 3 soatda qayta o‘qiladi; o‘qilmagan ro‘yxat egasiga bir marta xabar qilinadi.

## Keyingi qamrovni oshirish tartibi

- Importer hisobotidagi banklar soni, muvaffaqiyat/xato holati va eng oxirgi muvaffaqiyatli yangilanish vaqtini kuzatish.
- Rasmiy manbasi yo‘q banklar uchun filial xaritasining ommaviy sahifasini tekshirib, faqat ochiq va ruxsatli JSON/HTML ma’lumotini import qilish.
- Turli manbalardagi pinlarni yaqin koordinata bilan emas, bank nomi + normallashtirilgan manzil + masofa kombinatsiyasi bilan moslashtirish; mos kelmagan nuqtalarni o‘chirmasdan alohida qoldirish.
- Shubhali koordinatalarni (O‘zbekiston hududidan tashqarida, 0/0, bir bankda bitta nuqtaga haddan tashqari ko‘p yozuv) import vaqtida hisobotga chiqarish.

## Manbalar

- [Geofabrik: Uzbekistan OSM extract](https://download.geofabrik.de/asia/uzbekistan.html)
- [OSM data olish bo‘yicha qo‘llanma](https://wiki.openstreetmap.org/wiki/Downloading_data)
- [OSM litsenziyasi va atributsiya](https://www.openstreetmap.org/copyright)
- [Overpass API instansiyalari va fair-use eslatmalari](https://wiki.openstreetmap.org/wiki/Overpass_API)
