# Remittance comparison services: data sourcing, accuracy, legal limits and business models (for a UZS-destination comparison tool)

Research date: 2026-11-22. All web sources were accessed on that date unless a different date is given. "Own test" means a live HTTP request made from this research session on 2026-11-22.

## 1. How existing comparison products source prices, how fresh they are, and whether they cover Uzbekistan

### Takeaway
Three main data models exist: (a) Wise collects data itself by going through provider checkout flows up to the payment step, then re-prices using the stored markup against the live mid-market rate. (b) Monito-style affiliate aggregators say they read "live rates and fees" but do not publish how. (c) The World Bank RPW uses quarterly mystery shopping at USD 200/500. For **Uzbekistan as a destination**, coverage is thin. Wise's comparison API returns only Remitly, and only for EUR→UZS. It returns nothing for USD, GBP or RUB→UZS, and Wise itself is absent because it cannot pay out in UZS. No Russian or Uzbek comparison service with live UZS remittance quotes turned up.

### Cited Findings
**Wise comparison (wise.com/compare + API)**
- Wise's collection method: "we use their service for bank-to-bank transfers - just as if we were customers - right until the point of paying." It runs test amounts such as £100, £500 and £2000. Only online bank-to-bank transfers are compared unless stated otherwise. — [Wise comparison disclaimer (GB)](https://wise.com/gb/compare/disclaimer)
- Markup method: Wise records each provider's advertised rate, computes the difference from the mid-market rate at collection time (the "markup"), and estimates today's rate by "subtracting their markup we previously recorded, from the latest mid-market rate." — [Wise disclaimer (GB)](https://wise.com/gb/compare/disclaimer); [Wise API docs, Comparison](https://docs.wise.com/api-reference/comparison)
- Freshness: the API docs say data is collected "about once per hour." The consumer disclaimer says "we don't collect or update data in real time" and that data is captured "on specific dates and times." — [Wise API docs](https://docs.wise.com/api-reference/comparison); [Wise disclaimer (GB)](https://wise.com/gb/compare/disclaimer); [Wise disclaimer (US)](https://wise.com/us/compare/disclaimer)
- Scope caveat: the estimates cover only FX transfers with bank-transfer pay-in and pay-out. Card, cash and other methods "can have very different pricing." — [Wise API docs](https://docs.wise.com/api-reference/comparison)
- Inclusion and commission: a provider is shown only "if the relevant data on rates and fees is available to us." Wise also states, "we don't receive any commission for featuring providers on our comparison tables." It covers "the largest banks and non-bank providers in each country in which we operate." — [Wise disclaimer (GB)](https://wise.com/gb/compare/disclaimer)
- Note the conflict of interest: Wise is itself a provider, and its comparison is a marketing tool. Its disclaimer does not describe any way for providers to consent or opt out. — [Wise disclaimer (GB)](https://wise.com/gb/compare/disclaimer)

**Monito**
- Business model: Monito earns "through affiliate links to the various payment service providers featured on our website" and runs no banners or paywalls. It states: "Commissions we may receive never impact our independence." — [Monito Score methodology (dated 2 May 2023)](https://www.monito.com/en/wiki/monito-score)
- The engine "reads the live rates and fees of every available provider." The method (APIs, partner feeds or scraping) is not disclosed. The Monito Score is built from provider terms, fee schedules, public filings and customer reviews, and is peer-reviewed before publication. — [Monito Score](https://www.monito.com/en/wiki/monito-score)
- Provider count: more than 300 providers in older coverage, and "more than 470 money transfer operators" in a newer Product Hunt listing. — [Monito search summary via Product Hunt](https://www.producthunt.com/products/monito)
- Uzbekistan: search found Monito corridor pages for UZS→KGS and UZS→KZT (outbound from Uzbekistan), where Paysend is the top pick. A UZS-destination page at monito.com/en/send-money-to-uzbekistan returned 404. — [Monito UZS→KGS](https://www.monito.com/send-money/uzbekistan/kyrgyzstan/uzs/kgs); [Monito RU UZS→KZT](https://www.monito.com/otpravlyayte-dengi/uzbekistan/kazahstan/uzs/kzt)
- Monito's pages show historical stats such as "cheapest in 100% of 81 comparisons over the last three months," which is a useful pattern for showing history and reliability. — [Monito UZS→KGS (PT)](https://www.monito.com/enviar-dinheiro/uzbequistao/quirguistao/uzs/kgs)

**World Bank Remittance Prices Worldwide (RPW)**
- Collection: researchers pose as customers and also use the firms' websites. Prices are collected for the local-currency equivalents of USD 200 and USD 500. Firms are sampled to cover at least 80% of market share per corridor. Collection happens in a short window to limit FX noise. Coverage is about 367 corridors, from 48 sending to 105 receiving countries. — [RPW methodology](https://remittanceprices.worldbank.org/methodology) (page returned 403 to the fetcher; details are from the search snippet of the same page)
- Total cost = fee + FX margin. The FX spread "is not quoted in the transfer fee," so RPW measures it separately against an interbank reference. — [RPW methodology](https://remittanceprices.worldbank.org/methodology)
- Global average cost: 6.49% in Q1 2025, up from 6.26% in Q4 2024. — [RPW main report Q1 2025](https://remittanceprices.worldbank.org/sites/default/files/rpw_main_report_and_annex_q125_1_0.pdf)
- The Russia→Uzbekistan corridor page exists, but the latest data a search snippet showed was collected on **15 Nov 2021** (Zolotaya Korona among the listed firms), so it is stale for current use. — [RPW corridor RU→UZ](https://remittanceprices.worldbank.org/corridor/RU/UZ)
- RPW certifies national databases that meet its criteria, including validation through mystery shopping. — [RPW national databases certified](https://remittanceprices.worldbank.org/national-and-regional-databases-certified-by-the-world-bank)
- An academic commentator notes that Monito and Wise aim for RPW-like transparency, but their "near-daily" data is more granular. — [Devpolicy blog, 26 Jun 2025](https://devpolicy.org/tracking-foreign-exchange-margins-in-remittances-20250626/?output=pdf)

**RemitFinder**
- Has Uzbekistan pages (send to and from Uzbekistan; USA→UZ; UK→UZ). The USA→UZ page showed a "best rate today" of UZS 12,018.7950 from MoneyGram (date of capture unclear). The site says it "partners with many money transfer companies" and that listed companies are "established business[es] we evaluated." The pages found do not explain how rates are sourced or how often they update. The page is client-rendered, so the fetcher saw only the title. — [RemitFinder USA→Uzbekistan](https://www.remitfinder.com/best-exchange-rate-to-send-money-from-usa-to-uzbekistan); [RemitFinder send to Uzbekistan](https://www.remitfinder.com/send-money-to-uzbekistan)

**Other English-language aggregators covering Uzbekistan**
- Finder (US and AU) has "send money to Uzbekistan" pages with an "Advertiser disclosure" ("Finder makes money from featured partners, but editorial opinions are our own"). It advises comparing exactly how many soʻm the recipient will get. — [Finder US](https://www.finder.com/send-money-to-uzbekistan); [Finder AU](https://www.finder.com.au/international-money-transfers/send-money-to-uzbekistan)
- BestExchangeRates lists Wise USD→UZS at 11,813.42 against a mid-market rate of 11,891 (about 0.65% below). This is a single snapshot of unknown date. — [bestexchangerates.com](https://bestexchangerates.com/rates/wise-usd-to-uzs-foreign-transfers)

**Russian and Uzbek sources**
- Search found no Sravni.ru or Banki.ru tool that compares remittance prices to Uzbekistan. Results were instead editorial reviews: Brobank, and a vc.ru 2026 article naming Zolotaya Korona, Unistream, Contact, VTB, RSHB, Gazprombank and Sber as channels. — [brobank.ru](https://brobank.ru/perevody-v-uzbekistan/); [vc.ru 2026](https://vc.ru/money/2955710-perevody-v-uzbekistan-iz-rossii-zolotaya-korona-banki-i-uzcard)
- VTB advertises transfers to Uzbekistan by phone number or card (HUMO, Uzcard), with fees of 0–1%. — [VTB](https://www.vtb.ru/personal/platezhi-i-perevody/perevody-uzbekistan/)
- Profee (a provider, not a neutral comparator) publishes "cheapest transfer methods compared" articles for EU→Uzbekistan. It reports that KoronaPay is withdrawing its European authorisation and no longer accepts new transfers from Europe. — [Profee EU→UZ comparison](https://www.profee.com/uz/articles/from-the-eu-to-uzbekistan-cheapest-transfer-methods-compared)
- Daryo's 2024 guide gives Zolotaya Korona fees as typically 0.5–1.5% (secondary and unverified), with over 3,000 points in Uzbekistan. — [Daryo, 8 Oct 2024](https://daryo.uz/en/2024/10/08/navigating-uzbekistans-money-transfer-systems-a-guide-to-international-and-local-1199273/)
- Zolotaya Korona held a 51.1% share of inbound transfers to Uzbekistan, per Daryo. — [daryo.uz](https://daryo.uz/ru/nDyfCjZd/)

### Inferences
- No live, neutral, multi-provider comparison for RUB/KZT/USD/EUR→UZS appears to exist, either in Russian/Uzbek or in the large English aggregators, apart from a few RemitFinder pages and Finder editorial pages. That is a genuine gap, and it also means there is no ready-made feed to license.
- Wise's "collect markup, then re-apply to the live mid-market rate" model is a cheap way to keep displayed prices fresh between collections. It is also exactly what makes a quote stale when a provider changes its margin, so the collection timestamp (`dateCollected`) must always be shown alongside it.
- RPW is useful only as a methodology benchmark (USD 200/500 amounts, fee + FX margin) and as a historical baseline. Its RU→UZ data from 2021 cannot be used as live prices.

### Gaps
- CompareRemit: no information found on its Uzbekistan coverage or data method.
- Sravni.ru / Banki.ru: no remittance comparison tool found. Their sites were not fetched directly, so a manual check is advised.
- Telegram bots or Uzbek sites (bank.uz, Uzcard/Humo partners, Paynet) comparing inbound remittance prices: none found.
- Monito's actual collection mechanism (API or scraping) and whether it has any X→UZS corridor pages: not confirmed.
- The RPW methodology page and the RU→UZ corridor page returned HTTP 403 to the fetcher, so current RPW UZ corridor numbers were not verified.

## 2. Wise Comparison API: fields, access, terms, UZS coverage

### Takeaway
`GET /comparisons` (API version 2026Q4) returns, for each provider, quotes with `rate`, `fee`, `markup`, `receivedAmount`, `dateCollected` and delivery estimates. The docs say authentication is required and that the data "must not be used for critical flows." Even so, in this session's own test the legacy unauthenticated `api.wise.com/v3|v4/comparisons/` endpoints still returned HTTP 200. For UZS, the only provider returned was **Remitly on EUR→UZS**.

### Cited Findings
- Endpoint and version: `GET /comparisons`, API version `2026Q4`. Security schemes: `UserToken`, `PersonalToken`, `ClientCredentialsToken`. — [Wise API reference, comparison GET](https://docs.wise.com/api-reference/comparison/comparisonget.md)
- Query parameters: `sourceCurrency`, `targetCurrency` (required); exactly one of `sendAmount` or `recipientGetsAmount`; optional `midMarketRate`, `wiseFee`, `wiseEstimatedDelivery`, `wiseQuoteCreatedTime`, `payInMethod`, `sourceCountry`, `targetCountry`, `providerCountry`, `filter` (`POPULAR`), `numberOfProviders` (default 4), `providerTypes`, `providers`, `excludePartners` (default `true`, which excludes Wise's partner banks) and `includeWise`. — [Wise API reference](https://docs.wise.com/api-reference/comparison/comparisonget.md)
- Response: top level has `sourceCurrency`, `targetCurrency`, `amount`, `amountType`, `providers[]`. Each provider has `id`, `alias`, `name`, `logos` (SVG/PNG variants plus `altText`), `type` (bank / money transfer provider / travel money), `partner` and `quotes[]`. Each quote has `rate`, `fee`, `dateCollected`, `sourceCountry`, `targetCountry`, `markup` (% over mid-market), `receivedAmount`, `sendAmount`, `isConsideredMidMarketRate` and `deliveryEstimation` (`duration.min`/`max` as ISO 8601, `providerGivesEstimate`). — [Wise API reference](https://docs.wise.com/api-reference/comparison/comparisonget.md)
- Quotes are denormalised: one provider can return several quotes for the same currency pair, one per route, target country, speed or price factor. — [Wise API docs, Comparison](https://docs.wise.com/api-reference/comparison)
- Terms: the data is informational and "must not be used for critical flows." Inaccurate data can be reported to comparison@wise.com. The docs page states no explicit rate limit or attribution rule. — [Wise API docs](https://docs.wise.com/api-reference/comparison)
- **Own test, 2026-11-22**, unauthenticated `https://api.wise.com/v4/comparisons/?sourceCurrency=…&targetCurrency=UZS&sendAmount=…` (v3 gave the same results):
  - USD→UZS 500 (also with `sourceCountry=US&targetCountry=UZ`): HTTP 200, `providers: []`
  - GBP→UZS 500 (also with `sourceCountry=GB`): `providers: []`
  - RUB→UZS 20000: `providers: []`
  - EUR→UZS 500: 1 provider, **remitly** (`partner=false`). Quote: rate 12,792.67, fee 4.99, markup 2.955%, receivedAmount 6,332,499.58, `dateCollected` 2026-11-22T14:47:17Z, sourceCountry ES. With `sourceCountry=DE`: rate 12,711.36, markup 3.57%, collected 14:46:52Z. With `excludePartners=false`: still only Remitly, but with 4 quotes.
  - USD→KZT and RUB→KZT: `providers: []` (Central Asia is thin generally).
- Wise is not among the UZS providers. Wise's US page says it is "working hard" to allow sending UZS from the US "but we're not quite there yet," and other Wise locales describe sending USD to Uzbekistan by international wire. — [Wise US/ES send-money-to-uzbekistan](https://wise.com/es-us/send-money/send-money-to-uzbekistan); [Wise FI](https://wise.com/fi/send-money/send-money-to-uzbekistan)

### Inferences
- The Wise API is usable only as a minor supplementary source for UZS: one provider, EUR corridors only. `dateCollected` was minutes old, which suggests the hourly cadence holds for this route.
- Relying on the unauthenticated legacy endpoint is risky. The docs say auth is required, so it could be shut off without notice, and the "not for critical flows" language plus the lack of explicit redistribution terms means written permission (comparison@wise.com) should be sought before republishing the data commercially.
- The per-quote `markup` and `dateCollected` fields are a good data model to copy for the site's own schema.

### Gaps
- Wise's full API terms of use for comparison data (redistribution, attribution, rate limits) were not found in the docs page.
- How to obtain a `ClientCredentialsToken` for the comparison endpoint (whether a partner agreement is needed) was not confirmed.

## 3. Affiliate economics: commission models and networks

### Takeaway
The standard model is CPA per new customer who completes a first transfer, typically paid through networks: Wise on Partnerize; Remitly on Impact and FlexOffers, plus its own Ambassador programme. Public payout figures are sparse and conflict between sources. Wise is roughly £10–50 or $10 per new user (third-party figures), and Remitly is $1.60 per first transfer (FlexOffers) versus $5–20 (other directories). No figures were found for Western Union, Paysend or Xe.

### Cited Findings
- **Wise:** CPA for "any new paying customer" who makes a cross-currency transaction of any amount. The official guide says there is no conversion/cookie window limit. Conversions are approved within 30 days and paid via Partnerize by the 7th of each month. Commission can be reviewed at milestones, and business referrals pay more. — [Wise affiliate onboarding guide](https://wise.com/gb/blog/affiliate-onboarding-guide); [Wise Partnerize reporting guide](https://wise.com/gb/blog/reporting-in-partnerize); [Wise affiliate programme](https://wise.com/gb/affiliate-program)
- Wise amounts (third-party, conflicting): £10–£50 per new user, depending on market and transfer value. — [UpPromote directory](https://uppromote.com/affiliate-directory/wise/). $10 per action for the USD market. — [affi.io](https://affi.io/m/transferwise-usd). A 365-day cookie with a 30-day transfer window, which contradicts Wise's own "no window limit". — [WeCanTrack](https://wecantrack.com/programs/wise-affiliate-program/)
- **Remitly:** $1.60 per first transfer (web and app), "subject to change". — [FlexOffers Remitly](https://www.flexoffers.com/affiliate-programs/remitly-affiliate-program). Other directories cite $4–20 or $5–20 per new user, which is likely outdated. — [affpaying](https://www.affpaying.com/remitlyaffiliateprogram); [getlasso](https://getlasso.co/affiliate/remitly/). 30-day attribution window. Free to join. — [Remitly partner program](https://www.remitly.com/us/en/landing/affiliate-program). The individual "Ambassador" programme pays per qualifying first transfer by a new customer, and the amount varies by country. — [Remitly Ambassador T&C](https://www.remitly.com/home/ambassador-tnc); [Remitly Ambassador](https://www.remitly.com/us/en/partnerships/ambassador-program)
- **WorldRemit:** pays for every new customer who signs up and completes a transfer through the tracking link. Commission is paid within 30 days after the performance month ends. No amount is published. — [WorldRemit partners & affiliates](https://www.worldremit.com/en/partners-and-affiliates)
- **Monito** confirms that affiliate links are its revenue model. — [Monito Score](https://www.monito.com/en/wiki/monito-score). **Finder** discloses that it "makes money from featured partners." — [Finder](https://www.finder.com/send-money-to-uzbekistan). **Wise** says it takes no commission on its comparison. — [Wise disclaimer](https://wise.com/gb/compare/disclaimer)

### Inferences
- For a UZS-focused site, most real volume comes from Russia and Kazakhstan, through Zolotaya Korona, banks, and Uzcard/Humo card-to-card transfers. Western affiliate programmes (Remitly, Wise, WorldRemit) mostly cover EU/UK/US→UZ, which is a minority of flows. Affiliate revenue is therefore likely modest at first, and RU-side operators may have no public affiliate programmes at all.
- At about $1.60–$10 per new customer, sustaining a site needs many first-time conversions. Display advertising or B2B data sales may matter as much as affiliate income.

### Gaps
- Western Union, Paysend, Xe, KoronaPay, Unistream: no public CPA or revenue-share figures, and no confirmed networks (Impact, CJ, Awin) found.
- Whether Remitly's programme currently runs on Impact (versus FlexOffers only) was not verified from a primary source.

## 4. Legal constraints: scraping, ToS/robots.txt, comparison-site rules

### Takeaway
In the EU, a site's terms of use can contractually ban scraping even of data with no IP protection (Ryanair v PR Aviation, CJEU 2015). robots.txt files of the major providers mostly block transfer and checkout paths, which is where calculator quotes live. UK FCA rules on ranking neutrality apply formally only to high-cost credit comparison, but they are the de facto standard: rank by total cost, keep paid placements outside the ranking table, and disclose commercial ties.

### Cited Findings
- **Ryanair v PR Aviation (CJEU, C-30/14, Jan 2015):** where a database is not protected by copyright or the sui generis database right, the owner can set contractual restrictions on its use, including bans on screen-scraping by a price comparison site. — [Pinsent Masons Out-Law](https://www.pinsentmasons.com/out-law/news/website-operators-can-prohibit-screen-scraping-of-unprotected-data-via-terms-and-conditions-says-eu-court-in-ryanair-case); [Osborne Clarke](https://marketinglaw.osborneclarke.com/media-and-ip/could-ryanair-control-use-of-its-flight-data-by-pr-aviation-without-database-right/); [Kluwer Copyright Blog](https://legalblogs.wolterskluwer.com/copyright-blog/ryanair-ltd-v-pr-aviation-bv-contracts-rights-and-users-in-a-low-cost-database-law/)
- Enforceability depends on the terms having been properly notified or accepted (for example, click-to-accept). National positions vary, and Irish courts had not decided on database rights in website data. — [Pinsent Masons, screen scraping in Ireland](https://www.pinsentmasons.com/out-law/analysis/the-legality-of-screen-scraping-in-ireland)
- **robots.txt (own fetch, 2026-11-22):** Remitly disallows `/*/transfer/`, `/transfer/payouts/`, register and oauth paths. Paysend disallows `/api/`, `/send/`, `/web/`, `/corridor-render/`. Wise disallows `/*/*/currency-converter/` and `*/send-money/?sourceCurrency=*` and `/widget`. KoronaPay disallows `/*?sending*`, `/*?location=*`, `/*?phone=*` and tracking parameters. Western Union disallows only a few internal paths. Monito returned no disallow lines. — robots.txt at [remitly.com](https://www.remitly.com/robots.txt), [paysend.com](https://paysend.com/robots.txt), [wise.com](https://wise.com/robots.txt), [koronapay.com](https://koronapay.com/robots.txt), [westernunion.com](https://www.westernunion.com/robots.txt)
- **UK FCA, CONC 2.5A (high-cost short-term credit on PCWs):** results must not have ranking or prominence based on commercial interests. Commission or advertising payments "should not affect the ranking or prominence." Products are ranked in ascending order of total amount payable. Paid adverts must sit "outside of the ranking tables and not interspersed within them." — [FCA Handbook CONC 2.5A](https://handbook.fca.org.uk/handbook/CONC/2/5A.html); [FCA PS16/15](https://www.fca.org.uk/publication/policy/ps16-15.pdf); [Pinsent Masons on FCA payday PCW rules](https://www.pinsentmasons.com/out-law/news/price-comparison-websites-should-display-payday-loan-offers-in-order-of-price-and-separate-from-paid-for-adverts-says-uk-regulator)
- The FCA's general-insurance PCW review (TR14/11) criticised sites that did not make clear their role or their ownership by insurers or brokers. — [FCA TR14/11](https://www.fca.org.uk/news/tr14-11-price-comparison-websites-in-the-general-insurance-sector); [FCA press release](https://www.fca.org.uk/news/press-releases/price-comparison-websites-failing-meet-fca-expectations)
- EU consumer-law "Omnibus" Directive (EU) 2019/2161 (Enforcement and Modernisation) applies to digital operators including comparison platforms. A secondary source says this, but the exact ranking-parameter provisions were not retrieved. — [Montel blog](https://montel.energy/resources/blog/what-the-eu-omnibus-directive-means-for-the-energy-market)

### Inferences
- The safest data strategy, in order: (1) official APIs or partner feeds with written permission (Wise comparison, provider affiliate or price feeds); (2) manual or semi-manual "mystery shopping" at fixed times, as RPW and Wise do; (3) scraping public, non-disallowed pages, never logged-in or checkout flows, and only where the ToS does not prohibit it.
- Even outside UK/EU jurisdiction, adopting FCA-style neutrality (rank by recipient-gets, separate any sponsored slot, disclose affiliate links) is the cheapest way to earn trust.
- Uzbek law on comparison sites and advertising of financial services was not researched here.

### Gaps
- EU Payment Accounts Directive 2014/92/EU Article 7 (certified comparison websites: independence, date of last update, and similar): the EUR-Lex fetch returned empty, so the text was not verified.
- The specific ranking-transparency provisions of Directive 2019/2161 (the Article 6a/7(4a) UCPD amendments) were not retrieved.
- hiQ v LinkedIn (US) and the CFAA position were not covered by the results.
- Uzbek (CBU) or Russian rules on aggregators of payment services were not researched.
- The actual ToS text of Remitly, Western Union, KoronaPay and Paysend on automated access was not fetched.

## 5. Best practices for trustworthiness (total cost, ranking, timestamps, staleness, promotions)

### Takeaway
The established practice: compute total cost = fee + FX margin against a stated mid-market reference, rank by "recipient gets" at a fixed send amount, show the collection timestamp for each quote, state the scope (pay-in and pay-out method), disclose how the site is paid, and separate first-transfer promotions from standard pricing.

### Cited Findings
- **Total cost = fee + FX margin:** RPW notes the FX spread "is not quoted in the transfer fee," so both must be measured. — [RPW methodology](https://remittanceprices.worldbank.org/methodology). Wise reports `markup` as a % over mid-market per quote, plus `isConsideredMidMarketRate`. — [Wise API reference](https://docs.wise.com/api-reference/comparison/comparisonget.md)
- **Recipient-gets ranking:** Finder advises comparing "exactly how many Uzbekistani soʻms your recipient will end up with." — [Finder](https://www.finder.com/send-money-to-uzbekistan). Profee says not to look at the rate alone but to check how much money the recipient gets. — [Profee](https://www.profee.com/articles/send-soms-dollars-to-uzbekistan?parent=sending-money). The FCA standard ranks by total amount payable, ascending. — [FCA CONC 2.5A](https://handbook.fca.org.uk/handbook/CONC/2/5A.html)
- **Fixed reference amounts:** RPW uses USD 200 and 500. — [RPW methodology](https://remittanceprices.worldbank.org/methodology). Wise uses £100, £500 and £2000, applying the rate of the higher threshold. — [Wise disclaimer](https://wise.com/gb/compare/disclaimer)
- **Timestamps and staleness:** Wise exposes `dateCollected` per quote and warns users that figures may differ from the provider's site, advising them to "always confirm your transfer details before committing." — [Wise API reference](https://docs.wise.com/api-reference/comparison/comparisonget.md); [Wise disclaimer](https://wise.com/gb/compare/disclaimer). Monito warns that transfer prices can change daily. — [Monito RU pages](https://www.monito.com/otpravlyayte-dengi/uzbekistan/kyrgyzstan/uzs/kgs). RPW collects within a short window to control for FX movement. — [RPW methodology](https://remittanceprices.worldbank.org/methodology)
- **Scope labelling:** Wise states that its estimates apply only to bank-transfer pay-in and pay-out, and that card or cash can differ a lot. — [Wise API docs](https://docs.wise.com/api-reference/comparison)
- **Delivery-time representation:** Wise encodes "up to X" as max-only, "from X" as min-only, and an exact time as min = max. — [Wise API docs](https://docs.wise.com/api-reference/comparison)
- **Promotions:** providers advertise first-transfer offers, for example MoneyGram's "$0 fees on a first send," Profee's "0% commission on the first transfer" and Remitly's "New customer offer" pages. — [MoneyGram UZ corridor](https://www.moneygram.com/us/en/corridor/uzbekistan); [Profee to Uzbekistan](https://www.profee.com/ru/send-money/to-uzbekistan); [Remitly Uzbekistan](https://www.remitly.com/us/en/uzbekistan)
- **Disclosure and independence:** Monito ("commissions… never impact our independence"), Finder ("Advertiser disclosure") and Wise ("no commission for featuring providers") all state their position on page. — [Monito](https://www.monito.com/en/wiki/monito-score); [Finder](https://www.finder.com/send-money-to-uzbekistan); [Wise](https://wise.com/gb/compare/disclaimer)
- **Historical reliability:** Monito shows "cheapest in X% of N comparisons over last 3 months." — [Monito UZS→KGS (PT)](https://www.monito.com/enviar-dinheiro/uzbequistao/quirguistao/uzs/kgs)
- **Error reporting channel:** Wise publishes comparison@wise.com for inaccurate data. — [Wise API docs](https://docs.wise.com/api-reference/comparison)

### Inferences
- For a UZS tool: default ranking by "recipient gets (UZS)" for a fixed send amount per corridor (for example 5,000 / 20,000 RUB; 100 / 500 USD), with a per-row "collected at" time. Fade or flag rows older than a threshold (for example more than 6–12 hours for app quotes), and fall back to "last known" with an explicit label.
- Show first-transfer promotions as a separate badge or column ("first transfer only"). Rank by standard price, or offer a toggle, so promotions do not distort the default ranking.
- Use one stated mid-market reference (for example the CBU official rate, or an interbank or market mid rate) and name it on the page. The FX-margin column is only meaningful if that reference is consistent and timestamped.
- Label the payout method per row (card Uzcard/Humo, cash, account), because Wise and RPW both show that price varies strongly by method.

### Gaps
- No source found describing how comparators handle promotions in their ranking algorithms (whether they exclude them or show them separately); this is only observed indirectly through provider marketing.
- No published staleness thresholds from Monito or RemitFinder were found.
