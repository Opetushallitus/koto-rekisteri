CREATE TABLE koto_suoritus
(
    id                                  SERIAL PRIMARY KEY,
    first_name                          TEXT      NOT NULL,
    last_name                           TEXT      NOT NULL,
    oppija_oid                          TEXT      NOT NULL,
    email                               TEXT      NOT NULL,
    time_completed                      TIMESTAMP NOT NULL,
    luetun_ymmartaminen_result_system   FLOAT,
    luetun_ymmartaminen_result_teacher  FLOAT,
    kuullun_ymmartaminen_result_system  FLOAT,
    kuullun_ymmartaminen_result_teacher FLOAT,
    puhe_result_system                  FLOAT,
    puhe_result_teacher                 FLOAT,
    kirjoittaminen_result_system        FLOAT,
    kirjottaminen_result_teacher        FLOAT,
    total_evaluation_teacher            TEXT,
    total_evaluation_system             TEXT
);
