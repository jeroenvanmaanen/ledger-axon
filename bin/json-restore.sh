#!/usr/bin/env bash

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"
DATA="${PROJECT}/data"

PATTERN="$1" ; shift

for F in $(eval " echo ${DATA}/entries-export-${PATTERN}-local.json")
do
    echo ">>> ${F}"
    curl -F "file=@${F}" http://localhost:3000/api/entry/restore
done