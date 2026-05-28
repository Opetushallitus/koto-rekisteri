CREATE INDEX "yki_suoritus_solki_id_last_modified_idx"
    ON "yki_suoritus" ("solki_id", "last_modified" DESC, "id" DESC);

CREATE INDEX "yki_suoritus_tutkintopaiva_idx"
    ON "yki_suoritus" ("tutkintopaiva" DESC);