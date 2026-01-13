ALTER TABLE koto_suoritus
    ALTER COLUMN time_completed TYPE timestamptz USING time_completed AT TIME ZONE 'UTC';
