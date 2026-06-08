ALTER TABLE koto_suoritus
    ALTER COLUMN oppijanumero DROP NOT NULL,
    ALTER COLUMN suoritusaika DROP NOT NULL,
    ADD COLUMN completed BOOLEAN NOT NULL DEFAULT true;