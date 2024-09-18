#!/usr/bin/env bash
source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

(
  set -e

  BUILD_FOLDER="out"

  cd "$REPO_ROOT"/frontend
  
  npm ci
  npm run build
  rm -rf "$REPO_ROOT"/server/target/classes/static
  mkdir -p "$REPO_ROOT"/server/target/classes/static
  mv -f "$BUILD_FOLDER"/* "$REPO_ROOT"/server/target/classes/static/
)
