ALTER TABLE yki_suoritus
    ADD COLUMN suoritus_id INTEGER NOT NULL,
    ADD COLUMN last_modified TIMESTAMP WITH TIME ZONE NOT NULL,
    ADD COLUMN tarkistusarvioinnin_saapumis_pvm DATE,
    ADD COLUMN tarkistusarvioinnin_asiatunnus TEXT,
    ADD COLUMN tarkistusarvioidut_osakokeet INTEGER,
    ADD COLUMN arvosana_muuttui BOOLEAN,
    ADD COLUMN perustelu TEXT,
    ADD COLUMN tarkistusarvioinnin_kasittely_pvm DATE,
    DROP CONSTRAINT unique_suoritus,
    ADD CONSTRAINT unique_suoritus UNIQUE
    (
        suoritus_id,
        last_modified
    );
