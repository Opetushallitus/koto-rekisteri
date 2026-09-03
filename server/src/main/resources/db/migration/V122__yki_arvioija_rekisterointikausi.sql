-- Rekisterointikaudesta tulee virkailijan hallittava olio: han voi lisata kauden, muokata sen
-- alkupaivaa, passivoida sen ja poistaa vaaralle henkilolle kirjatun kauden. Se vaatii kaudelle
-- pysyvan tunnisteen, jollaista append-only-loki yki_arvioija_kausi ei tarjoa.
--
-- Kausi on arvioijakohtainen, ei kielikohtainen: OPH:n vahvistama saanto on ettei henkilolla ole
-- paallekkaisia arviointikausia vaan kaikilla kielilla on sama kausi.
--
-- yki_arviointioikeus jaa paikalleen mutta muuttuu johdetuksi projektioksi, jotta
-- Rekisterointitila.SQL, listanakyman kysely ja Solki-lahetys jatkavat ennallaan.
-- yki_arvioija_kausi jaa muutoslokiksi omalla nimellaan: uudelleennimeaminen rikkoisi
-- rinnakkain ajossa olevan vanhan sovellusversion tietosivun.

-- Ensimmainen rekisterointipaiva on arvioijakohtainen tieto, ei kausikohtainen. V63:n
-- Solki-tuonnissa se voi olla vanhempi kuin yksikaan rekonstruoitavissa oleva kausi, joten sita ei
-- saa laskea uudelleen kausista vaan se siirretaan omaksi sarakkeekseen.
ALTER TABLE yki_arvioija
    ADD COLUMN arvioijan_ensimmainen_rekisterointipaiva DATE;

UPDATE yki_arvioija
SET arvioijan_ensimmainen_rekisterointipaiva = vanhin.paiva
FROM (SELECT arvioija_id, min(ensimmainen_rekisterointipaiva) AS paiva
      FROM yki_arviointioikeus
      GROUP BY arvioija_id) vanhin
WHERE yki_arvioija.id = vanhin.arvioija_id;

CREATE TABLE yki_arvioija_rekisterointikausi
(
    id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    arvioija_id    INTEGER     NOT NULL REFERENCES yki_arvioija (id) ON DELETE CASCADE ON UPDATE CASCADE,
    alkupaiva      DATE        NOT NULL,
    paattymispaiva DATE,
    passivoitu     TIMESTAMPTZ,
    passivoija_oid henkilo_oid,
    luotu          TIMESTAMPTZ NOT NULL DEFAULT now(),
    luoja_oid      henkilo_oid,
    muokattu       TIMESTAMPTZ NOT NULL DEFAULT now(),
    muokkaaja_oid  henkilo_oid,

    -- Paivien jarjestysta ei rajoiteta kannassa: tuodussa datassa on rivaeja joilla
    -- paattymispaiva edeltaa alkupaivaa, ja kovaehto tekisi niista korjauskelvottomia.
    -- Jarjestys tarkistetaan validoinnissa uusille ja muokatuille kausille.
    CONSTRAINT yki_arvioija_rekisterointikausi_unique UNIQUE NULLS NOT DISTINCT
        (arvioija_id, alkupaiva, paattymispaiva)
);

CREATE INDEX yki_arvioija_rekisterointikausi_arvioija_idx
    ON yki_arvioija_rekisterointikausi (arvioija_id, alkupaiva DESC);

CREATE TABLE yki_arvioija_rekisterointikausi_oikeus
(
    id       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kausi_id INTEGER           NOT NULL REFERENCES yki_arvioija_rekisterointikausi (id) ON DELETE CASCADE ON UPDATE CASCADE,
    kieli    yki_tutkintokieli NOT NULL,
    tasot    TEXT[]            NOT NULL,

    CONSTRAINT yki_arvioija_rekisterointikausi_oikeus_unique UNIQUE (kausi_id, kieli)
);

