#!/usr/bin/env bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

(
    cd "${PROJECT}/core"

    docker rm -f ledger-axon-server || true
    docker rm -f ledger-axon-mongodb || true
    "${BIN}/docker-run-axon-server.sh"
    "${BIN}/docker-run-mongodb.sh"

    ../mvnw -Djansi.force=true clean package

    docker stop ledger-axon-server
    docker stop ledger-axon-mongodb
)
