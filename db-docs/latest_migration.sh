#!/bin/bash

docker exec kitu-db-1 psql \
  -U kitu \
  -d kitu-schemaspy \
  -t \
  -c "select script from flyway_schema_history order by version::int desc limit 1"