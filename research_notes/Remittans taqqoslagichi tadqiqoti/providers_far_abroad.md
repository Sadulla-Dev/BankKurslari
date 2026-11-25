# Money transfer providers to Uzbekistan from South Korea, USA, UK, EU (DE/PL), Turkey, UAE (2025–2026)

Research date: 2026-11-22. Public pages only, no logins. 16 tool calls used (search + fetch). Coverage is **partial**: no live quote prices were captured (see Gaps). Every "observed" date below is 2026-11-22 unless stated.

## Which providers actually support Uzbekistan payout, per corridor (with payout methods, currency, speed, limits)?

### Takeaway
Card-to-card payout to Humo/Uzcard is the main channel used by the fintech players (Paysend, Hanpass, E9PAY, Gmoneytrans, Profee). Wise does **not** pay out UZS to Uzbekistan from any corridor checked. It only sends USD by SWIFT to a USD bank account. Remitly (US page) and Western Union (US/UK/EU pages) do have Uzbekistan destination pages. Turkey- and UAE-specific providers (PTT, Al Ansari, LuLu, Al Fardan, Turkish banks) and the Korean banks were not verified.

### Cited Findings

**Wise (global)**
- UK→UZ: "We're working hard to allow customers to send UZS to Uzbekistan from the UK - but we're not quite there yet." Wise does support sending USD to Uzbekistan by international wire (SWIFT) to a USD account. The recipient's bank country is set to Uzbekistan, with account number and SWIFT code. The page warns that intermediary SWIFT fees are not disclosed upfront. Its example shows 9.03 USD total fees. Observed 2026-11-22 — [Wise UK](https://wise.com/gb/send-money/send-money-to-uzbekistan)
- The same "not quite there yet" wording for UZS appears on the US page. USD by wire is supported (regional pages: ID, BE, ES-US, AE, SG, NZ) — [Wise ES-US](https://wise.com/es-us/send-money/send-money-to-uzbekistan); [Wise AE](https://wise.com/ae/send-money/send-money-to-uzbekistan)
- Wise publishes a USD→UZS mid-market converter. This is a reference rate only, not a payout route — [Wise converter](https://wise.com/gb/currency-converter/usd-to-uzs-rate)

**Remitly (US page; other origins not verified)**
- US→Uzbekistan pricing page, observed 2026-11-22. It names payout partners Biznesni Rivojlantirish Banki, Sanoat Qurilish Bank, Agrobank and Visa ("and more"). It describes delivery as "trusted banks, Cash pickup and debit card deposit". Humo and Uzcard are **not mentioned**. The calculator shows the recipient getting **USD**. A separate line reads "$0.99 When you send from USD to UZS", so a UZS (card) option may exist at a $0.99 fee. That is not confirmed — [Remitly US→UZ pricing](https://www.remitly.com/us/en/uzbekistan/pricing)
- USD→USD fee table on the same page (Economy / Express): <$300: $5.99 / $6.99; $300–599.99: $8.99 / $9.99; $600–749.99: $11.99 / $12.99; $750–999.99: $14.99 / $13.99; $1,000–1,299.99: $20.99 / $15.99; higher tiers $17.99–$24.99. A **500 USD send therefore costs $8.99 (Economy) or $9.99 (Express) in fees**, plus any FX margin (none if USD→USD). The table layout is ambiguous: some rows show Express cheaper than Economy, and the column for single-value rows is inferred. Treat it with caution — [Remitly US→UZ pricing](https://www.remitly.com/us/en/uzbekistan/pricing)
- Promotions: no fee on the first transfer, and a "Promotional FX rate applies to first 1,000.00 USD sent". The URL promo code "USA-UZB-FXBOOST-BEST-34BPS-1000CAP" suggests a 34 bps promo margin. Unconfirmed — [Remitly US→UZ pricing](https://www.remitly.com/us/en/uzbekistan/pricing)
- Limits: Remitly says "We limit how much you can send in 24 hours, 30 days, and 180 days" but gives no amounts on the page. The live rate field rendered as "-" (JS-driven) — [Remitly US→UZ pricing](https://www.remitly.com/us/en/uzbekistan/pricing)

**Paysend (global, incl. UK/EU/US/UAE apps)**
- Paysend supports card-to-card transfers to **Humo and Uzcard** cards. A press release dated **18 March 2020 (outdated)** gave a flat fee of £1 / €1.5 (or equivalent) for Humo, regardless of amount. It says the recipient amount is shown before sending and claims "competitive exchange rates", with no margin figure. The homepage link parameter toCurrId=860 implies UZS payout. Sending-country eligibility, speed and limits were not stated — [Paysend blog](https://paysend.com/en/blog/article-paysend-launches-transfers-to-humo-accounts)
- A later Paysend release announces payouts to Humo and Uzcard for businesses (Paysend Enterprise). It describes "instant payouts directly to Uzbekistan's leading local card schemes". It names Korean partner **Gmoneytrans** ("South Korea's first licensed and largest remittance provider") as using this rail — [Paysend news](https://paysend.com/ae-jo/news/paysend-launches-payouts-humo-uzcard-cards); [The Payments Association](https://thepaymentsassociation.org/article/paysend-launches-payouts-to-humo-and-uzcard-cards-enabling-businesses-to-send-cross-border-payments-to-uzbekistan)

**Western Union (global)**
- WU has Uzbekistan destination pages for the US, GB, CH, AT, FI, ES, IE, NL, FR, BE and IT. The US page offers USD→USD sends, paid by bank, cash at an agent or card. It says bank-account transfers to Uzbekistan are free on the first transfer. The UK page shows a GBP→USD estimated rate before commitment — [WU US](https://www.westernunion.com/us/en/send-money-to-uzbekistan.html); [WU GB](https://www.westernunion.com/gb/en/send-money-to-uzbekistan.html)
- WU's Uzbekistan page describes agent pickup in UZS or EUR. USD was not mentioned in the snippet reviewed, which conflicts with the USD payout implied on the US page — [WU UZ](https://www.westernunion.com/uz/en/home.html)
- Spain page: limit of about EUR 3,000 per quarter after ID verification (Spain-specific) — [WU ES](https://www.westernunion.com/es/en/send-money-to-uzbekistan.html)

**Revolut**
- Revolut has "Send money to Uzbekistan" pages in EEA locales (SE, IS, SI). The search snippet indicates UZS transfers are advertised. Payout method and pricing were not fetched — [Revolut SE](https://www.revolut.com/en-SE/money-transfer/send-money-to-uzbekistan/)

**Profee (additional significant provider)**
- Profee has corridor pages UAE→Uzbekistan and UK→Uzbekistan targeting Uzcard and Humo cards. The snippet showed a 0% fee on the first transfer (promo) — [Profee UAE→UZ](https://www.profee.com/send-money/from-united-arab-emirates-to-uzbekistan); [Profee UK→UZ](https://www.profee.com/cs/send-money/from-united-kingdom-to-uzbekistan)

**TransferGo**
- TransferGo has a "send money to Uzbekistan" page (Spanish locale). Details were not fetched — [TransferGo ES](https://www.transfergo.com/es/envia-dinero-a-uzbekistan)

**South Korea corridor**
- **Hanpass → Uzcard** (Uzcard announcement, undated; the image path suggests about July 2024): payout to any Uzcard, including co-badged cards, using only the card number. Flat fee **2,500 KRW** regardless of amount. Per-transaction limit **UZS 56,000–10,000,000**, monthly limit **UZS 100,000,000**. Support in Uzbek and Russian, 24/7 — [Uzcard news](https://api.uzcard.uz/en/?p=6565)
- **E9PAY → Humo**: HUMO, E9PAY and Octobank launched a Korea→Uzbekistan route. E9PAY app users send to a HUMO card by card number, with Octobank as processing partner. The item is dated May 2026 (from the URL) and marked as advertising. No fee or limit was disclosed — [Spot.uz, 2026-05-08](https://www.spot.uz/ru/2026/05/08/humo/)
- **Gmoneytrans** (Korea): instant payouts to Humo and Uzcard via a Paysend partnership — [Paysend news](https://paysend.com/ae-jo/news/paysend-launches-payouts-humo-uzcard-cards)
- Context: BC Card and NIPC (HUMO's operator) signed an agreement in 2024 for cross-acceptance of domestic cards. This covers card payments, not remittances — [Asiae, 2024-08-26](https://www.asiae.co.kr/en/article/2024082609013036446)
- Mid-market KRW→UZS reference: about 9,281 UZS per 1,000 KRW, per Monito, sourced from XE. The observation date is not shown, but the page was seen in 2026-11 — [Monito KRW/UZS](https://www.monito.com/best-exchange-rates/krw/uzs/5000)

### Inferences
- Card rails (Humo/Uzcard) with UZS payout are the de facto standard for app-based remittances into Uzbekistan from Korea and Europe. Cash pickup and USD payout are mainly WU/Remitly-type networks.
- Wise is relevant only as a USD-SWIFT option, with unpredictable intermediary fees. It is not a UZS card competitor in any corridor as of 2026-11.
- At 1,000,000 KRW, Hanpass's flat 2,500 KRW fee is 0.25% before FX margin. The real cost depends on the undisclosed KRW→UZS margin.
- Remitly's US→UZ page indicates USD payout by default. A 500 USD send costs about $9–10 in fees (about 1.8–2.0%) with no FX margin if paid in USD.

### Gaps
- **No live quotes captured** for 500 USD / 500 EUR / 500 GBP / 1,000,000 KRW. All calculators (Remitly, WU, Paysend, Wise) render quotes via JS or need a session, so FX margins vs the mid-market/CBU rate are unmeasured. The next step is a headless-browser or app check per corridor.
- Not verified: MoneyGram, Ria, WorldRemit, Xe, TapTap Send Uzbekistan support. GME Remit (and whether it is the same company as Gmoneytrans; it probably is not), Sentbe, SBI Cosmoney, Coinshot, KB/Woori/Hana. Turkey: PTT/WU, Ria, Turkish banks. UAE: Al Ansari, LuLu, Al Fardan. Germany- and Poland-specific availability.
- Remitly UK/EU/Korea→Uzbekistan availability and whether Remitly pays out to Humo/Uzcard were not confirmed.
- Paysend's current fee for Humo/Uzcard is only documented in the 2020 release (outdated).

## How can prices be verified publicly: calculators, APIs, affiliate programs, ToS?

### Takeaway
Every provider checked has a public web calculator, but none offers an unauthenticated public pricing API. Wise's Comparison API (v4) aggregates third-party provider quotes, but it needs a bearer token and Wise itself doesn't pay out UZS. Wise and Remitly run affiliate programs. Commission terms are only partly public.

### Cited Findings
- Public calculator URLs: Remitly [US→UZ pricing](https://www.remitly.com/us/en/uzbekistan/pricing); Wise [UK→UZ](https://wise.com/gb/send-money/send-money-to-uzbekistan); WU [US](https://www.westernunion.com/us/en/send-money-to-uzbekistan.html) and [GB](https://www.westernunion.com/gb/en/send-money-to-uzbekistan.html); Profee [UAE→UZ](https://www.profee.com/send-money/from-united-arab-emirates-to-uzbekistan); Monito [KRW/UZS](https://www.monito.com/best-exchange-rates/krw/uzs/5000).
- **Wise Comparison API**: `GET /v4/comparisons/?sourceCurrency=..&targetCurrency=..&sendAmount=..`, with a 2026Q4-versioned path `api.wise.com/2026Q4/comparisons`. It returns price and speed for various providers, "based off of real quotes collected from 3rd party websites". The V3 endpoint is deprecated. The spec lists bearer-token (JWT) auth, and no evidence was found that it works anonymously — [Wise docs: Comparison](https://docs.wise.com/api-reference/comparison); [V3 deprecated](https://docs.wise.com/api-reference/comparison/comparisongetv3); [APIs.io listing](https://apis.io/apis/wise/wise-comparison-api/)
- **Wise affiliate program**: pays a CPA per new customer who completes a successful cross-currency action. The help centre mentions higher rates for referring large-amount senders. A third-party "~2.4%" figure is unverified — [Wise affiliate onboarding guide](https://wise.com/gb/blog/affiliate-onboarding-guide); [Wise help](https://wise.com/help/articles/2978038/whats-the-wise-affiliate-program); [UpPromote (third-party)](https://uppromote.com/affiliate-programs/wise/)
- **Remitly affiliate program**: partners "earn a cash commission" when referred users sign up and send money, paid via its affiliate platform. No amount is stated officially. Third-party sources report about $1.60 per first transfer with a 30-day cookie, possibly via FlexOffers. Unverified. Remitly's separate Ambassador (referral) program cites at least USD 40 per qualifying transfer — [Remitly affiliate](https://www.remitly.com/us/en/landing/affiliate-program); [Remitly partnerships](https://www.remitly.com/us/en/partnerships); [Ambassador T&C](https://www.remitly.com/home/ambassador-tnc); [UpPromote (third-party)](https://uppromote.com/affiliate-program-directory/remitly)
- **World Bank RPW**: covers 367 corridors (48 sending, 105 receiving countries). The search showed a last update of 18 Aug 2025. RPW warns its data is not a substitute for provider pricing. A Korea→Uzbekistan RPW figure was not found in the results — [RPW](https://remittanceprices.worldbank.org); [RPW about](https://remittanceprices.worldbank.org/about-remittance-prices-worldwide)

### Inferences
- For a comparison app, the feasible public sources are the provider calculators (scraping, subject to ToS), the Wise Comparison API (needs a Wise API token, and its third-party coverage for UZS routes is unknown), and affiliate feeds where they exist.
- Korean apps (Hanpass, E9PAY, GME) publish fees mainly in-app or through partner press releases, so automated verification is hardest for the Korea corridor.

### Gaps
- ToS clauses on automated price collection were **not reviewed** for any provider.
- Western Union, MoneyGram, Ria and Paysend affiliate programs (Impact/CJ/Awin) and their commission terms were not verified.
- Whether Wise's Comparison API returns any providers for USD/GBP/EUR/KRW→UZS was not tested.
- Whether RPW covers any of these corridors into Uzbekistan (Korea, Turkey, UAE, US, UK, DE, PL) was not confirmed. The RPW site is JS-driven and needs manual lookup.
