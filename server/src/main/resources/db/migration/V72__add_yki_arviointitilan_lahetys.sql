ALTER TABLE "yki_suoritus_lisatieto"
    ALTER COLUMN "tarkistusarviointi_hyvaksytty_pvm" DROP NOT NULL,
    ADD COLUMN "arviointitila_lahetetty" timestamp with time zone;
