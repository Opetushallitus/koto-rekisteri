#!/usr/bin/env bash
source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

(
  set -e

  BUILD_FOLDER="out"

  cd $REPO_ROOT/frontend
  
  npm ci
  npm run build
  mkdir -p ../server/target/classes
  mv $BUILD_FOLDER ../server/target/classes/static
)
