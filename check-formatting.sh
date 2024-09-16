#!/usr/bin/env bash

set -eux

prettier --check .
(cd server && ktlint)
