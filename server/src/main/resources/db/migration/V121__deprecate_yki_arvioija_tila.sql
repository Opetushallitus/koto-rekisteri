-- Kielikohtainen tila lasketaan nyt rekisterointikauden paivamaarista, ks. Rekisterointitila.
-- Kitu ei enaa kirjoita saraketta lainkaan; ainoa ei-tyhjan arvon kirjoittaja on Solkin
-- sisaantuleva push. Laskenta kunnioittaa vain tallennettua PASSIVOITUa: V117 taytti kaikki
-- tyhjat rivit arvolla 'AKTIIVINEN', joten se ei kanna tietoa jota paivamaarat eivat jo kerro.
-- Vanhoja rivaeja ei paiviteta, vaan arvo vanhenee seuraavassa tallennuksessa.
ALTER TABLE yki_arviointioikeus
    ALTER COLUMN tila DROP NOT NULL,
    ALTER COLUMN tila DROP DEFAULT;

ALTER TABLE yki_arvioija_kausi
    ALTER COLUMN tila DROP NOT NULL;

-- Osittainen indeksi kattoi vain tila = 'AKTIIVINEN' -rivit, joita kitu ei enaa kirjoita.
DROP INDEX IF EXISTS yki_arviointioikeus_passivointi_idx;

CREATE INDEX yki_arviointioikeus_kauden_paattyminen_idx
    ON yki_arviointioikeus (kauden_paattymispaiva);

COMMENT ON COLUMN yki_arviointioikeus.tila IS
    'DEPRECATED. Vain Solkin pushin kirjaama tila; NULL tarkoittaa etta tila lasketaan kauden paivamaarista (Rekisterointitila). Vain PASSIVOITU ohittaa laskennan.';
COMMENT ON COLUMN yki_arvioija_kausi.tila IS
    'DEPRECATED. Historiarivin tila lasketaan sen omista paivamaarista, ellei Solki ole kirjannut arvoa.';
