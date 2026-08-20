# Data sources

Research run 20 August 2026. Prices and endpoints move; the date on a claim matters as much as the
claim. Everything below marked **verified** was tested from this machine.

---

## The two findings that decide the business model

### 1. There is no lawful commercial route to per-vehicle Polish registry data

| Route | Status |
|---|---|
| CEPiK Open API (`api.cepik.gov.pl`) | Free and openly licensed, but returns **anonymised registration statistics with no VIN field**. You query by voivodeship and date range. |
| `historiapojazdu.gov.pl` | Genuinely rich — mileage from inspections, OC, owner count, import status. But a **human web form** requiring VIN **plus** plate **plus** date of first registration. No API, deliberate anti-scraping. |
| Individual data request | 30,40 zł per record, and requires a documented *interes prawny*. Running a vehicle-history site is not legal interest. |
| Teletransmission (art. 80c ust. 7) | Restricted by statute to entities with statutory tasks — bailiffs, paid-parking authorities. |
| EUCARIS | A treaty message exchange **between national authorities**, not a database. Not available to private companies at any price. |

**Verified trap.** The CEPiK API *accepts* a `vin=` parameter and **silently ignores it** whenever the
mandatory `wojewodztwo` + `data-od` + `data-do` parameters are present. Omit those and you get a 404
for the missing mandatory arguments, which is why this is sometimes reported as "404" — the 404 is
about the mandatory params, not the VIN. The dangerous path is the one that looks correct:

```
GET /pojazdy?wojewodztwo=02&data-od=20240101&data-do=20240131&limit=5&vin=THISISNOTAREALVIN
→ HTTP 200, 5 records
```

It does not 400, it does not 404. It returns somebody else's cars and echoes your VIN back in the
response links. A naive integration will look like it works in testing and be wrong in production —
which is exactly the class of fault `registry-api` exists to teach you to defend against.

### 2. Reselling vehicle-history reports is prohibited by nearly every provider

| Provider | Self-serve resale |
|---|---|
| CARFAX Europe | **No** — T&C §3, with §8.4 attaching €25 per resold report plus a €5,000 penalty |
| carVertical | **No** without express written consent — T&C §5.7, §7.6.1.7 |
| VinAudit | **No** without written permission |
| Auto.dev | **No** — explicitly prohibited |
| autoDNA | No blanket prohibition found; markets to "advertising portals"; already white-labels itself as VIN-Info |
| NHTSA vPIC | **Yes** — US federal public domain |

The binding constraint on SelProa as a paid product is a **contract**, not an integration. Consider
`autoDNA` first: Polish, publishes volume pricing, has a WebAPI, and its existing behaviour already
matches the model.

---

## What the market charges

**89,99 zł is the anchor price** in Poland for a single consumer report — carVertical, autoDNA and
CARFAX Europe have independently converged on the identical figure.

Wholesale, at published self-serve volume rates: **34–45 zł per report** (carVertical 100-pack
33,95 zł; CARFAX Europe 100 credits 35,99 zł net; autoDNA 100-pack 44,99 zł net). Note autoDNA and
CARFAX quote **net of 23% VAT**.

Spread is therefore roughly 45–56 zł per report before processing, marketing and refunds — real, but
not generous, and only available with resale rights.

---

## What is genuinely free

| Source | What it gives | Verdict |
|---|---|---|
| **NHTSA vPIC** | VIN structure decode, US-market. Public domain, no key, bulk DB downloadable | **Free and redistributable — but see the caveat below** |
| **NHTSA recalls** | US recall campaigns by make/model/year | Free; blind to EU-only campaigns |
| **German KBA recalls** | EU recall campaigns for the brands NHTSA misses | Free landing page confirmed; **the bulk CSV URL I was given 404s — the download path needs finding before this is actionable** |
| **Netherlands RDW** | 16.8M vehicles; per-plate recall chain verified end to end (`t49b-isb7` → `referentiecode_rdw` → `j9yg-7rg9`); **290,784 cars flagged `Onlogisch`** on odometer | Free and proven, but keyed by **plate**, not VIN (`chassisnummer` returns 400). Odd licence: CC0 that *forbids* attributing the data to RDW |
| **EU Safety Gate** | ~7,200 vehicle alerts, 2005–2026, CC0, via the weekly-report XML | Free. **Not VIN-indexed** — VINs appear only as free-text ranges in descriptions, and a VIN inside a stated range does not match. Use it as a recall corpus, not a lookup |
| **Ukraine** | Open registry with **unmasked VINs**, CC-BY, joinable to the National Police wanted list — 84 confirmed stolen-vehicle VIN matches against the 2025 registry | Free, verified, and the best per-vehicle open data found anywhere. Directly relevant to the Ukrainian-language tier |
| **UK MOT history** | Full odometer series, defects, plate at test — the richest per-vehicle set in Europe | Free to query; **whether DVSA permits displaying it in a public product is unverified, and it gates the dataset**. Answer this before designing around it |
| CEPiK Open API | Aggregate registration statistics | Free, and useless per-vehicle |

### Verified caveat on vPIC — correcting an earlier claim

I previously said vPIC decodes European VINs. **That was wrong, and it was drawn from a single
sample.** Tested against three VINs today:

| VIN | Result |
|---|---|
| `WBA8E9G50GNT12345` (US-market BMW) | BMW · 328i · 2016 — full decode |
| `WVWZZZ1KZAW123456` (EU-market VW) | VOLKSWAGEN · *no model* · 2010 — errors 1,5,14,400 |
| `TMBJJ7NE0J0123456` (EU-market Škoda) | **nothing** — errors 1,7 |

