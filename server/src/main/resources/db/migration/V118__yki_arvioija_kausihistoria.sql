-- Kausi ja tila pysyvat yki_arviointioikeus-rivilla (= voimassa oleva kausi).
-- Sen rinnalle tulee append-only-historiataulu, johon kirjataan jokainen kausi.
CREATE TABLE yki_arvioija_kausi
(
    id                    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    arvioija_id           INTEGER           NOT NULL REFERENCES yki_arvioija (id) ON DELETE CASCADE ON UPDATE CASCADE,
    kieli                 yki_tutkintokieli NOT NULL,
    tasot                 TEXT[]            NOT NULL,
    tila                  yki_arvioija_tila NOT NULL,
    kauden_alkupaiva      DATE,
    kauden_paattymispaiva DATE,
    jatkorekisterointi    BOOLEAN           NOT NULL DEFAULT FALSE,
    kirjattu              TIMESTAMPTZ       NOT NULL DEFAULT now(),
    kirjaaja_oid          henkilo_oid,

    CONSTRAINT yki_arvioija_kausi_unique UNIQUE NULLS NOT DISTINCT
        (arvioija_id, kieli, kauden_alkupaiva, kauden_paattymispaiva, tila, jatkorekisterointi)
);

CREATE INDEX yki_arvioija_kausi_arvioija_idx ON yki_arvioija_kausi (arvioija_id, kirjattu DESC);

-- Nykytila historian ensimmaiseksi riviksi
INSERT INTO yki_arvioija_kausi
    (arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva, jatkorekisterointi, kirjattu)
SELECT arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva,
       jatkorekisterointi, rekisteriintuontiaika
FROM yki_arviointioikeus
ON CONFLICT ON CONSTRAINT yki_arvioija_kausi_unique DO NOTHING;

COMMENT ON TABLE yki_arvioija_kausi IS 'Arvioijarekisterimerkintojen kausihistoria: yksi rivi jokaisesta kirjatusta rekisterointikaudesta kielikohtaisesti. Voimassa oleva kausi elaa yki_arviointioikeus-taulussa.';
