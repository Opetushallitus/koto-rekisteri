#!/usr/bin/env bash
set -euo pipefail

# Offline-kehityksen käynnistin.
# - Ei kutsua AWS Secrets Manageria (placeholderit on ylikirjoitettu
#   application-local-opintopolku.properties:ssä)
# - Ei kutsua Untuva Opintopolkua (mock-palvelut + /dev/mocklogin)
#
# Käyttö:
#   ./scripts/start_offline_dev.sh idea .
#   ./scripts/start_offline_dev.sh ./scripts/start_local_server.sh
#   ./scripts/start_offline_dev.sh ./mvnw spring-boot:run
#
# Profiilien järjestys: local-opintopolku viimeisenä, jotta sen overridet voittavat.

export SPRING_PROFILES_ACTIVE="local,local-opintopolku"
exec "$@"
