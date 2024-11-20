-- Data is to be "versioned", thus multiple rows per oppijanumero are allowed.
ALTER TABLE yki_arvioija
    DROP CONSTRAINT yki_arvioija_oppijanumero_is_unique;

ALTER TABLE yki_arvioija
    -- Add import timestamp to allow identifying the latest entry. Default to
    -- NOW() in order to make inserting more straightforward.
    ADD COLUMN rekisteriintuontiaika TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Add constraint for ON CONFLICT DO NOTHING when entries have not changed.
    -- This makes bulk inserting a breeze, without requiring e.g. triggers for
    -- detecting/skipping unnecessary inserts.
    ADD CONSTRAINT yki_arvioija_is_unique UNIQUE (
         arvioijan_oppijanumero,
         henkilotunnus,
         sukunimi,
         etunimet,
         sahkopostiosoite,
         katuosoite,
         postinumero,
         postitoimipaikka,
         tila,
         kieli,
         tasot
    );

-- Mark all existing rows without import timestamps to have been imported NOW().
-- Some timestamp is required on all rows for marking the row NOT NULL.
UPDATE yki_arvioija
    SET rekisteriintuontiaika = NOW()
WHERE
    yki_arvioija.rekisteriintuontiaika IS NULL;

-- Finally, set the timestamp as NOT NULL to make sure all future rows have it.
ALTER TABLE yki_arvioija
    ALTER COLUMN rekisteriintuontiaika SET NOT NULL;
