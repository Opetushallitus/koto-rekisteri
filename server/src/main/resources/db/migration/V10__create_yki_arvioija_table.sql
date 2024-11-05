CREATE TABLE yki_arvioija
(
    id                     SERIAL PRIMARY KEY,
    arvioijan_oppijanumero TEXT               NOT NULL,
    henkilotunnus          TEXT               NOT NULL,
    sukunimi               TEXT               NOT NULL,
    etunimet               TEXT               NOT NULL,
    sahkopostiosoite       TEXT               NOT NULL,
    katuosoite             TEXT               NOT NULL,
    postinumero            TEXT               NOT NULL,
    postitoimipaikka       TEXT               NOT NULL,
    tila                   INTEGER            NOT NULL,
    kieli                  YKI_TUTKINTOKIELI  NOT NULL,
    tasot                  TEXT[]             NOT NULL,

    CONSTRAINT all_tasot_are_valid_tutkintotaso CHECK (
        -- "tasot is contained by YKI_TUTKINTOTASO"
        tasot <@ enum_range(NULL::YKI_TUTKINTOTASO)::TEXT[]
    )
)
