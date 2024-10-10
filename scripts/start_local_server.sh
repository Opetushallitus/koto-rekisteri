#!/usr/bin/env bash

set -euo pipefail

source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

require_command humanlog

(
  cd "$REPO_ROOT"/server
  ./mvnw spring-boot:run | humanlog --truncate-length 9999
)
