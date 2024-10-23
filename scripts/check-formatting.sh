#!/usr/bin/env bash

set -eux

prettier --check '**/*.{md,yml,yaml,js,ts,jsx,tsx,cjs,cts,mjs,mts,vue,astro,json}'
(cd server && ktlint)
shellcheck -P ./scripts ./scripts/*.sh
