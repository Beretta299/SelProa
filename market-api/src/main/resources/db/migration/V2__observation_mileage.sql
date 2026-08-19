-- Price observations carry the mileage that was advertised at the time.
--
-- Without this, an odometer rollback is only inferable from soft signals
-- (km per year, service stamps, price against peers). With it, the strongest
-- possible evidence exists in the data -- an earlier advert for the same car
-- showing a higher reading -- and chapter 25 can be scored on whether the
-- system finds it.
alter table price_history add column mileage_km int;

comment on column price_history.mileage_km is
    'Mileage advertised at the time of the observation. Null for older rows.';
