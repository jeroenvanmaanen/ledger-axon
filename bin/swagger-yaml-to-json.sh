#!/usr/bin/env bash

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"
GENERATED_API="${PROJECT}/core/target/generated-api"

source "${BIN}/verbose.sh"

mkdir -p "${GENERATED_API}"

if [[ ".$1" = '.-x' ]]
then
    docker run --rm -v "${PROJECT}:${PROJECT}" -w "${PROJECT}" simplealpine/yaml2json:latest core/src/main/resources/web/etc/swagger.yaml \
        | tee "${GENERATED_API}/swagger.json"
else
    docker run --rm -v "${PROJECT}:${PROJECT}" -w "${PROJECT}" simplealpine/yaml2json:latest core/src/main/resources/web/etc/swagger.yaml \
        | docker run --rm -i stedolan/jq . \
        > "${GENERATED_API}/swagger.json"
fi
