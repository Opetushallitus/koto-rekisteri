#!/usr/bin/env bash
set -euo pipefail

current_dir=$( dirname "${BASH_SOURCE[0]}" )
source "$current_dir/common-functions.sh"


# on macos: brew install libpq && echo 'export PATH="/usr/local/opt/libpq/bin:$PATH"' >> ~/.zshrc
require_command pg_dump

# (macos) alternatively, you can `docker exec -it kitu-db-1 {command}`, for example:
# docker exec -it kitu-db-1 pg_dump --schema-only kitu-dev
pg_dump --schema-only kitu-dev
