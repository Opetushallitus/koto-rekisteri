ALTER TABLE yki_arvioija
    DROP CONSTRAINT yki_arvioija_is_unique,
    ADD CONSTRAINT yki_arvioija_is_unique UNIQUE NULLS NOT DISTINCT (
         arvioijan_oppijanumero,
         henkilotunnus,
         sukunimi,
         etunimet,
         sahkopostiosoite,
         katuosoite,
         postinumero,
         postitoimipaikka,
         tila,
         kieli,
         tasot,
         ensimmainen_rekisterointipaiva,
         kauden_alkupaiva,
         kauden_paattymispaiva,
         jatkorekisterointi
    );
