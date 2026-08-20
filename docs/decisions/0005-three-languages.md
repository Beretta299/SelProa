# ADR 0005 — Three languages, source documents untranslated

**Date:** 2026-08-20
**Status:** accepted

## Context

SelProa serves Polish, English and Ukrainian. The people buying used cars in Poland include a large
Ukrainian community, and the person building it is a Ukrainian speaker living in Wrocław — so this is
a market call, not a checkbox.

The source documents do not follow that split. A workshop manual for a German car is written in
German or English. A Polish inspection record is in Polish. A recall notice may be in either.

## Decision

**Documents stay in the language they were written in.** Questions are translated into the document's
vocabulary for retrieval, answers are generated in the reader's language, and every citation points at
the original page in its original language.

Figures — intervals, torque values, part numbers, fault codes — are carried across verbatim and never
restated, exactly as they already are within one language.

## Instead of

- **Translating the corpus once, at ingest.** Rejected. Every citation would then be a claim about a
  translation, and a reader who opens the cited page finds text that does not match what they were
  told. It also multiplies the corpus by three and makes a bad translation permanent.
- **Answering in the document's language.** Rejected. It defeats the purpose: a Ukrainian speaker who
  can read a German manual does not need this.
- **English only, translate later.** Rejected. Retrofitting language into a retrieval system means
  redoing the query rewriting, the prompts and the entire golden set. It is cheaper in week 6 than
  in week 11.

## Consequences

The interesting problem is not translation. It is that a question in Ukrainian must retrieve from a
German manual and produce a Ukrainian answer citing the German page, **with the numbers intact across
two language boundaries**. The evaluation harness has to check that specifically, which means the
golden set carries a language dimension and the judge is told to compare figures, not prose.

The query-rewriting prompt already restates a question in the document's vocabulary. That is now doing
two jobs — vocabulary and language — and it is where most of this work lands.

Costs roughly three days beyond the single-language version. Paid for out of the photograph work in
week 11, which is the first thing cut if this overruns.

**Revisit if** per-language evaluation shows one language materially worse than the others; the answer
then is language-specific retrieval, not a bigger model.
