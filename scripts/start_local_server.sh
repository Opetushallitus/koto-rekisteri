#!/usr/bin/env bash

set -euo pipefail

source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

get_secret() {
  aws secretsmanager get-secret-value --secret-id "$1" --output text --query SecretString
}

require_command humanlog
require_command aws

configure_aws_sso_profiles
require_dev_aws_session

require_env SPRING_PROFILES_ACTIVE
require_env KOTLIN_POST_PROCESS_FILE

cd "$REPO_ROOT"/server

KIELITESTI_TOKEN=$(get_secret kielitesti-token); export KIELITESTI_TOKEN
OPPIJANUMERO_PASSWORD=$(get_secret oppijanumero-password); export OPPIJANUMERO_PASSWORD
YKI_API_USER=$(get_secret yki-api-user); export YKI_API_USER
YKI_API_PASSWORD=$(get_secret yki-api-password); export YKI_API_PASSWORD

./mvnw spring-boot:run | humanlog --truncate-length 9999
