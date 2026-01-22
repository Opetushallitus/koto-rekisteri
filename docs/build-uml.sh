#!/usr/bin/env bash
set -euo pipefail

packages=(
  "auth"
  "i18n"
  "ilmoittautumisjarjestelma"
  "koodisto"
  "koski"
  "kotoutumiskoulutus"
  "logging"
  "oauth2client"
  "observability"
  "oppijanumero"
  "organisaatiot"
  "tiedonsiirtoschema"
  "validation"
  "vkt"
  "yki.suoritukset"
  "yki.arvioijat"
)

BASEDIR=$(dirname "$0")
OUTPUT_DIR="$BASEDIR/../uml"

function load_pumls() {
  local env="$1"
  local url="$2"
  local output_dir="$OUTPUT_DIR/$env"

  echo "Generate puml files for $env ($url)..."
  echo

  mkdir -p "$output_dir"
  for package in "${packages[@]}"; do
      echo "$env/$package.puml"
      curl -s "$url/uml/$package" > "$output_dir/$package.puml"
  done
}

function generate_images() {
  local env="$1"

  echo
  echo "Generate image files..."
  echo

  find "$OUTPUT_DIR" -type f -name "*.puml" | while IFS= read -r file; do
    target_svg="${file//puml/svg}"
    echo "Processing: $file -> $target_svg"
    docker run -e PLANTUML_LIMIT_SIZE=20000 --rm -i dstockhammer/plantuml:latest -tsvg -pipe > "$target_svg" < "$file"
  done
}

function generate_markdown() {
  local env="$1"
  local target_dir="$OUTPUT_DIR/$env"
  target_md="$target_dir/index.md"

  echo
  echo "Generate markdown file for $env: $target_md"
  echo

  {
    echo "# Beans-komponentit ympäristössä $env"
    echo
  } > "$target_md"

  find "$target_dir" -type f -name "*.svg" | while IFS= read -r file; do
    filebasename=$(basename "$file")
    name="fi.oph.kitu.${filebasename//.svg/}"
    {
      echo "## $name"
      echo "![$name diagram]($filebasename)"
      echo
    } >> "$target_md"
  done
}

function load_and_generate() {
  local env="$1"
  local source_url="$2"

  load_pumls "$env" "$source_url"
  generate_images "$env"
  generate_markdown "$env"
}

for arg in "$@"; do
  if [[ "$arg" == "local" ]]; then
    load_and_generate "local" "http://localhost:8080/kielitutkinnot"
    break
  fi
  if [[ "$arg" == "untuva" ]]; then
    load_and_generate "untuva" "https://virkailija.untuvaopintopolku.fi/kielitutkinnot"
    break
  fi
  if [[ "$arg" == "qa" ]]; then
    load_and_generate "qa" "https://virkailija.testiopintopolku.fi/kielitutkinnot"
    break
  fi
  if [[ "$arg" == "prod" ]]; then
    load_and_generate "prod" "https://virkailija.opintopolku.fi/kielitutkinnot"
    break
  fi
done
