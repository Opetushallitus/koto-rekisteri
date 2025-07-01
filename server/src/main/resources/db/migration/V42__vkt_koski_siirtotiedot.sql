ALTER TABLE "vkt_suoritus"
    ADD COLUMN "koski_opiskeluoikeus" opiskeluoikeus_oid,
    ADD COLUMN "koski_siirto_kasitelty" boolean NOT NULL DEFAULT 'false';
