CREATE TABLE "yki_arviointitilan_lahetys"
(
    "suoritus_id" integer NOT NULL,
    "lahetetty"   timestamp with time zone NOT NULL DEFAULT NOW(),
    PRIMARY KEY ("suoritus_id")
);
