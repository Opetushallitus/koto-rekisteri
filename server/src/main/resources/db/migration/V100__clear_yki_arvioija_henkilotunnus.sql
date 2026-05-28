-- 1.1.2026 voimaan tulleen lainmuutoksen myötä yki-arvioijan henkilötunnusta
-- ei enää saa säilyttää. Validointi ja upsert estävät uusien hetujen
-- tallennuksen, mutta ennen lainmuutosta tietokantaan tallennetut hetut
-- siivotaan tällä migraatiolla niiltä arvioijoilta, joiden arviointioikeus
-- alkaa lainmuutoksen voimaantulopäivänä tai sen jälkeen.
UPDATE yki_arvioija
SET henkilotunnus = NULL
WHERE henkilotunnus IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM yki_arviointioikeus
      WHERE yki_arviointioikeus.arvioija_id = yki_arvioija.id
        AND yki_arviointioikeus.kauden_alkupaiva >= DATE '2026-01-01'
  );