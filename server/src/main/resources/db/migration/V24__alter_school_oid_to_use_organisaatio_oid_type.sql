ALTER TABLE koto_suoritus
    ALTER COLUMN school_oid SET DATA TYPE organisaatio_oid USING
        CASE
            -- Use a placeholder value instead of an empty value
            WHEN school_oid = '' THEN '1.2.246.562.10.1234567890'::organisaatio_oid
            -- In common cases, just cast to the new type
            ELSE school_oid::organisaatio_oid
        END;
