DROP INDEX "yki_poikkeama_solki_id_kentta_havaittu_idx";

ALTER TABLE "yki_suoritus_poikkeama"
    ADD PRIMARY KEY ("solki_id", "kentta");
