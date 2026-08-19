-- market-api schema.
--
-- This service is a deliberately imperfect stand-in for a real used-car
-- marketplace. Fields are named the way a real vendor names them, which is to
-- say inconsistently -- see the API layer for the faults that are on purpose.

create table sellers (
    id          bigserial primary key,
    kind        text        not null check (kind in ('private', 'dealer')),
    display_name text       not null,
    city        text        not null,
    voivodeship text        not null,
    joined_at   timestamptz not null default now()
);

create table listings (
    id                 bigserial primary key,
    seller_id          bigint      not null references sellers (id),
    vin                text,                       -- absent on roughly half of private listings
    make               text        not null,
    model              text        not null,
    variant            text,
    year               int         not null,
    first_registration date,
    mileage_km         int         not null,
    price_eur          int         not null,
    fuel               text        not null,
    gearbox            text        not null,
    body               text        not null,
    engine_code        text,
    power_hp           int,
    service_stamps     int,                        -- null means the seller did not say
    damaged            boolean     not null default false,
    description        text        not null default '',
    posted_at          timestamptz not null,
    updated_at         timestamptz not null,
    sold_at            timestamptz,
    status             text        not null default 'active'
        check (status in ('active', 'sold', 'withdrawn'))
);

create index listings_peer_group_idx on listings (make, model, year);
create index listings_posted_idx     on listings (posted_at desc, id desc);
create index listings_vin_idx        on listings (vin) where vin is not null;
create index listings_status_idx     on listings (status) where status = 'active';

create table price_history (
    id          bigserial primary key,
    listing_id  bigint      not null references listings (id) on delete cascade,
    price_eur   int         not null,
    observed_at timestamptz not null
);

create index price_history_listing_idx on price_history (listing_id, observed_at);

create table photos (
    id         bigserial primary key,
    listing_id bigint not null references listings (id) on delete cascade,
    url        text   not null,
    position   int    not null
);

create index photos_listing_idx on photos (listing_id, position);

-- Standing in for a VIN decoding service. Deliberately does not cover every
-- VIN that appears on a listing, because real decoders do not either.
create table vin_records (
    vin                text primary key,
    make               text not null,
    model              text not null,
    year               int  not null,
    engine_code        text,
    factory_options    text[] not null default '{}',
    manufactured_in    text
);

-- Messages the API accepts from buyers. Nothing reads these; they exist so that
-- contacting a seller is a real write with real consequences.
create table contact_messages (
    id         bigserial primary key,
    listing_id bigint      not null references listings (id),
    body       text        not null,
    sent_at    timestamptz not null default now(),
    -- set by the client; lets the caller make a retry safe. See docs/api.md.
    idempotency_key text unique
);
