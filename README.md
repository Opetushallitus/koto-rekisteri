# Kielitutkintorekisteri

## Riippuvuudet

- mise
- IntelliJ IDEA
- Docker
- tmux

Loput riippuvuudet asennetaan käyttäen `mise` -työkalua. Misen asennus onnistuu homebrewlla (`brew install mise`) tai
[vaihtoehtoisilla tavoilla](https://mise.jdx.dev/getting-started.html#_1-install-mise-cli). Kehitysympäristön perustusskripti suorittaa `mise install` osana kehitysympäristön
perustusta, joten sen ajaminen erikseen ei ole tarpeen. Ajantasainen lista asennettavista riippuvuuksista on nähtävillä
`.mise.toml`-tiedostossa, jossa on myös muu Misen konfiguraatio (mukaanlukien joitain tarpeellisia ympäristömuuttujia).

## Kehittäminen

### Salaisuudet paikallisessa kehityksessä

Luo `server/src/main/resources/`-hakemiston alle `local.properties`-tiedosto, ja lisää sinne tarvittavat salaisuudet.
Esimerkki tiedoston sisällöstä löytyy samasta hakemistosta `example-local.properties`-tiedostosta.

Spring lataa automaattisesti `<aktiivinen profiili>.properties` tiedostoon lisätyt asetukset palvelimen käynnistyksen
yhteydessä, eli paikallisen kehitysympäristön tapauksessa `local.properties`.

### Kehitysympäristön perustaminen

Paikallisen kehitysympäristön perustamiseen käytetään skriptiä `start_local_env.sh`. Skriptiä sovelletaan myös
kehitysympäristön riippuvuuksien ja perustusvaiheiden dokumentaationa.

```shell
./scripts/start_local_env.sh
```

Skripti perustaa kehitysympäristön ja oletuksena avaa uuden `tmux` session, jonka eri ikkunoihin esim. tietokantaan ja
taustapalvelimeen liittyvät prosessit käynnistetään.

Mikäli et halua avata `tmux`-sessiota ja haluat käynnistää tietokannan ja taustapalvelimen yms. jollain muulla tavoin,
skriptille voi antaa `--setup-only` parametrin. Tällöin suoritetaan kehitysympäristön perustus ja konfigurointi, mutta
`tmux`-session perustaminen, sekä Docker-konttien ja palvelinten käynnistäminen jätetään tekemättä.

```shell
./scripts/start_local_env.sh --setup-only
```

### OpenAPI

Palvelimen API-rajapinnat, -tyypit ja näiden dokumentaatio generoidaan [OpenAPI-spesifikaatiosta](server/src/main/resources/static/open-api.yaml).
Spesifikaation pohjalta generoidaan API-rajapinta ([esim. `OppijaControllerApi`-rajapinta](server/src/main/kotlin/fi/oph/kitu/generated/api/OppijaControllerApi.kt)), sekä tarvittavat luokat rajapinnassa
käsiteltävien olioiden mallintamiseen muistissa (luokat [`generated.model.*`-paketissa](server/src/main/kotlin/fi/oph/kitu/generated/model)).
Varsinainen toteuttava luokka perii generoidun rajapinnan ja toteuttaa kaikki rajapinnan tarjoamat metodit ([esim. `OppijaController`-luokka](server/src/main/kotlin/fi/oph/kitu/oppija/OppijaController.kt)).

API-dokumentaatio on nähtävillä Swagger UI:sta, joka tarjoillaan polusta `/swagger-ui.html`, esim.

- Paikallinen kehitysympäristö: http://localhost:8080/swagger-ui.html
- Testiympäristö: https://kios.untuvaopintopolku.fi/swagger-ui.html

Tiivistettynä, vaiheet uuden rajapinnan lisäämiseen:

1. Dokumentoi rajapinnan API ja datatyypit `open-api.yaml`-spesifikaatiotiedostoon
2. Suorita `./mvnw generate-sources` generoidaksesi API-rajapinnat ja muut tarvittavat luokat
3. Mikäli kyseessä on kokonaan uusi API (uusi controller), tarvitaan uusi luokka, joka perii generoidun `*ControllerApi`-rajapinnan. Nimeämiskäytäntönä on käyttää generoidun rajapinnan nimeä, ilman `Api`-suffiksia (esim. `class OppijaController : OppijaControllerApi`).
4. Mikäli lisäsit uusia datatyyppejä, on konventiona lisätä näille tyyppialias (esim. [generated/model/Oppija.kt](server/src/main/kotlin/fi/oph/kitu/generated/model/Oppija.kt) ja [oppija/Oppija.kt](server/src/main/kotlin/fi/oph/kitu/oppija/Oppija.kt)).
   Konvention tarkoituksena on välttää suoria riippuvuuksia generoituun koodiin.
5. Toteuta uudet API-metodit luokkaan, joka perii generoidun API-rajapinnan.

### IDEA

#### Koodin tyyli ja muotoilu

Sovelluksessa käytetään `ktlint` - teknologiaa kotlin - tiedostojen tyylittämiseen. `mise` asentaa Ktlintin
kehitysympäristön perustamisen yhteydessä. IntelliJ IDEA:aan saa Ktlint-liitännäisen, jonka asentamisen jälkeen IDEA:n
voi laittaa muotoilemaan koodin tallentamisen yhteydessä.

1. Navigoi `Settings` -> `Tools` -> `KtLint`
2. Tämän valikon alta, aseta `Mode: Distract free` ja varmista että `Format: on save` -valintaruutu on valittu.

#### Palvelimen käynnistäminen editorista

Frontendia varten on `.run` - kansiossa valmis konfiguraatio, jolla voi ajaa frontin node-palvelinta IDEA:sta.

## Hyödyllisiä komentoja

```shell
# Jos haluat lisätä formatointitarkastuksen commitin luonnin yhteyteen
./scripts/setup-hooks.sh

# Tarkista formatointi. Voit formatoida koodin ajamalla `ktlint --format`
ktlint

# paketoi projektin.
mvn package

# Voit käyttää tätä jos ajat ympäristöä terminaalin kautta (ajettava 'server/' -kansiossa)
./mvnw spring-boot:run

# e2e-testien ajaminen e2e-hakemistossa
# Playwrightin UI testien ajamiseen --ui flagilla
npx playwright test
```

## Ympäristöt

Sovellus julkaistaan kolmelle AWS-tilille:

| Nimi | AWS-tili     |
| ---- | ------------ |
| dev  | 682033502734 |
| test | 961341546901 |
| prod | 515966535475 |

Julkaisu tapahtuu automaattisesti GitHub Actions -palvelussa [Build](./.github/workflows/build.yml)-tiedoston mukaisesti jokaisella `main`-haaran päivityksellä. Julkaisun voi myös ajaa omalta koneelta komennoilla:

[//]: # "TODO: luo skripti tätä varten"

```shell
(cd infra && TAG=$(git rev-parse HEAD) npx cdk deploy 'Util/**')
(cd infra && TAG=$(git rev-parse HEAD) npx cdk deploy 'Dev/**')
(cd infra && TAG=$(git rev-parse HEAD) npx cdk deploy 'Test/**')
(cd infra && TAG=$(git rev-parse HEAD) npx cdk deploy 'Prod/**')
```

### Salaisuudet AWS-ympäristöissä

Seuraavat salaisuudet pitää luoda manuaalisesti AWS Secret Manageriin.

- `slack-webhook-url`: Hälytysten lähettämiseen Slack-kanavalle. Ks. [slackNotifierLambda](infra/lib/lambdas/slackNotifierLambda). Pitää luoda regioonille `eu-west-1` sekä `us-east-1`.
- `oppijanumero-password`: Oppijanumeropalvelun salaisuus. Ks. [fi.oph.kitu.oppijanumero-paketti](server/src/main/kotlin/fi/oph/kitu/oppijanumero).
- `kielitesti-token`: Koealustan salaisuus. Ks. [fi.oph.kitu.kielitesti-paketti](server/src/main/kotlin/fi/oph/kitu/kotoutumiskoulutus).
