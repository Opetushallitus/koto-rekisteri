UPDATE yki_suoritus
SET arviointitila = 'EI_SUORITUSTA'
WHERE arviointitila = 'UUSITTAVA';

CREATE TYPE yki_arviointitila_new AS ENUM (
    'ARVIOITAVA',
    'ARVIOITU',
    'EI_SUORITUSTA',
    'KESKEYTETTY',
    'TARKISTUSARVIOITAVA',
    'TARKISTUSARVIOITU',
    'TARKISTUSARVIOINTI_HYVAKSYTTY'
    );

ALTER TABLE yki_suoritus
    ALTER COLUMN arviointitila TYPE yki_arviointitila_new
        USING arviointitila::text::yki_arviointitila_new;

DROP TYPE yki_arviointitila;

ALTER TYPE yki_arviointitila_new RENAME TO yki_arviointitila;
