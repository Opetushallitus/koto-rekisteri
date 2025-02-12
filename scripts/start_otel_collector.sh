#!/usr/bin/env bash
set -euo pipefail

docker run --rm -v "$REPO_ROOT/otel-config.yml":/otel-local-config.yml \
  -p 4318:4318 -p 55681:55681 \
  otel/opentelemetry-collector:latest \
  --config /otel-local-config.yml
  
