ALTER TABLE yki_suoritus
    ADD COLUMN tarkistusarvioidut_osakokeet_arr text[],
    ADD COLUMN arvosana_muuttui_arr             text[];

UPDATE yki_suoritus
SET tarkistusarvioidut_osakokeet_arr = array_remove(ARRAY [
                                                        case when tarkistusarvioidut_osakokeet & 1 <> 0 then 'PU' end,
                                                        case when tarkistusarvioidut_osakokeet & 2 <> 0 then 'KI' end,
                                                        case when tarkistusarvioidut_osakokeet & 4 <> 0 then 'TY' end,
                                                        case when tarkistusarvioidut_osakokeet & 8 <> 0 then 'PY' end
                                                        ], null),
    arvosana_muuttui_arr             = array_remove(ARRAY [
                                                        case when arvosana_muuttui & 1 <> 0 then 'PU' end,
                                                        case when arvosana_muuttui & 2 <> 0 then 'KI' end,
                                                        case when arvosana_muuttui & 4 <> 0 then 'TY' end,
                                                        case when arvosana_muuttui & 8 <> 0 then 'PY' end
                                                        ], null);

ALTER TABLE yki_suoritus
    DROP COLUMN tarkistusarvioidut_osakokeet,
    DROP COLUMN arvosana_muuttui;

ALTER TABLE yki_suoritus
    RENAME COLUMN tarkistusarvioidut_osakokeet_arr TO tarkistusarvioidut_osakokeet;

ALTER TABLE yki_suoritus
    RENAME COLUMN arvosana_muuttui_arr TO arvosana_muuttui;
