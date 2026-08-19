# Deliberate faults in registry-api

`registry-api` stands in for the upstream sources a vehicle history service buys from: a national
registry, technical inspection records, insurer claims, service networks, customs. It is built badly
on purpose — every real integration target misbehaves, and a client written against a well-behaved
service is useless in the job this project is preparing you for.

Chapter 13 defends against all three. Do not fix them here.

---

## 1. The same field has two names

| Endpoint | Field |
|---|---|
| `GET /vehicles/{vin}` | `model_year` |
| `GET /vehicles/{vin}/history` | `eventType`, `occurredOn`, `odometerKm` |
| `GET /events/{id}` | `event_type`, `occurred_on`, `odometer_km` |

```bash
curl -s localhost:8081/vehicles/$VIN | jq 'keys'
curl -s localhost:8081/vehicles/$VIN/history?limit=1 | jq '.items[0] | keys'
```

**Why it is here:** registries grow in layers written by different people at different times. If both
spellings leak past your client, everything downstream has to know about both, forever. Normalise in
the adapter (chapter 3).

## 2. The requested page size is advisory

Any `limit` divisible by four is quietly reduced to three quarters. Verified: asked 4 → got 3,
asked 8 → got 6, asked 12 → got 9.

**Why it is here:** a client that pages by counting rows against the limit it asked for will skip
events without ever erroring — and a skipped odometer reading is a missed rollback. Page with
`next_cursor` and stop when it is null.

## 3. HTTP 200 with an empty body on a lapsed partner

`POST /vehicles/{vin}/referrals` against a garage whose partnership has expired returns **200 with a
zero-length body**. No error.

```bash
curl -s -o /dev/null -w '%{http_code} %{size_download}\n' \
  -X POST localhost:8081/vehicles/$VIN/referrals -H 'Content-Type: application/json' \
  -d '{"garage_id":11,"requested_for":"2026-08-25T10:00:00Z",
       "customer_name":"...","customer_phone":"..."}'
# → 200 0
```

**Why it is here.** This is the important one. Every layer treats 2xx as success, so a client that
checks the status code tells the user their inspection is booked. It is not. They will drive to a
garage that is not expecting them, for a car they are about to buy.

Validate the response *shape*, not the status code. Chapter 13 turns this into an explicit error, and
chapter 19 is why it matters: by then a human has approved that referral, and the referral is how the
partner network gets paid.

---

## What is not a fault

- `check_digit_valid` being false on some vehicles — that is a **fraud signal**, not a data problem.
  The ninth VIN character is a checksum; a fabricated VIN usually fails it.
- Events with a null `odometer_km` — ownership changes and damage claims do not read the odometer.
- A vehicle with foreign events before its import — that is a normal imported car.
- A vehicle with foreign events *overlapping* its Polish ones — that is a cloned VIN, and finding it
  is the product.
