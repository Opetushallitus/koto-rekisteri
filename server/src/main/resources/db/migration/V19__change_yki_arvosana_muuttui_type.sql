ALTER TABLE yki_suoritus
    ALTER COLUMN arvosana_muuttui SET DATA TYPE INTEGER USING arvosana_muuttui::integer;
