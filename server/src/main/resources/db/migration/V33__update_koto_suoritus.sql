ALTER TABLE koto_suoritus
    ADD COLUMN teacher_email               TEXT,
    ADD COLUMN luetun_ymmartaminen_result  TEXT,
    ADD COLUMN kuullun_ymmartaminen_result TEXT,
    ADD COLUMN puhe_result                 TEXT,
    ADD COLUMN kirjoittaminen_result       TEXT,
    DROP COLUMN luetun_ymmartaminen_result_system,
    DROP COLUMN kuullun_ymmartaminen_result_system,
    DROP COLUMN puhe_result_system,
    DROP COLUMN kirjoittaminen_result_system,
    DROP COLUMN total_evaluation_system,
    DROP COLUMN total_evaluation_teacher;
