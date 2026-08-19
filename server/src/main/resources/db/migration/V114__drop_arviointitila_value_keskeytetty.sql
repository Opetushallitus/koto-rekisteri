-- KESKEYTETTY poistui Arviointitila-enumista ja sen muuntanut YkiArviointitilaMigration-ajo
-- on poistettu. Muunnetaan mahdolliset jäljellä olevat rivit samalla logiikalla
-- (laskeArviointitila) ja poistetaan arvo enum-tyypistä, jotta vanha sovellusversio
-- ei voi enää kirjoittaa sitä.

UPDATE yki_suoritus s
SET arviointitila = laskettu.uusi::yki_arviointitila
FROM (
    SELECT
        s.id,
        CASE
            WHEN ta.suoritus_id IS NOT NULL AND ta.kasittelypaiva IS NOT NULL THEN 'TARKISTUSARVIOITU'
            WHEN ta.suoritus_id IS NOT NULL THEN 'TARKISTUSARVIOITAVA'
            WHEN coalesce(o.null_count, 0) > 0 THEN 'ARVIOITAVA'
            WHEN coalesce(o.real_grade_count, 0) = 0 THEN 'EI_SUORITUSTA'
            ELSE 'ARVIOITU'
        END AS uusi
    FROM yki_suoritus s
        LEFT JOIN (
            SELECT suoritus_id,
                   count(*) FILTER (WHERE arvosana IS NULL)                      AS null_count,
                   count(*) FILTER (WHERE arvosana IS NOT NULL AND arvosana < 9) AS real_grade_count
            FROM yki_osakoe
            GROUP BY suoritus_id
        ) o ON o.suoritus_id = s.id
        LEFT JOIN (
            SELECT osakoe.suoritus_id,
                   max(tarkistusarviointi.kasittelypaiva) AS kasittelypaiva
            FROM yki_osakoe osakoe
                JOIN yki_osakoe_tarkistusarviointi ota ON ota.osakoe_id = osakoe.id
                JOIN yki_tarkistusarviointi tarkistusarviointi ON tarkistusarviointi.id = ota.tarkistusarviointi_id
            GROUP BY osakoe.suoritus_id
        ) ta ON ta.suoritus_id = s.id
    WHERE s.arviointitila = 'KESKEYTETTY'
) laskettu
WHERE s.id = laskettu.id;

CREATE TYPE yki_arviointitila_new AS ENUM (
    'ARVIOITAVA',
    'ARVIOITU',
    'EI_SUORITUSTA',
    'TARKISTUSARVIOITAVA',
    'TARKISTUSARVIOITU',
    'TARKISTUSARVIOINTI_HYVAKSYTTY',
    'ILMOITTAUTUNUT',
    'PERUTTU'
    );

ALTER TABLE yki_suoritus
    ALTER COLUMN arviointitila TYPE yki_arviointitila_new
        USING arviointitila::text::yki_arviointitila_new;

DROP TYPE yki_arviointitila;

ALTER TYPE yki_arviointitila_new RENAME TO yki_arviointitila;
