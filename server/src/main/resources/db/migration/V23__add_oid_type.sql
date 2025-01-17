-- Domain for ITU and ISO/IEC standard OIDs
CREATE DOMAIN iso_oid AS TEXT
    CHECK (
        -- Require format with one or more non-empty numeric strings, separated by periods.
        -- No other characters allowed.
        regexp_like(value, '^(?:\d+\.)*\d+$')
    );

-- Domain aliases to differentiate between Organization and Person OIDs
CREATE DOMAIN organisaatio_oid AS iso_oid;
CREATE DOMAIN henkilo_oid AS iso_oid;
