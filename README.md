# Kielitutkintorekisteri

## Riippuvuudet
- PostgreSQL
- maven
- java 21

## Kehittäminen
Paikallinen testaus- ja kehitysympäristö vaatii toimiakseen PostgreSQL -tietokannan. Tietokantaa ajetaan Dockerissa ja ne saa käyntiin `docker compose`:lla. Tietokannan skeema alustetaan migraatioilla. Migraatiotyökaluna käytössä on _Flyway_. Migraatioiden suorittamiseen paikallisessa kehitysympäristössä on konfiguroitu Maven-liitännäinen.

```shell
docker compose up -d db # Käynnistä tietokanta
./mvnw flyway:migrate # Aja migraatiot
ktlint # Tarkista formatointi. Voit formatoida koodin ajamalla `ktlint --format`
./mvnw spring:boot run # Voit käyttää tätä jos ajat ympäristöä terminaalin kautta
```

Kontissa suoritetaan PostgreSQL -palvelinohjelmaa, johon on [konfiguroitu](scripts/postgres-docker/init-db.sql) tietokannat `kitu-dev` (paikallisen kehitysympäristön käyttöön) ja `kitu-test` (automaattisille testeille). Flyway alustaa tietokannan skeeman [migraatioilla](src/main/resources/db/migration).


