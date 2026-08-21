# YKI-arvioijarekisterin siirto Solkilta Kituun

## Context

Tähän asti YKI-arvioijarekisteriä on hallinnoitu Jyväskylän yliopiston Solki-järjestelmässä, ja
Kielitutkintorekisteri (kitu) on ollut vain vastaanottava osapuoli: Solki työntää arvioijatiedot
kituun `POST /yki/api/arvioija` -rajapinnalla, ja kitu näyttää ne read-only-listana osoitteessa
`/yki/arvioijat`.

Vaatimusmäärittelyn (OPH:n käyttötapauskuvaus, `709673053_…-200826-1322-246.pdf`) mukaan hallinta
siirtyy kituun: **kitu on jatkossa arvioijadatan master**, OPH-virkailija ylläpitää rekisteriä kitun
syöttökäyttöliittymällä, ja kitu välittää tiedot rajapinnan yli Solkin YKI-sovellukseen.

Kolme käyttötapausta:

1. **Uuden arvioijan merkintä** — virkailija lisää arvioijan hallintopäätöksen perusteella. Järjestelmä
   hakee oppijanumeron ONR:stä hetun ja nimen perusteella (ja luo henkilön ONR:ään jos ei löydy),
   laskee 5 vuoden voimassaoloajan automaattisesti ja lähettää tiedot Solkille.
2. **Merkinnän muokkaus / uusi kausi** — virkailija syöttää uuden kauden alkupäivän, järjestelmä laskee
   päättymispäivän; myös yhteystietoja ja arviointioikeuksia voi muokata. Tiedot lähetetään Solkille.
3. **Passivointi** — järjestelmä passivoi arvioijan automaattisesti kauden päättymispäivän jälkeen;
   virkailija voi passivoida myös manuaalisesti kesken kauden.

Virhetilanteissa Solkille lähetys yritetään **3 kertaa** ja sen jälkeen **säännöllisesti uudestaan
(joka yö)**; virkailija näkee epäonnistuneet lähetykset kitun virhenäkymästä syineen.

Vaatimusmäärittelyn alustava työmääräarvio: syöttökäyttöliittymä ~5 vk (tietokanta 1 vk, lisäys 1 vk,
uusi hlö ONR:ään 1 vk, muokkaus + passivointi 2 vk) + Solki-integraatio 1 vk.

### Lähtötilanne koodissa (tarkistettu)

- Solkin **CSV-tuontiputki on jo purettu** (commitit `279bd81f`, `e0ff5d3f`, `d160c1f1`, `ff82bc5a`).
  Ainoa elävä sisääntulo on `POST /yki/api/arvioija`. `CLAUDE.md` ja
  `docs/technical/arkkitehtuuri.md` väittivät yhä CSV-putken olevan olemassa — **korjattu tässä PR:ssä**.
- Taulu `yki_arvioija_error` ja koko `yki/arvioijat/error/`-paketti on **kuollutta koodia**:
  kirjoituspolkua ei ole enää, joten `/yki/arvioijat/virheet` ja dashboardin laskuri ovat pysyvästi nollia.
- `dev/YkiController.kt` `GET /dev/yki/import/arvioijat` on kuollut stubi.
- Arvioijatiedoissa **ei ole koskaan ollut puhelinnumeroa**.
- `yki_arvioija`-rivit ovat jo kitussa, joten erillistä datan tuontia Solkilta ei tarvita — vain skeeman
  täydennys.
- Lomakekuvioita on olemassa (`vkt/html/VktErinomaisenArviointiPage.kt`), mutta **luontilomaketta eikä
  kenttäkohtaista validointivirheiden renderöintiä ei ole missään** — ne on rakennettava.
- Serverimoduulissa **ei ole JS-buildia** CLAUDE.md:n esbuild-väitteestä huolimatta; ainoa selainlogiikan
  mekanismi on `html/Common.kt`:n inline `javascript(code)`.

### Päätökset (sovittu)

| Kysymys             | Päätös                                                                                                                         |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Solkin rajapinta    | Ei ole vielä määritelty — **kitu ehdottaa sopimuksen**, sovitaan Jyväskylän kanssa                                             |
| Olemassa oleva data | Laajennetaan nykyisiä tauluja, `POST /yki/api/arvioija` poistetaan käytöstä                                                    |
| Laajuus             | Koko PDF:n sisältö                                                                                                             |
| Käyttöoikeus        | Uusi `Authority`-arvo                                                                                                          |
| Kausi/tila-taso     | **Pysyy arviointioikeuskohtaisena** (`yki_arviointioikeus`) — käyttöliittymä asettaa saman kauden kaikille valituille kielille |
| **Kausihistoria**   | **Talletetaan kantaan** — oma `yki_arvioija_kausi`-taulu, johon jokainen kausi kirjataan                                       |
| Puhelinnumero       | **Ei toteuteta toistaiseksi** — puuttuu tavoitetilan tietotaulukosta (§11 kys. 16)                                             |
| Listanäkymä         | Täysi käsittely: suodatus, sivutus, CSV-vienti                                                                                 |

### OPH:n vastaukset avoimiin kysymyksiin (21.8.2026)

Kaikki 14 OPH:lle esitettyä kysymystä on vastattu 21.8.2026. Vastaukset on viety suunnitelmaan;
**kuusi niistä muutti aiempaa ratkaisua** (merkitty ✱). Loput avoimet asiat ovat §11:ssä.

| #    | Kysymys                        | Päätös                                                                                    |
| ---- | ------------------------------ | ----------------------------------------------------------------------------------------- |
| 1    | Kauden päättymispäivä          | `alkupäivä + 5 v` samana päivänä                                                          |
| 2    | Päättymispäivän inklusiivisuus | Inklusiivinen — passivointi vasta päivän jälkeen                                          |
| 3 ✱  | Jatkokausi                     | **Järjestelmä päättelee automaattisesti `Kyllä`**, virkailija voi ylikirjoittaa           |
| 4 ✱  | Yksilöimätön henkilö           | **Merkintä saa tallentua keskeneräisenä** ja täydentyy myöhemmin                          |
| 5 ✱  | Turvakielto                    | **Ei rajoituksia tietoihin** — käyttöliittymässä näytetään varoitus virkailijalle         |
| 6    | Merkinnän poisto               | Ei toteuteta; passivointi riittää                                                         |
| 7    | Lukuoikeus                     | Säilyy kaikilla kitu-virkailijoilla                                                       |
| 8    | Hetu-sarake                    | **Jää avoimeksi** — säilytetään toistaiseksi, tietosuojan päätös myöhemmin                |
| 9    | Kausihistorian oikeusperuste   | Osa rekisterimerkinnän elinkaarta, sama 5 v säilytysaika                                  |
| 10 ✱ | Säilytysajan alkuhetki         | **Passivointihetkestä**, ei kauden päättymispäivästä                                      |
| 11 ✱ | Puhelinnumero                  | **Ei toteuteta** — tavoitetilan tietotaulukko pätee                                       |
| 12 ✱ | Hallintopäätöksen ASHA-numero  | **Toteutetaan** vapaana tekstikenttänä, ei muotovalidointia                               |
| 13   | Käyttöoikeus Otuvaan           | OPH perustaa; pyyntö tehdään heti, jotta läpimenoaika ei estä julkaisua                   |
| 14   | Cutover                        | Syöttökäyttöliittymä julkaistaan ensin, Solkin kirjoitukset katkaistaan vasta sen jälkeen |

Kaksi vastausta laajentaa toteutusta merkittävästi:

- **Keskeneräinen yksilöinti (kys. 4)** vaatii oman tilan, ajastetun täydennyksen ONR:stä ja
  Solki-lähetyksen lykkäämisen siihen asti kunnes oppijanumero on olemassa (§2.7, §6.3).
- **Säilytysaika passivointihetkestä (kys. 10)** vaatii uuden `passivoitu`-aikaleiman, jota kannassa ei
  tällä hetkellä ole (§1.1, §6.2).

### Muut lähdedokumentit: prosessikuvaukset ja tallennettavat tiedot

Käyttötapauskuvauksen lisäksi käytössä on kaksi OPH:n prosessidokumenttia:

| Dokumentti                        | Sisältö                                                                                    |
| --------------------------------- | ------------------------------------------------------------------------------------------ |
| `699059123_…-210826-0946-254.pdf` | _Nykytilan ja uuden prosessin kuvaus_ — nykyprosessi + "miten prosessia halutaan uudistaa" |
| `721421207_…-210826-1009-258.pdf` | _Tavoitetila_ — sama prosessi tavoitetilassa + **tavoitetilan tietotaulukko**              |

Molempien lopussa on taulukko **"Arvioijarekisteriin tallennettavat tiedot"** (oikeusperuste, tietolähde,
säilytyspaikka, säilytysaika). **Tavoitetilan taulukko on suunnittelun kannalta ratkaiseva** — nykytilan
taulukkoa luetaan vain lähtötilanteen kuvauksena (sen rivi _"Rekisteriintuontiaika (kun tiedot on siirretty
Solkilta kielitutkintorekisteriin)"_ kuvaa nykyistä Solki→kitu-suuntaa).

Tavoitetilan taulukon mukaan kitussa säilytetään: oppijanumero, hetu vain ennen 2026 tehdyille merkinnöille,
sukunimi, etunimet, sähköpostiosoite, osoite, tila, tutkinnot (kieli ja taso), viimeisimmän
rekisteröintikauden alku- ja päättymispäivä, jatkorekisteröinti, rekisteröintiaika, rekisteriintuontiaika ja
**hallintopäätöksen ASHA-numero**. Kaikilla säilytysaika **5 vuotta**.

Dokumenttien vahvistamat asiat:

- **Siirto Solkille tehdään arvioijan OID-tunnisteella:** _"Kielitutkintorekisteristä siirtyy uudet
  merkinnät Solkin YKI-kantaan arvioijan OID-tunnisteella."_ Vahvistaa §5.1:n avaimennusratkaisun.
- **Solki säilyttää omat lisätietonsa:** _"Solkilla on kopio arvioijarekisteristä (Solki voi täydentää
  rekisterimerkintöjä omilla tiedoillaan, esim. kauden arviointikerrat)."_ → §5.1:n täyden tilan PUT saa
  korvata **vain OPH:n omistamat kentät**, ei Solkin omia (arviointikerrat, huomautukset, lisätiedot,
  liitteet, puhelinnumerot, postinumero).
- **Kaikilla kielillä on sama kausi:** _"Pidetään edelleen käytäntönä, että henkilöllä ei ole päällekkäisiä
  arviointikausia vaan kaikilla sama kausi."_ → validointisääntö: kaikkien arviointioikeuksien kauden on
  oltava identtinen (§2.4). Vahvistaa käyttöliittymäratkaisun.
- **70 vuoden ikärajaa ei toteuteta:** _"Arviointikauden kesto on aina 5 vuotta, ei oteta teknisesti
  käyttöön 70v rajoitusta."_ (Solkin nykyjärjestelmässä kausi ei voi jatkua sen vuoden jälkeen, kun
  arvioija täyttää 70 — kituun tätä ei tule.)
- **Liitteet ja hallintopäätös eivät tule kituun.** Hallintopäätös laaditaan Ashassa ja tallennetaan
  liitteeksi Solkin järjestelmään; taulukossa _Liitteet_ on Solki-only. Kituun ei siis toteuteta
  liitteiden käsittelyä.
- **Säilytysaika kielitutkintorekisterissä on 5 vuotta** kaikille kitussa säilytettäville kentille.
  → **Uusi vaatimus, jota käyttötapauskuvauksessa ei ollut** (§6.2).
- **Uusi kieli tai taso liittyy olemassa olevaan kauteen:** _"Henkilön kaikilla kielillä ja eri
  tutkintotasoilla on yhtä pitkä arviointikausi. Jos henkilölle tulee uusi arvioitava kieli tai taitotaso,
  niin arviointikausi kaikille alkaa ja päättyy samaan aikaan."_ → kun virkailija lisää kielen kesken
  kauden, **järjestelmä kopioi voimassa olevan kauden päivämäärät uudelle arviointioikeudelle** eikä kysy
  uutta alkupäivää.
- **Arvioija on jo olemassa Solkissa ennen kitun lähetystä:** _"Solki tarvitsee arvioijille käyttäjätunnuksen
  ja viisinumeroisen arvioijatunnuksen YKI-sovellukseen, mutta arvioijarekisterimerkinnän tiedot voidaan
  lisätä arvioijan tietoihin kielitutkintorekisteristä OID-tunnisteella."_ Solki luo arvioijan tunnukset jo
  koulutuksen yhteydessä → kitun lähetys on käytännössä **päivitys olemassa olevaan Solki-riviin**, ei
  luonti (§11 JYU-kysymys 6).
- **Uusi kenttä: hallintopäätöksen ASHA-numero.** Tavoitetilan taulukossa on rivi _"Hallintopäätöksen
  ASHA-numero — Esittelijä kirjaa — Kielitutkintorekisteri — 5 vuotta"_, varauksella _"Tälle tiedolle ei ole
  välttämättä tarvetta. Katsotaan prosessivastaavan kanssa syksyllä."_ → varaudutaan valinnaiseen
  tekstikenttään, toteutus vasta kun tarve on vahvistettu (päätöstaulukko kys. 12).

