#!/bin/sh
(
  set -e

  REPO_ROOT=${1:-"."}
  BUILD_FOLDER="out"

  cd $REPO_ROOT/frontend
  
  npm ci
  npm run build
  mkdir -p ../server/target/classes
  mv $BUILD_FOLDER ../server/target/classes/static
)
