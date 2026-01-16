ALTER TABLE "yki_suoritus"
    DROP CONSTRAINT "yki_suoritus_hetu_not_null",
    ALTER COLUMN "hetu" DROP NOT NULL;