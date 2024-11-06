CREATE TYPE yki_sukupuoli AS ENUM ('M', 'N', 'E'); -- ("M"=mies, "N"=nainen, "E"=ei ilmoitettu tai muu)

ALTER TABLE yki_suoritus
    ADD COLUMN hetu TEXT NOT NULL,
    ADD COLUMN sukupuoli YKI_SUKUPUOLI NOT NULL,
    ADD COLUMN kansalaisuus TEXT NOT NULL,
    ADD COLUMN katuosoite TEXT NOT NULL,
    ADD COLUMN postinumero TEXT NOT NULL,
    ADD COLUMN postitoimipaikka TEXT NOT NULL,
    ADD COLUMN email TEXT,
    ADD COLUMN arviointipaiva DATE NOT NULL;

