CREATE TYPE vkt_tutkintokieli AS ENUM ('FIN', 'SWE');
CREATE TYPE vkt_taitotaso AS ENUM ('Erinomainen', 'HyväTyydyttävä');
CREATE TYPE vkt_arvosana AS ENUM ('Erinomainen', 'Hyvä', 'Tyydyttävä', 'Hylätty');

CREATE TABLE vkt_suoritus
(
    id                                  SERIAL PRIMARY KEY,
    ilmoittautumisen_id                 INTEGER             NOT NULL,
    suorittajan_oppijanumero            henkilo_oid         NOT NULL,
    etunimi                             TEXT                NOT NULL,
    sukunimi                            TEXT                NOT NULL,
    tutkintokieli                       vkt_tutkintokieli   NOT NULL,
    tutkintopaiva                       DATE                NOT NULL,
    ilmoittautumisen_tila               TEXT                NOT NULL,
    ilmoittautunut_puhuminen            BOOLEAN             NOT NULL,
    ilmoittautunut_puheen_ymmartaminen  BOOLEAN             NOT NULL,
    ilmoittautunut_kirjoittaminen       BOOLEAN             NOT NULL,
    ilmoittautunut_tekstin_ymmartaminen BOOLEAN             NOT NULL,
    suorituskaupunki                    TEXT                NOT NULL,
    taitotaso                           vkt_taitotaso       NOT NULL,
    suorituksen_vastaanottaja           TEXT,
    puhuminen                           vkt_arvosana,
    puheen_ymmartaminen                 vkt_arvosana,
    kirjoittaminen                      vkt_arvosana,
    tekstin_ymmartaminen                vkt_arvosana,
    suullinen_taito                     vkt_arvosana,
    kirjallinen_taito                   vkt_arvosana,
    ymmartamisen_taito                  vkt_arvosana
)
