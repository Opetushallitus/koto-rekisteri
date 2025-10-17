-- Poista devaajan kannassa mahdollisesti oleva virheellinen rivi.
-- Tämä rivi on poistettu kannan alustuksesta commitissa b4c04c71bf149c04887edab962e4ed3dce91d43f
DELETE FROM "yki_arvioija" WHERE arvioijan_oppijanumero = 'DELETED';

ALTER TABLE "yki_arvioija" ALTER COLUMN "arvioijan_oppijanumero" TYPE henkilo_oid;
