UPDATE yki_suoritus
SET arviointitila = 'TARKISTUSARVIOINTI_HYVAKSYTTY'::yki_arviointitila
WHERE tarkistusarvioinnin_kasittely_pvm IS NOT NULL;
