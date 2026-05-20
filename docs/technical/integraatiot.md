# Integraatiot

Kielitutkintorekisteri integroituu useisiin Opetushallituksen ja kolmannen
osapuolen järjestelmiin. Tämä sivu kuvaa kullekin integraatiolle vastaavan
paketin, asiakaskerroksen, virhemallin sekä vastuun rajauksen.

## KOSKI — suoritusten siirto

**Paketti:** `koski/`
**Suunta:** lähetys

Kielitutkintorekisteri lähettää onnistuneet YKI- ja VKT-suoritukset
KOSKI-palveluun, joka julkaisee ne Oma Opintopolun kautta loppukäyttäjille.

- `KoskiService.sendYkiSuorituksetToKoski()` /
  `sendVktSuorituksetToKoski()` — palauttavat
  `Either<KoskiTechnicalException, KoskiTransferReport>`. Tekniset virheet
  (esim. KOSKI ei vastaa) palautetaan vasempana; tietosisällölliset virheet kerätään
  raporttiin ja merkitään uudelleen lähetettäviksi.
- Eräajot `Lähetä YKI-suoritukset KOSKI-palveluun` ja vastaava VKT-versio
  ajavat lähetyksen aikataulutetusti — ks. [Eräajot](../db-scheduler).
- Virhetyypit: `KoskiException`-hierarkia, jossa `KoskiTechnicalException`
  on uudelleenyritettävä ja muut suoritusrajan ulkopuolisia virheitä.

## KIOS — ilmoittautumisjärjestelmä, VKT-suoritusten lähde

**Paketti:** `ilmoittautumisjarjestelma/`
**Suunta:** lähetys (arviointitilat), sisääntuleva pyyntö (vkt)

YKI-suoritusten arviointitilat ilmoitetaan KIOS-järjestelmälle. Tavallisesti
ilmoitus lähetetään välittömästi Solki-päivityksen yhteydessä; ajastetun
tehtävän `Lähetä YKI-arviointitilat KIOS-palveluun` tehtävänä on varmistaa
perille meno verkkokatkosten jälkeen.

- `IlmoittautumisjarjestelmaClientImpl.post(...)` palauttaa
  `Either<IlmoittautumisjarjestelmaException, T>`.
- Virhetyypit: `BadRequest`, `UnexpectedError`, `MalformedResponse`,
  `NullResponse`.

VKT-ilmoittautumiset ja -suoritukset tuodaan KIOS-palvelusta.

## Oppijanumerorekisteri

**Paketti:** `oppijanumero/`
**Suunta:** haku

Oppijoiden henkilötietojen ja oppijanumeroiden noutoon.

- `OppijanumerorekisteriClient.getUser(...)` palauttaa
  `Either<OppijanumeroException, Oppija>`.
- Virhetyypit: `OppijaNotFoundException`, `BadRequest`, `UnexpectedError`,
  `MalformedResponse`, `NullResponse`. `HasResponse`-rajapinta merkitsee
  ne, joilla on raaka HTTP-vastaus liitteenä.

## Organisaatiopalvelu

**Paketti:** `organisaatiot/`
**Suunta:** haku

Käytetään mm. oppilaitosten OID:eihin ja niiden hierarkian noutoon validoinnin tueksi.

- `OrganisaatiopalveluClient.get(...)` palauttaa
  `Either<OrganisaatiopalveluException, T>`.
- Virhetyypit vastaavat oppijanumeropuolen mallia.

## Koodisto

**Paketti:** `koodisto/`
**Suunta:** haku

OPH:n keskitetty koodistopalvelu — käytetään esim. kielikoodien ja
luokitusten validointiin.

## Koealusta (kotoutumiskoulutus)

**Paketti:** `kotoutumiskoulutus/koealusta/`
**Suunta:** haku + tiedoston tuonti

Koealusta on Moodle-pohjainen kielitestialusta, josta Kielitutkintorekisteri hakee
suoritustietoja ja Moodle-XML-pohjaisia tehtäväpaketteja.

- `KoealustaSuoritusValidator` — validoi yksittäisten suoritusten datan
  oppijanumerorekisteröintiä varten; palauttaa
  `Either<KoealustaMappingError.*Failure, …>`.
- `tehtavapankki/TehtavapankkiIngestService` — lataa XML:t S3:sta, parsii ne
  XmlParserilla ja tallentaa yleiseen tehtäväpankki-skeemaan.

## Solki — YKI-tietojen lähde

**Paketti:** `yki/`
**Suunta:** sisääntuleva pyyntö

Yleiset kielitutkinnot lähetetään Jyväskylän yliopiston Solki-järjestelmästä.

## CAS + OAuth2 — virkailija-autentikointi

**Paketit:** `auth/`, `oauth2client/`
**Suunta:** haku

Sovellus käyttää OPH:n keskitettyä Otuva-palvelua.

## Auditlokit

**Paketti:** `auditlogs/`

OPH:n auditlokitoteutus ECS-formaatissa. Logien siirto
S3:een hoidetaan infrassa stack-kohtaisesti
(`koski-audit-logs-integration-stack`).
KOSKI-tiimin ylläpitämä palvelu.
