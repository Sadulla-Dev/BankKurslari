# Regulation and payment rails for inbound remittances to Uzbekistan (state as of Nov 2026)

Research date: 2026-11-22. Note on sources: there were about 19 search and fetch calls. Primary regulator texts on cbu.uz and lex.uz were mostly not reachable through search, so many claims rest on Uzbek news outlets (Gazeta.uz, Kun.uz, Daryo, Yep.uz, Podrobno, Anhor) quoting the CBU. Where a source is old or of low authority, it is flagged.

## 1. CBU rules for inbound international transfers to individuals (payout currency, conversion, fees, limits, KYC)

### Takeaway
Inbound transfers to individuals have no regulatory cap. Cash is paid out in the currency in which the transfer arrived (USD/EUR/RUB), with passport or ID. Card payouts to Humo/Uzcard are credited in UZS, and the limits that apply are bank or provider limits, not CBU limits. From 9 Aug 2026, new CBU and prosecutor-office AML rules apply: monitoring triggers at 1,000 BCU (about $34k) of inbound card credits in 30 days, and fuller sender and recipient data must travel with transfers.

### Cited Findings
- The government portal says international transfers between individuals are carried out "without restrictions". Cash is paid out in the currency in which the transfer arrived. The recipient comes to a branch with a passport or ID card, gives the control code, amount and currency, and signs a receipt application. Receiving a transfer addressed to another person is not allowed. Legal basis cited: Law "On Currency Regulation". The page shows no effective date. — [gov.uz, accessed 2026-11-22](https://gov.uz/ru/advice/71/document/1035)
- The CBU has stated that there are no restrictions on international payment services and cross-border transfers through commercial bank platforms. The statement is about 3 years old, roughly 2023, so it may be historical. — [uz24.uz](https://uz24.uz/ru/articles/net-ogranicheniy)
- Payments in foreign currency inside Uzbekistan are prohibited. An older CBU release described automatic conversion of UZS card funds into FX for outbound transfers. It is old and should be treated as historical context. — [cbu.uz release 38954](https://cbu.uz/ru/press_center/releases/38954/)
- On the cash-payout currency during the KoronaPay disruption, Kun.uz (29 Jul 2026) reported that recipients "may withdraw funds in any available currency" while banks pay out from collateral deposits. — [Kun.uz 2026-07-29](https://kun.uz/en/news/2026/07/29/uzbekistans-banks-continue-processing-zolotaya-korona-remittances-despite-sanctions)
- Example of a provider-level split: Tranglo (B2B rail) offers cash pickup in USD or EUR, and direct credit to a bank card in UZS with a cumulative 30-day limit of USD 9,999. — [Tranglo service spec](https://tranglo.com/docs/tranglo-connect/service-specification/uzbekistan/)
- Example of bank-level limits: TBC Bank Uzbekistan lists international transfers at up to 3 per day and up to $12,500 / €11,000 / 150,000,000 UZS per month. This is a bank limit, not a CBU limit. — [tbcbank.uz blog](https://tbcbank.uz/ru/blog/articles/send-money-no-problem/)
- New AML rules: the CBU and the Department for Combating Economic Crimes signed them on 15–16 Apr 2026 (document "2886-12"). They were registered on 8 May 2026 and take effect on 9 Aug 2026. — [Yep.uz 2026-05-13](https://yep.uz/en/2026/05/uzbekistan-tightens-control-bank-transfers-card-transactions/)
  - Incoming transfers to a card totalling 1,000 BCU or more within 30 days trigger monitoring. In the same rules, mobile-app transfers to cards or wallets totalling 500 BCU or more in 30 days require "special attention". 1 BCU = 412,000 UZS, about $34 at publication, so 1,000 BCU ≈ 412m UZS ≈ $34k and 500 BCU ≈ $17k.
  - Transactions linked to FATF-monitored countries totalling 1,000 BCU or more in 30 days are treated as suspicious.
  - For individuals, banks must transmit full name, address, date of birth, ID card or biometric passport details, card or account numbers and unique transaction codes, so that each transfer can be traced end to end. Foreign institutions' data requests must be answered within 3 working days.
  - Domestic electronic transfers above 25 BCU must carry sender and recipient data, as international ones do.
  - Large P2P activity comes under additional monitoring. No threshold is published.
  - Cash FX purchase without ID rises from $100 to $500.
- A commercial guide says compliance checks are standard up to $10,000 and that source-of-funds documents are requested above that. This is a low-authority vc.ru/DTF-type source. — [gazetabirja.uz 2026](https://www.gazetabirja.uz/87261-10-21-kak-perevesti-dengi-v-uzbekistan-iz-rossii-v-2026-godu.html)

### Inferences
- In practice the binding limits for a comparison site are the sender provider's and the receiving bank's limits, for example $9,999 per 30 days to a card at Tranglo and $12,500 per month at TBC. A per-provider "limits" field is needed. A single national figure would be wrong.
- Card payouts are effectively UZS, because Humo and Uzcard are UZS cards. A USD-cash or USD-account payout is the way to avoid conversion. The comparator should therefore show the payout currency and FX margin separately for card and cash payouts.
- The 9 Aug 2026 AML rules make it more likely that large or frequent inbound card credits get held or questioned. That is worth a user-facing note above roughly $17k–34k per 30 days.

### Gaps
- I did not find the current CBU regulation text (lex.uz) on international money transfer systems. It would give the formal payout-currency rule, whether conversion is mandatory on card credit, and any CBU cap on recipient fees.
- I found no source on receiving-side fees. Inbound transfers are commonly free to the recipient, but I could not verify that with a 2025–26 source.
- I did not confirm whether a foreign-currency (USD) card or account can receive Visa Direct or Mastercard credits directly, or which banks support it.

## 2. Crediting Humo/Uzcard and Uzbek-issued Visa/Mastercard from abroad

### Takeaway
Direct card transfers are now the dominant channel: 51.7% of H1 2026 inflows, up 32% year on year. Foreign providers increasingly push straight to Humo and Uzcard, for example Kaspi (Mar 2025), Paysend, uPay and KoronaPay Europe. Uzbek Visa and Mastercard cards can also receive Visa Direct and Mastercard MoneySend credits, for example at Uzum Bank. Russian Mir cards have not been serviced in Uzbekistan since 23 Sep 2022.

### Cited Findings
- H1 2026 inflows by channel (CBU): direct transfers to bank cards $4.8bn, 51.7% of inflows, up 32%. Traditional money transfer systems $4.3bn, stagnant. SWIFT $142m, down 43%. — [Eastern Economy 2026-08-21, citing CBU](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks)
- Full-year 2025: transfers $18.9bn, up 27.2%. Traditional international systems $9.9bn (52%), P2P card transfers $8.6bn (46%), bank transfers $397m. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/)
- Kaspi.kz launched transfers to Uzcard and Humo. Before that, only Visa and Mastercard were possible from Kazakhstan. — [Kun.uz 2025-03-25](https://kun.uz/en/news/2025/03/25/kaspikz-launches-money-transfers-to-uzcard-and-humo-cards)
- uPay.net, a US service founded in New York by Uzbek entrepreneurs, launched direct transfers to Humo and Uzcard. Date not verified. — [Caspian Post](https://caspianpost.com/uzbekistan/us-launches-direct-money-transfers-to-uzbekistan-s-humo-and-uzcard-cards)
- Paysend launched instant payouts to Humo and Uzcard, adding to its Visa and Mastercard support. Date not verified. — [The Payments Association](https://thepaymentsassociation.org/article/paysend-launches-payouts-to-humo-and-uzcard-cards-enabling-businesses-to-send-cross-border-payments-to-uzbekistan/)
- KoronaPay Europe: transfers from European Visa, Mastercard and Maestro cards are credited instantly to Uzcard and Humo, reported as commission-free. Date not verified. — [UzDaily](https://www.uzdaily.uz/en/transfers-to-uzbekistan-from-europe-to-uzcard-and-humo-cards-can-now-be-sent-without-commission/)
- Uzum Bank receives international transfers via Visa Direct, MoneySend and Western Union. Bank licence No. 88 of 28 Nov 2022. — [uzumbank.uz transfers page](https://uzumbank.uz/ru/transfers/)
- Humo and Uzcard have co-badge agreements with Visa, Mastercard and UnionPay (e.g. Visa-Humo, Uzcard-Mastercard). — [Trustbank](https://trustbank.uz/en/private/plastic-cards/list/national/visa-humo-and-uzcard-mastercard-cobaging-cards/); [8b.world blog](https://www.8b.world/blog/visa-and-mastercard-in-uzbekistan-why-uzcard-and-humo-dominate-the-payment-landscape)
- Mir, historical: the Uzcard processing centre froze Mir card operations from 09:00 on 23 Sep 2022, citing "technical procedures". This followed an OFAC warning on 15 Sep 2022 to foreign banks about working with NSPK. — [dp.ru 2022-09-23](https://www.dp.ru/a/2022/09/23/Banki_Uzbekistana_s_23_se); [Spot.uz 2022-09-21](https://www.spot.uz/ru/2022/09/21/mir-disconnect/)
- In Nov 2022, CBU Chairman Mamarizo Nurmuratov said resuming Mir was "not on the agenda" because of large risks for commercial banks. — [Ukrainska Pravda 2022-11-03](https://epravda.com.ua/rus/news/2022/11/03/693420/)
- 2026 guides state that Mir cards are not used in Uzbekistan and that Russian senders transfer to Humo or Uzcard, or to an account. Low authority. — [vc.ru 2026](https://vc.ru/money/2955710-perevody-v-uzbekistan-iz-rossii-zolotaya-korona-banki-i-uzcard)

### Inferences
- For the comparator, "card" should be split into at least two destinations: Humo/Uzcard (UZS, national rails) and Uzbek-issued Visa/Mastercard (Visa Direct or MoneySend, usually still a UZS or FX account depending on the card). Provider coverage differs between them.
- Mir is not a valid destination. Treat it as historical, since Sep 2022.

### Gaps
- I found no official Visa or Mastercard announcement on Visa Direct or Mastercard Move reach to Humo or Uzcard. Cross-border pushes to Humo and Uzcard seem to run through bilateral or processor integrations, not through Visa Direct itself.
- I found nothing on UnionPay inbound (China to Uzbekistan) rules.
- I found no 2023–2026 confirmation of Mir status. The latest primary-ish sources are from 2022.

## 3. Russia → Uzbekistan: sanctions and working channels in 2026

### Takeaway
Russia is still about 72% of inflows (H1 2026), down from 77.6%. The main shock of 2026 was the EU's 21st package (23 Jul 2026), which listed KoronaPay's operator and settlement centre (RNKO "Platezhny Tsentr") and Tsifra Bank. Several Uzbek banks paused the service, and the CBU said banks would switch to a non-sanctioned technical operator. Completion of that switch is unconfirmed. The EU listed Unistream on 16 Nov 2026; it was already under US blocking sanctions from Jul 2023. On the Russian side, transfer limits for Russian citizens and residents of friendly countries were lifted from 8 Dec 2025.

### Cited Findings
- Russia's share of inflows fell from 77.6% (H1 2025) to 72.4% (H1 2026). Total H1 2026 inflows were $9.3bn, up 13%. — [Eastern Economy 2026-08-21, citing CBU](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks)
- 23 Jul 2026: the EU 21st package listed RNKO "Platezhny Tsentr", the operator and settlement centre of Zolotaya Korona / KoronaPay, and Tsifra Bank. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/); [Gazeta.uz 2026-07-24](https://www.gazeta.uz/ru/2026/07/24/koronapay-sanctions/)
- Zolotaya Korona stopped Russia→Georgia and Russia→Kazakhstan transfers, but Uzbekistan remained a working destination. — [Interfax](https://www.interfax.ru/business/1106326); [Caspian Post](https://caspianpost.com/kazakhstan/korona-pay-halts-money-transfers-from-russia-to-kazakhstan-and-georgia)
- Asakabank suspended Zolotaya Korona sending and receiving on 24 Jul 2026, citing "technical work". Transfers via Tsifra Bank were also suspended. — [Gazeta.uz 2026-07-24](https://www.gazeta.uz/ru/2026/07/24/koronapay-sanctions/)
- Kapitalbank, Asakabank and Agrobank suspended these transfers. — [Eastern Economy 2026-08-21](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks). Kapitalbank stopping is also reported by [Kun.uz 2026-07-29](https://kun.uz/en/news/2026/07/29/uzbekistans-banks-continue-processing-zolotaya-korona-remittances-despite-sanctions). One search summary also named Aloqabank, but I did not verify that from a primary article.
- CBU position (Otabek Tojiddinov, director of the Currency Regulation and Supervision Dept, 29 Jul 2026): the sanctions hit the processing bank, not the system. Banks are negotiating to move to a non-sanctioned technical operator, expected "from next week". The impact on Uzbekistan is minor. In the meantime, payouts continue from security or collateral deposits placed by Zolotaya Korona. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/); [Kun.uz 2026-07-29](https://kun.uz/en/news/2026/07/29/uzbekistans-banks-continue-processing-zolotaya-korona-remittances-despite-sanctions); [Daryo 2026-07-30](https://daryo.uz/en/2026/07/30/zolotaya-korona-transfers-in-uzbekistan-to-switch-operators-over-sanctions/); [UzDaily](https://www.uzdaily.uz/en/uzbek-banks-switch-technical-operator-for-zolotaya-korona-system/)
- Volume at stake: about $2.4bn via Zolotaya Korona in H1 2026, about 26% of all inflows. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/). Eastern Economy cites Kursiv for "about $2.4bn a year", including South Korea, Turkey and EU senders. — [Eastern Economy](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks)
- Possible new-operator vehicle: on 23 Jul 2026 the Russian company "Servis", sole founder of RNKO Platezhny Tsentr per registries, acquired 100% of Parapay. Parapay is an Uzbek payment organisation licensed by the CBU in Apr 2023. Gazeta notes that it is not confirmed Parapay is the new operator. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/)
- Unistream: the EU listed JSC CB Unistream (entry 1871) in measures announced on 16 Nov 2026 (12 individuals, 101 entities). The EU alleges it gives Russian military personnel preferential loan terms. Unistream disputes the EU's reasons and says it expects no major operational change because it has no European assets. The US imposed blocking sanctions in Jul 2023. After that, in Aug 2023, Unistream stopped USD and EUR transfers to Tajikistan while RUB transfers continued. — [Asia-Plus 2026-11-17](https://asiaplus.news/en/2026/11/17/eu-imposes-sanctions-on-unistream-which-operates-in-tajikistan-what-will-change/)
- Uzbek banks reportedly refused Unistream after the US sanctions (Ipoteka, Kapitalbank, Hulk Bank). This appears to date from 2023 and is historical. — [mind.ua](https://mind.ua/en/news/20260367-western-banks-refuse-to-work-with-the-russian-unistream-the-last-channel-for-withdrawing-money-abro)
- Russian-side outbound limits: from 8 Dec 2025 the Bank of Russia lifted limits on FX transfers abroad for Russian citizens and residents of friendly countries. The previous cap was $10,000 per month via money transfer systems. For non-residents from unfriendly countries, restrictions are extended from 8 Jun to 7 Dec 2026: working non-residents may transfer up to their salary, and non-working ones are banned. Uzbekistan is not on Russia's "unfriendly" list, so Uzbek migrants fall into the friendly category. — [Snob](https://snob.ru/news/bank-rossii-otmenil-limity-na-valiutnye-perevody-za-granitsu/); [finance.mail.ru](https://finance.mail.ru/article/cb-otmenil-limity-na-valyutnye-perevody-za-rubezh-dlya-rossiyan-69006058/); [finance.mail.ru, extension](https://finance.mail.ru/article/cb-prodlil-ogranicheniya-na-perevody-za-rubezh-dlya-nerezidentov-69211252/); [Consultant.ru](https://www.consultant.ru/legalnews/31713/)
- Other Russia→UZ channels per 2026 guides (low authority): Russian bank apps sending to Humo or Uzcard cards; ruble correspondent-account transfers to Xalq Bank and Hamkorbank. Sources conflict on SBP: one says cross-border SBP to Uzbekistan works only via Uzum Bank, another says SBP is unavailable for Uzbekistan. — [vc.ru](https://vc.ru/money/2955710-perevody-v-uzbekistan-iz-rossii-zolotaya-korona-banki-i-uzcard); [DTF](https://dtf.ru/howto/4842935-perevody-iz-rossii-v-uzbekistan); [gazetabirja.uz](https://www.gazetabirja.uz/87261-10-21-kak-perevesti-dengi-v-uzbekistan-iz-rossii-v-2026-godu.html)
- US secondary-sanctions framework: E.O. 14114 (Dec 2023) allows OFAC to cut foreign financial institutions off from US correspondent accounts for supporting Russia's military-industrial base. As of 2024 analyses it had not yet been used. I found no report of any Uzbek bank being designated. — [Morrison Foerster 2024-01-03](https://www.mofo.com/resources/insights/240103-us-authorizes-secondary-sanctions-on-banks); [OFAC FAQ](https://ofac.treasury.gov/faqs/topic/6626)
- Historical: an anhor.uz note saying Zolotaya Korona "works normally" in a list of Uzbek banks is dated 9 Apr 2014. It is not 2026 evidence. — [Anhor 2014-04-09](https://anhor.uz/events/zolotaya-korona-soobshaet-o-shtatnom-rezhime-raboti)

### Inferences
- The Russia→UZ corridor works in Nov 2026, but per-bank availability for KoronaPay and Unistream is volatile. A comparator should store availability per receiving bank, with a "last verified" date, rather than per corridor.
- The pattern is that sanctions hit operators or settlement banks, not Uzbek recipients directly. Uzbek banks react by pausing, quietly and citing "technical work", and the CBU then brokers a workaround. Expect brief outages after each EU or US package, typically in July and November in 2026.
- The shift from Russia toward card-to-card and the EU, UK and Korea is structural: the CIS migrant count is down 29% and Europe is up 42%.

### Gaps
- I found no source confirming that KoronaPay's switch to a new operator was completed, or naming the operator, after early Aug 2026.
- I found no post-16-Nov-2026 report on whether Uzbek banks paused Unistream.
- I could not settle the SBP cross-border status to Uzbekistan. Sources conflict, and I found no primary source from the Bank of Russia, NSPK or a bank.
- I could not verify whether Contact (Qiwi/Rosbank group) is still operating to Uzbekistan in 2026.

## 4. Other corridors: Kazakhstan, Korea, EU/UK, USA, Turkey, UAE

### Takeaway
None of these corridors has a specific Uzbek-side restriction in what I found. Volumes grew in H1 2026, with the UK fastest at +62%. Corridor-specific changes are on the provider side: Kaspi to Humo and Uzcard, US apps to Humo and Uzcard, KoronaPay Europe. The one systemic risk is that KoronaPay also carried Korea, Turkey and EU funds, so the July 2026 sanctions touched those corridors too.

### Cited Findings
- H1 2026 inflows by origin: Kazakhstan $469m (+28%), USA $372m (+19%), EU $319m (+27%), South Korea $314m (+11%), Turkey $303m (+14%), UK $144m (+62%). Within the EU, Ireland is up 86% and Lithuania up 18%. — [Eastern Economy 2026-08-21, citing Gazeta.uz/CBU](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks)
- Uzbek migrants abroad: 1.4m in total. Russia 834,200, Europe 258,000, Kazakhstan 84,400, Turkey 72,100, South Korea 46,400. CIS is down 29% and Europe up 42% year on year. — [same](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks)
- Kazakhstan: Kaspi.kz offers direct Humo and Uzcard transfers since Mar 2025. KoronaPay Russia→Kazakhstan stopped in Jul 2026; this is not relevant to KZ→UZ. — [Kun.uz 2025-03-25](https://kun.uz/en/news/2025/03/25/kaspikz-launches-money-transfers-to-uzcard-and-humo-cards)
- KoronaPay flows to Uzbekistan include funds from South Korea, Turkey and the EU. — [Eastern Economy citing Kursiv](https://eastern-economy.com/article/9-3-billion-uzbekistan-s-remittances-rise-while-russia-s-share-shrinks); [Gazeta.uz](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/)
- UK: a comparison guide says Wise does not support direct sending in UZS. — [svoi.uk](https://svoi.uk/en/humo)
- USA: uPay.net sends to Humo and Uzcard. — [Caspian Post](https://caspianpost.com/uzbekistan/us-launches-direct-money-transfers-to-uzbekistan-s-humo-and-uzcard-cards)

### Inferences
- For KoronaPay quotes from Korea, Turkey or the EU, the comparator should carry the same sanctions-volatility flag as for Russia.

### Gaps
- I found no corridor-specific regulation for the UAE, Turkey or Korea. Sender-side rules, such as Korea's annual limits on foreign-worker remittances and FX reporting, were outside what I could verify.
- I found no 2025–26 data on the UAE corridor's volume.

## 5. Recent changes 2024–2026 (CBU regulation, payment-organisation licensing, AML)

### Takeaway
The main 2024–26 changes I found are:
- payment organisations need a CBU licence, with minimum charter capital of 20bn UZS from 1 Jul 2025;
- the Competition Committee designated Payme, Click and Uzum Market as dominant digital platforms (Jan 2025 onward);
- the new AML and traceability rules took effect on 9 Aug 2026;
- sanctions-driven operator switches (KoronaPay, Jul 2026).

I could not confirm whether Payme or Click hold their own licence to accept inbound cross-border transfers. Uzum's international transfers run through Uzum Bank.

### Cited Findings
- A payment organisation is a non-bank legal entity providing payment services under a CBU licence. As of early 2025 there were 2 payment-system operators (Humo, Uzcard) and 49 payment organisations (including Payme, Click, Uzum, Oson). Minimum charter capital is 20bn UZS from 1 Jul 2025. — [Azizov & Partners, Dec 2024 PDF](https://azizovpartners.uz/wp-content/uploads/2024/12/Okazanie-platezhnyh-uslug-v-Respubliki-Uzbekistan.pdf); [Esplora Legal](https://esploralegal.com/regulation-of-payment-organizations-in-the-republic-of-uzbekistan/)
- Payme has held a CBU licence since 4 Apr 2020. In Jan 2025 the Competition Committee recognised Payme as a dominant digital-platform operator in online payments, and later Click and Uzum Market. — [Wikipedia (RU) Payme](https://ru.wikipedia.org/wiki/Payme), secondary source
- Uzum's international transfers are made by Uzum Bank (licence No. 88, 28 Nov 2022) via Visa Direct, MoneySend and Western Union. — [uzumbank.uz](https://uzumbank.uz/ru/transfers/)
- Parapay, a payment organisation licensed in Apr 2023, was bought on 23 Jul 2026 by Russian "Servis", which is linked to the KoronaPay settlement centre. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/)
- AML rules take effect on 9 Aug 2026; details are in Section 1. — [Yep.uz](https://yep.uz/en/2026/05/uzbekistan-tightens-control-bank-transfers-card-transactions/)
- The CBU kept its key rate at 14% (late Jul 2026) and did not factor the sanctions into its model. — [Gazeta.uz 2026-07-29](https://www.gazeta.uz/ru/2026/07/29/koronapay-uzb/)

### Inferences
- Payment-organisation apps (Payme, Click and others) likely act as front-ends for bank or processor partners for cross-border inbound flows, rather than as licensed cross-border remitters themselves. This is unverified.

### Gaps
- I found no text of any 2024–26 CBU regulation specifically on international money transfer systems, such as a requirement for operator collateral or security deposits. The CBU's July 2026 reference to "security deposits" implies such a regime exists.
- I found no primary list of which payment organisations are licensed for cross-border services.

## 6. Does a comparison/aggregator website need a licence or face advertising/consumer rules?

### Takeaway
I found no rule requiring a licence for a site that only compares and links to remittance offers without handling money. The Law "On Advertising" does apply. Article 16 bans advertising services that require a licence when the advertiser lacks one, so promoting unlicensed providers is risky. Credit advertising got new mandatory risk-warning rules (law amended, reported 10 Nov 2026). Those rules target credit, not transfers.

### Cited Findings
- Law "On Advertising", Art. 16: advertising goods or services whose sale requires a licence or permit is prohibited without that licence. This was applied to developers advertising unregistered property. — [Anhor](https://anhor.uz/news/fines-ad/); law text [lex.uz 6052633](https://lex.uz/acts/6052633)
- Credit, microloan and instalment advertising must carry a financial-risk warning taking at least 10% of the visual area or duration, and must show the overpayment and total cost. Guarantees of future benefit are banned. Changes to the law were reported on 10 Nov 2026. — [Podrobno.uz](https://podrobno.uz/cat/obchestvo/v-uzbekistane-reklamu-zaymov-obyazali-soprovozhdat-preduprezhdeniem-o-finansovykh-riskakh/); [Anhor](https://anhor.uz/news/trebovanija-k-reklame-kreditov-i-rassrochki/); [Anhor, Competition Committee + CBU proposal](https://anhor.uz/news/komitet-6/)
- Payment services may only be provided by banks or CBU-licensed payment organisations. — [Azizov & Partners](https://azizovpartners.uz/wp-content/uploads/2024/12/Okazanie-platezhnyh-uslug-v-Respubliki-Uzbekistan.pdf)

### Inferences
- If the comparator only shows prices and outbound links, it is unlikely to need a CBU licence. If it collects money, initiates transfers or stores card data, it would become a payment service needing a licence.
- To limit Article 16 risk, list only providers licensed in their home country, or Uzbek banks and payment organisations, and avoid presenting unlicensed P2P or crypto "exchangers" as remittance options.
- If the site later shows loans (the Bankkurslari app context), the new 10% risk-warning and overpayment-disclosure rules would apply to those ads.

### Gaps
- I found no explicit Uzbek rule on financial-comparison or aggregator sites, affiliate or referral fees, or ranking disclosure.
- I did not check consumer-protection law (Law "On Protection of Consumer Rights") or personal-data law (localisation of Uzbek users' data under the 2019 Personal Data law) for specifics. Both may matter if the site stores user data.
