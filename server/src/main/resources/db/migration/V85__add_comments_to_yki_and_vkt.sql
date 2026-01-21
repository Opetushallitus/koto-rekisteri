-- yki_suoritus

COMMENT ON COLUMN yki_suoritus.suorittajan_oid IS 'Suorittajahenkilön oppijanumero';
COMMENT ON COLUMN yki_suoritus.sukunimi IS 'Suorittajan sukunimi';
COMMENT ON COLUMN yki_suoritus.etunimet IS 'Suorittajan etunimet välilyönneillä eroteltuna';
COMMENT ON COLUMN yki_suoritus.hetu IS 'Suomalainen henkilötunnus';
COMMENT ON COLUMN yki_suoritus.sukupuoli IS 'Sukupuoli mies / nainen / ei tiedossa';
COMMENT ON COLUMN yki_suoritus.kansalaisuus IS 'Kansalaisuuden lyhenne, esim. FIN';
COMMENT ON COLUMN yki_suoritus.katuosoite IS 'Katuosoite';
COMMENT ON COLUMN yki_suoritus.postinumero IS 'Postinumero';
COMMENT ON COLUMN yki_suoritus.postitoimipaikka IS 'Postitoimipaikka nimenä';
COMMENT ON COLUMN yki_suoritus.email IS 'Sähköposti';
COMMENT ON COLUMN yki_suoritus.tutkintopaiva IS 'Päivä jolloin tutkintotilaisuus on järjestetty';
COMMENT ON COLUMN yki_suoritus.tutkintokieli IS 'Kielen lyhenne, esim. FIN';
COMMENT ON COLUMN yki_suoritus.tutkintotaso IS 'PT / KT / YT';
COMMENT ON COLUMN yki_suoritus.arviointitila IS 'Arviointitilan tunniste. Sallitut arvot kuvattu sivulla YKI-arviointitilat Opintopolussa.';
COMMENT ON COLUMN yki_suoritus.jarjestajan_tunnus_oid IS 'Suoritustilaisuuden järjestäjän organisaatiotunnus. Tunnuksen on viitattava oppilaitokseen tai toimipisteeseen.';
COMMENT ON COLUMN yki_suoritus.jarjestajan_nimi IS 'Suoritustilaisuuden järjestäjän nimi tilaisuuden järjestämishetkellä.';
COMMENT ON COLUMN yki_suoritus.solki_id IS 'Suorituksen tunniste Solki-järjestelmässä';
COMMENT ON COLUMN yki_suoritus.last_modified IS 'Päivämäärä, jolloin suoritusta on päivitetty Solki-järjestelmässä';
COMMENT ON COLUMN yki_suoritus.koski_opiskeluoikeus IS 'Koskeen viedyn suorituksen tunniste KOSKI-järjestelmässä';

-- yki_osakoe

COMMENT ON COLUMN yki_osakoe.tyyppi IS 'Osakokeen lyhenne, esim. PU. Käytöstä poistuneet osat "rakenteet ja sanasto" sekä "yleisarvosana" mallinnetaan tällä samalla arvolla. Sallitut arvot löytyvät tiedostosta TutkinnonOsa.kt';
COMMENT ON COLUMN yki_osakoe.arviointipaiva IS 'Päivämäärä, jolloin osakoe on arvioitu';
COMMENT ON COLUMN yki_osakoe.arvosana IS 'Arvosana numeraalisessa muodossa. Perustasolla alle 1 arvo = Alle 1. Keskitasolla alle 3 arvo = Alle 3. Ylimmällä tasolla alle 5 arvo = Alle 5. 9 = Ei voi arvioida. 10 = Keskeytetty. 11 = Vilppi.';

-- yki_tarkistusarviointi

COMMENT ON COLUMN yki_tarkistusarviointi.saapumispaiva IS 'Tarkistusarviointipyynnön saapumispäivä';
COMMENT ON COLUMN yki_tarkistusarviointi.kasittelypaiva IS 'Tarkistusarvioinnin käsittelypäivä';
COMMENT ON COLUMN yki_tarkistusarviointi.asiatunnus IS 'OPH:n antama asiatunnus';
COMMENT ON COLUMN yki_tarkistusarviointi.perustelu IS 'Tarkistusarvioinnin perustelu. Jos tarkistusarviointi sisältää useamman osakokeen, niiden kaikkien perustelut ovat tässä yhdessä kentässä.';

-- yki_arvioija

