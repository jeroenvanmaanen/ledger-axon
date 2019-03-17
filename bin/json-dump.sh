#!/usr/bin/env bash

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"
DATA="${PROJECT}/data"

YEAR="$1" ; shift

for M in "$@"
do
    echo ">>> $M"
    curl -F datePrefix="${YEAR}-${M}" http://localhost:3000/api/entries/date-prefix > "${DATA}/entries-export-${YEAR}${M}-local.json"
done
