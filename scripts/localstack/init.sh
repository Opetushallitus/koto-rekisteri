#!/usr/bin/env bash
# LocalStack ajaa tämän skriptin, kun S3-palvelu on valmis.
# Luo bucketin, jota kitu käyttää paikallisesti `local`-profiililla.

set -eu

awslocal s3 mb "s3://kitu-bucket-local"