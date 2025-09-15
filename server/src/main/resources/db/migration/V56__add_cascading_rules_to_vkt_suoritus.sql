ALTER TABLE "vkt_tutkinto"
    DROP CONSTRAINT "vkt_tutkinto_suoritus_id_fkey",
    ADD CONSTRAINT "vkt_tutkinto_suoritus_id_fkey" FOREIGN KEY ("suoritus_id") REFERENCES "vkt_suoritus"("id") ON DELETE CASCADE;


