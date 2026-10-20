# Design Lessons

Taste notes from 15 designers, boiled down to rules we can test a screen against.
Each entry: **the idea → the rule → the question to ask of a screen.**

---

## Part 1. The 15

### 1. Dieter Rams: "Less, but better"
- **Idea:** Good design is unobtrusive, honest, thorough down to the last detail. Decoration is a lie about importance.
- **Rule:** Every element must earn its pixels. If removing it loses no meaning, remove it.
- **Ask:** What is the *one* thing on this screen? What can I delete before I add anything?

### 2. Frank Chimero: design as narrative
- **Idea:** Interfaces unfold over time. Pacing, order and voice make context legible. A screen is a scene, not a dashboard.
- **Rule:** Decide what the user reads first, second, third. Let motion and hierarchy tell it in order. Give the product a voice.
- **Ask:** If this screen were read aloud, what sentence would it say?

### 3. Maggie Appleton: embodied cognition
- **Idea:** Abstract things become understandable when mapped to space, body and texture (near/far, heavy/light, up/down).
- **Rule:** Use spatial metaphor instead of labels. A gap you can *see* beats a number you must compute.
- **Ask:** Can this comparison be shown as distance, size or position rather than text?

### 4. Tobias van Schneider: anti-sterility
- **Idea:** Clean is not the same as good. Products people love have character, a point of view, a little roughness.
- **Rule:** Pick one signature (a shape, a voice, a number style, a colour behaviour) and repeat it everywhere. Generic teal fintech is forgettable.
- **Ask:** Cover the logo. Would anyone know this is *our* app?

### 5. Don Norman: human-centered design
- **Idea:** Affordances, signifiers, feedback, mental models. If users err, the design erred.
- **Rule:** Use the user's words and model, not the system's. Labels must say what *I* do, not what the bank does.
- **Ask:** Could a first-time user misread this label? What do they expect to happen when they tap?

### 6. Julie Zhou: metrics-informed craft
- **Idea:** Polish must move comprehension, not just taste. Define what "understood" means and measure it.
- **Rule:** Every visual decision has a hypothesis: "this makes people find the best bank faster." Ship, measure, keep what works.
- **Ask:** What number would prove this change is better? Time-to-decision? Taps to route?

### 7. Robin Rendle: typography as the medium
- **Idea:** The screen is a press. Type scales, adapts, and carries the design if you let it.
- **Rule:** Fluid, tabular, well-set numerals are the interface. Type must fit its container, never overflow or wrap by accident.
- **Ask:** Does the biggest number still look designed at its worst-case length?

### 8. John Maeda: simplicity, laws of
- **Idea:** Reduce, organise, save time, learn. Subtract the obvious; keep the meaningful. Hide complexity but never the essence.
- **Rule:** Shrink the system, group what belongs together, and say *less* than you could.
- **Ask:** What is obvious here that the user already knows, and why am I still showing it?

### 9. Dan Saffer: micro-interactions
- **Idea:** Trigger, rules, feedback, loops. Delight lives in tiny single-purpose moments.
- **Rule:** Each micro-interaction does one job and confirms it instantly. Signature moments should be tied to the core task, not decoration.
- **Ask:** What is the smallest moment in this flow, and is it satisfying?

### 10. Paco Coursey: extreme tactility
- **Idea:** Zero latency, keyboard-grade speed, every control feels physical.
- **Rule:** Respond before the user finishes the gesture. Prefer instant, interruptible, reversible. No spinners where optimistic UI works.
- **Ask:** Is there any moment where the app makes the user wait, or where the UI answers late?

### 11. Luke Wroblewski: interaction geometry
- **Idea:** Phones are held in hands. Thumb reach decides where controls belong.
- **Rule:** Primary actions in the bottom third. Read-only info at the top. Put inputs where the thumb already is.
- **Ask:** Map the thumb zone. Which key actions sit in the dead zone at the top?

### 12. Jenny Odell: calm and attention
- **Idea:** Attention is a place you live in. Interfaces should protect it, not harvest it.
- **Rule:** No nags, no streaks, no alarm colours for non-emergencies. Finish the job and let the user leave.
- **Ask:** Does this screen help the user get *out* faster, or hold them?

### 13. Erik Spiekermann: typography as voice
- **Idea:** Hierarchy via size, weight and rhythm carries the information architecture.
- **Rule:** Three levels maximum per view; use weight and spacing, not boxes, to separate. Numbers and labels never compete.
- **Ask:** Squint. Is the order of importance clear without colour?

### 14. Khoi Vinh: grid discipline
- **Idea:** A grid is a promise. Alignment, ratio and rhythm make the interface feel inevitable.
- **Rule:** One spacing scale, one radius family, one baseline. Break the grid only on purpose, once, for emphasis.
- **Ask:** Count distinct radii, paddings, font sizes. Can they be halved?

