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
- **Payments and subscriptions.** One free report per look-up. The revenue question is the garage
  referral, and that is a partnership, not a checkout.
- **Multi-user accounts, teams, sharing.** One person, one login.
- **Automated advert scraping at scale.** Legal exposure, unstable, unpublishable.
- **Languages beyond Polish and English.**
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
12. Photograph the VIN plate or the dashboard, and have the VIN or the odometer read from the picture
    and checked against the records. ← *this is the form the vision work takes now; it replaces the
    listing-photo analysis and is more useful.*

## Phases

| Weeks | Deliverables | Demoable at the end? |
|---|---|---|
| 1–4 | Registry mock with planted fraud · service with auth, jobs, streaming · report v1: decode, timeline, rollback detection · deployed behind a password | **Yes — applications go out in week 5** |
| 5–7 | Seller analysis · retrieval over manuals and bulletins with citations · the agent and its tools | Yes, meaningfully better |
| 8–9 | Fraud scoring across the whole registry · VIN and odometer from photographs · the approval gate · evals · injection defence | Yes |
| 10–11 | Terraform, CI with an eval gate, metrics and alerts · performance pass · second demo | Yes |
| 12 | Ten real users, then act on what they say | — |

## What I demo in week 5

Paste a VIN, watch the report stream in, and show the odometer going backwards between two dated
inspection records — with both records on screen.

## What I do not control

| What | Needed by | If it is late |
|---|---|---|
| AWS account activation | Week 4 | Deploy to a €4 VPS instead; Terraform still lands in week 10 |
| Domain | Week 4 | Demo on the raw IP with a self-signed certificate; ugly but works |
| Anthropic API key | Week 5 | Nothing else can start; get it in week 1 |
| A real data source (vPIC, recalls) | Week 7 | The mock carries the demo; the real source is a chapter, not a dependency |

## Open questions

- Is the referral genuinely the business model, or is it a story to justify the approval gate? Both
  are legitimate — but decide, because it changes what week 12's users are asked to react to.
- One free report, or a limit? A public demo with an unlimited free look-up and a metered model API
  behind it is a bill waiting to happen. Chapter 31 caps it; something has to cap it before then.
- Polish or English first? The users in week 12 will be Polish. The interviewers will not be.
