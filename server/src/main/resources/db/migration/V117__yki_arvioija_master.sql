-- Kitu on YKI-arvioijarekisterin master. Arvioijatason lisakentat, muokkausjaljet
-- ja Solki-lahetyksen outbox.
--
-- HUOM. puhelinnumeroa EI lisata: tavoitetilan tietotaulukossa ei ole puhelinnumeroa
-- lainkaan, vaikka kayttotapauskuvaus pyytaa sen lomakkeelle.
ALTER TABLE yki_arvioija
    ADD COLUMN luotu                         TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN luoja_oid                     henkilo_oid,
    ADD COLUMN muokattu                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN muokkaaja_oid                 henkilo_oid,
    ADD COLUMN solkiin_lahetetty             TIMESTAMPTZ,
    ADD COLUMN solki_lahetysvirhe            TEXT,
    ADD COLUMN solki_lahetysyritykset        INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN solki_viimeisin_lahetysyritys TIMESTAMPTZ,
    -- Hallintopaatoksen ASHA-numero: vapaa tekstikentta, ei muotovalidointia
    ADD COLUMN asha_numero                   TEXT,
    -- Sailytysaika lasketaan passivointihetkesta, joten se on tallennettava
    ADD COLUMN passivoitu                    TIMESTAMPTZ,
    -- Merkinta voi syntya ennen kuin ONR on yksiloinyt henkilon
    ADD COLUMN yksilointi_kesken             BOOLEAN     NOT NULL DEFAULT FALSE;

-- tila on Kotlinissa non-null mutta kannassa nullable
UPDATE yki_arviointioikeus SET tila = 'AKTIIVINEN' WHERE tila IS NULL;
ALTER TABLE yki_arviointioikeus
    ALTER COLUMN tila SET DEFAULT 'AKTIIVINEN',
    ALTER COLUMN tila SET NOT NULL;

-- KRIITTINEN: ennen kituun siirtoa data on perasin Solkista, joten sita ei tyonneta takaisin.
-- Ilman tata ensimmainen yoajo lahettaisi koko historiallisen rekisterin Solkiin.
UPDATE yki_arvioija a
SET luotu             = COALESCE(t.viimeisin, now()),
    muokattu          = COALESCE(t.viimeisin, now()),
    solkiin_lahetetty = COALESCE(t.viimeisin, now())
FROM (SELECT arvioija_id, max(rekisteriintuontiaika) AS viimeisin
      FROM yki_arviointioikeus GROUP BY arvioija_id) t
WHERE a.id = t.arvioija_id;

UPDATE yki_arvioija
SET solkiin_lahetetty = COALESCE(solkiin_lahetetty, now());

-- Sailytysaika lasketaan passivointihetkesta. Jo passivoiduille vanhoille riveille hetki
-- taytetaan kauden paattymispaivasta, jotta niiden sailytysaika kuluu historiallisella
-- aikajanalla eika ala vasta kayttoonotosta.
UPDATE yki_arvioija a
SET passivoitu = t.paattyi
FROM (
    SELECT arvioija_id,
           max(kauden_paattymispaiva)::timestamptz AS paattyi
    FROM yki_arviointioikeus
    GROUP BY arvioija_id
    HAVING bool_and(tila = 'PASSIVOITU')
       AND max(kauden_paattymispaiva) IS NOT NULL
) t
WHERE a.id = t.arvioija_id;

CREATE INDEX yki_arvioija_solki_lahettamattomat_idx
    ON yki_arvioija (muokattu)
    WHERE solkiin_lahetetty IS NULL OR solkiin_lahetetty < muokattu;

CREATE INDEX yki_arviointioikeus_passivointi_idx
    ON yki_arviointioikeus (kauden_paattymispaiva)
    WHERE tila = 'AKTIIVINEN';

COMMENT ON COLUMN yki_arvioija.luotu IS 'Milloin rekisterimerkinta luotiin kituun';
COMMENT ON COLUMN yki_arvioija.luoja_oid IS 'Merkinnan luoneen virkailijan OID; NULL jos jarjestelman tekema';
COMMENT ON COLUMN yki_arvioija.muokattu IS 'Milloin merkintaa viimeksi muutettiin; ohjaa Solki-lahetysta';
COMMENT ON COLUMN yki_arvioija.muokkaaja_oid IS 'Viimeksi muokanneen virkailijan OID; NULL jos jarjestelman tekema';
COMMENT ON COLUMN yki_arvioija.asha_numero IS 'Rekisterimerkintaa koskevan hallintopaatoksen ASHA-numero, esittelijan kirjaama';
COMMENT ON COLUMN yki_arvioija.passivoitu IS 'Hetki jolloin merkinta passivoitiin; sailytysajan (5 v) laskennan alkupiste';
COMMENT ON COLUMN yki_arvioija.yksilointi_kesken IS 'true = arvioija_oid on ONR:n henkilo-OID eika viela oppijanumero; taydennetaan ajastetusti';
COMMENT ON COLUMN yki_arvioija.solkiin_lahetetty IS 'Milloin rivi on viimeksi onnistuneesti lahetetty Solkiin; NULL tai < muokattu = lahetys kesken';
COMMENT ON COLUMN yki_arvioija.solki_lahetysvirhe IS 'Viimeisimman epaonnistuneen Solki-lahetyksen virheteksti';
COMMENT ON COLUMN yki_arvioija.solki_lahetysyritykset IS 'Perakkaisten epaonnistuneiden lahetysyritysten maara; nollataan onnistuneessa lahetyksessa';
COMMENT ON COLUMN yki_arvioija.solki_viimeisin_lahetysyritys IS 'Milloin Solki-lahetysta viimeksi yritettiin, onnistui tai ei';
