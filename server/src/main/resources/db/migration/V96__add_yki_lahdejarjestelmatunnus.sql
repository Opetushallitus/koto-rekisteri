ALTER TABLE "yki_suoritus"
    ADD COLUMN "lahdejarjestelmantunnus" text;

UPDATE yki_suoritus
SET lahdejarjestelmantunnus = 'yki.' || solki_id::text;

ALTER TABLE "yki_suoritus"
    ALTER COLUMN "lahdejarjestelmantunnus" SET NOT NULL;

CREATE INDEX "yki_suoritus_lahdejarjestelmantunnus_idx" ON "yki_suoritus" ("lahdejarjestelmantunnus");
