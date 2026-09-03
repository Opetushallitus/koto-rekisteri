-- ASHA-numero yksiloi hallintopaatoksen, ja jokainen arviointikausi on oma paatoksensa. Numero
-- kuuluu siis kaudelle, ei arvioijalle: arvioijatasolla jatkokausi olisi ylikirjoittanut edellisen
-- kauden paatosviitteen.
ALTER TABLE yki_arvioija_arviointikausi
    ADD COLUMN asha_numero TEXT;

-- Projektioon, jotta listanakyman haku, lajittelu ja CSV-vienti jatkavat ennallaan.
ALTER TABLE yki_arviointioikeus
    ADD COLUMN asha_numero TEXT;

-- Muutoslokiin, jotta paatosviitteen korjaus nakyy historiassa.
ALTER TABLE yki_arvioija_kausi
    ADD COLUMN asha_numero TEXT;

-- Nykyinen arvo kuuluu viimeisimmalle kaudelle: se on viimeisin hallintopaatos. Numeroa ei
-- monisteta vanhemmille kausille, koska ne ovat eri paatoksia joiden viitetta ei tiedeta.
UPDATE yki_arvioija_arviointikausi kausi
SET asha_numero = arvioija.asha_numero
FROM yki_arvioija arvioija
WHERE kausi.arvioija_id = arvioija.id
  AND arvioija.asha_numero IS NOT NULL
  AND kausi.alkupaiva = (SELECT max(uusin.alkupaiva)
                         FROM yki_arvioija_arviointikausi uusin
                         WHERE uusin.arvioija_id = arvioija.id);

UPDATE yki_arviointioikeus oikeus
SET asha_numero = arvioija.asha_numero
FROM yki_arvioija arvioija
WHERE oikeus.arvioija_id = arvioija.id
  AND arvioija.asha_numero IS NOT NULL;

ALTER TABLE yki_arvioija
    DROP COLUMN asha_numero;

COMMENT ON COLUMN yki_arvioija_arviointikausi.asha_numero IS 'Kautta koskevan hallintopaatoksen ASHA-numero, esittelijan kirjaama.';
COMMENT ON COLUMN yki_arviointioikeus.asha_numero IS 'DERIVED. Projisoitavan kauden ASHA-numero.';
COMMENT ON COLUMN yki_arvioija_kausi.asha_numero IS 'Lokirivin kirjaushetken ASHA-numero.';
