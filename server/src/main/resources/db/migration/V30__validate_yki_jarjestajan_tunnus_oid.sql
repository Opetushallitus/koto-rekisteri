ALTER TABLE yki_suoritus
    ALTER COLUMN jarjestajan_tunnus_oid SET DATA TYPE organisaatio_oid USING jarjestajan_tunnus_oid::organisaatio_oid;
