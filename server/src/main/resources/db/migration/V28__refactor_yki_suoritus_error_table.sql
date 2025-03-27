TRUNCATE TABLE yki_suoritus_error;
DROP TABLE yki_suoritus_error;

CREATE TABLE yki_suoritus_error
(
    id                  SERIAL PRIMARY KEY,
    suorittajan_oid     TEXT,
    hetu                TEXT,
    nimi                TEXT,
    last_modified       TIMESTAMP WITH TIME ZONE,
    virheellinen_kentta TEXT,
    virheellinen_arvo   TEXT,
    virheellinen_rivi   TEXT                        NOT NULL,
    virheen_rivinumero  INTEGER                     NOT NULL,
    virheen_luontiaika  TIMESTAMP WITH TIME ZONE    NOT NULL,

    CONSTRAINT unique_suoritus_error UNIQUE
    (
        virheellinen_rivi,
        virheen_rivinumero
    )
)
