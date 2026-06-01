-- Erillinen aikaleima jolla seurataan koska kitu vastaanotti rivin ulkoiselta
-- järjestelmältä. Tätä ei muuteta sisäisten versiokirjoitusten (esim.
-- tarkistusarvioinnin hyväksyminen) yhteydessä, joten dashboardin
-- "Viimeisin saapunut suoritus" -aikaleima ei hyppää virkailijan toiminnasta.
ALTER TABLE yki_suoritus ADD COLUMN received_at TIMESTAMPTZ;

-- Olemassa oleville riveille last_modified on paras arvio vastaanottoajasta.
UPDATE yki_suoritus SET received_at = last_modified;

ALTER TABLE yki_suoritus ALTER COLUMN received_at SET NOT NULL;

-- Tukee dashboardin MAX(received_at) -kyselyä.
CREATE INDEX yki_suoritus_received_at_idx ON yki_suoritus (received_at DESC);
