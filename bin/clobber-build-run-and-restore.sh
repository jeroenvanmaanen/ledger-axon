#!/usr/bin/env bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

declare -a FLAGS_INHERIT
source "${BIN}/verbose.sh"

if [[ ".$1" = '.--help' ]]
then
    echo "Usage: $(basename "$0") [ -v [ -v ] ] [ --tee <file> ] [ --skip-build ] <backup-timestamp> [ --dev ]" >&2
    echo "       $(basename "$0") --help" >&2
    exit 0
fi

if [[ ".$1" = '.--tee' ]]
then
    exec > >(tee "$2") 2>&1
    shift 2
fi

DO_BUILD='true'
if [[ ".$1" = '.--skip-build' ]]
then
  DO_BUILD='false'
  shift
fi

BACKUP_TIMESTAMP="$1" ; shift
log "BACKUP_TIMESTAMP=[${BACKUP_TIMESTAMP}]"

"${BIN}/swagger-yaml-to-json.sh"

function waitForServerReady() {
    local URL="$1"
    local N="$2"
    if [[ -z "${N}" ]]
    then
        N=120
    fi
    while [[ "${N}" -gt 0 ]]
    do
        N=$[$N - 1]
        sleep 1
        if curl -sS "${URL}" >/dev/null 2>&1
        then
            break
        fi
    done
}

function countRunningContainers() {
    local HASH
    for HASH in $(docker-compose ps -q 2>/dev/null)
    do
        docker inspect -f '{{.State.Status}}' "${HASH}"
    done | grep -c running
}

function waitForDockerComposeReady() {
    (
        cd "${PROJECT}/ledger"
        while [[ "$(countRunningContainers)" -gt 0 ]]
        do
            sleep 0.5
        done
    )
}

: ${AXON_SERVER_PORT=8024}
: ${API_SERVER_PORT=8080}
"${BIN}/create-local-settings.sh"

source "${PROJECT}/ledger/etc/settings-local.sh"

sleep 5 # Wait for Axon Server to start

(
    cd "${PROJECT}"

    if "${DO_BUILD}"
    then
        docker rm -f ledger-axon-server || true
        docker rm -f ledger-axon-mongodb || true
        "${BIN}/docker-run-axon-server.sh"
        "${BIN}/docker-run-mongodb.sh"

        ./mvnw -Djansi.force=true clean package

        docker stop ledger-axon-server
        docker stop ledger-axon-mongodb
    fi

    (
        cd ledger
        docker-compose rm --stop --force
        docker volume rm -f ledger_mongo-data
        docker volume rm -f ledger_axon-data
    )

    echo 'Restoring backup' >&2
    "${BIN}/data-restore.sh" "${BACKUP_TIMESTAMP}"
    echo 'Restored backup' >&2

    ledger/docker-compose-up.sh "${FLAGS_INHERIT[@]}" "$@"
)