#!/usr/bin/env bash
set -euo pipefail

# allow sourcing this file multiple times from different scripts
if [ -n "${COMMON_FUNCTIONS_SOURCED:-}" ]; then
  return
fi
readonly COMMON_FUNCTIONS_SOURCED="true"

function require_command {
  if ! command -v "$1" > /dev/null; then
    fatal "$1 is required, but it's not installed. Aborting."
  fi
}

function info {
  log "INFO" "$*"
}

function fatal {
  log "ERROR" "$*"
  exit 1
}

function log {
  local -r level="$1"
  local -r message="${*:2}"
  local -r timestamp=$(date +"%Y-%m-%d %H:%M:%S")

  >&2 echo -e "${timestamp} ${level} ${message}"
}
