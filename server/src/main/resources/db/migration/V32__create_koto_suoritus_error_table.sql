CREATE TABLE koto_suoritus_error
(
    id                  SERIAL PRIMARY KEY,
    suorittajan_oid     TEXT,
    hetu                TEXT,
    nimi                TEXT                     NOT NULL,
    viesti              TEXT                     NOT NULL,
    virheen_luontiaika  TIMESTAMP WITH TIME ZONE NOT NULL,
    virheellinen_kentta TEXT,
    virheellinen_arvo   TEXT
)
