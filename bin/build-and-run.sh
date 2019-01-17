#!/usr/bin/env bash

set -e

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

"${BIN}/swagger-yaml-to-json.sh"

(
    cd "${PROJECT}"
    ./mvnw clean package
    java -jar core/target/ledger-axon-core-0.0.1-SNAPSHOT.jar
)