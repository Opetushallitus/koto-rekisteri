#!/usr/bin/env bash

set -eux

prettier --check e2e
prettier --check infra
prettier --check scripts
prettier --check server
(cd server && ktlint)
shellcheck -P ./scripts ./scripts/*.sh
