ALTER TYPE yki_arviointitila ADD VALUE 'TARKISTUSARVIOITU';
ALTER TYPE yki_arviointitila ADD VALUE 'TARKISTUSARVIOINTI_HYVAKSYTTY';

CREATE TABLE "yki_suoritus_lisatieto"
(
    "suoritus_id"                       integer NOT NULL,
    "tarkistusarviointi_hyvaksytty_pvm" date    NOT NULL,
    PRIMARY KEY ("suoritus_id")
);
