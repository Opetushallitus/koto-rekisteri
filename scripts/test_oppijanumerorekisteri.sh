#!/usr/bin/env bash

curl -s -i \
    -X POST "https://virkailija.testiopintopolku.fi/cas/v1/tickets" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$1&password=$2"
