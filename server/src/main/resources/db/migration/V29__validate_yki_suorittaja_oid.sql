ALTER TABLE yki_suoritus
    ALTER COLUMN suorittajan_oid SET DATA TYPE henkilo_oid USING suorittajan_oid::henkilo_oid;
