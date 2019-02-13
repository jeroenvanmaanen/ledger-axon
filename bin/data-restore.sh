#!/bin/bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

source "${BIN}/verbose.sh"

SUFFIX=''
if [[ -n ".$1" ]]
then
    SUFFIX="-$1"
    shift
fi

AXON_VOLUME='ledger_axon-data'

function docker-volume-check() {
    local VOLUME_NAME="$1"
    local RESULT="$(docker volume inspect "${VOLUME_NAME}" -f '{{.Name}}' 2>/dev/null || true)"
    if [[ -n "${RESULT}" ]]
    then
        return 0
    else
        return 1
    fi
}

function docker-volume-rm() {
    local VOLUME_NAME="$1"
    if docker-volume-check "${VOLUME_NAME}"
    then
        docker volume rm "${VOLUME_NAME}"
        if docker-volume-check "${VOLUME_NAME}"
        then
            error "Could not remove volume: ${VOLUME_NAME}"
        fi
    fi
}

(
    cd "${PROJECT}/ledger"
    docker-compose rm -f axon-server || true
    docker-compose rm -f mongodb || true
)
docker-volume-rm "${AXON_VOLUME}"
docker-volume-rm ledger_mongo-data
docker volume create "${AXON_VOLUME}"

docker run --rm -ti \
    -v "${AXON_VOLUME}:/opt/axonframework/data" \
    -v "${PROJECT}:${PROJECT}" -w '/opt/axonframework/data' \
    alpine:latest /bin/sh -c "tar -xvzf '${PROJECT}/data/backup${SUFFIX}.tar.gz'"
