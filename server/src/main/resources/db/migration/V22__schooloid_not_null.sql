
UPDATE koto_suoritus
    SET school_oid = ''
    WHERE school_oid IS NULL;

ALTER TABLE koto_suoritus
    ALTER COLUMN school_oid
    SET NOT NULL;