### 15. Rauno Freiberg: digital physics
- **Idea:** Springs, mass, momentum and depth make a UI believable. Details compound into feel.
- **Rule:** Motion obeys physics (springs, not tweens). Elements transform into each other instead of cutting. Depth is consistent.
- **Ask:** Does anything teleport? Can the thing I tapped become the thing I see next?

---

## Part 2. Synthesis: 10 rules for this product

1. **One sentence per screen.** (Rams, Chimero, Maeda) If the screen can't say its answer in a sentence, the screen is wrong.
2. **Show the gap, don't compute it.** (Appleton) Differences become length, position or colour mass.
3. **Honest over impressive.** (Rams, Zhou, Odell) If banks differ by 0.2%, say so. Trust is the product.
4. **User's words, user's model.** (Norman) "I sell / I buy", my money, my distance, my time.
5. **Thumb first.** (Wroblewski) Actions low, reading high.
6. **Numbers are typography.** (Rendle, Spiekermann) Tabular, fitted, grouped, never overflowing.
7. **One grid.** (Vinh) Fewer radii, fewer sizes, repeat.
8. **Character on purpose.** (van Schneider, Chimero) One signature, repeated.
9. **Motion with mass.** (Rauno, Coursey, Saffer) Springs, haptics, instant response, one signature micro-interaction tied to the core job.
10. **Calm exit.** (Odell) Answer, route, done. Never nag.

### Screen checklist
- [ ] One hero answer, written as a sentence a person would say
- [ ] Worst-case data (huge, empty, offline, 1 result) still looks designed
- [ ] No alarm colour for a non-problem
- [ ] Primary action within thumb reach
- [ ] No more than 3 type levels, 1 radius family
- [ ] Something transforms instead of cutting
- [ ] Measurable success signal named

---

## Part 3. Review of Qulay Kurs (Android, checked on emulator 2026-10-20)

Screens reviewed: Kurs, Xarita, Tarix. Strengths first: restrained palette, tabular numbers, clean cards, real pull-to-refresh, good bones. The work now is **point of view and decision help**, not more polish.

### What is wrong (against the lessons)

