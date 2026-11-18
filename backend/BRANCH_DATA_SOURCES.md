# Bank filiallari geoma’lumotlari: manbalar va reja

## Tekshirilgan holat

- `data/branches.json` bank.uz filial sahifalaridan yig‘ilgan keng asosiy ro‘yxat. Har bir nuqtada `source: "bank.uz"` saqlanadi.
- `data/official-branches.json` banklarning o‘z filial xaritalari va ochiq JSON manbalaridan yig‘ilgan nuqtalar. Yig‘uvchi 20 ta bankning rasmiy sahifasini tekshiradi; manba domeni har bir yozuvda bor.
- API qurilganda rasmiy nuqtalar bank.uz yozuvlaridan ustun turadi. Avvalgi 250 m yaqinlik filtri bitta binoga yaqin turgan alohida filiallarni ham yo‘qotishi mumkin edi; endi faqat 75 m ichidagi ehtimoliy takror yozuvlar almashtiriladi.
- Asosiy bazada aynan bir xil bank, filial nomi, manzil va koordinatali 620 takror yozuv bor edi. Ular olib tashlandi; turli nom/manzildagi yoki yaqin joylashgan filiallar saqlandi.
- Aloqabank va MKBank rasmiy importlaridagi sahifa soni chegaralari olib tashlandi. Importer xatosi avvalgi ma’lumotni o‘chirib yubormaydi; hech bir rasmiy manba muvaffaqiyatli ishlamasa, keshning yangilangan vaqti ham o‘zgarmaydi.

## Yandex API kalitisiz variant

1. **Bankning o‘z sayti/API-si — asosiy aniq manba.** Koordinata, nom va manzilni bank o‘zi bergani uchun eng ishonchli manba. Hozirgi importerlarda muvaffaqiyatsiz yoki qamrab olinmagan banklar uchun yangi importerlar shu tartibda qo‘shiladi.
2. **bank.uz — keng zaxira ro‘yxati.** Qamrovi katta, ammo koordinata va yozuvlar rasmiy bank tasdig‘i bilan teng emas. Shuning uchun rasmiy koordinata yaqin bo‘lsa o‘sha ishlatiladi, bank.uz esa rasmiy ro‘yxatda yo‘q mini-bank/xizmat nuqtalarini saqlab qoladi.
3. **OpenStreetMap — bepul ochiq qo‘shimcha va audit manbasi.** O‘zbekiston uchun Geofabrik mamlakat extracti mavjud; sahifa extractlar odatda har kuni yangilanishini va joriy PBF hajmi taxminan 119 MB ekanini ko‘rsatadi. Bu barcha bank filiallarini kafolatlamaydi: ma’lumot ko‘ngillilar tomonidan to‘ldiriladi, qamrov hududga qarab farq qiladi. Keng qamrovli yangilash uchun Overpass’ga muntazam mamlakat miqyosidagi so‘rov yuborish o‘rniga extractni olib, server tomonida filtrlash kerak.

OSM ma’lumoti ODbL 1.0 litsenziyasida: manbani ko‘rsatish va hosila bazaga tegishli share-alike talablarini bajarish kerak. Shu sabab OSM koordinatalarini hozir bank.uz bazasiga ko‘r-ko‘rona qo‘shish tavsiya qilinmaydi; avval alohida OSM dataset/end-point va ilovada manba atributsiyasini saqlash kerak. Yandex xaritasidagi pinlarni ruxsatsiz ko‘chirib olish yoki yashirin ichki endpointlarini ishlatish barqaror va ruxsatli manba emas.

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
