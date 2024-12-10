#!/usr/bin/env bash
set -euo pipefail

current_dir=$( dirname "${BASH_SOURCE[0]}" )
source "$current_dir/common-functions.sh"

get_secret() {
  aws secretsmanager get-secret-value --secret-id "$1" --output text --query SecretString
}

"$current_dir/ensure_aws_profiles.sh"
require_dev_aws_session

export "KIELITESTI_TOKEN"="$(get_secret "kielitesti-token")"; echo "KIELITESTI_TOKEN exported"
export "OPPIJANUMERO_PASSWORD"="$(get_secret "oppijanumero-password")"; echo "OPPIJANUMERO_PASSWORD exported"
export "YKI_API_USER"="$(get_secret "yki-api-user")"; echo "YKI_API_USER exported"
export "YKI_API_PASSWORD"="$(get_secret "yki-api-password")"; echo "YKI_API_PASSWORD exported"

exec "$@"
