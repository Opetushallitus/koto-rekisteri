-- yki_arvioija_kausi lakkaa olemasta "kaikki kaudet" ja on jatkossa pelkka muutosloki: master on
-- yki_arvioija_rekisterointikausi. Rivi kertoo nyt mika toimenpide sen kirjasi.
ALTER TABLE yki_arvioija_kausi
    ADD COLUMN toimenpide TEXT,
    -- Ei viiteavainta: kauden kovapoisto ei saa viedä lokirivia mukanaan.
    ADD COLUMN kausi_id   INTEGER;

ALTER TABLE yki_arvioija_kausi
    ADD CONSTRAINT yki_arvioija_kausi_toimenpide CHECK
        (toimenpide IS NULL OR toimenpide IN ('LISAYS', 'MUOKKAUS', 'PASSIVOINTI', 'POISTO', 'TALLENNUS'));

-- Uniikkiehto oli olemassa vain koska loki kirjoitettiin jokaisella tallennuksella ja
-- muuttumaton kausi piti vaimentaa. Nyt rivi kirjataan vain kun kausi tosiasiassa muuttuu,
-- ja saman kauden uudelleenlisays poiston jalkeen on aito tapahtuma jonka on nayttava lokissa.
ALTER TABLE yki_arvioija_kausi
    DROP CONSTRAINT yki_arvioija_kausi_unique;

COMMENT ON TABLE yki_arvioija_kausi IS 'Append-only muutosloki kausiin kohdistuneista toimenpiteista. Master on yki_arvioija_rekisterointikausi; tama taulu naytetaan tietosivulla muutoshistoriana.';
COMMENT ON COLUMN yki_arvioija_kausi.toimenpide IS 'Toimenpide joka kirjasi rivin. NULL = ennen V123:a kirjattu rivi, jolloin toimenpidetta ei tiedeta.';
COMMENT ON COLUMN yki_arvioija_kausi.kausi_id IS 'Kohteena ollut yki_arvioija_rekisterointikausi.id. Ei viiteavainta, jotta poistetun kauden lokirivit sailyvat.';
