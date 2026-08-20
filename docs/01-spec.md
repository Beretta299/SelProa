# Spec

> **Draft proposed by Claude on 2026-08-20. Argue with it.**
> The numbers and the cut lines are guesses. Every one of them is yours to change, and the ones I am
> least sure about are marked. What matters is that after you have edited it, this document — not
> your memory — is what week 8 gets scoped against.

## What it does

You are about to buy a used car. You have a VIN and a phone number from the advert. SelProa tells you
what that car actually is, what has happened to it, whether the records contradict each other, who is
selling it and what else they are selling, and what a mechanic should look at before you hand over
money. Every statement it makes points at the record it came from.

It speaks **Polish, English and Ukrainian** — the three languages of the people actually buying used
cars in Poland.

The basic report is free. A **paid report** adds what the manufacturer built into that specific car:
the factory options decoded from the VIN, so you can tell whether the "full option" advert is true.
Revenue also comes from the partner network — independent garages and local dealerships who take
pre-purchase inspection work from report readers and carry the service in return.

## Not building

- **Prices and valuation.** Not "is this a fair price", not market comparisons. There are ten tools
  that do this and it is not what people get wrong when buying a car.
- **A marketplace.** No listings, no browsing, no search by make and model. The only advert data held
  is which contact advertised which VIN, where and when — the minimum that makes seller analysis
  possible. See ADR 0004.
- **Insurance, financing, warranty, parts ordering.** Adjacent businesses, all of them bigger than
  this one.
- **OBD-II or live telemetry.** Requires hardware in the car; you do not own the car yet.
- **A mobile app.** ← *the one I am least sure about, given seven years of Android. Cut for now
  because the roles you are applying to assume web. Revisit at week 10 if there is slack.*
- **A payment processor, for now.** The paid tier is real, but in the first 12 weeks it is an
  *entitlement* — the service knows who is entitled to what, meters it and gates it. Wiring Stripe is
  three or four days of plumbing that teaches nothing this project is for, and the entitlement layer
  is needed either way. Decide at week 7, when there is something worth charging for. ← *decision
  deferred deliberately; the trigger is a real user asking to pay.*
- **Multi-user accounts, teams, sharing.** One person, one login.
- **Automated advert scraping at scale.** Legal exposure, unstable, unpublishable.
- **Languages beyond Polish, English and Ukrainian.**
- **Translating the source documents.** Manuals and bulletins stay in the language they were written
  in. Questions are translated for retrieval, answers are written in the reader's language, and every
  citation points at the original page. Translating a corpus would make every citation a claim about
  a translation.
- **Anything that writes to a registry.** Read-only against every upstream source, except the garage
  referral.

## What it must do

Each one is a thing a person does and sees. If it cannot be written this way it is an implementation
note, not a requirement.

1. Paste a VIN, and within 10 seconds see what the car is — make, model, year, engine, where it was
   built — or a clear "this VIN is not one we hold".
2. Paste a VIN whose checksum does not compute, and be told **that first**, before anything else,
   because nothing after it can be trusted.
3. See the car's odometer as a timeline of dated readings, and if any reading is lower than an earlier
   one, see both dates and both figures called out at the top of the report.
4. See every event in the car's life — registrations, inspections, services, damage claims, imports,
   changes of owner — each with its date, the country it happened in, and which source reported it.
5. Click any claim in the report and see the underlying record it came from.
6. Paste the seller's phone number and see how many other cars that number has advertised, over what
   period, in how many cities — and whether it claims to be a private seller while behaving like a
   dealer.
7. Ask a question in plain language — "what goes wrong with this engine around 200,000 km" — and get
   an answer from the workshop manual or a service bulletin, with the document and page.
8. Ask something the documents do not cover and be told so plainly, along with what kind of document
   would answer it, rather than receiving a guess.
9. See partner garages near the car with their price for a pre-purchase inspection, and request one.
10. Before any request reaches a garage, read the exact message, edit it, and approve or refuse it.
    Nothing leaves without that.
