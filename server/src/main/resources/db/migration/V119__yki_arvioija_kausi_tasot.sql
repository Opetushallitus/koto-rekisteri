-- Tasojen muutos saman kauden sisalla on hallintopaatoksen tulos, joten sen on paadyttava
-- historiaan. Ilman tasot-saraketta avaimessa uusi taso katosi ON CONFLICT DO NOTHINGiin ja
-- historiarivi jai vanhaan kokoonpanoon.
--
-- Taulukon jarjestys on osa taulukon identiteettia, joten tasot normalisoidaan aakkosjarjestykseen
-- seka taalla etta kirjoitettaessa. Muuten {PT,KT} ja {KT,PT} olisivat eri kausia.
UPDATE yki_arvioija_kausi
SET tasot = (SELECT array_agg(taso ORDER BY taso) FROM unnest(tasot) AS taso)
WHERE cardinality(tasot) > 1;

UPDATE yki_arviointioikeus
SET tasot = (SELECT array_agg(taso ORDER BY taso) FROM unnest(tasot) AS taso)
WHERE cardinality(tasot) > 1;

-- Vanha avain on uuden osajoukko, joten uusi ehto ei voi rikkoutua olemassa olevalla datalla.
ALTER TABLE yki_arvioija_kausi
    DROP CONSTRAINT yki_arvioija_kausi_unique,
    ADD CONSTRAINT yki_arvioija_kausi_unique UNIQUE NULLS NOT DISTINCT
        (arvioija_id, kieli, tasot, kauden_alkupaiva, kauden_paattymispaiva, tila, jatkorekisterointi);
