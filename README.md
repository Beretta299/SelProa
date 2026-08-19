# SelProa

A vehicle history and condition service, keyed by VIN.

Give it a VIN and it tells you what the car is, what has happened to it, what is known to go wrong
with that model, and what to check before you buy. It reads the registry, inspection, insurer and
customs records; cross-checks them for odometer rollback, cloned VINs and laundered write-offs; and
answers questions from workshop manuals and service bulletins with the page it came from.

It can also refer you to a partner garage for a pre-purchase inspection — but only after you have
read and approved what gets sent.

> Built over 12 weeks as a full-time project. Course materials and the chapter plan live outside
> this repo. See `docs/decisions/0004-vin-report-not-marketplace.md` for why this is not a marketplace.

**Status:** week 1. `registry-api` runs: 20,000 vehicles, 260k history events, 240 garages, and
74 planted fraud cases across five patterns. Nothing else exists yet.

---

## The pieces

| Directory | What it is | Stack |
|---|---|---|
| `registry-api/` | A mock of the upstream data sources — registry, inspections, insurer claims, customs, plus the garage network. Built with deliberate faults | Kotlin, Ktor, Postgres |
| `selproa-api/` | The service: auth, jobs, streaming, retrieval, report generation, the agent loop, the approval gate, evals | Python, FastAPI, pgvector |
| `dashboard/` | VIN lookup, the report, saved vehicles, the approval inbox | React, TypeScript, Vite |
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
