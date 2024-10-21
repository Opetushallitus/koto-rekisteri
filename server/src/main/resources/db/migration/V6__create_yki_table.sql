CREATE TYPE yki_tutkintokieli AS ENUM ('DEU','ENG','FIN','FRA', 'ITA', 'RUS','SME','SPA','SWE');
CREATE TYPE yki_tutkintotaso AS ENUM ('PT', 'KT', 'YT'); -- ("PT"=perustaso, "KT"=keskitaso, "YT"=ylin taso)

CREATE TABLE yki_suoritus
(
    id                       SERIAL PRIMARY KEY,
    suorittajan_oppijanumero TEXT                NOT NULL,
    sukunimi                 TEXT                NOT NULL,
    etunimet                 TEXT                NOT NULL,
    tutkintopaiva            DATE                NOT NULL,
    tutkintokieli            YKI_TUTKINTOKIELI   NOT NULL,
    tutkintotaso             YKI_TUTKINTOTASO    NOT NULL,
    jarjestajan_tunnus_oid   TEXT                NOT NULL,
    jarjestajan_nimi         TEXT,
    tekstin_ymmartaminen     FLOAT,
    kirjoittaminen           FLOAT,
    rakenteet_ja_sanasto     FLOAT,
    puheen_ymmartaminen      FLOAT,
    puhuminen                FLOAT,
    yleisarvosana            FLOAT,

    CONSTRAINT unique_suoritus UNIQUE
    (
        suorittajan_oppijanumero,
        tutkintopaiva,
        tutkintokieli,
        tutkintotaso
    )
)
