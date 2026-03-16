CREATE TYPE koto_testikieli AS ENUM ('FIN', 'SWE');

ALTER TABLE koto_suoritus
    ADD COLUMN testikieli koto_testikieli;
