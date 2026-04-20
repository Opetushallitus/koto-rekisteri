#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common-functions.sh"

DEFAULT_PAYLOAD="${REPO_ROOT}/server/src/test/resources/yki-tiedonsiirto-example.json"
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/kitu"

function usage {
  cat <<EOF
Usage: $(basename "$0") [--env local|dev|test] [--payload path/to/payload.json] [--reconfigure]

Uploads a YKI suoritus via POST /yki/api/suoritus, authenticating with OAuth2
client_credentials.

For 'dev' and 'test', the OAuth client id and secret are prompted on first use
and cached under ${CONFIG_DIR}. Delete the file or pass --reconfigure to
re-enter them.

Options:
  --env            Target environment: local, dev, or test. Default: local.
  --payload        Path to the JSON payload. Default: ${DEFAULT_PAYLOAD}.
  --reconfigure    Prompt for credentials again and overwrite the cached file.
  -h, --help       Show this help.
EOF
}

env_name="local"
payload_file="${DEFAULT_PAYLOAD}"
reconfigure="false"

while [ $# -gt 0 ]; do
  case "$1" in
    --env)
      env_name="$2"
      shift 2
      ;;
    --payload)
      payload_file="$2"
      shift 2
      ;;
    --reconfigure)
      reconfigure="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fatal "Unknown argument: $1"
      ;;
  esac
done

function load_or_prompt_credentials {
  local env_key="$1"
  local config_file="${CONFIG_DIR}/yki-upload-${env_key}.json"

  if [ "$reconfigure" = "false" ] && [ -f "$config_file" ]; then
    oauth_client_id=$(jq -r '.client_id // empty' "$config_file")
    oauth_client_secret=$(jq -r '.client_secret // empty' "$config_file")
    if [ -n "$oauth_client_id" ] && [ -n "$oauth_client_secret" ]; then
      info "Loaded credentials for '${env_key}' from ${config_file}"
      return
    fi
    info "Cached config at ${config_file} is incomplete — reprompting."
  fi

  info "Configuring credentials for '${env_key}'."
  read -r -p "OAuth client id: " oauth_client_id
  [ -n "$oauth_client_id" ] || fatal "client id must not be empty."
  read -r -s -p "OAuth client secret: " oauth_client_secret
  echo >&2
  [ -n "$oauth_client_secret" ] || fatal "client secret must not be empty."

  mkdir -p "$CONFIG_DIR"
  chmod 700 "$CONFIG_DIR"
  ( umask 077 && jq -n \
      --arg id "$oauth_client_id" \
      --arg secret "$oauth_client_secret" \
      '{client_id: $id, client_secret: $secret}' > "$config_file" )
  chmod 600 "$config_file"
  info "Saved credentials to ${config_file}"
}

require_command curl
require_command jq

[ -f "$payload_file" ] || fatal "Payload file not found: ${payload_file}"

case "$env_name" in
  local)
    host="http://localhost:8080/kielitutkinnot"
    oauth_token_url="http://localhost:8080/kielitutkinnot/dev/oauth/token"
    oauth_client_id="ROOT"
    oauth_client_secret="hunter2"
    ;;
  dev)
    host="https://virkailija.untuvaopintopolku.fi/kielitutkinnot"
    oauth_token_url="https://dev.otuva.opintopolku.fi/kayttooikeus-service/oauth2/token"
    load_or_prompt_credentials "$env_name"
    ;;
  test)
    host="https://virkailija.testiopintopolku.fi/kielitutkinnot"
    oauth_token_url="https://qa.otuva.opintopolku.fi/kayttooikeus-service/oauth2/token"
    load_or_prompt_credentials "$env_name"
    ;;
  *)
    fatal "Unknown environment: ${env_name}. Expected one of: local, dev, test."
    ;;
esac

info "Fetching OAuth2 access token from ${oauth_token_url}"
token_response=$(curl --fail --silent --show-error \
  -X POST "${oauth_token_url}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "client_id=${oauth_client_id}" \
  --data-urlencode "client_secret=${oauth_client_secret}")

access_token=$(printf '%s' "$token_response" | jq -r '.access_token // empty')
[ -n "$access_token" ] || fatal "OAuth2 response did not contain access_token: ${token_response}"

info "Posting YKI suoritus from ${payload_file} to ${host}/yki/api/suoritus"
response_body=$(mktemp)
trap 'rm -f "$response_body"' EXIT

http_status=$(curl --silent --show-error \
  -X POST "${host}/yki/api/suoritus" \
  -H "Authorization: Bearer ${access_token}" \
  -H "Content-Type: application/json" \
  --data-binary "@${payload_file}" \
  -o "${response_body}" \
  -w "%{http_code}")

jq . "${response_body}" >&2 2>/dev/null || cat "${response_body}" >&2
echo >&2

if [ "${http_status}" -ge 400 ]; then
  fatal "Upload failed with HTTP ${http_status}."
fi

info "Upload succeeded (HTTP ${http_status})."