#!/usr/bin/env bash
set -euo pipefail

# allow sourcing this file multiple times from different scripts
if [ -n "${COMMON_FUNCTIONS_SOURCED:-}" ]; then
  return
fi
readonly COMMON_FUNCTIONS_SOURCED="true"

function require_command {
  if ! command -v "$1" > /dev/null; then
    echo "$1 is required, but it's not installed. Aborting."
    exit 1
  fi
}
