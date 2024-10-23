#!/usr/bin/env bash

set -eux

prettier --write '**/*.{md,yml,yaml,js,ts,jsx,tsx,cjs,cts,mjs,mts,vue,astro,json}'
(cd server && ktlint --format)
