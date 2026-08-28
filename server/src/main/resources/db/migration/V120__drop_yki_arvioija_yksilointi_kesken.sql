-- Arvioija lisataan vain oppijanumerolla ja yksiloimaton henkilo hylataan jo hakuvaiheessa, joten
-- keskeneraista yksilointia ei enaa synny eika sille tarvita omaa tilaa rekisterissa.
ALTER TABLE yki_arvioija
    DROP COLUMN yksilointi_kesken;
