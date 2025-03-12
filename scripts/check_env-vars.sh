#!/usr/bin/env bash

# Parse flags
verbose=false
while getopts "X" opt; do
  case $opt in
    X) verbose=true ;;
    *) exit 1 ;;
  esac
done

is_env_variable_exported() {
  local var_name="$1"
  if env | grep -q "^${var_name}="; then
    $verbose && echo "[DEBUG] variable ${var_name} is exported"
    return 0
  else
    $verbose && echo "[DEBUG] variable ${var_name} is missing!"
    return 1
  fi
}

amount=0

is_env_variable_exported "KIELITESTI_TOKEN" || amount=$((amount + 1))
is_env_variable_exported "PALVELUKAYTTAJA_PASSWORD" || amount=$((amount + 1))
is_env_variable_exported "YKI_API_USER" || amount=$((amount + 1))
is_env_variable_exported "YKI_API_PASSWORD" || amount=$((amount + 1))

echo "$amount variable(s) are missing."
exit $amount



