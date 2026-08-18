-- Hetuja ei saa tallentaa 1.1.2026 tai myöhemmin järjestetyille YKI-tutkinnoille.
-- Vanha Solki-CSV-import tallensi niitä 30.3.2026 asti, joten poistetaan ne kaikista versioriveistä.
UPDATE yki_suoritus
SET hetu = NULL
WHERE hetu IS NOT NULL
  AND tutkintopaiva >= '2026-01-01';