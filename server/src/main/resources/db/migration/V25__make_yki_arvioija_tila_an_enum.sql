CREATE TYPE yki_arvioija_tila AS ENUM ('AKTIIVINEN', 'PASSIVOITU');

ALTER TABLE yki_arvioija
    ALTER COLUMN tila SET DATA TYPE yki_arvioija_tila USING
    CASE
        WHEN tila = 0 THEN 'AKTIIVINEN'::yki_arvioija_tila
        WHEN tila = 1 THEN 'PASSIVOITU'::yki_arvioija_tila
    END;
