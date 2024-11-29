ALTER TABLE yki_arvioija
    -- Default value is only added to satify SQL syntax.
    -- We will truncate the table any ways so the default value do not matter.
    ADD COLUMN ensimmainen_rekisterointipaiva DATE NOT NULL default '1970-01-01',
    ADD COLUMN kauden_alkupaiva DATE NULL,
    ADD COLUMN kauden_paattymispaiva DATE NULL,
    ADD COLUMN jatkorekisterointi BOOLEAN;

-- Delete the data.
-- This requires reimport, so we can get data for kaydenAlkupaiva and jatkorekisterointi.
TRUNCATE yki_arvioija