COMMENT ON COLUMN yki_arvioija.arvioija_oid IS 'Arvioijan henkilö-oid';
COMMENT ON COLUMN yki_arvioija.henkilotunnus IS 'Suomalainen henkilötunnus';
COMMENT ON COLUMN yki_arvioija.sukunimi IS 'Arvioijan sukunimi';
COMMENT ON COLUMN yki_arvioija.etunimet IS 'Arvioijan etunimet välilyönnillä eroteltuna';
COMMENT ON COLUMN yki_arvioija.sahkopostiosoite IS 'Arvioijan sähköpostiosoite';
COMMENT ON COLUMN yki_arvioija.katuosoite IS 'Katuosoite';
COMMENT ON COLUMN yki_arvioija.postinumero IS 'Postinumero';
COMMENT ON COLUMN yki_arvioija.postitoimipaikka IS 'Postitoimipaikka nimenä';

-- yki_suoritus_lisatieto

COMMENT ON COLUMN yki_suoritus_lisatieto.solki_id IS 'Viittaa yki_suoritus.solki_id';
COMMENT ON COLUMN yki_suoritus_lisatieto.arviointitila_lahetetty IS 'Aika jolloin arviointitila on lähetetty ilmoittautumisjärjestelmään (KIOS)';
COMMENT ON COLUMN yki_suoritus_lisatieto.arviointitilan_lahetysvirhe IS 'Mahdollinen virhe lähetettäessä arviointitila ilmoittautumisjärjestelmään (KIOS)';
COMMENT ON COLUMN yki_suoritus_lisatieto.tarkistusarviointi_hyvaksytty_pvm IS 'Tarkistusarvioidun suorituksen hyväksymispäivä';

-- vkt_suoritus

COMMENT ON COLUMN vkt_suoritus.ilmoittautumisen_id IS 'Tunniste, joka määrittelee tiedon lähteen ja sen käyttämän vapaamuotoisen tunnisteen muodostaman yhdistelmän, esim. KIOS:HTT-31';
COMMENT ON COLUMN vkt_suoritus.etunimet IS 'Suorittajan etunimet välilyönneillä eroteltuna';
COMMENT ON COLUMN vkt_suoritus.sukunimi IS 'Suorittajan sukunimi';
COMMENT ON COLUMN vkt_suoritus.tutkintokieli IS 'Tutkintokieli. Sallitut arvot määritelty luokalla Koodisto.Tutkintokieli';
COMMENT ON COLUMN vkt_suoritus.suorituspaikkakunta IS 'Suorituspaikkakunnan kuntakoodi, esim. 091, kts. https://virkailija.testiopintopolku.fi/koodisto-service/ui/koodisto/view/kunta/2';
COMMENT ON COLUMN vkt_suoritus.taitotaso IS 'Taitotaso';
COMMENT ON COLUMN vkt_suoritus.suorituksen_vastaanottaja IS 'Suorituksen vastaanottajan henkilö-oid';
COMMENT ON COLUMN vkt_suoritus.koski_opiskeluoikeus IS 'Koskeen viedyn suorituksen tunniste KOSKI-järjestelmässä';

-- vkt_tutkinto

COMMENT ON COLUMN vkt_tutkinto.tyyppi IS 'Tutkinnon/kielitaidon tyyppi, esim. Suullinen';
COMMENT ON COLUMN vkt_tutkinto.arviointipaiva IS 'Viimeisin arviointipäivä tähän kielitaitoon liittyvistä osakokeista, mutta vain jos ne on kaikki arvioitu';
COMMENT ON COLUMN vkt_tutkinto.arvosana IS 'Osakokeista johdettu kielitaidon arvosana';

-- vkt_osakoe

COMMENT ON COLUMN vkt_osakoe.tyyppi IS 'Osakokeen tyyppi (esim. Puhuminen)';
COMMENT ON COLUMN vkt_osakoe.tutkintopaiva IS 'Osakokeen tutkintopäivä';
COMMENT ON COLUMN vkt_osakoe.arviointipaiva IS 'Osakokeen arviointipäivä';
COMMENT ON COLUMN vkt_osakoe.arvosana IS 'Osakokeen arvosana tai tieto suorituksen puuttumisesta';
COMMENT ON COLUMN vkt_osakoe.merkitty_poistettavaksi IS 'Aika jolloin osakoe on merkitty poistettavaksi käyttöliittymässä. Kun osakokeen retentioaika (määritelty konfiguraatiolla) täyttyy, rivi poistetaan tietokannasta.';
