ALTER TABLE yki_suoritus_error
    ADD COLUMN key_values TEXT NOT NULL default '';

ALTER TABLE yki_suoritus_error
    ADD COLUMN source_type TEXT NOT NULL default '';