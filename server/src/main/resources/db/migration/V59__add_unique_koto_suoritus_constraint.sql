ALTER TABLE koto_suoritus
    ADD CONSTRAINT unique_koto_suoritus UNIQUE (
        courseid,
        oppijanumero,
        time_completed
    );
