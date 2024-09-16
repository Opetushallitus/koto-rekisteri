#!/usr/bin/env bash

set -eu

prettier --write .
(cd server && ktlint --format)
