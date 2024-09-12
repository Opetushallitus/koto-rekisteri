# Kielitutkintorekisteri

## Riippuvuudet
- Docker
- PostgreSQL
- maven
- java 21
- node 22.7.0
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

#### Hyödyllisiä komentoja

```shell
# Jos haluat lisätä formatointitarkastuksen commitin luonnin yhteyteen
./scripts/setup-hooks.sh

# Tarkista formatointi. Voit formatoida koodin ajamalla `ktlint --format`
ktlint

# paketoi projektin.
mvn package

 # Voit käyttää tätä jos ajat ympäristöä terminaalin kautta
./mvnw spring-boot:run
```
