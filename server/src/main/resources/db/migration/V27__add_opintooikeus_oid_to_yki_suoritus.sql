CREATE DOMAIN opiskeluoikeus_oid AS iso_oid;

ALTER TABLE yki_suoritus
    ADD COLUMN koski_opiskeluoikeus opiskeluoikeus_oid;
