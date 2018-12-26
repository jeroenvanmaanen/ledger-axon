#!/usr/bin/env bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

"${BIN}/swagger-yaml-to-json.sh"

(
    cd "${PROJECT}"
    ./mvnw clean package
    java -jar target/ledger-axon-0.0.1-SNAPSHOT.jar
)