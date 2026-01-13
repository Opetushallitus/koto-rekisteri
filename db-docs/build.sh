#!/bin/bash
set -euo pipefail

DB=kitu-schemaspy
USER=kitu
PASSWORD=kitu
PORT=5432

# Käynnistä tietokanta
docker compose up -d --build db
sleep 3

# Aja migraatiot
docker run --rm \
  -v "$(pwd)/../server/src/main/resources/db/migration:/flyway/sql" \
  flyway/flyway:latest \
  -url="jdbc:postgresql://host.docker.internal:$PORT/$DB" \
  -user="$USER" \
  -password="$PASSWORD" \
  migrate

# Luo dokumentaatio
docker run \
  -v "$PWD/output:/output" \
  schemaspy/schemaspy:latest \
  -t pgsql11 \
  -host host.docker.internal \
  -port "$PORT" \
  -db "$DB" \
  -u "$USER" \
  -p "$PASSWORD" \
  -s=public

# Tallenna viimeisin migraation versio tiedostoon CI:llä ajettavaa checkiä varten
bash latest_migration.sh > docversion