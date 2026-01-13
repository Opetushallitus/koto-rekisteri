#!/bin/bash
set -euo pipefail

DB=kitu-schemaspy
USER=kitu
PASSWORD=kitu
PORT=5432
HOST="${HOST:-host.docker.internal}"

SCRIPT_DIR="$(dirname "$(realpath "${BASH_SOURCE[0]}")")"

# Käynnistä tietokanta
docker compose up -d --build db
sleep 3

# Aja migraatiot
docker run --rm \
  -v "$SCRIPT_DIR/../server/src/main/resources/db/migration:/flyway/sql" \
  --network host \
  flyway/flyway:latest \
  -url="jdbc:postgresql://$HOST:$PORT/$DB" \
  -user="$USER" \
  -password="$PASSWORD" \
  migrate

# Hae versiot
CURRENT="$(cat "$SCRIPT_DIR/docversion")"
LATEST="$("$SCRIPT_DIR/latest_migration.sh")"

# Tulosta havainnot
echo "Dokumentaation versio:    $CURRENT"
echo "Tietokantaskeeman versio: $LATEST"
echo

if [ "$CURRENT" = "$LATEST" ]; then
  echo "Dokumentaatio on ajan tasalla"
else
  echo "Dokumentaatio on vanhentunut. Aja build.sh kansiossa db-docs."
  exit 1
fi