| # | Finding | Lesson broken |
|---|---|---|
| 1 | Hero shows `3 589 666 665 480 so'm` from a remembered test amount (`298 765 432`). No fit, no sanity cap, looks broken at worst case. | Rendle, Maeda |
| 2 | Red `−1 493 827 160` under every bank. Every bank looks like a loss, red means danger. Same figure repeated on two rows. | Odell, Appleton |
| 3 | The red/pink bar under each bank name has no label, no meaning to a first-time user. | Norman |
| 4 | Toggle says "Sotish / Olish" but our own PROJECT.md principle is "Men sotaman / Men olaman". The UI drifted from the rule. | Norman |
| 5 | Location ask is a dashed box in the middle of the content, above the list. A nag. | Odell |
| 6 | Amount input and quick chips are at the top; the thumb lives at the bottom. System keyboard then covers the list. | Wroblewski |
| 7 | Map: every pin reads `BANK rate ×N`. 12 pins that look the same: `NBU 12 010` ×10. Differences of 10 so'm are noise. Map doesn't answer "where do I go". | Maeda, Appleton |
| 8 | Map peek card prints raw `298765432 USD` (no grouping). Pin text clipped (`taxt`). City pill overlaps a pin. | Vinh, Rendle |
| 9 | Tarix: `205, 1,73%` is ambiguous (comma-comma). Tooltip sits on top of the line. Gold bars share the tab with rate history. | Spiekermann, Norman |
| 10 | The most useful fact (the spread between best buy and best sell, 30 so'm) is a small third card. | Zhou |
| 11 | Visual identity is generic teal fintech. Nothing says Uzbekistan, nothing says *Qulay Kurs*. | van Schneider |
| 12 | Five radii (28/22/18/12/pill), many chip styles (pill, segmented, dropdown pill, black pill). | Vinh |

### Core product insight
The app answers "which bank gives the most so'm". But look at the data: top banks differ by **10–30 so'm per dollar (≈0.1–0.25%)**. For a normal $100 exchange, best vs. nearest is often **1 000–3 000 so'm, less than a taxi to get there.**

So the *honest and useful* question is not "who is best" but **"is the trip worth it?"** No competitor frames it this way. That is the unique vision.

---

## Part 4. Better solutions

### A. Vision: "Arziydimi?" ("Is it worth it?")
Tagline idea: **Eng yaxshi kursni emas, eng arzigulik yo'lni toping.**

Hero becomes a **verdict**, in a sentence:

> 100 dollarga **1 201 500 so'm** olasiz.
> Eng yaqin yaxshi joy: **Asaka, 400 m**.
> BRB 1 200 so'm ko'proq beradi, lekin 6 km uzoqda. **Arzimaydi.**

How: net gain = `extra so'm − travel cost` (walking free, taxi estimate from distance, plus time value). Show **break-even amount**: *"BRB faqat 800 dollardan ko'p bo'lsa arziydi."* The verdict can flip to *"Arziydi, +14 500 so'm"* for $2 000. This is the signature, the screen's voice, and it respects the user (Rams, Odell, Zhou).

### B. Kurs screen
1. **Sentence hero.** Amount is inline inside the sentence, tap to edit. Auto-fit number: above 1 billion switch to `1,2 mlrd` / scale down. Reset remembered amount if absurd; default to 100.
2. **Spread ribbon instead of red negatives.** Replace the per-row negative with a single horizontal "rate line": all 27 banks as small dots along one track, best at the right, you-nearest marked. Rows show only a muted `−1 200` in ink-soft, no red. Red is reserved for real warnings (stale data, bank closed).
3. **Thumb dock.** Amount and quick chips move into a bottom dock above the nav (or a custom numpad with haptic ticks). List scrolls above it. The top shows only the verdict.
4. **Words.** "Men sotaman / Men olaman" restored. Unlabelled bar removed.
5. **Calm location.** Ask once as a quiet chip in the header (`📍 Joylashuvni yoqing`), not a dashed box in the list. After 2 dismissals, never again.
6. **Rows.** Bank tile = a **monogram in the bank's own brand colour** (small), name, one line: `600 m · ochiq 18:00 gacha`, right side: so'm total only.

### C. Xarita
1. **Label only three pins:** *Eng yaqin*, *Eng yaxshi kurs*, *Eng arzigulik*. Everything else is a quiet dot whose size/opacity = closeness to best rate. Zoom reveals text.
2. **Walking rings** around you: 5 / 10 / 15 min (Appleton). The map becomes a story of time, not rate.
3. **Pin becomes the card.** Tapping a pin morphs it into the bottom card (Rauno). Format amounts (`298 765 432 USD`).
4. Fix clipped labels and city pill collision.

### D. Tarix
1. **Headline is a sentence:** *"Bugun dollar oxirgi oyning eng qimmat kuniga yaqin."* with a **position-in-range** bar (today vs 30d min/max). Replace `205, 1,73%` with `+205 so'm (+1,73%)`.
2. **Set an alert by dragging on the chart.** Long-press the chart, a line appears, pull it to a level, release = alert. One gesture, one purpose (Saffer, Coursey).
3. Tooltip anchors above the point, never on the line.
4. **Spread card promoted:** "Bank oladi / sotadi, farq 30 so'm (0,25%)" goes next to the chart with a short line: *"Almashtirish xarajati ≈ 0,25%."*
5. Move **gold** to a quiet "Boshqa" section or Settings sub-page. One tab = one question.

### E. Character (van Schneider, Chimero)
- **Signature number:** the so'm amount always rendered with a thin grouped-thousands rhythm and a small custom `so'm` mark; roll odometer-style when it changes.
- **Pattern, used once:** a subtle *ikat-inspired* geometric strip as a section divider or empty-state, not wallpaper. Keep it abstract and tasteful.
- **Voice:** short, human copy. *"Hammasi deyarli bir xil. Eng yaqinini tanlang."*
- **Colour behaviour:** green = best/worth it, ink = neutral, amber = stale, red = only for errors/closed.

### F. System tidy (Vinh)
- Radii: **24 (cards), 16 (controls), pill (chips)**. Delete 22/18/12 variants.
- One chip style. One segmented control. One icon button size.
- Type: keep 4 sizes; add one "display" for the hero sentence only.

### G. Physics and feel (Rauno, Coursey)
- Quick chips: haptic detent on each tap. Amount change = odometer roll.
- Bank row → sheet: shared-element transition, spring, interruptible.
- Chart scrub: haptic tick per day, stronger tick at the min/max.
- Pull-to-refresh already good; add a one-line result: *"Yangilandi, 3 bank o'zgardi."*

### H. Metrics (Zhou)
- **Time to decision:** app open → route tap (target < 10 s).
- **Verdict agreement:** % users who follow the "arziydi/arzimaydi" advice.
- **Route-tap rate on nearest vs best** (does the verdict change behaviour?).
- **Alert creation per user** from the chart gesture.

---

## Part 5. Priority order

| Order | Change | Effort | Impact |
|---|---|---|---|
| 1 | Fix hero overflow, reset absurd amount, format map card | S | high (looks broken today) |
| 2 | Remove red negatives, add muted deltas | S | high |
| 3 | "Arziydimi?" verdict + break-even | M | **the differentiator** |
| 4 | Thumb dock for amount | M | high |
| 5 | Map: three labelled pins + rings | M | high |
| 6 | Tarix sentence + drag-to-alert | M | medium |
| 7 | Radii/chip cleanup | S | medium |
| 8 | Character layer (pattern, odometer, voice) | M | medium, long-term brand |
