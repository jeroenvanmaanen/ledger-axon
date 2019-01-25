#!/usr/bin/env bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

source "${BIN}/verbose.sh"

if [[ ".$1" = '.--tee' ]]
then
    exec > >(tee "$2") 2>&1
    shift 2
fi

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

docker rm -f ledger-axon-server
docker rm -f ledger-axon-mongodb

"${BIN}/docker-run-axon-server.sh"
"${BIN}/docker-run-mongodb.sh"
sleep 5 # Wait for Axon Server to start

(
    cd "${PROJECT}"
    ./mvnw -Djansi.force=true clean package
    docker stop ledger-axon-server
    docker stop ledger-axon-mongodb

    (
        cd ledger
        docker-compose rm --stop --force
        docker volume rm -f ledger_mongo-data
        docker volume rm -f ledger_axon-data
    )
    ledger/docker-compose-up.sh &
    PID_LEDGER="$!"
    trap "echo ; kill '${PID_LEDGER}' ; waitForDockerComposeReady" EXIT

    cd data
    AXON_SERVER_URL='http://localhost:8024'
    waitForServerReady "${AXON_SERVER_URL}/actuator/health"
    LEDGER_API_URL='http://localhost:8090'
    waitForServerReady "${LEDGER_API_URL}/actuator/health"
    sleep 5

    echo 'Importing accounts' >&2
    curl -sS -X POST "${LEDGER_API_URL}/account/upload" -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "file=@accounts-local.yaml"
    echo 'Importing entries' >&2
    curl -sS -X POST "${LEDGER_API_URL}/entry/upload/TDF" -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "file=@transactions-local.tsv"
    echo 'Importing compound samples' >&2
    curl -sS -X POST "${LEDGER_API_URL}/compound/upload" -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "file=@compound-local.yaml"
    echo 'Imported all' >&2

    wait "${PID_LEDGER}"
)