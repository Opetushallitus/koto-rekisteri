CREATE TYPE yki_todistuskieli AS ENUM ('ENG','FIN','SWE');

ALTER TABLE yki_suoritus
    ADD COLUMN todistuskieli yki_todistuskieli;
