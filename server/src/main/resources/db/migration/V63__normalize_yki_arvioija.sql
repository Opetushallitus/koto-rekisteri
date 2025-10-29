-- Uusi taulu yleisen kielitutkinnon arviointioikeuksille
CREATE TABLE "yki_arviointioikeus" (
                                       "id" integer GENERATED ALWAYS AS IDENTITY,
                                       "arvioija_id" integer NOT NULL,
                                       "kieli" yki_tutkintokieli NOT NULL,
                                       "tasot" text[] NOT NULL,
                                       "tila" yki_arvioija_tila,
                                       "kauden_alkupaiva" date,
                                       "kauden_paattymispaiva" date,
                                       "jatkorekisterointi" boolean NOT NULL DEFAULT 'false',
                                       "ensimmainen_rekisterointipaiva" date NOT NULL,
                                       "rekisteriintuontiaika" timestamp with time zone NOT NULL DEFAULT now(),
                                       PRIMARY KEY ("id"),
                                       FOREIGN KEY ("arvioija_id") REFERENCES "public"."yki_arvioija" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
                                       CONSTRAINT yki_arviointioikeus_unique_arvioija_kieli UNIQUE ("arvioija_id", "kieli")
);

-- Kopioi tiedot eri kielten arviointioikeuksista uuteen tauluun.
-- Arvioijalla, jolla on useamman kielen arviointioikeus, käytetään linkkaukseen pienintä löydettyä id:tä,
-- koska duplikaattirivit arvioijasta tullaan poistamaan myöhemmin.
WITH arvioija_by_oid AS (
    SELECT
        arvioijan_oppijanumero AS oid,
        min(id) AS arvioija_id
    FROM yki_arvioija
    GROUP BY arvioijan_oppijanumero
), arviointioikeus AS (
    SELECT
        arvioija_id,
        kieli,
        tasot,
        tila,
        kauden_alkupaiva,
        kauden_paattymispaiva,
        jatkorekisterointi,
        rekisteriintuontiaika,
        ensimmainen_rekisterointipaiva,
        row_number() OVER (PARTITION BY arvioija_id, kieli ORDER BY rekisteriintuontiaika DESC) rn
    FROM yki_arvioija
             JOIN arvioija_by_oid ON yki_arvioija.arvioijan_oppijanumero = arvioija_by_oid.oid
)
INSERT INTO "yki_arviointioikeus" (
    arvioija_id,
    kieli,
    tasot,
    tila,
    kauden_alkupaiva,
    kauden_paattymispaiva,
    jatkorekisterointi,
    rekisteriintuontiaika,
    ensimmainen_rekisterointipaiva
)
SELECT
    arvioija_id,
    kieli,
    tasot,
    tila,
    kauden_alkupaiva,
    kauden_paattymispaiva,
    jatkorekisterointi,
    rekisteriintuontiaika,
    ensimmainen_rekisterointipaiva
FROM arviointioikeus
WHERE rn = 1;

-- Poistetaan alkuperäisestä taulusta sarakkeet, jotka siirrettiin uuteen tauluun.
ALTER TABLE "yki_arvioija"
    DROP COLUMN "tila",
    DROP COLUMN "kieli",
    DROP COLUMN "tasot",
    DROP COLUMN "ensimmainen_rekisterointipaiva",
    DROP COLUMN "kauden_alkupaiva",
    DROP COLUMN "kauden_paattymispaiva",
    DROP COLUMN "jatkorekisterointi",
    DROP COLUMN "rekisteriintuontiaika";

-- Poistetaan arvioijataulusta rivit, joille ei löydy vastinparia arviointioikeustaulusta
DELETE FROM yki_arvioija
    WHERE id NOT IN (SELECT arvioija_id FROM yki_arviointioikeus);

-- Estetään arvioijan lisääminen samalla oidilla
ALTER TABLE "yki_arvioija"
    ADD UNIQUE ("arvioijan_oppijanumero");
