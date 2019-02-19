#!/bin/bash

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

source "${BIN}/verbose.sh"

SUFFIX=''
if [[ -n "$1" ]]
then
    SUFFIX="$1"
    shift
else
    SUFFIX="$(date '+%Y%m%dT%H%M')"
fi
log "SUFFIX=[${SUFFIX}]"

docker run --rm -ti \
    -v ledger_axon-data:/opt/axonframework/data \
    -v "${PROJECT}:${PROJECT}" -w '/opt/axonframework/data' \
    alpine:latest /bin/sh -c "tar -cvzf - . > '${PROJECT}/data/backup-${SUFFIX}.tar.gz'"
