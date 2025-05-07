CREATE TYPE vkt_tutkintokieli AS ENUM ('FIN', 'SWE');
CREATE TYPE vkt_taitotaso AS ENUM ('Erinomainen', 'HyväJaTyydyttävä');
CREATE TYPE vkt_arvosana AS ENUM ('Erinomainen', 'Hyvä', 'Tyydyttävä', 'Hylätty');
CREATE TYPE vkt_osakokeen_tyyppi AS ENUM (
    'Puhuminen',
    'Kirjoittaminen',
    'PuheenYmmärtäminen',
    'TekstinYmmärtäminen');
CREATE TYPE vkt_tutkinnon_tyyppi AS ENUM (
    'SuullinenTaito',
    'KirjallinenTaito',
    'YmmärtämisenTaito');

CREATE TABLE vkt_suoritus
(
    id                                  SERIAL PRIMARY KEY,
    ilmoittautumisen_id                 INTEGER             NOT NULL,
    suorittajan_oppijanumero            henkilo_oid         NOT NULL,
    etunimi                             TEXT                NOT NULL,
    sukunimi                            TEXT                NOT NULL,
    tutkintokieli                       vkt_tutkintokieli   NOT NULL,
    ilmoittautumisen_tila               TEXT                NOT NULL,
    suorituskaupunki                    TEXT                NOT NULL,
    taitotaso                           vkt_taitotaso       NOT NULL,
    suorituksen_vastaanottaja           TEXT
);

CREATE TABLE vkt_osakoe
(
    id              SERIAL PRIMARY KEY,
    suoritus_id     INT                     REFERENCES vkt_suoritus(id),
    tyyppi          vkt_osakokeen_tyyppi    NOT NULL,
    tutkintopaiva   DATE                    NOT NULL,
    arviointipaiva  DATE,
    arvosana        vkt_arvosana
);

CREATE TABLE vkt_tutkinto
(
    id              SERIAL PRIMARY KEY,
    suoritus_id     INT                     REFERENCES vkt_suoritus(id),
    tyyppi          vkt_tutkinnon_tyyppi    NOT NULL,
    tutkintopaiva   DATE                    NOT NULL,
    arviointipaiva  DATE,
    arvosana        vkt_arvosana
)
