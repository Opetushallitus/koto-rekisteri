CREATE TYPE koto_arvosana AS ENUM ('ALLEA1', 'A1', 'A2', 'B1', 'YLIB1', 'EVA');

ALTER TABLE koto_suoritus
    ALTER COLUMN luetun_ymmartaminen SET DATA TYPE koto_arvosana USING
    CASE
        WHEN luetun_ymmartaminen = 'Alle A1' THEN 'ALLEA1'::koto_arvosana
        WHEN luetun_ymmartaminen = 'Yli B1' THEN 'YLIB1'::koto_arvosana
        ELSE luetun_ymmartaminen::koto_arvosana
    END,
    ALTER COLUMN kuullun_ymmartaminen SET DATA TYPE koto_arvosana USING
    CASE
        WHEN kuullun_ymmartaminen = 'Alle A1' THEN 'ALLEA1'::koto_arvosana
        WHEN kuullun_ymmartaminen = 'Yli B1' THEN 'YLIB1'::koto_arvosana
        ELSE kuullun_ymmartaminen::koto_arvosana
    END,
    ALTER COLUMN puhe SET DATA TYPE koto_arvosana USING
    CASE
        WHEN puhe = 'Alle A1' THEN 'ALLEA1'::koto_arvosana
        WHEN puhe = 'Yli B1' THEN 'YLIB1'::koto_arvosana
        ELSE puhe::koto_arvosana
    END,
    ALTER COLUMN kirjoittaminen SET DATA TYPE koto_arvosana USING
    CASE
        WHEN kirjoittaminen = 'Alle A1' THEN 'ALLEA1'::koto_arvosana
        WHEN kirjoittaminen = 'Yli B1' THEN 'YLIB1'::koto_arvosana
        ELSE kirjoittaminen::koto_arvosana
    END;