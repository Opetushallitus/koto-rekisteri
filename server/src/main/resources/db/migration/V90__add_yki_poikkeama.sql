CREATE TABLE "yki_suoritus_poikkeama"
(
    "solki_id"      integer                  NOT NULL,
    "kentta"        text                     NOT NULL,
    "arvo_kitussa"  text,
    "arvo_solkissa" text,
    "havaittu"      timestamp with time zone NOT NULL
);

CREATE UNIQUE INDEX "yki_poikkeama_solki_id_kentta_havaittu_idx" ON "yki_suoritus_poikkeama" ("solki_id", "kentta", "havaittu");
