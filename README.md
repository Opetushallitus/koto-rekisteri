# Kielitutkintorekisteri

## Riippuvuudet

- Docker
- PostgreSQL
- maven
- java 21
- node 22.9.0
  - npm 10.8.3

## Kehittäminen

Paikallinen testaus- ja kehitysympäristö vaatii toimiakseen PostgreSQL -tietokannan. Tietokantaa ajetaan Dockerissa ja ne saa käyntiin `docker compose`:lla. Tietokannan skeema alustetaan migraatioilla. Migraatiotyökaluna käytössä on _Flyway_. Migraatioiden suorittamiseen paikallisessa kehitysympäristössä on konfiguroitu Maven-liitännäinen.

```shell
docker compose up -d db # Käynnistä tietokanta
./mvnw flyway:migrate # Aja migraatiot

```

Kontissa suoritetaan PostgreSQL -palvelinohjelmaa, johon on [konfiguroitu](scripts/postgres-docker/init-db.sql) tietokannat `kitu-dev` (paikallisen kehitysympäristön käyttöön) ja `kitu-test` (automaattisille testeille). Flyway alustaa tietokannan skeeman [migraatioilla](src/main/resources/db/migration).

### mise

Javan ja noden voi asentaa [mise](https://github.com/jdx/mise)-työkalulla. Misen asennus onnistuu homebrewlla (`brew install mise`) tai [vaihtoehtoisilla tavoilla](https://mise.jdx.dev/getting-started.html#_1-install-mise-cli).
Asennettuasi misen voit ajaa komennon `mise install`, ja oikeat java- ja node-versiot asentuvat.

### Linttaus

Sovelluksessa käytetään `ktlint` - teknologiaa kotlin - tiedostojen tyylittämiseen. MacOS:llä sen saa asennettua ajamalla `brew install ktlint`. IntelliJ IDEA:aan saa plugin `ktlint`,
jonka asentamisen jälkeen IDEA:n voi laittaa formatoimaan tallentamisen yhteydessä `Settings` -> `Tools` -> `KtLint` alta `Mode`: `Distract free` - radiobutton ja `Format`: `on save` - checkbox täpätty.

### IDEA

Frontendia varten on `.run` - kansiossa Konfiguraatio, jolla voi ajaa nodea IDEA:sta.

### Hyödyllisiä komentoja

```shell
# Jos haluat lisätä formatointitarkastuksen commitin luonnin yhteyteen
./scripts/setup-hooks.sh

# Tarkista formatointi. Voit formatoida koodin ajamalla `ktlint --format`
ktlint

# paketoi projektin.
mvn package

# Voit käyttää tätä jos ajat ympäristöä terminaalin kautta
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
