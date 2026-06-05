ALTER TABLE "tehtavapaketti"
    ADD COLUMN "lahde_published" TIMESTAMP WITH TIME ZONE,
    ADD COLUMN "lahde_version"   TEXT,
    ADD COLUMN "lahde_language"  TEXT;
