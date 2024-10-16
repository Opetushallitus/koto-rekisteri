CREATE TABLE yki_suoritus
(
    id                      SERIAL PRIMARY KEY,
    suorittajanOppijanumero TEXT    NOT NULL,
    sukunimi                TEXT    NOT NULL,
    etunimet                TEXT    NOT NULL,
    tutkintopaiva           DATE    NOT NULL,                                           -- ISO-8601-muodossa
    tutkintokieli           CHAR(3) NOT NULL CHECK(tutkintokieli ~ '^[A-Z]{3}$'),       -- ISO 649-2 alpha-3 -muodossa
    tutkintotaso            CHAR(2) NOT NULL CHECK (tutkintotaso IN ('PT', 'KT', 'YT'), -- ("PT"=perustaso, "KT"=keskitaso, "YT"=ylin taso)
    jarjestajanTunnusOid    TEXT    NOT NULL,
    jarjestajanNimi         TEXT,
    tekstinYmmartaminen     FLOAT,
    kirjoittaminen          FLOAT,
    rakenteetJaSanasto      FLOAT,
    puheenYmmartaminen      FLOAT,
    puhuminen               FLOAT,
    yleisarvosana           FLOAT,
)