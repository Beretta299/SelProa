-- registry-api schema.
--
-- Stands in for the upstream sources a vehicle history service buys from: a
-- national registry, technical inspection records, insurer damage claims,
-- service networks, customs and auction houses.
--
-- Deliberately imperfect. See docs/faults.md.

-- One row per VIN. What the car is.
create table vehicles (
    vin                text primary key,
    make               text not null,
    model              text not null,
    model_year         int  not null,
    body_class         text,
    fuel_type          text,
    displacement_l     numeric(3,1),
    engine_code        text,
    power_hp           int,
    plant_country      text,
    -- A VIN's ninth character is a checksum. A fabricated VIN usually fails it,
    -- which makes this the cheapest fraud signal in the whole product.
    check_digit_valid  boolean not null default true,
    first_seen_at      timestamptz not null default now()
);

-- The spine of the product. Everything a report says comes from these.
create table history_events (
    id           bigserial primary key,
    vin          text not null references vehicles (vin),
    event_type   text not null check (event_type in (
                     'registration', 'ownership_change', 'technical_inspection',
                     'service', 'damage_claim', 'total_loss', 'theft_report',
                     'theft_recovery', 'import', 'export', 'auction_sale',
                     'mileage_reading', 'scrapped')),
    occurred_on  date not null,
    odometer_km  int,                        -- inspections, services and auctions carry one
    country      text not null default 'PL',
    source       text not null,              -- cepik | insurer | service_network | customs | auction
    detail       jsonb not null default '{}',
    recorded_at  timestamptz not null default now()
);

create index history_vin_idx      on history_events (vin, occurred_on);
create index history_odometer_idx on history_events (vin, occurred_on) where odometer_km is not null;
create index history_type_idx     on history_events (event_type);

-- The partner network. Garages and independent mechanics who take inspection
-- work from report readers and promote the service in return.
create table garages (
    id            bigserial primary key,
    name          text not null,
    city          text not null,
    voivodeship   text not null,
    address       text not null,
    specialties   text[] not null default '{}',
    partner       boolean not null default false,
    rating        numeric(2,1),
    inspection_price_pln int not null,
    phone         text not null,
    accepts_until date                       -- partnership expiry; null means open-ended
);

create index garages_city_idx on garages (city) where partner;

-- The consequential write. Creating one of these tells a real business a real
-- customer's name and number, and commits them to an appointment.
create table inspection_referrals (
    id              bigserial primary key,
    vin             text   not null references vehicles (vin),
    garage_id       bigint not null references garages (id),
    requested_for   timestamptz not null,
    customer_name   text   not null,
    customer_phone  text   not null,
    note            text,
    status          text   not null default 'sent'
                       check (status in ('sent', 'accepted', 'declined')),
    idempotency_key text   unique,
    created_at      timestamptz not null default now()
);
