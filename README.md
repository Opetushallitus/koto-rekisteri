# Kielitutkintorekisteri

## Kehitysympäristö

Paikallinen testaus- ja kehitysympäristö vaatii toimiakseen PostgreSQL -tietokannan. Tietokantaa ajetaan Dockerissa ja ne saa käyntiin `docker compose`:lla.
```shell
docker compose up -d db
```
Kontissa suoritetaan PostgreSQL -palvelinohjelmaa, johon on [konfiguroitu](scripts/postgres-docker/init-db.sql) tietokannat `kitu-dev` (paikallisen kehitysympäristön käyttöön) ja `kitu-test` (automaattisille testeille).
