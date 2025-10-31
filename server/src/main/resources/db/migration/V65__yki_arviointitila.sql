CREATE TYPE yki_arviointitila AS ENUM (
    'ARVIOITAVANA',
    'EI_SUORITUSTA',
    'KESKEYTETTY',
    'ARVIOITU',
    'UUSINTA'
    );

ALTER TABLE yki_suoritus
    ADD COLUMN arviointitila yki_arviointitila;

UPDATE yki_suoritus
SET arviointitila = 'ARVIOITU';

ALTER TABLE yki_suoritus
    ALTER COLUMN arviointitila SET NOT NULL,
    ALTER COLUMN arviointipaiva DROP NOT NULL;