#!/usr/bin/env bash

set -eux

prettier --check .
(cd server && ktlint)
shellcheck -P ./scripts ./scripts/*.sh
shellcheck -P ./infra/scripts ./infra/scripts/*.sh
