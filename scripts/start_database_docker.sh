#!/usr/bin/env bash
set -euo pipefail

KERNEL="$(uname -s)"
if [[ $KERNEL == "Darwin" ]]; then
  echo "Opening Docker Desktop to ensure daemon is running"
  open -a "Docker"
  sleep 5
fi

docker compose up db
