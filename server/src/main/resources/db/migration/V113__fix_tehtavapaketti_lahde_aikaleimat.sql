-- Moodlen epoch-sekunteina palauttamat aikaleimat tulkittiin millisekunneiksi,
-- jolloin ne tallentuivat vuodelle 1970. Kerrotaan virheellisten arvojen epoch
-- tuhannella; aidot arvot ovat 2020-lukua.
UPDATE tehtavapaketti
SET lahde_filegenerated = to_timestamp(extract(epoch FROM lahde_filegenerated) * 1000)
WHERE lahde_filegenerated < timestamptz '1980-01-01T00:00:00Z';

UPDATE tehtavapaketti
SET lahde_published = to_timestamp(extract(epoch FROM lahde_published) * 1000)
WHERE lahde_published < timestamptz '1980-01-01T00:00:00Z';
