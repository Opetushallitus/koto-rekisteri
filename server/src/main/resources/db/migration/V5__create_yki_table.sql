CREATE DOMAIN DATE_ISO649_ALPHA3 AS CHAR(3) CHECK (VALUE ~ '^[A-Z]{3}$');
CREATE TYPE yki_tutkintotaso AS ENUM ('PT', 'KT', 'YT'); -- ("PT"=perustaso, "KT"=keskitaso, "YT"=ylin taso)

CREATE TABLE yki_suoritus
(
    id                      SERIAL PRIMARY KEY,
    suorittajanOppijanumero TEXT                NOT NULL,
    sukunimi                TEXT                NOT NULL,
    etunimet                TEXT                NOT NULL,
    tutkintopaiva           DATE                NOT NULL,
    tutkintokieli           DATE_ISO649_ALPHA3  NOT NULL,
    tutkintotaso            YKI_TUTKINTOTASO    NOT NULL,
    jarjestajanTunnusOid    TEXT                NOT NULL,
    jarjestajanNimi         TEXT,
    tekstinYmmartaminen     FLOAT,
    kirjoittaminen          FLOAT,
    rakenteetJaSanasto      FLOAT,
    puheenYmmartaminen      FLOAT,
    puhuminen               FLOAT,
    yleisarvosana           FLOAT,

    CONSTRAINT unique_suoritus UNIQUE
    (
        suorittajanOppijanumero,
        tutkintopaiva,
        tutkintokieli,
        tutkintotaso
    )
)