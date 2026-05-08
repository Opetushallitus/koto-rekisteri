CREATE TABLE "tehtavapaketti" (
    "id"                SERIAL PRIMARY KEY,
    "lahdejarjestelma"  TEXT NOT NULL,
    "lahde_id"          TEXT NOT NULL,
    "nimi"              TEXT NOT NULL,
    "versio_hash"       TEXT NOT NULL,
    "s3_avain"          TEXT,
    "metadata"          JSONB NOT NULL DEFAULT '{}'::jsonb,
    "luotu"             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE ("lahdejarjestelma", "lahde_id", "versio_hash")
);

CREATE INDEX "tehtavapaketti_lahde_idx"
    ON "tehtavapaketti" ("lahdejarjestelma", "lahde_id");

CREATE TABLE "tehtava" (
    "id"                SERIAL PRIMARY KEY,
    "paketti_id"        INT NOT NULL REFERENCES "tehtavapaketti"("id") ON DELETE CASCADE,
    "tyyppi"            TEXT NOT NULL,
    "lahde_id"          TEXT,
    "kategoria"         TEXT,
    "nimi"              TEXT,
    "teksti"            TEXT,
    "tekstin_formaatti" TEXT,
    "jarjestys"         INT NOT NULL,
    "metadata"          JSONB NOT NULL DEFAULT '{}'::jsonb,
    "luotu"             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX "tehtava_paketti_jarjestys_idx" ON "tehtava" ("paketti_id", "jarjestys");
CREATE INDEX "tehtava_tyyppi_idx"            ON "tehtava" ("tyyppi");

CREATE TABLE "tehtava_vastaus" (
    "id"                SERIAL PRIMARY KEY,
    "tehtava_id"        INT NOT NULL REFERENCES "tehtava"("id") ON DELETE CASCADE,
    "jarjestys"         INT NOT NULL,
    "teksti"            TEXT,
    "tekstin_formaatti" TEXT,
    "metadata"          JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX "tehtava_vastaus_tehtava_jarjestys_idx"
    ON "tehtava_vastaus" ("tehtava_id", "jarjestys");

CREATE TABLE "tehtava_tiedosto" (
    "id"            SERIAL PRIMARY KEY,
    "tehtava_id"    INT NOT NULL REFERENCES "tehtava"("id") ON DELETE CASCADE,
    "tiedostonimi"  TEXT NOT NULL,
    "s3_avain"      TEXT NOT NULL
);

CREATE INDEX "tehtava_tiedosto_tehtava_idx" ON "tehtava_tiedosto" ("tehtava_id");