vPIC is a **US-market** database. It decodes European *brands* sold in America and degrades to
useless on EU-market VINs. European VINs also carry no valid ISO check digit, so the check-digit
fraud signal that `registry-api` implements applies to North American VINs and **not** to most cars
in Poland. That is a real limitation of the mock and should be written into the report as such
rather than papered over.

---

## Factory options — the paid feature, and why it is the hard one

The proposed first paid feature is "what options was this specific car built with". It is a good
product idea and it is the **most tightly licensed data in this entire document**.

### What a VIN decoder returns versus what a build sheet returns

| From the 17 characters alone | Only from the manufacturer's build record |
|---|---|
| Make, model family, model year, plant, body, engine family | The actual option list: which seats, which lights, which suspension, which paint code, production date, exact trim |

The first is arithmetic on the VIN. The second is a database lookup inside the manufacturer, and it
is the thing an advert claiming "full options" can be checked against.

### Every manufacturer gates it, in near-identical language

VAG's erWin portal is the clearest case. Vehicle data became **company-only** — Škoda ended
private-customer access on 18 December 2025 — and the disclaimer adds the clause that matters most:

> *"data from erWin must not be processed if there is no order for the vehicle."*

That is decisive. It licenses the data for servicing a car physically in front of you. Looking up an
arbitrary advertised VIN breaches it **even with zero automation**, so "we'll do it by hand" is not a
workaround. Stellantis prohibits "integrating in products and/or services"; Ford bars "mechanized or
algorithmic methods"; DAT restricts to "repair-cost calculation and/or vehicle valuation". The
convergence is not coincidence — it is the industry's settled position on exactly this product.

Pricing is not the barrier. VW erWin is €3,420/year for **unlimited** lookups, Škoda €2,800.80. Per
VIN that is nothing. **Eligibility and terms are the barrier.**

The competitor whose own FAQ says it plainly:

> *"Manufacturers don't give anyone API access to option level data tied to a specific VIN — not us,
> not the big platforms, not anyone."*

Sites selling per-VIN VAG option reports at €12.99 exist and their output is unmistakably erWin
data — which means they are almost certainly operating on a licence breach. **Do not build on them.**
Inheriting somebody else's breach is not a supply chain.

### Where options *are* licensable

| Route | Coverage | Price | Catch |
|---|---|---|---|
| **OneAutoAPI** | Mercedes, Ford, Opel/Vauxhall, Volvo | €1.95 → €0.85/VIN, Poland pricing published | **Not VAG, not BMW** — the two that dominate the Polish market |
| **DAT Polska** | ~40 brands | €1.85/VIN | Terms restrict to repair costing and valuation — ask before integrating |
| **Tradesoft / catcar RPO** | Opel, pre-2017 | ~$0.28/lookup | Narrow, but genuinely clean |
| Bilateral licence with VW or Mercedes | First-party | negotiated | Slow, may be refused; the only durable route |

**Ask "may this be consumer-facing?" before integrating anything.** That one question, asked first,
is the difference between a product and a cease-and-desist.

### Timing

erWin is migrating to a fully authenticated platform with no public surface, and **Poland rolls over
around mid-September 2026** — roughly five weeks out. Anything scoped against today's portals has a
short shelf life.

### Unresolved and worth a lawyer, not a scraper

The **EU Data Act (Reg. 2023/2854)**, applicable since 12 September 2025, may strengthen third-party
access to vehicle-related data. Nobody established whether it reaches factory build records. If it
does, it changes the negotiating posture with every manufacturer above. Highest-leverage open
question in this document.

## What this means for SelProa

**The free tier cannot contain anything that costs money per lookup.** Everything genuinely valuable
— odometer history, damage, ownership, import records — is 34–45 zł wholesale at best and legally
unreachable at worst. So the free report is limited to:

- VIN structure decode (weak for EU-market cars, per above)
- Recall matching from free EU and US sources
- **Our own analysis** — the cross-record consistency checks, the seller analysis, the manual
  retrieval. This costs model tokens, not data licences, and it is the part nobody else does well.

That is the honest answer to "is it free": **the data is not free, but the analysis is ours.** The
paid tier is therefore not optional — it is the only way the per-lookup data cost is covered.

**For the twelve weeks, this changes nothing about what gets built.** `registry-api` remains the
demo backbone precisely because the real sources are gated; that was already the reasoning in
ADR 0004. Chapter 21's real source becomes the free recall data and vPIC, with their limitations
documented rather than hidden — which is a better engineering story than a clean integration would
have been.

**The commercial conversation is a week-12 activity, not a week-7 one.** Approaching autoDNA for
resale terms makes sense once there is a working product to show them.

**Poland is the weakest link, and that is worth sitting with.** Your home market has the most closed
registry of any country examined, and Germany — the main origin of Polish imports — has no public
per-vehicle API at all. The two datasets most commercially important to this product are precisely
the two that no free source supplies. Meanwhile Ukraine, the market you added for language reasons,
turns out to have the best open vehicle data found anywhere: unmasked VINs under CC-BY, joinable to
the police wanted list. That is an argument for taking the Ukrainian tier more seriously than a
translation feature.

---

## Not verified

- The KBA bulk CSV download path (the URL supplied 404s)
- Whether DVSA permits displaying UK MOT data in a public product — the single highest-value
  unanswered question in this document
- carVertical's VAT treatment at Polish checkout
- carQ.pl's data sourcing — it publishes Polish registry data at prices far below anything else here,
  and its `/regulamin` returns 404. Understand how before building on it.
- Whether the EU Data Act reaches factory build records
- Whether OneAutoAPI and DAT permit consumer-facing use of option data
