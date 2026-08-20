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

**Verified trap.** The CEPiK API *accepts* a `vin=` parameter and **silently ignores it**:

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
| **Netherlands RDW** | Per-plate recall chain, verified end to end (`t49b-isb7` → `referentiecode_rdw` → `j9yg-7rg9`) | Free and proven — but keyed by **plate**, not VIN |
| **Ukraine** | Open registry plus a National Police stolen-vehicle join | Free; directly relevant given the Ukrainian-language tier |
| **UK MOT history** | Full odometer series, defects, plate at test — the richest per-vehicle set in Europe | Free to query; **redistribution rights unresolved** |
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

---

## Not verified

- The KBA bulk CSV download path (the URL supplied 404s)
- UK MOT redistribution rights
- carVertical's VAT treatment at Polish checkout
- carQ.pl's data sourcing — it publishes Polish registry data at prices far below anything else here,
  and its `/regulamin` returns 404. Understand how before building on it.
