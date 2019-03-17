#!/usr/bin/env bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

: ${API_SERVER_PORT=8080}
"${BIN}/create-local-settings.sh"

source "${PROJECT}/ledger/etc/settings-local.sh"

(
    cd "${PROJECT}/data"
    LEDGER_API_URL="http://localhost:${API_SERVER_PORT}"
    echo 'Importing accounts' >&2
    curl -sS -X POST "${LEDGER_API_URL}/api/account/upload" -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "file=@accounts-local.yaml"
)