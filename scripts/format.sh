#!/usr/bin/env bash

set -eu

prettier --write e2e
prettier --write infra
prettier --write scripts
prettier --write server
(cd server && ktlint --format)
