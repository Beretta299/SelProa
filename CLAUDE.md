# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" -> "Write tests for invalid inputs, then make them pass"
- "Fix the bug" -> "Write a test that reproduces it, then make it pass"
- "Refactor X" -> "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] -> verify: [check]
2. [Step] -> verify: [check]
3. [Step] -> verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

# Project: SelProa

A copilot for buying and running cars. Built over 12 weeks as a learning project — the point is
that **Dmytro learns this**, not that it gets finished fast.

## What that changes about how you help

- **Explain the reasoning behind decisions**, including what was rejected and why. A choice without
  its alternative is just a fact about the stack.
- **Don't silently do the thinking parts.** Scaffolding, plumbing and boilerplate: go ahead.
  Schema design, prompt design, retrieval tuning, eval rubrics: propose and discuss, don't just ship.
- Kotlin is his strong language. Python he knows but has not shipped a production service in.
  TypeScript, React, retrieval, agents, Terraform and cloud are all new.

## Things that look like bugs and are not

- **`market-api` has three deliberate faults** — inconsistent field naming, page-size drift, and
  HTTP 200 with an empty body on a sold listing. See `docs/faults.md`. **Never fix them.**
  Chapter 13 exists to defend against them.
- **`market-api/seed-truth.json` is gitignored on purpose.** It is the ground truth for the planted
  fraud. Do not read it, do not let any service read it, and do not put it anywhere a retrieval
  corpus could reach. Regenerate with `./gradlew seed` (deterministic on `SEED` in `Seed.kt`).

## Local setup

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # JDK 21 is keg-only
colima start                                    # docker runtime
docker compose -f infra/docker-compose.yml up -d
```

Postgres is on **5433** (not 5432), `market-api` on **8081**.

## Conventions

- One toolchain per service. Nothing shared between them except the OpenAPI schema and the Makefile.
- Migrations are Flyway SQL in `market-api/src/main/resources/db/migration`. Never edit an applied
  migration; add a new one.
- Decisions worth defending go in `docs/decisions/` as ADRs, using `0000-template.md`.
- `make check` must stay identical locally and in CI.
