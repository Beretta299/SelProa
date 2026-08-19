# SelProa

A copilot for buying and running cars.

It reads workshop manuals and service bulletins and answers with the page it came from, scores used-car
listings for what a knowledgeable buyer would spot, reads the listing photos, and can contact a seller
on your behalf — but only after you have read and approved the message.

> Built over 12 weeks as a full-time project. Course materials and the chapter plan live outside this
> repo. Working name in those materials is *Pitwall*.

**Status:** week 1, scaffolding. Nothing works yet.

---

## The pieces

| Directory | What it is | Stack |
|---|---|---|
| `market-api/` | A mock used-car marketplace to integrate against — listings, VIN data, price history, a live feed, and deliberate faults | Kotlin, Ktor, Postgres |
| `pitwall-api/` | The service: auth, jobs, streaming, retrieval, the agent loop, the approval gate, evals | Python, FastAPI, pgvector |
| `dashboard/` | Chat with citations, garage, watchlist, flagged listings, approval inbox | React, TypeScript, Vite |
| `infra/` | Local compose, deployment, later Terraform and CI | Docker, Caddy, Terraform |
| `evals/` | Golden sets and the evaluation harness | Python |
| `docs/` | Spec, architecture, ADRs, API contract, eval reports | Markdown |
| `week-notes/` | One note per week — what shipped, what slipped, what was cut | Markdown |

Each service keeps its own toolchain and lockfile. Nothing is shared between them except the OpenAPI
schema (the dashboard's client is generated from it) and the `Makefile`.

## Running it

```bash
make up        # everything, locally
make check     # lint, types, tests — the same command CI runs
make evals     # the evaluation suite
```

Not yet implemented — see the Makefile.

## Documentation

- [`docs/01-spec.md`](docs/01-spec.md) — what this does, and what it deliberately does not
- [`docs/architecture.md`](docs/architecture.md) — services, stores, jobs, trust boundaries
- [`docs/decisions/`](docs/decisions/) — architecture decision records
- [`docs/api.md`](docs/api.md) — the API contract: errors, idempotency, pagination

## License

Not yet decided.
