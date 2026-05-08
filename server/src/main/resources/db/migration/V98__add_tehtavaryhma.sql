CREATE TABLE "tehtavaryhma" (
    "id"         SERIAL PRIMARY KEY,
    "paketti_id" INT NOT NULL REFERENCES "tehtavapaketti"("id") ON DELETE CASCADE,
    "nimi"       TEXT NOT NULL,
    "jarjestys"  INT NOT NULL,
    "metadata"   JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX "tehtavaryhma_paketti_jarjestys_idx"
    ON "tehtavaryhma" ("paketti_id", "jarjestys");

-- Synteettinen oletusryhmä jokaiselle olemassaolevalle paketille,
-- jotta NOT NULL FK voidaan lisätä rikkomatta dev/CI-tietokantoja
-- joissa voi jo olla tehtava-rivejä.
INSERT INTO "tehtavaryhma" ("paketti_id", "nimi", "jarjestys")
SELECT "id", '(jaottelematta)', 1 FROM "tehtavapaketti";

ALTER TABLE "tehtava"
    ADD COLUMN "ryhma_id" INT
        REFERENCES "tehtavaryhma"("id") ON DELETE CASCADE;

UPDATE "tehtava" t
SET "ryhma_id" = (
    SELECT r."id"
    FROM "tehtavaryhma" r
    WHERE r."paketti_id" = t."paketti_id"
    ORDER BY r."jarjestys"
    LIMIT 1
);

ALTER TABLE "tehtava"
    ALTER COLUMN "ryhma_id" SET NOT NULL,
    DROP COLUMN "kategoria";

CREATE INDEX "tehtava_ryhma_idx" ON "tehtava" ("ryhma_id");