# Architecture

> Chapter 1. Draw it before the spec is finished — the expensive mistakes here are structural.
> Revise it twice more and you have the most useful artefact you will own in a system design round.

## Diagram

> Services, data stores, background jobs, and **every boundary where something untrusted enters**.
> Untrusted here means: any document text, and anything a user or a partner garage supplies. Mark those explicitly — chapter 30 depends on knowing where they are.

```
(replace with your drawing — excalidraw export, mermaid, or ASCII)
```

## Components

| Component | Owns | Talks to |
|---|---|---|
| registry-api | | |
| selproa-api | | |
| worker | | |
| dashboard | | |
| Postgres | | |
| Redis | | |

## Trust boundaries

| Boundary | What crosses it | Trusted? |
|---|---|---|
| | | |

## Open questions

>
