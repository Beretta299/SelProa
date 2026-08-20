-- Who is advertising the car.
--
-- A registry does not know this; it lives in the advert. This is the thinnest
-- slice of advert data that makes seller analysis possible: a contact, a VIN,
-- a place and a date. No prices, no descriptions, no search.
--
-- Phone numbers are personal data under GDPR. The plaintext number is never
-- stored: lookups hash the input and compare. The last three digits are kept
-- in clear so a report can say "the number ending 725" without the reader
-- having to trust that we matched the right one.

create table seller_contacts (
    id            bigserial primary key,
    phone_sha256  text    not null unique,
    phone_suffix  text    not null,          -- last three digits, for display only
    claimed_kind  text    not null default 'private'
                     check (claimed_kind in ('private', 'dealer')),
    first_seen_on date    not null,
    last_seen_on  date    not null
);

create index seller_contacts_hash_idx on seller_contacts (phone_sha256);

create table advert_sightings (
    id          bigserial primary key,
    vin         text   not null references vehicles (vin),
    contact_id  bigint not null references seller_contacts (id),
    seen_on     date   not null,
    city        text   not null,
    voivodeship text   not null,
    source      text   not null default 'aggregator',
    unique (vin, contact_id, seen_on)
);

create index advert_vin_idx     on advert_sightings (vin);
create index advert_contact_idx on advert_sightings (contact_id, seen_on);
