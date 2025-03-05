ALTER TABLE koto_suoritus
    ALTER COLUMN oppijanumero SET DATA TYPE henkilo_oid USING oppijanumero::henkilo_oid;
