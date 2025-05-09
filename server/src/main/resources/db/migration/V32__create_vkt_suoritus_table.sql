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
    ilmoittautumisen_id                 TEXT                NOT NULL,
    suorittajan_oppijanumero            henkilo_oid         NOT NULL,
    etunimi                             TEXT                NOT NULL,
    sukunimi                            TEXT                NOT NULL,
    tutkintokieli                       vkt_tutkintokieli   NOT NULL,
    ilmoittautumisen_tila               TEXT                NOT NULL,
    suorituskaupunki                    TEXT                NOT NULL,
    taitotaso                           vkt_taitotaso       NOT NULL,
    suorituksen_vastaanottaja           TEXT,
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE vkt_osakoe
(
    id              SERIAL PRIMARY KEY,
    suoritus_id     INT                     REFERENCES vkt_suoritus(id),
    tyyppi          vkt_osakokeen_tyyppi    NOT NULL,
    tutkintopaiva   DATE                    NOT NULL,
    arviointipaiva  DATE,
    arvosana        vkt_arvosana,
    created_at                              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE vkt_tutkinto
(
    id              SERIAL PRIMARY KEY,
    suoritus_id     INT                     REFERENCES vkt_suoritus(id),
    tyyppi          vkt_tutkinnon_tyyppi    NOT NULL,
    arviointipaiva  DATE,
    arvosana        vkt_arvosana,
    created_at                              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE FUNCTION add_created_at() RETURNS trigger AS $add_rekisteriintuontiaika$
    BEGIN
        NEW.created_at := NOW();
        RETURN NEW;
    END;
$add_rekisteriintuontiaika$ LANGUAGE plpgsql;

CREATE TRIGGER add_rekisteriintuontiaika BEFORE INSERT ON vkt_suoritus
    FOR EACH ROW EXECUTE FUNCTION add_created_at();
