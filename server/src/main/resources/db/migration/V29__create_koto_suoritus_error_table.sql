CREATE TABLE koto_suoritus_error
(
    id                  SERIAL PRIMARY KEY,
    suorittajan_oid     TEXT,
    hetu                TEXT,
    nimi                TEXT,
    last_modified       TIMESTAMP WITH TIME ZONE,
    virheellinen_kentta TEXT,
    virheellinen_arvo   TEXT,
    virheellinen_rivi   TEXT                     NOT NULL,
    virheen_rivinumero  INTEGER                  NOT NULL,
    virheen_luontiaika  TIMESTAMP WITH TIME ZONE NOT NULL
)
