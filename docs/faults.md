# Deliberate faults in market-api

`market-api` stands in for a used-car marketplace you do not control. It is built badly on purpose:
every real integration target misbehaves, and a client written against a well-behaved service is
useless in the job this project is preparing you for.

Chapter 13 defends against all three. Do not fix them here.

---

## 1. The same entity has two different field names

| Endpoint | Identifier field |
|---|---|
| `GET /listings` | `listingId` |
| `GET /listings/{id}` | `listing_id` |

Reproduce:

```bash
curl -s 'localhost:8081/listings?limit=2' | jq '.items[0] | keys'
curl -s  localhost:8081/listings/1        | jq 'keys'
```

**Why it is here:** vendors grow their APIs in layers written by different people at different
times. If both spellings leak past your client, every function downstream has to know about both,
forever. Normalise in the adapter (chapter 3) so the mess stops at the boundary.

## 2. The requested page size is not honoured

`limit` is capped at 100, and any `limit` divisible by four is quietly reduced to three quarters.

```bash
for n in 4 8 12; do
  curl -s "localhost:8081/listings?limit=$n" | jq -c "{asked: $n, got: .page_size}"
done
```

**Why it is here:** a client that assumes `limit` is honoured, and pages by counting, will skip
rows without ever erroring. Page using `next_cursor` and stop when it is null — never by arithmetic
on the count you asked for.

## 3. HTTP 200 with an empty body when a listing has sold

`POST /listings/{id}/contact` on a listing whose status is not `active` returns **200 with a
zero-length body**. No error, no explanation.

```bash
curl -s -o /dev/null -w '%{http_code} %{size_download}\n' \
  -X POST localhost:8081/listings/3/contact -d 'Is this still available?'
# → 200 0
```

**Why it is here.** This is the important one. Every layer treats 2xx as success, so the emptiness
only surfaces three functions later as a missing field — or worse, never surfaces, and the user is
told their message was sent to a seller who never received it. Validate the response *shape*, not
the status code. Chapter 13 turns this into an explicit error, and chapter 19 is why it matters:
by then a human has approved that message.

---

## What is not a fault

- Missing `vin` on some listings — that is real. Roughly half of private listings have no VIN.
- `service_stamps` being null — the seller did not say, which is different from zero.
- `/vin/{vin}` returning 404 — the decoder does not cover every VIN, and neither do real ones.