**Kaksi tavoitetilakuvauksen kohtaa muuttaa aiempia suunnitelmapäätöksiä** — molemmat on merkitty
suunnitelmaan ja avoimiin kysymyksiin:

1. **Solki→kitu-muutosrajapintaa tarvitaan yhä.** Tavoitetilassa arvioijan omasta toiveesta tapahtuva
   kesken kauden passivointi kulkee _Solkista kituun_: _"Solki tekee merkinnän YKI-sovellukseen ja tieto
   siirretään muutosrajapintaa pitkin kielitutkintorekisteriin."_ Sisääntulevaa rajapintaa ei siis voi
   poistaa kokonaan (§4.2, §11 JYU-kysymys 5).
2. **Puhelinnumero puuttuu tavoitetilan tietotaulukosta.** Käyttötapauskuvaus vaatii sen lomakkeelle,
   mutta tavoitetilan taulukossa ei ole työpuhelinta, kotipuhelinta eikä puhelinnumeroa lainkaan
   (nykytilan taulukossa ne ovat Solki-only). Sarake jätetään toteuttamatta kunnes asia on ratkaistu
   (§1.1, ks. päätöstaulukko kys. 11).

Muut **ristiriidat ja tarkennukset** ovat avoimina kysymyksinä §11:ssä (päätöstaulukko ja §11).

---

## 1. Tietokanta

Korkein käytössä oleva migraatio on **V115** — jatka siitä, älä käytä numeroaukkoja.

### 1.1 `V116__yki_arvioija_master.sql`

```sql
-- Kitu on YKI-arvioijarekisterin master. Arvioijatason lisäkentät, muokkausjäljet
-- ja Solki-lähetyksen outbox.
ALTER TABLE yki_arvioija
    -- HUOM. puhelinnumeroa EI lisätä: tavoitetilan tietotaulukossa ei ole puhelinnumeroa
    -- lainkaan, vaikka käyttötapauskuvaus pyytää sen lomakkeelle. Ks. päätöstaulukko kys. 11.
    ADD COLUMN luotu                        TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN luoja_oid                    henkilo_oid,
    ADD COLUMN muokattu                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN muokkaaja_oid                henkilo_oid,
    ADD COLUMN solkiin_lahetetty            TIMESTAMPTZ,
    ADD COLUMN solki_lahetysvirhe           TEXT,
    ADD COLUMN solki_lahetysyritykset       INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN solki_viimeisin_lahetysyritys TIMESTAMPTZ,
    -- Hallintopaatoksen ASHA-numero: vapaa tekstikentta, ei muotovalidointia (OPH kys. 12)
    ADD COLUMN asha_numero                  TEXT,
    -- Sailytysaika lasketaan passivointihetkesta, joten se on tallennettava (OPH kys. 10)
    ADD COLUMN passivoitu                   TIMESTAMPTZ,
    -- Merkinta voi syntya ennen kuin ONR on yksiloinyt henkilon (OPH kys. 4)
    ADD COLUMN yksilointi_kesken            BOOLEAN     NOT NULL DEFAULT FALSE;

-- tila on Kotlinissa non-null mutta kannassa nullable
UPDATE yki_arviointioikeus SET tila = 'AKTIIVINEN' WHERE tila IS NULL;
ALTER TABLE yki_arviointioikeus
    ALTER COLUMN tila SET DEFAULT 'AKTIIVINEN',
    ALTER COLUMN tila SET NOT NULL;

-- KRIITTINEN: ennen kituun siirtoa data on peräisin Solkista, joten sitä ei työnnetä takaisin.
-- Ilman tätä ensimmäinen yöajo lähettäisi koko historiallisen rekisterin Solkiin.
UPDATE yki_arvioija a
SET luotu             = COALESCE(t.viimeisin, now()),
    muokattu          = COALESCE(t.viimeisin, now()),
    solkiin_lahetetty = COALESCE(t.viimeisin, now())
FROM (SELECT arvioija_id, max(rekisteriintuontiaika) AS viimeisin
      FROM yki_arviointioikeus GROUP BY arvioija_id) t
WHERE a.id = t.arvioija_id;

UPDATE yki_arvioija
SET solkiin_lahetetty = COALESCE(solkiin_lahetetty, now());

-- Sailytysaika lasketaan passivointihetkesta. Jo passivoiduille vanhoille riveille hetki
-- taytetaan kauden paattymispaivasta, jotta niiden sailytysaika kuluu historiallisella
-- aikajanalla eika ala vasta kayttoonotosta.
UPDATE yki_arvioija a
SET passivoitu = t.paattyi
FROM (
    SELECT arvioija_id,
           max(kauden_paattymispaiva)::timestamptz AS paattyi
    FROM yki_arviointioikeus
    GROUP BY arvioija_id
    HAVING bool_and(tila = 'PASSIVOITU')
       AND max(kauden_paattymispaiva) IS NOT NULL
) t
WHERE a.id = t.arvioija_id;

CREATE INDEX yki_arvioija_solki_lahettamattomat_idx
    ON yki_arvioija (muokattu)
    WHERE solkiin_lahetetty IS NULL OR solkiin_lahetetty < muokattu;

CREATE INDEX yki_arviointioikeus_passivointi_idx
    ON yki_arviointioikeus (kauden_paattymispaiva)
    WHERE tila = 'AKTIIVINEN';

COMMENT ON COLUMN yki_arvioija.asha_numero IS 'Rekisterimerkintaa koskevan hallintopaatoksen ASHA-numero, esittelijan kirjaama';
COMMENT ON COLUMN yki_arvioija.passivoitu IS 'Hetki jolloin merkinta passivoitiin; sailytysajan (5 v) laskennan alkupiste';
COMMENT ON COLUMN yki_arvioija.yksilointi_kesken IS 'true = arvioija_oid on ONR:n henkilo-OID eika viela oppijanumero; taydennetaan ajastetusti';
COMMENT ON COLUMN yki_arvioija.solkiin_lahetetty IS 'Milloin rivi on viimeksi onnistuneesti lähetetty Solkiin; NULL tai < muokattu = lähetys kesken';
COMMENT ON COLUMN yki_arvioija.solki_lahetysvirhe IS 'Viimeisimmän epäonnistuneen Solki-lähetyksen virheteksti';
COMMENT ON COLUMN yki_arvioija.solki_lahetysyritykset IS 'Peräkkäisten epäonnistuneiden lähetysyritysten määrä; nollataan onnistuneessa lähetyksessä';
```

Ei triggereitä (projektissa ei ole yhtään) — `muokattu` asetetaan aina eksplisiittisesti UPDATE-lauseissa.

### 1.2 `V117__yki_arvioija_kausihistoria.sql`

Kausi ja tila **pysyvät** `yki_arviointioikeus`-rivillä (= nykyinen, voimassa oleva kausi). Sen rinnalle
tulee append-only-historiataulu, johon kirjataan jokainen kausi. Tällä mallilla kaikki nykyiset kyselyt,
Solki-payload ja passivointi toimivat ennallaan, ja historia on erillinen luettava aikajana.

```sql
CREATE TABLE yki_arvioija_kausi (
    id                     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    arvioija_id            INTEGER            NOT NULL REFERENCES yki_arvioija (id) ON DELETE CASCADE ON UPDATE CASCADE,
    kieli                  yki_tutkintokieli  NOT NULL,
    tasot                  TEXT[]             NOT NULL,
    tila                   yki_arvioija_tila  NOT NULL,
    kauden_alkupaiva       DATE,
    kauden_paattymispaiva  DATE,
    jatkorekisterointi     BOOLEAN            NOT NULL DEFAULT FALSE,
    kirjattu               TIMESTAMPTZ        NOT NULL DEFAULT now(),
    kirjaaja_oid           henkilo_oid,
    CONSTRAINT yki_arvioija_kausi_unique UNIQUE NULLS NOT DISTINCT
        (arvioija_id, kieli, kauden_alkupaiva, kauden_paattymispaiva, tila, jatkorekisterointi)
);

CREATE INDEX yki_arvioija_kausi_arvioija_idx ON yki_arvioija_kausi (arvioija_id, kirjattu DESC);

-- Nykytila historian ensimmäiseksi riviksi
INSERT INTO yki_arvioija_kausi
    (arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva, jatkorekisterointi, kirjattu)
SELECT arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva,
       jatkorekisterointi, rekisteriintuontiaika
FROM yki_arviointioikeus;

COMMENT ON TABLE yki_arvioija_kausi IS 'Arvioijarekisterimerkintöjen kausihistoria: yksi rivi jokaisesta kirjatusta rekisteröintikaudesta kielikohtaisesti. Voimassa oleva kausi elää yki_arviointioikeus-taulussa.';
```

**Kirjoitussääntö:** jokainen tallennus vertaa uutta kautta voimassa olevaan, ja lisää historiarivin vain
kun jokin kausikentistä muuttuu (`ON CONFLICT DO NOTHING` uniikkiehdon turvin). Näin pelkkä yhteystiedon
korjaus ei kasvata historiaa.

### 1.3 `V118__drop_yki_arvioija_error.sql`

```sql
-- Taulun kirjoituspolku poistui CSV-tuonnin mukana (commitit 279bd81f, e0ff5d3f,
-- d160c1f1, ff82bc5a). Solki-lähetysten virheet elävät nyt yki_arvioija-rivillä.
DROP TABLE IF EXISTS yki_arvioija_error;
```

### 1.4 Entiteetit

`yki/arvioijat/YkiArvioijaEntity.kt`:

- `YkiArvioijaEntity` += `luotu`, `luojaOid`, `muokattu`, `muokkaajaOid`,
  `solkiinLahetetty`, `solkiLahetysvirhe`, `solkiLahetysyritykset`, `solkiViimeisinLahetysyritys`.
  `RowMapper`-companion päivitetään vastaavasti.
- `YkiArviointioikeusEntity` säilyy sellaisenaan.
- Uusi `YkiArvioijaKausiEntity` (`@Table("yki_arvioija_kausi")`) + `fromRow`.
- `YkiArvioijaArviointioikeus`-liitosprojektio (`YkiArvioijaRepository.kt`) laajenee uusilla kentillä.
- `henkilotunnus` jää sarakkeeksi mutta **kirjoituspolku ei enää koskaan aseta sitä** (2026 lainmuutos).
  Poisto vasta tietosuojahyväksynnän jälkeen (avoin kysymys §12).

---

## 2. Palvelukerros

Uudet tiedostot pakettiin `server/src/main/kotlin/fi/oph/kitu/yki/arvioijat/`. Kaikki riippuvuudet
konkreettiselle `@Service`-luokalle — **ei abstraktia kantaluokkaa**, koska `@WithSpan` + CGLIB lukee
kantaluokan injektoidut kentät `null`iksi (CLAUDE.md).

### 2.1 `Rekisterikausi.kt` — 5 vuoden laskenta yhdessä paikassa

```kotlin
object Rekisterikausi {
    const val KAUDEN_PITUUS_VUOSINA = 5L

    /** Rekisterimerkintä on voimassa 5 vuotta alkupäivästä, kaikille tutkintokielille ja tasoille. */
    fun paattymispaiva(alkupaiva: LocalDate): LocalDate = alkupaiva.plusYears(KAUDEN_PITUUS_VUOSINA)
}
```

`plusYears(5)` (ei `−1 pv`), koska nykyinen Solki-data käyttää tarkalleen +5 v samaa päivää
(`dev/YkiController.kt`-fixture: `2015-12-07 → 2020-12-07`). Varmistettava OPH:lta (§12).
Puhdas `object`, ei `@Service` — ei CGLIB-proxya, testattavissa ilman Spring-kontekstia.

### 2.2 `YkiArvioijaCommand.kt` — sisäinen komento, korvaa poistuvan API-DTO:n

```kotlin
data class TallennaArvioija(
    val arvioijaOid: Oid,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val kaudenAlkupaiva: LocalDate,
    val jatkorekisterointi: Boolean,
    val tila: YkiArvioijaTila,
    val ashaNumero: String?,
    val arviointioikeudet: List<Arviointioikeus>,
) {
    /** Sama kausi kaikille valituille kielille — vaatimus: "5 vuotta kaikille tutkintokielille ja tasoille". */
    val kaudenPaattymispaiva: LocalDate get() = Rekisterikausi.paattymispaiva(kaudenAlkupaiva)

    data class Arviointioikeus(val kieli: Tutkintokieli, val tasot: Set<Tutkintotaso>)
}
```

**Ei `henkilotunnus`-kenttää** — hetu kulkee vain ONR-hakupyynnössä eikä koskaan päädy kantaan.

### 2.3 `YkiArvioijaError.kt` — Either-vasemman puolen tyyppi

```kotlin
sealed interface YkiArvioijaError {
    data class Validointivirheet(val virheet: NonEmptyList<ValidationError>) : YkiArvioijaError
    data class OppijanumeroaEiSaatu(val syy: OppijanumeroException) : YkiArvioijaError
    data class OppijaaEiYksiloity(val oid: Oid?) : YkiArvioijaError
    data object ArvioijaaEiLoydy : YkiArvioijaError
}
```

### 2.4 `YkiArvioijaValidation.kt` — kirjoitetaan uusiksi tyypille `TallennaArvioija`

