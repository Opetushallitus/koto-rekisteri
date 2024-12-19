ALTER TABLE koto_suoritus
    ALTER COLUMN luetun_ymmartaminen_result_system SET DATA TYPE TEXT USING luetun_ymmartaminen_result_system::TEXT,
    ALTER COLUMN luetun_ymmartaminen_result_teacher SET DATA TYPE TEXT USING luetun_ymmartaminen_result_teacher::TEXT,
    ALTER COLUMN kuullun_ymmartaminen_result_system SET DATA TYPE TEXT USING kuullun_ymmartaminen_result_system::TEXT,
    ALTER COLUMN kuullun_ymmartaminen_result_teacher SET DATA TYPE TEXT USING kuullun_ymmartaminen_result_teacher::TEXT,
    ALTER COLUMN puhe_result_system SET DATA TYPE TEXT USING puhe_result_system::TEXT,
    ALTER COLUMN puhe_result_teacher SET DATA TYPE TEXT USING puhe_result_teacher::TEXT,
    ALTER COLUMN kirjoittaminen_result_system SET DATA TYPE TEXT USING kirjoittaminen_result_system::TEXT,
    ALTER COLUMN kirjottaminen_result_teacher SET DATA TYPE TEXT USING kirjottaminen_result_teacher::TEXT;

ALTER TABLE koto_suoritus
    RENAME COLUMN first_name TO first_names;

ALTER TABLE koto_suoritus
    ADD COLUMN school_oid TEXT;
