# ADR 0004 — A VIN history service, not a marketplace

**Date:** 2026-08-20
**Status:** accepted, supersedes the product described in ADR 0001–0003

## Context

The first four days built toward a used-car buying assistant: browse listings, score them for risk,
contact sellers. Two problems with it. It is a crowded idea, and the thing that made it useful — the
analysis — was buried under a marketplace nobody needs another of.

## Decision

SelProa is a **vehicle history and condition service, keyed by VIN**. You give it a VIN, it gives you
a report: what the car is, what has happened to it, what is known to go wrong with it, and what to
check before buying. No listings, no browsing, no marketplace.

Two consequences decided at the same time:

- **Market and pricing data is dropped entirely.** No peer-price comparison, no price outliers.
- **The consequential action is a referral to a partner garage.** SelProa integrates with garages
  and independent mechanics on a mutual-promotion basis: they receive inspection work from report
  readers, they promote the service. The agent proposing that referral — which discloses the
  customer's identity to a partner business and commits them to an appointment — is what the
  approval gate now guards.

## Instead of

- **Keeping listings as valuation context.** Rejected. It preserves the price-outlier detection, but
  it drags a marketplace's worth of data and UI into a product that does not need one, and the
  report is useful without a price.
- **A read-only report with no outward action.** Rejected. It would have removed the approval gate,
  which is the single most defensible piece of engineering in the whole project.
- **Sending the report to the seller as the gated action.** Rejected in favour of the referral: the
  consequence is softer, and it does not serve a business model.

## Consequences

**Better.** The real data source (chapter 21) is now genuinely strong: NHTSA vPIC decodes VINs free
with no key, and it validates the check digit — which is itself a fraud signal, since a fabricated
VIN fails it. Recall data is free from the same source. Fraud detection stops being a bolt-on and
becomes the product: odometer rollback is what a history service exists to catch.

**Worse.** Peer-group comparison is gone, so chapter 25 changes from scoring a market to scoring
vehicle histories at scale. Roughly a day of the mock is thrown away.

**Unchanged.** Everything structural: the mock upstream with its three deliberate faults, the adapter
layer, jobs, streaming, retrieval over manuals and bulletins, the agent loop, the approval gate,
evals, injection defence, and all of production.

**Revisit if** the referral network turns out to be unbuildable solo, in which case the gated action
becomes sending the report to a third party and the business model question is deferred.