-- Backfill. Nykytila voittaa lokin, loki taydentaa historian.
--
-- Kaudetta ilman alkupaivaa ei voi tunnistaa eika muokata, joten niita ei siirreta. Projektion
-- kirjoitus on no-op arvioijalle jolla ei ole yhtaan kautta, joten naiden rivit sailyvat
-- koskemattomina.
--
-- Legacy-kielia ei siirreta: arviointioikeusmatriisi ei renderoi niita eika niita saa myontaa
-- eika perua, joten ne jaavat jaadytetyiksi yki_arviointioikeus-riveiksi.
WITH lahde AS (SELECT arvioija_id,
                      kieli,
                      tasot,
                      kauden_alkupaiva,
                      kauden_paattymispaiva,
                      0                     AS prioriteetti,
                      rekisteriintuontiaika AS kirjattu
               FROM yki_arviointioikeus
               WHERE kauden_alkupaiva IS NOT NULL
                 AND kieli::text <> ALL (ARRAY ['SWE10', 'ENG11', 'ENG12'])
               UNION ALL
               SELECT arvioija_id,
                      kieli,
                      tasot,
                      kauden_alkupaiva,
                      kauden_paattymispaiva,
                      1,
                      kirjattu
               FROM yki_arvioija_kausi
               WHERE kauden_alkupaiva IS NOT NULL
                 AND kieli::text <> ALL (ARRAY ['SWE10', 'ENG11', 'ENG12'])),
     -- Loki sisaltaa saman kauden useana tilannekuvana: passivointi katkaisee
     -- paattymispaivan, jolloin V119:n uniikkiehto paastaa lapi toisen rivin. Ilman tata
     -- deduplikointia yhdesta kaudesta syntyisi kaksi paallekkaista kautta.
     oikeus AS (SELECT DISTINCT ON (arvioija_id, kauden_alkupaiva, kieli) arvioija_id,
                                                                          kauden_alkupaiva,
                                                                          kieli,
                                                                          tasot,
                                                                          kauden_paattymispaiva
                FROM lahde
                ORDER BY arvioija_id, kauden_alkupaiva, kieli, prioriteetti, kirjattu DESC),
     -- Vasta deduplikoinnin jalkeen paivapariin ryhmittely on turvallista: jaljelle jaavat
     -- erot ovat aitoja Solki-aikaisia kielikohtaisia poikkeamia.
     kausi AS (SELECT arvioija_id, kauden_alkupaiva, kauden_paattymispaiva
               FROM oikeus
               GROUP BY arvioija_id, kauden_alkupaiva, kauden_paattymispaiva),
     lisatty AS (
         INSERT INTO yki_arvioija_rekisterointikausi (arvioija_id, alkupaiva, paattymispaiva)
             SELECT arvioija_id, kauden_alkupaiva, kauden_paattymispaiva FROM kausi
             RETURNING id, arvioija_id, alkupaiva, paattymispaiva)
INSERT
INTO yki_arvioija_rekisterointikausi_oikeus (kausi_id, kieli, tasot)
SELECT lisatty.id,
       oikeus.kieli,
       (SELECT array_agg(taso ORDER BY taso) FROM unnest(oikeus.tasot) AS taso)
FROM lisatty
         JOIN oikeus
              ON oikeus.arvioija_id = lisatty.arvioija_id
                  AND oikeus.kauden_alkupaiva = lisatty.alkupaiva
                  AND oikeus.kauden_paattymispaiva IS NOT DISTINCT FROM lisatty.paattymispaiva;

-- Kasin passivoidun merkinnan viimeisin kausi merkitaan passivoiduksi, jottei alkupaivan
-- myohempi korjaus laske paattymispaivaa uudelleen ja elvyta merkintaa.
UPDATE yki_arvioija_rekisterointikausi kausi
SET passivoitu = arvioija.passivoitu
FROM yki_arvioija arvioija
WHERE kausi.arvioija_id = arvioija.id
  AND arvioija.passivoitu IS NOT NULL
  AND kausi.alkupaiva = (SELECT max(alkupaiva)
                         FROM yki_arvioija_rekisterointikausi uusin
                         WHERE uusin.arvioija_id = arvioija.id);

COMMENT ON TABLE yki_arvioija_rekisterointikausi IS 'Arvioijan rekisterointikaudet. Master: virkailija lisaa, muokkaa, passivoi ja poistaa naita. Kausi on arvioijakohtainen, joten kaikilla kielilla on sama kausi.';
COMMENT ON COLUMN yki_arvioija_rekisterointikausi.paattymispaiva IS 'Yleensa alkupaiva + 5 vuotta. Passivointi katkaisee sen, ja NULL esiintyy vain historiadatassa jolta paiva puuttuu.';
COMMENT ON COLUMN yki_arvioija_rekisterointikausi.passivoitu IS 'Asetettu jos kausi katkaistiin kesken kauden. Erottaa katkaistun kauden luonnollisesti paattyneesta, jotta alkupaivan muokkaus ei laske paattymispaivaa uudelleen.';
COMMENT ON TABLE yki_arvioija_rekisterointikausi_oikeus IS 'Yhteen rekisterointikauteen kuuluvat arviointioikeudet kielittain.';
COMMENT ON COLUMN yki_arvioija.arvioijan_ensimmainen_rekisterointipaiva IS 'Arvioijan ensimmainen rekisterointipaiva. Nimetty erikseen yki_arviointioikeus-sarakkeesta, koska yki_arvioija.* ja liitos samannimiseen sarakkeeseen osuisivat paallekkain. Siirretty riveilta, koska tieto on arvioijakohtainen ja voi olla vanhempi kuin yksikaan tallennettu kausi.';
COMMENT ON TABLE yki_arviointioikeus IS 'DERIVED. Projektio yki_arvioija_rekisterointikausi-taulusta: arvioijan voimassa oleva kausi kielittain. Kirjoitetaan uusiksi kausimuutoksen jalkeen. Legacy-kielten rivit ovat jaadytettya tuontidataa eivatka kuulu projektioon.';
