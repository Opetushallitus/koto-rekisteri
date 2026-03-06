ALTER TABLE koto_suoritus
    DROP CONSTRAINT unique_koto_suoritus;

ALTER TABLE koto_suoritus
    ADD COLUMN last_modified TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD CONSTRAINT unique_koto_suoritus UNIQUE (
            kurssi_id,
            oppijanumero,
            suoritusaika,
            last_modified
        );

