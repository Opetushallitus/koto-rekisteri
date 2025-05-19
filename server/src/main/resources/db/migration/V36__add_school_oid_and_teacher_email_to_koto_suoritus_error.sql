ALTER TABLE koto_suoritus_error
    ADD COLUMN school_oid organisaatio_oid,
    ADD COLUMN teacher_email TEXT;