11. Save a VIN and be told when a new record appears against it.
12. Read the whole report, ask questions and receive answers in Polish, English or Ukrainian,
    whichever was chosen — including when the underlying manual is in a different language.
13. Pay for the advanced report and see the factory options the VIN was actually built with, so an
    advert claiming "full options" can be checked against what left the factory.
14. Photograph the VIN plate or the dashboard, and have the VIN or the odometer read from the picture
    and checked against the records. ← *this is the form the vision work takes now. It is also the
    first thing I would cut if the three languages overrun — see the budget note below.*

## Phases

| Weeks | Deliverables | Demoable at the end? |
|---|---|---|
| 1–4 | Registry mock with planted fraud · service with auth, jobs, streaming · report v1: decode, timeline, rollback detection · deployed behind a password | **Yes — applications go out in week 5** |
| 5–7 | Seller analysis · retrieval over manuals and bulletins with citations · cross-lingual questions and answers · the agent and its tools | Yes, meaningfully better |
| 8–9 | Fraud scoring across the whole registry · factory-option decoding behind the entitlement gate · the approval gate · evals in all three languages · injection defence | Yes |
| 10–11 | Terraform, CI with an eval gate, metrics and alerts · performance pass · VIN and odometer from photographs, if there is room · second demo | Yes |
| 12 | Ten real users, then act on what they say | — |

## What I demo in week 5

Paste a VIN, watch the report stream in, and show the odometer going backwards between two dated
inspection records — with both records on screen.

## The budget these answers cost

Three languages and a paid tier are not free, and the twelve weeks were already full. My estimate:

| Work | Cost |
|---|---|
| UI strings in three languages | ~1.5 days |
| Cross-lingual retrieval, generation and per-language evals | ~3 days |
| Entitlement layer, metering and gating | ~1.5 days |
| Factory-option decoding, mock and paid gate | ~1 day |

About seven working days, or a week and a half. It comes out of the photograph work in week 11 and
the slack in the performance pass. If it overruns, requirement 14 is what goes — reading a VIN from a
photo is a nice demo, and answering a Ukrainian speaker's question about a German manual is a
product.

**The part that is genuinely hard is not the translation.** It is that a question in Ukrainian has to
retrieve from a manual written in German and return an answer in Ukrainian that cites the German
page — and then be evaluated for whether the figure survived two language boundaries intact. That is
one of the more interesting problems in the whole project and it is worth the three days.

## What I do not control

| What | Needed by | If it is late |
|---|---|---|
| AWS account activation | Week 4 | Deploy to a €4 VPS instead; Terraform still lands in week 10 |
| Domain | Week 4 | Demo on the raw IP with a self-signed certificate; ugly but works |
| Anthropic API key | Week 5 | Nothing else can start; get it in week 1 |
| A real data source (vPIC, recalls) | Week 7 | The mock carries the demo; the real source is a chapter, not a dependency |

## Answered

**The referral is real.** It is one half of an integration with existing businesses — independent
garages and local dealerships — who receive inspection work and carry the service in return. Week 12's
users get asked about it as a product, not as a hypothetical.

**Freemium.** The basic report is free; the advanced report is paid, and the first thing behind that
line is factory-option decoding from the VIN. That also solves the spend problem: the expensive work
sits behind the tier that pays for it.

**Polish, English and Ukrainian.** Not a translation layer bolted on at the end — see the budget above.

## Still open

- **Does the free report cost you money before anyone pays?** A public demo with unlimited free
  look-ups and a metered model API behind it is a bill waiting to happen. Something has to cap the
  free tier before chapter 31 arrives. Suggest: free look-ups are rate-limited per IP and the model
  work in the free report is cached per VIN, so the second person to check a car costs nothing.
  You need to check it if it is, if it's not free we should make it paid. MAke a research in internet, and come back.
- **What else goes behind the paid line?** Factory options is one feature, not a tier. Candidates:
  the full event timeline versus a summary, the seller analysis, the cross-check against foreign
  registries, and the exportable PDF a buyer can show a seller. Let's find everything that we can find for car from vin-code, to make sure what we can do. MAke a research in internet, and come back.
