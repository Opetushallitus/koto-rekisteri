-- Taulun kirjoituspolku poistui Solkin CSV-tuonnin mukana (commitit 279bd81f,
-- e0ff5d3f, d160c1f1, ff82bc5a). Arvioijatiedot tulevat sisaan JSON-rajapinnan
-- kautta, eika mikaan kirjoita tahan tauluun enaa. Nakyma /yki/arvioijat/virheet
-- ja dashboardin laskuri ovat siten olleet pysyvasti nollia.
DROP TABLE IF EXISTS yki_arvioija_error;
