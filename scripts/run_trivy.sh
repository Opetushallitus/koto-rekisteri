#!/usr/bin/env bash

set -euo pipefail

source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

repo="https://github.com/Opetushallitus/koto-rekisteri"
reportDir="temp"
repoName="koto-rekisteri"
trivyImage=aquasec/trivy:latest

docker pull ${trivyImage}

docker run --rm \
  --volume trivy-cache:/trivy-cache \
  --volume ${reportDir}:/reports \
  ${trivyImage} \
  repo ${repo} \
  --cache-dir /trivy-cache \
  --scanners vuln \
  --format json \
  --output /reports/${repoName}_trivy.json