Säilytetään nykyinen rakenne (`accumulate { accumulating { … } }`, `util/validation/Validation.kt`) ja
ONR-olemassaolotarkistus (`OppijanumeroValidation.validateOppijanumeroInOnr`). Lisättävät säännöt:
pakolliset kentät, postinumeron muoto (5 numeroa), sähköpostin muoto, vähintään yksi arviointioikeus,
ei tyhjiä tasojoukkoja, ei duplikaattikieliä, kauden alkupäivä ei yli vuotta tulevaisuudessa.

Tavoitetilakuvauksesta johdettu lisäsääntö: **kaikkien arviointioikeuksien kauden on oltava identtinen**
(_"Henkilön kaikilla kielillä ja eri tutkintotasoilla on yhtä pitkä arviointikausi"_). Käyttöliittymä
takaa tämän kirjoittamalla saman kauden kaikille riveille, ja `validateAfterEnrichment` varmistaa sen —
näin per-kieli-tallennus ei voi ajautua epäjohdonmukaiseksi esimerkiksi rajapinnan kautta.

Kun virkailija **lisää kielen tai tason kesken kauden**, uusi arviointioikeus saa voimassa olevan kauden
päivämäärät (_"Jos henkilölle tulee uusi arvioitava kieli tai taitotaso, niin arviointikausi kaikille alkaa
ja päättyy samaan aikaan"_) — lomake ei kysy uutta alkupäivää, vaan `enrich` kopioi sen olemassa olevalta
merkinnältä.

`ValidationError.path` täytetään lomakkeen kenttänimillä (`listOf("postinumero")`) — se on §3.1:n
`FormErrors`-kartan avain. Hetun siirtokielto (`kitu.validaatiot.yki.hetunSiirronRajapaiva`) poistuu tästä
validaattorista, koska hetua ei enää oteta vastaan lainkaan; property jää `YkiSuoritusValidation`in käyttöön.

### 2.5 `YkiArvioijaService.kt`

```kotlin
@Service
class YkiArvioijaService(
    private val repository: YkiArvioijaRepository,
    private val validationService: ValidationService,
    private val solki: SolkiArvioijaService,
    private val oppijanumeroHaku: OppijanumeroHakuService,
    private val oppijanumeroService: OppijanumeroService,
    private val auditLogger: AuditLogger,
    private val timeService: TimeService,
) {
    @WithSpan fun haeArvioija(id: Int): YkiArvioijaEntity?                       // + YkiArvioijaViewed
    @WithSpan fun haeKausihistoria(arvioijaId: Int): List<YkiArvioijaKausiEntity>
    @WithSpan fun haeSivullinen(params: YkiArvioijaParams): List<YkiArvioijaListRow>
    @WithSpan fun laske(params: YkiArvioijaParams): Long

    @WithSpan fun haeHenkilotiedot(haku: OnrHaku): Either<YkiArvioijaError, ArvioijanEsitaytto>
    @WithSpan fun luoArvioija(k: TallennaArvioija, tekija: Oid?): Either<YkiArvioijaError, YkiArvioijaEntity>
    @WithSpan fun paivitaArvioija(id: Int, k: TallennaArvioija, tekija: Oid?): Either<YkiArvioijaError, YkiArvioijaEntity>
    @WithSpan fun passivoiArvioija(id: Int, tekija: Oid?): Either<YkiArvioijaError, YkiArvioijaEntity>

    /** Ajastettu (§6). */ @WithSpan fun passivoiPaattyneetKaudet(): Int
}
```

`luoArvioija` / `paivitaArvioija` kulku:

1. `validationService.validateAndEnrich(komento)` → `Left(Validointivirheet)` epäonnistuessa.
   **Ei `getOrThrow()`** — lomakepolku tarvitsee kenttäkohtaiset virheet, ei 400-JSONia.
2. `repository.tallenna(...)` yhdessä transaktiossa: upsert `yki_arvioija`, korvaa `yki_arviointioikeus`
   -rivit (myös poistot), kirjaa muuttuneet kaudet `yki_arvioija_kausi`-tauluun.
   Samalla `muokattu = now()`, `muokkaaja_oid`, `solkiin_lahetetty = NULL`, `solki_lahetysvirhe = NULL`,
   `solki_lahetysyritykset = 0`.
3. `auditLogger.log(YkiArvioijaCreated | YkiArvioijaUpdated, komento.arvioijaOid)`.
4. `solki.lahetaArvioija(id)` — **yksi synkroninen yritys**, jotta virkailija näkee tuloksen heti.
   Epäonnistuminen ei kaada tallennusta; rivi jää outboxiin.
5. `Right(entity)` — controller lukee `solkiLahetysvirhe`-kentän flash-viestiä varten.

**ONR-haku:** `YkiApiController.postOppijanumeroHaku`:n runko **erotetaan** uuteen
`oppijanumero/OppijanumeroHakuService.kt`-palveluun, jota sekä API-kontrolleri että tämä käyttävät.
`OppijanumeroService.getMasterOid(Oppija(etunimet, hetu, kutsumanimi, sukunimi))` POSTaa ONR:n
`yleistunniste/hae`-endpointiin, joka **luo henkilön jos sitä ei ole** — PDF:n vaihtoehtoinen kulku
"järjestelmä luo hänelle uuden oppijanumeron" toteutuu ilman lisäkoodia. Ainoa aito epäonnistuminen on
yksilöimätön henkilö (`OppijaNotIdentifiedException`) → `OppijaaEiYksiloity`, jonka lomake renderöi
virheeksi + ONR-linkiksi (sama kuvio kuin `YkiSuoritusPage.henkilonTiedot`).
Esitäyttö `getHenkiloByMasterOid(oid)`:n `etunimet`/`kutsumanimi`/`sukunimi` +
`yhteystiedotRyhma[].yhteystieto[]`-arvoista (sähköposti, osoite).

### 2.7 Keskeneräinen yksilöinti (OPH kys. 4)

OPH:n päätöksen mukaan merkintä **saa syntyä, vaikka ONR ei ole vielä yksilöinyt henkilöä**. ONR:n
`yleistunniste/hae` palauttaa tällöin `oid`-kentän (henkilö-OID) mutta `oppijanumero` on `null`, jolloin
nykyinen `getMasterOid(oppija)` päätyy `OppijaNotIdentifiedException`iin.

Toimintamalli:

1. Tallennuksessa käytetään ONR:n palauttamaa **henkilö-OID:ta** `arvioija_oid`-sarakkeessa
   (`henkilo_oid`-domain hyväksyy sen) ja merkitään `yksilointi_kesken = true`.
   Käyttöliittymä näyttää merkinnällä varoituksen "odottaa yksilöintiä".
2. **Solki-lähetys ohitetaan** niin kauan kuin `yksilointi_kesken = true`: `findLahetettavat` suodattaa ne
   pois. Tavoitetilakuvauksen mukaan Solki tunnistaa arvioijan OID-tunnisteella, joten vaillinaista
   tunnistetta ei lähetetä.
3. Ajastettu tehtävä (§6.3) yrittää uudelleen `oppijanumeroService.getMasterOid(henkiloOid)`. Onnistuessa
   `arvioija_oid` päivitetään master-OID:ksi, `yksilointi_kesken` nollataan ja outbox likataan, jolloin
   merkintä lähtee Solkiin normaalisti.

**Varottava reunatapaus:** `arvioija_oid` on `UNIQUE`, joten yksilöinnin valmistuttua master-OID voi jo
olla toisella rivillä (sama henkilö on ehditty lisätä kahdesti). Päivitys on siis tehtävä
`ON CONFLICT`-tietoisesti: jos master-OID löytyy jo, rivit on yhdistettävä tai keskeneräinen merkintä
merkittävä virheelliseksi virkailijan ratkaistavaksi — **ei saa kaatua uniikkirikkeeseen**.

### 2.8 Jatkokauden päättely (OPH kys. 3)

`jatkorekisterointi` päätellään `enrich`-vaiheessa: **`true`, jos arvioijalla on jo aiempi
rekisteröintikausi** (`yki_arvioija_kausi`-taulussa on rivi tai merkintä on olemassa ennestään).
Virkailija voi ylikirjoittaa arvon lomakkeen valintaruudulla, joten kyseessä on esitäyttö, ei pakotus.
Uudella arvioijalla oletus on `false`.

### 2.6 `YkiArvioijaRepository.kt`

Poistetaan `saveAllNewEntities` (kuoli CSV-tuonnin mukana). `upsert` korvataan `tallenna`-metodilla.

```kotlin
interface CustomYkiArvioijaRepository {
    @Transactional fun tallenna(arvioija: YkiArvioijaEntity, tekija: Oid?): Int
    fun findByArvioijaOid(oid: Oid): YkiArvioijaEntity?
    fun findById(id: Int): YkiArvioijaEntity?
    fun findKausihistoria(arvioijaId: Int): List<YkiArvioijaKausiEntity>
    fun findForListView(params: YkiArvioijaParams): List<YkiArvioijaListRow>
    fun count(params: YkiArvioijaParams): Long

    // Solki-outbox
    fun findLahetettavat(maxYritykset: Int?): List<YkiArvioijaEntity>
    fun merkitseLahetetyksi(id: Int)
    fun merkitseLahetysvirhe(id: Int, virhe: String)

    // Automaattipassivointi
    @Transactional fun passivoiPaattyneet(today: LocalDate): List<Int>
}
```

Keskeiset SQL:t:

```sql
-- tallenna(): arviointioikeuksien TÄYSI korvaus. Nykyinen upsert ei poista koskaan mitään,
-- mikä masterina on virhe: peruttu kielioikeus jäisi roikkumaan ja lähtisi Solkiin.
DELETE FROM yki_arviointioikeus
WHERE arvioija_id = :id AND kieli <> ALL (:kielet::yki_tutkintokieli[]);

-- tallenna(): kausihistorian kirjaus (vain aidosti muuttuneet)
INSERT INTO yki_arvioija_kausi
    (arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva, jatkorekisterointi, kirjaaja_oid)
VALUES (:id, :kieli, :tasot, :tila, :alku, :loppu, :jatko, :tekija)
ON CONFLICT ON CONSTRAINT yki_arvioija_kausi_unique DO NOTHING;

-- findLahetettavat(3)    -> nopeat uusinnat
-- findLahetettavat(null) -> yöllinen kaikkien läpikäynti
SELECT * FROM yki_arvioija
WHERE (solkiin_lahetetty IS NULL OR solkiin_lahetetty < muokattu)
  AND yksilointi_kesken = FALSE   -- vaillinaista tunnistetta ei lähetetä Solkiin
  -- AND solki_lahetysyritykset < :maxYritykset   (vain kun maxYritykset != null)
ORDER BY muokattu;

-- passivoiPaattyneet(): passivoi päättyneet oikeudet ja likaa outboxin.
-- passivoitu-hetkeksi merkitaan KAUDEN PAATTYMISPAIVA, ei now(): muuten kayttoonoton
-- yhteydessa passivoituvien vanhojen rivien sailytysaika alkaisi vasta kayttoonotosta.
WITH paattyneet AS (
    UPDATE yki_arviointioikeus SET tila = 'PASSIVOITU'
    WHERE tila = 'AKTIIVINEN'
      AND kauden_paattymispaiva IS NOT NULL
      AND kauden_paattymispaiva < :today
    RETURNING arvioija_id
)
UPDATE yki_arvioija a
SET muokattu = now(), muokkaaja_oid = NULL,
    passivoitu = COALESCE(
        a.passivoitu,
        (SELECT max(o.kauden_paattymispaiva)::timestamptz
         FROM yki_arviointioikeus o WHERE o.arvioija_id = a.id)
    ),
    solkiin_lahetetty = NULL, solki_lahetysvirhe = NULL, solki_lahetysyritykset = 0
WHERE a.id IN (SELECT DISTINCT arvioija_id FROM paattyneet)
RETURNING a.id;
```

Päättymispäivä on **inklusiivinen**: arvioija on aktiivinen vielä päättymispäivänä ja passivoituu
seuraavana yönä. Passivointi kirjaa myös uuden rivin `yki_arvioija_kausi`-tauluun.

Litteä listaprojektio (vrt. `VktSuoritusFlat`) `YkiArvioijaListRow` rakennetaan **kahdella kyselyllä**
(arvioijat sivullisittain + arviointioikeudet `WHERE arvioija_id IN (…)`) ja zipataan repositoryssa —
luettavampi kuin `json_agg`, ja taulu on satojen rivien kokoluokkaa.

---

## 3. Käyttöliittymä

### 3.1 Uudelleenkäytettävä lomakevirhekehys (uusi — sellaista ei ole)

`server/src/main/kotlin/fi/oph/kitu/html/FormErrors.kt`:

```kotlin
/** Kenttäpolkuun sidotut validointivirheet lomakerenderöintiä varten. */
class FormErrors private constructor(private val byPath: Map<String, List<String>>) {
    operator fun get(name: String): List<String> = byPath[name].orEmpty()
    val yleiset: List<String> get() = this[""]
    fun isEmpty() = byPath.isEmpty()

    companion object {
        val EMPTY = FormErrors(emptyMap())
        fun of(errors: Iterable<ValidationError>) =
            FormErrors(errors.groupBy({ it.path.joinToString(".") }, { it.message }))
    }
}

/** Pico.css-yhteensopiva kenttäkääre: label, syöte, aria-invalid ja virheteksti. */
fun FlowContent.formField(
    label: LocalizedString,
    name: String,
    errors: FormErrors = FormErrors.EMPTY,
    testId: String? = null,
    input: FlowContent.(invalid: Boolean) -> Unit,
)

fun FlowContent.formErrorSummary(errors: FormErrors)   // errorMessage(...) yleisistä virheistä
```

Ei uutta CSS:ää eikä JS:ää — Pico tyylittää `aria-invalid`in valmiiksi, ja `error-text`-luokka on jo
`style.css`:ssä (`ViewMessageType.ERROR`).

**Round-trip:** POST-käsittelijä palauttaa aina `ResponseEntity<String>`:

- virhe → `ResponseEntity.ok(sivu.render(form, FormErrors.of(virheet)))` — HTTP 200, syötteet säilyvät,
  ei session-tilaa
- onnistuminen → `303 See Other` + `Location: Links.Yki.arvioija(id)` + `ViewMessage`-flash

Yksi paluutyyppi, ei `RedirectView`/`@ResponseBody`-sekamelskaa.

### 3.2 Reitit — uusi `yki/arvioijat/YkiArvioijaViewController.kt`

`arvioijatView` ja `arvioijatVirheetView` **siirretään pois** paisuvasta `YkiViewController`:sta.

| Metodi | Polku                                | Tarkoitus                                   |
| ------ | ------------------------------------ | ------------------------------------------- |
| GET    | `/yki/arvioijat`                     | lista (suodatus, järjestys, sivutus)        |
| GET    | `/yki/arvioijat/uusi`                | lisäyslomake, vaihe 1 (hetu + nimet)        |
| POST   | `/yki/arvioijat/uusi/haku`           | ONR-haku → vaihe 2 esitäytettynä (200 HTML) |
| POST   | `/yki/arvioijat/uusi`                | luonti → 303 `/yki/arvioijat/{id}`          |
| GET    | `/yki/arvioijat/{id}`                | tiedot, kausihistoria + muokkauslomake      |
| POST   | `/yki/arvioijat/{id}`                | tallennus → 303 self                        |
| POST   | `/yki/arvioijat/{id}/passivoi`       | manuaalinen passivointi → 303 self          |
| POST   | `/yki/arvioijat/{id}/laheta-solkiin` | manuaalinen uudelleenlähetys → 303 takaisin |
| GET    | `/yki/arvioijat/virheet`             | Solki-lähetysvirheiden näkymä               |

`webmvc/Links.kt`-lisäykset: `arvioija(id)`, `uusiArvioija()`, `arvioijaHaku()`, `passivoiArvioija(id)`,
`lahetaArvioijaSolkiin(id)`, `arvioijatCsv()`. HATEOAS-sääntö: `linkTo(methodOn(C::class.java).method(...))`,
lambdan **paluuarvo** ratkaisee (`(C) -> Any`, ei `C.() -> Unit`).

### 3.3 Listanäkymä (`YkiArvioijaPage.kt` uusiksi)

`YkiArvioijaColumn` annotoidaan `@ColumnTags(...)` (nyt annotaatioita ei ole lainkaan, joten enum on
näkymätön tagipohjaiselle `DisplayTableColumn.of`:lle) ja siirtyy tyypille `YkiArvioijaListRow`:

| Sarake                                                                  | Tagit                                   |
| ----------------------------------------------------------------------- | --------------------------------------- |
| `Toiminto` (linkki "Näytä")                                             | `LIST_VIEW`                             |
| `Oppijanumero`, `Sukunimi`, `Etunimet`                                  | `LIST_VIEW, CSV_EXPORT, PERSONAL_DATA`  |
| `Sahkoposti`                                                            | `CSV_EXPORT, PERSONAL_DATA`             |
| `Osoite`                                                                | `LIST_VIEW, CSV_EXPORT, PERSONAL_DATA`  |
| `Kieli`, `Tasot`                                                        | `LIST_VIEW, CSV_EXPORT`                 |
| `Tila`, `KaudenAlkupaiva`, `KaudenPaattymispaiva`, `Jatkorekisterointi` | `LIST_VIEW, CSV_EXPORT`                 |
| `EnsimmainenRekisterointipaiva`, `Muokattu`, `AshaNumero`               | `CSV_EXPORT`                            |
| `SolkiTila` (lähetetty / odottaa / virhe)                               | `LIST_VIEW, CSV_EXPORT`                 |
| `Henkilotunnus`                                                         | **poistuu** — hetuja ei enää tallenneta |

- Korjataan `Katuosoite.getValue` (koko osoite) ja sivun renderöinti (pelkkä katuosoite) vastaamaan
  toisiaan — ero merkitsee heti kun CSV-vienti otetaan käyttöön.
- Uusi `YkiArvioijaParams.kt` (mallina `YkiSuorituksetParams`, `@ModelAttribute`-sidottu):
  `search`, `tila`, `kieli`, `taso`, `kausiPaattyyEnnen`, `vainSolkiVirheet`, `piilotaHenkilotiedot`,
  `sortColumn`, `sortDirection`, `pageNumber`, `pageSize` + `toFilter()`, `toOrder()`, `excludeTags()`,
  `csvFileName()`, `filterDescriptions()` (`jdbc/SqlFilterBuilder`, `jdbc/PaginatedSortOrder`).
- `YkiArvioijaColumn` rekisteröidään `webmvc/EnumFromUrlParamsParsingConfig.kt`:iin — muuten
  `?sortColumn=` ei sidostu.
- Suodatindialogi `html/table/TableFilterDialog.kt`:n `tableFilterDialog` + `enumFilter`/`dateFilter`/
  `toggleFilter`.
- **Sivutus tehdään arvioijittain**, ei arviointioikeusriveittäin: `rowspan`-renderöinti (henkilösarakkeet
  ulottuvat useamman kielirivin yli) rikkoutuisi muuten. `LIMIT/OFFSET` kohdistuu `yki_arvioija`-tasolle.
- Yläreunaan `csvDownloadButton(Links.Yki.arvioijatCsv() + httpParams(params.toMap()))` ja — vain
  kirjoitusoikeudella — `a(href = Links.Yki.uusiArvioija()) { role = "button"; +UiText.Yki.lisaaArvioija }`.

### 3.4 Lisäys- ja muokkauslomake

**Vaihe 1 (`GET /yki/arvioijat/uusi`)** — pieni kortti: `henkilotunnus`, `sukunimi`, `etunimet`,
`kutsumanimi`, sekä vaihtoehtoinen `oppijanumero`-kenttä ohituspoluksi. Nappi "Hae henkilön tiedot"
(`formPost(Links.Yki.arvioijaHaku())`).

**Vaihe 2** — sama sivufunktio, `esitaytto != null`: koko lomake esitäytettynä ONR:n tiedoilla.
`arvioijaOid` piilokenttänä; **hetu ei kulje tallennuspyynnössä lainkaan**.

Puhdas palvelimen round-trip kahdella lomakkeella — ei fetchiä, ei uutta JS-buildia.

**Lomakedata** (yksi koodattu monivalinta rinnakkaislistojen sijaan):

```kotlin
data class ArvioijaFormData(
    val arvioijaOid: String? = null,
    val sukunimi: String? = null,
    val etunimet: String? = null,
    val sahkopostiosoite: String? = null,
    val katuosoite: String? = null,
    val postinumero: String? = null,
    val postitoimipaikka: String? = null,
    val kaudenAlkupaiva: LocalDate? = null,
    val jatkorekisterointi: Boolean = false,
    val tila: YkiArvioijaTila = YkiArvioijaTila.AKTIIVINEN,
    val ashaNumero: String? = null,
    /** Yksi monivalinta, arvot muotoa "FIN:PT" — kieli×taso-matriisin valintaruudut. */
    val arviointioikeus: List<String>? = null,
) {
    fun laskettuPaattymispaiva(): LocalDate? = kaudenAlkupaiva?.let(Rekisterikausi::paattymispaiva)
    fun arviointioikeudet(): List<TallennaArvioija.Arviointioikeus>
    fun toCommand(oid: Oid): TallennaArvioija
}
```

**Arviointioikeusmatriisi** (`FlowContent.arviointioikeusMatriisi(valitut, errors)`): taulukko, rivit =
ei-legacy `Tutkintokieli`t (`@HideInTableFilter` suodattaa `SWE10`/`ENG11`/`ENG12` pois — sama annotaatio
kuin `enumFilter`issa), sarakkeet = `PT`/`KT`/`YT`, solut
`input type=checkbox name="arviointioikeus" value="FIN:PT"`. Nolla JS:ää, yksi kenttänimi, ja
"sallitaan useita" toteutuu kirjaimellisesti. Jo tallennetut legacy-kielet näytetään read-only-rivinä.

**Kauden päättymispäivä** on read-only (`disabled` input + selite "Järjestelmä laskee 5 vuotta
alkupäivästä"). Lisätään `html/Common.kt`:n `javascript(...)`-tyylinen ~5 rivin inline-snippet, joka
päivittää esikatseluarvon `change`-tapahtumassa — sama kuvio kuin `Forms.kt`:n `submitButton`.

### 3.5 Tietosivu (`GET /yki/arvioijat/{id}`)

`Page.renderHtml`: `h1 { kokoNimi }`, `viewMessage(flash)`, sitten

- **Henkilötiedot** — `infoTable` (oppijanumero ONR-linkkinä, nimi, yhteystiedot).
  **Turvakielto** (OPH kys. 5): jos ONR palauttaa `turvakielto = true`, sivun yläreunassa näytetään
  `warningMessage(...)` virkailijalle. Tietoihin ei kohdisteta rajoituksia — osoite näkyy, viedään
  CSV:hen ja lähetetään Solkille normaalisti. Turvakieltoa **ei tallenneta kituun**, vaan se luetaan
  ONR:stä näyttöhetkellä.
  Jos `yksilointi_kesken = true`, näytetään lisäksi varoitus "odottaa yksilöintiä".
- **Rekisterimerkintä** — §3.4:n muokkauslomake ilman vaihetta 1, sisältäen **hallintopäätöksen
  ASHA-numeron** vapaana tekstikenttänä (OPH kys. 12) ja **jatkokausi**-valintaruudun, jonka arvon
  järjestelmä esitäyttää (§2.8)
- **Rekisteröintikaudet** — `displayTable` `yki_arvioija_kausi`-riveistä (kieli, tasot, tila, alku, loppu,
  jatkokausi, kirjattu, kirjaaja) laskevassa `kirjattu`-järjestyksessä
- **Integraatiot** — Solki-lähetyksen tila (`solkiinLahetetty` / `solkiLahetysvirhe` /
  `solkiLahetysyritykset`) + nappi "Lähetä uudelleen Solkiin"
- **Toiminnot** — "Merkitse passiiviseksi" `<dialog>`-varmistuksella (`html/Modal.kt`,
  `modalCommandButton` — natiivi `command`/`commandfor`, ei JS:ää)

Kirjoitusnapit renderöidään vain jos `CurrentUser.hasAuthority(Authority.YKI_ARVIOIJAREKISTERI)`.

### 3.6 Virhenäkymä (`GET /yki/arvioijat/virheet`)

Ei uutta taulua eikä `errorTablePage`-kutsua: sama `YkiArvioijaColumn`-taulukko suodattimella
`vainSolkiVirheet = true`, lisättynä sarakkeilla `SolkiVirhe` ja `SolkiLahetysyritykset` sekä rivikohtaisella
"Lähetä uudelleen" -napilla. Näin virhe ja rivi eivät voi ajautua epäsynkroniin.
`webmvc/DashboardService.kt`:n `arvioijaImportErrorCount` → `arvioijaSolkiVirheCount`, ja
`webmvc/HomePage.kt`:n rivin teksti vastaavasti.

### 3.7 Navigaatio ja käännökset

`html/Navigation.kt`: "Arvioijat" osoittaa jo `Links.Yki.arvioijat()`:iin — ei muutosta. Muista että
`mainNavigation` **on pysyttävä laskettuna `val … get()`**:nä (Tolgee-jäätymisansa).

`i18n/UiText.kt` → `object Yki` uudet avaimet (`tr("...", fi = "...")`):
`lisaaArvioija`, `muokkaaArvioijaa`, `arvioijanTiedot`, `rekisterimerkinta`, `rekisterointikausi`,
`rekisterointikaudet`, `kausiLasketaanAutomaattisesti`, `haeHenkilonTiedot`, `arvioitavatTutkinnot`,
`merkitsePassiiviseksi`, `passivointiVarmistus`, `arvioijaTallennettu`, `arvioijaPassivoitu`,
`solkiLahetysOnnistui`, `solkiLahetysEpaonnistui`, `lahetaUudelleenSolkiin`, `solkiLahetystenVirheet`,
`odottaaLahetysta`, `henkiloaEiYksiloity`.
`object Sarake` uudet: `ensimmainenRekisterointipaiva`, `ashaNumero`, `solkiTila`, `solkiVirhe`,
`lahetysyritykset`, `muokattu`, `kirjattu`.
Poistuu: `arvioijienTuonninVirheet`.

Muista `import fi.oph.kitu.i18n.unaryPlus` jokaiseen uuteen renderöintitiedostoon.

---

## 4. Sisäinen rajapinta

### 4.1 Uusi `yki/arvioijat/YkiArvioijaApiController.kt`

```kotlin
@RestController
@RequestMapping("/yki/api")
@Tag(name = "Yleinen kielitutkinto")
class YkiArvioijaApiController(private val service: YkiArvioijaService) {

    /** Virkailijan CSV-lataus. */
    @GetMapping("/arvioijat", produces = ["text/csv"])
    fun getArvioijatAsCsv(@ModelAttribute params: YkiArvioijaParams): ResponseEntity<StreamingResponseBody> =
        csvAttachmentResponse<YkiArvioijaColumn, _>(
            filename = params.csvFileName(),
            data = service.haeKaikki(params),
            excludeTags = params.excludeTags(),
        )

    /** Solkin täsmäytysluku. */
    @GetMapping("/arvioijat", produces = ["application/json"])
    @Tag(name = "oauth2")
    fun getArvioijat(
        @RequestParam tila: YkiArvioijaTila? = null,
        @RequestParam muuttunutJalkeen: OffsetDateTime? = null,
        @RequestParam(defaultValue = "0") sivu: Int,
        @RequestParam(defaultValue = "200") koko: Int,
    ): ResponseEntity<ArvioijatResponse>
}
```

JSON-vastaus on **sama dokumentti kuin push-payload** (§5.1), jolloin Solki voi käyttää samaa
deserialisointia sekä pushille että täsmäytyspullille. Endpointin on oltava `…/api/…`-polun alla, jotta
se näkyy `springdoc.pathsToMatch=/**/api/**/*` -suodattimen läpi `/api-docs`issa.

### 4.2 Sisääntuleva `POST /yki/api/arvioija` — kavennetaan, ei poisteta

> **Muutos aiempaan päätökseen.** Suunnitelmassa oli tarkoitus poistaa sisääntuleva rajapinta kokonaan.
> Tavoitetilakuvaus kuitenkin edellyttää **Solki→kitu-muutosrajapintaa** yhdessä tapauksessa:
> arvioijan omasta toiveesta tapahtuva kesken kauden passivointi tehdään Solkin YKI-sovelluksessa, ja
> _"tieto siirretään muutosrajapintaa pitkin kielitutkintorekisteriin"_. Kolmesta passivointitavasta siis
> kaksi kulkee kitusta Solkiin (automaattinen kauden päättyminen, laiminlyönnin takia tehty passivointi)
> ja **yksi Solkista kituun**.

Tavoiterakenne:

| Suunta       | Tapaus                                                 | Rajapinta                                       |
| ------------ | ------------------------------------------------------ | ----------------------------------------------- |
| kitu → Solki | Uusi merkintä, muokkaus, uusi kausi                    | §5.1 `PUT /arvioijat/{oppijanumero}`            |
| kitu → Solki | Automaattinen passivointi kauden päätyttyä             | sama                                            |
| kitu → Solki | Passivointi laiminlyönnin takia (OPH kuulee arvioijaa) | sama                                            |
| Solki → kitu | **Passivointi arvioijan omasta toiveesta**             | kavennettu `POST /yki/api/arvioija/passivointi` |

Suositus: **ei säilytetä nykyistä koko rekisterimerkinnän kirjoittavaa `POST /yki/api/arvioija`-rajapintaa**,
vaan korvataan se kapealla passivointi-endpointilla, joka ottaa vastaan vain oppijanumeron, passivoinnin
syyn ja päivämäärän. Näin master-vastuu ei vuoda takaisin Solkille: Solki ei voi kirjoittaa nimiä,
yhteystietoja eikä kausia, vain passivoida. Kavennettu rajapinta ei myöskään saa laukaista kitun omaa
`PUT`-lähetystä takaisin Solkiin (kaikuvaara) — passivointi merkitään `solkiin_lahetetty`-kenttään heti
lähetetyksi.

Vanhan endpointin osalta edetään kaksivaiheisesti, koska JYU saattaa jo kutsua sitä:

**Vaihe A (tässä työssä):** mapping jää, mutta palauttaa `410 Gone` selittävällä
`TiedonsiirtoFailure`-rungolla ja `@Deprecated`-merkinnällä. Security-sääntö säilyy, jotta oikeutettu
kutsuja saa 410:n eikä 403:a. Samalla poistuvat: `YkiArvioija`/`YkiArviointioikeus`-DTO:t,
`ValidationService`in DTO-ylikuormitus, `SchemaExamplesController.ykiArvioija()` +
`badRequestArvioijaResponse()`, `server/src/test/resources/yki-arvioija-example.json`,
`YkiApiControllerTest`in neljä arvioija-testiä. e2e: `securityconfig.spec.ts`:n odotusarvot `400 → 410`.

**Vaihe B (myöhempi PR, JYU vahvistanut):** mapping, security-sääntö ja e2e-rivi poistetaan kokonaan.

---

## 5. Solki-integraatio (lähtevä)

Uusi paketti `server/src/main/kotlin/fi/oph/kitu/yki/arvioijat/solki/`.

### 5.1 EHDOTETTU REST-sopimus (sovittava Jyväskylän kanssa)

```
PUT  {base}/arvioijat/{oppijanumero}
Authorization:   Basic <Solkin kitulle myöntämät tunnukset>
Content-Type:    application/json; charset=utf-8
Idempotency-Key: {oppijanumero}:{versio}
```

```json
{
  "oppijanumero": "1.2.246.562.24.59267607404",
  "versio": "2026-08-21T09:12:33.512Z",
  "sukunimi": "Kivinen-Testi",
  "etunimet": "Petro Testi",
  "sahkopostiosoite": "petro.kivinen@example.com",
  "osoite": {
    "katuosoite": "Testikatu 1 A 2",
    "postinumero": "00100",
    "postitoimipaikka": "Helsinki"
  },
  "arviointioikeudet": [
    {
      "kieli": "fin",
      "tasot": ["PT", "KT", "YT"],
      "tila": "AKTIIVINEN",
      "kaudenAlkupaiva": "2026-01-01",
      "kaudenPaattymispaiva": "2031-01-01",
      "jatkorekisterointi": false,
      "ensimmainenRekisterointipaiva": "2005-01-21"
    }
  ]
}
```

| Päätös                                                                  | Perustelu                                                                                                                                                         |
| ----------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **PUT + koko dokumentti, avaimena oppijanumero**                        | Uudelleenyritys on triviaalisti idempotentti; arviointioikeuden poisto ilmaistavissa (kieli katoaa taulukosta); ei erillisiä create/update/delete-verbejä         |
| **Kausi ja tila arviointioikeuskohtaisina**                             | Vastaa kitun tietomallia (sovittu päätös) ja poistuneen CSV:n riviesitystä                                                                                        |
| **Ei henkilötunnusta**                                                  | 1.1.2026 lainmuutos — Solkin on avaimennettava oppijanumerolla                                                                                                    |
| **Ei puhelinnumeroa**                                                   | OPH vahvisti (kys. 11), ettei puhelinnumeroa säilytetä kitussa lainkaan — se on kokonaan Solkin omaa tietoa                                                       |
| **Ei ASHA-numeroa**                                                     | Hallintopäätöksen viite tallennetaan kituun (kys. 12) mutta se on OPH:n sisäinen hallinnollinen tieto; varmistetaan JYU:lta, onko sille Solkissa käyttöä          |
| `kieli` = `Tutkintokieli.solkiCode` (`"fin"`), `tasot` = `PT`/`KT`/`YT` | Sama lankamuoto kuin poistuneessa CSV:ssä → ei muunnostyötä JYU:n päässä                                                                                          |
| `versio` = kitun `muokattu`                                             | Solki voi hylätä vanhemman version, jolloin epäjärjestyksessä saapuva uusinta ei palauta vanhaa tilaa                                                             |
| **Ei DELETE-operaatiota**                                               | Rekisteristä ei poisteta, vain passivoidaan (UC3)                                                                                                                 |
| **PUT korvaa vain OPH:n omistamat kentät**                              | Solki täydentää merkintöjä omilla tiedoillaan (arviointikerrat, huomautukset, lisätiedot, liitteet, puhelinnumerot, postinumero) — lähetys ei saa tyhjentää niitä |

| Status                    | Merkitys                | Kitu tekee                                                    |
| ------------------------- | ----------------------- | ------------------------------------------------------------- |
| `200` / `204`             | Hyväksytty              | `merkitseLahetetyksi`                                         |
| `409 Conflict`            | Solkilla uudempi versio | käsitellään onnistumisena                                     |
| `400` + `{"virheet":[…]}` | Pysyvä virhe            | `merkitseLahetysvirhe`; ei uusintaa ennen kuin rivi muokataan |
| `401` / `403`             | Konfiguraatio-ongelma   | virhe, uusinta aikataulun mukaan                              |
| `5xx` / yhteysvirhe       | Ohimenevä               | virhe + yrityslaskuri, uusinta                                |

Dokumentoidaan `docs/technical/integraatiot.md`:hen omana lukunaan (Solki-luku muuttuu kaksisuuntaiseksi).

### 5.2 Luokat (malli 1:1 `ilmoittautumisjarjestelma/`-paketista)

| Tiedosto                         | Sisältö                                                                                                                                                            |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `SolkiArvioijaRequest.kt`        | Payload-dataluokat + `fun of(entity): SolkiArvioijaRequest`                                                                                                        |
| `SolkiArvioijaException.kt`      | Sealed: `BadRequest`, `Unauthorized`, `Conflict`, `UnexpectedError`, `NullResponse`, `MalformedResponse` + `debugString()`                                         |
| `SolkiArvioijaClient.kt`         | Interface + `@ConditionalOnProperty("kitu.yki.arvioijat.solki.enabled", havingValue="true")` -impl; `retrieveEntitySafely(String::class.java)`; palauttaa `Either` |
| `SolkiArvioijaService.kt`        | Interface + `@ConditionalOnBean`-impl + `@ConditionalOnMissingBean`-mock (pelkkä loki)                                                                             |
| `SolkiArvioijaScheduledTasks.kt` | Kaksi `tracer.recurringTask(...)`-beania (§5.3)                                                                                                                    |

**`debugString()` ei saa serialisoida pyyntörunkoa** (poikkeama KIOS-mallista): osoite ja sähköposti ovat
henkilötietoa, joka päätyisi lokeihin ja virhesarakkeeseen. Mukaan vain oppijanumero, statuskoodi ja
vastausrunko.

**`SolkiRestClientConfig` on täydennettävä `.withLenientStringConverter()`-kutsulla** — ilman sitä
`retrieveEntitySafely(String::class.java)` kaatuu Jacksonin sisällä, koska Spring 7:n oletusarvoinen
`StringHttpMessageConverter` mainostaa vain `text/*` (CLAUDE.md). Uusi kytkinproperty
`kitu.yki.arvioijat.solki.enabled` erikseen, koska `kitu.yki.baseUrl` on asetettu joka ympäristössä
(myös local/e2e dev-stubiin).

### 5.3 Uudelleenyritys: "3 kertaa, sitten yöllisesti"

> **Tärkeä havainto:** `@RetryOutboundIntegration` uusii vain `ResourceAccessException`in ja
> `HttpServerErrorException`in. `retrieveEntitySafely` **ei heitä** kumpaakaan 5xx:llä vaan palauttaa
> `Either.Left` — eli KIOS-palvelun `@RetryOutboundIntegration` ei tosiasiassa uusi HTTP 500:aa.
> **Älä kopioi tätä.** (Kannattaa harkita erillistä korjausta myös `IlmoittautumisjarjestelmaService`en.)

Käytetään eksplisiittistä, kantaan tallennettua yrityslaskuria (`solki_lahetysyritykset`), joka näkyy myös
virhenäkymässä:

| Kerros                                 | Toteutus                                                                          |
| -------------------------------------- | --------------------------------------------------------------------------------- |
| Välitön palaute virkailijalle          | 1 synkroninen yritys tallennuksen jälkeen                                         |
| "3 kertaa"                             | `FIXED_DELAY\|900s` -ajastus, poimii rivit `solki_lahetysyritykset < 3`           |
| "sen jälkeen säännöllisesti (joka yö)" | `DAILY\|02:15` -ajastus, poimii **kaikki** lähettämättömät riippumatta laskurista |

```properties
kitu.yki.arvioijat.solki.enabled=false
kitu.yki.arvioijat.solki.url=${SOLKI_ARVIOIJA_API_URL:}
kitu.yki.arvioijat.solki.username=${SOLKI_ARVIOIJA_API_USER:}
kitu.yki.arvioijat.solki.password=${SOLKI_ARVIOIJA_API_PASSWORD:}
kitu.yki.scheduling.lahetaArvioijatSolkiin.schedule=FIXED_DELAY|900s
kitu.yki.scheduling.lahetaEpaonnistuneetArvioijatSolkiin.schedule=DAILY|02:15
kitu.yki.scheduling.passivoiPaattyneetArvioijat.schedule=DAILY|01:15
```

untuva/qa/prod: `enabled=true` (JYU:n valmistuttua), URLit ympäristökohtaisiin propertytiedostoihin,
salaisuudet AWS Secrets Manageriin (`scripts/ensure_aws_secrets.sh` + README:n salaisuuslista päivitettävä).
local: `enabled=true`, ajastukset `-`.
**e2e: ajastukset `0 0 0 29 2 ?`** (karkausvuoden 29.2.) eikä `-` — CLAUDE.md:n mukaan `-` tekee tehtävästä
kokonaan näkymättömän db-scheduler-UI:ssa, jolloin sitä ei voi laukaista testistä käsin.

Paikallinen Solki-stubi `dev/YkiController.kt`:iin (kuollut `GET /dev/yki/import/arvioijat` poistetaan
tieltä): `PUT /dev/yki/import/arvioijat/{oppijanumero}`, joka palauttaa `204` tai `?failWith=500`-parametrilla
halutun virheen — e2e-testit ohjaavat sillä virhepolkua.

---

## 6. Ajastetut tehtävät

### 6.1 Automaattinen passivointi (UC3)

`yki/arvioijat/YkiArvioijaScheduledTasks.kt`:

```kotlin
@Configuration
class YkiArvioijaScheduledTasks(private val tracer: Tracer) {
    @Value($$"${kitu.yki.scheduling.passivoiPaattyneetArvioijat.schedule}")
    lateinit var passivointiSchedule: String

    @WithSpan @Bean
    fun passivoiPaattyneetArvioijat(service: YkiArvioijaService): Task<Void> =
        tracer.recurringTask("Passivoi päättyneet YKI-arvioijarekisterimerkinnät", passivointiSchedule) {
            service.passivoiPaattyneetKaudet()
        }
}
```

`passivoiPaattyneetKaudet()`:

1. `repository.passivoiPaattyneet(timeService.today())` → `List<Int>` (UPDATE nollaa outbox-kentät →
   uudelleenlähetys pakotettu, §2.6) + historiarivit `yki_arvioija_kausi`-tauluun.
2. `auditLogger.logAllInternalOnly("Yki arvioija passivoitu automaattisesti", passivoidut) { … }` —
   **ei** `auditLogger.log(...)`, joka vaatii `AuditContext`in (CasUserDetails + HTTP-pyyntö) eikä siis
   toimi ajastetussa tehtävässä.
3. `solki.lahetaLahettamattomat(maxYritykset = 3)` — sama kuvio kuin
   `YkiViewController.hyvaksyTarkistusArvioinnit`, joka kutsuu KIOS-lähetystä heti kirjoituksen jälkeen.

Ajastukset porrastettu: passivointi `01:15`, yöllinen lähetys `02:15`.
`ExtendedSchedules.parse` hyväksyy `DAILY|HH:mm`; jos halutaan cron, se on **6-kenttäinen** SPRID53-cron
ilman vuosikenttää.

### 6.2 Säilytysajan valvonta (5 vuotta)

Prosessikuvauksen tietotaulukko määrittelee **säilytysajaksi kielitutkintorekisterissä 5 vuotta**
kaikille kitussa säilytettäville arvioijakentille. Käyttötapauskuvauksessa tätä ei mainittu, eikä
nykyisessä toteutuksessa ole minkäänlaista arvioijatietojen poistoa — **tämä on uusi vaatimus.**

OPH on vahvistanut, että **5 vuotta lasketaan passivointihetkestä** (ei kauden päättymispäivästä).
Tämä on syy `passivoitu`-aikaleimalle (§1.1): pelkkä kauden päättymispäivä ei riitä, koska kesken kauden
passivoidun merkinnän säilytysaika alkaa aiemmin kuin kausi olisi päättynyt.

Poisto on peruuttamaton, joten tehtävä pidetään **oletuksena pois päältä** ja otetaan käyttöön vasta kun
sen toiminta on todennettu untuvassa.

Toteutus samaan `YkiArvioijaScheduledTasks`-luokkaan:

```kotlin
@Value($$"${kitu.yki.scheduling.poistaVanhentuneetArvioijat.schedule}")
lateinit var sailytysaikaSchedule: String

@WithSpan @Bean
fun poistaVanhentuneetArvioijat(service: YkiArvioijaService): Task<Void> =
    tracer.recurringTask("Poista säilytysajan ylittäneet YKI-arvioijamerkinnät", sailytysaikaSchedule) {
        service.poistaSailytysajanYlittaneet()
    }
```

Vanhoilla Solki-peräisillä riveillä ei ole passivointihetkeä. Se **täytetään kauden päättymispäivästä**
kahdessa kohdassa, jotta säilytysaika kuluu historiallisella aikajanalla eikä ala vasta käyttöönotosta:

1. **Migraatiossa** (§1.1) jo passivoiduille riveille, joilla on kauden päättymispäivä.
2. **Automaattipassivoinnissa** (§6.1) niille legacy-riveille, jotka ovat vielä `AKTIIVINEN` mutta joiden
   kausi on jo mennyt — ne passivoituvat ensimmäisellä ajolla ja saavat passivointihetkekseen kauden
   päättymispäivän, eivät `now()`:ta.

Manuaalinen passivointi kesken kauden merkitsee `passivoitu = now()`, koska silloin arviointioikeus
tosiasiassa päättyy sillä hetkellä. Näin `passivoitu` vastaa aina hetkeä, jolloin henkilö lakkasi olemasta
arvioija — riippumatta siitä, päättyikö kausi luonnollisesti vai kesken.

```sql
-- Poistaa arvioijan, jonka passivoinnista on kulunut yli 5 vuotta.
-- yki_arviointioikeus ja yki_arvioija_kausi poistuvat ON DELETE CASCADE -säännöllä.
DELETE FROM yki_arvioija a
WHERE a.passivoitu IS NOT NULL
  AND a.passivoitu < now() - interval '5 years'
  AND NOT EXISTS (
        SELECT 1 FROM yki_arviointioikeus o
        WHERE o.arvioija_id = a.id AND o.tila = 'AKTIIVINEN'
      )
RETURNING id, arvioija_oid;
```

Huomioita:

- **Aktiivinen arviointioikeus suojaa poistolta.** `NOT EXISTS`-ehto varmistaa, ettei arvioijaa poisteta
  niin kauan kuin yhdelläkään kielellä on `AKTIIVINEN`-oikeus, vaikka jokin kausi olisi vanhentunut.
- **`passivoitu IS NULL` suojaa poistolta.** Rivi, jolta kauden päättymispäivä puuttuu, ei saa
  passivointihetkeä kummassakaan täyttökohdassa eikä siten koskaan poistu automaattisesti. Vanhin ja
  epäluotettavin data on näin suojassa ilman erillistä ehtoa.
- **Poisto on takautuva.** Koska passivointihetki täytetään historiallisesta päivämäärästä, käyttöönoton
  jälkeen kannassa on heti rivejä, joiden säilytysaika on jo umpeutunut. Tämä on tarkoitus, mutta se
  tarkoittaa myös, että **ensimmäinen ajo poistaa kerralla suuren joukon rivejä** — ajo on tehtävä
  ensin untuvassa ja tarkistettava poistuvien määrä (`SELECT count(*)` samalla ehdolla) ennen
  tuotantoon ottamista.
- Poisto lokitetaan `logAllInternalOnly`lla (ajastetussa tehtävässä ei ole `AuditContext`ia, §8).
- Poisto **ei** lähde Solkille: Solkilla on oma säilytysaikansa ja oma kopionsa rekisteristä.
- Propertyn oletusarvo on **pois päältä** kunnes sääntö on vahvistettu:
  `kitu.yki.scheduling.poistaVanhentuneetArvioijat.schedule=0 0 0 29 2 ?` (karkausvuoden 29.2.,
  eli käytännössä ei koskaan, mutta tehtävä näkyy db-scheduler-UI:ssa ja on ajettavissa käsin).
- Testattava erikseen, ettei poisto koske aktiivisia eikä `passivoitu IS NULL` -rivejä, ja että
  migraation sekä automaattipassivoinnin täyttämä `passivoitu` vastaa kauden päättymispäivää.

### 6.3 Keskeneräisten yksilöintien täydennys

```kotlin
@Value($$"${kitu.yki.scheduling.taydennaYksiloimattomatArvioijat.schedule}")
lateinit var yksilointiSchedule: String

@WithSpan @Bean
fun taydennaYksiloimattomatArvioijat(service: YkiArvioijaService): Task<Void> =
    tracer.recurringTask("Täydennä yksilöimättömät YKI-arvioijat", yksilointiSchedule) {
        service.taydennaYksiloinnit()
    }
```

Hakee rivit joilla `yksilointi_kesken = true`, kutsuu kullekin
`oppijanumeroService.getMasterOid(henkiloOid)` ja onnistuessa päivittää `arvioija_oid`:n, nollaa lipun ja
likaa outboxin. Epäonnistuminen (yksilöinti yhä kesken) on **normaali tila**, ei virhe — ei kirjata
lähetysvirheeksi eikä kasvateta yrityslaskuria, vain lokitetaan.
Ajastus `kitu.yki.scheduling.taydennaYksiloimattomatArvioijat.schedule=DAILY|00:45`, eli ennen
passivointia (01:15) ja Solki-lähetystä (02:15), jotta samana yönä yksilöityneet ehtivät mukaan.

---

## 7. Käyttöoikeudet

### 7.1 `security/Authority.kt`

```kotlin
YKI_ARVIOIJAREKISTERI("YKI_ARVIOIJAREKISTERI_KIRJOITUS"),
```

→ `ROLE_APP_KIELITUTKINTOREKISTERI_YKI_ARVIOIJAREKISTERI_KIRJOITUS`. OPH liittää tämän Otuvassa
käyttöoikeusryhmään **"Kielitutkintorekisteri-oph-pääkäyttäjä"**. Luku jää `VIRKAILIJA`lle (lista on jo
nyt kaikkien virkailijoiden nähtävissä; erillinen lukuoikeus vaatisi turhaan uuden Otuva-määrittelyn).

### 7.2 `security/WebSecurityConfig.kt`

Kirjoitussäännöt **vain CAS-ketjuun** (`casSecurityFilterChain`in `authorizeHttpRequests`-lohkoon),
ei jaettuun `configureCommonAuthorizations`-funktioon — muuten sama sääntö avautuisi myös
OAuth2-bearer-ketjuun, jonka ei ole tarkoitus päästä syöttökäyttöliittymään:

```kotlin
authorize(GET,  "/yki/arvioijat/uusi", hasAuthority(Authority.YKI_ARVIOIJAREKISTERI.role()))
authorize(POST, "/yki/arvioijat/**",   hasAuthority(Authority.YKI_ARVIOIJAREKISTERI.role()))
```

Jaettuun `configureCommonAuthorizations`iin (molemmat ketjut) uusi luku-API:

```kotlin
authorize(GET, "/yki/api/arvioijat/**", hasAnyAuthority(
    *Authority.YKI_ARVIOIJAREKISTERI.authStrings(), *Authority.YKI_TALLENNUS.authStrings()))
```

`GET /yki/arvioijat` ja `/yki/arvioijat/{id}` osuvat CAS-ketjun loppusääntöön `hasAuthority(VIRKAILIJA)`.
CSRF: `/yki/arvioijat/**` **ei** ole `ignoringRequestMatchers`-listalla → lomakkeiden **on** käytettävä
`formPost(...)`ia, joka injektoi tokenin; suora `form { }` epäonnistuu 403:lla.

### 7.3 Uusi `security/CurrentUser.kt`

Plain `object` (**ei** `@Service` — vrt. `i18n/CurrentLanguage`, välttää `@WithSpan`-CGLIB-ansan):

```kotlin
object CurrentUser {
    fun hasAuthority(authority: Authority): Boolean
    fun oid(): Oid?      // CasUserDetails.oid — myös luoja_oid/muokkaaja_oid -arvojen lähde
}
```

### 7.4 `dev/MockLoginController.kt`

`MockUser.DEFAULT` ja `MockUser.ROOT` saavat `Authority.YKI_ARVIOIJAREKISTERI`. Uusi
`MockUser.VIRKAILIJA` (pelkkä `Authority.VIRKAILIJA`) e2e-oikeusmatriisiin todistamaan 200 luvulle /
403 kirjoitukselle.

---

## 8. Auditlokit

`auditlogs/AuditLogEntry.kt`:n `AuditLogOperation` on suljettu enum, jossa on tällä hetkellä vain
`*Viewed`-operaatioita (ja käyttämätön `YkiSuoritusPatched`). Lisätään:

```kotlin
YkiArvioijaViewed("YkiArvioijaViewed"),
YkiArvioijaCreated("YkiArvioijaCreated"),
YkiArvioijaUpdated("YkiArvioijaUpdated"),
YkiArvioijaPassivated("YkiArvioijaPassivated"),
```

| Operaatio               | Kutsupaikka (`YkiArvioijaService`, `target = arvioijaOid`)      |
| ----------------------- | --------------------------------------------------------------- |
| `YkiArvioijaViewed`     | `haeArvioija(id)` (tietosivu)                                   |
| `YkiArvioijaCreated`    | `luoArvioija`, onnistuneen INSERTin jälkeen                     |
| `YkiArvioijaUpdated`    | `paivitaArvioija`, onnistuneen UPDATEn jälkeen                  |
| `YkiArvioijaPassivated` | `passivoiArvioija` (manuaalinen)                                |
| —                       | automaattipassivointi ja listan massaluku: `logAllInternalOnly` |

Tämä on projektin **ensimmäinen** luonti/muokkaus-audit-operaatio.

---

## 9. Testaus

### 9.1 Backend (`server/src/test/kotlin/fi/oph/kitu/yki/arvioijat/`)

Uudet:

- `RekisterikausiTest.kt` — +5 v, karkauspäivä 29.2. → 28.2.
- `YkiArvioijaValidationTest.kt` — pakolliset kentät, postinumeromuoto, duplikaattikieli, tyhjät tasot.
- `ArvioijaFormDataTest.kt` — `"FIN:PT"`-koodauksen purku, viallisten arvojen sieto, päättymispäivä.
- `YkiArvioijaColumnTest.kt` — tagikattavuus, CSV-otsikot, `PERSONAL_DATA`-poissulku
  (mallina `YkiSuoritusColumnTest`).
- `YkiArvioijaServiceTest.kt` — `@SpringBootTest` + Testcontainers + `MockOppijanumeroService`:
  luonti ONR-haulla, yksilöimätön henkilö, päivitys laskee päättymispäivän uusiksi, kielen poisto,
  **kausihistoriarivin synty vain kun kausi muuttuu**, manuaalinen passivointi, `passivoiPaattyneetKaudet`.
- `SolkiArvioijaClientTest.kt` — `MockRestServiceServer`. **Muista laiskan `RestClient`in ansa:**
  `@TestInstance(PER_CLASS)`, yksi `reset()`-palvelin, kaikki client-mock-testit samassa luokassa.
- `SolkiArvioijaServiceTest.kt` — outbox-tilakone: onnistuminen nollaa virheen ja laskurin, 400 on pysyvä,
  500 kasvattaa laskuria, `findLahetettavat(3)` vs `findLahetettavat(null)`.
- `YkiArvioijaViewControllerTest.kt` — MockMvc: 403 ilman oikeutta, 200 sen kanssa, virheellinen POST →
  200 + `aria-invalid`, onnistunut POST → 303.

Muutettavat: `YkiArvioijaRepositoryTest.kt` (uusi `tallenna`-semantiikka, arviointioikeuksien korvaus,
outbox-kentät, kausihistoria, `passivoiPaattyneet`), `YkiApiControllerTest.kt` (arvioija-testit pois,
410-testi + CSV-testi), `webmvc/DashboardServiceTest.kt`. Poistetaan `yki/YkiArvioijaErrorTests.kt`.

### 9.2 E2E (`e2e/`)

- `fixtures/ykiArvioija.ts` — uudet sarakkeet; variantit `insertPaattynytKausi`, `insertSolkiVirheella`,
  `insertLahettamaton`.
- `models/yki/YkiArvioijatPage.ts` (päivitys: suodatin, sivutus, "Lisää arvioija", rivilinkki) sekä uudet
  `YkiArvioijaPage.ts`, `YkiArvioijaLomake.ts`, `YkiArvioijatFilterDialog.ts`.
- `tests/yki/yki-arvioijat.spec.ts` — päivitetyt sarakeodotukset (hetu pois, Solki-tila mukaan),
  suodatus, sivutus, CSV.
- Uusi `tests/yki/yki-arvioija-lisays.spec.ts` — ONR-haku → esitäyttö → tallennus → flash → rivi listalla;
  yksilöimätön henkilö → virhe + ONR-linkki; puuttuva pakollinen kenttä → lomake re-renderöityy
  `aria-invalid`illa eivätkä syötteet häviä; hetu **ei** tallennu kantaan.
- Uusi `tests/yki/yki-arvioija-muokkaus.spec.ts` — uusi kauden alkupäivä laskee päättymispäivän ja
  **synnyttää kausihistoriarivin**, kielen lisäys/poisto, manuaalinen passivointi dialogin kautta.
- Uusi `tests/yki/yki-arvioija-solki.spec.ts` — dev-stubi 500 → virhenäkymässä rivi + syy + laskuri;
  "Lähetä uudelleen" (stubi 204) → virhe katoaa.
- `tests/security/securityconfig.spec.ts` — uudet reitit, `POST /yki/api/arvioija` → 410, uusi
  `MockUser.VIRKAILIJA`-lohko.

---

## 10. Vaiheistus (PR-ehdotus)

Jokainen askel on itsenäisesti julkaisukelpoinen. Askeleet 1–9 eivät riipu Solkin rajapintasopimuksesta,
joten työ ei jää odottamaan Jyväskylää.

| #   | Vaihe                                                    | Koko | Riippuu                 |
| --- | -------------------------------------------------------- | ---- | ----------------------- |
| 1   | Poista kuollut arvioijien virhetuontikoneisto            | S    | —                       |
| 2   | Laajenna arvioijataulut masteriksi (V116–V118)           | L    | 1                       |
| 3   | Uudista arvioijalistanäkymä                              | L    | 2                       |
| 4   | Lomakevirhekehys ja arvioijarekisterin käyttöoikeus      | M    | —                       |
| 5   | Uuden arvioijan tallennus + ONR-haku (UC1)               | L    | 2, 4                    |
| 6   | Keskeneräisen yksilöinnin käsittely                      | M    | 5                       |
| 7   | Muokkaus, kausihistoria ja manuaalinen passivointi (UC2) | L    | 5                       |
| 8   | Automaattinen passivointi (UC3)                          | M    | 7                       |
| 9   | Säilytysajan valvonta (5 v)                              | M    | 8                       |
| 10  | YKI-arvioijien Solki-lähetys                             | L    | 7, sopimus JYU:n kanssa |
| 11  | Kavenna sisääntuleva rajapinta passivointi-endpointiksi  | M    | 10                      |
| 12  | Käyttöönotto ja kytkimet                                 | S    | 10, 11                  |

1. **`Poista kuollut arvioijien virhetuontikoneisto`** — `yki/arvioijat/error/`, `V118` DROP TABLE,
   `dev/YkiController` kuollut stubi, `DashboardService`/`HomePage`/`EnumFromUrlParamsParsingConfig`
   -viittaukset, `YkiArvioijaErrorTests`. (Vanhentuneet CSV-tuontiväitteet `CLAUDE.md`:ssä ja
   `arkkitehtuuri.md`:ssä on jo korjattu tämän suunnitelman PR:ssä.)
2. **`Laajenna arvioijataulut masteriksi`** — `V116` (uudet sarakkeet, outbox, `passivoitu`-backfill),
   `V117` (kausihistoria), entiteetit + `RowMapper`it, `YkiArvioijaRepository.tallenna`
   (arviointioikeuksien korvaus + kausihistorian kirjaus), repository-testit.
3. **`Uudista arvioijalistanäkymä`** — `@ColumnTags`, `YkiArvioijaParams`, sivutus arvioijittain,
   suodatindialogi, CSV-vienti, uusi `YkiArvioijaViewController` + `Links`-lisäykset. e2e-päivitys.
4. **`Lisää lomakevirhekehys ja arvioijarekisterin käyttöoikeus`** — `html/FormErrors.kt`,
   `security/CurrentUser.kt`, `Authority.YKI_ARVIOIJAREKISTERI`, security-säännöt, `MockUser`-päivitykset,
   e2e-oikeusmatriisi.
5. **`Lisää uuden YKI-arvioijan tallennus (UC1)`** — `Rekisterikausi`, `TallennaArvioija`,
   `YkiArvioijaError`, uusi `YkiArvioijaValidation` (sama kausi kaikille kielille), jatkokauden päättely,
   `YkiArvioijaService.luoArvioija`, `OppijanumeroHakuService`-ekstraktio, kaksivaiheinen lomake,
   ASHA-numerokenttä, turvakieltovaroitus, audit-operaatiot, UiText. e2e.
6. **`Käsittele keskeneräinen ONR-yksilöinti`** — `yksilointi_kesken`-tila, §6.3:n ajastettu täydennys,
   Solki-lähetyksen suodatus, uniikkirikkeen käsittely yksilöinnin valmistuessa. e2e.
7. **`Lisää arvioijan muokkaus, kausihistoria ja manuaalinen passivointi (UC2)`** — tietosivu +
   kausihistoriataulukko + POST, `passivoiArvioija`, kielen lisäys perii voimassa olevan kauden. e2e.
8. **`Lisää arvioijien automaattinen passivointi (UC3)`** — `YkiArvioijaScheduledTasks`,
   `passivoitu`-leima kauden päättymispäivästä, ajastukset.
9. **`Lisää säilytysajan valvonta`** — §6.2:n poistotehtävä, oletuksena pois päältä, testit
   suojaehdoille. Ajettava ensin untuvassa ja tarkistettava poistuvien määrä.
10. **`Lisää YKI-arvioijien Solki-lähetys`** — `solki/`-paketti, outbox-kirjoitukset, dev-stubi, propertyt
    (`enabled=false`), virhenäkymä + "Lähetä uudelleen" + dashboard-laskuri,
    `docs/technical/integraatiot.md`. Testit + e2e.
11. **`Kavenna sisääntuleva arvioijarajapinta`** — `POST /yki/api/arvioija` → 410, tilalle kavennettu
    passivointi-endpoint (§4.2), DTO:t + schema-esimerkit + testit pois, e2e-matriisi.
12. _(JYU:n vahvistuksen jälkeen)_ `enabled=true` untuvaan/QA:han/prodiin; 410-mapping kokonaan pois;
    säilytysajan poisto päälle; (tietosuojan luvalla) `henkilotunnus`-sarakkeen poisto.

Muista jokaisen askeleen lopuksi `./scripts/format.sh` (ktlint + prettier). Infraan ei tule muutoksia,
joten `infra/README.md` pysyy koskemattomana.

---

## 11. Avoimet kysymykset

**Jyväskylän yliopisto / Solki**

1. Koko §5.1:n REST-sopimus on ehdotus. Vahvistettava: polku, verbi, autentikointi, virhekoodit,
   `versio`-semantiikka, `Idempotency-Key`, per-ympäristö base-URL ja tunnukset.
2. ~~Voiko Solki avaimentaa pelkällä oppijanumerolla?~~ **Vastattu OPH:n tavoitetilakuvauksessa:**
   _"arvioijarekisterimerkinnän tiedot voidaan lisätä arvioijan tietoihin kielitutkintorekisteristä
   OID-tunnisteella."_ Varmistetaan JYU:lta enää tekninen toteutettavuus ja aikataulu.
3. Autentikointi: HTTP Basic (kanava on jo pystyssä) vai Otuva OAuth2 client credentials? Suositus: Basic v1:ssä.
4. Mitä Solki tekee, kun kieli katoaa payloadista — poistaako oikeuden vai säilyttääkö historian?
5. **Solki→kitu-muutosrajapinta:** tavoitetilassa arvioijan omasta toiveesta tehtävä passivointi kulkee
   Solkista kituun. Riittääkö §4.2:n kavennettu passivointi-endpoint (oppijanumero + syy + päivämäärä), vai
   tarvitseeko Solki välittää muutakin? Kutsuuko Solki jo nykyistä `POST /yki/api/arvioija`-rajapintaa
   (ratkaisee, tarvitaanko 410-välivaihe)?

6. **Onko arvioija aina jo olemassa Solkissa, kun kitu lähettää merkinnän?** Tavoitetilakuvauksen mukaan
   Solki luo arvioijalle käyttäjätunnuksen ja viisinumeroisen arvioijatunnuksen jo koulutuksen yhteydessä,
   joten kitun `PUT` olisi päivitys olemassa olevaan riviin. Mitä Solki tekee, jos OID:ta ei tunneta —
   luodaanko rivi vai palautetaanko virhe?

**Jyväskylän yliopisto / Solki — OPH:n päätöksistä seuranneet uudet kysymykset**

7. **Merkintä voi viivästyä, jos ONR-yksilöinti on kesken.** OPH:n päätöksen (kys. 4) mukaan merkintä saa
   syntyä kituun ennen kuin henkilö on yksilöity ONR:ssä. Tällöin oppijanumeroa ei ole eikä merkintää voi
   lähettää Solkille; kitu täydentää tunnisteen ajastetusti (§6.3) ja lähettää vasta sitten. Hallintopäätöksen
   ja Solkiin saapumisen väliin voi siis tulla muutaman vuorokauden viive. Onko tämä Solkin prosessin
   kannalta ongelma, ja pitääkö odottavasta merkinnästä ilmoittaa etukäteen?
8. **Kitu poistaa merkinnät 5 vuoden kuluttua passivoinnista** (§6.2). Poistoa **ei** ole tarkoitus välittää
   Solkille — oletus on, että Solkilla on oma säilytysaikansa ja oma kopionsa rekisteristä. Pitääkö tämä
   paikkansa? Huomattava myös, ettei kitu poiston jälkeen pysty vastaamaan kyseistä arvioijaa koskeviin
   kyselyihin, mikäli Solki käyttää täsmäytykseen kitun luku-rajapintaa (§4.1).
9. **Cutover-päivä ja päällekkäisen kirjoituksen välttäminen.** OPH:n linjaus (kys. 14) on julkaista
   syöttökäyttöliittymä ensin ja katkaista Solkin kirjoitukset vasta sen jälkeen, jolloin väliin jää jakso,
   jolla molemmat voisivat kirjoittaa. Sovitaanko jaksolle menettely — esim. että Solki lopettaa
   arvioijamerkintöjen tekemisen sovittuna päivänä, vaikka rajapinta olisi teknisesti auki? Kuka tarkistaa
   datan täsmäävyyden ennen kytkintä? **Tämä on ainoa kysymys, jolla on kalenteririippuvuus.**

**OPH — avoimet**

OPH on vastannut kaikkiin 14 kysymykseen (ks. suunnitelman alun päätöstaulukko). Vain yksi jäi auki:

10. **Henkilötunnussarakkeen kohtalo.** Uusi kirjoituspolku ei kirjoita hetuja lainkaan, mutta
    `yki_arvioija`-taulussa on yhä ennen 2026 tehtyjen merkintöjen hetut. OPH: _"säilytetään toistaiseksi,
    päätös myöhemmin"_ — asia palautetaan tietosuojan käsittelyyn. Suunnitelma ei siis poista saraketta
    eikä tyhjennä sitä; kirjoituspolku vain jättää sen koskematta.

**OPH — vastauksista seuraavat tarkennettavat kohdat**

Nämä eivät estä toteutusta, mutta on syytä varmistaa ennen kyseisen vaiheen koodausta:

11. **Kahteen kertaan lisätty henkilö.** Jos keskeneräisenä tallennettu merkintä yksilöityy OID:ksi, joka on
    jo toisella rivillä, rivit on yhdistettävä. Kumpi tiedoista voittaa, ja saako yhdistämisen tehdä
    automaattisesti vai pitääkö virkailijan ratkaista se?
12. **ASHA-numeron muoto.** Toteutetaan vapaana tekstikenttänä ilman validointia. Jos numerolla on vakiintunut
    muoto (diaarinumero), validointi kannattaa lisätä myöhemmin.

---

## 12. Verifiointi

```shell
# Muotoilu (aina ennen valmiiksi raportointia)
./scripts/format.sh && ./scripts/check-formatting.sh

# Backend-testit (Docker päällä — Testcontainers)
cd server && ./mvnw test -Dtest='YkiArvioija*'
cd server && ./mvnw test -Dtest='SolkiArvioija*'
cd server && ./mvnw package            # koko sarja

# E2E
cd e2e && TEST_WORKERS=4 npx playwright test yki-arvioija

# Paikallinen ajo
./scripts/start_local_env.sh
```

Manuaalinen läpiajo paikallisesti (`http://localhost:8080/kielitutkinnot`):

1. `/dev/mocklogin/ROOT` → kirjaudu.
2. `/yki/arvioijat` → lista renderöityy, suodatin ja sivutus toimivat, CSV latautuu ja sarakkeet
   vastaavat `CSV_EXPORT`-tageja.
3. "Lisää arvioija" → syötä hetu + nimi → ONR-haku (local-profiilissa `MockOppijanumeroService`) →
   lomake esitäyttyy → tallenna tyhjällä pakollisella kentällä → **kenttäkohtainen virhe näkyy eikä
   syötteitä häviä** → korjaa → tallennus onnistuu, flash-viesti näkyy.
   Tarkista: `select henkilotunnus from yki_arvioija where arvioija_oid = …` → **NULL**.
4. Kauden päättymispäivä on täsmälleen 5 v alkupäivästä ja sama kaikilla valituilla kielillä.
5. Muokkaa kauden alkupäivä → `select * from yki_arvioija_kausi where arvioija_id = …` sisältää nyt
   **kaksi** riviä; pelkkä sähköpostin muutos ei lisää kolmatta.
6. Poista yksi kieli arviointioikeusmatriisista → rivi katoaa myös `yki_arviointioikeus`-taulusta.
7. Passivoi manuaalisesti → tila päivittyy, `solkiin_lahetetty` nollautuu, historiaan tulee rivi.
8. Solki-stubi päälle → tallennus lähettää heti; stubi palauttamaan 500 → `/yki/arvioijat/virheet`
   näyttää rivin syineen ja yrityslaskureineen, ja "Lähetä uudelleen" tyhjentää virheen.
9. db-scheduler-UI: kaikki viisi uutta tehtävää näkyvät ja ovat käsin ajettavissa (passivointi,
   yksilöinnin täydennys, säilytysajan poisto sekä Solki-lähetyksen pikauusinta ja yöajo).
10. Auditlokit: konsolista löytyvät `YkiArvioijaCreated` ja `YkiArvioijaUpdated`.

---

## Liite A: Kanban-kortit

Suunnitelman §10 vaiheistus korttimuodossa. Koot ovat suhteellisia: **S** ≈ 1–2 pv, **M** ≈ 3–5 pv,
**L** ≈ 1–2 vk. Kokonaisuus on jonkin verran käyttötapauskuvauksen ~6 viikon arviota suurempi, koska
laajuus kasvoi säilytysajalla, keskeneräisen yksilöinnin käsittelyllä ja listanäkymän uudistuksella.

### Ei-tekniset kortit

Nämä kannattaa aloittaa ensimmäisenä: kummassakin on ulkoisen tahon läpimenoaikaa, eikä kumpikaan etene
tiimin omalla työllä.

| Kortti    | Sisältö                                                                                                                      | Estää                    |
| --------- | ---------------------------------------------------------------------------------------------------------------------------- | ------------------------ |
| **KTR-A** | Sovi Solki-rajapintasopimus Jyväskylän kanssa (§5.1, §11 kysymykset 1–9)                                                     | KTR-10, KTR-11, KTR-12   |
| **KTR-B** | Pyydä käyttöoikeus `YKI_ARVIOIJAREKISTERI_KIRJOITUS` Otuvaan, liitettäväksi ryhmään "Kielitutkintorekisteri-oph-pääkäyttäjä" | julkaisun (ei kehitystä) |
| **KTR-C** | Tietosuojan päätös `henkilotunnus`-sarakkeesta (§11 kysymys 10)                                                              | — (jää odottamaan)       |

### Kehityskortit

| Kortti | Otsikko                                                  | Koko | Riippuu        |
| ------ | -------------------------------------------------------- | ---- | -------------- |
| KTR-1  | Poista kuollut arvioijien virhetuontikoneisto            | S    | —              |
| KTR-2  | Laajenna arvioijataulut masteriksi                       | L    | KTR-1          |
| KTR-3  | Uudista arvioijalistanäkymä                              | L    | KTR-2          |
| KTR-4  | Lomakevirhekehys ja arvioijarekisterin käyttöoikeus      | M    | —              |
| KTR-5  | Uuden arvioijan tallennus + ONR-haku (UC1)               | L    | KTR-2, KTR-4   |
| KTR-6  | Keskeneräisen ONR-yksilöinnin käsittely                  | M    | KTR-5          |
| KTR-7  | Muokkaus, kausihistoria ja manuaalinen passivointi (UC2) | L    | KTR-5          |
| KTR-8  | Automaattinen passivointi (UC3)                          | M    | KTR-7          |
| KTR-9  | Säilytysajan valvonta (5 v)                              | M    | KTR-8          |
| KTR-10 | YKI-arvioijien Solki-lähetys                             | L    | KTR-7, KTR-A   |
| KTR-11 | Kavenna sisääntuleva arvioijarajapinta                   | M    | KTR-10         |
| KTR-12 | Käyttöönotto ja kytkimet                                 | S    | KTR-10, KTR-11 |

**KTR-4 on rinnakkaistettavissa** — se ei riipu tietokantatyöstä, joten kaksi tekijää voi edetä yhtä
aikaa (KTR-2 → KTR-3 ja KTR-4). Muuten ketju on käytännössä lineaarinen.

**KTR-6 ja KTR-9 ovat laajuuden kasvua**, eivät alkuperäistä sisältöä: molemmat seurasivat OPH:n
vastauksista (keskeneräinen yksilöinti sallitaan, säilytysaika 5 vuotta). Ne on syytä nostaa esiin, jos
työmääräarviota verrataan käyttötapauskuvauksen alkuperäiseen arvioon.

### Korttien sisältö ja valmiin määritelmä

**KTR-1 · Poista kuollut arvioijien virhetuontikoneisto** — S
Poistetaan `yki/arvioijat/error/`, `V118 DROP TABLE yki_arvioija_error`, `dev/YkiController`in kuollut
stubi sekä viittaukset `DashboardService`/`HomePage`/`EnumFromUrlParamsParsingConfig`.
_Valmis kun:_ `/yki/arvioijat/virheet` ja dashboard-laskuri on poistettu tai ohjattu uudelleen,
`YkiArvioijaErrorTests` poistettu, build vihreä.

**KTR-2 · Laajenna arvioijataulut masteriksi** — L
`V116` (uudet sarakkeet, `asha_numero`, `passivoitu` + backfill kauden päättymispäivästä,
`yksilointi_kesken`, outbox-sarakkeet, `solkiin_lahetetty`-backfill) ja `V117` (`yki_arvioija_kausi`).
Entiteetit, `RowMapper`it ja `YkiArvioijaRepository.tallenna`.
_Valmis kun:_ arviointioikeuksien poisto toimii upsertissä, kausihistoriarivi syntyy vain kauden
muuttuessa, migraatio ei laukaise massalähetystä Solkiin, repository-testit läpi.

**KTR-3 · Uudista arvioijalistanäkymä** — L
`@ColumnTags`, `YkiArvioijaParams`, sivutus arvioijittain, suodatindialogi, CSV-vienti, uusi
`YkiArvioijaViewController` + `Links`-lisäykset.
_Valmis kun:_ suodatus, järjestys ja sivutus toimivat, CSV vastaa `CSV_EXPORT`-tageja, "piilota
henkilötiedot" toimii, e2e päivitetty.

**KTR-4 · Lomakevirhekehys ja käyttöoikeus** — M
`html/FormErrors.kt` + `formField`, `security/CurrentUser.kt`, `Authority.YKI_ARVIOIJAREKISTERI`,
security-säännöt vain CAS-ketjuun, `MockUser`-päivitykset.
_Valmis kun:_ virheellinen lomake renderöityy uudelleen `aria-invalid`illa eivätkä syötteet häviä;
e2e-oikeusmatriisi kattaa sekä 403:n että 200:n.

**KTR-5 · Uuden arvioijan tallennus + ONR (UC1)** — L
`Rekisterikausi`, `TallennaArvioija`, `YkiArvioijaValidation`, jatkokauden päättely,
`OppijanumeroHakuService`-ekstraktio, kaksivaiheinen lomake, arviointioikeusmatriisi, ASHA-numerokenttä,
turvakieltovaroitus, auditlokit, UiText.
_Valmis kun:_ kausi on alkupäivä + 5 v ja sama kaikilla kielillä, **hetu ei tallennu kantaan**, e2e vihreä.

**KTR-6 · Keskeneräisen ONR-yksilöinnin käsittely** — M
`yksilointi_kesken`-tila, §6.3:n ajastettu täydennys, Solki-lähetyksen suodatus.
_Valmis kun:_ merkintä tallentuu ilman oppijanumeroa ja täydentyy ajossa, **eikä kaadu
uniikkirikkeeseen**, jos master-OID on jo toisella rivillä (§11 kysymys 11).

**KTR-7 · Muokkaus, kausihistoria ja manuaalinen passivointi (UC2)** — L
Tietosivu + kausihistoriataulukko + POST, `passivoiArvioija`.
_Valmis kun:_ uusi kauden alkupäivä synnyttää historiarivin mutta pelkkä yhteystiedon muutos ei; kielen
lisäys kesken kauden perii voimassa olevan kauden.

**KTR-8 · Automaattinen passivointi (UC3)** — M
`YkiArvioijaScheduledTasks`, `passivoitu`-leima kauden päättymispäivästä (ei `now()`), outboxin likaus.
_Valmis kun:_ päättyneet kaudet passivoituvat, `passivoitu` vastaa kauden päättymispäivää, tehtävä näkyy
db-scheduler-UI:ssa ja on ajettavissa käsin.

**KTR-9 · Säilytysajan valvonta (5 v)** — M
§6.2:n poistotehtävä, oletuksena pois päältä.
_Valmis kun:_ poisto ei koske aktiivisia eikä `passivoitu IS NULL` -rivejä, ja tehtävä on **ajettu
untuvassa ja poistuvien rivien määrä tarkistettu** ennen kuin tuotantoon ottamista harkitaan.

**KTR-10 · YKI-arvioijien Solki-lähetys** — L
`solki/`-paketti, outbox-kirjoitukset, dev-stubi, propertyt (`enabled=false`), virhenäkymä +
"Lähetä uudelleen" + dashboard-laskuri, `docs/technical/integraatiot.md`.
_Valmis kun:_ 3 yritystä ja yöajo toimivat, virhe näkyy syineen,
`.withLenientStringConverter()` on mukana ja `debugString()` ei vuoda henkilötietoja lokiin.

**KTR-11 · Kavenna sisääntuleva arvioijarajapinta** — M
`POST /yki/api/arvioija` → 410, tilalle kavennettu passivointi-endpoint (§4.2).
_Valmis kun:_ Solki voi passivoida muttei kirjoittaa nimiä tai kausia, ja **inbound-passivointi ei
laukaise kaikuvaa PUT-lähetystä takaisin Solkiin**.

**KTR-12 · Käyttöönotto ja kytkimet** — S
`enabled=true` untuvaan, QA:han ja prodiin, 410-mapping pois, cutover sovitusti.
_Valmis kun:_ cutover-päivä on sovittu JYU:n kanssa ja datan täsmäävyys tarkistettu ennen kytkintä.
